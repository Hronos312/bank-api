import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class MigrateLegacyData {

    public static void main(String[] args) throws Exception {
        if (args.length != 6 || !"--apply".equals(args[0])
                || List.of(args).subList(1, 6).stream().distinct().count() != 5) {
            throw new IllegalArgumentException("Ожидаются --apply и пять различных контейнеров PostgreSQL");
        }
        var legacy = new Database(args[1], "bank_user", "bank_api");
        var legacyNotifications = new Database(args[2], "notification_user", "notifications_db");
        var identity = new Database(args[3], "identity_user", "identity_db");
        var banking = new Database(args[4], "banking_user", "banking_db");
        var notifications = new Database(args[5], "notification_user", "notifications_db");
        var migrations = List.of(
                new Migration(legacy, identity, List.of("users")),
                new Migration(legacy, banking, List.of("bank_accounts", "cards", "transactions", "outbox_events")),
                new Migration(legacyNotifications, notifications, List.of("processed_events", "notifications")));

        Path work = Files.createTempDirectory("bank-api-migration-");
        try {
            for (var migration : migrations) {
                verifyEmpty(migration, work);
                export(migration, work);
            }
            for (var migration : migrations) {
                importAndVerify(migration, work);
            }
            run(banking.psql("-c", "SELECT count(*) AS accounts, coalesce(sum(balance), 0) AS total_balance"
                    + " FROM bank_accounts"), null, null);
        } finally {
            try (var files = Files.list(work)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(work);
        }
    }

    private static void verifyEmpty(Migration migration, Path work) throws Exception {
        Path result = work.resolve("preflight.txt");
        for (String table : migration.tables()) {
            run(migration.target().psql("-c", "SELECT count(*) FROM " + table), null, result);
            if (!"0".equals(Files.readString(result).strip())) {
                throw new IllegalStateException("Целевая таблица уже содержит данные: " + table);
            }
        }
    }

    private static void export(Migration migration, Path work) throws Exception {
        var command = migration.source().command("pg_dump", "--data-only", "--no-owner", "--no-privileges",
                "--column-inserts", "--rows-per-insert=100");
        for (String table : migration.tables()) {
            command.add("--table=public." + table);
        }
        Path dump = dumpFile(migration, work);
        run(command, null, dump);
        for (String table : migration.tables()) {
            exportRows(migration.source(), table, work.resolve(table + ".source"));
        }
    }

    private static void importAndVerify(Migration migration, Path work) throws Exception {
        Path importFile = work.resolve(migration.target().name() + "-import.sql");
        String lock = "LOCK TABLE " + String.join(", ", migration.tables()) + " IN ACCESS EXCLUSIVE MODE;\n";
        StringBuilder checks = new StringBuilder(lock).append("DO $$ BEGIN\n");
        for (String table : migration.tables()) {
            checks.append("IF EXISTS (SELECT 1 FROM ").append(table)
                    .append(") THEN RAISE EXCEPTION 'Целевая таблица уже содержит данные'; END IF;\n");
        }
        Files.writeString(importFile, checks.append("END $$;\n").toString());
        try (var output = Files.newOutputStream(importFile, StandardOpenOption.APPEND)) {
            Files.copy(dumpFile(migration, work), output);
        }
        run(migration.target().psql("--single-transaction", "-f", "-"), importFile, work.resolve("import.log"));

        for (String table : migration.tables()) {
            Path target = work.resolve(table + ".target");
            exportRows(migration.target(), table, target);
            if (Files.mismatch(work.resolve(table + ".source"), target) != -1L) {
                throw new IllegalStateException("Данные после переноса различаются: " + table);
            }
            System.out.println("Содержимое таблицы совпадает: " + table);
        }
    }

    private static void exportRows(Database database, String table, Path output) throws Exception {
        String key = "processed_events".equals(table) || "outbox_events".equals(table)
                ? ("processed_events".equals(table) ? "event_id" : "id") : "id";
        run(database.psql("-c", "SET TIME ZONE 'UTC'; SET DateStyle TO 'ISO, YMD';"
                + " COPY (SELECT * FROM " + table + " ORDER BY " + key + ") TO STDOUT WITH (FORMAT csv)"), null, output);
    }

    private static Path dumpFile(Migration migration, Path work) {
        return work.resolve(migration.target().name() + ".sql");
    }

    private static void run(List<String> command, Path input, Path output) throws IOException, InterruptedException {
        var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
        process.redirectOutput(output == null ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.to(output.toFile()));
        if (input != null) {
            process.redirectInput(input.toFile());
        }
        if (process.start().waitFor() != 0) {
            throw new IOException("Команда PostgreSQL не выполнилась в контейнере " + command.get(3));
        }
    }

    private record Database(String container, String user, String name) {

        List<String> command(String program, String... arguments) {
            var command = new ArrayList<>(List.of("docker", "exec", "-i", container, program, "-U", user, "-d", name));
            command.addAll(List.of(arguments));
            return command;
        }

        List<String> psql(String... arguments) {
            var command = command("psql", "-XqAt", "-v", "ON_ERROR_STOP=1");
            command.addAll(List.of(arguments));
            return command;
        }
    }

    private record Migration(Database source, Database target, List<String> tables) {
    }
}

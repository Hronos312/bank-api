$ErrorActionPreference = 'Stop'
$prefix = 'bank-migration-' + [guid]::NewGuid().ToString('N').Substring(0, 10)
$started = [System.Collections.Generic.List[string]]::new()

function Start-Database([string]$name, [string]$user, [string]$database) {
    $container = "$prefix-$name"
    $null = docker run --detach --rm --name $container --env "POSTGRES_USER=$user" `
        --env "POSTGRES_DB=$database" --env 'POSTGRES_PASSWORD=synthetic-migration-password' postgres:17-alpine
    if ($LASTEXITCODE) { throw "Не удалось запустить временную базу $name" }
    $started.Add($container)
    $deadline = [datetime]::UtcNow.AddSeconds(45)
    do {
        $null = docker exec $container pg_isready -q -h 127.0.0.1 -U $user -d $database
        if ($LASTEXITCODE -eq 0) { return $container }
        Start-Sleep -Seconds 1
    } until ([datetime]::UtcNow -gt $deadline)
    throw "Временная база $name не запустилась"
}

function Sql([string]$container, [string]$user, [string]$database, [string[]]$text) {
    $text | docker exec -i $container psql -XAtq -v 'ON_ERROR_STOP=1' -U $user -d $database
    if ($LASTEXITCODE) { throw "Не удалось выполнить SQL в $container" }
}

function Legacy-Schema([string]$path) {
    git show "181493f:$path"
    if ($LASTEXITCODE) { throw 'В истории Git отсутствует исходная схема 181493f' }
}

try {
    $legacy = Start-Database legacy-bank bank_user bank_api
    $legacyNotification = Start-Database legacy-notification notification_user notifications_db
    $identity = Start-Database identity identity_user identity_db
    $banking = Start-Database banking banking_user banking_db
    $notification = Start-Database notification notification_user notifications_db

    foreach ($name in @('V1__init_schema.sql','V2__create_outbox_events.sql','V3__add_outbox_lease.sql')) {
        Sql $legacy bank_user bank_api (Legacy-Schema "src/main/resources/db/migration/$name")
    }
    $legacyNotificationSchema = 'services/notification-service/src/main/resources/db/migration/V1__init_notification_schema.sql'
    Sql $legacyNotification notification_user notifications_db (Legacy-Schema $legacyNotificationSchema)
    Sql $identity identity_user identity_db (Get-Content 'services/identity-service/src/main/resources/db/migration/V1__init_users.sql')
    foreach ($migration in (Get-ChildItem 'services/banking-service/src/main/resources/db/migration' -Filter '*.sql' | Sort-Object Name)) {
        Sql $banking banking_user banking_db (Get-Content -LiteralPath $migration.FullName)
    }
    Sql $notification notification_user notifications_db (Get-Content $legacyNotificationSchema)

    Sql $legacy bank_user bank_api @'
INSERT INTO users (email,password_hash,first_name,last_name,birth_date,phone,role,status)
VALUES ('sender@example.test','$2a$10$01234567890123456789012345678901234567890123456789012','Test','Sender','1990-01-01','+79990000101','CLIENT','BLOCKED'),
       ('admin@example.test','$2a$10$01234567890123456789012345678901234567890123456789012','Test','Admin','1990-01-01','+79990000102','ADMIN','ACTIVE');
INSERT INTO bank_accounts (account_number,user_id,balance)
VALUES ('40817810000000000101',1,75.01),('40817810000000000102',2,24.99);
INSERT INTO cards (card_number,bank_account_id,expiration_date)
VALUES ('4111111111111111',1,'2030-01-01');
INSERT INTO transactions (type,source_account_id,destination_account_id,amount,description)
VALUES ('TRANSFER',1,2,24.99,E'Test\nmultiline "description"');
INSERT INTO outbox_events (id,topic,event_key,event_type,payload)
VALUES ('19b5e17a-9b34-4ec5-a649-2c4946ba1488','bank.transactions.v1','account-1','TRANSFER_COMPLETED',
        '{"eventId":"19b5e17a-9b34-4ec5-a649-2c4946ba1488","transactionId":1}');
'@
    Sql $legacyNotification notification_user notifications_db @'
INSERT INTO processed_events (event_id) VALUES ('19b5e17a-9b34-4ec5-a649-2c4946ba1488');
INSERT INTO notifications (event_id,transaction_id,event_type,source_account_id,destination_account_id,amount,currency)
VALUES ('19b5e17a-9b34-4ec5-a649-2c4946ba1488',1,'TRANSFER_COMPLETED',1,2,24.99,'RUB');
'@
    java '-Dstdout.encoding=UTF-8' tools/MigrateLegacyData.java --apply $legacy $legacyNotification $identity $banking $notification
    if ($LASTEXITCODE) { throw 'Репетиция миграции завершилась ошибкой' }
    $total = Sql $banking banking_user banking_db 'SELECT count(*) || chr(124) || sum(balance) FROM bank_accounts'
    if ($total -ne '2|100.00') { throw 'Баланс после миграции не совпал' }
    $id = Sql $banking banking_user banking_db "INSERT INTO bank_accounts (account_number,user_id) VALUES ('40817810000000000103',1) RETURNING id"
    if ($id -ne '3') { throw 'Последовательность счетов не перенесена' }
    'Репетиция миграции прошла. Совпали семь таблиц, баланс и следующий ID счёта'
} finally {
    foreach ($container in $started) {
        $null = docker stop $container
    }
}

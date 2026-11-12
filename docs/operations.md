# Эксплуатация и проверка Bank API

```powershell
mvn -B clean verify
java tools/GenerateDevSecrets.java
docker compose --env-file secrets/compose.env -p bank-api-smoke up -d --build --wait
pwsh -File tools/smoke.ps1 -Recovery
pwsh -File tools/rehearse-migration.ps1
```

Smoke test создаёт синтетические данные. Сценарий восстановления временно останавливает Identity и Kafka и перезапускает Notification. Репетиция переноса запускает отдельные временные PostgreSQL. Удаляйте тома только у тестового проекта.

Проверяйте `/actuator/health/liveness` и `/actuator/health/readiness` внутри сервиса. Banking принимает операции при отказе Kafka, потому что события сохраняются в Outbox. Notification проверяет базу и брокер. При недоступности Identity Banking отклоняет защищённые запросы до восстановления.

Для расследования используйте `X-Correlation-Id`, `traceId`, `sourceTraceId` и `eventId` в JSON-логах. Gateway генерирует UUID при отсутствии корректного идентификатора. Banking сохраняет корреляцию вместе с событием, Notification устанавливает её в MDC на время обработки. Для задержек используйте метрики Actuator и `pwsh -File tools/measure-read.ps1 -Samples 120 -Concurrency 8` на отдельном стенде.

Пишите прикладные сообщения через SLF4J на русском. INFO фиксирует значимые события, WARN — повторяемые сбои и DLQ, DEBUG — подробности с безопасными идентификаторами, ERROR — внутренние ошибки. Исключайте пароли, токены, PAN, номера счетов, личные данные и полный payload.

При росте Outbox контролируйте возраст старейшей неопубликованной записи и `attempts`. При проблеме Notification контролируйте lag группы `notification-service-dev`. Ограничивайте доступ к DLQ, где хранится оригинальное сообщение. Перед повторной публикацией исправленного события сверяйте причину отказа и `eventId`. `notifications.status=PENDING` означает, что запись сохранена для канала доставки.

Перед тегом версии проверьте Maven, Compose с восстановлением, миграцию и сканирование зависимостей на одном коммите. Для реального развёртывания настройте TLS, аутентификацию Kafka, права на topic и DLQ, защищённое хранение ключей, резервные копии и лимиты на периметре. Локальный Compose использует PLAINTEXT Kafka, лимит Gateway действует в рамках одного экземпляра.

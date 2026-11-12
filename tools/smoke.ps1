param(
    [string]$Project = 'bank-api-smoke',
    [string]$EnvFile = 'secrets/compose.env',
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [switch]$Recovery
)

$ErrorActionPreference = 'Stop'
$composeArgs = @('compose', '--env-file', $EnvFile, '-p', $Project)
$suffix = [guid]::NewGuid().ToString('N')
$password = 'Synthetic-Smoke-Password-Only!'
$topic = 'bank.transactions.v1'

function Docker-Compose([string[]]$arguments) {
    $result = & docker @composeArgs @arguments
    if ($LASTEXITCODE) { throw "Docker Compose завершился с ошибкой ($LASTEXITCODE)" }
    $result
}

function Invoke-Sql([string]$service, [string]$user, [string]$database, [string]$sql) {
    Docker-Compose @('exec', '-T', $service, 'psql', '-XAtq', '-v', 'ON_ERROR_STOP=1',
                     '-U', $user, '-d', $database, '-c', $sql)
}

function Request([string]$method, [string]$path, [int]$status,
                 [hashtable]$headers = @{}, [hashtable]$body) {
    $argsForRequest = @{
        Method = $method
        Uri = "$BaseUrl$path"
        Headers = $headers
        TimeoutSec = 10
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $body) {
        $argsForRequest.ContentType = 'application/json'
        $argsForRequest.Body = ConvertTo-Json -InputObject $body -Compress
    }
    $result = Invoke-WebRequest @argsForRequest
    if ($result.StatusCode -ne $status) {
        throw "$method $path вернул $($result.StatusCode), ожидался $status"
    }
    if ($result.Content -and $result.Headers.'Content-Type' -like 'application/json*') {
        return ConvertFrom-Json -InputObject $result.Content -AsHashtable
    }
}

function Wait-For([scriptblock]$check, [string]$failure, [int]$seconds = 95) {
    $deadline = [datetime]::UtcNow.AddSeconds($seconds)
    do {
        if (& $check) { return }
        Start-Sleep -Seconds 1
    } until ([datetime]::UtcNow -gt $deadline)
    throw $failure
}

function Register([string]$label) {
    $email = "$label-$suffix@example.test"
    $null = Request POST /api/auth/register 201 @{} @{
        email = $email
        password = $password
        firstName = 'Smoke'
        lastName = 'Test'
        birthDate = '1990-01-01'
        phone = '+7999' + (Get-Random -Minimum 1000000 -Maximum 9999999)
    }
    $email
}

function Login([string]$email) {
    $auth = Request POST /api/auth/login 200 @{} @{ email = $email; password = $password }
    @{ Authorization = "Bearer $($auth.accessToken)" }
}

function Outbox([long]$transactionId) {
    Invoke-Sql banking-db banking_user banking_db (
        "SELECT id, event_key, payload::text FROM outbox_events WHERE payload->>'transactionId' = '$transactionId' AND published_at IS NOT NULL")
}

function Notification-Count([guid]$eventId) {
    Invoke-Sql notification-db notification_user notifications_db (
        "SELECT (SELECT count(*) FROM processed_events WHERE event_id = '$eventId'), (SELECT count(*) FROM notifications WHERE event_id = '$eventId')")
}

$null = Request GET /actuator/health/readiness 200
$docs = Request GET /v3/api-docs 200
if (-not ($docs.paths.Contains('/api/auth/login') -and $docs.paths.Contains('/api/accounts'))) {
    throw 'В OpenAPI отсутствуют маршруты Identity или Banking'
}
$null = Request GET /api/accounts 401
$clientEmail = Register sender
$sender = Login $clientEmail
$receiver = Login (Register receiver)
$profile = Request GET /api/users/me 200 $sender
if ($profile.email -ne $clientEmail) { throw 'Identity вернул неверный профиль' }
$null = Request GET /api/admin/users 403 $sender
$null = Request GET "/internal/users/$($profile.id)" 401 $sender

$first = Request POST /api/accounts 201 $sender
$second = Request POST /api/accounts 201 $receiver
$null = Request GET "/api/accounts/$($second.id)" 404 $sender
$card = Request POST "/api/accounts/$($first.id)/card" 201 $sender
$null = Request GET "/api/cards/$($card.id)" 200 $sender

$correlation = [guid]::NewGuid().ToString()
$keyed = $sender.Clone()
$keyed['Idempotency-Key'] = $suffix
$keyed['X-Correlation-Id'] = $correlation
$deposit = Request POST "/api/accounts/$($first.id)/deposit" 200 $keyed @{ amount = 120.00 }
$repeat = Request POST "/api/accounts/$($first.id)/deposit" 200 $keyed @{ amount = 120 }
if ($repeat.id -ne $deposit.id) { throw 'Повтор создал новую операцию' }
$conflict = Request POST "/api/accounts/$($first.id)/deposit" 409 $keyed @{ amount = 121 }
if ($conflict.code -ne 'IDEMPOTENCY_CONFLICT') { throw 'Получен неверный код конфликта ключа' }
$withdraw = Request POST "/api/accounts/$($first.id)/withdraw" 200 $sender @{ amount = 20 }
$transfer = Request POST "/api/accounts/$($first.id)/transfer" 200 $sender @{
    amount = 25
    destinationAccountNumber = $second.accountNumber
}
$senderAccount = Request GET "/api/accounts/$($first.id)" 200 $sender
$receiverAccount = Request GET "/api/accounts/$($second.id)" 200 $receiver
$report = Request GET /api/reports/spending 200 $sender
if ($senderAccount.balance -ne 75 -or $receiverAccount.balance -ne 25 -or $report.totalSpent -ne 45) {
    throw 'Неверный баланс или сумма расходов'
}

foreach ($transaction in @($deposit, $withdraw, $transfer)) {
    Wait-For { $script:published = Outbox $transaction.id; [bool]$script:published } 'Outbox не завершил публикацию'
    $eventId, $eventKey, $payload = $published -split '\|', 3
    Wait-For { (Notification-Count ([guid]$eventId)) -eq '1|1' } 'Уведомление не сохранено'
    if ($transaction.id -eq $deposit.id -and ($payload | ConvertFrom-Json).correlationId -ne $correlation) {
        throw 'Идентификатор корреляции не дошёл от Gateway до Outbox'
    }
}

$adminEmail = Register administrator
$null = Invoke-Sql identity-db identity_user identity_db "UPDATE users SET role='ADMIN' WHERE email='$adminEmail'"
$admin = Login $adminEmail
$null = Request GET /api/admin/users 200 $admin
$null = Request GET /api/admin/accounts 200 $admin
$null = Request PATCH "/api/admin/users/$($profile.id)/block" 200 $admin
$null = Request GET "/api/accounts/$($first.id)" 403 $sender
$null = Request PATCH "/api/admin/users/$($profile.id)/unblock" 200 $admin
$null = Request GET "/api/accounts/$($first.id)" 200 $sender

if ($Recovery) {
    $null = Docker-Compose @('stop', 'identity')
    try {
        $failure = Request POST "/api/accounts/$($first.id)/deposit" 503 $sender @{ amount = 1000 }
        if ($failure.code -ne 'SERVICE_UNAVAILABLE') { throw 'Неверная ошибка при отказе Identity' }
    } finally {
        $null = Docker-Compose @('up', '-d', '--wait', 'identity')
    }
    if ((Request GET "/api/accounts/$($first.id)" 200 $sender).balance -ne 75) {
        throw 'Отказ Identity изменил баланс'
    }

    $null = Docker-Compose @('stop', 'kafka')
    try {
        $offline = Request POST "/api/accounts/$($first.id)/deposit" 200 $sender @{ amount = 1 }
        if (Outbox $offline.id) { throw 'Outbox подтвердил публикацию при остановленной Kafka' }
    } finally {
        $null = Docker-Compose @('up', '-d', '--wait', 'kafka')
    }
    Wait-For { $script:offlineEvent = Outbox $offline.id; [bool]$script:offlineEvent } 'Outbox не восстановился после запуска Kafka'
    $eventId, $eventKey, $payload = $offlineEvent -split '\|', 3
    Wait-For { (Notification-Count ([guid]$eventId)) -eq '1|1' } 'Уведомление после сбоя Kafka не доставлено'
    $null = Docker-Compose @('restart', 'notification-service')
    $poison = "{invalid-$suffix"
    @("$eventKey|$payload", "poison-$suffix|$poison") |
        & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:19092 `
            --sync --topic $topic --reader-property parse.key=true --reader-property 'key.separator=|'
    if ($LASTEXITCODE) { throw 'Kafka не приняла повтор и некорректную запись' }
    $dlq = & docker @composeArgs exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh `
        --bootstrap-server kafka:19092 --topic "$topic.DLT" --from-beginning --timeout-ms 15000 2>$null
    if ($dlq -notcontains $poison) { throw 'Некорректная запись не найдена в DLQ' }
    if ((Notification-Count ([guid]$eventId)) -ne '1|1') { throw 'Рестарт Notification создал дубликат' }
}

'Пройдены проверки Gateway, Identity, Banking, Kafka, Outbox и Notification'

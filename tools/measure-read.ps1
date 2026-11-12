param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [ValidateRange(20, 2000)][int]$Samples = 120,
    [ValidateRange(1, 32)][int]$Concurrency = 1
)

$ErrorActionPreference = 'Stop'
$email = "latency-$([guid]::NewGuid().ToString('N'))@example.test"
$password = 'Synthetic-Latency-Test-Password!'
$user = @{
    email = $email
    password = $password
    firstName = 'Latency'
    lastName = 'Test'
    birthDate = '1990-01-01'
    phone = '+7999' + (Get-Random -Minimum 1000000 -Maximum 9999999)
}
$null = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType 'application/json' `
    -Body (ConvertTo-Json $user -Compress) -TimeoutSec 10
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType 'application/json' `
    -Body (ConvertTo-Json @{ email = $email; password = $password } -Compress) -TimeoutSec 10
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$account = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/accounts" -Headers $headers -TimeoutSec 10
$endpoint = "$BaseUrl/api/accounts/$($account.id)"
1..10 | ForEach-Object { $null = Invoke-RestMethod -Uri $endpoint -Headers $headers -TimeoutSec 10 }

$wall = [Diagnostics.Stopwatch]::StartNew()
$measurements = @(1..$Samples | ForEach-Object -ThrottleLimit $Concurrency -Parallel {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-RestMethod -Uri $using:endpoint -Headers $using:headers -TimeoutSec 10 -ErrorAction Stop
        [pscustomobject]@{ Succeeded = $true; Milliseconds = $timer.Elapsed.TotalMilliseconds }
    } catch {
        [pscustomobject]@{ Succeeded = $false; Milliseconds = $timer.Elapsed.TotalMilliseconds }
    }
})
$wall.Stop()
$successful = @($measurements | Where-Object Succeeded | Sort-Object Milliseconds)
if ($successful.Count -eq 0) { throw 'Измерение не получило успешных ответов' }
[pscustomobject]@{
    Samples = $Samples
    Concurrency = $Concurrency
    Errors = $Samples - $successful.Count
    P50Milliseconds = [Math]::Round($successful[[Math]::Ceiling($successful.Count * .50) - 1].Milliseconds, 2)
    P95Milliseconds = [Math]::Round($successful[[Math]::Ceiling($successful.Count * .95) - 1].Milliseconds, 2)
    RequestsPerSecond = [Math]::Round($Samples / $wall.Elapsed.TotalSeconds, 1)
}

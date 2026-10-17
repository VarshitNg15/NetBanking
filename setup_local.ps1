# Run once on your personal Windows laptop. Existing .env and containers are preserved.
. "$PSScriptRoot/scripts/local-common.ps1"
Require-Command podman
Invoke-Podman info --format '{{.Host.Arch}}'

function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return ([BitConverter]::ToString($bytes)).Replace('-', '')
}
$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path $envFile)) {
    $content = Get-Content "$PSScriptRoot/.env.example" -Raw
    foreach ($key in @('JWT_SECRET','AUTH_DB_PASSWORD','USER_DB_PASSWORD','ACCOUNT_DB_PASSWORD','TRANSACTION_DB_PASSWORD','NOTIFICATION_DB_PASSWORD')) {
        $secret = New-LocalSecret
        if ($key -eq 'JWT_SECRET') { $secret = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($secret)) }
        $content = [regex]::Replace($content, "(?m)^$key=.*$", "$key=$secret")
    }
    $content = [regex]::Replace($content, '(?m)^MAIL_USERNAME=.*$', 'MAIL_USERNAME=')
    $content = [regex]::Replace($content, '(?m)^MAIL_PASSWORD=.*$', 'MAIL_PASSWORD=')
    $content += "`nORACLE_PASSWORD=$(New-LocalSecret)`n"
    [IO.File]::WriteAllText($envFile, $content)
    Write-Host 'Created .env with unique local passwords. Keep it private.'
}
$config = Import-LocalEnv
if (-not $config['ORACLE_PASSWORD']) { throw 'Add ORACLE_PASSWORD (a strong new local admin password) to .env before creating Oracle.' }

function Ensure-Container([string]$Name, [string[]]$Options) {
    & podman container exists $Name
    if ($LASTEXITCODE -eq 0) { Invoke-Podman start $Name; return }
    if ($LASTEXITCODE -ne 1) { throw "Cannot inspect container $Name" }
    Invoke-Podman run -d --name $Name @Options
}
Ensure-Container 'oracle-db-full' @('-p','127.0.0.1:1521:1521','-e','ORACLE_PASSWORD','-v','netbanking-oracle:/opt/oracle/oradata','docker.io/gvenzl/oracle-free:23.26.3-slim')
Ensure-Container 'netbanking-redis' @('-p','127.0.0.1:6379:6379','-v','netbanking-redis:/data','docker.io/library/redis:7-alpine','redis-server','--appendonly','yes')
Ensure-Container 'netbanking-kafka' @(
    '-p','127.0.0.1:9092:9092','-v','netbanking-kafka:/var/lib/kafka/data',
    '-e','KAFKA_NODE_ID=1','-e','KAFKA_PROCESS_ROLES=broker,controller',
    '-e','KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093',
    '-e','KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092',
    '-e','KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER',
    '-e','KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT',
    '-e','KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093',
    '-e','KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1',
    '-e','KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1',
    '-e','KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1',
    '-e','KAFKA_LOG_DIRS=/var/lib/kafka/data',
    '-e','KAFKA_HEAP_OPTS=-Xms256m -Xmx512m',
    'docker.io/apache/kafka:4.1.2'
)
Write-Host 'Waiting for Oracle (first startup may take several minutes)...'
$ready = $false
for ($attempt = 0; $attempt -lt 120; $attempt++) {
    & podman exec oracle-db-full healthcheck.sh *> $null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 5
}
if (-not $ready) { throw 'Oracle did not become ready. Run: podman logs oracle-db-full' }
Write-Host 'Infrastructure started. Next: .\initialize_database.ps1 (new database only), then configure SMTP in .env.'

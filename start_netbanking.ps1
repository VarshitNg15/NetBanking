param([switch]$Build, [switch]$Wait, [switch]$NoTunnel)
. "$PSScriptRoot/scripts/local-common.ps1"
foreach ($tool in @('java','mvn','node','podman')) { Require-Command $tool }
$config = Import-LocalEnv
foreach ($key in @('JWT_SECRET','AUTH_DB_PASSWORD','USER_DB_PASSWORD','ACCOUNT_DB_PASSWORD','TRANSACTION_DB_PASSWORD','NOTIFICATION_DB_PASSWORD')) {
    if (-not $config[$key] -or $config[$key] -like 'your_*') { throw "Configure $key in .env first." }
}
$services = @(
    @{ Name='eureka-server'; Port=8761 }, @{ Name='auth-service'; Port=8081 },
    @{ Name='User-Service'; Port=8082 }, @{ Name='Account-Service'; Port=8083 },
    @{ Name='Transaction-service'; Port=8084 }, @{ Name='Notification-Service'; Port=8085 },
    @{ Name='api-gateway'; Port=8080 }
)
foreach ($port in @($services.Port) + @(8000)) {
    if (Test-LocalPort $port) { throw "Port $port is occupied. Stop the existing app before starting another copy." }
}
Invoke-Podman start oracle-db-full netbanking-kafka netbanking-redis
$logDir = Join-Path $PSScriptRoot 'logs'
New-Item -ItemType Directory $logDir -Force | Out-Null
$stateFile = Join-Path $logDir 'local-processes.json'
if (Test-Path $stateFile) {
    foreach ($record in @(Get-Content $stateFile -Raw | ConvertFrom-Json)) {
        $existing = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
        if ($existing -and $existing.StartTime.ToUniversalTime().Ticks -eq $record.StartTicks) {
            throw 'A previous launch still has processes running. Run stop_netbanking.ps1 first.'
        }
    }
}
$records = @()
function Save-StartedProcess($Process, [string]$Name) {
    $script:records += @{ Id=$Process.Id; StartTicks=$Process.StartTime.ToUniversalTime().Ticks; Name=$Name }
    ConvertTo-Json -InputObject @($script:records) | Set-Content $stateFile
}
Write-Host 'Building missing service JARs (use -Build after pulling code changes)...'
foreach ($service in $services) {
    $folder = Join-Path $PSScriptRoot "Model_Class/$($service.Name)"
    $jars = @(Get-ChildItem "$folder/target/*.jar" -ErrorAction SilentlyContinue | Where-Object Name -notmatch '(sources|javadoc|original)')
    if ($Build -or $jars.Count -eq 0) {
        Push-Location $folder
        try { & mvn clean package -DskipTests; if ($LASTEXITCODE -ne 0) { throw "Build failed: $($service.Name)" } }
        finally { Pop-Location }
        $jars = @(Get-ChildItem "$folder/target/*.jar" | Where-Object Name -notmatch '(sources|javadoc|original)')
    }
    if ($jars.Count -ne 1) { throw "Expected one runnable JAR in $folder/target." }
    $service.Jar = $jars[0].FullName
}
foreach ($service in $services) {
    $name = $service.Name
    Write-Host "Starting $name..."
    $process = Start-Process java -ArgumentList @('-Xms128m','-Xmx512m','-jar', ('"' + $service.Jar + '"'), "--server.port=$($service.Port)") -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logDir/$name.log" -RedirectStandardError "$logDir/$name.error.log"
    Save-StartedProcess $process $name
    Wait-LocalHttp "http://127.0.0.1:$($service.Port)/actuator/health"
}
$process = Start-Process node -ArgumentList 'server.js' -WorkingDirectory "$PSScriptRoot/netbanking-frontend" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logDir/frontend.log" -RedirectStandardError "$logDir/frontend.error.log"
Save-StartedProcess $process 'frontend'
Wait-LocalHttp 'http://127.0.0.1:8000/' 30

$ngrokUrl = $null
if (-not $NoTunnel) {
    Write-Host 'Starting OPay Live Public Tunnel (https://opay-netbanking-world.loca.lt)...' -ForegroundColor Cyan
    $tunnelScript = Join-Path $PSScriptRoot 'scripts/run_tunnel.ps1'
    $tunnelProcess = Start-Process powershell -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $tunnelScript + '"'), '-Port', '8000', '-Subdomain', 'opay-netbanking-world') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logDir/tunnel.log" -RedirectStandardError "$logDir/tunnel.error.log"
    Save-StartedProcess $tunnelProcess 'localtunnel'

    $ngrokCommand = Get-Command ngrok -ErrorAction SilentlyContinue
    $ngrokExe = Join-Path $PSScriptRoot '.local-tools\ngrok\ngrok.exe'
    if ($ngrokCommand) { $ngrokExe = $ngrokCommand.Source }
    if (Test-Path -LiteralPath $ngrokExe) {
        Write-Host 'Starting ngrok tunnel...' -ForegroundColor Cyan
        $ngrokProcess = Start-Process $ngrokExe -ArgumentList @('http', '8000', '--inspect=false') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logDir/ngrok.log" -RedirectStandardError "$logDir/ngrok.error.log"
        Save-StartedProcess $ngrokProcess 'ngrok'
        Start-Sleep -Seconds 2
        try {
            $tunnels = (Invoke-RestMethod http://127.0.0.1:4040/api/tunnels -TimeoutSec 3 -ErrorAction SilentlyContinue).tunnels
            if ($tunnels) {
                $ngrokUrl = ($tunnels | Where-Object proto -eq 'https').public_url
                if (-not $ngrokUrl) { $ngrokUrl = $tunnels[0].public_url }
            }
        } catch {}
        if (-not $ngrokUrl) { $ngrokUrl = 'https://snagged-humble-outlook.ngrok-free.dev' }
    }
}

Write-Host '========================================================================' -ForegroundColor Green
Write-Host ' OPay - Netbanking World is LIVE and Running!' -ForegroundColor Green
Write-Host ' Local Dashboard:   http://localhost:8000' -ForegroundColor White
if (-not $NoTunnel) {
    Write-Host ' Custom Live Link:  https://opay-netbanking-world.loca.lt' -ForegroundColor Cyan
    if ($ngrokUrl) {
        Write-Host " Ngrok Live Link:   $ngrokUrl" -ForegroundColor Cyan
    }
}
Write-Host ' Eureka Registry:   http://localhost:8761' -ForegroundColor Gray
Write-Host ' Logs Directory:    logs/' -ForegroundColor Gray
Write-Host '========================================================================' -ForegroundColor Green

if ($Wait) {
    Write-Host 'All NetBanking microservices and frontend are running. Keeping session alive...'
    try {
        while ($true) { Start-Sleep -Seconds 5 }
    } finally {
        & "$PSScriptRoot/stop_netbanking.ps1"
    }
}


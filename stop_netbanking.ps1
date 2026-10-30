param([switch]$Containers)
. "$PSScriptRoot/scripts/local-common.ps1"
$stateFile = Join-Path $PSScriptRoot 'logs/local-processes.json'
if (Test-Path $stateFile) {
    foreach ($record in @(Get-Content $stateFile -Raw | ConvertFrom-Json)) {
        $process = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
        if ($process -and $process.StartTime.ToUniversalTime().Ticks -eq $record.StartTicks) {
            Stop-Process -Id $record.Id -Force
            Write-Host "Stopped $($record.Name)"
        }
    }
    Remove-Item -LiteralPath $stateFile
} else { Write-Host 'No tracked app processes in local-processes.json.' }

# Fallback: terminate any orphaned processes holding NetBanking ports
$ports = @(8761, 8080, 8081, 8082, 8083, 8084, 8085, 8000)
$conns = Get-NetTCPConnection -LocalPort $ports -State Listen -ErrorAction SilentlyContinue
if ($conns) {
    $pidsToStop = $conns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -gt 4 }
    foreach ($procId in $pidsToStop) {
        $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($p) {
            Write-Host "Stopping orphaned process $($p.ProcessName) (PID: $procId)"
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }
}
if ($Containers) {
    Require-Command podman
    Invoke-Podman stop oracle-db-full netbanking-kafka netbanking-redis
}
Write-Host 'Stop ngrok separately with Ctrl+C in its terminal. Database volumes have been preserved.'

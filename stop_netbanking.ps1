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
} else { Write-Host 'No tracked app processes. Stop manually launched services in their own terminals.' }
if ($Containers) {
    Require-Command podman
    Invoke-Podman stop oracle-db-full netbanking-kafka netbanking-redis
}
Write-Host 'Stop ngrok separately with Ctrl+C in its terminal. Database volumes have been preserved.'

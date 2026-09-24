# ==============================================================================
# NetBanking Platform Shutdown Script - PowerShell
# ==============================================================================

Write-Host "==================================================================" -ForegroundColor Yellow
Write-Host "            STOPPING NETBANKING MICROSERVICES PLATFORM            " -ForegroundColor Yellow
Write-Host "==================================================================" -ForegroundColor Yellow

param(
    [switch]$Containers,
    [switch]$All
)

$ports = @(8000, 8080, 8081, 8082, 8083, 8084, 8085, 8761)

foreach ($port in $ports) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conns) {
            foreach ($conn in $conns) {
                $pidToKill = $conn.OwningProcess
                Write-Host "Stopping process PID $pidToKill listening on port $port..." -ForegroundColor Cyan
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            }
        }
    } catch {
        # ignore
    }
}

Write-Host "`nAll NetBanking service ports (8000, 8080-8085, 8761) have been released." -ForegroundColor Green

if ($Containers -or $All) {
    Write-Host "`nStopping Podman containers (Oracle DB, Kafka, Redis)..." -ForegroundColor Yellow
    try {
        podman stop oracle-db-full kafka springboot-podman-demo-redis-1 2>$null
        Write-Host "Podman containers stopped." -ForegroundColor Green
    } catch {
        Write-Host "Error stopping Podman containers." -ForegroundColor Red
    }
} else {
    Write-Host "Note: Podman infrastructure containers are still running." -ForegroundColor Gray
    Write-Host "To also stop Podman containers, run: .\stop_netbanking.ps1 -Containers" -ForegroundColor DarkGray
    Write-Host "Or manually run: podman stop oracle-db-full kafka springboot-podman-demo-redis-1" -ForegroundColor DarkGray
}
Write-Host "==================================================================" -ForegroundColor Yellow

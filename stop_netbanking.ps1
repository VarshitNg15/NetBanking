# ==============================================================================
# NetBanking Platform Shutdown Script - PowerShell
# ==============================================================================

Write-Host "==================================================================" -ForegroundColor Yellow
Write-Host "            STOPPING NETBANKING MICROSERVICES PLATFORM            " -ForegroundColor Yellow
Write-Host "==================================================================" -ForegroundColor Yellow

$ports = @(8080, 8081, 8082, 8083, 8084, 8085, 8761)

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

Write-Host "`nAll NetBanking microservice ports (8080-8085, 8761) have been released." -ForegroundColor Green
Write-Host "To also stop Podman containers, run: podman stop kafka springboot-podman-demo-redis-1" -ForegroundColor Gray
Write-Host "==================================================================" -ForegroundColor Yellow

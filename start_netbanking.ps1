# ==============================================================================
# NetBanking Platform Launcher - PowerShell Script
# ==============================================================================

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "          NETBANKING MICROSERVICES PLATFORM LAUNCHER              " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$modelDir = Join-Path $rootDir "Model_Class"

# 1. Start Infrastructure Containers via Podman
Write-Host "`n[1/4] Starting Podman Infrastructure Containers (Oracle, Kafka, Redis)..." -ForegroundColor Yellow
try {
    podman start oracle-db-full kafka springboot-podman-demo-redis-1 2>$null
    Write-Host "Infrastructure containers active." -ForegroundColor Green
} catch {
    Write-Host "Warning: Podman containers could not be verified automatically. Ensure Oracle (:1521), Kafka (:9092), and Redis (:6379) are running." -ForegroundColor DarkYellow
}

# 2. Launch Eureka Server (Port 8761)
Write-Host "`n[2/4] Starting Eureka Discovery Server (Port 8761)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\eureka-server'; `$host.UI.RawUI.WindowTitle = 'Eureka Server (8761)'; Write-Host '--- EUREKA SERVER (8761) ---' -ForegroundColor Green; mvn spring-boot:run"

# Wait for Eureka to initialize
Write-Host "Waiting 12 seconds for Eureka Server to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 12

# 3. Launch Backend Microservices
Write-Host "`n[3/4] Launching Core Microservices..." -ForegroundColor Yellow

# Auth Service (:8081)
Write-Host " -> Launching Auth Service (:8081)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\auth-service'; `$host.UI.RawUI.WindowTitle = 'Auth Service (8081)'; Write-Host '--- AUTH SERVICE (8081) ---' -ForegroundColor Green; mvn spring-boot:run"

# User Service (:8082)
Write-Host " -> Launching User Service (:8082)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\User-Service'; `$host.UI.RawUI.WindowTitle = 'User Service (8082)'; Write-Host '--- USER SERVICE (8082) ---' -ForegroundColor Green; mvn spring-boot:run"

# Account Service (:8083)
Write-Host " -> Launching Account Service (:8083)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\Account-Service'; `$host.UI.RawUI.WindowTitle = 'Account Service (8083)'; Write-Host '--- ACCOUNT SERVICE (8083) ---' -ForegroundColor Green; mvn spring-boot:run"

# Transaction Service (:8084)
Write-Host " -> Launching Transaction Service (:8084)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\Transaction-service'; `$host.UI.RawUI.WindowTitle = 'Transaction Service (8084)'; Write-Host '--- TRANSACTION SERVICE (8084) ---' -ForegroundColor Green; mvn spring-boot:run"

# Notification Service (:8085)
Write-Host " -> Launching Notification Service (:8085)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\Notification-Service'; `$host.UI.RawUI.WindowTitle = 'Notification Service (8085)'; Write-Host '--- NOTIFICATION SERVICE (8085) ---' -ForegroundColor Green; mvn spring-boot:run"

# 4. Launch API Gateway (:8080)
Write-Host "`n[4/4] Starting API Gateway (Port 8080)..." -ForegroundColor Yellow
Start-Sleep -Seconds 6
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$modelDir\api-gateway'; `$host.UI.RawUI.WindowTitle = 'API Gateway (8080)'; Write-Host '--- API GATEWAY (8080) ---' -ForegroundColor Green; mvn spring-boot:run"

Write-Host "`n==================================================================" -ForegroundColor Green
Write-Host "All services have been launched in separate console windows." -ForegroundColor Green
Write-Host "Service Endpoints:" -ForegroundColor White
Write-Host " - Eureka Dashboard:        http://localhost:8761" -ForegroundColor Gray
Write-Host " - API Gateway (Main Entry): http://localhost:8080" -ForegroundColor Gray
Write-Host "`nSwagger UI Documentation Endpoints:" -ForegroundColor Yellow
Write-Host " - Auth Service Swagger:         http://localhost:8081/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host " - User Service Swagger:         http://localhost:8082/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host " - Account Service Swagger:      http://localhost:8083/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host " - Transaction Service Swagger:  http://localhost:8084/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host " - Notification Service Swagger: http://localhost:8085/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "Ready for testing. To stop all services, run .\stop_netbanking.ps1" -ForegroundColor Green

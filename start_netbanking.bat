@echo off
REM ==============================================================================
REM NetBanking Platform Launcher - Batch Shortcut
REM Launches Podman containers, Eureka, Microservices, Gateway & OJET Frontend
REM ==============================================================================
powershell -ExecutionPolicy Bypass -File "%~dp0start_netbanking.ps1"
pause

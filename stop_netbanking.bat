@echo off
REM ==============================================================================
REM NetBanking Platform Shutdown - Batch Shortcut
REM Releases ports 8000, 8080-8085, 8761 and stops background services
REM ==============================================================================
powershell -NoProfile -File "%~dp0stop_netbanking.ps1" %*
pause

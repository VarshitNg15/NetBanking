@echo off
setlocal
set MAVEN_VERSION=3.9.11
set MAVEN_HOME=%~dp0.mvn\maven-%MAVEN_VERSION%
set MAVEN_DIR=%MAVEN_HOME%
if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run_maven

echo Maven %MAVEN_VERSION% was not found. Downloading it into .mvn ...
set ZIP=%~dp0.mvn\apache-maven-%MAVEN_VERSION%-bin.zip
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ZIP%'"
if errorlevel 1 exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%~dp0.mvn'"
if errorlevel 1 exit /b 1
if not exist "%MAVEN_HOME%\bin\mvn.cmd" exit /b 1
del /q "%ZIP%" >nul 2>&1

:run_maven
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) { throw "Install $Name and reopen PowerShell; it is not on PATH." }
}
function Import-LocalEnv {
    $file = Join-Path $projectRoot '.env'
    if (-not (Test-Path $file)) { throw 'Run setup_local.ps1 first, or copy .env.example to .env and configure it.' }
    $values = @{}
    foreach ($line in Get-Content $file) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
            $value = $Matches[2].Trim().Trim('"').Trim("'")
            $values[$Matches[1]] = $value
            [Environment]::SetEnvironmentVariable($Matches[1], $value, 'Process')
        }
    }
    return $values
}
function Invoke-Podman {
    & podman @args
    if ($LASTEXITCODE -ne 0) { throw "Podman command failed (exit $LASTEXITCODE). Check Podman Desktop and the container logs." }
}
function Wait-LocalHttp([string]$Url, [int]$Seconds = 180) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Not ready: $Url. Read logs/; fix the error, stop the app, then retry."
}
function Test-LocalPort([int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try { return $client.ConnectAsync('127.0.0.1', $Port).Wait(500) -and $client.Connected }
    catch { return $false }
    finally { $client.Dispose() }
}

param(
    [int]$Port = 8000,
    [switch]$UsePodman,
    [ValidateSet('Cloudflare', 'Ngrok')]
    [string]$Provider = 'Cloudflare',
    [string]$NgrokUrl
)

$ErrorActionPreference = 'Stop'
if ($Provider -eq 'Ngrok' -and $UsePodman) { throw 'Ngrok uses the native Windows CLI. Omit -UsePodman; your app infrastructure still runs in Podman.' }
$origin = "http://127.0.0.1:$Port"
try {
    $page = Invoke-WebRequest "$origin/js/services/apiService.js" -UseBasicParsing -TimeoutSec 5
    if ($page.Content -notmatch "const GATEWAY_URL = ''") {
        throw 'The frontend is not serving the updated same-origin client.'
    }
    # This read-only request may return 401/404; a proxy 502 means the gateway is down.
    try {
        $null = Invoke-WebRequest "$origin/api/v1/accounts" -UseBasicParsing -TimeoutSec 10
    } catch {
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ge 500) { throw }
    }
} catch {
    throw "App readiness check failed. Start the backend services and restart 'node server.js' in netbanking-frontend first. Details: $_"
}

Write-Host 'Sharing the frontend and /api through an HTTPS URL.' -ForegroundColor Cyan
Write-Host 'Keep this terminal and the app running. Press Ctrl+C to stop sharing.'
if ($Provider -eq 'Ngrok') {
    $ngrokCommand = Get-Command ngrok -ErrorAction SilentlyContinue
    $ngrokExe = Join-Path $PSScriptRoot '.local-tools\ngrok\ngrok.exe'
    if ($ngrokCommand) { $ngrokExe = $ngrokCommand.Source }
    if (-not (Test-Path -LiteralPath $ngrokExe)) { throw 'Install ngrok from https://ngrok.com/download/windows or place ngrok.exe in .local-tools\ngrok. See Documentation/LOCAL_TUNNEL.md.' }
    # Preserve the public Host header so the frontend same-origin check works.
    # Disable local request capture, which would otherwise retain login payloads.
    $ngrokArgs = @('http', $origin, '--inspect=false')
    if ($NgrokUrl) {
        $domain = $NgrokUrl.Trim().TrimEnd('/') -replace '^https?://', ''
        $ngrokArgs += @("--domain=$domain")
    }
    & $ngrokExe @ngrokArgs
} elseif ($UsePodman) {
    if (-not (Get-Command podman -ErrorAction SilentlyContinue)) { throw 'Podman is not on PATH. Open a terminal where Podman is available.' }
    & podman run --rm --name netbanking-tunnel docker.io/cloudflare/cloudflared:latest tunnel --no-autoupdate --url "http://host.containers.internal:$Port"
} else {
    if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
        throw 'Install cloudflared (winget install --id Cloudflare.cloudflared --exact), reopen the terminal, and retry. Alternatively use .\start_tunnel.ps1 -UsePodman.'
    }
    & cloudflared tunnel --no-autoupdate --url $origin
}
if ($LASTEXITCODE -ne 0) { throw "Tunnel exited with code $LASTEXITCODE" }

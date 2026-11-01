param(
    [int]$Port = 8000,
    [string]$Subdomain = 'opay-netbanking-world'
)

$npxCmd = 'npx'
$cmd = Get-Command npx.cmd -ErrorAction SilentlyContinue
if ($cmd) { $npxCmd = $cmd.Source }

while ($true) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Starting localtunnel on https://$Subdomain.loca.lt..." -ForegroundColor Cyan
    & $npxCmd --yes localtunnel --port $Port --subdomain $Subdomain
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Tunnel dropped. Auto-reconnecting in 3s..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}

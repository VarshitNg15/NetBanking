# Local tunnel deployment (Windows, Podman)

The frontend Node server serves the UI and proxies `/api` requests to the local
gateway. Visitors use one HTTPS URL. No Docker, Caddy, domain, or changing gateway
CORS allowlists is required. Only API paths are proxied; operational dashboards
remain local.

## Start

### ngrok (stable free account hostname)

1. Create/sign in to a free account at https://dashboard.ngrok.com/signup.
2. Install the Windows CLI from https://ngrok.com/download/windows. If installing
   without the Store, extract `ngrok.exe` into `.local-tools/ngrok` in this repo
   (this directory is ignored by Git).
3. In your own terminal, authenticate once with the token from your ngrok dashboard:

   ```powershell
   # If installed on PATH:
   ngrok config add-authtoken YOUR_TOKEN
   # Or if extracted into this project:
   .\.local-tools\ngrok\ngrok.exe config add-authtoken YOUR_TOKEN
   ```

   Replace YOUR_TOKEN locally. Do not paste it into chat or commit it to Git.
4. Start your existing backend/Podman services and restart the Node frontend.
5. From the project root run:

   ```powershell
   .\start_tunnel.ps1 -Provider Ngrok
   ```

Share the HTTPS forwarding URL shown by ngrok. The free account uses its assigned
development hostname; you do not need DigitalPlat or Cloudflare. The launcher
disables ngrok's local request inspector and preserves the public Host header.
Do not add `--host-header=rewrite`: this would break the proxy's Origin check.
Visitors may see ngrok's free-plan notice before opening the application.
Keep the app, PC and ngrok running; Ctrl+C stops sharing. Free-plan traffic quotas
apply: https://ngrok.com/docs/pricing-limits/free-plan-limits/.

### Cloudflare (temporary hostname, alternative)

1. Start the existing Podman infrastructure and Java services using your usual
   workflow (`.\start_netbanking.ps1` if already configured). Ensure they are healthy.
2. Restart the frontend after pulling these changes. From `netbanking-frontend`,
   run `node server.js`. Do not start a second instance on port 8000.
3. From the repository root, choose one tunnel method:

   ```powershell
   # Native cloudflared (install once, then reopen your terminal)
   winget install --id Cloudflare.cloudflared --exact
   .\start_tunnel.ps1

   # Or use your existing Podman runtime; no native cloudflared installation
   .\start_tunnel.ps1 -UsePodman
   ```

4. Copy the `https://....trycloudflare.com` URL printed in the terminal. Open it
   from another device. Use demo accounts/data: anyone with this URL can access
   the app's public login and registration flows.

The Podman option reaches Windows through `host.containers.internal`. If your
Podman machine cannot resolve/reach that host, use native cloudflared instead.
The launcher checks the frontend and gateway before opening a tunnel, but does
not certify every backend dependency is healthy.

## Configuration

Defaults: frontend port 8000, gateway `http://127.0.0.1:8080`.
For a different gateway, set this in the frontend terminal before starting Node:

```powershell
$env:API_GATEWAY_URL = 'http://127.0.0.1:8080'
node server.js
```

For a containerized frontend with a gateway running on Windows, use
`API_GATEWAY_URL=http://host.containers.internal:8080` and publish port 8000.
For a custom frontend port, set `PORT` before starting Node and pass the same
port to `start_tunnel.ps1 -Port 8001`.

The browser uses relative `/api/...` URLs. The Node proxy validates browser Origin
against the incoming Host and removes Origin for the internal server-to-server
request. Keep the gateway's existing restricted CORS policy. Do not tunnel port
8080 directly, or expose Oracle, Redis, Kafka, Eureka, or Actuator.

Admin links to local service dashboards are local maintenance features and will
not work for remote visitors; they are intentionally not forwarded.

## Verify and stop

- Check login, registration/OTP, accounts, transfers, history, and statement
  downloads through the public URL. API requests should use that same HTTPS host.
- A 502 JSON response means the gateway is unavailable. A gateway 503 can mean
  services are missing from Eureka. Check backend logs and infrastructure health.
- If startup reports port 8000 in use, stop the old frontend and restart it.
- If cloudflared refuses Quick Tunnel mode because of an existing configuration,
  follow Cloudflare's Quick Tunnel documentation; do not overwrite an existing
  tunnel configuration.
- Press Ctrl+C in the tunnel terminal to stop sharing. If the Podman container
  remains running, use `podman stop netbanking-tunnel`.
- Stop the app separately with your existing shutdown script when finished.

Keep the PC awake and online. The tunnel URL changes when restarted; the app
needs no address changes, but browser sessions belong to each distinct hostname.
Quick Tunnels are temporary demo infrastructure with no uptime guarantee.

Reference: https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/

## Proxy regression checks

```powershell
node --test netbanking-frontend/server.test.js
```

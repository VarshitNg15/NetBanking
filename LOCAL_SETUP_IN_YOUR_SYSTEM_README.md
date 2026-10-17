# Local setup in your system

For a **personal Windows 10/11 laptop**, using Podman only. Run commands in
PowerShell from the project folder. Start locally first; ngrok is optional.

## 1. Install once

Install [Git](https://git-scm.com/downloads/win), [JDK 17](https://adoptium.net/temurin/releases/?version=17),
[Maven 3.9+](https://maven.apache.org/download.cgi), [Node.js LTS](https://nodejs.org/),
and [Podman Desktop](https://podman-desktop.io/downloads). Set `JAVA_HOME` to JDK 17
and add Maven's `bin` directory to PATH. Reopen PowerShell.

Recommended: 16 GB RAM and 25 GB free disk. Enable virtualization/WSL2 when Podman
asks. In Podman Desktop create and start a Podman machine; allocate at least
4 GB RAM to it, leaving memory for the seven Java services on Windows.

```powershell
git --version
java -version
mvn -version
node --version
podman info
git clone --branch main https://github.com/VarshitNg15/NetBanking.git
cd NetBanking
```

Private repository? Sign into GitHub with an account that has access.
On your own laptop only, if PowerShell blocks reviewed local scripts:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

This persists for your Windows user. Do not override corporate Group Policy.
The `.bat` shortcuts do not bypass policy.

## 2. Create database, Kafka, Redis

```powershell
.\setup_local.ps1
.\initialize_database.ps1
```

The first command creates a private `.env` with random passwords and three
Podman containers with persistent volumes. Image downloads can take several
minutes. The second creates the five Oracle schemas and tables from `Schemas/`.
**Initialize only a new database**: existing schemas cause it to stop, never drop
data. If initialization fails partway, inspect the database before retrying.
The initializer excludes the legacy SQL's demo-admin seed; no shared admin
password is enabled. Register a fresh user through the app.

Optional SQL Developer connection to inspect tables:

| Setting | Value |
|---|---|
| Host / port | `localhost` / `1521` |
| Connection type | Basic; **Service name** `FREEPDB1` (not SID) |
| User | `AUTH_SCHEMA`, `USER_SCHEMA`, `ACCOUNT_SCHEMA`, `TRANSACTION_SCHEMA`, or `NOTIFICATION_SCHEMA` |
| Password | Matching `*_DB_PASSWORD` in your new `.env` |

For administration use `SYSTEM` and `ORACLE_PASSWORD`. No old laptop database,
accounts, credentials, ngrok executable, or `.env` is transferred by Git.
If you already have Oracle on port 1521, do not create another database there;
configure its URLs/users in `.env` and initialize its schemas manually instead.

## 3. Configure email and start

Open `.env`. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD`
using a working SMTP account. Gmail requires an app password where supported.
Email delivery is needed for OTP/registration flows. Keep all generated passwords
and local URLs unchanged for the default setup. Never commit `.env`.

```powershell
.\start_netbanking.ps1 -Build
```

This builds the Java services, starts them in the background, checks health,
and starts the Node frontend. Open **http://localhost:8000** and register a test
user. This launch does not run Maven tests (`mvn test` can be run per service).
The current frontend uses CDN assets and needs no `npm install` to run `server.js`.
Logs are in `logs/`. Eureka is at http://localhost:8761.

## 4. Optional: share through ngrok

Install [ngrok for Windows](https://ngrok.com/download/windows), create/sign into
your free account, and get a **new token** from its dashboard. Revoke any token
previously posted in chat. Enter the replacement only in your local terminal:

```powershell
ngrok config add-authtoken YOUR_NEW_TOKEN
.\start_tunnel.ps1 -Provider Ngrok
```

Share the HTTPS URL ngrok prints. Your free account chooses its assigned stable
hostname; no DigitalPlat/domain purchase is needed. The frontend forwards API
calls automatically. Keep your laptop awake, app running, and tunnel terminal open.
Only use demo data: the app is publicly reachable. Free-plan usage limits apply.

If ngrok is not on PATH, put `ngrok.exe` in `.local-tools/ngrok/` and use
`.\.local-tools\ngrok\ngrok.exe config add-authtoken YOUR_NEW_TOKEN`.
The launcher also finds it there. Never copy credentials into repository files.
For direct launch without a PowerShell script:

```powershell
ngrok http http://127.0.0.1:8000 --inspect=false
```

## 5. Everyday start / stop / update

```powershell
# Start Podman machine in Podman Desktop first
.\start_netbanking.ps1
# Optional public tunnel, in another terminal
.\start_tunnel.ps1 -Provider Ngrok
# Stop tunnel with Ctrl+C; then stop app + containers (data is retained)
.\stop_netbanking.ps1 -Containers
# To get new code: stop app first, then
git pull --ff-only
.\start_netbanking.ps1 -Build
```

The stop script stops only processes tracked by this launcher, not unrelated Java
apps. Stop older manually launched services in their original terminals.

| Problem | Check |
|---|---|
| Podman command missing | Reopen terminal; ensure Podman CLI is on PATH |
| Podman connection fails | Start the Podman machine in Podman Desktop |
| Port already occupied | Stop the previous app/container; do not start a duplicate |
| Oracle not ready | `podman logs oracle-db-full` |
| Service health timeout | Read `logs/<service>.log` and `.error.log`; check schema/password/SMTP |
| Schema validation error | Check tables in `FREEPDB1`; do not switch Hibernate to `create` |
| Tunnel API gives 502/503 | Check gateway/backend logs and Eureka registration |

More tunnel details: [LOCAL_TUNNEL.md](Documentation/LOCAL_TUNNEL.md).

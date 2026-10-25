# NetBanking - Live Demo Guide

## 🌐 Permanent Demo Link
- **Live Portal**: [https://snagged-humble-outlook.ngrok-free.dev](https://snagged-humble-outlook.ngrok-free.dev)
- **Admin Dashboard**: [https://snagged-humble-outlook.ngrok-free.dev/#/admin](https://snagged-humble-outlook.ngrok-free.dev/#/admin)
- **Admin Credentials**: `admin@netbanking.com` / `AdminPassword123!`

> **Note for Interviewers/Visitors**: On the first visit, click the blue **"Visit Site"** button on the ngrok screen to enter the portal.

---

## 🚀 How to Start the Demo

### Step 1: Start the Backend & Frontend
In PowerShell from the project root:
```powershell
.\start_netbanking.ps1
```
*Automatically starts Podman containers (Oracle DB, Kafka, Redis), compiles/boots all 7 microservices, and launches the web portal on `http://localhost:8000`.*

### Step 2: Start the Ngrok Tunnel
In a second PowerShell terminal:
```powershell
.\start_tunnel.ps1 -Provider Ngrok -NgrokUrl "https://snagged-humble-outlook.ngrok-free.dev"
```
*Your application is now publicly accessible at your permanent demo link.*

---

## 🛑 How to Stop
1. In the tunnel terminal, press **`Ctrl + C`**.
2. In your main terminal, stop the app and containers:
```powershell
.\stop_netbanking.ps1 -Containers
```

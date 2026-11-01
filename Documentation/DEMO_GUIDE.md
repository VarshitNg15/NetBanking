# OPay - Netbanking World - Live Demo Guide

## 🌐 Public Live Demo Links
- **Primary Live Portal (Custom Subdomain)**: [https://opay-netbanking-world.loca.lt](https://opay-netbanking-world.loca.lt)
- **High-Reliability Ngrok Portal**: [https://snagged-humble-outlook.ngrok-free.dev](https://snagged-humble-outlook.ngrok-free.dev)
- **Admin Dashboard**: [https://opay-netbanking-world.loca.lt/?ojr=admin](https://opay-netbanking-world.loca.lt/?ojr=admin)
- **Admin Credentials**: `admin@netbanking.com` / `AdminPassword123!`

> **Note for Interviewers/Visitors**: 
> - On **Localtunnel** (`opay-netbanking-world.loca.lt`), if prompted for an endpoint IP, enter the public IP of the host machine or click Submit.
> - On **Ngrok** (`snagged-humble-outlook.ngrok-free.dev`), click the blue **"Visit Site"** button on the ngrok advisory screen to enter the portal.

---

## 🚀 How to Start the Demo (1-Click Startup)

In PowerShell from the project root:
```powershell
.\start_netbanking.ps1
```
*This command automatically:*
1. Starts Podman containers (Oracle DB, Kafka, Redis).
2. Verifies and boots all 7 Spring Boot microservices & Eureka discovery.
3. Launches the Oracle JET web portal on `http://localhost:8000`.
4. Starts the resilient auto-reconnecting **Localtunnel** (`https://opay-netbanking-world.loca.lt`).
5. Starts the background **ngrok** tunnel (`https://snagged-humble-outlook.ngrok-free.dev`).

Both live public links will be printed right in your console!

---

## 🛑 How to Stop
In your PowerShell terminal:
```powershell
.\stop_netbanking.ps1 -Containers
```
*This cleanly stops all microservices, Node frontend, localtunnel, and ngrok background processes, preserving database volumes.*


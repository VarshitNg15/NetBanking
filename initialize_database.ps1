# Creates fresh schemas only. Never drops or overwrites an existing schema.
. "$PSScriptRoot/scripts/local-common.ps1"
Require-Command podman
$config = Import-LocalEnv
$domains = @('Auth','User','Account','Transaction','Notification')
$checkSql = "set heading off feedback off`nwhenever sqlerror exit failure`nalter session set container=FREEPDB1;`nselect 'EXISTING=' || count(*) from dba_users where username in ('AUTH_SCHEMA','USER_SCHEMA','ACCOUNT_SCHEMA','TRANSACTION_SCHEMA','NOTIFICATION_SCHEMA');`nexit;"
$result = $checkSql | & podman exec -i oracle-db-full sqlplus -s / as sysdba
if ($LASTEXITCODE -ne 0 -or ($result -join "`n") -notmatch 'EXISTING=0\s') {
    throw 'Database already contains application schemas, or cannot be inspected. Nothing changed. Do not rerun initialization on an existing/partially initialized database; inspect it in SQL Developer.'
}
$sql = "set echo off verify off define off`nwhenever sqlerror exit failure rollback`nalter session set container=FREEPDB1;`n"
foreach ($domain in $domains) {
    $prefix = $domain.ToUpperInvariant()
    $username = $config["${prefix}_DB_USERNAME"]
    $password = $config["${prefix}_DB_PASSWORD"]
    if ($username -ne "${prefix}_SCHEMA" -or $password -notmatch '^[A-Za-z0-9_!@#%+=.-]{12,128}$') {
        throw "Use ${prefix}_SCHEMA and a 12-128 character password containing letters/numbers or _!@#%+=.- in .env."
    }
    $sql += "CREATE USER $username IDENTIFIED BY `"$password`";`nGRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE, CREATE TRIGGER, UNLIMITED TABLESPACE TO $username;`n"
    # CURRENT_SCHEMA makes SYS create objects owned by the application schema.
    $sql += "ALTER SESSION SET CURRENT_SCHEMA=$username;`n"
    $schemaFile = Join-Path $PSScriptRoot "Schemas/${domain}_Schema/$($domain.ToLowerInvariant())_schema.sql"
    $schemaSql = Get-Content $schemaFile -Raw
    if ($domain -eq 'Auth') {
        # The legacy SQL ends with a publicly known demo-admin password.
        # Fresh laptop setup imports DDL only, never that administrator account.
        $schemaSql = ($schemaSql -split '(?im)^INSERT INTO AUTH_SCHEMA\.AUTH_USER\s*\(', 2)[0]
    }
    $sql += $schemaSql + "`nALTER SESSION SET CURRENT_SCHEMA=SYS;`n"
}
$sql += "exit;`n"
$output = $sql | & podman exec -i oracle-db-full sqlplus -s / as sysdba
if ($LASTEXITCODE -ne 0) { throw 'Schema initialization failed and may be partial. Inspect the database before retrying. SQL output is suppressed to protect credentials.' }
Write-Host 'All five application schemas created. Passwords match .env. No demo users are seeded; register through the app.'

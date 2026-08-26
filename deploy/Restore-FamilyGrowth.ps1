[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [Parameter(Mandatory = $true)][ValidatePattern('^family_growth_restore_[a-z0-9_]+$')][string]$TargetDatabase,
    [string]$EnvironmentFile = ".env"
)

$ErrorActionPreference = "Stop"
$deployDirectory = Split-Path -Parent $PSCommandPath
$source = (Resolve-Path -LiteralPath $BackupFile).Path
$containerFile = "/tmp/family-growth-restore-$([guid]::NewGuid().ToString('N')).dump"

Push-Location $deployDirectory
try {
    docker compose --env-file $EnvironmentFile cp $source "db:$containerFile"
    if ($LASTEXITCODE -ne 0) { throw "docker compose cp failed" }
    docker compose --env-file $EnvironmentFile exec -T db sh -c 'createdb --username="$POSTGRES_USER" "$1"' restore $TargetDatabase
    if ($LASTEXITCODE -ne 0) { throw "Target database must be new and empty; existing databases are never overwritten" }
    docker compose --env-file $EnvironmentFile exec -T db sh -c 'pg_restore --username="$POSTGRES_USER" --dbname="$1" --exit-on-error "$2"' restore $TargetDatabase $containerFile
    if ($LASTEXITCODE -ne 0) { throw "pg_restore failed" }
    docker compose --env-file $EnvironmentFile exec -T db sh -c 'psql --username="$POSTGRES_USER" --dbname="$1" --tuples-only --command="SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE;"' verify $TargetDatabase
    if ($LASTEXITCODE -ne 0) { throw "restored database verification failed" }
} finally {
    docker compose --env-file $EnvironmentFile exec -T db rm -f $containerFile | Out-Null
    Pop-Location
}

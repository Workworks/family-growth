[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$DestinationDirectory,
    [string]$EnvironmentFile = ".env"
)

$ErrorActionPreference = "Stop"
$deployDirectory = Split-Path -Parent $PSCommandPath
$destination = [System.IO.Path]::GetFullPath($DestinationDirectory)
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$containerFile = "/tmp/family-growth-$stamp.dump"
$targetFile = Join-Path $destination "family-growth-$stamp.dump"

Push-Location $deployDirectory
try {
    docker compose --env-file $EnvironmentFile exec -T db sh -c 'pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --format=custom --file="$1"' backup $containerFile
    if ($LASTEXITCODE -ne 0) { throw "pg_dump failed" }
    docker compose --env-file $EnvironmentFile cp "db:$containerFile" $targetFile
    if ($LASTEXITCODE -ne 0) { throw "docker compose cp failed" }
    docker compose --env-file $EnvironmentFile exec -T db rm -f $containerFile
    if ($LASTEXITCODE -ne 0) { throw "temporary backup cleanup failed" }
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $targetFile).Hash.ToLowerInvariant()
    [pscustomobject]@{ Backup = $targetFile; Sha256 = $hash; Bytes = (Get-Item -LiteralPath $targetFile).Length }
} finally {
    Pop-Location
}

# Rebuilds the demo images without pulling any base image.
#
# The Dockerfiles in this repository pull `maven:3.9-eclipse-temurin-21` and
# `node:22.14.0-alpine3.21`. In an offline environment those layers are missing,
# so this script builds the artifacts on the host and layers them on top of the
# already-present runtime images. Use the normal `docker compose build` when the
# registry is reachable.
param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [string]$JdkHome = 'C:\workspace\environment\JDK21\OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9\jdk-21.0.13+9'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if (-not $SkipBackend) {
    if (Test-Path $JdkHome) {
        $env:JAVA_HOME = $JdkHome
        $env:PATH = "$JdkHome\bin;$env:PATH"
    }
    Push-Location (Join-Path $root 'backend')
    try {
        mvn -B -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'maven package failed' }
    } finally {
        Pop-Location
    }
    $dockerfile = Join-Path $root 'backend/target/Dockerfile.local'
    @'
FROM derivatives-demo-backend:latest
COPY derivatives-demo-0.1.0-SNAPSHOT.jar /app/app.jar
'@ | Set-Content -Path $dockerfile -Encoding ascii
    docker build -f $dockerfile -t derivatives-demo-backend:latest (Join-Path $root 'backend/target')
    if ($LASTEXITCODE -ne 0) { throw 'backend image rebuild failed' }
    Write-Host 'backend image rebuilt'
}

if (-not $SkipFrontend) {
    Push-Location (Join-Path $root 'frontend')
    try {
        npm run build
        if ($LASTEXITCODE -ne 0) { throw 'frontend build failed' }
    } finally {
        Pop-Location
    }
    $dockerfile = Join-Path $env:TEMP 'derivatives-frontend-offline.Dockerfile'
    @'
FROM derivatives-demo-frontend:latest
COPY dist/ /usr/share/nginx/html/
'@ | Set-Content -Path $dockerfile -Encoding ascii
    docker build -f $dockerfile -t derivatives-demo-frontend:latest (Join-Path $root 'frontend')
    if ($LASTEXITCODE -ne 0) { throw 'frontend image rebuild failed' }
    Write-Host 'frontend image rebuilt'
}

Write-Host 'done. Run: docker compose up -d --wait'

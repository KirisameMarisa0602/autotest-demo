param(
    [string]$BackendUrl = "http://localhost:8080",
    [string]$FrontendUrl = "http://localhost:4173"
)

$ErrorActionPreference = "Stop"
$headers = @{ Authorization = "Bearer trader-token" }

$health = Invoke-RestMethod -Method Get -Uri "$BackendUrl/actuator/health"
if ($health.status -ne "UP") {
    throw "Backend health was '$($health.status)', expected UP."
}

$reset = Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/demo/reset" -Headers $headers
if ($reset.businessDate -ne "2026-01-15" -or $reset.portfolioCount -ne 2) {
    throw "Demo reset returned unexpected deterministic seed metadata."
}

$market = Invoke-RestMethod -Method Get -Uri "$BackendUrl/api/market-data" -Headers $headers
if ($market.Count -ne 2 -or "AAPL" -notin $market.underlying) {
    throw "Expected the two fixed market snapshots after reset."
}

$frontend = Invoke-WebRequest -UseBasicParsing -Method Get -Uri $FrontendUrl
if ($frontend.StatusCode -ne 200 -or $frontend.Content -notmatch "衍生品交易 Demo") {
    throw "Frontend did not return the expected application shell."
}

Write-Host "Smoke checks passed: backend health, deterministic reset, market data, frontend shell."

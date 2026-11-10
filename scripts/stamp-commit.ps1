$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$sha = (& git -C $repoRoot rev-parse HEAD).Trim()

if ($LASTEXITCODE -ne 0 -or $sha -notmatch "^[0-9a-f]{40}$") {
    throw "Unable to resolve a full Git HEAD SHA."
}

# Deliberately does not rewrite tracked oracle files, so invoking it keeps the baseline clean.
Write-Output $sha

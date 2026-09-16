# Fail if business modules still host cross-module API contracts (not api.impl)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$modulesRoot = Join-Path $root "edu-mind-modules"

$violations = @()
Get-ChildItem -Path $modulesRoot -Recurse -Filter "*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    $path = $_.FullName
    if ($path -notmatch "\\api\\") {
        return
    }
    if ($path -match "\\api\\impl\\") {
        return
    }
    $violations += $path
}

if ($violations.Count -gt 0) {
    Write-Error @"
Business modules must only contain api/impl implementations.
Move contracts to backend/edu-mind-api/edu-mind-*-api/:
$($violations -join [Environment]::NewLine)
"@
}

$legacy = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '^edu-mind-.+-api$' -and $_.Name -ne 'edu-mind-api' }
if ($legacy) {
    Write-Warning "Remove legacy top-level API dirs (use edu-mind-api/ only): $($legacy.Name -join ', ')"
}

Write-Host "verify-api-modules: OK"

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$local = [System.IO.File]::ReadAllText((Join-Path $root '.env'))

function Read-Setting([string] $name) {
    $match = [regex]::Match($local, '(?im)^\s*' + [regex]::Escape($name) + '\s*=\s*["'']?([^\r\n"'']+)')
    if (-not $match.Success) { throw "Required setting $name is missing." }
    return $match.Groups[1].Value.Trim()
}

$env:VITE_SUPABASE_URL = Read-Setting 'SUPABASE_URL'
$env:VITE_SUPABASE_PUBLISHABLE_KEY = Read-Setting 'SUPABASE_PUBLISHABLE_KEY'
$env:VITE_API_PROXY_TARGET = 'http://127.0.0.1:8094'

Set-Location (Join-Path $root 'frontend')
& npm run dev -- --host 127.0.0.1 --port 5177 --strictPort
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

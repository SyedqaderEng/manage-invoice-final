$ErrorActionPreference = 'Stop'
$ollama = Get-Command ollama -ErrorAction SilentlyContinue
if ($ollama) {
    $executable = $ollama.Source
} else {
    $executable = Join-Path $env:LOCALAPPDATA 'Programs\Ollama\ollama.exe'
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw 'Ollama is not installed. Install it and pull qwen2.5vl:3b before starting local receipt AI.'
    }
}
if (Get-NetTCPConnection -LocalPort 11435 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 11435 is already in use. Stop that specific service before starting receipt AI.'
}
$env:OLLAMA_HOST = '127.0.0.1:11435'
$env:OLLAMA_NO_CLOUD = '1'
$env:OLLAMA_NUM_PARALLEL = '1'
Write-Host 'Starting local receipt AI on 127.0.0.1:11435. Keep this terminal open.'
& $executable serve
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

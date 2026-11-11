param([switch] $ReceiptAi, [switch] $ConfigureEclipse)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Read-Setting([string] $text, [string] $name) {
    $match = [regex]::Match($text, '(?im)^\s*' + [regex]::Escape($name) + '\s*=\s*["'']?([^\r\n"'']+)')
    if (-not $match.Success) { throw "Required setting $name is missing." }
    return $match.Groups[1].Value.Trim()
}

$local = [System.IO.File]::ReadAllText((Join-Path $root '.env'))
$secrets = [System.IO.File]::ReadAllText((Join-Path $root 'secrets'))
$env:SUPABASE_URL = Read-Setting $secrets 'NEXT_PUBLIC_SUPABASE_URL'
$env:SUPABASE_PUBLISHABLE_KEY = Read-Setting $secrets 'NEXT_PUBLIC_SUPABASE_ANON_KEY'
$database = [uri](Read-Setting $secrets 'DATABASE_URL')
$authUrl = [uri]$env:SUPABASE_URL
if ($database.Scheme -notin @('postgres', 'postgresql') -or
    $database.Host -ne "db.$($authUrl.Host)" -or $authUrl.Scheme -ne 'https' -or
    $authUrl.Host -notmatch '^[a-z0-9]+\.supabase\.co$') {
    throw 'The database and authentication URLs must belong to the same Supabase project.'
}
$credentials = $database.UserInfo.Split(':', 2)
if ($credentials.Length -ne 2) { throw 'Database username or password is missing.' }
$env:SUPABASE_DB_USERNAME = [uri]::UnescapeDataString($credentials[0])
$env:SUPABASE_DB_PASSWORD = [uri]::UnescapeDataString($credentials[1])
$caPath = Join-Path $root 'prod-ca-2021.crt'
if (-not (Test-Path -LiteralPath $caPath -PathType Leaf)) { throw 'Supabase database CA certificate is missing.' }
$rootCert = [uri]::EscapeDataString($caPath)
$dbName = $database.AbsolutePath.TrimStart('/')
if ($dbName -notmatch '^[A-Za-z0-9_]+$') { throw 'Unsupported database name.' }
$port = if ($database.IsDefaultPort) { 5432 } else { $database.Port }
$env:SUPABASE_DB_URL = "jdbc:postgresql://$($database.Host):$port/$($dbName)?sslmode=verify-full&sslrootcert=$rootCert&currentSchema=ledgerly"
$env:SUPABASE_SERVICE_ROLE_KEY = Read-Setting $secrets 'SUPABASE_SERVICE_ROLE_KEY'
$env:SUPABASE_STORAGE_BUCKET = 'ledgerly-documents'
$env:SPRING_FLYWAY_SCHEMAS = 'ledgerly'
$env:SPRING_FLYWAY_DEFAULT_SCHEMA = 'ledgerly'
$env:SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA = 'ledgerly'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '8094'
$ocr = [regex]::Match($local, '(?im)^\s*OCR_TESSERACT_COMMAND\s*=\s*["'']?([^\r\n"'']+)')
if ($ocr.Success) { $env:OCR_TESSERACT_COMMAND = $ocr.Groups[1].Value.Trim() }
if ($ReceiptAi) {
    $env:RECEIPT_AI_ENABLED = 'true'
    $env:RECEIPT_AI_PORT = '11435'
    $env:RECEIPT_AI_MODEL = 'qwen2.5vl:3b'
}

if ($ConfigureEclipse) {
    $configPath = Join-Path $root '.env.supabase'
    if (Test-Path -LiteralPath $configPath) {
        throw 'The local .env.supabase already exists; remove or update it explicitly before regenerating it.'
    }
    $settings = [ordered]@{
        SUPABASE_URL = $env:SUPABASE_URL
        SUPABASE_PUBLISHABLE_KEY = $env:SUPABASE_PUBLISHABLE_KEY
        SUPABASE_DB_URL = $env:SUPABASE_DB_URL
        SUPABASE_DB_USERNAME = $env:SUPABASE_DB_USERNAME
        SUPABASE_DB_PASSWORD = $env:SUPABASE_DB_PASSWORD
        SUPABASE_SERVICE_ROLE_KEY = $env:SUPABASE_SERVICE_ROLE_KEY
        SUPABASE_STORAGE_BUCKET = $env:SUPABASE_STORAGE_BUCKET
    }
    $lines = foreach ($entry in $settings.GetEnumerator()) {
        if ($entry.Value.Contains('${') -or $entry.Value.Contains("`n") -or $entry.Value.Contains("`r")) {
            throw "Setting $($entry.Key) cannot safely be written to Spring properties."
        }
        $escaped = $entry.Value.Replace('\', '\\')
        "$($entry.Key)=$escaped"
    }
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl = [System.Security.AccessControl.FileSecurity]::new()
    $acl.SetOwner($identity)
    $acl.SetAccessRuleProtection($true, $false)
    $rule = [System.Security.AccessControl.FileSystemAccessRule]::new(
        $identity, 'FullControl', 'Allow')
    $acl.AddAccessRule($rule)
    $created = $false
    try {
        $stream = [System.IO.File]::Open($configPath, [System.IO.FileMode]::CreateNew,
            [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
        $created = $true
        try {
            Set-Acl -LiteralPath $configPath -AclObject $acl
            $writer = [System.IO.StreamWriter]::new($stream, [System.Text.UTF8Encoding]::new($false))
            try { foreach ($line in $lines) { $writer.WriteLine($line) } }
            finally { $writer.Dispose() }
        } finally { $stream.Dispose() }
    } catch {
        if ($created) { Remove-Item -LiteralPath $configPath -Force }
        throw
    }
    Write-Host 'Created a user-restricted local Supabase configuration for Eclipse.'
    return
}

Set-Location (Join-Path $root 'backend')
& .\mvnw.cmd spring-boot:run
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

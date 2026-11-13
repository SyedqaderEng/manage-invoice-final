param([switch] $ReceiptAi, [switch] $ConfigureEclipse)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Read-Setting([string] $text, [string] $name) {
    $match = [regex]::Match($text, '(?im)^\s*' + [regex]::Escape($name) + '\s*=\s*["'']?([^\r\n"'']+)')
    if (-not $match.Success) { throw "Required setting $name is missing." }
    return $match.Groups[1].Value.Trim()
}

$local = [System.IO.File]::ReadAllText((Join-Path $root '.env'))
$env:SUPABASE_URL = Read-Setting $local 'SUPABASE_URL'
$env:SUPABASE_PUBLISHABLE_KEY = Read-Setting $local 'SUPABASE_PUBLISHABLE_KEY'
$databaseUrl = Read-Setting $local 'SUPABASE_DB_URL'
$env:SUPABASE_DB_USERNAME = Read-Setting $local 'SUPABASE_DB_USERNAME'
$env:SUPABASE_DB_PASSWORD = Read-Setting $local 'SUPABASE_DB_PASSWORD'
$authUrl = [uri]$env:SUPABASE_URL
if ($authUrl.Scheme -ne 'https' -or
    $authUrl.Host -notmatch '^[a-z0-9]+\.supabase\.co$') {
    throw 'The Supabase authentication URL is invalid.'
}
$database = [regex]::Match($databaseUrl, '^jdbc:postgresql://(?<host>[^/:?]+)(?::(?<port>\d+))?/(?<name>[A-Za-z0-9_]+)\?(?<query>.+)$')
if (-not $database.Success) {
    throw 'The Supabase database JDBC URL is invalid.'
}
$projectRef = $authUrl.Host.Split('.')[0]
$databaseHost = $database.Groups['host'].Value
$directConnection = $databaseHost -eq "db.$($authUrl.Host)"
$poolerConnection = $databaseHost.EndsWith('.pooler.supabase.com') -and
    $env:SUPABASE_DB_USERNAME -eq "postgres.$projectRef"
if (-not $directConnection -and -not $poolerConnection) {
    throw 'The database and authentication URLs must belong to the same Supabase project.'
}
if ($database.Groups['query'].Value -notmatch '(?:^|&)sslmode=verify-full(?:&|$)') {
    throw 'The Supabase database URL must verify the server certificate and hostname.'
}
$rootCertMatch = [regex]::Match($database.Groups['query'].Value, '(?:^|&)sslrootcert=([^&]+)')
if (-not $rootCertMatch.Success) { throw 'The Supabase database CA certificate is not configured.' }
$caPath = [uri]::UnescapeDataString($rootCertMatch.Groups[1].Value)
if (-not (Test-Path -LiteralPath $caPath -PathType Leaf)) { throw 'Supabase database CA certificate is missing.' }
$env:SUPABASE_DB_URL = $databaseUrl
$env:AWS_REGION = 'us-east-2'
$env:AWS_S3_BUCKET = 'manage-recipt-s3-365513553621-us-east-2-an'
if ([string]::IsNullOrWhiteSpace($env:AWS_PROFILE)) { $env:AWS_PROFILE = 'manage-invoice' }
$env:SPRING_FLYWAY_SCHEMAS = 'manage-receipt'
$env:SPRING_FLYWAY_DEFAULT_SCHEMA = 'manage-receipt'
$env:SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA = '"manage-receipt"'
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
        AWS_REGION = $env:AWS_REGION
        AWS_S3_BUCKET = $env:AWS_S3_BUCKET
        AWS_PROFILE = $env:AWS_PROFILE
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
    Write-Host 'Created a user-restricted local application configuration for Eclipse.'
    return
}

Set-Location (Join-Path $root 'backend')
& .\mvnw.cmd spring-boot:run
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

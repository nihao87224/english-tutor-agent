param(
    [string] $DbHost = $env:DB_HOST,
    [int] $DbPort = $(if ($env:DB_PORT) { [int] $env:DB_PORT } else { 3306 }),
    [string] $RedisHost = $env:REDIS_HOST,
    [int] $RedisPort = $(if ($env:REDIS_PORT) { [int] $env:REDIS_PORT } else { 6379 }),
    [string] $RedisPassword = $env:REDIS_PASSWORD,
    [string] $S3Endpoint = $env:S3_ENDPOINT,
    [string] $S3Region = $env:S3_REGION,
    [string] $S3Bucket = $env:S3_BUCKET,
    [string] $S3AccessKey = $env:S3_ACCESS_KEY,
    [string] $S3SecretKey = $env:S3_SECRET_KEY,
    [string] $S3PublicBaseUrl = $env:S3_PUBLIC_BASE_URL,
    [switch] $SkipRedisAuth,
    [switch] $SkipObjectStorage
)

$ErrorActionPreference = "Stop"

function Assert-NonBlank {
    param(
        [string] $Value,
        [string] $Name
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required"
    }
}

function Test-TcpEndpoint {
    param(
        [string] $HostName,
        [int] $Port,
        [string] $Name
    )

    Assert-NonBlank $HostName "$Name host"
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connect = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $connect.AsyncWaitHandle.WaitOne([TimeSpan]::FromSeconds(5))) {
            throw "$Name TCP check timed out"
        }
        $client.EndConnect($connect)
        Write-Host "OK $Name TCP endpoint is reachable"
    }
    finally {
        $client.Dispose()
    }
}

function Send-RedisCommand {
    param(
        [System.IO.Stream] $Stream,
        [string[]] $Parts
    )

    $command = "*" + $Parts.Count + "`r`n"
    foreach ($part in $Parts) {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($part)
        $command += "$" + $bytes.Length + "`r`n" + $part + "`r`n"
    }

    $payload = [System.Text.Encoding]::UTF8.GetBytes($command)
    $Stream.Write($payload, 0, $payload.Length)

    $buffer = New-Object byte[] 512
    $read = $Stream.Read($buffer, 0, $buffer.Length)
    return [System.Text.Encoding]::UTF8.GetString($buffer, 0, $read)
}

function Test-RedisPing {
    param(
        [string] $HostName,
        [int] $Port,
        [string] $Password
    )

    Assert-NonBlank $HostName "Redis host"
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $client.Connect($HostName, $Port)
        $stream = $client.GetStream()
        $stream.ReadTimeout = 5000
        $stream.WriteTimeout = 5000

        if (-not [string]::IsNullOrWhiteSpace($Password)) {
            $auth = Send-RedisCommand $stream @("AUTH", $Password)
            if (-not $auth.StartsWith("+OK")) {
                throw "Redis AUTH failed"
            }
        }

        $pong = Send-RedisCommand $stream @("PING")
        if (-not $pong.StartsWith("+PONG")) {
            throw "Redis PING failed"
        }
        Write-Host "OK Redis AUTH/PING check passed"
    }
    finally {
        $client.Dispose()
    }
}

function Test-S3Endpoint {
    param(
        [string] $Endpoint,
        [string] $Region,
        [string] $Bucket,
        [string] $AccessKey,
        [string] $SecretKey,
        [string] $PublicBaseUrl
    )

    Assert-NonBlank $Endpoint "S3_ENDPOINT"
    Assert-NonBlank $Region "S3_REGION"
    Assert-NonBlank $Bucket "S3_BUCKET"
    Assert-NonBlank $AccessKey "S3_ACCESS_KEY"
    Assert-NonBlank $SecretKey "S3_SECRET_KEY"
    Assert-NonBlank $PublicBaseUrl "S3_PUBLIC_BASE_URL"

    $uri = [Uri] $Endpoint
    $port = if ($uri.IsDefaultPort) {
        if ($uri.Scheme -eq "https") { 443 } else { 80 }
    }
    else {
        $uri.Port
    }

    Test-TcpEndpoint $uri.Host $port "S3"
    Write-Host "OK S3 required configuration is present"
    Write-Host "INFO S3 bucket authorization requires a signed SDK request"
}

Test-TcpEndpoint $DbHost $DbPort "MySQL"

if ($SkipRedisAuth) {
    Test-TcpEndpoint $RedisHost $RedisPort "Redis"
    Write-Host "WARN Redis AUTH/PING check skipped"
}
else {
    Test-RedisPing $RedisHost $RedisPort $RedisPassword
}

if ($SkipObjectStorage) {
    Write-Host "WARN object storage check skipped"
}
else {
    Test-S3Endpoint $S3Endpoint $S3Region $S3Bucket $S3AccessKey $S3SecretKey $S3PublicBaseUrl
}

Write-Host "External infrastructure check completed"

param(
    [Parameter(Mandatory = $true)] [string] $BaseUrl,
    [Parameter(Mandatory = $true)] [string] $AccessToken,
    [ValidateSet('import', 'publish', 'unpublish', 'disable', 'grant', 'revoke')] [string] $Action,
    [string] $ManifestPath,
    [string] $ResourceKey,
    [string] $SemanticVersion,
    [string] $ManifestHash,
    [string] $UserKey,
    [string] $CollectionKey,
    [string] $Reason,
    [datetime] $ExpiresAt
)

$headers = @{ Authorization = "Bearer $AccessToken" }
switch ($Action) {
    'import' {
        if (-not $ManifestPath) { throw 'ManifestPath is required for import.' }
        $body = @{ manifestJson = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $ManifestPath)) } | ConvertTo-Json -Compress
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/admin/content-imports" -Headers $headers -ContentType 'application/json' -Body $body
    }
    { $_ -in 'publish', 'unpublish', 'disable' } {
        if (-not $ResourceKey -or -not $SemanticVersion -or -not $ManifestHash) { throw 'ResourceKey, SemanticVersion and ManifestHash are required.' }
        $headers['If-Match'] = $ManifestHash
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/admin/learning-resources/$ResourceKey/versions/$SemanticVersion/$Action" -Headers $headers
    }
    { $_ -in 'grant', 'revoke' } {
        if (-not $UserKey -or -not $CollectionKey) { throw 'UserKey and CollectionKey are required.' }
        $body = @{ userKey = $UserKey; collectionKey = $CollectionKey; reason = $Reason; expiresAt = if ($ExpiresAt) { $ExpiresAt.ToUniversalTime().ToString('o') } else { $null } } | ConvertTo-Json -Compress
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/admin/entitlements/$Action" -Headers $headers -ContentType 'application/json' -Body $body
    }
}

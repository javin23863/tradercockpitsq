[CmdletBinding()]
param(
    [string]$SourceDir,
    [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$authorityRoot = Join-Path $repoRoot 'references/ui-authority'
$manifestPath = Join-Path $authorityRoot 'manifest.json'
$destinationRoot = Join-Path $authorityRoot 'screenshots'

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "UI authority manifest not found: $manifestPath"
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

function Assert-CanonicalAsset {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Asset
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing canonical UI asset: $Path"
    }

    $file = Get-Item -LiteralPath $Path
    if ([int64]$file.Length -ne [int64]$Asset.bytes) {
        throw "Byte-count mismatch for $($Asset.name): expected $($Asset.bytes), got $($file.Length)"
    }

    $actualSha = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    $expectedSha = ([string]$Asset.sha256).ToLowerInvariant()
    if ($actualSha -ne $expectedSha) {
        throw "SHA-256 mismatch for $($Asset.name): expected $expectedSha, got $actualSha"
    }
}

if ([string]::IsNullOrWhiteSpace($SourceDir)) {
    $inputRoot = $destinationRoot
    $mode = 'verify repository copy'
} else {
    $resolved = Resolve-Path -LiteralPath $SourceDir
    $inputRoot = $resolved.Path
    $mode = if ($VerifyOnly) { 'verify recovered source' } else { 'import recovered source' }
}

foreach ($asset in $manifest.canonical_assets) {
    $sourcePath = Join-Path $inputRoot ([string]$asset.name)
    Assert-CanonicalAsset -Path $sourcePath -Asset $asset

    if (-not [string]::IsNullOrWhiteSpace($SourceDir) -and -not $VerifyOnly) {
        New-Item -ItemType Directory -Path $destinationRoot -Force | Out-Null
        $destinationPath = Join-Path $destinationRoot ([string]$asset.name)
        Copy-Item -LiteralPath $sourcePath -Destination $destinationPath -Force
        Assert-CanonicalAsset -Path $destinationPath -Asset $asset
    }
}

$assetCount = @($manifest.canonical_assets).Count
Write-Host "PASS: $mode — $assetCount canonical TraderCockpit UI assets match manifest byte counts and SHA-256 digests."

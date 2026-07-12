# scripts/pseudocode/New-FileManifest.ps1
param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
  [string]$OutFile = (Join-Path $RepoRoot 'docs\pseudocode\_index\file-manifest.md')
)

$ErrorActionPreference = 'Stop'
$exts = @('*.cs','*.ts','*.tsx','*.kt','*.js')
$exclude = '\\(node_modules|bin|obj|dist|publish|\.gradle|build)\\|\\wwwroot\\'
$files = @()
foreach ($r in @('src','tests')) {
  $path = Join-Path $RepoRoot $r
  if (Test-Path $path) {
    $files += Get-ChildItem -Path $path -Recurse -File -Include $exts |
      Where-Object { $_.FullName -notmatch $exclude }
  }
}
$rels = $files |
  ForEach-Object { ($_.FullName.Substring($RepoRoot.Length + 1) -replace '\\','/') } |
  Sort-Object -Unique

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine('# File Manifest')
[void]$sb.AppendLine('')
[void]$sb.AppendLine("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
[void]$sb.AppendLine("Total: $($rels.Count)")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('| Status | Source | Doc |')
[void]$sb.AppendLine('| --- | --- | --- |')
foreach ($rel in $rels) {
  $doc = "docs/pseudocode/files/$rel.md"
  $done = Test-Path (Join-Path $RepoRoot ($doc -replace '/','\'))
  $status = if ($done) { 'x' } else { ' ' }
  [void]$sb.AppendLine("| [$status] | `$rel` | `$doc` |".Replace('$rel', $rel).Replace('$doc', $doc))
}

$dir = Split-Path $OutFile -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
Set-Content -Path $OutFile -Value $sb.ToString() -Encoding UTF8
Write-Host "Wrote $($rels.Count) entries -> $OutFile"

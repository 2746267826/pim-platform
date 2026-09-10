# scripts/pseudocode/Merge-Coverage.ps1
param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
)

$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'New-FileManifest.ps1') -RepoRoot $RepoRoot

$manifest = Join-Path $RepoRoot 'docs\pseudocode\_index\file-manifest.md'
$lines = Get-Content $manifest
$total = 0; $done = 0
foreach ($line in $lines) {
  if ($line -match '^\| \[([ x])\] \|') {
    $total++
    if ($Matches[1] -eq 'x') { $done++ }
  }
}
$pct = if ($total -eq 0) { 0 } else { [math]::Round(100.0 * $done / $total, 2) }
$coverage = @"
# Coverage

- Updated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
- Done: $done / $total ($pct%)
- Next: run ``scripts/pseudocode/Split-WaveAssignments.ps1`` then launch 10 subagents

## Rules
- Only mark done when dual-granularity doc exists for the source file.
- Orchestrator merges after each 10-agent wave.
"@
Set-Content -Path (Join-Path $RepoRoot 'docs\pseudocode\_index\coverage.md') -Value $coverage -Encoding UTF8
Write-Host "Coverage $done/$total ($pct%)"

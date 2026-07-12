# scripts/pseudocode/Split-WaveAssignments.ps1
param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
  [int]$WaveSizePerSlot = 8,
  [string]$OutFile = (Join-Path $RepoRoot 'docs\pseudocode\_index\wave-assignments.json')
)

$ErrorActionPreference = 'Stop'
$manifest = Join-Path $RepoRoot 'docs\pseudocode\_index\file-manifest.md'
if (-not (Test-Path $manifest)) { throw "manifest missing: $manifest" }

$pending = @()
Get-Content $manifest | ForEach-Object {
  if ($_ -match '^\| \[ \] \| `([^`]+)` \|') { $pending += $Matches[1] }
}

$take = [Math]::Min($pending.Count, 10 * $WaveSizePerSlot)
$slice = $pending | Select-Object -First $take
$slots = @{}
for ($i = 0; $i -lt 10; $i++) { $slots["A$($i+1)"] = @() }
for ($i = 0; $i -lt $slice.Count; $i++) {
  $slot = "A$(($i % 10) + 1)"
  $slots[$slot] += $slice[$i]
}

$payload = [ordered]@{
  generated = (Get-Date -Format 'o')
  pendingTotal = $pending.Count
  assignedTotal = $slice.Count
  slots = $slots
}
$json = $payload | ConvertTo-Json -Depth 6
Set-Content -Path $OutFile -Value $json -Encoding UTF8
Write-Host "Assigned $($slice.Count) / pending $($pending.Count) -> $OutFile"
if ($pending.Count -gt 0 -and $slice.Count -lt 10 -and $pending.Count -ge 10) {
  throw 'Invariant broken: had >=10 pending but assigned <10 files total'
}

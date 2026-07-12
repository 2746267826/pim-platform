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
  # Accept paths with or without surrounding backticks
  if ($_ -match '^\| \[ \] \| `?([^`|]+?)`? \|') { $pending += $Matches[1].Trim() }
}

if ($pending.Count -eq 0) {
  $sample = Get-Content $manifest | Select-Object -Skip 6 -First 3
  throw "No pending rows parsed from manifest. Check Status/path format. Sample:`n$($sample -join "`n")"
}

function Get-PriorityScore([string]$p) {
  if ($p -like 'src/Pim.Core/*') { return 0 }
  if ($p -like 'src/Pim.Infrastructure/*') { return 1 }
  if ($p -like 'src/Pim.Api/*') { return 2 }
  if ($p -like 'src/modules/*') { return 3 }
  if ($p -like 'src/client-windows/*') { return 4 }
  if ($p -like 'src/client-web/*') { return 5 }
  if ($p -like 'src/client-android/*') { return 6 }
  if ($p -like 'tests/*') { return 7 }
  return 8
}

$ordered = @($pending | Sort-Object @{ Expression = { Get-PriorityScore $_ } }, @{ Expression = { $_ } })
$take = [Math]::Min($ordered.Count, 10 * $WaveSizePerSlot)
$slice = @($ordered | Select-Object -First $take)
$slots = @{}
for ($i = 0; $i -lt 10; $i++) { $slots["A$($i+1)"] = @() }
for ($i = 0; $i -lt $slice.Count; $i++) {
  $slot = "A$(($i % 10) + 1)"
  $slots[$slot] += $slice[$i]
}

$assignedTotal = $slice.Count
if ($pending.Count -ge 10 -and $assignedTotal -eq 0) {
  throw "Invariant broken: pending=$($pending.Count) but assignedTotal=0"
}
if ($pending.Count -gt 0 -and $take -gt 0 -and $assignedTotal -eq 0) {
  throw "Invariant broken: take=$take but all slots empty after assign"
}
$nonEmptySlots = @($slots.Keys | Where-Object { @($slots[$_]).Count -gt 0 }).Count
if ($take -gt 0 -and $nonEmptySlots -eq 0) {
  throw "Invariant broken: take=$take but every slot is empty"
}

$payload = [ordered]@{
  generated = (Get-Date -Format 'o')
  pendingTotal = $pending.Count
  assignedTotal = $assignedTotal
  slots = $slots
}
$json = $payload | ConvertTo-Json -Depth 6
Set-Content -Path $OutFile -Value $json -Encoding UTF8
Write-Host "Assigned $assignedTotal / pending $($pending.Count) -> $OutFile"

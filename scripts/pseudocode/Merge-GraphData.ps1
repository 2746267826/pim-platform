# scripts/pseudocode/Merge-GraphData.ps1
param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
  [string]$EdgesDir = (Join-Path $RepoRoot 'docs\pseudocode\_index\edge-fragments')
)

$ErrorActionPreference = 'Stop'
$out = Join-Path $RepoRoot 'docs\pseudocode\graphs\interactive\graph-data.json'
$nodes = @{}
$edges = New-Object System.Collections.Generic.List[object]

function Import-Fragment($path) {
  try {
    $txt = [System.IO.File]::ReadAllText($path)
    if ($txt.Length -gt 0 -and [int][char]$txt[0] -eq 0xFEFF) { $txt = $txt.Substring(1) }
    $raw = $txt | ConvertFrom-Json -ErrorAction Stop
  } catch {
    Write-Warning "Skip invalid edge fragment: $path ($($_.Exception.Message))"
    return
  }
  foreach ($n in @($raw.nodes)) {
    if ($null -ne $n -and $n.id) { $nodes[$n.id] = $n }
  }
  foreach ($e in @($raw.edges)) {
    if ($null -ne $e -and $e.from -and $e.to) { $edges.Add($e) }
  }
}

# Prefer explicit fragments under docs/pseudocode/_index/edge-fragments/*.json
# (subagents write per-slot merge files; missing dir is created empty)
if (-not (Test-Path $EdgesDir)) {
  New-Item -ItemType Directory -Path $EdgesDir -Force | Out-Null
}
Get-ChildItem $EdgesDir -Filter *.json -ErrorAction SilentlyContinue | ForEach-Object {
  Import-Fragment $_.FullName
}

# Ensure every completed doc path has at least a node
$filesRoot = Join-Path $RepoRoot 'docs\pseudocode\files'
if (Test-Path $filesRoot) {
  Get-ChildItem $filesRoot -Recurse -Filter *.md | ForEach-Object {
    $relDoc = $_.FullName.Substring($RepoRoot.Length + 1) -replace '\\','/'
    $src = $relDoc -replace '^docs/pseudocode/files/','' -replace '\.md$',''
    if (-not $nodes.ContainsKey($src)) {
      $layer = 'other'
      if ($src -like 'src/Pim.Core/*') { $layer = 'core' }
      elseif ($src -like 'src/Pim.Infrastructure/*') { $layer = 'infrastructure' }
      elseif ($src -like 'src/Pim.Api/*') { $layer = 'api' }
      elseif ($src -like 'src/modules/Pim.Module.Stats/*') { $layer = 'module.stats' }
      elseif ($src -like 'src/modules/Pim.Module.QuickNotes/*') { $layer = 'module.quicknotes' }
      elseif ($src -like 'src/modules/Pim.Module.Files/*') { $layer = 'module.files' }
      elseif ($src -like 'src/modules/Pim.Module.Mobile/*') { $layer = 'module.mobile' }
      elseif ($src -like 'src/modules/Pim.Module.PcTracker/*') { $layer = 'module.pctracker' }
      elseif ($src -like 'src/modules/Pim.Module.Calendar/*') { $layer = 'module.calendar' }
      elseif ($src -like 'src/client-web/*') { $layer = 'client-web' }
      elseif ($src -like 'src/client-windows/*') { $layer = 'client-windows' }
      elseif ($src -like 'src/client-android/*') { $layer = 'client-android' }
      elseif ($src -like 'tests/*') { $layer = 'tests' }
      $nodes[$src] = [pscustomobject]@{
        id = $src
        label = Split-Path $src -Leaf
        path = $src
        doc = $relDoc
        layer = $layer
        kind = $(if ($layer -eq 'tests') { 'test' } else { 'other' })
      }
    }
  }
}

$result = [ordered]@{
  nodes = @($nodes.Values | Sort-Object id)
  edges = @($edges | Sort-Object from, to, type)
}
$dir = Split-Path $out -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
($result | ConvertTo-Json -Depth 8) | Set-Content -Path $out -Encoding UTF8
Write-Host "graph-data nodes=$($result.nodes.Count) edges=$($result.edges.Count)"

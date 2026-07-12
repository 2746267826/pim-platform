# Version, Icons, and GitHub Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify CalVer versioning across API/Web/Windows/Android, ship the locked four-color mosaic P icon to all clients, and orchestrate CI so every successful master build publishes a versioned GitHub Release with same-keystore Android PR/master signing.

**Architecture:** One `scripts/ci/resolve-version.sh` + composite action produces shared version outputs. `.github/workflows/ci.yml` orchestrates four reusable `workflow_call` build workflows, injects the same version into each surface, and creates `vYYYY.MM.N` releases only when all four master builds succeed. Branding source of truth is `branding/pim-mark.svg` (byte-locked to the approved mark); an export script materializes Web/Windows/Android icon derivatives.

**Tech Stack:** Bash, GitHub Actions (`workflow_call`, `softprops/action-gh-release`), .NET 8 MSBuild props, Vite/`VITE_APP_VERSION`, Android Gradle env version/signing, Node (icon export), xUnit/shell assertions.

**Spec:** `docs/superpowers/specs/2026-07-12-version-icons-github-actions-design.md`  
**Locked mark:** `docs/superpowers/specs/attachments/2026-07-12-version-icons/pim-mark-locked.svg`

---

## File Map

| Path | Responsibility |
|------|----------------|
| `branding/pim-mark.svg` | Canonical mark (copy of locked SVG) |
| `branding/README.md` | Colors, export command, do-not-hand-edit derivatives |
| `scripts/branding/export-icons.mjs` | Export favicon/ico/png/mipmap from mark |
| `scripts/branding/package.json` | Minimal deps for export (`sharp`, `to-ico` or equivalent) |
| `scripts/ci/resolve-version.sh` | Single version algorithm |
| `scripts/ci/resolve-version.ps1` | Thin Windows wrapper calling bash script |
| `scripts/ci/test-resolve-version.sh` | Deterministic fixture tests for version script |
| `.github/actions/resolve-version/action.yml` | Composite action wrapping the script |
| `.github/workflows/ci.yml` | Orchestrator: resolve → 4 builds → release |
| `.github/workflows/build-api.yml` | Reusable API build (`workflow_call` + dispatch) |
| `.github/workflows/build-web.yml` | Reusable web build |
| `.github/workflows/build-windows.yml` | Reusable Windows build |
| `.github/workflows/build-android.yml` | Reusable Android build (same signing PR/master) |
| `src/Directory.Build.props` | Local defaults + CI-overridable InformationalVersion |
| `src/client-windows/Pim.Client.App/Pim.Client.App.csproj` | ApplicationIcon + embed `app.ico` |
| `src/client-windows/Pim.Client.App/app.ico` | Generated tray/app icon |
| `src/client-web/public/favicon.svg` | Web favicon |
| `src/client-web/public/favicon.ico` | Web favicon multi-size |
| `src/client-web/public/apple-touch-icon.png` | Optional 180×180 |
| `src/client-web/src/vite-env.d.ts` | `__APP_VERSION__` type |
| `src/client-web/src/layout/Sidebar.tsx` | Minimal version display |
| `src/client-android/app/src/main/AndroidManifest.xml` | `android:icon` / `roundIcon` |
| `src/client-android/app/src/main/res/mipmap-*/ic_launcher*.png` | Launcher icons |
| `src/client-android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon if used |
| `tests/Pim.UnitTests/Versioning/DirectoryBuildPropsVersionTests.cs` | Optional MSBuild/info version sanity (or script-only if simpler) |

---

### Task 0: Sync Branch With Master

**Files:** none (git only)

- [ ] **Step 1: Rebase or merge latest `origin/master`**

```bash
git fetch --all --prune
git checkout codex/version-icons-github-actions
git merge origin/master
# resolve conflicts if any; keep design/plan commits
```

Expected: branch contains design commits + current master; `git status` clean except intentional untracked noise.

- [ ] **Step 2: Commit merge if needed**

```bash
git status --short --branch
# if merge commit created, leave it; do not rewrite design history
```

---

### Task 1: Lock Branding Source Assets

**Files:**
- Create: `branding/pim-mark.svg`
- Create: `branding/README.md`

- [ ] **Step 1: Copy locked SVG to branding (exact geometry)**

Copy from `docs/superpowers/specs/attachments/2026-07-12-version-icons/pim-mark-locked.svg` to `branding/pim-mark.svg`. Content must be:

```svg
<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="0 0 72 72" fill="none">
  <!-- PIM mark: four-color mosaic P (locked 2026-07-12) -->
  <rect x="10" y="8" width="18" height="56" rx="4" fill="#00a4ef"/>
  <rect x="28" y="8" width="30" height="18" rx="4" fill="#f25022"/>
  <rect x="28" y="28" width="26" height="16" rx="4" fill="#7fba00"/>
  <circle cx="52" cy="52" r="10" fill="#ffb900"/>
</svg>
```

- [ ] **Step 2: Write `branding/README.md`**

```markdown
# PIM Branding

## Canonical mark

- File: `pim-mark.svg`
- Design: four-color mosaic **P** (approved 2026-07-12)
- Colors: `#f25022` `#7fba00` `#00a4ef` `#ffb900`
- Spec: `docs/superpowers/specs/2026-07-12-version-icons-github-actions-design.md`

Do not hand-edit derived icons under `src/client-web/public/`, `src/client-windows/**/app.ico`, or Android `mipmap-*`. Regenerate with:

```bash
node scripts/branding/export-icons.mjs
```

After changing `pim-mark.svg`, run export and commit both source and derivatives.
```

- [ ] **Step 3: Commit**

```bash
git add branding/pim-mark.svg branding/README.md
git commit -m "feat: add locked PIM brand mark source"
```

---

### Task 2: Icon Export Script And Derivatives

**Files:**
- Create: `scripts/branding/package.json`
- Create: `scripts/branding/export-icons.mjs`
- Create: `src/client-web/public/favicon.svg`
- Create: `src/client-web/public/favicon.ico`
- Create: `src/client-web/public/apple-touch-icon.png`
- Create: `src/client-windows/Pim.Client.App/app.ico`
- Create: Android mipmap PNGs under `src/client-android/app/src/main/res/`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Modify: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`

- [ ] **Step 1: Add export package manifest**

`scripts/branding/package.json`:

```json
{
  "name": "pim-branding-export",
  "private": true,
  "type": "module",
  "dependencies": {
    "sharp": "^0.33.5",
    "to-ico": "^1.1.5"
  }
}
```

- [ ] **Step 2: Implement `export-icons.mjs`**

Script requirements:

1. Read `branding/pim-mark.svg`.
2. Write identical SVG to `src/client-web/public/favicon.svg`.
3. Rasterize to PNG sizes: 16, 32, 48, 64, 128, 180, 192, 256, 512 on **white** background for platform icons that need it; keep transparency only where adaptive foreground expects it.
4. Build `favicon.ico` from 16/32/48.
5. Write `apple-touch-icon.png` 180×180 (white bg).
6. Write `src/client-windows/Pim.Client.App/app.ico` from 16/32/48/256.
7. Write Android:
   - `mipmap-mdpi/ic_launcher.png` 48
   - `mipmap-hdpi/ic_launcher.png` 72
   - `mipmap-xhdpi/ic_launcher.png` 96
   - `mipmap-xxhdpi/ic_launcher.png` 144
   - `mipmap-xxxhdpi/ic_launcher.png` 192
   - same sizes for `ic_launcher_round.png` (can be same bitmap for v1)
8. Exit non-zero if source SVG missing required fill colors `#00a4ef|#f25022|#7fba00|#ffb900`.

Minimal implementation sketch:

```js
import fs from 'node:fs';
import path from 'node:path';
import sharp from 'sharp';
import toIco from 'to-ico';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const srcSvg = path.join(root, 'branding/pim-mark.svg');
const svg = fs.readFileSync(srcSvg, 'utf8');
for (const c of ['#00a4ef', '#f25022', '#7fba00', '#ffb900']) {
  if (!svg.includes(c)) throw new Error(`mark missing color ${c}`);
}

async function png(size, out, { whiteBg = true } = {}) {
  let img = sharp(Buffer.from(svg)).resize(size, size);
  if (whiteBg) {
    img = sharp({
      create: { width: size, height: size, channels: 3, background: '#ffffff' }
    }).composite([{ input: await sharp(Buffer.from(svg)).resize(size, size).png().toBuffer() }]);
  }
  await img.png().toFile(out);
}

// ... write favicon.svg, ico buffers via toIco, android mipmaps, app.ico
console.log('export-icons: ok');
```

Fill in all output paths completely when implementing; do not leave stubs.

- [ ] **Step 3: Install and run export**

```bash
cd scripts/branding
npm install
node export-icons.mjs
```

Expected: all target files exist; no error.

- [ ] **Step 4: Wire Windows csproj**

In `Pim.Client.App.csproj` add inside first `PropertyGroup`:

```xml
<ApplicationIcon>app.ico</ApplicationIcon>
```

And item group:

```xml
<ItemGroup>
  <Resource Include="app.ico" />
</ItemGroup>
```

- [ ] **Step 5: Wire AndroidManifest icons**

On `<application ...>` add:

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```

- [ ] **Step 6: Smoke-check files**

```bash
# PowerShell
Test-Path branding/pim-mark.svg
Test-Path src/client-web/public/favicon.svg
Test-Path src/client-web/public/favicon.ico
Test-Path src/client-windows/Pim.Client.App/app.ico
Test-Path src/client-android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png
```

Expected: all `True`.

- [ ] **Step 7: Commit**

```bash
git add branding scripts/branding src/client-web/public src/client-windows/Pim.Client.App/app.ico src/client-windows/Pim.Client.App/Pim.Client.App.csproj src/client-android/app/src/main/res src/client-android/app/src/main/AndroidManifest.xml
git commit -m "feat: export brand mark to web windows android icons"
```

Note: commit `scripts/branding/package-lock.json`; do **not** commit `node_modules`.

---

### Task 3: resolve-version Script (TDD)

**Files:**
- Create: `scripts/ci/resolve-version.sh`
- Create: `scripts/ci/resolve-version.ps1`
- Create: `scripts/ci/test-resolve-version.sh`

- [ ] **Step 1: Write failing tests first**

`scripts/ci/test-resolve-version.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT="$ROOT/scripts/ci/resolve-version.sh"
fail=0

run_case() {
  local name="$1"; shift
  local out
  out="$( "$@" )"
  echo "$out" > "/tmp/rv-$name.env"
}

assert_eq() {
  local key="$1" expected="$2" file="$3"
  local actual
  actual="$(grep "^${key}=" "$file" | cut -d= -f2-)"
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL $file $key: expected [$expected] got [$actual]"
    fail=1
  else
    echo "OK $key=$actual"
  fi
}

# master
run_case master \
  env GITHUB_REF=refs/heads/master GITHUB_RUN_NUMBER=42 GITHUB_SHA=abcdef1234567890 \
      GITHUB_EVENT_NAME=push \
      bash "$SCRIPT" --date 2026-07-12 --print-env
assert_eq version "2026.07.42" /tmp/rv-master.env
assert_eq version_code "100042" /tmp/rv-master.env
assert_eq is_release "true" /tmp/rv-master.env
assert_eq git_sha_short "abcdef1" /tmp/rv-master.env
assert_eq year_month "2026.07" /tmp/rv-master.env
assert_eq artifact_slug "2026.07.42" /tmp/rv-master.env

# PR
run_case pr \
  env GITHUB_REF=refs/pull/12/merge GITHUB_RUN_NUMBER=42 GITHUB_SHA=abcdef1234567890 \
      GITHUB_EVENT_NAME=pull_request GITHUB_REF_NAME=12/merge \
      PR_NUMBER=12 \
      bash "$SCRIPT" --date 2026-07-12 --print-env
assert_eq version "2026.07.42-pr.12+abcdef1" /tmp/rv-pr.env
assert_eq version_code "100042" /tmp/rv-pr.env
assert_eq is_release "false" /tmp/rv-pr.env
assert_eq artifact_slug "2026.07.42-pr.12-abcdef1" /tmp/rv-pr.env

# dispatch non-master
run_case dev \
  env GITHUB_REF=refs/heads/codex/foo GITHUB_RUN_NUMBER=7 GITHUB_SHA=deadbeefcafebabe \
      GITHUB_EVENT_NAME=workflow_dispatch \
      bash "$SCRIPT" --date 2026-07-12 --print-env
assert_eq version "2026.07.7-dev+deadbee" /tmp/rv-dev.env
assert_eq is_release "false" /tmp/rv-dev.env

# client patch on master
run_case patch \
  env GITHUB_REF=refs/heads/master GITHUB_RUN_NUMBER=42 GITHUB_SHA=abcdef1234567890 \
      GITHUB_EVENT_NAME=workflow_dispatch CLIENT_PATCH=android.1 \
      bash "$SCRIPT" --date 2026-07-12 --print-env
assert_eq version "2026.07.42+android.1" /tmp/rv-patch.env
assert_eq is_release "true" /tmp/rv-patch.env

if [[ "$fail" -ne 0 ]]; then
  echo "resolve-version tests failed"
  exit 1
fi
echo "resolve-version tests passed"
```

- [ ] **Step 2: Run tests (expect fail — script missing)**

```bash
bash scripts/ci/test-resolve-version.sh
```

Expected: fail (script not found or wrong outputs).

- [ ] **Step 3: Implement `scripts/ci/resolve-version.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail

DATE_OVERRIDE=""
PRINT_ENV=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --date) DATE_OVERRIDE="$2"; shift 2 ;;
    --print-env) PRINT_ENV=true; shift ;;
    *) echo "Unknown arg: $1" >&2; exit 2 ;;
  esac
done

if [[ -n "$DATE_OVERRIDE" ]]; then
  YEAR="${DATE_OVERRIDE:0:4}"
  MONTH="${DATE_OVERRIDE:5:2}"
else
  YEAR="$(date -u +%Y)"
  MONTH="$(date -u +%m)"
fi

N="${GITHUB_RUN_NUMBER:-0}"
if ! [[ "$N" =~ ^[0-9]+$ ]] || [[ "$N" -lt 1 ]]; then
  echo "GITHUB_RUN_NUMBER must be positive integer, got: ${GITHUB_RUN_NUMBER-}" >&2
  exit 1
fi

SHA_FULL="${GITHUB_SHA:-0000000000000000000000000000000000000000}"
SHA7="${SHA_FULL:0:7}"
YEAR_MONTH="${YEAR}.${MONTH}"
BASE="${YEAR_MONTH}.${N}"
VERSION_CODE=$((100000 + N))

REF="${GITHUB_REF:-}"
EVENT="${GITHUB_EVENT_NAME:-}"
CLIENT_PATCH="${CLIENT_PATCH:-}"

is_release=false
version="$BASE"
if [[ "$REF" == "refs/heads/master" ]]; then
  is_release=true
  if [[ -n "$CLIENT_PATCH" ]]; then
    version="${BASE}+${CLIENT_PATCH}"
  fi
elif [[ "$EVENT" == "pull_request" ]]; then
  PR_NUMBER="${PR_NUMBER:-${GITHUB_PR_NUMBER:-}}"
  if [[ -z "$PR_NUMBER" && "$REF" =~ refs/pull/([0-9]+)/ ]]; then
    PR_NUMBER="${BASH_REMATCH[1]}"
  fi
  if [[ -z "$PR_NUMBER" ]]; then
    echo "PR_NUMBER required for pull_request" >&2
    exit 1
  fi
  version="${BASE}-pr.${PR_NUMBER}+${SHA7}"
else
  version="${BASE}-dev+${SHA7}"
fi

# filesystem-safe slug
artifact_slug="$(echo "$version" | sed 's/+/-/g')"

assembly_version="${YEAR}.$((10#$MONTH)).${N}.0"

if [[ "$PRINT_ENV" == true ]]; then
  cat <<EOF
version=$version
version_code=$VERSION_CODE
artifact_slug=$artifact_slug
git_sha_short=$SHA7
is_release=$is_release
year_month=$YEAR_MONTH
assembly_version=$assembly_version
base_version=$BASE
EOF
  exit 0
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "version=$version"
    echo "version_code=$VERSION_CODE"
    echo "artifact_slug=$artifact_slug"
    echo "git_sha_short=$SHA7"
    echo "is_release=$is_release"
    echo "year_month=$YEAR_MONTH"
    echo "assembly_version=$assembly_version"
    echo "base_version=$BASE"
  } >> "$GITHUB_OUTPUT"
fi

echo "Resolved version=$version code=$VERSION_CODE release=$is_release"
```

- [ ] **Step 4: Implement `resolve-version.ps1` wrapper**

```powershell
param(
  [string]$Date = "",
  [switch]$PrintEnv
)
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $root) { $root = Resolve-Path (Join-Path $PSScriptRoot "../..") }
# Prefer Git Bash
$bash = @(
  "C:\Program Files\Git\bin\bash.exe",
  "bash"
) | Where-Object { $_ -eq "bash" -or (Test-Path $_) } | Select-Object -First 1
$script = Join-Path $PSScriptRoot "resolve-version.sh"
$args = @()
if ($Date) { $args += @("--date", $Date) }
if ($PrintEnv) { $args += "--print-env" }
& $bash $script @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
```

- [ ] **Step 5: Make executable and re-run tests**

```bash
chmod +x scripts/ci/resolve-version.sh scripts/ci/test-resolve-version.sh
bash scripts/ci/test-resolve-version.sh
```

Expected: `resolve-version tests passed`.

- [ ] **Step 6: Commit**

```bash
git add scripts/ci/resolve-version.sh scripts/ci/resolve-version.ps1 scripts/ci/test-resolve-version.sh
git commit -m "feat: add shared CalVer resolve-version script"
```

---

### Task 4: Composite Action resolve-version

**Files:**
- Create: `.github/actions/resolve-version/action.yml`

- [ ] **Step 1: Write composite action**

```yaml
name: Resolve version
description: CalVer YYYY.MM.N from GITHUB_RUN_NUMBER with PR/dev suffixes
inputs:
  client_patch:
    description: Optional patch suffix like android.1
    required: false
    default: ''
  pr_number:
    description: PR number when event is pull_request
    required: false
    default: ''
outputs:
  version:
    description: Display version
    value: ${{ steps.run.outputs.version }}
  version_code:
    description: Android versionCode
    value: ${{ steps.run.outputs.version_code }}
  artifact_slug:
    value: ${{ steps.run.outputs.artifact_slug }}
  git_sha_short:
    value: ${{ steps.run.outputs.git_sha_short }}
  is_release:
    value: ${{ steps.run.outputs.is_release }}
  year_month:
    value: ${{ steps.run.outputs.year_month }}
  assembly_version:
    value: ${{ steps.run.outputs.assembly_version }}
  base_version:
    value: ${{ steps.run.outputs.base_version }}
runs:
  using: composite
  steps:
    - id: run
      shell: bash
      env:
        CLIENT_PATCH: ${{ inputs.client_patch }}
        PR_NUMBER: ${{ inputs.pr_number }}
      run: bash "${{ github.action_path }}/../../../scripts/ci/resolve-version.sh"
```

Note: `github.action_path` is `.github/actions/resolve-version`; relative path to repo scripts is `../../../scripts/ci/resolve-version.sh` only if action lives in repo checkout root layout — prefer:

```bash
bash "$GITHUB_WORKSPACE/scripts/ci/resolve-version.sh"
```

Use `$GITHUB_WORKSPACE` in the run step (requires checkout before this action).

- [ ] **Step 2: Commit**

```bash
git add .github/actions/resolve-version/action.yml
git commit -m "feat: add resolve-version composite action"
```

---

### Task 5: Directory.Build.props + API/Windows Injection

**Files:**
- Modify: `src/Directory.Build.props`
- Modify: `.github/workflows/build-api.yml` (partial now; finish in Task 7)
- Modify: `.github/workflows/build-windows.yml` (partial)

- [ ] **Step 1: Update `Directory.Build.props`**

```xml
<Project>
  <PropertyGroup>
    <!-- Numeric assembly version (CI overrides with assembly_version) -->
    <Version Condition="'$(Version)' == ''">0.0.0.0</Version>
    <FileVersion Condition="'$(FileVersion)' == ''">$(Version)</FileVersion>
    <!-- Display version: local default; CI passes -p:InformationalVersion=... -->
    <InformationalVersion Condition="'$(InformationalVersion)' == ''">0.0.0-local</InformationalVersion>
  </PropertyGroup>
</Project>
```

- [ ] **Step 2: Local smoke — API still builds**

```bash
dotnet build src/Pim.Api/Pim.Api.csproj -c Release
```

Expected: success.

- [ ] **Step 3: Verify InformationalVersion override**

```bash
dotnet publish src/Pim.Api/Pim.Api.csproj -c Release -o /tmp/pim-api-vertest \
  -p:InformationalVersion=2026.07.42 -p:Version=2026.7.42.0
# then inspect:
# PowerShell: [System.Diagnostics.FileVersionInfo]::GetVersionInfo(".../Pim.Api.dll").ProductVersion
```

Expected: ProductVersion / InformationalVersion contains `2026.07.42`.

- [ ] **Step 4: Commit**

```bash
git add src/Directory.Build.props
git commit -m "feat: align Directory.Build.props with CalVer injection"
```

---

### Task 6: Web Version Observability

**Files:**
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Create or modify: `src/client-web/src/vite-env.d.ts`
- Modify: `src/client-web/vite.config.ts` (default string only if needed)

- [ ] **Step 1: Declare global**

`src/client-web/src/vite-env.d.ts` (create if missing):

```ts
declare const __APP_VERSION__: string;
```

- [ ] **Step 2: Show version in sidebar footer**

In `Sidebar.tsx` footer block (near username), add a small muted version line:

```tsx
<p className="mt-1 truncate text-[10px] text-slate-400" title={__APP_VERSION__}>
  {__APP_VERSION__}
</p>
```

Place under username so it does not redesign layout.

- [ ] **Step 3: Default local string**

In `vite.config.ts` keep:

```ts
__APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || '0.0.0-local')
```

- [ ] **Step 4: Typecheck / build smoke**

```bash
npm --prefix src/client-web run build
```

Expected: success; built assets under `src/Pim.Api/wwwroot` contain version string when `VITE_APP_VERSION` set:

```bash
# Unix
VITE_APP_VERSION=2026.07.42 npm --prefix src/client-web run build
rg -n "2026\.07\.42" src/Pim.Api/wwwroot | head
```

- [ ] **Step 5: Commit**

```bash
git add src/client-web/src/layout/Sidebar.tsx src/client-web/src/vite-env.d.ts src/client-web/vite.config.ts
git commit -m "feat: surface injected web app version in sidebar"
```

---

### Task 7: Convert Four Build Workflows To workflow_call

**Files:**
- Rewrite: `.github/workflows/build-api.yml`
- Rewrite: `.github/workflows/build-web.yml`
- Rewrite: `.github/workflows/build-windows.yml`
- Rewrite: `.github/workflows/build-android.yml`

Common contract for each reusable workflow:

```yaml
on:
  workflow_call:
    inputs:
      version:
        required: true
        type: string
      version_code:
        required: true
        type: string
      artifact_slug:
        required: true
        type: string
      assembly_version:
        required: true
        type: string
      git_sha_short:
        required: true
        type: string
  workflow_dispatch:
    inputs:
      version:
        required: false
        default: ''
      # optional overrides for local debug
```

For `workflow_dispatch` without version, job may call resolve-version once (dev only). **Release must not use dispatch-only path.**

#### build-api.yml key publish step

```yaml
- name: Publish API
  run: |
    dotnet publish src/Pim.Api/Pim.Api.csproj \
      --configuration Release \
      --runtime linux-x64 \
      -o publish/ \
      -p:InformationalVersion=${{ inputs.version }} \
      -p:Version=${{ inputs.assembly_version }} \
      -p:FileVersion=${{ inputs.assembly_version }}

- name: Stage artifact
  run: |
    mkdir -p build/artifacts
    tar czf "build/artifacts/pim-api-v${{ inputs.artifact_slug }}.tar.gz" -C publish .

- uses: actions/upload-artifact@v4
  with:
    name: pim-api-v${{ inputs.artifact_slug }}
    path: build/artifacts/*.tar.gz
```

Remove old inline `0.1.${BUILD_NUMBER}` generation.

Keep: restore, `dotnet test Pim.sln`.

#### build-web.yml

```yaml
- name: Build
  run: npm run build
  working-directory: src/client-web
  env:
    VITE_APP_VERSION: ${{ inputs.version }}

- uses: actions/upload-artifact@v4
  with:
    name: pim-web-v${{ inputs.artifact_slug }}
    path: src/Pim.Api/wwwroot/
    if-no-files-found: error
```

Keep existing schedule workbench tests if still required by project; if too slow for every PR, keep them on web path filter only (already true via ci path filter).

#### build-windows.yml

```yaml
- name: Publish
  run: |
    dotnet publish Pim.Client.App/Pim.Client.App.csproj `
      -c Release -o publish/ -r win-x64 --self-contained true `
      -p:PublishSingleFile=true `
      -p:IncludeNativeLibrariesForSelfExtract=true `
      -p:InformationalVersion=${{ inputs.version }} `
      -p:Version=${{ inputs.assembly_version }} `
      -p:FileVersion=${{ inputs.assembly_version }}
  working-directory: src/client-windows

- name: Write VERSION file
  shell: bash
  run: echo "${{ inputs.version }}" > src/client-windows/publish/VERSION

- uses: actions/upload-artifact@v4
  with:
    name: pim-windows-v${{ inputs.artifact_slug }}
    path: src/client-windows/publish/
```

Keep KeyStats clone step if still needed; keep companion unit test filter.

#### build-android.yml

```yaml
- name: Export version env
  run: |
    echo "CI_APP_VERSION=${{ inputs.version }}" >> "$GITHUB_ENV"
    echo "CI_VERSION_CODE=${{ inputs.version_code }}" >> "$GITHUB_ENV"
    echo "VERSION_FILE=${{ inputs.artifact_slug }}" >> "$GITHUB_ENV"

# Keep existing keystore decode + same secrets for ALL events (PR and push)
# Keep fail-fast if secrets missing

- name: Stage APK
  run: |
    mkdir -p build/artifacts
    cp app/build/outputs/apk/release/app-release.apk \
      "build/artifacts/pim-android-v${{ inputs.artifact_slug }}-vc${{ inputs.version_code }}.apk"
  working-directory: src/client-android

- uses: actions/upload-artifact@v4
  with:
    name: pim-android-v${{ inputs.artifact_slug }}-vc${{ inputs.version_code }}
    path: src/client-android/build/artifacts/*.apk
```

Remove temporary branch triggers (`codex/schedule-task-complete-system`).  
Do **not** use different signing for PR vs master.

- [ ] **Step 1: Implement all four workflow files**
- [ ] **Step 2: YAML sanity**

```bash
# if actionlint available:
# actionlint .github/workflows/*.yml
```

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build-api.yml .github/workflows/build-web.yml .github/workflows/build-windows.yml .github/workflows/build-android.yml
git commit -m "feat: make platform builds reusable with shared version inputs"
```

---

### Task 8: Orchestrator ci.yml + Release

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Write orchestrator**

```yaml
name: CI

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]
  workflow_dispatch:
    inputs:
      client_patch:
        description: 'Optional client patch suffix (e.g. android.1)'
        required: false
        default: ''
      force_all:
        description: 'Build all platforms even on PR'
        type: boolean
        default: false

permissions:
  contents: write

jobs:
  resolve-version:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.v.outputs.version }}
      version_code: ${{ steps.v.outputs.version_code }}
      artifact_slug: ${{ steps.v.outputs.artifact_slug }}
      git_sha_short: ${{ steps.v.outputs.git_sha_short }}
      is_release: ${{ steps.v.outputs.is_release }}
      year_month: ${{ steps.v.outputs.year_month }}
      assembly_version: ${{ steps.v.outputs.assembly_version }}
      base_version: ${{ steps.v.outputs.base_version }}
    steps:
      - uses: actions/checkout@v4
      - id: v
        uses: ./.github/actions/resolve-version
        with:
          client_patch: ${{ inputs.client_patch }}
          pr_number: ${{ github.event.pull_request.number }}

  changes:
    runs-on: ubuntu-latest
    outputs:
      api: ${{ steps.filter.outputs.api }}
      web: ${{ steps.filter.outputs.web }}
      windows: ${{ steps.filter.outputs.windows }}
      android: ${{ steps.filter.outputs.android }}
      all: ${{ steps.flags.outputs.all }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        if: github.event_name == 'pull_request' && inputs.force_all != true
        with:
          filters: |
            api:
              - 'src/Pim.Api/**'
              - 'src/Pim.Core/**'
              - 'src/Pim.Infrastructure/**'
              - 'src/modules/**'
              - 'tests/Pim.UnitTests/**'
              - 'Pim.sln'
              - 'src/Directory.Build.props'
              - 'scripts/ci/**'
              - '.github/workflows/build-api.yml'
              - '.github/workflows/ci.yml'
            web:
              - 'src/client-web/**'
              - 'branding/**'
              - 'scripts/branding/**'
              - '.github/workflows/build-web.yml'
              - '.github/workflows/ci.yml'
            windows:
              - 'src/client-windows/**'
              - 'src/Directory.Build.props'
              - 'branding/**'
              - '.github/workflows/build-windows.yml'
              - '.github/workflows/ci.yml'
            android:
              - 'src/client-android/**'
              - 'branding/**'
              - '.github/workflows/build-android.yml'
              - '.github/workflows/ci.yml'
      - id: flags
        shell: bash
        run: |
          if [[ "${{ github.ref }}" == "refs/heads/master" || "${{ github.event_name }}" == "workflow_dispatch" || "${{ inputs.force_all }}" == "true" || "${{ github.event_name }}" != "pull_request" ]]; then
            echo "all=true" >> "$GITHUB_OUTPUT"
          else
            echo "all=false" >> "$GITHUB_OUTPUT"
          fi

  build-api:
    needs: [resolve-version, changes]
    if: needs.changes.outputs.all == 'true' || needs.changes.outputs.api == 'true'
    uses: ./.github/workflows/build-api.yml
    with:
      version: ${{ needs.resolve-version.outputs.version }}
      version_code: ${{ needs.resolve-version.outputs.version_code }}
      artifact_slug: ${{ needs.resolve-version.outputs.artifact_slug }}
      assembly_version: ${{ needs.resolve-version.outputs.assembly_version }}
      git_sha_short: ${{ needs.resolve-version.outputs.git_sha_short }}

  build-web:
    needs: [resolve-version, changes]
    if: needs.changes.outputs.all == 'true' || needs.changes.outputs.web == 'true'
    uses: ./.github/workflows/build-web.yml
    with:
      version: ${{ needs.resolve-version.outputs.version }}
      version_code: ${{ needs.resolve-version.outputs.version_code }}
      artifact_slug: ${{ needs.resolve-version.outputs.artifact_slug }}
      assembly_version: ${{ needs.resolve-version.outputs.assembly_version }}
      git_sha_short: ${{ needs.resolve-version.outputs.git_sha_short }}

  build-windows:
    needs: [resolve-version, changes]
    if: needs.changes.outputs.all == 'true' || needs.changes.outputs.windows == 'true'
    uses: ./.github/workflows/build-windows.yml
    with:
      version: ${{ needs.resolve-version.outputs.version }}
      version_code: ${{ needs.resolve-version.outputs.version_code }}
      artifact_slug: ${{ needs.resolve-version.outputs.artifact_slug }}
      assembly_version: ${{ needs.resolve-version.outputs.assembly_version }}
      git_sha_short: ${{ needs.resolve-version.outputs.git_sha_short }}

  build-android:
    needs: [resolve-version, changes]
    if: needs.changes.outputs.all == 'true' || needs.changes.outputs.android == 'true'
    uses: ./.github/workflows/build-android.yml
    secrets: inherit
    with:
      version: ${{ needs.resolve-version.outputs.version }}
      version_code: ${{ needs.resolve-version.outputs.version_code }}
      artifact_slug: ${{ needs.resolve-version.outputs.artifact_slug }}
      assembly_version: ${{ needs.resolve-version.outputs.assembly_version }}
      git_sha_short: ${{ needs.resolve-version.outputs.git_sha_short }}

  release:
    needs: [resolve-version, build-api, build-web, build-windows, build-android]
    if: |
      always() &&
      needs.resolve-version.outputs.is_release == 'true' &&
      needs.build-api.result == 'success' &&
      needs.build-web.result == 'success' &&
      needs.build-windows.result == 'success' &&
      needs.build-android.result == 'success'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          path: release-assets
          pattern: pim-*
          merge-multiple: true
      - name: List assets
        run: find release-assets -type f | sort
      - name: Create or update GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ needs.resolve-version.outputs.version }}
          name: v${{ needs.resolve-version.outputs.version }}
          generate_release_notes: true
          fail_on_unmatched_files: true
          files: |
            release-assets/**
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**Important implementation notes:**

1. `workflow_call` child workflows must declare matching `inputs` and accept `secrets: inherit` for Android.
2. On master, `changes.all=true` so all four always run (required for release gate).
3. On PR, skipped platforms make `needs.build-*.result` = `skipped`. Release job only runs on master full success — good.
4. If `softprops` tag already exists, action updates release assets when configured; if not, add `make_latest: true` and re-run tolerance.
5. Child workflows currently also trigger on `push`/`pull_request` — **remove those** so only `ci.yml` orchestrates, avoiding double builds and divergent versions. Keep only `workflow_call` + `workflow_dispatch`.

- [ ] **Step 2: Remove direct push/PR triggers from the four build workflows** (only `workflow_call` + `workflow_dispatch`)

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml .github/workflows/build-*.yml
git commit -m "feat: orchestrate CalVer CI builds and master GitHub Releases"
```

---

### Task 9: Android Signing Consistency Guardrails

**Files:**
- Modify: `.github/workflows/build-android.yml` (comments + optional cert fingerprint step)
- Modify: `src/client-android/app/build.gradle.kts` only if needed to ensure CI signing on both debug/release when secrets present (already present — verify)

- [ ] **Step 1: Verify gradle still uses CI signing for release when env set**

Confirm `hasCiSigning` applies to `release` and that CI always builds `assembleRelease` with secrets.

- [ ] **Step 2: Add post-build fingerprint log (non-secret)**

```yaml
- name: Log signing cert fingerprint
  working-directory: src/client-android
  run: |
    APK=$(ls build/artifacts/*.apk | head -n1)
    "$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --print-certs "$APK" | tee build/artifacts/signing-certs.txt || true
```

Upload `signing-certs.txt` next to APK or as part of artifact for PR vs master comparison.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build-android.yml
git commit -m "test: log android apk signing fingerprint for upgrade checks"
```

---

### Task 10: End-to-End Verification Checklist

**Files:** none (verification only); fix any gaps found

- [ ] **Step 1: Local script tests**

```bash
bash scripts/ci/test-resolve-version.sh
```

Expected: pass.

- [ ] **Step 2: Local icon paths**

```bash
test -f branding/pim-mark.svg
test -f src/client-web/public/favicon.svg
test -f src/client-windows/Pim.Client.App/app.ico
test -f src/client-android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png
```

- [ ] **Step 3: .NET inject**

```bash
dotnet test Pim.sln -c Release
dotnet publish src/Pim.Api/Pim.Api.csproj -c Release -o publish-api-test \
  -p:InformationalVersion=2026.07.99 -p:Version=2026.7.99.0
```

- [ ] **Step 4: Web inject**

```bash
VITE_APP_VERSION=2026.07.99 npm --prefix src/client-web run build
```

- [ ] **Step 5: Push branch and open PR**

```bash
git push -u origin codex/version-icons-github-actions
gh pr create --base master --title "feat: unify CalVer, brand icons, and CI releases" --body "$(cat <<'EOF'
## Summary
- CalVer `YYYY.MM.N` shared via resolve-version
- Locked four-color mosaic P icons on Web/Windows/Android
- Orchestrated CI + master GitHub Releases
- Android PR/master same signing

## Spec
docs/superpowers/specs/2026-07-12-version-icons-github-actions-design.md

## Test plan
- [ ] resolve-version script tests
- [ ] CI PR builds changed platforms with shared version
- [ ] Android APK signed; fingerprint logged
- [ ] After merge: Release vYYYY.MM.N with four artifacts
EOF
)"
```

- [ ] **Step 6: Wait for GitHub Actions on the PR; fix failures**

- [ ] **Step 7: After merge to master, confirm Release exists with four assets and identical version strings**

---

## Spec Coverage Matrix

| Spec requirement | Task |
|------------------|------|
| CalVer `YYYY.MM.N` + `version_code=100000+N` | Task 3–4, 8 |
| PR / dev / client_patch suffixes | Task 3, 8 |
| Single resolve-version exit | Task 3–4 |
| Locked four-color mosaic P SVG | Task 1–2 |
| Web/Windows/Android derivatives | Task 2 |
| Directory.Build.props + InformationalVersion authority | Task 5, 7 |
| API/Web/Windows/Android injection | Task 5–7 |
| ci.yml orchestrator, master always 4 builds | Task 8 |
| workflow_call reusable builds | Task 7–8 |
| Remove temp branch triggers | Task 7 |
| Artifact naming | Task 7–8 |
| GitHub Release on master full success | Task 8 |
| Android same signing PR/master | Task 7, 9 |
| Sidebar/min UI version observability | Task 6 |
| Acceptance checklist | Task 10 |

## Self-Review Notes

- No TBD left for version formula, icon geometry, or release gate.
- `assembly_version` uses `YYYY.M.N.0` numeric form for MSBuild; display uses CalVer string.
- Child workflows must not self-trigger on push/PR after Task 8.
- Branding export commits binaries so CI does not require `npm install` in branding unless regenerating.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-12-version-icons-github-actions.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
2. **Inline Execution** — this session with executing-plans and checkpoints  

Which approach?

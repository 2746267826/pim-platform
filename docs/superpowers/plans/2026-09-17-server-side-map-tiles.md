# Server-Side Map Tiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make historical-location map tiles render through a PIM API endpoint with validated coordinates, proxy-aware upstream access, durable expiring cache, and frontend same-origin URLs.

**Architecture:** Add a focused `TileService` and `TileCoordinate` in `src/Pim.Api/Tiles`, expose them through `MapTileEndpoints`, and register a named HttpClient plus options in `Program.cs`. Use an injected `TimeProvider`, per-coordinate async locks, and atomic file replacement. Update Leaflet/Vite/docs and cover behavior with isolated unit tests.

**Tech Stack:** .NET 8 Minimal API, `HttpClient`, xUnit, React Leaflet, Vite.

**Spec:** `docs/superpowers/specs/2026-09-17-server-side-map-tiles-design.md`

## Global Constraints

- Never fetch arbitrary URLs; only the configured OpenStreetMap base URL is used.
- Invalid z/x/y returns HTTP 400; malformed or non-image upstream responses never enter the cache.
- Cache writes use temp files plus atomic move and expire through injected `TimeProvider`.
- Production nginx configuration is outside this repository and must be removed by the operator separately.
- No generated outputs are committed; all user-facing text remains Simplified Chinese where applicable.

### Task 1: Coordinate validation and cache service tests

**Files:**
- Create: `tests/Pim.UnitTests/Tiles/TileCoordinateTests.cs`
- Create: `tests/Pim.UnitTests/Tiles/TileServiceTests.cs`

**Interfaces:** Tests target `TileCoordinate.TryParse`, `TileService.GetTileAsync`, and a handler-injected `HttpClient`.

- [ ] Write tests for valid coordinates, z/x/y boundaries, out-of-range and non-numeric input; write cache-hit, bad-response, and concurrent-request tests using a temporary directory, fake handler, and mutable `TimeProvider`.
- [ ] Run `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj -c Release --filter FullyQualifiedName~Tiles` and observe the expected missing-type failures.

### Task 2: Implement tile coordinate, options, fetcher, cache service, and endpoint

**Files:**
- Create: `src/Pim.Api/Tiles/TileCoordinate.cs`
- Create: `src/Pim.Api/Tiles/TileOptions.cs`
- Create: `src/Pim.Api/Tiles/TileService.cs`
- Create: `src/Pim.Api/Endpoints/TileEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`

**Interfaces:** `TileService` returns `TileResult` with bytes and cache metadata; `MapTileEndpoints` maps `/api/v1/tiles/{z:int}/{x:int}/{y:int}.png` and translates validation/upstream failures to 400/502.

- [ ] Implement the smallest validation and cache behavior needed to make Task 1 green.
- [ ] Add named HttpClient configuration with descriptive User-Agent and short timeout; bind `Tiles` options and register `TileService`/`TimeProvider.System`.
- [ ] Map the endpoint with anonymous access, PNG content type, long Cache-Control, and `X-PIM-Tile-Cache` hit/miss header.
- [ ] Run the tile tests and then the full unit project.

### Task 3: Frontend and documentation

**Files:**
- Modify: `src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx`
- Modify: `src/client-web/vite.config.ts`
- Modify: `README.md`

- [ ] Point Leaflet at a BASE_URL-safe `api/v1/tiles/{z}/{x}/{y}.png` path and remove the Vite `/tiles` proxy.
- [ ] Replace README nginx `/tiles` instructions with the server-side API behavior and operator note.
- [ ] Run `npm --prefix src/client-web run build`.

### Task 4: Verification and delivery

- [ ] Run `dotnet build Pim.sln -c Release` and `dotnet test Pim.sln -c Release`; record actual counts.
- [ ] Start the API against `pim_test`, authenticate, curl a known tile twice, verify status/content type/size/PNG magic/cache-hit header, and curl invalid coordinates for 400.
- [ ] Review the diff, commit with a bilingual conventional message, push `cv-linux/tiles-server`, open a bilingual PR with all required sections and command output evidence.
- [ ] Poll `gh pr checks <n>` until relevant jobs reach terminal states and report exact results.

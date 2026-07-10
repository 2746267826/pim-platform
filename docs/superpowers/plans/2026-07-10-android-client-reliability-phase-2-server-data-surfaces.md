# Android Client Reliability Phase 2 Server Data Surfaces Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用受信任、仅内存鉴权的服务端 WebUI 嵌入替换 Android Today 和 Tracks 占位内容，同时保留原生五标签、采集健康和真实传输状态。

**Architecture:** React 在桌面 `AuthProvider/AppLayout` 之外新增两个 Android embed routes，通过 `window.pimNative` 请求短期 access token，并用独立 fetch client 在单次 401 后向原生请求刷新。Android 使用 AndroidX WebKit 限制精确 origin/path、管理 WebView 生命周期和错误状态；原生 wrapper 始终展示本地队列/同步事实，Web 只展示服务端已经接收的数据。

**Tech Stack:** React 19, TypeScript 6, TanStack Query 5, date-fns-tz, React Leaflet 5, Playwright 1.61, AndroidX WebKit, Jetpack Compose, Hilt, OkHttp MockWebServer, .NET 8/xUnit.

---

## Final Objective

Phase 2 结束时，Today 和 Tracks 的所有指标、地图、片段与原始点均来自服务端；用户能同时看到这些服务端数据的生成时间和原生传输状态。断网、未登录、服务端空、筛选空、HTTP/Web resource 失败都显示明确状态，不出现空白 WebView。

## Preconditions

- Phase 1 PR 已合并到 `master`，`mobileItemResultsV1` 已部署且 Android 状态/同步基础通过。
- 从最新 `origin/master` 创建 `codex/android-server-data-surfaces` 独立 worktree。
- 不直接复用 `/location-history` 或 `/mobile-records`，因为它们包含桌面 Sidebar/QuickNote/AppLayout。
- 不提交 `src/Pim.Api/wwwroot/`、Playwright 临时截图、build/dist、APK 或地图缓存。
- 不修改 `.github/workflows/*`；通过 npm lifecycle 把新测试接入现有 Web CI 命令。

## File Structure Map

### Web Embed Boundary

- Create: `src/client-web/src/embed/androidWebMessageBridge.ts`
- Create: `src/client-web/src/embed/AndroidEmbedAuthContext.tsx`
- Create: `src/client-web/src/embed/androidEmbedApiClient.ts`
- Create: `src/client-web/src/embed/AndroidEmbedApp.tsx`
- Create: `src/client-web/src/pages/embed/AndroidTodayPage.tsx`
- Create: `src/client-web/src/pages/embed/AndroidTracksPage.tsx`
- Create: `src/client-web/src/components/mobile/MobileLocationMapViewport.tsx`
- Create: `src/client-web/src/components/mobile/AndroidTodayMapPreview.tsx`
- Create: `src/client-web/src/components/mobile/AndroidTracksDashboard.tsx`
- Create: `src/client-web/src/components/mobile/locationGapSegments.ts`
- Modify: `src/client-web/src/App.tsx`
- Modify: `src/client-web/src/api/mobile.ts`
- Modify: `src/client-web/src/components/mobile/mobileFormatting.ts`
- Modify: `src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx`
- Modify: `src/client-web/src/components/mobile/LocationHistoryMap.tsx`
- Modify: `src/client-web/src/components/mobile/LocationRawPointTable.tsx`
- Modify: `src/client-web/src/components/mobile/LocationSegmentDetail.tsx`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`

### Web/API Tests And Contract

- Create: `tests/client-web/androidEmbedBridge.test.ts`
- Create: `tests/client-web/androidEmbedBridgeFixture.ts`
- Create: `tests/client-web/androidEmbedAuth.test.tsx`
- Create: `tests/client-web/androidEmbedRoutes.test.tsx`
- Create: `tests/client-web/androidEmbedTimeRange.test.ts`
- Create: `tests/client-web/androidEmbedPages.test.tsx`
- Create: `tests/client-web/androidTracksInteractions.test.tsx`
- Create: `tests/client-web/locationGapSegments.test.ts`
- Create: `tests/client-web/androidEmbedVisualAudit.test.ts`
- Create: `tests/client-web/androidEmbedFixtures.ts`
- Create: `tests/client-web/androidMapTileFixture.ts`
- Create: `tests/client-web/tsconfig.android-embed.json`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs`
- Modify: `src/Pim.Api/Endpoints/VersionEndpoints.cs`
- Modify: `tests/Pim.UnitTests/Api/VersionEndpointTests.cs`

### Android Embed Boundary

- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedWebProtocol.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedWebSessionController.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedTokenProvider.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedNavigationPolicy.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedWebState.kt`
- Replace: `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Modify: `src/client-android/app/build.gradle.kts`
- Test: `src/client-android/app/src/test/java/com/pim/app/web/EmbeddedWebProtocolTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/web/EmbeddedNavigationPolicyTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/web/EmbeddedWebSessionControllerTest.kt`
- Test: `src/client-android/app/src/androidTest/java/com/pim/app/web/PimWebViewScreenTest.kt`
- Test: `src/client-android/app/src/androidTest/java/com/pim/app/ui/today/TodayWrapperContentTest.kt`
- Test: `src/client-android/app/src/androidTest/java/com/pim/app/ui/tracks/TracksWrapperContentTest.kt`

### Phase Reports

- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`
- Create: `docs/superpowers/reports/2026-07-10-android-client-reliability-phase-2.md`

## Locked Message Protocol

Use these exact wire message types in TypeScript and Kotlin:

```text
Web -> Native
  request-access-token { requestId }
  refresh-access-token { requestId }
  embed-state { route, state, generatedAtUtc, hasData, errorCode }

Native -> Web
  access-token { requestId, accessToken, expiresAtUtc }
  sync-completed { runId, serverConfirmedAtUtc }
  auth-cleared {}
```

Valid `route`: `today | tracks`.

Valid `state`: `loading | content | server-empty | filtered-empty | partial | error | auth-required`.

Every message carries `protocolVersion: 1`. Unknown type/version is ignored and logged locally without echoing payload tokens.

## Task 0: Create The Phase Worktree And Verify Phase 1 Baseline

**Files:**
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`

- [ ] **Step 1: Create the isolated Phase 2 worktree**

Invoke `superpowers:using-git-worktrees`, create `codex/android-server-data-surfaces` from updated `origin/master`, then run:

```powershell
git status --short --branch
git log -5 --oneline --decorate
```

Expected: Phase 1 merge is present and the worktree is clean.

- [ ] **Step 2: Run cross-surface baseline commands**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Pim.UnitTests.Mobile|FullyQualifiedName~VersionEndpointTests"
npm --prefix src/client-web run build
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
```

Expected: PASS. Remove only the generated `src/Pim.Api/wwwroot/` build output created by this command and keep it unstaged.

- [ ] **Step 3: Mark Phase 2 coverage rows as Implementing**

Update REL-09 and REL-10 to `Implementing` with branch name and baseline results; do not change any Verified Phase 1 row.

- [ ] **Step 4: Commit the phase marker**

```powershell
git add docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md
git commit -m "docs: start android server surface phase"
```

## Task 1: Build The Web Message Bridge And Memory-Only Embed Authentication

**Files:**
- Create: `src/client-web/src/embed/androidWebMessageBridge.ts`
- Create: `src/client-web/src/embed/androidEmbedApiClient.ts`
- Create: `src/client-web/src/embed/AndroidEmbedAuthContext.tsx`
- Test: `tests/client-web/androidEmbedBridge.test.ts`
- Create: `tests/client-web/androidEmbedBridgeFixture.ts`
- Test: `tests/client-web/androidEmbedAuth.test.tsx`
- Create: `tests/client-web/tsconfig.android-embed.json`

- [ ] **Step 1: Write failing protocol parsing tests**

```ts
const message = parseNativeMessage(JSON.stringify({
  protocolVersion: 1,
  type: 'access-token',
  requestId: 'request-1',
  accessToken: 'short-lived-access',
  expiresAtUtc: '2026-07-10T12:00:00Z',
}));

assert.equal(message?.type, 'access-token');
assert.equal(parseNativeMessage('{"protocolVersion":2,"type":"access-token"}'), null);
assert.equal(parseNativeMessage('not-json'), null);

const native = createNativeBridgeFixture();
const bridge = new AndroidWebMessageBridge(native.object);
const first = bridge.requestAccessToken();
let deliveries = 0;
const unsubscribe = bridge.subscribe(() => { deliveries += 100; });
unsubscribe(); // Simulates the StrictMode effect cleanup.
bridge.subscribe(() => { deliveries += 1; }); // Simulates the second effect mount.
const second = bridge.requestAccessToken();
assert.equal(native.messages.filter(message => message.type === 'request-access-token').length, 1);
const request = native.messages.find(message => message.type === 'request-access-token');
assert.ok(request);
native.deliver(accessToken(request.requestId, 'token-a'));
assert.equal(deliveries, 1);
assert.equal((await first).accessToken, 'token-a');
assert.equal((await second).accessToken, 'token-a');
```

The bridge must coalesce outstanding requests by kind until the matching request ID resolves, so the StrictMode cleanup/remount leaves one listener and one initial token request.

- [ ] **Step 2: Define bridge types and the injected object**

```ts
export interface PimNativeBridgeObject {
  postMessage(message: string): void;
  onmessage: ((event: MessageEvent<string>) => void) | null;
}

declare global {
  interface Window { pimNative?: PimNativeBridgeObject }
}

export type NativeMessage =
  | { protocolVersion: 1; type: 'access-token'; requestId: string; accessToken: string; expiresAtUtc: string | null }
  | { protocolVersion: 1; type: 'sync-completed'; runId: string; serverConfirmedAtUtc: string }
  | { protocolVersion: 1; type: 'auth-cleared' };
```

`AndroidWebMessageBridge` creates request IDs, posts only the locked request types, parses/validates replies, supports multiple event subscribers, and restores the previous `onmessage` handler on dispose.

- [ ] **Step 3: Write failing fetch tests for initial token and one 401 refresh**

```ts
const native = createNativeBridgeFixture();
const bridge = new AndroidWebMessageBridge(native.object);
const { fetchImpl, calls: fetchCalls } = createFetchFixture([200]);
const client = createAndroidEmbedApiClient({ bridge, fetchImpl });
const pending = client.get<{ code: number }>('/mobile/devices');

assert.equal(fetchCalls.length, 0);
const request = native.messages.find(message => message.type === 'request-access-token');
assert.ok(request);
native.deliver(accessToken(request.requestId, 'token-a'));
await pending;
assert.equal(fetchCalls[0].headers.get('Authorization'), 'Bearer token-a');

export function accessToken(requestId: string, token: string): NativeMessage {
  return {
    protocolVersion: 1,
    type: 'access-token',
    requestId,
    accessToken: token,
    expiresAtUtc: '2026-07-10T12:00:00Z',
  };
}

export function createNativeBridgeFixture() {
  const messages: Array<Record<string, any>> = [];
  const object: PimNativeBridgeObject = {
    onmessage: null,
    postMessage(raw) { messages.push(JSON.parse(raw)); },
  };
  return {
    object,
    messages,
    deliver(message: NativeMessage) {
      object.onmessage?.(new MessageEvent('message', { data: JSON.stringify(message) }));
    },
  };
}

export function createFetchFixture(statuses: number[]) {
  const calls: Array<{ url: string; headers: Headers }> = [];
  let index = 0;
  const fetchImpl: typeof fetch = async (input, init) => {
    calls.push({ url: String(input), headers: new Headers(init?.headers) });
    const status = statuses[index++] ?? statuses.at(-1) ?? 200;
    return new Response('{"code":0,"data":{}}', {
      status,
      headers: { 'Content-Type': 'application/json' },
    });
  };
  return { fetchImpl, calls };
}
```

Place these three helpers in `tests/client-web/androidEmbedBridgeFixture.ts` and import them from both bridge/auth test files.

For the 401 case, use `createFetchFixture([401, 200])`; deliver token-b using the recorded refresh request ID and assert one `refresh-access-token` plus two fetches. Use `[401, 401]` for the terminal case and assert there is no third fetch.

- [ ] **Step 4: Implement a dedicated embed client**

```ts
export interface AndroidEmbedApiClient {
  get<T>(path: string): Promise<T>;
  clear(): void;
}
```

Rules:

- API base remains `/api/v1`;
- wait for a trusted token before the first fetch;
- keep token in a closure only, never `localStorage`/`sessionStorage`/cookie;
- on first 401 request native refresh and retry once;
- on second 401 clear memory and emit auth-required;
- `auth-cleared` aborts in-flight requests and clears query data;
- log method/path/status only, never headers or token.

- [ ] **Step 5: Implement `AndroidEmbedAuthContext`**

Expose `authState: waiting | ready | required`, the client, bridge events, and a callback that invalidates only `mobile-*` query keys after `sync-completed`. Do not render data-query children until `ready`.

- [ ] **Step 6: Run bridge/auth tests and typecheck**

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedBridge.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedAuth.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.android-embed.json
```

Expected: PASS with zero API calls before token, one refresh, no persisted token, and idempotent StrictMode behavior.

- [ ] **Step 7: Commit the Web auth boundary**

```powershell
git add src/client-web/src/embed tests/client-web/androidEmbedBridgeFixture.ts tests/client-web/androidEmbedBridge.test.ts tests/client-web/androidEmbedAuth.test.tsx tests/client-web/tsconfig.android-embed.json
git commit -m "feat: add trusted android embed authentication"
```

## Task 2: Add Sidebar-Free Embed Routing And IANA Time Boundaries

**Files:**
- Create: `src/client-web/src/embed/AndroidEmbedApp.tsx`
- Create: `src/client-web/src/pages/embed/AndroidTodayPage.tsx`
- Create: `src/client-web/src/pages/embed/AndroidTracksPage.tsx`
- Modify: `src/client-web/src/App.tsx`
- Modify: `src/client-web/src/components/mobile/mobileFormatting.ts`
- Modify: `src/client-web/src/api/mobile.ts`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`
- Test: `tests/client-web/androidEmbedRoutes.test.tsx`
- Test: `tests/client-web/androidEmbedTimeRange.test.ts`

- [ ] **Step 1: Write failing route isolation tests**

Render `/embed/android/today` and `/embed/android/tracks`; assert neither tree contains `AppLayout`, `Sidebar`, login form, QuickNote, or desktop navigation. Assert an unknown `/embed/android/*` path renders an embed-local error, not `/today` desktop redirect.

```tsx
assert.equal(resolveAndroidEmbedRoute('/embed/android/today'), 'today');
assert.equal(resolveAndroidEmbedRoute('/embed/android/tracks'), 'tracks');
assert.equal(resolveAndroidEmbedRoute('/embed/android/unknown'), 'not-found');

const native = createNativeBridgeFixture();
const bridge = new AndroidWebMessageBridge(native.object);
for (const path of ['/embed/android/today', '/embed/android/tracks', '/embed/android/unknown']) {
  const html = renderToStaticMarkup(<AndroidEmbedApp pathname={path} bridge={bridge} />);
  for (const forbidden of ['桌面导航', '快捷笔记', '登录', 'Sidebar']) {
    assert.equal(html.includes(forbidden), false, `${path} leaked ${forbidden}`);
  }
}
assert.match(
  renderToStaticMarkup(<AndroidEmbedApp pathname="/embed/android/unknown" bridge={bridge} />),
  /页面不可用/,
);
```

Expose `resolveAndroidEmbedRoute(pathname)` as a pure exact-match function. `AndroidEmbedApp` accepts `pathname=window.location.pathname` and a production bridge by default; tests pass both explicitly, so they do not mutate global location or depend on an injected browser object.

- [ ] **Step 2: Route embed paths before desktop AuthProvider**

```tsx
export default function App() {
  const embed = window.location.pathname.startsWith('/embed/android/');
  if (embed) return <AndroidEmbedApp />;

  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<Navigate to="/today" replace />} />
        <Route path="/*" element={<AppLayout />} />
      </Routes>
    </AuthProvider>
  );
}
```

`AndroidEmbedApp` wraps only `AndroidEmbedAuthProvider` and its two routes.

In this routing commit, both page files render only the auth context's truthful `waiting`/`required` state and an unframed loading surface; they make no data query and show no metric. Task 4 replaces the Today boundary with queries/content, and Task 5 does the same for Tracks. This keeps Task 2 buildable without fixed production data.

- [ ] **Step 3: Write failing DST-safe range tests**

```ts
assert.deepEqual(
  toMobileAnalyticsUtcRange(
    { startDate: '2026-03-08', endDate: '2026-03-08' },
    'America/New_York',
  ),
  {
    rangeStartUtc: '2026-03-08T05:00:00.000Z',
    rangeEndUtc: '2026-03-09T04:00:00.000Z',
    timezone: 'America/New_York',
  },
);

assert.deepEqual(
  toMobileAnalyticsUtcRange(
    { startDate: '2026-11-01', endDate: '2026-11-01' },
    'America/New_York',
  ),
  {
    rangeStartUtc: '2026-11-01T04:00:00.000Z',
    rangeEndUtc: '2026-11-02T05:00:00.000Z',
    timezone: 'America/New_York',
  },
);

assert.deepEqual(
  toMobileAnalyticsUtcRange(
    { startDate: '2026-07-10', endDate: '2026-07-10' },
    'Asia/Shanghai',
  ),
  {
    rangeStartUtc: '2026-07-09T16:00:00.000Z',
    rangeEndUtc: '2026-07-10T16:00:00.000Z',
    timezone: 'Asia/Shanghai',
  },
);
```

The three assertions cover the 23-hour spring day, 25-hour autumn day, and `Asia/Shanghai` midnight.

- [ ] **Step 4: Replace hardcoded +08 calculations with `date-fns-tz`**

Install the locked timezone dependency so `package.json` and `package-lock.json` change together:

```powershell
npm --prefix src/client-web install date-fns-tz@3.2.0
```

Then use:

```ts
import { fromZonedTime, formatInTimeZone } from 'date-fns-tz';

export function resolveBrowserTimeZone() {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
}

export function zonedDateStartUtc(dateInput: string, timeZone: string) {
  return fromZonedTime(`${dateInput}T00:00:00`, timeZone).toISOString();
}

export function formatDateInputInTimeZone(date: Date, timeZone: string) {
  return formatInTimeZone(date, timeZone, 'yyyy-MM-dd');
}
```

Use pure calendar-day arithmetic for date inputs, then convert both half-open boundaries with `fromZonedTime`. Keep existing exported Shanghai functions as compatibility wrappers calling the generic functions.

- [ ] **Step 5: Add explicit range summary paths**

Extend `mobileApiPaths.summary` and embed calls to send `rangeStartUtc`, `rangeEndUtc`, timezone, and device ID. Do not rely on a bare UTC `date` for Today.

- [ ] **Step 6: Run route/time tests and Web build**

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedRoutes.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedTimeRange.test.ts
npm --prefix src/client-web run build
```

Expected: PASS. Inspect and leave generated `src/Pim.Api/wwwroot/` unstaged.

- [ ] **Step 7: Commit routing and timezone behavior**

```powershell
git add src/client-web/src/App.tsx src/client-web/src/embed/AndroidEmbedApp.tsx src/client-web/src/pages/embed/AndroidTodayPage.tsx src/client-web/src/pages/embed/AndroidTracksPage.tsx src/client-web/src/components/mobile/mobileFormatting.ts src/client-web/src/api/mobile.ts src/client-web/package.json src/client-web/package-lock.json tests/client-web
git commit -m "feat: add isolated android embed routes"
```

## Task 3: Complete The Server Location Segment Contract

**Files:**
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs`
- Modify: `src/client-web/src/api/mobile.ts`

- [ ] **Step 1: Write failing aggregation and JSON contract tests**

```csharp
[Fact]
public async Task SegmentReportsProviderMixAndAltitudeAvailability()
{
    await using var db = MobileTestHelpers.CreateDb();
    SeedPoint(db, "11111111-1111-1111-1111-111111111111", "2026-07-07T10:20:00Z", 31.230416, 121.473701, 12, "usable", provider: "gps", altitudeMeters: 12.5m);
    SeedPoint(db, "22222222-2222-2222-2222-222222222222", "2026-07-07T10:25:00Z", 31.235000, 121.480000, 18, "usable", provider: "network", altitudeMeters: null);
    await db.SaveChangesAsync();

    var tracks = await Service(db).GetTracksAsync(new MobileLocationQueryRequest(
        RangeStartUtc: DateTimeOffset.Parse("2026-07-07T00:00:00Z"),
        RangeEndUtc: DateTimeOffset.Parse("2026-07-08T00:00:00Z")), CancellationToken.None);
    var segment = Assert.Single(Assert.Single(tracks).Segments);

    Assert.Equal(1, segment.ProviderMix["gps"]);
    Assert.Equal(1, segment.ProviderMix["network"]);
    Assert.True(segment.HasAltitude);
    Assert.Equal(1, segment.AltitudePointCount);

    var json = JsonSerializer.Serialize(segment, new JsonSerializerOptions(JsonSerializerDefaults.Web));
    Assert.Contains("\"providerMix\"", json, StringComparison.Ordinal);
    Assert.Contains("\"hasAltitude\":true", json, StringComparison.Ordinal);
    Assert.Contains("\"altitudePointCount\":1", json, StringComparison.Ordinal);
}
```

Extend the existing test helper rather than introducing another service fixture:

```csharp
private static void SeedPoint(
    PimDbContext db,
    string id,
    string recordedAt,
    double lat,
    double lon,
    double accuracy,
    string quality,
    string deviceId = "pixel-8",
    string provider = "gps",
    decimal? altitudeMeters = null)
{
    db.Set<MobileLocationPointEntity>().Add(new MobileLocationPointEntity
    {
        Id = Guid.Parse(id),
        UserId = MobileTestHelpers.UserId,
        DeviceId = deviceId,
        RecordedAtUtc = DateTimeOffset.Parse(recordedAt),
        Latitude = Convert.ToDecimal(lat),
        Longitude = Convert.ToDecimal(lon),
        HorizontalAccuracyMeters = Convert.ToDecimal(accuracy),
        Provider = provider,
        AltitudeMeters = altitudeMeters,
        Source = "auto",
        Quality = quality,
        RawJson = "{}",
        CreatedAt = DateTimeOffset.Parse(recordedAt),
    });
}
```

The final three assertions lock the camel-case JSON contract for `providerMix`, `hasAltitude`, and `altitudePointCount`.

- [ ] **Step 2: Extend the DTO and aggregation**

Append these fields to `MobileLocationSegmentDto` before bounds/path:

```csharp
IReadOnlyDictionary<string, int> ProviderMix,
bool HasAltitude,
int AltitudePointCount,
```

Build provider counts with case-insensitive grouping and `unknown` fallback. `HasAltitude` is true when `AltitudePointCount > 0`.

- [ ] **Step 3: Update TypeScript types and detail rendering contract**

```ts
export interface MobileLocationSegment {
  providerMix: Record<string, number>;
  hasAltitude: boolean;
  altitudePointCount: number;
}
```

Preserve all existing fields; `LocationSegmentDetail` displays provider mix and `有高度数据/无高度数据` with count.

- [ ] **Step 4: Add a 30-day path performance fixture**

Test the response builder with 10,000 path points and assert it returns complete polyline paths within a fixed test timeout while UI marker/circle reduction remains a Web responsibility. Do not paginate or truncate the server map path.

```csharp
[Fact]
public async Task GetTracksAsync_ReturnsAllTenThousandPathPointsWithinTenSeconds()
{
    await using var db = MobileTestHelpers.CreateDb();
    var start = DateTimeOffset.Parse("2026-06-10T00:00:00Z");
    var expectedIds = new HashSet<string>(StringComparer.Ordinal);
    for (var index = 0; index < 10_000; index++)
    {
        var id = Guid.NewGuid();
        expectedIds.Add(id.ToString());
        db.Set<MobileLocationPointEntity>().Add(new MobileLocationPointEntity
        {
            Id = id,
            UserId = MobileTestHelpers.UserId,
            DeviceId = "pixel-9",
            RecordedAtUtc = start.AddSeconds(index),
            Latitude = 31.20m + index * 0.000001m,
            Longitude = 121.40m + index * 0.000001m,
            HorizontalAccuracyMeters = 8m,
            Provider = "gps",
            Source = "auto",
            Quality = "usable",
            RawJson = "{}",
            CreatedAt = start.AddSeconds(index)
        });
    }
    await db.SaveChangesAsync();
    using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));

    var tracks = await Service(db).GetTracksAsync(new MobileLocationQueryRequest(
        RangeStartUtc: start,
        RangeEndUtc: start.AddDays(30)), timeout.Token);

    var returnedIds = tracks.SelectMany(track => track.Segments)
        .SelectMany(segment => segment.Path)
        .Select(point => point.Id)
        .ToHashSet(StringComparer.Ordinal);
    Assert.Equal(10_000, returnedIds.Count);
    Assert.True(expectedIds.SetEquals(returnedIds));
}
```

- [ ] **Step 5: Run focused backend and Web type tests**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileLocationAggregationServiceTests|FullyQualifiedName~MobileWebContractTests"
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.android-embed.json
```

Expected: PASS with the new fields serialized and consumed.

- [ ] **Step 6: Commit the location contract**

```powershell
git add src/modules/Pim.Module.Mobile tests/Pim.UnitTests/Mobile src/client-web/src/api/mobile.ts
git commit -m "feat: expose complete mobile segment evidence"
```

## Task 4: Build The Server-Only Today Embed Page

**Files:**
- Modify: `src/client-web/src/pages/embed/AndroidTodayPage.tsx`
- Create: `src/client-web/src/components/mobile/AndroidTodayMapPreview.tsx`
- Modify: `src/client-web/src/embed/AndroidEmbedApp.tsx`
- Create: `tests/client-web/androidEmbedFixtures.ts`
- Test: `tests/client-web/androidEmbedPages.test.tsx`

- [ ] **Step 1: Write failing state tests**

Cover these explicit render states with injected query results:

- loading;
- content with generated time, track map, stays, distance, usable/rejected points, completeness, foreground total, top apps;
- server-empty;
- partial when one query succeeds and another fails;
- auth-required;
- request-error with retry.

Assert `embed-state` reports route, state, generatedAtUtc, and hasData for each terminal render.

Keep query hooks outside the pure renderer and use this complete fixture shape in `androidEmbedPages.test.tsx`:

```tsx
type AndroidTodayRenderState =
  | { kind: 'loading' }
  | { kind: 'content'; model: AndroidTodayUiModel }
  | { kind: 'server-empty'; generatedAtUtc: string }
  | { kind: 'partial'; model: AndroidTodayUiModel; failedSections: string[] }
  | { kind: 'auth-required' }
  | { kind: 'error'; code: string };

interface AndroidTodayUiModel {
  generatedAtUtc: string;
  trackCount: number;
  stayCount: number;
  distanceMeters: number;
  usablePointCount: number;
  rejectedPointCount: number;
  completenessPercent: number;
  foregroundSeconds: number;
  topApps: Array<{ packageName: string; displayName: string; foregroundSeconds: number }>;
  tracks: MobileLocationTrack[];
}

const todayFixture = (overrides: Partial<AndroidTodayUiModel> = {}): AndroidTodayUiModel => ({
  generatedAtUtc: '2026-07-10T08:00:00Z',
  trackCount: 1,
  stayCount: 2,
  distanceMeters: 3600,
  usablePointCount: 12,
  rejectedPointCount: 1,
  completenessPercent: 92,
  foregroundSeconds: 5400,
  topApps: [{ packageName: 'com.example.mail', displayName: '邮件', foregroundSeconds: 1800 }],
  tracks: [mobileTrackFixture()],
  ...overrides,
});

const state: AndroidTodayRenderState = { kind: 'content', model: todayFixture() };
const html = renderToStaticMarkup(<AndroidTodayContent state={state} />);
assert.match(html, /邮件/);
assert.match(html, /3\.6 km/);
assert.deepEqual(toTodayEmbedState(state), {
  protocolVersion: 1,
  type: 'embed-state',
  route: 'today',
  state: 'content',
  generatedAtUtc: '2026-07-10T08:00:00Z',
  hasData: true,
  errorCode: null,
});

const mappingCases: Array<[AndroidTodayRenderState, string, boolean, string | null]> = [
  [{ kind: 'loading' }, 'loading', false, null],
  [{ kind: 'content', model: todayFixture() }, 'content', true, null],
  [{ kind: 'server-empty', generatedAtUtc: '2026-07-10T08:00:00Z' }, 'server-empty', false, null],
  [{ kind: 'partial', model: todayFixture(), failedSections: ['usage'] }, 'partial', true, 'usage'],
  [{ kind: 'auth-required' }, 'auth-required', false, 'auth-required'],
  [{ kind: 'error', code: 'timeout' }, 'error', false, 'timeout'],
];
for (const [renderState, expectedState, hasData, errorCode] of mappingCases) {
  const message = toTodayEmbedState(renderState);
  assert.equal(message.state, expectedState);
  assert.equal(message.hasData, hasData);
  assert.equal(message.errorCode, errorCode);
}

function mobileTrackFixture(): MobileLocationTrack {
  const path = [
    { latitude: 31.230416, longitude: 121.473701, recordedAtUtc: '2026-07-10T07:00:00Z', horizontalAccuracyMeters: 8, quality: 'usable' },
    { latitude: 31.235000, longitude: 121.480000, recordedAtUtc: '2026-07-10T07:30:00Z', horizontalAccuracyMeters: 12, quality: 'usable' },
  ];
  return {
    id: 'track-1',
    deviceId: 'pixel-9',
    startUtc: '2026-07-10T07:00:00Z',
    endUtc: '2026-07-10T07:30:00Z',
    distanceMeters: 3600,
    durationSeconds: 1800,
    pointCount: 2,
    segmentCount: 1,
    bounds: { minLatitude: 31.230416, minLongitude: 121.473701, maxLatitude: 31.235, maxLongitude: 121.48 },
    qualityFlags: [],
    segments: [{
      id: 'segment-1',
      trackId: 'track-1',
      deviceId: 'pixel-9',
      kind: 'move',
      startUtc: '2026-07-10T07:00:00Z',
      endUtc: '2026-07-10T07:30:00Z',
      localStart: '2026-07-10 15:00',
      localEnd: '2026-07-10 15:30',
      durationSeconds: 1800,
      distanceMeters: 3600,
      pointCount: 2,
      averageSpeedMetersPerSecond: 2,
      averageAccuracyMeters: 10,
      maxAccuracyMeters: 12,
      providerMix: { gps: 2 },
      hasAltitude: true,
      altitudePointCount: 2,
      quality: 'usable',
      qualityFlags: [],
      bounds: { minLatitude: 31.230416, minLongitude: 121.473701, maxLatitude: 31.235, maxLongitude: 121.48 },
      path,
    }],
  };
}
```

Place `mobileTrackFixture()` in `tests/client-web/androidEmbedFixtures.ts` and reuse it in Tracks tests; do not replace required fields with casts. `AndroidTodayPage` owns queries/effects, while exported `AndroidTodayContent` and `toTodayEmbedState` make all render/message mappings deterministic.

- [ ] **Step 2: Query a device-local half-open day only after auth**

Use `resolveBrowserTimeZone()`, current date in that zone, and explicit UTC start/end for:

- mobile location overview;
- location tracks;
- usage summary/overview;
- top-app chart/summary.

All values are server responses. Native pending rows never enter metrics or map points.

- [ ] **Step 3: Implement the compact Today layout**

Use an unframed page root with stable sections:

```tsx
<main data-embed-route="today" className="min-h-full space-y-3 bg-white p-3 text-slate-950">
  <AndroidTodayMapPreview tracks={tracks} />
  <TodayLocationMetrics overview={locationOverview} />
  <TodayUsageMetrics overview={usageOverview} topApps={topApps} />
  <ServerDataTime generatedAtUtc={generatedAtUtc} timeZone={timeZone} />
</main>
```

No hero, desktop navigation, nested card, feature explanation, or duplicate native title.

- [ ] **Step 4: Implement map preview stability**

Preview has a fixed responsive aspect ratio/min-height, draws the real server path/stays, fits bounds after data change, and renders a visible path even when tiles fail. Empty map state does not fabricate a route.

- [ ] **Step 5: Run Today page tests**

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedPages.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.android-embed.json
```

Expected: PASS for every state and no API call before token.

- [ ] **Step 6: Commit Today embed**

```powershell
git add src/client-web/src/pages/embed/AndroidTodayPage.tsx src/client-web/src/components/mobile/AndroidTodayMapPreview.tsx src/client-web/src/embed/AndroidEmbedApp.tsx tests/client-web/androidEmbedFixtures.ts tests/client-web/androidEmbedPages.test.tsx
git commit -m "feat: add server-backed android today view"
```

## Task 5: Build Tracks Filters, Gap/Accuracy Map, Segment Detail, And Real Pagination

**Files:**
- Modify: `src/client-web/src/pages/embed/AndroidTracksPage.tsx`
- Create: `src/client-web/src/components/mobile/AndroidTracksDashboard.tsx`
- Create: `src/client-web/src/components/mobile/MobileLocationMapViewport.tsx`
- Create: `src/client-web/src/components/mobile/locationGapSegments.ts`
- Modify: `src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx`
- Modify: `src/client-web/src/components/mobile/LocationHistoryMap.tsx`
- Modify: `src/client-web/src/components/mobile/LocationRawPointTable.tsx`
- Modify: `src/client-web/src/components/mobile/LocationSegmentDetail.tsx`
- Test: `tests/client-web/androidTracksInteractions.test.tsx`
- Test: `tests/client-web/locationGapSegments.test.ts`

- [ ] **Step 1: Write failing filter/selection/pagination tests**

Simulate Today/7d/30d/custom, device, max accuracy, include rejected, segment selection, and Load More. Assert filter changes reset segment/cursor but `sync-completed` invalidation preserves filter values.

Use a pure state reducer for the reset/preservation contract before wiring React controls:

```ts
const initial: TracksInteractionState = {
  range: { kind: '7d' },
  deviceId: 'pixel-9',
  maxAccuracyMeters: 50,
  includeRejected: false,
  selectedSegmentId: 'segment-1',
  loadedPointIds: ['point-1'],
  nextCursor: 'cursor-2',
  invalidationGeneration: 0,
};

const changed = reduceTracksInteraction(initial, { type: 'set-range', range: { kind: '30d' } });
assert.equal(changed.selectedSegmentId, null);
assert.deepEqual(changed.loadedPointIds, []);
assert.equal(changed.nextCursor, null);

const invalidated = reduceTracksInteraction(initial, { type: 'sync-completed' });
assert.deepEqual(invalidated.range, initial.range);
assert.equal(invalidated.deviceId, 'pixel-9');
assert.equal(invalidated.maxAccuracyMeters, 50);
assert.equal(invalidated.includeRejected, false);
assert.equal(invalidated.selectedSegmentId, 'segment-1');
```

Define the exhaustive reducer in `AndroidTracksDashboard.tsx`:

```ts
export type TracksRange =
  | { kind: 'today' | '7d' | '30d' }
  | { kind: 'custom'; startDate: string; endDate: string };

export interface TracksInteractionState {
  range: TracksRange;
  deviceId: string | null;
  maxAccuracyMeters: number;
  includeRejected: boolean;
  selectedSegmentId: string | null;
  loadedPointIds: string[];
  nextCursor: string | null;
  invalidationGeneration: number;
}

type TracksInteractionAction =
  | { type: 'set-range'; range: TracksRange }
  | { type: 'set-device'; deviceId: string | null }
  | { type: 'set-accuracy'; maxAccuracyMeters: number }
  | { type: 'set-rejected'; includeRejected: boolean }
  | { type: 'select-segment'; segmentId: string | null }
  | { type: 'append-page'; pointIds: string[]; nextCursor: string | null }
  | { type: 'sync-completed' };

const resetResults = (state: TracksInteractionState) => ({
  ...state,
  selectedSegmentId: null,
  loadedPointIds: [],
  nextCursor: null,
});

export function reduceTracksInteraction(
  state: TracksInteractionState,
  action: TracksInteractionAction,
): TracksInteractionState {
  switch (action.type) {
    case 'set-range': return { ...resetResults(state), range: action.range };
    case 'set-device': return { ...resetResults(state), deviceId: action.deviceId };
    case 'set-accuracy': return { ...resetResults(state), maxAccuracyMeters: action.maxAccuracyMeters };
    case 'set-rejected': return { ...resetResults(state), includeRejected: action.includeRejected };
    case 'select-segment': return { ...state, selectedSegmentId: action.segmentId, loadedPointIds: [], nextCursor: null };
    case 'append-page': return {
      ...state,
      loadedPointIds: [...new Set([...state.loadedPointIds, ...action.pointIds])],
      nextCursor: action.nextCursor,
    };
    case 'sync-completed': return { ...state, invalidationGeneration: state.invalidationGeneration + 1 };
  }
}
```

The query invalidation effect consumes `invalidationGeneration`; filter values and the selected segment remain unchanged after sync.

- [ ] **Step 2: Add deterministic gap segments between adjacent same-device tracks**

```ts
export interface MobileLocationGapSegment {
  id: string;
  kind: 'gap';
  deviceId: string;
  startUtc: string;
  endUtc: string;
  durationSeconds: number;
  path: [MobileLocationPathPoint, MobileLocationPathPoint];
}
```

Create a gap only when the next track begins after the previous track ends. Sort by device/time; never connect different devices.

- [ ] **Step 3: Centralize Leaflet viewport and tile state**

`MobileLocationMapViewport` owns TileLayer, fitBounds/invalidateSize, tile loading/error, and two modes:

- preview: stable `aspect-ratio: 4 / 3`, minimum 240px;
- full: height constrained by `clamp(360px, 58vh, 620px)` without viewport-scaled font.

Draw all polylines, dashed gaps, stay markers, and `Circle` accuracy overlays only for selected or low-quality points. For large datasets, reduce markers/circles to selected/quality points while retaining complete polylines.

- [ ] **Step 4: Implement `useInfiniteQuery` for raw points**

```ts
const pointsQuery = useInfiniteQuery({
  queryKey: ['mobile-location-segment-points', selectedSegmentId, locationQuery],
  enabled: Boolean(selectedSegmentId),
  initialPageParam: null as string | null,
  queryFn: ({ pageParam }) => api.getSegmentPoints(selectedSegmentId!, {
    ...locationQuery,
    cursor: pageParam,
    pageSize: 100,
  }),
  getNextPageParam: page => page.hasMore ? page.nextCursor : undefined,
});
```

Flatten pages without duplicate IDs. `LocationRawPointTable` receives `hasMore`, `isLoadingMore`, `onLoadMore` and keeps selection stable.

- [ ] **Step 5: Render complete selected-segment evidence**

Show duration, distance, point count, average speed, average/max accuracy, provider mix, altitude availability, quality flags, and gap-specific evidence. A selected gap shows no fabricated raw points.

- [ ] **Step 6: Render explicit states and publish `embed-state`**

Handle loading, server-empty, filtered-empty, partial, error, auth-required, and content. Report latest server generated time without replacing it with local refresh time.

- [ ] **Step 7: Run focused Tracks tests**

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/locationGapSegments.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/androidTracksInteractions.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.android-embed.json
```

Expected: PASS for filters, gap ownership, selection, provider/altitude detail, pagination, and preserved filters after invalidation.

- [ ] **Step 8: Commit Tracks embed**

```powershell
git add src/client-web/src/pages/embed/AndroidTracksPage.tsx src/client-web/src/components/mobile src/client-web/src/embed/AndroidEmbedApp.tsx tests/client-web
git commit -m "feat: add interactive android tracks view"
```

## Task 6: Add Browser-Level Embed, Map, And Responsive Verification

**Files:**
- Create: `tests/client-web/androidEmbedVisualAudit.test.ts`
- Modify: `tests/client-web/androidEmbedFixtures.ts`
- Create: `tests/client-web/androidMapTileFixture.ts`
- Modify: `src/client-web/package.json`

- [ ] **Step 1: Write the failing Playwright harness**

Reuse the repository's dynamic-port Vite pattern. Test viewports are exactly:

```ts
const viewports = [
  { width: 360, height: 800 },
  { width: 412, height: 915 },
] as const;
```

Install a fake trusted `window.pimNative` before page scripts, and route every `/api/v1/mobile/**` request to deterministic loading/content/empty/error fixtures.

Use this init-script bridge fixture so token timing and outbound messages are observable:

```ts
await page.addInitScript(() => {
  const outbound: Array<Record<string, unknown>> = [];
  const bridge = {
    onmessage: null as ((event: MessageEvent<string>) => void) | null,
    postMessage(raw: string) {
      const message = JSON.parse(raw) as Record<string, unknown>;
      outbound.push(message);
      if (message.type === 'request-access-token' || message.type === 'refresh-access-token') {
        const token = message.type === 'request-access-token' ? 'embed-test-token' : 'embed-refresh-token';
        queueMicrotask(() => bridge.onmessage?.(new MessageEvent('message', {
          data: JSON.stringify({
            protocolVersion: 1,
            type: 'access-token',
            requestId: message.requestId,
            accessToken: token,
            expiresAtUtc: '2026-07-10T12:00:00Z',
          }),
        })));
      }
    },
  };
  Object.assign(window, {
    pimNative: bridge,
    __pimEmbedTest: {
      outbound,
      deliver(message: Record<string, unknown>) {
        bridge.onmessage?.(new MessageEvent('message', { data: JSON.stringify(message) }));
      },
    },
  });
});
```

`androidEmbedFixtures.ts` exports `mobileTrackFixture()` from Task 4 plus `contentResponseFor(pathname)` for exactly `/devices`, `/location/analytics/overview`, `/location/analytics/tracks`, `/analytics/overview`, `/analytics/charts`, and `/location/analytics/segments/{id}/points`. Each response is `{ code: 0, message: 'OK', data: ... }`, uses the same `generatedAt`, and the points fixture has two pages with cursor `points-page-2`. Throw on any unhandled `/api/v1/mobile/**` path so a new request cannot silently receive the wrong fixture.

Implement the dispatcher with typed fixtures; keep the full `mobileTrackFixture()` body from Task 4 in this shared file:

```ts
const generatedAt = '2026-07-10T08:00:00Z';
const range = {
  rangeStartUtc: '2026-07-09T16:00:00Z',
  rangeEndUtc: '2026-07-10T16:00:00Z',
  timezone: 'Asia/Shanghai',
  localStartDate: '2026-07-10',
  localEndDate: '2026-07-10',
};

const locationOverview: MobileLocationAnalyticsOverview = {
  range,
  generatedAt,
  pointCount: 13,
  usablePointCount: 12,
  rejectedPointCount: 1,
  activeSpanSeconds: 1800,
  distanceMeters: 3600,
  stayCount: 2,
  longestStaySeconds: 600,
  averageAccuracyMeters: 10,
  qualityIssueCount: 0,
  qualityFlags: [],
};

const usageOverview: MobileAnalyticsOverview = {
  range,
  generatedAt,
  isStale: false,
  totalForegroundSeconds: 5400,
  dailyAverageSeconds: 5400,
  previousPeriodChange: 0,
  highestUseLocalDate: '2026-07-10',
  peakLocalHour: 15,
  appCount: 1,
  switchOrPickupCount: 4,
  completeness: 0.92,
  quality: {
    usageEventsCoverage: 1,
    fallbackShare: 0,
    missingMetadataAppCount: 0,
    systemNoiseShare: 0,
    shortEventShare: 0,
    failedOrPartialSyncBatchCount: 0,
    lastSyncAt: generatedAt,
    qualityFlags: [],
  },
  goalProgress: null,
  anomalies: [],
  suggestions: [],
};

const topApps: MobileAnalyticsChart[] = [{
  key: 'top-apps',
  title: 'Top Apps',
  chartType: 'top-apps',
  unit: 'seconds',
  points: [{ key: 'mail', label: '邮件', value: 1800, foregroundSeconds: 1800, packageName: 'com.example.mail' }],
}];

const points = [
  {
    id: 'point-1', deviceId: 'pixel-9', recordedAtUtc: '2026-07-10T07:00:00Z', submittedAtUtc: '2026-07-10T07:00:05Z',
    latitude: 31.230416, longitude: 121.473701, horizontalAccuracyMeters: 8, provider: 'gps', sourceKind: 'auto',
    altitudeMeters: 12.5, verticalAccuracyMeters: 3, speedMetersPerSecond: 1, speedAccuracyMetersPerSecond: 0.5,
    bearingDegrees: 90, bearingAccuracyDegrees: 5, isAutoSubmitted: true, quality: 'usable', rawJson: '{}',
  },
  {
    id: 'point-2', deviceId: 'pixel-9', recordedAtUtc: '2026-07-10T07:30:00Z', submittedAtUtc: '2026-07-10T07:30:05Z',
    latitude: 31.235, longitude: 121.48, horizontalAccuracyMeters: 12, provider: 'gps', sourceKind: 'auto',
    altitudeMeters: 13, verticalAccuracyMeters: 3, speedMetersPerSecond: 1, speedAccuracyMetersPerSecond: 0.5,
    bearingDegrees: 90, bearingAccuracyDegrees: 5, isAutoSubmitted: true, quality: 'usable', rawJson: '{}',
  },
] satisfies MobileLocationPoint[];

const ok = (data: unknown) => ({ code: 0, message: 'OK', data });

export function contentResponseFor(url: URL) {
  const path = url.pathname.replace(/^\/api\/v1\/mobile/, '');
  if (path === '/devices') return ok([{
    id: 'device-row-1',
    deviceId: 'pixel-9',
    androidIdHash: 'test-hash',
    displayName: 'Pixel 9',
    manufacturer: 'Google',
    brand: 'google',
    model: 'Pixel 9',
    androidVersion: '16',
    sdkInt: 36,
    appVersion: '1.0.0',
    metadataJson: '{}',
    firstSeenAt: '2026-07-01T00:00:00Z',
    lastSeenAt: generatedAt,
    lastHeartbeatAt: generatedAt,
    lastSyncAt: generatedAt,
    isActive: true,
  } satisfies MobileDevice]);
  if (path === '/location/analytics/overview') return ok(locationOverview);
  if (path === '/location/analytics/tracks') return ok([mobileTrackFixture()]);
  if (path === '/analytics/overview') return ok(usageOverview);
  if (path === '/analytics/charts') return ok(topApps);
  if (/^\/location\/analytics\/segments\/[^/]+\/points$/.test(path)) {
    const secondPage = url.searchParams.get('cursor') === 'points-page-2';
    return ok(secondPage
      ? { items: [points[1]], nextCursor: null, hasMore: false }
      : { items: [points[0]], nextCursor: 'points-page-2', hasMore: true });
  }
  throw new Error(`Unhandled Android embed fixture path: ${url.pathname}`);
}
```

- [ ] **Step 2: Add a deterministic local tile fixture**

`androidMapTileFixture.ts` exports a valid 256×256 PNG `Buffer` from a fixed base64 string. Intercept `https://*.tile.openstreetmap.org/**` and fulfill with that buffer, `image/png`, and cache disabled. No test may access public tiles.

```ts
export const ANDROID_MAP_TILE_PNG = Buffer.from(
  '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',
  'base64',
);

assert.equal(ANDROID_MAP_TILE_PNG.readUInt32BE(16), 256);
assert.equal(ANDROID_MAP_TILE_PNG.readUInt32BE(20), 256);

await page.route('https://*.tile.openstreetmap.org/**', route => route.fulfill({
  status: 200,
  contentType: 'image/png',
  headers: { 'Cache-Control': 'no-store' },
  body: ANDROID_MAP_TILE_PNG,
}));
```

- [ ] **Step 3: Assert route isolation and auth behavior**

For both routes:

- before token delivery, API request count is zero;
- after token delivery, Authorization is `Bearer embed-test-token`;
- Sidebar, QuickNote, login form, desktop title, and duplicate bottom navigation are absent;
- 401 causes one refresh request and one retry;
- `auth-cleared` renders auth-required;
- `sync-completed` causes fresh mobile requests but preserves selected filters.

- [ ] **Step 4: Assert map and layout evidence**

```ts
const audit = await page.evaluate(() => ({
  overflow: document.documentElement.scrollWidth - window.innerWidth,
  loadedTiles: Array.from(document.querySelectorAll<HTMLImageElement>('.leaflet-tile-loaded'))
    .filter(tile => tile.naturalWidth > 0 && tile.naturalHeight > 0).length,
  pathCount: document.querySelectorAll('path.leaflet-interactive').length,
  clippedButtons: Array.from(document.querySelectorAll('button'))
    .filter(button => button.scrollWidth > button.clientWidth + 2)
    .map(button => button.textContent?.trim()),
}));

assert.ok(audit.overflow <= 4);
assert.ok(audit.loadedTiles > 0);
assert.ok(audit.pathCount > 0);
assert.deepEqual(audit.clippedButtons, []);
```

Capture the map locator screenshot to a temporary test directory and assert the PNG buffer is nontrivial. Delete temporary screenshots in `finally`.

- [ ] **Step 5: Cover every explicit page state**

Run loading, server-empty, filtered-empty, partial, error, content, and auth-required. Assert each has meaningful Chinese primary text and no blank white main area.

- [ ] **Step 6: Add the npm test script and existing-CI lifecycle hook**

Add:

```json
{
  "scripts": {
    "test:android-embed": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedBridge.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedAuth.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedTimeRange.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedRoutes.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedPages.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/locationGapSegments.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/androidTracksInteractions.test.tsx && npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.android-embed.json && npm --prefix src/client-web exec tsx -- tests/client-web/androidEmbedVisualAudit.test.ts",
    "pretest:schedule-workbench-complete": "npm run test:android-embed"
  }
}
```

Merge these keys into the existing scripts object. Preserve the complete existing `test:schedule-workbench-complete` command unchanged; npm automatically runs the pre-script in Web CI.

- [ ] **Step 7: Run the aggregate embed test twice**

```powershell
npm --prefix src/client-web run test:android-embed
npm --prefix src/client-web run test:android-embed
```

Expected: both runs PASS, proving no leaked port/browser/listener state.

- [ ] **Step 8: Run the existing complete Web gate**

```powershell
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
```

Expected: old schedule tests, new pre-hook embed tests, and production build all PASS.

- [ ] **Step 9: Commit browser verification**

```powershell
git add src/client-web/package.json tests/client-web/androidEmbedVisualAudit.test.ts tests/client-web/androidEmbedFixtures.ts tests/client-web/androidMapTileFixture.ts
git commit -m "test: verify android embed maps and layouts"
```

## Task 7: Advertise `androidEmbedV1` Only After Embed Routes Pass

**Files:**
- Modify: `src/Pim.Api/Endpoints/VersionEndpoints.cs`
- Modify: `tests/Pim.UnitTests/Api/VersionEndpointTests.cs`

- [ ] **Step 1: Change the capability test first**

```csharp
[Fact]
public void Capabilities_AdvertiseBothShippedAndroidContracts()
{
    Assert.Equal(
        [VersionEndpoints.MobileItemResultsV1, VersionEndpoints.AndroidEmbedV1],
        VersionEndpoints.Capabilities);
}
```

- [ ] **Step 2: Run the focused test and verify failure**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~VersionEndpointTests"
```

Expected: FAIL because `AndroidEmbedV1` is not yet advertised.

- [ ] **Step 3: Add the capability in the same branch as passing routes**

```csharp
public const string AndroidEmbedV1 = "androidEmbedV1";
public static IReadOnlyList<string> Capabilities { get; } =
    [MobileItemResultsV1, AndroidEmbedV1];
```

Return `Capabilities` from `/api/version`.

- [ ] **Step 4: Verify endpoint and Web routes together**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~VersionEndpointTests"
npm --prefix src/client-web run test:android-embed
```

Expected: PASS; server claim and route evidence are in one commit.

- [ ] **Step 5: Commit the capability**

```powershell
git add src/Pim.Api/Endpoints/VersionEndpoints.cs tests/Pim.UnitTests/Api/VersionEndpointTests.cs
git commit -m "feat: advertise android embed capability"
```

## Task 8: Implement Android Bridge Protocol, Session, Token, And Navigation Policies

**Files:**
- Modify: `src/client-android/app/build.gradle.kts`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedWebProtocol.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedWebSessionController.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedTokenProvider.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedNavigationPolicy.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/web/EmbeddedWebState.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/web/EmbeddedWebProtocolTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/web/EmbeddedNavigationPolicyTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/web/EmbeddedWebSessionControllerTest.kt`

- [ ] **Step 1: Add WebKit and write failing protocol tests**

Add:

```kotlin
implementation("androidx.webkit:webkit:1.12.1")
```

Then test valid request-access-token, refresh-access-token, embed-state, unknown version/type, malformed JSON, and token-free log descriptions.

- [ ] **Step 2: Define Kotlin wire types matching TypeScript exactly**

```kotlin
sealed interface EmbeddedWebIncoming {
    data class RequestAccessToken(val requestId: String) : EmbeddedWebIncoming
    data class RefreshAccessToken(val requestId: String) : EmbeddedWebIncoming
    data class EmbedState(
        val route: EmbeddedRoute,
        val state: EmbeddedPageState,
        val generatedAtUtc: String?,
        val hasData: Boolean,
        val errorCode: String?
    ) : EmbeddedWebIncoming
}

enum class EmbeddedRoute { Today, Tracks }
enum class EmbeddedPageState { Loading, Content, ServerEmpty, FilteredEmpty, Partial, Error, AuthRequired }
```

Use `org.json` to keep the app module dependency shape; serialize with `protocolVersion=1` and exact kebab-case wire names.

- [ ] **Step 3: Write failing navigation policy tests**

```kotlin
@Test
fun allowsOnlyConfiguredOriginAndEmbedPaths() {
    val policy = EmbeddedNavigationPolicy(PimServerEndpoints.from("https://pim.example/api/v1/"))

    assertTrue(policy.isAllowedMainFrame("https://pim.example/embed/android/today"))
    assertTrue(policy.isAllowedMainFrame("https://pim.example/embed/android/tracks?range=7d"))
    assertFalse(policy.isAllowedMainFrame("https://pim.example/location-history"))
    assertFalse(policy.isAllowedMainFrame("https://evil.example/embed/android/today"))
}
```

- [ ] **Step 4: Implement a single token provider**

`EmbeddedTokenProvider.current()` returns access token/expiry through Phase 1 `AuthSessionStore`. `refreshOnce()` delegates to the single Phase 1 `AuthRefreshCoordinator`, then rereads the rotated access token; a rejected refresh returns null after that coordinator clears native auth. It never creates a second refresh mutex and never exposes the refresh token to protocol messages or logs.

- [ ] **Step 5: Implement session event flow**

`EmbeddedWebSessionController`:

- handles main-frame trusted incoming messages;
- responds to each request ID once;
- allows one refresh request per failed Web request ID;
- exposes `StateFlow<Map<EmbeddedRoute, EmbeddedWebState>>` from `embed-state`;
- broadcasts `sync-completed` after a `SyncRun` reaches server-confirmed success/partial success;
- broadcasts `auth-cleared` on logout;
- drops state/listeners when a WebView unregisters.

- [ ] **Step 6: Run Android bridge tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.web.*" --no-daemon
```

Expected: PASS for parsing, exact-origin/path, single refresh, auth clear, and state publication.

- [ ] **Step 7: Commit Android bridge contracts**

```powershell
git add src/client-android/app/build.gradle.kts src/client-android/app/src/main/java/com/pim/app/web src/client-android/app/src/test/java/com/pim/app/web
git commit -m "feat: add secure android web session controller"
```

## Task 9: Replace The Broken WebView With A Trusted Lifecycle-Aware Host

**Files:**
- Replace: `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/web/PimWebViewScreenTest.kt`

- [ ] **Step 1: Write failing instrumentation tests against MockWebServer**

Serve a page that calls `window.pimNative.postMessage()` and records the reply. Assert:

- trusted main frame receives access token reply;
- untrusted origin and subframe receive no reply;
- allowed embed navigation stays inside;
- desktop path/external origin opens an external Intent;
- HTTP/Web resource error renders native error state;
- disposal calls `stopLoading`, clears clients/listener, loads `about:blank`, and destroys WebView.

- [ ] **Step 2: Build and lock the WebView before loading**

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    allowFileAccess = false
    allowContentAccess = false
    databaseEnabled = false
    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
}
```

For cleartext configured origin, allow same-origin HTTP while showing the native security warning; never enable mixed HTTP content under HTTPS.

- [ ] **Step 3: Register the trusted message listener**

Use `WebViewCompat.addWebMessageListener` before `loadUrl`, object name `pimNative`, and the resolver's path-free `trustedOrigin` as the only allowed origin rule. Reject `isMainFrame=false` or source origin mismatch. Reply through `JavaScriptReplyProxy`; never inject token with `evaluateJavascript` or localStorage.

- [ ] **Step 4: Enforce navigation and external opening**

`WebViewClient.shouldOverrideUrlLoading()` permits only exact origin plus `/embed/android/today|tracks`. All other HTTP(S) URLs launch `ACTION_VIEW`; unsupported schemes are blocked and surfaced. Back consumes allowed Web history before returning to the native tab.

- [ ] **Step 5: Render native loading/auth/network/HTTP/resource errors**

Use `EmbeddedWebHostState` with `Loading`, `Content`, `AuthRequired`, `NetworkError`, `HttpError`, `ResourceError`. Never leave a blank WebView visible after main-frame failure; provide retry and Status actions.

- [ ] **Step 6: Destroy WebView from Compose disposal**

Use `DisposableEffect(webView)` and unregister the session listener. Do not retain Activity context in the singleton session controller.

- [ ] **Step 7: Run WebView instrumentation**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.web.PimWebViewScreenTest --no-daemon
```

Expected: PASS for trusted token, navigation, every native error surface, and destruction.

- [ ] **Step 8: Commit the WebView host**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt src/client-android/app/src/androidTest/java/com/pim/app/web/PimWebViewScreenTest.kt
git commit -m "feat: host trusted android embed webviews"
```

## Task 10: Add Native Today/Tracks Wrappers And Cross-Page Refresh

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/today/TodayWrapperContentTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/tracks/TracksWrapperContentTest.kt`

- [ ] **Step 1: Write failing native wrapper tests**

```kotlin
@Test
fun todayKeepsLocalTransferStateVisibleAboveServerContent() {
    compose.setContent {
        TodayContent(
            state = todayState(pending = 12, phase = SyncPhase.UploadingLocations, serverGeneratedAt = "2026-07-10T08:00:00Z"),
            onOpenTransfer = {},
            webContent = { Text("web-content") }
        )
    }

    compose.onNodeWithText("12 条待传").assertExists()
    compose.onNodeWithText("正在上传定位").assertExists()
    compose.onNodeWithText("服务器数据：").assertExists()
    compose.onNodeWithText("web-content").assertExists()
}

private fun todayState(
    pending: Int,
    phase: SyncPhase?,
    serverGeneratedAt: String?,
    pageState: EmbeddedPageState = EmbeddedPageState.Content,
    collectionDesired: Boolean = true
) = EmbeddedTabHeaderState(
    collectionHealth = OperationalHealth.Healthy,
    collectionDesired = collectionDesired,
    pendingBusinessCount = pending,
    syncPhase = phase,
    lastSuccessfulSyncAtUtcMillis = 1_000L,
    nextAttemptAtUtcMillis = null,
    serverGeneratedAtUtc = serverGeneratedAt,
    serverPageState = pageState,
    cleartextWarning = false
)

@Test
fun contextualEmptyCopyUsesOnlyNativeTransferFacts() {
    val cases = listOf(
        todayState(12, null, null, EmbeddedPageState.ServerEmpty) to "手机已采集，仍在等待上传",
        todayState(0, null, null, EmbeddedPageState.ServerEmpty, collectionDesired = false) to "尚未开始采集",
        todayState(0, SyncPhase.Succeeded, "2026-07-10T08:00:00Z", EmbeddedPageState.ServerEmpty) to "服务器在所选范围内没有数据",
        todayState(0, SyncPhase.Failed, null, EmbeddedPageState.Error) to "服务器数据加载失败"
    )
    cases.forEach { (state, expected) ->
        assertEquals(expected, serverContextMessage(state))
    }
}

@Test
fun authAndErrorExposeExactRecoveryActions() {
    assertEquals(
        listOf(EmbeddedTabAction.Login, EmbeddedTabAction.OpenStatus),
        wrapperActions(todayState(0, null, null, EmbeddedPageState.AuthRequired))
    )
    assertEquals(
        listOf(EmbeddedTabAction.RetryWeb, EmbeddedTabAction.OpenStatus),
        wrapperActions(todayState(0, null, null, EmbeddedPageState.Error))
    )
}
```

Sync success refresh is covered by `EmbeddedWebSessionControllerTest`; Tracks filter stability is covered by the Task 5 reducer and browser test.

- [ ] **Step 2: Define one wrapper state shared by both tabs**

```kotlin
data class EmbeddedTabHeaderState(
    val collectionHealth: OperationalHealth,
    val collectionDesired: Boolean,
    val pendingBusinessCount: Int,
    val syncPhase: SyncPhase?,
    val lastSuccessfulSyncAtUtcMillis: Long?,
    val nextAttemptAtUtcMillis: Long?,
    val serverGeneratedAtUtc: String?,
    val serverPageState: EmbeddedPageState,
    val cleartextWarning: Boolean
)

enum class EmbeddedTabAction { OpenTransfer, Login, RetryWeb, OpenStatus }
```

Build from `OperationalStatusRepository` plus `EmbeddedWebSessionController` state.

- [ ] **Step 3: Implement stable native headers and non-nested scrolling**

Today/Tracks keep native title, collection health, pending/phase, server timestamp, transfer link, and cleartext warning. The Compose container does not wrap WebView in vertical scroll; Web owns body scroll below a stable native header.

- [ ] **Step 4: Implement contextual server-empty copy in native context**

- pending local data: `手机已采集，仍在等待上传`;
- collection off and no pending: `尚未开始采集`;
- successful sync and server truly empty: server-empty copy;
- server failure: retry plus Status access.

The context banner must not inject unsynced local coordinates or usage into Web metrics.

```kotlin
fun serverContextMessage(state: EmbeddedTabHeaderState): String? = when {
    state.serverPageState == EmbeddedPageState.Error -> "服务器数据加载失败"
    state.serverPageState != EmbeddedPageState.ServerEmpty -> null
    state.pendingBusinessCount > 0 -> "手机已采集，仍在等待上传"
    !state.collectionDesired -> "尚未开始采集"
    state.syncPhase == SyncPhase.Succeeded || state.syncPhase == SyncPhase.SucceededWithRejects ->
        "服务器在所选范围内没有数据"
    else -> "服务器暂未收到数据"
}

fun wrapperActions(state: EmbeddedTabHeaderState): List<EmbeddedTabAction> = when (state.serverPageState) {
    EmbeddedPageState.AuthRequired -> listOf(EmbeddedTabAction.Login, EmbeddedTabAction.OpenStatus)
    EmbeddedPageState.Error -> listOf(EmbeddedTabAction.RetryWeb, EmbeddedTabAction.OpenStatus)
    else -> listOf(EmbeddedTabAction.OpenTransfer)
}
```

- [ ] **Step 5: Broadcast sync and auth lifecycle events**

After server-confirmed `Succeeded` or `SucceededWithRejects`, broadcast `sync-completed`; Web invalidates mobile queries without resetting filters. Logout invokes `auth-cleared`, clears in-memory bridge tokens, calls `WebStorage.deleteOrigin(endpoints.trustedOrigin)`, and leaves collection intent unchanged.

- [ ] **Step 6: Keep the five-tab root authoritative**

Today and Tracks render wrappers directly under existing destinations. Transfer link selects Status and focuses transfer; Web cannot replace bottom navigation. Android back consumes allowed Web history, then stays in the selected tab.

- [ ] **Step 7: Run wrapper and full instrumentation tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.pim.app.ui.today --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.pim.app.ui.tracks --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

Expected: PASS with transfer header always visible, no nested-scroll conflict, sync refresh, auth clear, and native error states.

- [ ] **Step 8: Commit native wrappers**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/ui src/client-android/app/src/androidTest/java/com/pim/app/ui
git commit -m "feat: embed server today and tracks in android"
```

## Task 11: Verify Phase 2, Packaging, Coverage, And PR Checks

**Files:**
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`
- Create: `docs/superpowers/reports/2026-07-10-android-client-reliability-phase-2.md`

- [ ] **Step 1: Run focused and full backend tests**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileLocationAggregationServiceTests|FullyQualifiedName~MobileWebContractTests|FullyQualifiedName~VersionEndpointTests"
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 2: Run complete Web tests and build**

```powershell
npm --prefix src/client-web run test:android-embed
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
```

Expected: PASS; both viewports and map evidence pass. Keep generated `src/Pim.Api/wwwroot/` unstaged.

- [ ] **Step 3: Verify a deployable server contains embed assets**

With the freshly built Web output present only for this command:

```powershell
dotnet publish src/Pim.Api/Pim.Api.csproj --configuration Release --output build/phase2-api
Test-Path build/phase2-api/wwwroot/index.html
```

Expected: `True`, and a local published server returns 200 for both embed deep links through SPA fallback. Remove `build/phase2-api` and generated wwwroot after recording evidence; do not stage either.

- [ ] **Step 4: Run complete Android tests and release build**

```powershell
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Expected: PASS on Pixel_9.

- [ ] **Step 5: Perform Pixel_9 and Pixel_Tablet visual sanity**

On both AVDs, verify Today/Tracks loading, content, empty, auth, network and Web error states; map pan/zoom/selection; raw point load-more; transfer link; larger font; rotation/resume; back behavior. Record screenshots without committing private coordinates.

- [ ] **Step 6: Update REL-09 and REL-10 to Verified**

Evidence must include Web test command, map audit, Android instrumentation, server packaging/deep-link response, AVD identity, and commit hashes.

- [ ] **Step 7: Write and commit the Phase 2 report**

```powershell
git add docs/superpowers/reports
git commit -m "docs: record android server surface evidence"
```

- [ ] **Step 8: Inspect the branch for generated output**

```powershell
git status --short --branch
git diff --check origin/master...HEAD
git log --oneline origin/master..HEAD
```

Expected: no `wwwroot`, build, dist, Playwright screenshot, APK, or `.opencode/` entry.

- [ ] **Step 9: Push, open the PR, and watch all relevant checks**

```powershell
git push -u origin codex/android-server-data-surfaces
gh pr create --base master --head codex/android-server-data-surfaces --title "feat: embed server data surfaces in android" --body-file docs/superpowers/reports/2026-07-10-android-client-reliability-phase-2.md
gh pr checks --watch
```

Expected: Android, Web, and API workflows trigger and pass. Web's existing schedule command invokes `pretest:schedule-workbench-complete`, so embed Playwright evidence runs without editing workflow files.

## Phase 2 Completion Gate

Do not begin Phase 3 until the Phase 2 PR is merged and all are true:

- embed routes bypass desktop auth/layout and render only server-backed data;
- access token is memory-only and 401 refreshes exactly once through native;
- Today/Tracks use IANA/DST-safe half-open ranges;
- segment contract contains provider/altitude evidence;
- map shows paths, stays, accuracy, gaps, selection, tile failure and real pagination;
- deterministic Playwright passes at 360×800 and 412×915 with no overflow/blank map;
- Android WebView restricts origin/path, destroys cleanly, opens external links outside, and never shows a blank failure;
- native collection/transfer/server-time header remains visible and sync refresh preserves filters;
- deployable server package serves both deep links;
- full Android/Web/.NET gates and all relevant PR checks pass.

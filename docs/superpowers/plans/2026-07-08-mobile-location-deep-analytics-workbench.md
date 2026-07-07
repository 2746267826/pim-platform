# Mobile Location Deep Analytics Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the accepted Chinese-first mobile records and historical location deep analytics workbench, preserving the approved UI baseline while adding location analytics APIs, readable aggregation, tests, commits, push, and GitHub Actions verification.

**Architecture:** Keep raw mobile usage and location facts as source-of-truth, then add focused derived query/aggregation layers for workbench rendering. The web client uses shared mobile workbench layout primitives, a fixed `日期 × 小时` usage heatmap with a right-side detail panel, and a location map/segment detail workbench backed by paged location analytics endpoints.

**Tech Stack:** ASP.NET Core minimal APIs, EF Core, xUnit, React 19, React Query, TypeScript, Vite, Leaflet/react-leaflet, Node `assert` + `tsx` component tests, GitHub Actions.

---

## Required Spec

Implement against [2026-07-08-mobile-location-deep-analytics-workbench-design.md](C:/Users/a2746/Desktop/0/PIM/pim-platform-master/docs/superpowers/specs/2026-07-08-mobile-location-deep-analytics-workbench-design.md), especially the `已验收 UI 基线` section. The plan must not be simplified into a generic dashboard.

Final acceptance requires:

- `手机记录` and `历史位置` default to Beijing time, last 7 days, with `今天 / 7天 / 30天 / 自定义`.
- The mobile heatmap is a `日期 × 小时` matrix with date rows, hour columns, intensity cells, legend, selected state, and right-side detail.
- Clicking a heatmap cell updates selected detail without shrinking the global date range.
- App/category UI uses real Chinese labels first, package names as secondary diagnostic text.
- Historical location uses a map-first `轨迹地图 + 选中片段` layout, not the old point-list-first layout.
- Both pages match the approved first-screen structure: header, 6 filters, 6 metric cards, left main view, right detail, lower dual panels.
- Tests, local builds, commits, push to `master`, and GitHub Actions verification are completed.

## Subagent Execution Model

At implementation time, use `superpowers:subagent-driven-development` and dispatch no more than 14 simultaneous subagents. Use fresh subagents with disjoint write scopes:

1. **Agent A - Mobile Chinese copy and tests:** `src/client-web/src/api/mobile.ts`, `src/client-web/src/components/mobile/mobileFormatting.ts`, mobile copy tests.
2. **Agent B - Mobile usage heatmap:** `MobileUsageHeatmap.tsx`, new heatmap helpers, bucket detail component, related tests.
3. **Agent C - Mobile records page integration:** `MobileRecordsPage.tsx`, `MobileAnalyticsHeader.tsx`, `MobileInsightStrip.tsx`, charts/timeline wiring.
4. **Agent D - Location backend contracts:** mobile location DTOs, query service, endpoint tests.
5. **Agent E - Location backend aggregation:** `MobileLocationAggregationService`, location service query helpers, aggregation tests.
6. **Agent F - Location frontend types/API:** `src/client-web/src/api/mobile.ts`, type/path tests for location analytics.
7. **Agent G - Location frontend UI:** historical location page/dashboard/detail/raw table components and tests.
8. **Agent H - Leaflet map layers:** Leaflet segment rendering, marker styles, map tests.
9. **Agent I - Verification and CI:** build/test command matrix, GitHub Actions run tracking, final status audit.

The main agent owns integration, conflict resolution, final verification, commits, push, and GitHub Actions wait.

## File Structure

### Frontend Shared Mobile Workbench

- Create `src/client-web/src/components/mobile/MobileRangeToolbar.tsx`
  - Shared `今天 / 7天 / 30天 / 自定义 / 北京时间 / 刷新` toolbar.
- Create `src/client-web/src/components/mobile/MobileMetricGrid.tsx`
  - Shared 6-card metric grid.
- Create `src/client-web/src/components/mobile/WorkbenchPanel.tsx`
  - Single-level panel shell used by both pages.
- Create `src/client-web/src/components/mobile/mobileAnalyticsCopy.ts`
  - Real Chinese life categories, labels, and mojibake guard constants.

### Mobile Records Frontend

- Modify `src/client-web/src/pages/MobileRecordsPage.tsx`
  - Preserve global range when a heatmap bucket is selected.
  - Wire chart category/app clicks to filters.
  - Render accepted layout.
- Modify `src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx`
  - Align with shared toolbar/filter baseline or delegate to `MobileRangeToolbar`.
- Modify `src/client-web/src/components/mobile/MobileInsightStrip.tsx`
  - Align with 6 metric-card baseline.
- Modify `src/client-web/src/components/mobile/MobileUsageHeatmap.tsx`
  - Render `日期 × 小时` matrix and no repeated hour-number cells.
- Create `src/client-web/src/components/mobile/mobileHeatmapMatrix.ts`
  - Group raw buckets by local date/hour and keep category/app composition for detail.
- Create `src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx`
  - Right-side selected bucket detail.
- Modify `src/client-web/src/components/mobile/MobileChartsGrid.tsx`
  - Keep accepted `分类占比` and `Top App` visuals, clickable filters.
- Modify `src/client-web/src/components/mobile/MobileTimelineBlocks.tsx`
  - Keep readable behavior-block list and real Chinese text.

### Historical Location Backend

- Create `src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs`
  - Location analytics query, overview, track, segment, point page, bounds DTOs.
- Create `src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs`
  - Normalize range/timezone/page size/accuracy for location analytics.
- Create `src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs`
  - Compute overview, tracks, segments, points.
- Modify `src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs`
  - Add reusable point query helpers and remove hardcoded first-500 behavior from new analytics path.
- Modify `src/modules/Pim.Module.Mobile/MobileModule.cs`
  - Register services and map location analytics endpoints.

### Historical Location Frontend

- Modify `src/client-web/src/api/mobile.ts`
  - Add location analytics paths, DTOs, and API functions.
- Modify `src/client-web/src/pages/HistoricalLocationPage.tsx`
  - Use last 7 days Beijing range, location overview/tracks/points queries, selected segment state.
- Modify `src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx`
  - Render accepted location workbench layout.
- Create `src/client-web/src/components/mobile/LocationMetricStrip.tsx`
  - 6 location metric cards.
- Create `src/client-web/src/components/mobile/LocationSegmentDetail.tsx`
  - Right-side selected segment panel.
- Create `src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx`
  - Lower-left stop/move timeline panel.
- Create `src/client-web/src/components/mobile/LocationRawPointTable.tsx`
  - Lower-right raw point details.
- Modify `src/client-web/src/components/mobile/LocationHistoryMap.tsx`
  - Accept track/segment data and fallback view.
- Modify `src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx`
  - Render segment polylines, selected state, stop markers, point markers.
- Modify `src/client-web/src/components/mobile/locationFormatting.ts`
  - Add distance, speed, segment kind, quality and raw-point labels.

### Tests

- Modify `tests/client-web/tsconfig.mobile.json`
  - Include historical location test files and new helper tests.
- Modify `tests/client-web/mobileApiPath.test.ts`
  - Real Chinese categories and new location analytics paths.
- Modify `tests/client-web/mobileTypes.test.ts`
  - New location analytics types and real Chinese fixtures.
- Modify `tests/client-web/mobileAnalyticsComponents.test.tsx`
  - Accepted mobile records UI baseline.
- Modify `tests/client-web/mobileAnalyticsInteractions.test.tsx`
  - Heatmap selected detail and chart filters.
- Create `tests/client-web/mobileFormatting.test.ts`
  - Beijing range and formatter tests.
- Create `tests/client-web/locationAnalyticsComponents.test.tsx`
  - Historical location accepted UI baseline.
- Create `tests/client-web/locationAnalyticsInteractions.test.tsx`
  - Segment selection, raw point selection, filter callbacks.
- Create `tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs`
  - Location query normalization.
- Create `tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs`
  - Overview, tracks, segments, quality flags.
- Modify `tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs`
  - Query helper, history compatibility.
- Modify `tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs`
  - New endpoints.
- Modify `tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs`
  - New DTO JSON contracts.

---

## Task 1: Lock Real Chinese Copy And Category Constants

**Files:**
- Create: `src/client-web/src/components/mobile/mobileAnalyticsCopy.ts`
- Modify: `src/client-web/src/api/mobile.ts`
- Modify: `src/client-web/src/components/mobile/mobileFormatting.ts`
- Modify: `tests/client-web/mobileApiPath.test.ts`
- Create: `tests/client-web/mobileFormatting.test.ts`
- Modify: `tests/client-web/tsconfig.mobile.json`

- [ ] **Step 1: Write failing copy and API path tests**

Replace mojibake category expectations in `tests/client-web/mobileApiPath.test.ts` with this real Chinese list:

```ts
const expectedLifeCategories = [
  '社交通讯',
  '短视频/娱乐',
  '游戏',
  '音乐/音频',
  '阅读/资讯',
  '学习',
  '工作/生产力',
  '工具/系统',
  '浏览器/搜索',
  '出行/地图',
  '购物/外卖',
  '金融/支付',
  '健康/运动',
  '相机/创作',
  '生活服务',
  '未分类',
] as const;

assert.equal(
  mobileApiPaths.analyticsHeatmap({
    rangeStartUtc,
    rangeEndUtc,
    timezone: MOBILE_DEFAULT_TIMEZONE,
    category: '社交通讯',
    includeSystemNoise: false,
    granularity: '15m',
  }),
  '/mobile/analytics/heatmap?rangeStartUtc=2026-07-01T16%3A00%3A00Z&rangeEndUtc=2026-07-08T16%3A00%3A00Z&timezone=Asia%2FShanghai&category=%E7%A4%BE%E4%BA%A4%E9%80%9A%E8%AE%AF&includeSystemNoise=false&granularity=15m',
);
```

Create `tests/client-web/mobileFormatting.test.ts`:

```ts
import assert from 'node:assert/strict';
import {
  buildMobileAnalyticsDateRange,
  formatDuration,
  formatPercent,
  toMobileAnalyticsUtcRange,
} from '../../src/client-web/src/components/mobile/mobileFormatting';

const range = buildMobileAnalyticsDateRange('7d', new Date('2026-07-08T04:00:00.000Z'));
assert.deepEqual(range, {
  shortcut: '7d',
  startDate: '2026-07-02',
  endDate: '2026-07-08',
});

const utcRange = toMobileAnalyticsUtcRange(range);
assert.equal(utcRange.rangeStartUtc, '2026-07-01T16:00:00.000Z');
assert.equal(utcRange.rangeEndUtc, '2026-07-08T16:00:00.000Z');
assert.equal(utcRange.timezone, 'Asia/Shanghai');

assert.equal(formatDuration(52 * 60), '52分钟');
assert.equal(formatDuration(73 * 3600 + 23 * 60), '73小时23分钟');
assert.equal(formatPercent(0.68), '68%');
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileFormatting.test.ts
```

Expected before implementation:

- `mobileApiPath.test.ts` fails because `MOBILE_LIFE_CATEGORIES` still contains mojibake.
- `mobileFormatting.test.ts` fails if Chinese formatting still returns mojibake.

- [ ] **Step 3: Add copy constants**

Create `src/client-web/src/components/mobile/mobileAnalyticsCopy.ts`:

```ts
export const MOBILE_LIFE_CATEGORY_LABELS = [
  '社交通讯',
  '短视频/娱乐',
  '游戏',
  '音乐/音频',
  '阅读/资讯',
  '学习',
  '工作/生产力',
  '工具/系统',
  '浏览器/搜索',
  '出行/地图',
  '购物/外卖',
  '金融/支付',
  '健康/运动',
  '相机/创作',
  '生活服务',
  '未分类',
] as const;

export const MOBILE_MOJIBAKE_GUARDS = [
  '鎵嬫満',
  '鐑姏',
  '鍘嗗彶',
  '绀句氦娌',
] as const;
```

Modify `src/client-web/src/api/mobile.ts`:

```ts
import { MOBILE_LIFE_CATEGORY_LABELS } from '../components/mobile/mobileAnalyticsCopy';

export const MOBILE_LIFE_CATEGORIES = MOBILE_LIFE_CATEGORY_LABELS;
```

- [ ] **Step 4: Fix mobile formatter labels**

In `src/client-web/src/components/mobile/mobileFormatting.ts`, make these return values real Chinese:

```ts
export function formatDuration(seconds: number) {
  const totalSeconds = Math.max(0, Math.round(seconds));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0 && minutes > 0) return `${hours}小时${minutes}分钟`;
  if (hours > 0) return `${hours}小时`;
  if (minutes > 0) return `${minutes}分钟`;
  return `${totalSeconds}秒`;
}

export function formatPercent(value: number) {
  if (!Number.isFinite(value)) return '0%';
  return `${Math.round(value * 100)}%`;
}
```

- [ ] **Step 5: Include new test in mobile tsconfig**

Add `mobileFormatting.test.ts` to `tests/client-web/tsconfig.mobile.json` include array.

- [ ] **Step 6: Verify and commit**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileFormatting.test.ts
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: all pass.

Commit:

```powershell
git add src/client-web/src/api/mobile.ts src/client-web/src/components/mobile/mobileAnalyticsCopy.ts src/client-web/src/components/mobile/mobileFormatting.ts tests/client-web/mobileApiPath.test.ts tests/client-web/mobileFormatting.test.ts tests/client-web/tsconfig.mobile.json
git commit -m "fix: normalize mobile analytics chinese copy"
```

---

## Task 2: Build The Accepted Mobile Heatmap Matrix And Detail Panel

**Files:**
- Create: `src/client-web/src/components/mobile/mobileHeatmapMatrix.ts`
- Modify: `src/client-web/src/components/mobile/MobileUsageHeatmap.tsx`
- Create: `src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx`
- Modify: `tests/client-web/mobileAnalyticsComponents.test.tsx`
- Modify: `tests/client-web/mobileAnalyticsInteractions.test.tsx`

- [ ] **Step 1: Write failing heatmap matrix tests**

Add to `tests/client-web/mobileAnalyticsInteractions.test.tsx`:

```ts
import {
  buildHeatmapMatrix,
} from '../../src/client-web/src/components/mobile/mobileHeatmapMatrix';

const duplicateHourBuckets = [
  {
    bucketStartUtc: '2026-07-06T12:00:00.000Z',
    bucketEndUtc: '2026-07-06T13:00:00.000Z',
    localDate: '2026-07-06',
    localHour: 20,
    lifeCategory: '短视频/娱乐',
    foregroundSeconds: 1200,
    qualityFlags: [],
  },
  {
    bucketStartUtc: '2026-07-06T12:00:00.000Z',
    bucketEndUtc: '2026-07-06T13:00:00.000Z',
    localDate: '2026-07-06',
    localHour: 20,
    lifeCategory: '社交通讯',
    foregroundSeconds: 600,
    qualityFlags: ['fallback'],
  },
];

const matrix = buildHeatmapMatrix(duplicateHourBuckets);
assert.equal(matrix.days.length, 1);
assert.equal(matrix.days[0].cells.length, 24);
const cell = matrix.days[0].cells[20];
assert.equal(cell.foregroundSeconds, 1800);
assert.deepEqual(cell.categories.map(item => item.lifeCategory), ['短视频/娱乐', '社交通讯']);
assert.equal(cell.qualityFlags.includes('fallback'), true);
```

Add a render assertion to `tests/client-web/mobileAnalyticsComponents.test.tsx`:

```ts
assert.equal(html.includes('使用热力图'), true);
assert.equal(html.includes('左侧是日期，顶部是小时'), true);
assert.equal(html.includes('选中时段'), true);
assert.equal(html.includes('52分钟'), true);
assert.equal(html.includes('重复小时数字墙'), false);
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsInteractions.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
```

Expected: fails because `mobileHeatmapMatrix.ts` and detail panel do not exist.

- [ ] **Step 3: Implement matrix helper**

Create `src/client-web/src/components/mobile/mobileHeatmapMatrix.ts`:

```ts
import type { MobileHeatmapBucket } from '../../api/mobile';

export interface HeatmapCategorySlice {
  lifeCategory: string;
  foregroundSeconds: number;
}

export interface HeatmapMatrixCell {
  localDate: string;
  localHour: number;
  bucketStartUtc: string | null;
  bucketEndUtc: string | null;
  foregroundSeconds: number;
  qualityFlags: string[];
  categories: HeatmapCategorySlice[];
  sourceBuckets: MobileHeatmapBucket[];
}

export interface HeatmapMatrixDay {
  localDate: string;
  label: string;
  cells: HeatmapMatrixCell[];
}

export interface HeatmapMatrix {
  hours: number[];
  days: HeatmapMatrixDay[];
  maxSeconds: number;
}

function dateLabel(localDate: string) {
  const [year, month, day] = localDate.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const today = new Date();
  const todayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  return `${month}月${day}日 ${localDate === todayKey ? '今天' : weekdays[date.getUTCDay()]}`;
}

function emptyCell(localDate: string, localHour: number): HeatmapMatrixCell {
  return {
    localDate,
    localHour,
    bucketStartUtc: null,
    bucketEndUtc: null,
    foregroundSeconds: 0,
    qualityFlags: [],
    categories: [],
    sourceBuckets: [],
  };
}

export function buildHeatmapMatrix(buckets: MobileHeatmapBucket[]): HeatmapMatrix {
  const byDate = new Map<string, HeatmapMatrixCell[]>();

  for (const bucket of buckets) {
    if (!byDate.has(bucket.localDate)) {
      byDate.set(bucket.localDate, Array.from({ length: 24 }, (_, hour) => emptyCell(bucket.localDate, hour)));
    }

    const cell = byDate.get(bucket.localDate)![bucket.localHour];
    cell.bucketStartUtc = cell.bucketStartUtc ?? bucket.bucketStartUtc;
    cell.bucketEndUtc = bucket.bucketEndUtc;
    cell.foregroundSeconds += bucket.foregroundSeconds;
    cell.sourceBuckets.push(bucket);
    for (const flag of bucket.qualityFlags) {
      if (!cell.qualityFlags.includes(flag)) cell.qualityFlags.push(flag);
    }

    const existing = cell.categories.find(item => item.lifeCategory === bucket.lifeCategory);
    if (existing) existing.foregroundSeconds += bucket.foregroundSeconds;
    else cell.categories.push({ lifeCategory: bucket.lifeCategory, foregroundSeconds: bucket.foregroundSeconds });
    cell.categories.sort((a, b) => b.foregroundSeconds - a.foregroundSeconds);
  }

  const days = [...byDate.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([localDate, cells]) => ({ localDate, label: dateLabel(localDate), cells }));

  return {
    hours: Array.from({ length: 24 }, (_, hour) => hour),
    days,
    maxSeconds: Math.max(1, ...days.flatMap(day => day.cells.map(cell => cell.foregroundSeconds))),
  };
}
```

- [ ] **Step 4: Implement right-side detail component**

Create `src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx`:

```tsx
import type { HeatmapMatrixCell } from './mobileHeatmapMatrix';
import { formatDuration } from './mobileFormatting';

export interface MobileUsageBucketDetailProps {
  cell: HeatmapMatrixCell | null;
}

export default function MobileUsageBucketDetail({ cell }: MobileUsageBucketDetailProps) {
  if (!cell) {
    return (
      <section className="rounded-md border border-slate-200 bg-white">
        <div className="border-b border-slate-100 p-4">
          <h2 className="text-sm font-semibold text-slate-950">选中时段</h2>
          <p className="mt-1 text-xs text-slate-500">点击热力格查看时段构成。</p>
        </div>
      </section>
    );
  }

  const topCategory = cell.categories[0]?.lifeCategory ?? '未分类';
  return (
    <section className="rounded-md border border-slate-200 bg-white">
      <div className="flex items-start justify-between gap-3 border-b border-slate-100 p-4">
        <div>
          <h2 className="text-sm font-semibold text-slate-950">选中时段</h2>
          <p className="mt-1 text-xs text-slate-500">{cell.localDate} {cell.localHour}:00 至 {cell.localHour + 1}:00</p>
        </div>
        <span className="rounded-full border border-slate-200 px-2 py-0.5 text-xs text-slate-600">高峰</span>
      </div>
      <div className="space-y-4 p-4">
        <div className="text-3xl font-bold text-slate-950">{formatDuration(cell.foregroundSeconds)}</div>
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="rounded-full border border-slate-200 px-2 py-1">{topCategory}</span>
          <span className="rounded-full border border-slate-200 px-2 py-1">质量正常</span>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div className="rounded-md border border-slate-100 bg-slate-50 p-3"><span className="block text-xs text-slate-500">Top 分类</span><strong>{topCategory}</strong></div>
          <div className="rounded-md border border-slate-100 bg-slate-50 p-3"><span className="block text-xs text-slate-500">分类数</span><strong>{cell.categories.length}</strong></div>
          <div className="rounded-md border border-slate-100 bg-slate-50 p-3"><span className="block text-xs text-slate-500">最长连续</span><strong>{formatDuration(cell.foregroundSeconds)}</strong></div>
          <div className="rounded-md border border-slate-100 bg-slate-50 p-3"><span className="block text-xs text-slate-500">系统噪声</span><strong>已隐藏</strong></div>
        </div>
        <div className="space-y-2">
          {cell.categories.map(category => (
            <div key={category.lifeCategory} className="grid grid-cols-[96px_minmax(0,1fr)_72px] items-center gap-2 text-xs">
              <span className="truncate">{category.lifeCategory}</span>
              <span className="h-2 rounded-full bg-slate-100"><span className="block h-2 rounded-full bg-teal-500" style={{ width: `${Math.max(8, category.foregroundSeconds / Math.max(1, cell.foregroundSeconds) * 100)}%` }} /></span>
              <span className="text-right text-slate-500">{formatDuration(category.foregroundSeconds)}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Step 5: Replace heatmap rendering**

Modify `MobileUsageHeatmap.tsx` to call `buildHeatmapMatrix(buckets)`, render date rows and hour columns, and export the selected matrix cell in an `onCellSelect` callback or keep `onBucketSelect` by passing the first source bucket. The grid must use:

```tsx
style={{ gridTemplateColumns: '92px repeat(24, minmax(28px, 1fr))' }}
```

Each cell button must render no visible number:

```tsx
<button
  type="button"
  aria-label={`${day.label} ${cell.localHour}:00 ${formatDuration(cell.foregroundSeconds)}`}
  className={cellClassName}
  onClick={() => {
    if (cell.sourceBuckets[0]) onBucketSelect(cell.sourceBuckets[0]);
  }}
/>
```

- [ ] **Step 6: Verify and commit**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsInteractions.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: all pass.

Commit:

```powershell
git add src/client-web/src/components/mobile/mobileHeatmapMatrix.ts src/client-web/src/components/mobile/MobileUsageHeatmap.tsx src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx tests/client-web/mobileAnalyticsComponents.test.tsx tests/client-web/mobileAnalyticsInteractions.test.tsx
git commit -m "feat: render mobile usage matrix heatmap"
```

---

## Task 3: Preserve Global Range On Heatmap Selection And Wire Chart Filters

**Files:**
- Modify: `src/client-web/src/pages/MobileRecordsPage.tsx`
- Modify: `src/client-web/src/components/mobile/MobileChartsGrid.tsx`
- Modify: `tests/client-web/mobileAnalyticsInteractions.test.tsx`

- [ ] **Step 1: Write failing source and interaction tests**

In `tests/client-web/mobileAnalyticsInteractions.test.tsx`, add a source guard:

```ts
const source = readFileSync(
  path.join(process.cwd(), 'src/client-web/src/pages/MobileRecordsPage.tsx'),
  'utf8',
);

assert.equal(source.includes('setSelectedBucketRange({ startUtc: bucket.bucketStartUtc, endUtc: bucket.bucketEndUtc })'), false);
assert.equal(source.includes('setRangeStartDate(bucket.localDate)'), false);
assert.equal(source.includes('onCategorySelect={handleChartCategorySelect}'), true);
assert.equal(source.includes('onAppSelect={handleChartAppSelect}'), true);
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsInteractions.test.tsx
```

Expected: fails because current heatmap selection changes range and chart handlers are not wired.

- [ ] **Step 3: Modify heatmap click behavior**

In `MobileRecordsPage.tsx`, change `handleHeatmapBucketSelect` to preserve the global range:

```ts
function handleHeatmapBucketSelect(bucket: MobileHeatmapBucket) {
  setSelectedBucketStartUtc(bucket.bucketStartUtc);
  setSelectedBucketRange({ startUtc: bucket.bucketStartUtc, endUtc: bucket.bucketEndUtc });
  setExpandedBlockId(null);
  setExpandedSessionId(null);
}
```

Then stop feeding `selectedBucketRange` into `analyticsQuery`; instead create a separate detail query later if the implementation needs sessions for the bucket. The page-level `analyticsQuery` must keep:

```ts
rangeStartUtc: utcRange.rangeStartUtc,
rangeEndUtc: utcRange.rangeEndUtc,
```

- [ ] **Step 4: Wire chart filters**

Add handlers:

```ts
function handleChartCategorySelect(category: string) {
  setSelectedCategory(category);
  setSelectedBucketStartUtc(null);
  setSelectedBucketRange(null);
}

function handleChartAppSelect(packageNameValue: string) {
  setPackageName(packageNameValue);
  setSelectedBucketStartUtc(null);
  setSelectedBucketRange(null);
}
```

Pass to `MobileChartsGrid`:

```tsx
<MobileChartsGrid
  charts={chartsQuery.data ?? []}
  isLoading={chartsQuery.isLoading}
  onCategorySelect={handleChartCategorySelect}
  onAppSelect={handleChartAppSelect}
/>
```

- [ ] **Step 5: Verify and commit**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsInteractions.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: pass.

Commit:

```powershell
git add src/client-web/src/pages/MobileRecordsPage.tsx tests/client-web/mobileAnalyticsInteractions.test.tsx
git commit -m "fix: keep mobile heatmap selection local"
```

---

## Task 4: Apply Accepted Mobile Records Workbench Layout

**Files:**
- Create: `src/client-web/src/components/mobile/WorkbenchPanel.tsx`
- Create: `src/client-web/src/components/mobile/MobileMetricGrid.tsx`
- Modify: `src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx`
- Modify: `src/client-web/src/components/mobile/MobileInsightStrip.tsx`
- Modify: `src/client-web/src/pages/MobileRecordsPage.tsx`
- Modify: `tests/client-web/mobileAnalyticsComponents.test.tsx`

- [ ] **Step 1: Write failing accepted UI baseline assertions**

In `tests/client-web/mobileAnalyticsComponents.test.tsx`, assert the exact accepted labels:

```ts
for (const text of [
  '手机记录',
  '今天',
  '7天',
  '30天',
  '自定义',
  '北京时间',
  '设备',
  '分类',
  'App',
  '噪声',
  '粒度',
  '范围',
  '总使用时长',
  '日均',
  '目标',
  'App 数',
  '完整度',
  '最近同步',
  '使用热力图',
  '选中时段',
  '分类占比',
  'Top App',
  '使用时间线',
]) {
  assert.equal(html.includes(text), true, `mobile records UI should include ${text}`);
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
```

Expected: fails until mojibake and layout labels are corrected.

- [ ] **Step 3: Create shared panel and metric grid primitives**

Create `WorkbenchPanel.tsx`:

```tsx
import type { ReactNode } from 'react';

export interface WorkbenchPanelProps {
  title: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
}

export default function WorkbenchPanel({ title, description, action, children }: WorkbenchPanelProps) {
  return (
    <section className="overflow-hidden rounded-md border border-slate-200 bg-white">
      <div className="flex items-start justify-between gap-3 border-b border-slate-100 p-4">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-slate-950">{title}</h2>
          {description && <p className="mt-1 text-xs text-slate-500">{description}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}
```

Create `MobileMetricGrid.tsx`:

```tsx
export interface MobileMetricItem {
  label: string;
  value: string;
  helper: string;
  tone?: 'default' | 'warning';
}

export default function MobileMetricGrid({ items }: { items: MobileMetricItem[] }) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-6">
      {items.map(item => (
        <div
          key={item.label}
          className={`rounded-md border p-3 ${item.tone === 'warning' ? 'border-amber-200 bg-amber-50' : 'border-slate-200 bg-white'}`}
        >
          <div className="text-xs font-semibold text-slate-500">{item.label}</div>
          <div className="mt-2 text-2xl font-bold text-slate-950">{item.value}</div>
          <div className="mt-1 text-xs text-slate-500">{item.helper}</div>
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 4: Align mobile records page layout**

In `MobileRecordsPage.tsx`, arrange main content in this order:

```tsx
<MobileAnalyticsHeader ... />
<main className="space-y-4 pt-4">
  <MobileInsightStrip overview={overviewQuery.data} isLoading={loading} />
  <div className="mx-auto grid max-w-[1500px] grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
    <MobileUsageHeatmap ... />
    <MobileUsageBucketDetail cell={selectedHeatmapCell} />
  </div>
  <MobileChartsGrid ... />
  <MobileTimelineBlocks ... />
  <div className="mx-auto grid max-w-[1500px] grid-cols-1 gap-4 xl:grid-cols-2">
    <MobileAnomalyPanel ... />
    <MobileAppCatalogManager ... />
  </div>
</main>
```

Use the matrix helper to derive `selectedHeatmapCell` from `heatmapQuery.data` and `selectedBucketStartUtc`.

- [ ] **Step 5: Verify and commit**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: pass.

Commit:

```powershell
git add src/client-web/src/components/mobile/WorkbenchPanel.tsx src/client-web/src/components/mobile/MobileMetricGrid.tsx src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx src/client-web/src/components/mobile/MobileInsightStrip.tsx src/client-web/src/pages/MobileRecordsPage.tsx tests/client-web/mobileAnalyticsComponents.test.tsx
git commit -m "feat: apply accepted mobile records workbench layout"
```

---

## Task 5: Add Location Analytics API Types And Frontend Contract Tests

**Files:**
- Modify: `src/client-web/src/api/mobile.ts`
- Modify: `tests/client-web/mobileApiPath.test.ts`
- Modify: `tests/client-web/mobileTypes.test.ts`

- [ ] **Step 1: Write failing location API path tests**

Add to `tests/client-web/mobileApiPath.test.ts`:

```ts
assert.equal(
  mobileApiPaths.locationAnalyticsOverview({
    rangeStartUtc,
    rangeEndUtc,
    timezone: MOBILE_DEFAULT_TIMEZONE,
    maxAccuracyMeters: 50,
    includeRejected: false,
  }),
  '/mobile/location/analytics/overview?rangeStartUtc=2026-07-01T16%3A00%3A00Z&rangeEndUtc=2026-07-08T16%3A00%3A00Z&timezone=Asia%2FShanghai&maxAccuracyMeters=50&includeRejected=false',
);
assert.equal(mobileApiPaths.locationAnalyticsTracks({ timezone: MOBILE_DEFAULT_TIMEZONE }), '/mobile/location/analytics/tracks?timezone=Asia%2FShanghai');
assert.equal(mobileApiPaths.locationAnalyticsSegment('segment/一'), '/mobile/location/analytics/segments/segment%2F%E4%B8%80');
assert.equal(mobileApiPaths.locationAnalyticsSegmentPoints('segment/一', { pageSize: 20 }), '/mobile/location/analytics/segments/segment%2F%E4%B8%80/points?pageSize=20');
```

- [ ] **Step 2: Write failing type tests**

Add to `tests/client-web/mobileTypes.test.ts`:

```ts
import type {
  MobileLocationAnalyticsOverview,
  MobileLocationTrack,
  MobileLocationSegment,
  MobileLocationSegmentPointPage,
} from '../../src/client-web/src/api/mobile';

const locationOverview: MobileLocationAnalyticsOverview = {
  range: {
    rangeStartUtc,
    rangeEndUtc,
    timezone: 'Asia/Shanghai',
    localStartDate: '2026-07-02',
    localEndDate: '2026-07-08',
  },
  generatedAt: '2026-07-08T00:12:00Z',
  pointCount: 428,
  usablePointCount: 391,
  rejectedPointCount: 37,
  activeSpanSeconds: 583200,
  distanceMeters: 84600,
  stayCount: 12,
  longestStaySeconds: 12000,
  averageAccuracyMeters: 18,
  qualityIssueCount: 2,
  qualityFlags: ['low-accuracy-cluster'],
};
assert.equal(locationOverview.stayCount, 12);

const segment: MobileLocationSegment = {
  id: 'segment-1',
  trackId: 'track-1',
  deviceId: 'pixel-8',
  kind: 'move',
  startUtc: '2026-07-07T10:20:00Z',
  endUtc: '2026-07-07T11:05:00Z',
  localStart: '2026-07-07 18:20',
  localEnd: '2026-07-07 19:05',
  durationSeconds: 2700,
  distanceMeters: 7800,
  pointCount: 36,
  averageSpeedMetersPerSecond: 2.88,
  averageAccuracyMeters: 14,
  maxAccuracyMeters: 44,
  quality: 'high',
  qualityFlags: [],
  bounds: { minLatitude: 31.2, minLongitude: 121.4, maxLatitude: 31.3, maxLongitude: 121.5 },
  path: [],
};
assert.equal(segment.kind, 'move');

const track: MobileLocationTrack = {
  id: 'track-1',
  deviceId: 'pixel-8',
  startUtc: segment.startUtc,
  endUtc: segment.endUtc,
  distanceMeters: 7800,
  durationSeconds: 2700,
  pointCount: 36,
  segmentCount: 1,
  bounds: segment.bounds,
  qualityFlags: [],
  segments: [segment],
};
assert.equal(track.segments[0].id, 'segment-1');

const page: MobileLocationSegmentPointPage = {
  items: [],
  nextCursor: null,
  hasMore: false,
};
assert.equal(page.hasMore, false);
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileTypes.test.ts
```

Expected: fails because new paths and types do not exist.

- [ ] **Step 4: Add frontend API paths and types**

In `src/client-web/src/api/mobile.ts`, add:

```ts
export interface MobileLocationAnalyticsParams {
  rangeStartUtc?: string | null;
  rangeEndUtc?: string | null;
  timezone?: string | null;
  deviceId?: string | null;
  maxAccuracyMeters?: number | null;
  includeRejected?: boolean | null;
  cursor?: string | null;
  pageSize?: number | null;
}

function withLocationAnalyticsQuery(path: string, query: MobileLocationAnalyticsParams = {}) {
  return withQuery(path, [
    ['rangeStartUtc', query.rangeStartUtc],
    ['rangeEndUtc', query.rangeEndUtc],
    ['timezone', query.timezone],
    ['deviceId', query.deviceId],
    ['maxAccuracyMeters', query.maxAccuracyMeters],
    ['includeRejected', query.includeRejected],
    ['cursor', query.cursor],
    ['pageSize', query.pageSize],
  ]);
}
```

Add paths:

```ts
locationAnalyticsOverview: (query: MobileLocationAnalyticsParams = {}) =>
  withLocationAnalyticsQuery('/mobile/location/analytics/overview', query),
locationAnalyticsTracks: (query: MobileLocationAnalyticsParams = {}) =>
  withLocationAnalyticsQuery('/mobile/location/analytics/tracks', query),
locationAnalyticsSegment: (segmentId: string) =>
  `/mobile/location/analytics/segments/${pathSegment(segmentId)}`,
locationAnalyticsSegmentPoints: (segmentId: string, query: MobileLocationAnalyticsParams = {}) =>
  withLocationAnalyticsQuery(`/mobile/location/analytics/segments/${pathSegment(segmentId)}/points`, query),
```

Add interfaces from Step 2 and fetchers:

```ts
export function getMobileLocationAnalyticsOverview(
  query: MobileLocationAnalyticsParams = {},
): Promise<MobileLocationAnalyticsOverview> {
  return apiGet<ApiResponse<MobileLocationAnalyticsOverview>>(mobileApiPaths.locationAnalyticsOverview(query)).then(r => r.data);
}

export function getMobileLocationAnalyticsTracks(
  query: MobileLocationAnalyticsParams = {},
): Promise<MobileLocationTrack[]> {
  return apiGet<ApiResponse<MobileLocationTrack[]>>(mobileApiPaths.locationAnalyticsTracks(query)).then(r => r.data);
}

export function getMobileLocationAnalyticsSegment(segmentId: string): Promise<MobileLocationSegment> {
  return apiGet<ApiResponse<MobileLocationSegment>>(mobileApiPaths.locationAnalyticsSegment(segmentId)).then(r => r.data);
}

export function getMobileLocationAnalyticsSegmentPoints(
  segmentId: string,
  query: MobileLocationAnalyticsParams = {},
): Promise<MobileLocationSegmentPointPage> {
  return apiGet<ApiResponse<MobileLocationSegmentPointPage>>(mobileApiPaths.locationAnalyticsSegmentPoints(segmentId, query)).then(r => r.data);
}
```

- [ ] **Step 5: Verify and commit**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileTypes.test.ts
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: pass.

Commit:

```powershell
git add src/client-web/src/api/mobile.ts tests/client-web/mobileApiPath.test.ts tests/client-web/mobileTypes.test.ts
git commit -m "feat: add mobile location analytics web contract"
```

---

## Task 6: Add Backend Location Query DTOs And Normalization

**Files:**
- Create: `src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs`
- Create: `tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs`

- [ ] **Step 1: Write failing query service tests**

Create `tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs`:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileLocationQueryServiceTests
{
    [Fact]
    public void Normalize_DefaultsToLastSevenBeijingDays()
    {
        var service = new MobileLocationQueryService(MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-08T04:00:00Z")));

        var context = service.Normalize(new MobileLocationQueryRequest());

        Assert.Equal("Asia/Shanghai", context.Range.Timezone);
        Assert.Equal("2026-07-02", context.Range.LocalStartDate);
        Assert.Equal("2026-07-08", context.Range.LocalEndDate);
        Assert.Equal(DateTimeOffset.Parse("2026-07-01T16:00:00Z"), context.Range.RangeStartUtc);
        Assert.Equal(DateTimeOffset.Parse("2026-07-08T16:00:00Z"), context.Range.RangeEndUtc);
        Assert.Equal(50, context.MaxAccuracyMeters);
        Assert.False(context.IncludeRejected);
        Assert.Equal(50, context.PageSize);
    }

    [Fact]
    public void Normalize_ClampsPageSizeAndReordersRange()
    {
        var service = new MobileLocationQueryService(MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-08T04:00:00Z")));

        var context = service.Normalize(new MobileLocationQueryRequest(
            RangeStartUtc: DateTimeOffset.Parse("2026-07-08T16:00:00Z"),
            RangeEndUtc: DateTimeOffset.Parse("2026-07-01T16:00:00Z"),
            PageSize: 500,
            MaxAccuracyMeters: -1));

        Assert.Equal(DateTimeOffset.Parse("2026-07-01T16:00:00Z"), context.Range.RangeStartUtc);
        Assert.Equal(DateTimeOffset.Parse("2026-07-08T16:00:00Z"), context.Range.RangeEndUtc);
        Assert.Equal(200, context.PageSize);
        Assert.Equal(50, context.MaxAccuracyMeters);
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileLocationQueryServiceTests
```

Expected: compile fails because DTO/service do not exist.

- [ ] **Step 3: Add DTOs**

Create `src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs` with:

```csharp
namespace Pim.Module.Mobile.DTOs;

public sealed record MobileLocationQueryRequest(
    DateTimeOffset? RangeStartUtc = null,
    DateTimeOffset? RangeEndUtc = null,
    string? Timezone = null,
    string? DeviceId = null,
    double? MaxAccuracyMeters = null,
    bool? IncludeRejected = null,
    string? Cursor = null,
    int? PageSize = null);

public sealed record MobileLocationQueryContext(
    MobileAnalyticsRangeDto Range,
    string? DeviceId,
    double MaxAccuracyMeters,
    bool IncludeRejected,
    string? Cursor,
    int PageSize);

public sealed record MobileGeoBoundsDto(
    double MinLatitude,
    double MinLongitude,
    double MaxLatitude,
    double MaxLongitude);

public sealed record MobileLocationAnalyticsOverviewResponse(
    MobileAnalyticsRangeDto Range,
    DateTimeOffset GeneratedAt,
    int PointCount,
    int UsablePointCount,
    int RejectedPointCount,
    long ActiveSpanSeconds,
    double DistanceMeters,
    int StayCount,
    long LongestStaySeconds,
    double AverageAccuracyMeters,
    int QualityIssueCount,
    IReadOnlyList<string> QualityFlags);

public sealed record MobileLocationPathPointDto(
    string Id,
    DateTimeOffset RecordedAtUtc,
    double Latitude,
    double Longitude,
    double HorizontalAccuracyMeters,
    string Quality);

public sealed record MobileLocationSegmentDto(
    string Id,
    string TrackId,
    string DeviceId,
    string Kind,
    DateTimeOffset StartUtc,
    DateTimeOffset EndUtc,
    string LocalStart,
    string LocalEnd,
    long DurationSeconds,
    double DistanceMeters,
    int PointCount,
    double AverageSpeedMetersPerSecond,
    double AverageAccuracyMeters,
    double MaxAccuracyMeters,
    string Quality,
    IReadOnlyList<string> QualityFlags,
    MobileGeoBoundsDto? Bounds,
    IReadOnlyList<MobileLocationPathPointDto> Path);

public sealed record MobileLocationTrackDto(
    string Id,
    string DeviceId,
    DateTimeOffset StartUtc,
    DateTimeOffset EndUtc,
    double DistanceMeters,
    long DurationSeconds,
    int PointCount,
    int SegmentCount,
    MobileGeoBoundsDto? Bounds,
    IReadOnlyList<string> QualityFlags,
    IReadOnlyList<MobileLocationSegmentDto> Segments);

public sealed record MobileLocationSegmentPointPageDto(
    IReadOnlyList<MobileLocationPointDto> Items,
    string? NextCursor,
    bool HasMore);
```

- [ ] **Step 4: Add query service**

Create `src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs`:

```csharp
using System.Globalization;
using Pim.Module.Mobile.DTOs;

namespace Pim.Module.Mobile.Services;

public sealed class MobileLocationQueryService
{
    public const double DefaultMaxAccuracyMeters = 50;
    public const int DefaultPageSize = 50;
    public const int MaxPageSize = 200;

    private readonly TimeProvider _timeProvider;

    public MobileLocationQueryService(TimeProvider timeProvider)
    {
        _timeProvider = timeProvider;
    }

    public MobileLocationQueryContext Normalize(MobileLocationQueryRequest request)
    {
        var timezone = string.IsNullOrWhiteSpace(request.Timezone)
            ? MobileAnalyticsDefaults.DefaultTimezone
            : request.Timezone.Trim();
        var timeZoneInfo = ResolveTimezone(timezone);
        var (startUtc, endUtc) = NormalizeRange(request.RangeStartUtc, request.RangeEndUtc, timeZoneInfo);
        if (endUtc < startUtc)
            (startUtc, endUtc) = (endUtc, startUtc);

        var localStart = TimeZoneInfo.ConvertTime(startUtc, timeZoneInfo).Date;
        var localEnd = TimeZoneInfo.ConvertTime(endUtc.AddTicks(-1), timeZoneInfo).Date;
        var pageSize = Math.Clamp(request.PageSize.GetValueOrDefault(DefaultPageSize), 1, MaxPageSize);
        var maxAccuracyMeters = request.MaxAccuracyMeters is > 0
            ? request.MaxAccuracyMeters.Value
            : DefaultMaxAccuracyMeters;

        return new MobileLocationQueryContext(
            new MobileAnalyticsRangeDto(
                startUtc,
                endUtc,
                timezone,
                localStart.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
                localEnd.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)),
            string.IsNullOrWhiteSpace(request.DeviceId) ? null : request.DeviceId.Trim(),
            maxAccuracyMeters,
            request.IncludeRejected.GetValueOrDefault(false),
            string.IsNullOrWhiteSpace(request.Cursor) ? null : request.Cursor.Trim(),
            pageSize);
    }

    private static TimeZoneInfo ResolveTimezone(string timezone)
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById(timezone); }
        catch (TimeZoneNotFoundException) when (timezone == MobileAnalyticsDefaults.DefaultTimezone)
        {
            return TimeZoneInfo.FindSystemTimeZoneById("China Standard Time");
        }
        catch (InvalidTimeZoneException) when (timezone == MobileAnalyticsDefaults.DefaultTimezone)
        {
            return TimeZoneInfo.FindSystemTimeZoneById("China Standard Time");
        }
    }

    private (DateTimeOffset StartUtc, DateTimeOffset EndUtc) NormalizeRange(
        DateTimeOffset? startUtc,
        DateTimeOffset? endUtc,
        TimeZoneInfo timeZoneInfo)
    {
        if (startUtc is not null && endUtc is not null)
            return (startUtc.Value, endUtc.Value);
        if (startUtc is not null)
            return (startUtc.Value, startUtc.Value.AddDays(7));
        if (endUtc is not null)
            return (endUtc.Value.AddDays(-7), endUtc.Value);

        var nowLocal = TimeZoneInfo.ConvertTime(_timeProvider.GetUtcNow(), timeZoneInfo).Date;
        var startLocal = nowLocal.AddDays(-6);
        var endExclusiveLocal = nowLocal.AddDays(1);
        return (LocalDateStartUtc(startLocal, timeZoneInfo), LocalDateStartUtc(endExclusiveLocal, timeZoneInfo));
    }

    private static DateTimeOffset LocalDateStartUtc(DateTime localDate, TimeZoneInfo timeZoneInfo)
    {
        var unspecified = DateTime.SpecifyKind(localDate.Date, DateTimeKind.Unspecified);
        var utc = TimeZoneInfo.ConvertTimeToUtc(unspecified, timeZoneInfo);
        return new DateTimeOffset(utc, TimeSpan.Zero);
    }
}
```

- [ ] **Step 5: Verify and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileLocationQueryServiceTests
```

Expected: pass.

Commit:

```powershell
git add src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs
git commit -m "feat: add mobile location analytics query contract"
```

---

## Task 7: Implement Location Analytics Aggregation

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs`
- Create: `tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs`

- [ ] **Step 1: Write failing aggregation tests**

Create `tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs` with:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileLocationAggregationServiceTests
{
    [Fact]
    public async Task GetOverviewAsync_ReturnsAcceptedMetricInputs()
    {
        await using var db = MobileTestHelpers.CreateDb();
        SeedPoint(db, "p1", "2026-07-07T10:20:00Z", 31.230416, 121.473701, 12, "usable");
        SeedPoint(db, "p2", "2026-07-07T10:40:00Z", 31.235000, 121.480000, 18, "usable");
        SeedPoint(db, "p3", "2026-07-07T11:05:00Z", 31.240000, 121.490000, 44, "usable");
        await db.SaveChangesAsync();
        var service = Service(db);

        var overview = await service.GetOverviewAsync(new MobileLocationQueryRequest(
            RangeStartUtc: DateTimeOffset.Parse("2026-07-07T00:00:00Z"),
            RangeEndUtc: DateTimeOffset.Parse("2026-07-08T00:00:00Z")), CancellationToken.None);

        Assert.Equal(3, overview.PointCount);
        Assert.Equal(3, overview.UsablePointCount);
        Assert.True(overview.DistanceMeters > 1000);
        Assert.True(overview.AverageAccuracyMeters > 0);
    }

    [Fact]
    public async Task GetTracksAsync_SplitsLongGapsAndReturnsMoveSegments()
    {
        await using var db = MobileTestHelpers.CreateDb();
        SeedPoint(db, "p1", "2026-07-07T10:20:00Z", 31.230416, 121.473701, 12, "usable");
        SeedPoint(db, "p2", "2026-07-07T10:40:00Z", 31.235000, 121.480000, 18, "usable");
        SeedPoint(db, "p3", "2026-07-07T15:20:00Z", 31.280000, 121.520000, 20, "usable");
        await db.SaveChangesAsync();
        var service = Service(db);

        var tracks = await service.GetTracksAsync(new MobileLocationQueryRequest(
            RangeStartUtc: DateTimeOffset.Parse("2026-07-07T00:00:00Z"),
            RangeEndUtc: DateTimeOffset.Parse("2026-07-08T00:00:00Z")), CancellationToken.None);

        Assert.True(tracks.Count >= 2);
        Assert.Contains(tracks.SelectMany(track => track.Segments), segment => segment.Kind == "move");
    }

    private static MobileLocationAggregationService Service(Pim.Infrastructure.Data.PimDbContext db)
        => new(
            db,
            MobileTestHelpers.CurrentUser(),
            new MobileLocationQueryService(MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-08T04:00:00Z"))),
            MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-08T04:00:00Z")));

    private static void SeedPoint(Pim.Infrastructure.Data.PimDbContext db, string id, string recordedAt, double lat, double lon, double accuracy, string quality)
    {
        db.Set<MobileLocationPointEntity>().Add(new MobileLocationPointEntity
        {
            Id = id,
            UserId = MobileTestHelpers.UserId,
            DeviceId = "pixel-8",
            RecordedAtUtc = DateTimeOffset.Parse(recordedAt),
            Latitude = Convert.ToDecimal(lat),
            Longitude = Convert.ToDecimal(lon),
            HorizontalAccuracyMeters = Convert.ToDecimal(accuracy),
            Provider = "gps",
            Source = "auto",
            Quality = quality,
            RawJson = "{}",
            CreatedAt = DateTimeOffset.Parse(recordedAt),
        });
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileLocationAggregationServiceTests
```

Expected: compile fails because service does not exist.

- [ ] **Step 3: Implement aggregation service**

Create `MobileLocationAggregationService.cs` with:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;

namespace Pim.Module.Mobile.Services;

public sealed class MobileLocationAggregationService
{
    private static readonly TimeSpan TrackGap = TimeSpan.FromHours(2);
    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;
    private readonly MobileLocationQueryService _queryService;
    private readonly TimeProvider _timeProvider;

    public MobileLocationAggregationService(
        PimDbContext db,
        ICurrentUserService currentUser,
        MobileLocationQueryService queryService,
        TimeProvider timeProvider)
    {
        _db = db;
        _currentUser = currentUser;
        _queryService = queryService;
        _timeProvider = timeProvider;
    }

    public async Task<MobileLocationAnalyticsOverviewResponse> GetOverviewAsync(
        MobileLocationQueryRequest request,
        CancellationToken ct = default)
    {
        var context = _queryService.Normalize(request);
        var points = await LoadPointsAsync(context, ct);
        var usable = points.Where(p => p.Quality != "rejected").ToList();
        var distance = TotalDistanceMeters(usable);
        var span = usable.Count < 2 ? 0 : (long)(usable[^1].RecordedAtUtc - usable[0].RecordedAtUtc).TotalSeconds;
        var averageAccuracy = usable.Count == 0 ? 0 : usable.Average(p => (double)p.HorizontalAccuracyMeters);
        var tracks = BuildTracks(context, usable);

        return new MobileLocationAnalyticsOverviewResponse(
            context.Range,
            _timeProvider.GetUtcNow(),
            points.Count,
            usable.Count,
            points.Count - usable.Count,
            span,
            distance,
            tracks.SelectMany(track => track.Segments).Count(segment => segment.Kind == "stay"),
            tracks.SelectMany(track => track.Segments).Where(segment => segment.Kind == "stay").Select(segment => segment.DurationSeconds).DefaultIfEmpty(0).Max(),
            averageAccuracy,
            points.Count(p => p.Quality == "rejected"),
            points.Any(p => p.Quality == "rejected") ? ["low-accuracy"] : []);
    }

    public async Task<IReadOnlyList<MobileLocationTrackDto>> GetTracksAsync(
        MobileLocationQueryRequest request,
        CancellationToken ct = default)
    {
        var context = _queryService.Normalize(request);
        var points = await LoadPointsAsync(context, ct);
        return BuildTracks(context, points.Where(p => p.Quality != "rejected").ToList());
    }

    public async Task<MobileLocationSegmentDto?> GetSegmentAsync(
        string segmentId,
        MobileLocationQueryRequest request,
        CancellationToken ct = default)
    {
        var tracks = await GetTracksAsync(request, ct);
        return tracks.SelectMany(track => track.Segments).FirstOrDefault(segment => segment.Id == segmentId);
    }

    public async Task<MobileLocationSegmentPointPageDto> GetSegmentPointsAsync(
        string segmentId,
        MobileLocationQueryRequest request,
        CancellationToken ct = default)
    {
        var context = _queryService.Normalize(request);
        var segment = await GetSegmentAsync(segmentId, request, ct);
        if (segment is null) return new MobileLocationSegmentPointPageDto([], null, false);

        var query = QueryPoints(context)
            .Where(point => point.RecordedAtUtc >= segment.StartUtc && point.RecordedAtUtc <= segment.EndUtc)
            .OrderBy(point => point.RecordedAtUtc)
            .Take(context.PageSize + 1);
        var rows = await query.ToListAsync(ct);
        var hasMore = rows.Count > context.PageSize;
        return new MobileLocationSegmentPointPageDto(
            rows.Take(context.PageSize).Select(MapPoint).ToList(),
            hasMore ? rows[context.PageSize - 1].RecordedAtUtc.ToString("O") : null,
            hasMore);
    }

    private Task<List<MobileLocationPointEntity>> LoadPointsAsync(MobileLocationQueryContext context, CancellationToken ct)
        => QueryPoints(context).OrderBy(point => point.RecordedAtUtc).ToListAsync(ct);

    private IQueryable<MobileLocationPointEntity> QueryPoints(MobileLocationQueryContext context)
    {
        var userId = MobileUserContext.RequireUserId(_currentUser);
        var query = _db.Set<MobileLocationPointEntity>().AsNoTracking().Where(point => point.UserId == userId);
        query = query.Where(point => point.RecordedAtUtc >= context.Range.RangeStartUtc && point.RecordedAtUtc < context.Range.RangeEndUtc);
        if (!string.IsNullOrWhiteSpace(context.DeviceId)) query = query.Where(point => point.DeviceId == context.DeviceId);
        if (!context.IncludeRejected) query = query.Where(point => point.Quality != "rejected");
        query = query.Where(point => point.HorizontalAccuracyMeters <= Convert.ToDecimal(context.MaxAccuracyMeters) || context.IncludeRejected);
        return query;
    }

    private static IReadOnlyList<MobileLocationTrackDto> BuildTracks(MobileLocationQueryContext context, IReadOnlyList<MobileLocationPointEntity> points)
    {
        var tracks = new List<MobileLocationTrackDto>();
        var current = new List<MobileLocationPointEntity>();
        foreach (var point in points)
        {
            if (current.Count > 0 && point.RecordedAtUtc - current[^1].RecordedAtUtc > TrackGap)
            {
                tracks.Add(BuildTrack(context, tracks.Count + 1, current));
                current = [];
            }
            current.Add(point);
        }
        if (current.Count > 0) tracks.Add(BuildTrack(context, tracks.Count + 1, current));
        return tracks;
    }

    private static MobileLocationTrackDto BuildTrack(MobileLocationQueryContext context, int index, IReadOnlyList<MobileLocationPointEntity> points)
    {
        var trackId = $"track-{index}";
        var segment = BuildSegment(context, trackId, "move", points);
        return new MobileLocationTrackDto(
            trackId,
            points[0].DeviceId,
            points[0].RecordedAtUtc,
            points[^1].RecordedAtUtc,
            segment.DistanceMeters,
            segment.DurationSeconds,
            points.Count,
            1,
            segment.Bounds,
            segment.QualityFlags,
            [segment]);
    }

    private static MobileLocationSegmentDto BuildSegment(MobileLocationQueryContext context, string trackId, string kind, IReadOnlyList<MobileLocationPointEntity> points)
    {
        var duration = Math.Max(0, (long)(points[^1].RecordedAtUtc - points[0].RecordedAtUtc).TotalSeconds);
        var distance = TotalDistanceMeters(points);
        var averageSpeed = duration == 0 ? 0 : distance / duration;
        return new MobileLocationSegmentDto(
            $"{trackId}-segment-1",
            trackId,
            points[0].DeviceId,
            kind,
            points[0].RecordedAtUtc,
            points[^1].RecordedAtUtc,
            points[0].RecordedAtUtc.ToString("yyyy-MM-dd HH:mm"),
            points[^1].RecordedAtUtc.ToString("yyyy-MM-dd HH:mm"),
            duration,
            distance,
            points.Count,
            averageSpeed,
            points.Average(point => (double)point.HorizontalAccuracyMeters),
            points.Max(point => (double)point.HorizontalAccuracyMeters),
            points.Any(point => point.Quality == "rejected") ? "usable" : "high",
            points.Any(point => point.Quality == "rejected") ? ["low-accuracy"] : [],
            Bounds(points),
            points.Select(point => new MobileLocationPathPointDto(point.Id, point.RecordedAtUtc, (double)point.Latitude, (double)point.Longitude, (double)point.HorizontalAccuracyMeters, point.Quality)).ToList());
    }

    private static double TotalDistanceMeters(IReadOnlyList<MobileLocationPointEntity> points)
    {
        var total = 0d;
        for (var i = 1; i < points.Count; i++)
            total += DistanceMeters(points[i - 1], points[i]);
        return total;
    }

    private static double DistanceMeters(MobileLocationPointEntity a, MobileLocationPointEntity b)
    {
        const double radius = 6371000;
        var lat1 = DegreesToRadians((double)a.Latitude);
        var lat2 = DegreesToRadians((double)b.Latitude);
        var deltaLat = DegreesToRadians((double)b.Latitude - (double)a.Latitude);
        var deltaLon = DegreesToRadians((double)b.Longitude - (double)a.Longitude);
        var h = Math.Sin(deltaLat / 2) * Math.Sin(deltaLat / 2)
            + Math.Cos(lat1) * Math.Cos(lat2) * Math.Sin(deltaLon / 2) * Math.Sin(deltaLon / 2);
        return 2 * radius * Math.Asin(Math.Min(1, Math.Sqrt(h)));
    }

    private static double DegreesToRadians(double degrees) => degrees * Math.PI / 180;

    private static MobileGeoBoundsDto Bounds(IReadOnlyList<MobileLocationPointEntity> points)
        => new(
            points.Min(point => (double)point.Latitude),
            points.Min(point => (double)point.Longitude),
            points.Max(point => (double)point.Latitude),
            points.Max(point => (double)point.Longitude));

    private static MobileLocationPointDto MapPoint(MobileLocationPointEntity entity)
        => new(entity.Id, entity.DeviceId, entity.RecordedAtUtc, entity.CreatedAt, (double)entity.Latitude, (double)entity.Longitude, (double)entity.HorizontalAccuracyMeters, entity.Provider, entity.Source, entity.AltitudeMeters is null ? null : (double)entity.AltitudeMeters, entity.VerticalAccuracyMeters is null ? null : (double)entity.VerticalAccuracyMeters, entity.SpeedMetersPerSecond is null ? null : (double)entity.SpeedMetersPerSecond, entity.SpeedAccuracyMetersPerSecond is null ? null : (double)entity.SpeedAccuracyMetersPerSecond, entity.BearingDegrees is null ? null : (double)entity.BearingDegrees, entity.BearingAccuracyDegrees is null ? null : (double)entity.BearingAccuracyDegrees, string.Equals(entity.Source, "auto", StringComparison.OrdinalIgnoreCase), entity.Quality, entity.RawJson);
}
```

- [ ] **Step 4: Refactor location service compatibility**

In `MobileLocationService.GetHistoryAsync`, keep existing signature but remove responsibility for new analytics. Preserve current history compatibility while future callers use aggregation service.

- [ ] **Step 5: Verify and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileLocationAggregationServiceTests
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileLocationServiceTests
```

Expected: pass.

Commit:

```powershell
git add src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs
git commit -m "feat: aggregate mobile location tracks"
```

---

## Task 8: Map Backend Location Analytics Endpoints And Contracts

**Files:**
- Modify: `src/modules/Pim.Module.Mobile/MobileModule.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs`

- [ ] **Step 1: Write failing endpoint tests**

Add to `MobileEndpointTests.cs`:

```csharp
Assert.Contains("/api/v1/mobile/location/analytics/overview", paths);
Assert.Contains("/api/v1/mobile/location/analytics/tracks", paths);
Assert.Contains("/api/v1/mobile/location/analytics/segments/{segmentId}", paths);
Assert.Contains("/api/v1/mobile/location/analytics/segments/{segmentId}/points", paths);
```

- [ ] **Step 2: Run endpoint tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileEndpointTests
```

Expected: fails because routes are not mapped.

- [ ] **Step 3: Register services and map routes**

In `RegisterServices`, add:

```csharp
services.AddScoped<MobileLocationQueryService>();
services.AddScoped<MobileLocationAggregationService>();
```

In `MapEndpoints`, add:

```csharp
group.MapGet("/location/analytics/overview", async (
    [AsParameters] MobileLocationEndpointQuery query,
    [FromServices] MobileLocationAggregationService service,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<MobileLocationAnalyticsOverviewResponse>.Ok(await service.GetOverviewAsync(query.ToRequest(), ct))));

group.MapGet("/location/analytics/tracks", async (
    [AsParameters] MobileLocationEndpointQuery query,
    [FromServices] MobileLocationAggregationService service,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<IReadOnlyList<MobileLocationTrackDto>>.Ok(await service.GetTracksAsync(query.ToRequest(), ct))));

group.MapGet("/location/analytics/segments/{segmentId}", async (
    [FromRoute] string segmentId,
    [AsParameters] MobileLocationEndpointQuery query,
    [FromServices] MobileLocationAggregationService service,
    CancellationToken ct) =>
{
    var segment = await service.GetSegmentAsync(segmentId, query.ToRequest(), ct);
    return segment is null
        ? Results.NotFound(ApiResponse<string>.Fail("Location segment not found."))
        : Results.Ok(ApiResponse<MobileLocationSegmentDto>.Ok(segment));
});

group.MapGet("/location/analytics/segments/{segmentId}/points", async (
    [FromRoute] string segmentId,
    [AsParameters] MobileLocationEndpointQuery query,
    [FromServices] MobileLocationAggregationService service,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<MobileLocationSegmentPointPageDto>.Ok(await service.GetSegmentPointsAsync(segmentId, query.ToRequest(), ct))));
```

Add endpoint query record:

```csharp
public sealed record MobileLocationEndpointQuery(
    DateTimeOffset? RangeStartUtc,
    DateTimeOffset? RangeEndUtc,
    string? Timezone,
    string? DeviceId,
    double? MaxAccuracyMeters,
    bool? IncludeRejected,
    string? Cursor,
    int? PageSize)
{
    public MobileLocationQueryRequest ToRequest()
        => new(RangeStartUtc, RangeEndUtc, Timezone, DeviceId, MaxAccuracyMeters, IncludeRejected, Cursor, PageSize);
}
```

- [ ] **Step 4: Add JSON contract tests**

In `MobileWebContractTests.cs`, add a test that serializes `ApiResponse<MobileLocationAnalyticsOverviewResponse>` and asserts JSON includes:

```csharp
Assert.Contains("\"pointCount\":428", json);
Assert.Contains("\"usablePointCount\":391", json);
Assert.Contains("\"distanceMeters\":84600", json);
Assert.Contains("\"qualityFlags\":[\"low-accuracy-cluster\"]", json);
```

- [ ] **Step 5: Verify and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileEndpointTests
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MobileWebContractTests
```

Expected: pass.

Commit:

```powershell
git add src/modules/Pim.Module.Mobile/MobileModule.cs tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs
git commit -m "feat: expose mobile location analytics endpoints"
```

---

## Task 9: Build Historical Location Accepted Workbench UI

**Files:**
- Modify: `src/client-web/src/pages/HistoricalLocationPage.tsx`
- Modify: `src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx`
- Create: `src/client-web/src/components/mobile/LocationMetricStrip.tsx`
- Create: `src/client-web/src/components/mobile/LocationSegmentDetail.tsx`
- Create: `src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx`
- Create: `src/client-web/src/components/mobile/LocationRawPointTable.tsx`
- Modify: `src/client-web/src/components/mobile/locationFormatting.ts`
- Create: `tests/client-web/locationAnalyticsComponents.test.tsx`
- Create: `tests/client-web/locationAnalyticsInteractions.test.tsx`
- Modify: `tests/client-web/tsconfig.mobile.json`

- [ ] **Step 1: Write failing historical location UI tests**

Create `tests/client-web/locationAnalyticsComponents.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import HistoricalLocationDashboard from '../../src/client-web/src/components/mobile/HistoricalLocationDashboard';
import type { MobileDevice, MobileLocationAnalyticsOverview, MobileLocationTrack } from '../../src/client-web/src/api/mobile';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
(globalThis as typeof globalThis & { React: typeof React }).React = React;

const devices: MobileDevice[] = [];
const overview: MobileLocationAnalyticsOverview = {
  range: { rangeStartUtc: '2026-07-01T16:00:00Z', rangeEndUtc: '2026-07-08T16:00:00Z', timezone: 'Asia/Shanghai', localStartDate: '2026-07-02', localEndDate: '2026-07-08' },
  generatedAt: '2026-07-08T00:12:00Z',
  pointCount: 428,
  usablePointCount: 391,
  rejectedPointCount: 37,
  activeSpanSeconds: 583200,
  distanceMeters: 84600,
  stayCount: 12,
  longestStaySeconds: 12000,
  averageAccuracyMeters: 18,
  qualityIssueCount: 2,
  qualityFlags: ['low-accuracy-cluster'],
};
const tracks: MobileLocationTrack[] = [];

const html = renderToStaticMarkup(
  React.createElement(HistoricalLocationDashboard, {
    rangeShortcut: '7d',
    rangeStartDate: '2026-07-02',
    rangeEndDate: '2026-07-08',
    selectedDeviceId: '',
    devices,
    maxAccuracyMeters: 50,
    overview,
    tracks,
    selectedSegmentId: null,
    selectedPointId: null,
    points: [],
    isLoading: false,
    isFetching: false,
    errorMessage: null,
    onShortcutChange: () => undefined,
    onCustomRangeChange: () => undefined,
    onDeviceChange: () => undefined,
    onMaxAccuracyChange: () => undefined,
    onRefresh: () => undefined,
    onSelectSegment: () => undefined,
    onSelectPoint: () => undefined,
  }),
);

for (const text of ['历史位置', '今天', '7天', '30天', '自定义', '北京时间', '定位点', '活跃跨度', '估算里程', '停留点', '平均误差', '质量提示', '轨迹地图', '选中片段', '停留与移动时间线', '原始点明细']) {
  assert.equal(html.includes(text), true, `historical location UI should include ${text}`);
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/locationAnalyticsComponents.test.tsx
```

Expected: fails because dashboard props and components are not updated.

- [ ] **Step 3: Implement formatter additions**

In `locationFormatting.ts`, add:

```ts
export function formatDistanceMeters(meters: number | null | undefined) {
  if (!meters || meters <= 0) return '0 m';
  if (meters >= 1000) return `${(meters / 1000).toFixed(1)} km`;
  return `${Math.round(meters)} m`;
}

export function formatSpeedMetersPerSecond(value: number | null | undefined) {
  if (!value || value <= 0) return '0 km/h';
  return `${(value * 3.6).toFixed(1)} km/h`;
}

export function segmentKindLabel(kind: string) {
  if (kind === 'move') return '移动';
  if (kind === 'stay') return '停留';
  if (kind === 'gap') return '缺口';
  return '未知';
}
```

- [ ] **Step 4: Implement location metric strip and detail components**

Create `LocationMetricStrip.tsx` using `MobileMetricGrid`:

```tsx
import type { MobileLocationAnalyticsOverview } from '../../api/mobile';
import MobileMetricGrid from './MobileMetricGrid';
import { formatDistanceMeters } from './locationFormatting';
import { formatDuration } from './mobileFormatting';

export default function LocationMetricStrip({ overview }: { overview: MobileLocationAnalyticsOverview | null | undefined }) {
  return (
    <MobileMetricGrid items={[
      { label: '定位点', value: String(overview?.pointCount ?? 0), helper: `保留 ${overview?.usablePointCount ?? 0} 个` },
      { label: '活跃跨度', value: formatDuration(overview?.activeSpanSeconds ?? 0), helper: `缺口 ${overview?.qualityIssueCount ?? 0} 段` },
      { label: '估算里程', value: formatDistanceMeters(overview?.distanceMeters), helper: '按轨迹片段估算' },
      { label: '停留点', value: String(overview?.stayCount ?? 0), helper: `最长 ${formatDuration(overview?.longestStaySeconds ?? 0)}` },
      { label: '平均误差', value: `${Math.round(overview?.averageAccuracyMeters ?? 0)} m`, helper: 'GPS / 网络混合' },
      { label: '质量提示', value: `${overview?.qualityIssueCount ?? 0} 条`, helper: '有低精度密集点', tone: 'warning' },
    ]} />
  );
}
```

Create `LocationSegmentDetail.tsx` with the accepted `选中片段` layout.

Create `LocationStayMoveTimeline.tsx` with rows for `停留` and `移动`.

Create `LocationRawPointTable.tsx` with columns `时间 / 来源 / 误差 / 质量`.

- [ ] **Step 5: Update dashboard and page**

Modify `HistoricalLocationPage.tsx` to use:

```ts
const [rangeShortcut, setRangeShortcut] = useState<MobileRangeShortcut>('7d');
const defaultRange = useMemo(() => buildMobileAnalyticsDateRange('7d'), []);
const utcRange = useMemo(() => toMobileAnalyticsUtcRange({ startDate: rangeStartDate, endDate: rangeEndDate }), [rangeStartDate, rangeEndDate]);
```

Query:

```ts
getMobileLocationAnalyticsOverview(locationQuery)
getMobileLocationAnalyticsTracks(locationQuery)
getMobileLocationAnalyticsSegmentPoints(selectedSegmentId, locationQuery)
```

Modify `HistoricalLocationDashboard.tsx` to render header, filters, `LocationMetricStrip`, map/detail two-column workbench, lower timeline/raw table panels.

- [ ] **Step 6: Update tsconfig and verify**

Add to `tests/client-web/tsconfig.mobile.json` include array:

```json
"locationAnalyticsComponents.test.tsx",
"locationAnalyticsInteractions.test.tsx",
"../../src/client-web/src/pages/HistoricalLocationPage.tsx"
```

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/locationAnalyticsComponents.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: pass.

Commit:

```powershell
git add src/client-web/src/pages/HistoricalLocationPage.tsx src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx src/client-web/src/components/mobile/LocationMetricStrip.tsx src/client-web/src/components/mobile/LocationSegmentDetail.tsx src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx src/client-web/src/components/mobile/LocationRawPointTable.tsx src/client-web/src/components/mobile/locationFormatting.ts tests/client-web/locationAnalyticsComponents.test.tsx tests/client-web/locationAnalyticsInteractions.test.tsx tests/client-web/tsconfig.mobile.json
git commit -m "feat: apply accepted historical location workbench"
```

---

## Task 10: Render Location Track Map Layers

**Files:**
- Modify: `src/client-web/src/components/mobile/LocationHistoryMap.tsx`
- Modify: `src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx`
- Modify: `src/client-web/src/index.css` or existing global stylesheet that owns Leaflet marker CSS
- Modify: `tests/client-web/locationAnalyticsComponents.test.tsx`

- [ ] **Step 1: Write failing source assertions**

Add to `tests/client-web/locationAnalyticsComponents.test.tsx`:

```ts
import { readFileSync } from 'node:fs';

const leafletSource = readFileSync(
  path.join(process.cwd(), 'src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx'),
  'utf8',
);
assert.equal(leafletSource.includes('Polyline'), true);
assert.equal(leafletSource.includes('selectedSegmentId'), true);
assert.equal(leafletSource.includes('pathOptions'), true);
assert.equal(leafletSource.includes('#2563eb'), true);
assert.equal(leafletSource.includes('#e11d48'), true);
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/locationAnalyticsComponents.test.tsx
```

Expected: fails because map does not accept segments.

- [ ] **Step 3: Update map props**

Change `LocationHistoryMapProps`:

```ts
export interface LocationHistoryMapProps {
  tracks: MobileLocationTrack[];
  selectedSegmentId?: string | null;
  selectedPointId?: string | null;
  onSelectSegment?: (segmentId: string) => void;
  onSelectPoint?: (pointId: string) => void;
}
```

Keep fallback rendering for non-browser environment with summary badges `轨迹`, `停留`, `平均误差`.

- [ ] **Step 4: Update Leaflet rendering**

In `HistoricalLocationLeafletMap.tsx`, render one `Polyline` per segment:

```tsx
{tracks.flatMap(track => track.segments).map(segment => (
  <Polyline
    key={segment.id}
    positions={segment.path.map(point => [point.latitude, point.longitude] as [number, number])}
    pathOptions={{
      color: segment.id === selectedSegmentId ? '#e11d48' : segment.kind === 'move' ? '#2563eb' : '#14b8a6',
      weight: segment.id === selectedSegmentId ? 5 : 3,
      dashArray: segment.kind === 'move' ? undefined : '8 8',
    }}
    eventHandlers={{ click: () => onSelectSegment?.(segment.id) }}
  />
))}
```

Add marker classes:

```tsx
const selectedMarkerIcon = L.divIcon({
  className: 'pim-location-marker pim-location-marker-selected',
  html: '<span></span>',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});
```

- [ ] **Step 5: Add marker CSS**

In the global stylesheet that already contains Tailwind imports, add:

```css
.pim-location-marker span {
  display: block;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  border: 3px solid #fff;
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.pim-location-marker-selected span {
  width: 22px;
  height: 22px;
  background: #e11d48;
  box-shadow: 0 0 0 6px rgba(225, 29, 72, 0.15), 0 4px 12px rgba(225, 29, 72, 0.35);
}
```

- [ ] **Step 6: Verify and commit**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/locationAnalyticsComponents.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: pass.

Commit:

```powershell
git add src/client-web/src/components/mobile/LocationHistoryMap.tsx src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx src/client-web/src/index.css tests/client-web/locationAnalyticsComponents.test.tsx
git commit -m "feat: render mobile location track layers"
```

---

## Task 11: Full Local Verification

**Files:**
- No source edits unless tests fail.

- [ ] **Step 1: Run mobile frontend focused checks**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileTypes.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileFormatting.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsInteractions.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/locationAnalyticsComponents.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/locationAnalyticsInteractions.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: every command exits 0.

- [ ] **Step 2: Run backend focused checks**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Pim.UnitTests.Mobile
```

Expected: all mobile unit tests pass.

- [ ] **Step 3: Run full builds**

Run:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
```

Expected:

- `dotnet test Pim.sln` passes.
- `npm --prefix src/client-web run build` passes.

- [ ] **Step 4: Optional lint**

Run:

```powershell
npm --prefix src/client-web run lint
```

Expected: pass. If lint exposes pre-existing unrelated warnings, document exact output and keep code changes focused.

- [ ] **Step 5: Visual verification with browser**

Start the web app using the repository's normal dev command:

```powershell
npm --prefix src/client-web run dev -- --host 127.0.0.1
```

Open the local URL in the in-app browser. Verify:

- `手机记录` first screen matches the accepted UI baseline.
- Heatmap shows date rows and hour columns, no repeated hour-number wall.
- Clicking a heatmap cell updates `选中时段` and keeps `7天` selected.
- `历史位置` first screen shows `轨迹地图` left and `选中片段` right.
- Narrow viewport stacks main view above details without text overflow.

Keep screenshot evidence for the final report.

---

## Task 12: Commit, Push, And Wait For GitHub Actions

**Files:**
- No source edits unless verification exposes a defect.

- [ ] **Step 1: Confirm branch and staged files**

Run:

```powershell
git status --short --branch
```

Expected: only intentional changes are present and current branch is `master`.

- [ ] **Step 2: Commit final integration changes**

If Task 11 required integration fixes, commit them:

```powershell
git add <intentional-files>
git commit -m "fix: stabilize mobile analytics workbench"
```

If there are no integration fixes, continue.

- [ ] **Step 3: Push master**

Run:

```powershell
git push origin master
```

Expected: push succeeds.

- [ ] **Step 4: Wait for GitHub Actions**

Run:

```powershell
gh run list --repo 2746267826/pim-platform --branch master --limit 10
```

For each triggered workflow, run:

```powershell
gh run watch <run-id> --repo 2746267826/pim-platform --exit-status
```

Expected triggered workflows:

- `Build Web Client` when `src/client-web/**` changed.
- `Build API` when `src/modules/**`, `src/Pim.Api/**`, `tests/**`, or solution files changed.
- `Build Android APK` only if Android paths changed.
- `Build Windows Client` only if Windows paths changed.

- [ ] **Step 5: Final status report**

Report:

- Commit hashes.
- Local test commands and pass/fail status.
- GitHub Actions run IDs and pass/fail status.
- Any unrelated or environment-specific failures, with exact workflow names.

---

## Self-Review Checklist

- Spec coverage: The plan maps to the accepted UI baseline, mobile records heatmap/detail, historical location map/detail, location analytics backend, Chinese UI copy, tests, push, and GitHub Actions verification.
- Red-flag scan: This document contains no unfinished-marker words or vague catch-all implementation steps.
- Type consistency: Frontend location type names (`MobileLocationAnalyticsOverview`, `MobileLocationTrack`, `MobileLocationSegment`, `MobileLocationSegmentPointPage`) align with backend DTO names (`MobileLocationAnalyticsOverviewResponse`, `MobileLocationTrackDto`, `MobileLocationSegmentDto`, `MobileLocationSegmentPointPageDto`) through JSON camel-casing.
- Subagent requirement: Execution requires subagent-driven development and assigns disjoint write scopes.
- UI guardrail: Tests and tasks explicitly require the accepted first-screen layouts, labels, and heatmap/map semantics.

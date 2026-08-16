# PIM 前端 ECharts 改造（阶段 3）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 手写图表全部换 ECharts（数据接口不变只换渲染层），接入阶段 2 聚合接口补齐今日页/电脑记录页/手机记录页的新面板，历史位置地图增加轨迹平滑、停留点精度圈与常去地点热区。

**架构：** `echarts/core` 按需注册（一个集中模块）+ 自写 `EChartBox` 薄封装（init/setOption/ResizeObserver/dispose，SSR 安全）；每个图表的 option 构建抽成**纯函数**（输入 props 数据、输出 `EChartsOption`），组件只负责取数与容器——option 纯函数可用现有 tsx 测试体系直接断言，绕开 canvas 无法静态渲染的测试盲区。既有 API 全部复用，仅新增阶段 2 聚合端点的客户端函数。

**技术栈：** React 19 + TypeScript + Vite 8（rolldown）、echarts（新依赖，按需引入）、react-leaflet 5（已有）、TanStack Query、node:test + tsx 测试模式。

**需求文档：** `PIM展示层与分类体系改造需求文档_20260815_2317.md` §4（前端展示层改造）。

**worktree：** `/workspace/pim-wt/echarts-fe`（分支 `opencode-linux/echarts-frontend`，基于 master 45b8112b）

---

## 0. 已锁定的设计决策（调查结论 + 需求综合）

### 0.1 ECharts 引入与封装

- `npm i echarts`（^5）加入 dependencies；**禁止整包 import**，统一从 `echarts/core` 按需注册。
- 集中注册模块 `src/client-web/src/lib/echarts.ts`：注册 `BarChart/LineChart/PieChart/HeatmapChart/CustomChart/GaugeChart` + `GridComponent/TooltipComponent/LegendComponent/VisualMapComponent/DataZoomComponent/GraphicComponent/MarkAreaComponent` + `CanvasRenderer`，导出 `echarts` 实例与 `EChartsOption` 类型。
- `EChartBox`（`src/client-web/src/components/charts/EChartBox.tsx`）：props `{ option, height?, className?, ariaLabel?, onEvents? }`；`useEffect` 里 init（判空防 SSR）、`ResizeObserver` 自适应、option 变更 `setOption(option, { notMerge: true })`、卸载 dispose + off/all 移除事件；容器必须有显式高度（默认 240）。
- 色板常量 `chartColors.ts` 镜像 `index.css` 的 `--pim-*`（canvas 不继承 CSS 变量）：primary `#2563eb`、activity `#14b8a6`、warning `#f59e0b`、danger `#ef4444`、textMuted `#64748b`、borderSoft `#e2e8f0`、surfaceMuted `#f8fafc`；teal 热力色阶 `['#f8fafc','#ccfbf1','#5eead4','#2dd4bf','#0f766e']`；分类 7 色沿用 `CategoryLegacyMapper`（编程/折腾 #6B5EE4、学习 #14b8a6、视频 #F97316、聊天 #3B82F6、文档 #F59E0B、游戏 #F43F5E、其他 #64748b）。
- **测试策略**：所有 option 构建器纯函数化（如 `buildFocusAreaOption(heatmap)`），测试直接断言 `option.series[0].data`、`visualMap`、`xAxis.data`——不启动浏览器。组件静态渲染只断言占位容器（`<div role="img" aria-label>`）与非 canvas 附属文本。

### 0.2 数据接口（既有全复用，新增 6 个客户端函数）

pcTracker.ts 新增（query 统一 `date` 或 `start&end` + `timezone`，默认不传时区）：

```ts
export interface PcFocusBlockItem { startUtc: string; endUtc: string; startLocal: string; endLocal: string; durationMinutes: number; mainApp: string; topApps: { name: string; minutes: number }[] }
export interface PcFocusBlocksResponse { items: PcFocusBlockItem[] }
export interface PcAppUsageItem { appName: string; displayName: string | null; totalMinutes: number; percentage: number }
export interface PcAppUsageResponse { items: PcAppUsageItem[]; totalMinutes: number }
export interface PcLateNightDayItem { date: string; minutes: number; hadActivity: boolean }
export interface PcLateNightResponse { items: PcLateNightDayItem[] }
export interface PcCategoryDistributionItem { categoryName: string; color: string; minutes: number; percentage: number }
export interface PcCategoryDistributionResponse { items: PcCategoryDistributionItem[] }
export const pcAggregationApiPaths = {
  focusBlocks(params: { date?: string; start?: string; end?: string; timezone?: string }): string,
  appUsage(params & { limit?: number }): string,
  lateNight(params): string,
  categoryDistribution(params): string,
}
export const getPcFocusBlocks / getPcAppUsage / getPcLateNight / getPcCategoryDistribution
```

mobile.ts 新增（复用 `MobileLocationAnalyticsParams` + `withLocationAnalyticsQuery`）：

```ts
export interface MobileFrequentPlace { centerLatitude: number; centerLongitude: number; radiusMeters: number; pointCount: number; visitDayCount: number; isHome: boolean }
export interface MobileFrequentPlacesResponse { home: MobileFrequentPlace | null; places: MobileFrequentPlace[] }
export interface MobileMovementStatsResponse { homeCenter: { latitude: number; longitude: number } | null; outingCount: number; outingSeconds: number; outings: { startUtc: string; endUtc: string; seconds: number }[]; distanceMeters: number; maxSpeedMetersPerSecond: number | null; perDay: { date: string; outingCount: number; outingSeconds: number; distanceMeters: number }[] }
export const mobileApiPaths.locationAnalyticsFrequentPlaces / locationAnalyticsMovementStats
export const getMobileFrequentPlaces / getMobileMovementStats
```

注意：DTO 字段是 `centerLatitude/centerLongitude`（camelCase 序列化，与后端 `MobileFrequentPlaceDtos.cs` 一致），不是计划早期草稿的 centerLat/centerLon。

### 0.3 既有接口口径勘误（需求文档 vs 实况）

- `pc/heatmap/grid` 的 `hour` 维度返回**单日 1×24 桶**（04:00 业务时起），不是需求文档说的"24h×7"；day/month/year 是按天切桶的日历维度。**按现有维度用 ECharts 渲染**（hour→24 桶热力条、day/month/year→日历热力），不改接口。
- ActivityHeatmap 的"生产性/中性/分心"筛选 pill 是死代码（filteredCells 两分支相同，桶数据也没有 productivity 字段）→ 本阶段**移除该死 UI**，不做假筛选。
- ProductivityDashboard 现文案是「今日效率/生产性/分心」，无「生产力评分」字样；按需求去主观评判：改「专注概况」（专注占比/最长专注/碎片化/深夜使用，数据来自 focus-blocks + late-night + summary），ECharts 环形仪表替代 SVG 圆环。
- 分类时间线数据来自 `getPcSummary().timeline`（不是独立端点），改造时维持该数据流。

### 0.4 受影响测试清单（实现时同步修）

- 会破：`pcRoute3Components.test.tsx`（ActivityAnalysisHeatmap SSR 的 aria-pressed/「30 活跃分钟」断言、PcTrackerPage 源码字符串）；`mobileAnalyticsInteractions.test.tsx`（条形行 onClick 元素树、`data-bucket-start` 按钮点击两个用例）。
- 保护性约束：`locationAnalyticsComponents.test.tsx`（CI）要求 Leaflet 源码保留 `Polyline/selectedSegmentId/pathOptions/#2563eb/#e11d48/#14b8a6/pim-location-marker-selected`——地图增强不得移除这些；`mobileMapDisplayModel.test.ts`（CI）要求 `buildMapDisplayModel` 行为不变——**平滑函数独立于该模块**（渲染层叠加）。
- 新增测试全部追加进 `test:schedule-workbench-complete` 链（阶段 1 先例），并顺手把 mobile 分析测试中已过期的 OSM 直连断言（mobileComponents.test.tsx:531）修正为 `/tiles` 中转。

### 0.5 本地环境注意事项（worker 必读）

本地 Node 20.18.0 低于 Vite 要求的 20.19+。`npm --prefix src/client-web ci` 后若 vite 启动报 `Cannot find native binding`（npm optional-deps bug），执行：
```bash
npm pack @rolldown/binding-linux-x64-gnu@1.0.1 --pack-destination /tmp/opencode
mkdir -p src/client-web/node_modules/@rolldown/binding-linux-x64-gnu
tar -xzf /tmp/opencode/rolldown-binding-linux-x64-gnu-1.0.1.tgz -C src/client-web/node_modules/@rolldown/binding-linux-x64-gnu --strip-components=1
```
（阶段 2 实测有效；CI Node 22 不受影响。）

---

## 任务 1：ECharts 基础设施

**文件：**
- 修改：`src/client-web/package.json` / `package-lock.json`（加 echarts）
- 创建：`src/client-web/src/lib/echarts.ts`
- 创建：`src/client-web/src/components/charts/EChartBox.tsx`
- 创建：`src/client-web/src/components/charts/chartColors.ts`
- 测试：`tests/client-web/echartsInfra.test.tsx`（新建）

- [ ] **步骤 1：写失败测试**

```tsx
// tests/client-web/echartsInfra.test.tsx
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { echarts, type EChartsOption } from '../../src/client-web/src/lib/echarts';
import { chartColors } from '../../src/client-web/src/components/charts/chartColors';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
(globalThis as typeof globalThis & { React: typeof React }).React = React;
const EChartBox = require('../../src/client-web/src/components/charts/EChartBox').default;

function test(name: string, run: () => void) { run(); }

test('echarts core registers required charts without full bundle import', () => {
  assert.ok(echarts.init);
  const source = readFileSync('src/client-web/src/lib/echarts.ts', 'utf8');
  assert.ok(source.includes("from 'echarts/core'"));
  assert.ok(!source.includes("from 'echarts'\""));
});

test('EChartBox renders accessible placeholder in static markup', () => {
  const option: EChartsOption = { series: [{ type: 'bar', data: [1] }] };
  const html = renderToStaticMarkup(React.createElement(EChartBox, { option, height: 120, ariaLabel: '测试图' }));
  assert.ok(html.includes('role="img"'));
  assert.ok(html.includes('aria-label="测试图"'));
  assert.ok(html.includes('height:120px'));
});

test('chart colors mirror pim css variables', () => {
  assert.equal(chartColors.primary, '#2563eb');
  assert.equal(chartColors.activity, '#14b8a6');
  assert.equal(chartColors.heatmapTeal[0], '#f8fafc');
  assert.equal(chartColors.heatmapTeal[4], '#0f766e');
  assert.equal(chartColors.category['编程/折腾'], '#6B5EE4');
});
console.log('echartsInfra tests passed');
```

- [ ] **步骤 2：确认失败**（模块不存在）

- [ ] **步骤 3：实现**

`lib/echarts.ts`：
```ts
import * as echarts from 'echarts/core';
import { BarChart, CustomChart, GaugeChart, HeatmapChart, LineChart, PieChart } from 'echarts/charts';
import { DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent, VisualMapComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
echarts.use([BarChart, CustomChart, GaugeChart, HeatmapChart, LineChart, PieChart, DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent, VisualMapComponent, CanvasRenderer]);
export { echarts };
export type EChartsOption = echarts.EChartsCoreOption;
```

`EChartBox.tsx`（要点）：`useRef<HTMLDivElement>` + `useRef<echarts.ECharts>`；init 一次；`ResizeObserver` resize；option 变更 setOption(notMerge)；onEvents 键值对 `chart.on(name, handler)` 且变更时先 off 旧；卸载 dispose；`role="img"` + `aria-label` + 显式 height（number→px）。

`chartColors.ts`：§0.1 清单 + 分类色映射 + 注释指向 `index.css` 对应变量。

- [ ] **步骤 4：测试通过 + build**

```bash
npm --prefix src/client-web exec tsx -- tests/client-web/echartsInfra.test.tsx
npm --prefix src/client-web run build
```

- [ ] **步骤 5：把测试加进 CI 链**（package.json `test:schedule-workbench-complete` 末尾追加 `&& npm --prefix src/client-web exec tsx -- tests/client-web/echartsInfra.test.tsx`）

- [ ] **步骤 6：Commit**

```bash
git commit -m "feat: modular echarts infrastructure with accessible chart container / 按需注册的 ECharts 基础设施与无障碍图表容器"
```

---

## 任务 2：今日页（PC 概览面积图 + 分类环图 + 专注段；质量环形卡）

**文件：**
- 修改：`src/client-web/src/api/pcTracker.ts`（新增 §0.2 的 4 组类型/路径/函数）
- 创建：`src/client-web/src/components/charts/pcTodayOptions.ts`（option 纯函数）
- 修改：`src/client-web/src/components/today/TodayPcOverview.tsx`
- 修改：`src/client-web/src/components/today/TodayPcQualitySection.tsx`
- 测试：`tests/client-web/pcAggregationApiPath.test.ts`、`tests/client-web/pcTodayCharts.test.tsx`（新建）

- [x] **步骤 1：写失败测试**

`pcAggregationApiPath.test.ts`（仿 pcRoute3ApiPath 模式）至少断言：
1. `pcAggregationApiPaths.focusBlocks({ date: '2026-08-15' })` === `/pc/aggregation/focus-blocks?date=2026-08-15`
2. `appUsage({ date: '2026-08-15', limit: 8 })` 含 `limit=8`
3. `lateNight({ start: '2026-08-01', end: '2026-08-15' })` 含 start/end
4. `categoryDistribution({ date: '2026-08-15', timezone: 'Asia/Shanghai' })` 含 timezone
5. 空参数对象不产生多余 `?`

`pcTodayCharts.test.tsx` 至少断言：
1. `buildTodayActivityAreaOption(heatmap)`（输入 `summary.heatmap` 24 桶）：`xAxis.data` = PC_BUSINESS_HOURS 24 项、`series[0].type==='line'`、areaStyle 存在、数据取 `activeMinutes`
2. `buildCategoryDonutOption(distribution)`：`series[0].type==='pie'`、radius 内外环、data 首项 `{ name: '编程/折腾', value: 分钟, itemStyle: { color: '#6B5EE4' } }`、空数据返回空 data
3. `buildQualityRingOption(quality)`：环形 pie（healthy 绿 + issue 灰比例）、中心 graphic 文本 = 百分比
4. `buildFocusSummary(focusBlocks)`：`{ count, longestMinutes, totalMinutes }`（3 块 [30,82,12] → count 3 / longest 82 / total 124）
5. TodayPcOverview 静态渲染：包含「分类分布」「专注段」文案与两个占位容器 role="img"（数据由 props.summary 提供热力图；聚合数据走 useQuery，SSR 下为 loading 态——断言加载文案「加载中」存在）

- [x] **步骤 2：确认失败 → 步骤 3：实现**

- `pcTracker.ts` 按 §0.2 补全（路径函数手写 query 拼接：null/undefined/空串跳过；getter 走现有 `apiGet`）。
- `pcTodayOptions.ts`：
  - `buildTodayActivityAreaOption(heatmap: PcHeatmapBucket[]): EChartsOption`：x=业务小时标签（04:00..次日03:00，格式 `HH:00`）、y=activeMinutes、smooth line + primary 渐变 area、tooltip formatter 显示「活跃 X 分钟 / 事件 Y 次」、grid 紧凑（left 28/right 8/top 8/bottom 18）。
  - `buildCategoryDonutOption(items: PcCategoryDistributionItem[], opts?: { center?: string }): EChartsOption`：donut pie radius ['52%','74%']、label 外置 `{b} {d}%`、色取 item.color。
  - `buildQualityRingOption(healthy: number, total: number): EChartsOption`：donut [activity 色 + borderSoft 余量]、graphic 中心文本 `${pct}%`。
  - `buildFocusSummary(items: PcFocusBlockItem[])` 纯对象。
- `TodayPcOverview.tsx`：保留 4 张 MetricCard 与「主要应用」；「24 小时热力图」区块改为 `<EChartBox option={buildTodayActivityAreaOption(summary.heatmap)} height={160} ariaLabel="今日 24 小时 PC 活跃面积图" />`；新增右侧/下方双卡：「分类分布」donut（useQuery `['pc-category-distribution', date]` → `getPcCategoryDistribution({ date })`）、「专注段」摘要（useQuery focus-blocks → `buildFocusSummary` 显示 N 段/最长 X 分钟/合计 Y 分钟）。数据不可用（查询失败/空）时显示「暂无数据」小字，不阻塞其余区块。
- `TodayPcQualitySection.tsx`：在 message 文字上方加 `<EChartBox option={buildQualityRingOption(healthyComponents, totalComponents)} height={120} ariaLabel="PC 数据质量完成率" />`；healthy = `quality.components` 中 status==='healthy' 数（`normalizePcQuality` 后的字段名以现有类型为准），中心文本 `Math.round(healthy/total*100)%`。保留问题数/组件数/nextStep 与链接。

- [x] **步骤 4：测试通过 + build + Commit**

```bash
git commit -m "feat: today page echarts charts on aggregation apis / 今日页接入聚合接口的 ECharts 图表"
```

---

## 任务 3：电脑记录页 Part 1（质量环形卡 / 复盘指标行 / 应用排行条形）

**文件：**
- 修改：`src/client-web/src/components/pc-tracker/PcQualitySummary.tsx`
- 修改：`src/client-web/src/components/pc-tracker/PcReviewSummary.tsx`
- 修改：`src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx`
- 创建：`src/client-web/src/components/charts/pcPanelOptions.ts`
- 测试：`tests/client-web/pcPanelCharts.test.tsx`（新建）

- [x] **步骤 1：写失败测试**

至少断言：
1. `buildAppUsageBarOption(appUsage)`：横向 bar（yAxis category inverse）、data 前 8 项按 totalMinutes、每项 itemStyle 色取分类色或 primary、label position right 显示 `X 分钟`、xAxis value 轴隐藏
2. `buildReviewMetrics(summary, focusBlocks, lateNight, distribution)`：返回数组含 6 项 `{ label, value, helper }`——记录时长/活跃输入（来自 summary.metrics）、专注块 `N 段`（helper `最长 X 分钟`）、深夜使用 `X 分钟`（helper `23:30 后`）、分类覆盖 `XX%`（helper = 100 - 其他占比，distribution 为空时 '—'）
3. `PcReviewSummary` 静态渲染含「专注块」「深夜使用」「分类覆盖率」文案
4. `PcQualitySummary` 静态渲染含 role="img" 占位（compact 模式也有环）
5. `DailyActivityPanel` 静态渲染含「应用时长排行」标题

- [x] **步骤 2：确认失败 → 步骤 3：实现**

- `PcQualitySummary.tsx`：标题行下加质量环（复用 `buildQualityRingOption`，从 `pcPanelOptions.ts` re-export 或直接 import pcTodayOptions）+ 右侧两格「健康组件 n/总数」「问题数」；原有三格 dl、问题列表、nextSteps 全保留（compact 截断逻辑不动，StatusPage 共用不破）。
- `PcReviewSummary.tsx`：props 增加 `focusBlocks?/lateNight?/categoryDistribution?`（页面传 query 数据；组件内部不再自行请求，保持纯展示）；指标卡从 6 张硬编码改为 `buildReviewMetrics(...)` 驱动的 grid（每张 MetricCard 形态不变）。PcTrackerPage 增加 useQuery（focus-blocks/late-night/category-distribution，key 前缀 `pc-aggregation-*`，与现有查询同 date）并传 props；写回 invalidate 列表追加三个 key。
- `DailyActivityPanel.tsx`：props 增加 `appUsage?`；应用排行区（原 Top5 手写进度条）改为 `<EChartBox option={buildAppUsageBarOption(appUsage)} height={按条数 28*n+40} ariaLabel="应用时长排行" />`；分类 Top5 可点列表保留（继续用 summary.categories，onSelectCategory 不变）。空 appUsage 时回退现有 appRanking 渲染（兼容老数据）。
- `pcPanelOptions.ts`：上述两个 builder + `buildReviewMetrics`。

- [x] **步骤 4：测试通过 + build + 修 `pcRecordsReviewLayout.test.tsx` 若文案断言受影响 + Commit**

```bash
git commit -m "feat: pc page quality ring, review metrics row and app usage bars / 电脑记录页质量环形卡、复盘指标行与应用时长排行"
```

---

## 任务 4：电脑记录页 Part 2（分类甘特 / 活动热力 / 时间块热力）

**文件：**
- 修改：`src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`
- 修改：`src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx`
- 修改：`src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx`
- 创建：`src/client-web/src/components/charts/pcHeatmapOptions.ts`
- 测试：`tests/client-web/pcHeatmapCharts.test.tsx`（新建）、修改 `tests/client-web/pcRoute3Components.test.tsx`

- [x] **步骤 1：写失败测试**

`pcHeatmapCharts.test.tsx` 至少断言：
1. `buildCategoryGanttOption(timeline)`（输入 3 段 TimelineItem，跨 09:00-11:30）：`series[0].type==='custom'`、renderItem 返回 rect 形状、data 日期值来自 start/end、颜色取 categoryColor、xAxis type 'time'、yAxis data 为去重小时行（09/10/11 时）、tooltip formatter 含应用名
2. `buildActivityHeatmapOption(grid, 'hour')`：1×24 heatmap——xAxis 24 个 `HH:00`（04:00 起）、visualMap min 0 max maxKeyCount、teal 色阶、series data `[hourIndex, 0, intensityScore]`
3. `buildActivityHeatmapOption(grid, 'day')`：x=周列（周一..周日）、y=周序号行、data 索引来自 grid 行列
4. `buildAnalysisBlocksOption(blocks)`（12 块）：heatmap、xAxis = 块序号 1..12、色阶 intensityScore 0-4、点击回调经组件层 `chart.on('click')` 反查 block（option 层断言 data 含 blockIndex）
5. `ActivityAnalysisHeatmap` 静态渲染：保留详情面板文案（「活跃分钟」「上下文切换」「待分类」）与 role="img" 占位

`pcRoute3Components.test.tsx` 同步修改：删除依赖旧网格 DOM 的 `aria-pressed`/「30 活跃分钟」断言，改为断言新组件渲染 `role="img"` 占位 + 详情区文案；`PcTrackerPage` 源码字符串断言保留（组件名不变）。

- [x] **步骤 2：确认失败 → 步骤 3：实现**

- `CategoryTimeline.tsx`：保留顶部 stats bar、legend、分类统计；中段手写甘特 div 替换为 `<EChartBox option={buildCategoryGanttOption(timeline)} height={hours.length*rowHeight 等价高度} onEvents={{ click: 打开事件详情（沿用现有选中/弹窗行为，params 反查段） }} ariaLabel="分类时间线" />`。悬停 tooltip 用 ECharts tooltip（深色底、显示分类/应用/标题/时长），删除原 fixed tooltip div。
- `ActivityHeatmap.tsx`：四种 dimension 全走 `buildActivityHeatmapOption`（hour→1×24；day→7 列周历 heatmap；month→按月分组多行 heatmap；year→全年小格 heatmap，dataZoom 可选不加）；移除死的生产性筛选 pill；保留「少→多」色标说明（用 visualMap show:true 的 orient horizontal 替代或保留 HTML 说明）；onDateClick 经 `chart.on('click')` 反查 bucket.start（day/month/year 维度）。Loading/空态沿用现有。
- `ActivityAnalysisHeatmap.tsx`：网格替换为 `<EChartBox option={buildAnalysisBlocksOption(analysis.blocks)} height={按行数} onEvents={{ click: params => onSelectBlock(blocks[params.data[0]]) }} />`；选中块详情面板（时间范围/活跃分钟/切换/待分类/分类 Top4/应用 Top4）保留为 HTML（这保住 pcRoute3 静态断言改造后的文案）；选中态高亮经 option 的 `buildAnalysisBlocksOption(blocks, selectedStart)` 参数在对应格加边框（itemStyle borderColor primary）。
- `pcHeatmapOptions.ts`：三个 builder，甘特 renderItem 参考原型（rect + 白描边 + 圆角 4）。

- [x] **步骤 4：测试通过 + build + Commit**

```bash
git commit -m "feat: echarts gantt timeline and heatmaps for pc tracker / 分类甘特与活动/时间块热力图"
```

---

## 任务 5：专注概况仪表（ProductivityDashboard）

**文件：**
- 修改：`src/client-web/src/components/pc-tracker/ProductivityDashboard.tsx`
- 创建/并入：`src/client-web/src/components/charts/pcPanelOptions.ts`（加 `buildFocusGaugeOption` / `buildWeeklyTrendOption`）
- 测试：`tests/client-web/pcPanelCharts.test.tsx`（追加）

- [x] **步骤 1：写失败测试**

1. `buildFocusGaugeOption(focusBlocks, summaryMetrics)`：gauge 类型、min 0 max 100、进度 = 专注分钟/记录时长*100（clamp）、色带 primary→activity 渐变、中心 detail 文本 `{value}%`、标题「专注占比」；记录时长 0 时 value 0
2. `buildWeeklyTrendOption(daily)`：每日 bar/line（focusMinutes 或既有 productivity 数据），x=日期、tooltip 分钟
3. 组件静态渲染：含「专注概况」「最长专注」「碎片化」「深夜使用」文案；不含「今日效率」「生产性」「分心」（主观评判词移除断言）

- [x] **步骤 2：确认失败 → 步骤 3：实现**

- 标题「今日效率」→「专注概况」；删除 CircularScore SVG 与「生产性/中性/分心」三分统计（这是阶段 1 已拍板的去评判语义）。
- 新结构：左 ECharts gauge（专注占比 = focus-blocks 总分钟 / summary.metrics.totalRecordedDuration 分钟）+ 右侧 4 行描述性指标（最长专注 / 专注块数 / 碎片化 = 上下文切换次数每小时（来自 summary.metrics 或 activity-analysis，取现有可得字段）/ 深夜使用分钟（late-night API））。
- 「本周趋势」改 `buildWeeklyTrendOption`（数据源沿用 `getProductivityRange`，字段映射为每日专注/记录分钟——若该接口无专注分钟，则用 focus blocks 按日聚合不可得时降级为记录时长趋势，tooltip 注明口径）。
- useQuery 已有 dashboard 查询保留，新增 focus-blocks/late-night 复用页面层 props 或组件内自取（与 PcReviewSummary 一致：props 传入）。

- [x] **步骤 4：测试通过 + build + Commit**

```bash
git commit -m "feat: descriptive focus dashboard replacing productivity score / 描述性专注概况替代效率评分"
```

---

## 任务 6：手机记录页 ECharts

**文件：**
- 修改：`src/client-web/src/api/mobile.ts`（新增 §0.2 frequent-places/movement-stats 类型/路径/函数）
- 修改：`src/client-web/src/components/mobile/MobileUsageHeatmap.tsx`
- 修改：`src/client-web/src/components/mobile/MobileChartsGrid.tsx`
- 修改：`src/client-web/src/components/mobile/MobileTimelineBlocks.tsx`
- 创建：`src/client-web/src/components/charts/mobileChartOptions.ts`
- 测试：`tests/client-web/mobileChartOptions.test.tsx`（新建）、修改 `tests/client-web/mobileAnalyticsInteractions.test.tsx`、`tests/client-web/mobileApiPath.test.ts`（追加）、修复 `tests/client-web/mobileComponents.test.tsx`

- [x] **步骤 1：写失败测试**

`mobileChartOptions.test.tsx` 至少断言：
1. `buildUsageHeatmapOption(matrix)`（输入 buildHeatmapMatrix 产物）：xAxis=hours 0..23、yAxis=days 倒序、data 每项 `[hourIdx, dayIdx, seconds]`（0 值也入 data 以保格子）、visualMap teal 色阶 min 0 max matrix.maxSeconds、itemStyle borderColor '#fff' borderWidth 1、qualityFlags 非空格 itemStyle 标 amber 边框（经 data item itemStyle）
2. `buildAnalyticsChartOption(chart)` 按 chartType 分派：category-share→pie donut、top-apps→横向 bar（yAxis category 包名）、daily-total→line、hour-distribution→bar、category-trend→line 多 series（points 含 lifeCategory 分组）、switch-trend→bar；可点数据点带 `packageName/lifeCategory` 原始值（供点击反查）
3. `buildTimelineStripOption(blocks)`（输入时间块列表，跨 08:00-20:00，stay/move 或 lifeCategory 分类着色）：custom series rect、xAxis time、每块一个 data 项携带 block.id（componentName 编码进 data 供反查）、tooltip 显示块分类/时长
4. MobileUsageHeatmap 静态渲染：保留「使用热力图」标题、粒度分段按钮文案（小时/30 分钟/15 分钟）、role="img" 占位

`mobileApiPath.test.ts` 追加：frequent-places/movement-stats 路径含 rangeStartUtc/timezone/deviceId 参数拼接正确。

`mobileComponents.test.tsx`：把 :531 的 OSM 直连 URL 断言改为 `/tiles` 中转断言（修过期测试）。

- [x] **步骤 2：确认失败 → 步骤 3：实现**

- `MobileUsageHeatmap.tsx`：保留卡片头/粒度按钮/加载空态；CSS grid 替换为 `<EChartBox option={buildUsageHeatmapOption(matrix)} height={days.length*34+60} onEvents={{ click: params => 反查 matrix cell 的 sourceBuckets[0] → onBucketSelect }} ariaLabel="手机使用热力图" />`；继续用 `buildHeatmapMatrix`（列仍 24 小时，粒度差异在服务端桶内折叠——现状语义，不改）。
- `MobileChartsGrid.tsx`：每张卡标题保留 HTML `<h2>`；body 按 chartType 分派 `buildAnalyticsChartOption`；`chart.on('click')` 反查 point 携带的 lifeCategory/packageName 调 onCategorySelect/onAppSelect（无可点数据时忽略）；保留「暂无数据」空态。
- `MobileTimelineBlocks.tsx`：头部与列表之间插入 `<EChartBox option={buildTimelineStripOption(blocks)} height={110} onEvents={{ click: 反查 block.id → onToggleBlock(id) }} ariaLabel="停留与移动时间线" />`（当前页 blocks 即可，不做全量）；列表/分页/展开行为全部不动。
- `mobileAnalyticsInteractions.test.tsx`：两个旧 DOM 交互用例改为——①图表卡可点行为改为断言 `buildAnalyticsChartOption` 产出的 data 项携带 packageName（可点性数据层验证）+ MobileChartsGrid 静态 HTML 有按钮语义标题；②热力图点击改为断言 buildUsageHeatmapOption 的 data 反查表（`heatmapCellByParams` 辅助纯函数返回 bucket）+ 组件静态渲染保留粒度按钮。保留其余不破的用例。

- [x] **步骤 4：测试通过 + build + Commit**

```bash
git commit -m "feat: mobile echarts heatmap, typed charts and timeline strip / 手机使用热力图、分类图表与时间线条带"
```

---

## 任务 7：历史位置地图增强（平滑/停留圈/常去地点/移动统计）

**文件：**
- 修改：`src/client-web/src/api/mobile.ts`（若任务 6 未覆盖则在此补，见 §0.2）
- 创建：`src/client-web/src/components/mobile/pathSmoothing.ts`（Douglas-Peucker 纯函数）
- 修改：`src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx`
- 修改：`src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx`（或 LocationMetricStrip 处加移动统计条）
- 修改：`src/client-web/src/pages/HistoricalLocationPage.tsx`（新增 2 个 useQuery）
- 测试：`tests/client-web/mobileMapEnhancements.test.tsx`（新建）

- [ ] **步骤 1：写失败测试**

至少断言：
1. `simplifyPath(positions, toleranceMeters)`：3 点折线中点近直线（垂距 < tolerance）→ 输出 2 点；中点偏离大（> tolerance）→ 保留 3 点；首尾恒保留；空/单点/两点原样
2. `buildFrequentPlaceCircles(places)`：每 place → `{ center: [lat, lon], radiusMeters, isHome }`；isHome 用 chartColors.primary、其他 activity 色
3. `buildMovementMetricStrip(stats)`：`[{ label: '出门次数', value }, { label: '外出时长', value: 'X 小时 Y 分' }, { label: '移动里程', value: 'X.X km' }, { label: '速度峰值', value: 'X.X m/s' }]`；null stats → 全 '—'
4. LeafletMap 源码断言（保 locationAnalyticsComponents.test.tsx 的约束）：仍含 `Polyline`、`selectedSegmentId`、`#2563eb`、`#e11d48`、`#14b8a6`、`pim-location-marker-selected`，且新增含 `Circle`、`simplifyPath`
5. `buildMapDisplayModel` 行为不变（跑既有 mobileMapDisplayModel.test.ts 确认零改动通过）

- [ ] **步骤 2：确认失败 → 步骤 3：实现**

- `pathSmoothing.ts`：`export function simplifyPath<T extends { lat: number; lng: number }>(points: T[], toleranceMeters = 15): T[]`——标准 Douglas-Peucker（haversine 点到线段垂距，首尾索引恒保留，递归分段，≤2 点原样）。渲染层对每条 `movePolylines` 的 positions 先过该函数（jump 点已在 model 层剔除，顺序：剔除→简化）。默认 tolerance 15m（GPS 精度中位 8m 量级）。
- `HistoricalLocationLeafletMap.tsx`：
  - move Polyline positions 过 `simplifyPath`（props 加 `simplify?: boolean` 默认 true，选中段不简化可选——实现取始终简化，保持简单）。
  - stay marker 增加精度圈：`<Circle center={[质心]} radius={散开半径} pathOptions={{ color: chartColors.activity, weight: 1, fillOpacity: 0.08 }} />`；marker icon 按停留时长分两档尺寸（>=30 分钟大档 36px，否则 30px），选中态沿用现有类名。
  - 新 props `frequentPlaces?: MobileFrequentPlace[]`：每地一个 `<Circle radius={radiusMeters} pathOptions={{ color: isHome ? primary : activity, fillOpacity: 0.12 }}>` + `<Popup>`（家/常去 · 点数 N · 天数 D）；家额外加 label marker（divIcon 文本「家」）。
  - **不得**移除 locationAnalyticsComponents.test.tsx 断言的任何标识符/色值。
- `HistoricalLocationPage.tsx`：新增 `useQuery(['mobile-frequent-places', locationQuery])` 与 `['mobile-movement-stats', locationQuery]`（getter 复用 params；随既有刷新 force 逻辑）传给 Dashboard/Map。
- `HistoricalLocationDashboard.tsx`：`LocationMetricStrip` 下方加一行移动统计四格（`buildMovementMetricStrip`，纯 HTML MetricCard 风格，无图表）；props 增加 `movementStats?`。
- `mobileMapEnhancements.test.tsx` 覆盖以上 + `mobileMapDisplayModel.test.ts` 复跑零改动。

- [ ] **步骤 4：测试通过 + build + locationAnalytics 套件复跑 + Commit**

```bash
git commit -m "feat: path smoothing, stay radius circles and frequent place layer / 轨迹平滑、停留精度圈与常去地点热区"
```

---

## 任务 8：收尾（全量门禁 + 视觉验证 + PR + 三视角 review）

- [ ] **步骤 1：全量门禁**

```bash
dotnet test Pim.sln --no-restore
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
git diff --check origin/master
```

- [ ] **步骤 2：视觉验证（Playwright 截图，非提交物）**

按 scheduleWorkbenchVisualAudit 模式写临时脚本（/tmp/opencode 下）：Vite dev server + Playwright，`context.route` mock 全部 `/api/v1/pc/*`、`/api/v1/today/*`、`/api/v1/mobile/*` 返回构造数据（复用聚合 DTO 形状），访问 `/today`、`/pc-tracker`、`/mobile-records`、`/history-location`（路由名以 App 路由表为准），viewport 1440/390 各截图，人工核对：图表 canvas 非空（`canvas` 元素存在且宽高 > 0）、无 console error、无布局重叠。脚本不入库。

- [ ] **步骤 3：push + PR**（四节双语）

- [ ] **步骤 4：CI 门禁**（gh pr checks --watch，全绿）

- [ ] **步骤 5：三视角 review**（sol/terra/flash 并行，Important+ 清零循环）

- [ ] **步骤 6：合并后清理**（worktree remove + branch -d + master fast-forward）

---

## 明确不做（阶段 3 边界）

- 不改任何后端接口/DTO（阶段 2 已就绪；前端只加客户端函数）。
- 不做 24h×7 新矩阵接口（现有 grid 维度口径渲染；若要真 24×7 需后端改造，另行立项）。
- 不把 KeyboardHeatmap 换 ECharts（div 键帽 + SVG 鼠标保留，仅在其他任务顺手时对齐配色，不强制）。
- 不为 /pc-tracker、/mobile-records 写常驻 Playwright 视觉审计（mock fixture 成本高；本阶段以 option 纯函数测试 + 任务 8 一次性截图验证代替，常驻视觉测试另行立项）。
- 不动 TodayClassificationSuggestionsSection / LabelingQueue（阶段 1 已完成）。
- 不实现 ActivityHeatmap 的 productivity 筛选（数据无该字段，删除死 UI）。

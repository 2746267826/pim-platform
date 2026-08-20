import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import { chartColors } from '../../src/client-web/src/components/charts/chartColors';
import { PC_BUSINESS_HOURS, pcHourLabel } from '../../src/client-web/src/utils/pcBusinessDay';
import {
  buildCategoryGanttOption,
  buildActivityHeatmapOption,
  buildAnalysisBlocksOption,
  mapActivityGrid,
} from '../../src/client-web/src/components/charts/pcHeatmapOptions';
import type {
  HeatmapBucket,
  HeatmapGridResponse,
  PcActivityAnalysisBlock,
  PcActivityAnalysisResponse,
  TimelineItem,
} from '../../src/client-web/src/types';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

const ActivityAnalysisHeatmap = require('../../src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap').default;

function test(name: string, run: () => void) { run(); }

function bucket(start: string, intensityScore: number, hour = 4, extra: Partial<HeatmapBucket> = {}): HeatmapBucket {
  return {
    start,
    end: '',
    hour,
    activeMinutes: 10,
    totalEvents: 2,
    intensityScore,
    ...extra,
  };
}

function timelineItem(start: string, end: string, categoryName: string, categoryColor: string, appName: string): TimelineItem {
  return {
    start,
    end,
    durationMinutes: Math.round((new Date(end).getTime() - new Date(start).getTime()) / 60000),
    appName,
    windowTitle: null,
    categoryName,
    categoryColor,
    projectTag: null,
    classificationConfidence: 0.9,
    classificationSource: 'builtin',
    classificationExplanation: '',
  };
}

test('buildCategoryGanttOption renders custom rect gantt over deduped hour rows', () => {
  const timeline: TimelineItem[] = [
    timelineItem('2026-08-15T09:00:00', '2026-08-15T10:00:00', '编程', '#6B5EE4', 'Code.exe'),
    timelineItem('2026-08-15T10:00:00', '2026-08-15T11:00:00', '文档', '#F59E0B', 'msedge.exe'),
    timelineItem('2026-08-15T11:00:00', '2026-08-15T11:30:00', '编程', '#6B5EE4', 'Terminal'),
  ];
  const option = buildCategoryGanttOption(timeline) as any;
  const series = option.series[0];
  assert.equal(series.type, 'custom');
  assert.equal(typeof series.renderItem, 'function', 'renderItem must be a function');
  assert.equal(option.xAxis[0].type, 'time');
  assert.deepEqual(option.yAxis[0].data, ['09:00', '10:00', '11:00'], 'hour rows from segment start hours, deduped ascending');

  assert.equal(series.data.length, 3);
  assert.deepEqual(series.data[0].value, [0, new Date('2026-08-15T09:00:00').getTime(), new Date('2026-08-15T10:00:00').getTime()]);
  assert.equal(series.data[0].itemStyle.color, '#6B5EE4');
  assert.equal(series.data[1].itemStyle.color, '#F59E0B');
  assert.equal(series.data[2].value[0], 2, 'third segment sits on 11:00 row');

  // renderItem returns a rect shape with a real pixel width
  const rect = series.renderItem(
    { value: [0, new Date('2026-08-15T09:00:00').getTime(), new Date('2026-08-15T10:00:00').getTime()], data: series.data[0] },
    { coord: (v: number[]) => [v[1], v[2]], size: () => [1, 44] }
  );
  assert.equal(rect.type, 'rect');
  assert.ok(rect.shape.width > 0, 'rect width derived from start/end pixel coordinates');
  assert.equal(rect.shape.height, 22, 'rect height is half the row band');

  const tooltipText = option.tooltip.formatter({ data: { segment: timeline[0] } });
  assert.ok(tooltipText.includes('Code.exe'), 'tooltip shows app name');
  assert.ok(tooltipText.includes('编程'), 'tooltip shows category');
});

test('buildCategoryGanttOption returns empty option for empty timeline', () => {
  const option = buildCategoryGanttOption([]) as any;
  assert.equal(option.series[0].type, 'custom');
  assert.deepEqual(option.series[0].data, []);
  assert.deepEqual(option.yAxis[0].data, []);
});

test('buildActivityHeatmapOption hour dimension renders 1x24 bar from business hours', () => {
  const grid: HeatmapGridResponse = {
    dimension: 'hour',
    maxKeyCount: 4,
    grid: [PC_BUSINESS_HOURS.map((hour, index) => bucket(
      `2026-08-15T${String(hour).padStart(2, '0')}:00:00`,
      index % 4,
      hour
    ))],
  };
  const option = buildActivityHeatmapOption(grid) as any;
  assert.equal(option.series[0].type, 'heatmap');
  assert.equal(option.xAxis[0].data.length, 24);
  assert.equal(option.xAxis[0].data[0], '04:00');
  assert.equal(option.xAxis[0].data[23], '03:00');
  assert.equal(option.xAxis[0].data[3], '07:00');
  assert.equal(option.visualMap.min, 0);
  assert.equal(option.visualMap.max, 4, 'visualMap max is maxKeyCount');
  assert.deepEqual(option.visualMap.inRange.color, chartColors.githubGreen);
  assert.equal(option.series[0].data.length, 24);
  const byHour = Object.fromEntries(option.series[0].data.map((d: any) => [d.bucket.hour, d]));
  assert.deepEqual(byHour[4].value, [0, 0, 0], 'hour 04:00 bucket at x=0');
  assert.deepEqual(byHour[5].value, [1, 0, 1], 'hour 05:00 bucket at x=1');
  assert.deepEqual(byHour[3].value, [23, 0, 3], 'hour 03:00 bucket wraps to x=23');
  assert.equal(byHour[4].bucket.hour, 4, 'data carries the raw bucket for click reverse lookup');
});

test('mapActivityGrid hour dimension exposes business-hour mapping', () => {
  const map = mapActivityGrid({
    dimension: 'hour',
    maxKeyCount: 2,
    grid: [[bucket('2026-08-15T03:00:00', 1, 3), bucket('2026-08-15T04:00:00', 2, 4)]],
  })!;
  assert.equal(map.xLabels[0], '04:00');
  assert.equal(map.cells.find(c => c.bucket.hour === 3)?.x, 23);
  assert.equal(map.cells.find(c => c.bucket.hour === 4)?.x, 0);
  assert.equal(map.cells.find(c => c.bucket.hour === 4)?.y, 0);
});

test('buildActivityHeatmapOption day dimension maps buckets to weekday columns and week rows', () => {
  const grid: HeatmapGridResponse = {
    dimension: 'day',
    maxKeyCount: 10,
    grid: [
      [
        bucket('2026-08-10T04:00:00', 2), // Monday
        bucket('2026-08-11T04:00:00', 4), // Tuesday
        bucket('2026-08-15T04:00:00', 1), // Saturday
      ],
    ],
  };
  const option = buildActivityHeatmapOption(grid) as any;
  assert.deepEqual(option.xAxis[0].data, ['周一', '周二', '周三', '周四', '周五', '周六', '周日']);
  assert.deepEqual(option.yAxis[0].data, ['2026-08-10'], 'single Monday-anchored week row');
  assert.equal(option.series[0].data.length, 3);
  assert.deepEqual(option.series[0].data[0].value, [0, 0, 2]);
  assert.deepEqual(option.series[0].data[1].value, [1, 0, 4]);
  assert.deepEqual(option.series[0].data[2].value, [5, 0, 1]);
});

test('mapActivityGrid day dimension splits cross-week buckets into separate rows', () => {
  const map = mapActivityGrid({
    dimension: 'day',
    maxKeyCount: 10,
    grid: [[bucket('2026-08-17T04:00:00', 3), bucket('2026-08-10T04:00:00', 1)]],
  })!;
  assert.deepEqual(map.yLabels, ['2026-08-10', '2026-08-17'], 'weeks anchored on Monday');
  assert.equal(map.cells.find(c => c.bucket.start.startsWith('2026-08-10'))?.y, 0);
  assert.equal(map.cells.find(c => c.bucket.start.startsWith('2026-08-17'))?.y, 1);
});

test('buildActivityHeatmapOption month dimension groups rows by month with day columns', () => {
  const grid: HeatmapGridResponse = {
    dimension: 'month',
    maxKeyCount: 6,
    grid: [
      [
        bucket('2026-07-05T04:00:00', 3),
        bucket('2026-07-20T04:00:00', 1),
        bucket('2026-08-01T04:00:00', 5),
      ],
    ],
  };
  const option = buildActivityHeatmapOption(grid) as any;
  assert.deepEqual(option.yAxis[0].data, ['2026-07', '2026-08']);
  assert.equal(option.xAxis[0].data.length, 31, 'day columns 1-31');
  assert.equal(option.xAxis[0].data[0], '1');
  assert.equal(option.xAxis[0].data[30], '31');
  assert.equal(option.series[0].data.length, 3);
  assert.deepEqual(option.series[0].data[0].value, [4, 0, 3], 'day 5 -> x=4');
  assert.deepEqual(option.series[0].data[1].value, [19, 0, 1], 'day 20 -> x=19');
  assert.deepEqual(option.series[0].data[2].value, [0, 1, 5], 'August on second month row');
});

test('buildActivityHeatmapOption year dimension renders 53x7 github calendar', () => {
  const grid: HeatmapGridResponse = {
    dimension: 'year',
    maxKeyCount: 8,
    grid: [
      [
        bucket('2026-01-01T04:00:00', 4), // Thursday -> y=3, week 0
        bucket('2026-01-05T04:00:00', 2), // Monday -> y=0, week 1
        bucket('2026-08-15T04:00:00', 3), // Saturday -> y=5, week 32
      ],
    ],
  };
  const option = buildActivityHeatmapOption(grid) as any;
  assert.equal(option.xAxis[0].data.length, 53, '53 week columns');
  assert.deepEqual(option.yAxis[0].data, ['周一', '周二', '周三', '周四', '周五', '周六', '周日']);
  assert.equal(option.series[0].data.length, 3);
  assert.deepEqual(option.series[0].data[0].value, [0, 3, 4], 'Jan 1 in week 0 on Thursday row');
  assert.deepEqual(option.series[0].data[1].value, [1, 0, 2], 'Jan 5 in week 1 on Monday row');
  assert.deepEqual(option.series[0].data[2].value, [32, 5, 3], 'Aug 15 in week 32 on Saturday row');
});

test('buildActivityHeatmapOption returns empty series for undefined or empty data', () => {
  const empty = buildActivityHeatmapOption(undefined) as any;
  assert.equal(empty.series[0].type, 'heatmap');
  assert.deepEqual(empty.series[0].data, []);
  const noCells = buildActivityHeatmapOption({ dimension: 'day', maxKeyCount: 1, grid: [] }) as any;
  assert.deepEqual(noCells.series[0].data, []);
});

function makeBlocks(count: number): PcActivityAnalysisBlock[] {
  return Array.from({ length: count }, (_, i) => ({
    start: `2026-07-05T${String(i).padStart(2, '0')}:00:00Z`,
    end: `2026-07-05T${String(i + 1).padStart(2, '0')}:00:00Z`,
    intensityScore: i % 5,
    activeDurationSeconds: 1800,
    pendingClassificationCount: i % 3,
    contextSwitchCount: 2,
    categoryChangeCount: 1,
    categories: [{ categoryName: 'Programming', color: '#2563eb', durationSeconds: 1800 }],
    apps: [{ appName: 'Code.exe', durationSeconds: 1800 }],
  }));
}

test('buildAnalysisBlocksOption renders heatmap with ordinal columns and 0-4 intensity scale', () => {
  const option = buildAnalysisBlocksOption(makeBlocks(12)) as any;
  assert.equal(option.series[0].type, 'heatmap');
  assert.deepEqual(option.xAxis[0].data, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12], 'block ordinal 1..n');
  assert.equal(option.visualMap.min, 0);
  assert.equal(option.visualMap.max, 4);
  assert.deepEqual(
    option.visualMap.inRange.color,
    ['#f8fafc', '#d9f2ec', '#9fdacf', '#43afa3', '#0f8f88'],
    'existing 0-4 teal scale preserved'
  );
  assert.equal(option.series[0].data.length, 12);
  assert.equal(option.series[0].data[0].blockIndex, 0, 'data carries blockIndex for click reverse lookup');
  assert.deepEqual(option.series[0].data[3].value, [3, 0, 3]);
  assert.equal(option.series[0].data[3].blockIndex, 3);
});

test('buildAnalysisBlocksOption highlights the selected block with primary border', () => {
  const option = buildAnalysisBlocksOption(makeBlocks(12), '2026-07-05T03:00:00Z') as any;
  const selected = option.series[0].data.find((d: any) => d.blockIndex === 3);
  assert.deepEqual(selected.itemStyle, { borderColor: '#2563eb', borderWidth: 2 });
  const other = option.series[0].data.find((d: any) => d.blockIndex === 4);
  assert.notDeepEqual(other.itemStyle.borderColor, '#2563eb', 'unselected block keeps its border');
});

test('ActivityAnalysisHeatmap statically renders chart placeholder and detail panel', () => {
  const analysis: PcActivityAnalysisResponse = {
    date: '2026-07-05',
    blockMinutes: 60,
    blocks: makeBlocks(12),
  };
  const html = renderToStaticMarkup(
    React.createElement(ActivityAnalysisHeatmap, {
      analysis,
      selectedStart: null,
      onSelectBlock: () => undefined,
    })
  );
  assert.equal(html.includes('role="img"'), true, 'EChartBox placeholder renders');
  assert.equal(html.includes('活动分析'), true);
  assert.equal(html.includes('活跃分钟'), true);
  assert.equal(html.includes('上下文切换'), true);
  assert.equal(html.includes('待分类'), true);
  assert.equal(html.includes('Programming'), true, 'detail panel category list stays');
  assert.equal(html.includes('30 活跃分钟'), true, 'detail panel minutes stay');
});

console.log('pcHeatmapCharts tests passed');

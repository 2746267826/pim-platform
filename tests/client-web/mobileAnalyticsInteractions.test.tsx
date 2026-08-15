import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import type {
  MobileDevice,
  MobileAnalyticsChart,
  MobileHeatmapBucket,
} from '../../src/client-web/src/api/mobile';
import MobileAnalyticsHeader from '../../src/client-web/src/components/mobile/MobileAnalyticsHeader';
import MobileUsageHeatmap from '../../src/client-web/src/components/mobile/MobileUsageHeatmap';
import MobileChartsGrid from '../../src/client-web/src/components/mobile/MobileChartsGrid';
import MobileTimelineBlocks from '../../src/client-web/src/components/mobile/MobileTimelineBlocks';
import { buildHeatmapMatrix } from '../../src/client-web/src/components/mobile/mobileHeatmapMatrix';
import {
  buildMobileAnalyticsDateRange,
  toMobileAnalyticsUtcRange,
} from '../../src/client-web/src/components/mobile/mobileFormatting';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

function test(_name: string, run: () => void) {
  run();
}

type ReactNodeLike = {
  props?: Record<string, unknown> & { children?: unknown };
};

function flattenChildren(children: unknown): unknown[] {
  if (Array.isArray(children)) return children.flatMap(flattenChildren);
  if (children === null || children === undefined || children === false) return [];
  return [children];
}

function textContent(node: unknown): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (!node || typeof node !== 'object') return '';
  return flattenChildren((node as ReactNodeLike).props?.children).map(textContent).join('');
}

function findElement(node: unknown, predicate: (node: ReactNodeLike) => boolean): ReactNodeLike {
  if (node && typeof node === 'object') {
    const element = node as ReactNodeLike;
    if (predicate(element)) return element;
    for (const child of flattenChildren(element.props?.children)) {
      try {
        return findElement(child, predicate);
      } catch {
        // Keep searching siblings.
      }
    }
  }

  throw new Error('Expected element was not found.');
}

const device: MobileDevice = {
  id: 'device-row-1',
  deviceId: 'pixel-8',
  androidIdHash: 'hash-1',
  displayName: 'Pixel 8',
  manufacturer: 'Google',
  brand: 'google',
  model: 'Pixel 8',
  androidVersion: '16',
  sdkInt: 36,
  appVersion: '1.0.0',
  metadataJson: '{}',
  firstSeenAt: '2026-07-01T00:00:00Z',
  lastSeenAt: '2026-07-07T02:00:00Z',
  lastHeartbeatAt: '2026-07-07T02:01:00Z',
  lastSyncAt: '2026-07-07T02:02:00Z',
  isActive: true,
};

const bucket: MobileHeatmapBucket = {
  bucketStartUtc: '2026-07-06T14:00:00.000Z',
  bucketEndUtc: '2026-07-06T14:30:00.000Z',
  localDate: '2026-07-06',
  localHour: 22,
  lifeCategory: '短视频/娱乐',
  foregroundSeconds: 1800,
  qualityFlags: [],
};

test('default mobile analytics range is the last 7 Asia/Shanghai days', () => {
  const range = buildMobileAnalyticsDateRange('7d', new Date('2026-07-07T04:00:00.000Z'));
  const utcRange = toMobileAnalyticsUtcRange(range);

  assert.deepEqual(range, {
    shortcut: '7d',
    startDate: '2026-07-01',
    endDate: '2026-07-07',
  });
  assert.equal(utcRange.rangeStartUtc, '2026-06-30T16:00:00.000Z');
  assert.equal(utcRange.rangeEndUtc, '2026-07-07T16:00:00.000Z');
  assert.equal(utcRange.timezone, 'Asia/Shanghai');
});

test('header shortcut and custom controls call shared range callbacks', () => {
  const shortcutChanges: string[] = [];
  const customChanges: Array<{ startDate: string; endDate: string }> = [];
  const includeSystemNoiseChanges: boolean[] = [];

  const tree = MobileAnalyticsHeader({
    rangeShortcut: '7d',
    rangeStartDate: '2026-07-01',
    rangeEndDate: '2026-07-07',
    selectedDeviceId: device.deviceId,
    devices: [device],
    selectedCategory: '',
    packageName: '',
    includeSystemNoise: false,
    isFetching: false,
    onShortcutChange: value => shortcutChanges.push(value),
    onCustomRangeChange: value => customChanges.push(value),
    onDeviceChange: () => undefined,
    onCategoryChange: () => undefined,
    onPackageNameChange: () => undefined,
    onIncludeSystemNoiseChange: value => includeSystemNoiseChanges.push(value),
    onRefresh: () => undefined,
  });

  const thirtyDayButton = findElement(tree, node => textContent(node) === '30天');
  (thirtyDayButton.props?.onClick as () => void)();

  const customButton = findElement(tree, node => textContent(node) === '自定义');
  (customButton.props?.onClick as () => void)();

  const startDateInput = findElement(tree, node => node.props?.['aria-label'] === '开始日期');
  (startDateInput.props?.onChange as (event: { target: { value: string } }) => void)({
    target: { value: '2026-07-03' },
  });

  const includeCheckbox = findElement(tree, node => node.props?.['aria-label'] === '隐藏系统噪声');
  (includeCheckbox.props?.onChange as (event: { target: { checked: boolean } }) => void)({
    target: { checked: false },
  });

  assert.deepEqual(shortcutChanges, ['30d']);
  assert.deepEqual(customChanges, [
    { startDate: '2026-07-01', endDate: '2026-07-07' },
    { startDate: '2026-07-03', endDate: '2026-07-07' },
  ]);
  assert.deepEqual(includeSystemNoiseChanges, [true]);
});

test('chart rows are only buttons when they can update filters', () => {
  const categoryChanges: string[] = [];
  const appChanges: string[] = [];
  const chart: MobileAnalyticsChart = {
    key: 'mixed',
    title: '混合图表',
    chartType: 'mixed',
    unit: 'seconds',
    points: [
      { key: 'social', label: '社交通讯', value: 1800, lifeCategory: '社交通讯' },
      { key: 'wechat', label: '微信', value: 1200, packageName: 'com.tencent.mm' },
      { key: '09', label: '09:00', value: 600, localHour: 9 },
    ],
  };

  const tree = MobileChartsGrid({
    charts: [chart],
    isLoading: false,
    onCategorySelect: value => categoryChanges.push(value),
    onAppSelect: value => appChanges.push(value),
  });

  const categoryButton = findElement(tree, node => textContent(node).includes('社交通讯') && typeof node.props?.onClick === 'function');
  (categoryButton.props?.onClick as () => void)();
  const appButton = findElement(tree, node => textContent(node).includes('微信') && typeof node.props?.onClick === 'function');
  (appButton.props?.onClick as () => void)();
  const inertRow = findElement(tree, node => textContent(node).includes('09:00'));

  assert.deepEqual(categoryChanges, ['社交通讯']);
  assert.deepEqual(appChanges, ['com.tencent.mm']);
  assert.equal(typeof inertRow.props?.onClick, 'undefined');
});

test('heatmap granularity controls and bucket click emit shared filter state', () => {
  const granularities: string[] = [];
  const selectedBuckets: MobileHeatmapBucket[] = [];

  const tree = MobileUsageHeatmap({
    buckets: [bucket],
    granularity: 'hour',
    selectedBucketStartUtc: null,
    isLoading: false,
    onGranularityChange: value => granularities.push(value),
    onBucketSelect: selected => selectedBuckets.push(selected),
  });

  const halfHourButton = findElement(tree, node => textContent(node) === '30m');
  (halfHourButton.props?.onClick as () => void)();

  const bucketButton = findElement(tree, node => node.props?.['data-bucket-start'] === bucket.bucketStartUtc);
  (bucketButton.props?.onClick as () => void)();

  assert.deepEqual(granularities, ['30m']);
  assert.deepEqual(selectedBuckets, [bucket]);
});

test('heatmap matrix merges duplicate category buckets into one date hour cell', () => {
  const duplicateHourBuckets: MobileHeatmapBucket[] = [
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
});

test('timeline blocks expose page and page size controls', () => {
  const pages: number[] = [];
  const pageSizes: number[] = [];

  const tree = MobileTimelineBlocks({
    blocks: [],
    sessionsByBlock: {},
    eventsBySession: {},
    page: 2,
    pageSize: 20,
    totalCount: 45,
    totalPages: 3,
    isLoading: false,
    onToggleBlock: () => undefined,
    onToggleSession: () => undefined,
    onPageChange: value => pages.push(value),
    onPageSizeChange: value => pageSizes.push(value),
  });

  const previous = findElement(tree, node => node.props?.['aria-label'] === '上一页');
  const next = findElement(tree, node => node.props?.['aria-label'] === '下一页');
  const pageSizeSelect = findElement(tree, node => node.props?.['aria-label'] === '每页数量');

  (previous.props?.onClick as () => void)();
  (next.props?.onClick as () => void)();
  (pageSizeSelect.props?.onChange as (event: { target: { value: string } }) => void)({
    target: { value: '50' },
  });

  assert.deepEqual(pages, [1, 3]);
  assert.deepEqual(pageSizes, [50]);
  assert.equal(textContent(tree).includes('加载更多'), false);
});

test('mobile records page integrates analytics queries and bucket-driven shared state', () => {
  const source = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/MobileRecordsPage.tsx'),
    'utf8',
  );

  for (const text of [
    'getMobileAnalyticsOverview',
    'getMobileAnalyticsHeatmap',
    'getMobileAnalyticsCharts',
    'getMobileAnalyticsTimelineBlocks',
    'getMobileTimelineBlockSessions',
    'getMobileSessionEvents',
    '<LabelingQueue limit={20} />',
    "useState<MobileRangeShortcut>('7d')",
    'MOBILE_DEFAULT_TIMEZONE',
    'handleHeatmapBucketSelect',
    'bucket.bucketStartUtc',
    'timelinePage',
    'timelinePageSize',
    'onPageChange={setTimelinePage}',
    'onPageSizeChange={handleTimelinePageSizeChange}',
    'onCategorySelect={handleChartCategorySelect}',
    'onAppSelect={handleChartAppSelect}',
    "setPackageName('')",
    "setSelectedCategory('')",
  ]) {
    assert.equal(source.includes(text), true, `MobileRecordsPage should include: ${text}`);
  }

  assert.equal(source.includes('MobileAppCatalogManager'), false, 'MobileRecordsPage should no longer import the removed catalog manager');

  assert.equal(source.includes('setSelectedBucketRange({ startUtc: bucket.bucketStartUtc, endUtc: bucket.bucketEndUtc })'), false);
  assert.equal(source.includes('setRangeStartDate(bucket.localDate)'), false);
});

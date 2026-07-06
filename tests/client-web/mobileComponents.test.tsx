import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import type {
  MobileAppUsageSummary,
  MobileDevice,
  MobileLocationPoint,
  MobileQuality,
  MobileSyncBatchSummary,
  MobileTimelineItem,
} from '../../src/client-web/src/api/mobile';
import type { MobileQualityDiagnosticsData } from '../../src/client-web/src/components/status/MobileDiagnosticsPanel';

const clientPackagePath = path.join(process.cwd(), 'src/client-web/package.json');
const requireFromClient = createRequire(clientPackagePath);
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

function test(_name: string, run: () => void) {
  run();
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
  firstSeenAt: '2026-07-06T00:00:00Z',
  lastSeenAt: '2026-07-06T09:00:00Z',
  lastHeartbeatAt: '2026-07-06T09:01:00Z',
  lastSyncAt: '2026-07-06T09:02:00Z',
  isActive: true,
};

const ranking: MobileAppUsageSummary[] = [
  {
    packageName: 'com.tencent.mm',
    displayName: '微信',
    categoryName: '社交',
    foregroundSeconds: 4200,
    sessionCount: 8,
    launchCount: 18,
    lastUsedAt: '2026-07-06T10:00:00+08:00',
    source: 'events',
    share: 0.58,
  },
  {
    packageName: 'com.example.reader',
    displayName: '阅读器',
    categoryName: '阅读',
    foregroundSeconds: 1800,
    sessionCount: 2,
    launchCount: 3,
    lastUsedAt: '2026-07-06T08:00:00+08:00',
    source: 'fallback',
    share: 0.25,
  },
];

const syncBatch: MobileSyncBatchSummary = {
  id: 'batch-1',
  deviceId: device.deviceId,
  clientBatchId: 'client-batch-1',
  sourceWindowStartUtc: '2026-07-06T00:00:00Z',
  sourceWindowEndUtc: '2026-07-06T08:00:00Z',
  submittedAtUtc: '2026-07-06T08:02:00Z',
  status: 'partial',
  acceptedEventCount: 128,
  skippedEventCount: 2,
  acceptedLocationCount: 9,
  rejectedLocationCount: 1,
  errorMessage: '1 条定位记录因精度过低被拒绝',
};

const timelineItems: MobileTimelineItem[] = [
  {
    kind: 'session',
    id: 'session-1',
    deviceId: device.deviceId,
    packageName: ranking[0].packageName,
    displayName: ranking[0].displayName,
    start: '2026-07-06T08:00:00+08:00',
    end: '2026-07-06T08:35:00+08:00',
    durationSeconds: 2100,
    source: 'events',
    confidence: 0.98,
  },
  {
    kind: 'fallback',
    id: 'fallback-1',
    deviceId: device.deviceId,
    packageName: ranking[1].packageName,
    displayName: ranking[1].displayName,
    start: '2026-07-06T09:00:00+08:00',
    end: '2026-07-06T09:30:00+08:00',
    durationSeconds: 1800,
    source: 'fallback',
    reason: 'usage-events-missing',
  },
];

const quality: MobileQuality = {
  overallStatus: 'Warning',
  label: '需要关注',
  message: '今日数据存在少量空窗，请检查 Android 权限和同步状态。',
  checkedAt: '2026-07-06T09:05:00Z',
  components: [
    {
      key: 'mobile-sync',
      name: '移动同步',
      status: 'Warning',
      message: '最近批次部分接受。',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { acceptedCount: '128', rejectedCount: '1' },
    },
  ],
  issues: [
    {
      code: 'mobile-location-rejected',
      severity: 'Warning',
      componentKey: 'mobile-location',
      message: '1 条定位记录因精度过低被拒绝。',
      nextStep: '打开 Android 客户端后重新同步。',
    },
  ],
  nextSteps: ['打开 Android 客户端后重新同步。'],
};

const locationPoints: MobileLocationPoint[] = [
  {
    id: 'point-1',
    deviceId: device.deviceId,
    recordedAtUtc: '2026-07-06T08:15:00+08:00',
    submittedAtUtc: '2026-07-06T08:16:00+08:00',
    latitude: 31.2304,
    longitude: 121.4737,
    horizontalAccuracyMeters: 9.4,
    provider: 'gps',
    sourceKind: 'android',
    altitudeMeters: null,
    verticalAccuracyMeters: null,
    speedMetersPerSecond: null,
    speedAccuracyMetersPerSecond: null,
    bearingDegrees: null,
    bearingAccuracyDegrees: null,
    isAutoSubmitted: true,
    quality: 'high',
    rawJson: '{}',
  },
  {
    id: 'point-2',
    deviceId: device.deviceId,
    recordedAtUtc: '2026-07-06T09:15:00+08:00',
    submittedAtUtc: '2026-07-06T09:16:00+08:00',
    latitude: 31.231,
    longitude: 121.475,
    horizontalAccuracyMeters: 58,
    provider: 'network',
    sourceKind: 'android',
    altitudeMeters: null,
    verticalAccuracyMeters: null,
    speedMetersPerSecond: null,
    speedAccuracyMetersPerSecond: null,
    bearingDegrees: null,
    bearingAccuracyDegrees: null,
    isAutoSubmitted: false,
    quality: 'usable',
    rawJson: '{}',
  },
];

const canonicalDiagnostics: MobileQualityDiagnosticsData = {
  overallStatus: 'Warning',
  label: 'mobile-warning',
  message: 'canonical diagnostics',
  checkedAt: '2026-07-06T09:05:00Z',
  components: [
    {
      key: 'mobile-usage-coverage',
      name: 'usage canonical',
      status: 'Warning',
      message: 'canonical usage message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { fallbackSummaryCount: '1' },
    },
    {
      key: 'mobile-sync',
      name: 'sync canonical',
      status: 'Warning',
      message: 'canonical sync message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { failedBatchCount: '1' },
    },
    {
      key: 'mobile-location',
      name: 'location canonical',
      status: 'Warning',
      message: 'canonical location message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { rejectedLocationCount: '1' },
    },
    {
      key: 'mobile-app-metadata',
      name: 'metadata canonical',
      status: 'Warning',
      message: 'canonical metadata message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { missingAppMetadataCount: '1' },
    },
  ],
  issues: [],
  nextSteps: [],
};

const legacyDiagnostics: MobileQualityDiagnosticsData = {
  ...canonicalDiagnostics,
  components: [
    {
      key: 'fallback-only-days',
      name: 'usage legacy',
      status: 'Warning',
      message: 'legacy usage message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { fallbackSummaryCount: '1' },
    },
    {
      key: 'sync-batch-failures',
      name: 'sync legacy',
      status: 'Warning',
      message: 'legacy sync message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { failedBatchCount: '1' },
    },
    {
      key: 'location-accuracy-rejections',
      name: 'location legacy',
      status: 'Warning',
      message: 'legacy location message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { rejectedLocationCount: '1' },
    },
    {
      key: 'app-metadata-completeness',
      name: 'metadata legacy',
      status: 'Warning',
      message: 'legacy metadata message',
      checkedAt: '2026-07-06T09:05:00Z',
      details: { missingAppMetadataCount: '1' },
    },
  ],
};

async function main() {
  const { default: MobileRecordsDashboard } = await import(
    '../../src/client-web/src/components/mobile/MobileRecordsDashboard'
  );
  const { default: HistoricalLocationDashboard } = await import(
    '../../src/client-web/src/components/mobile/HistoricalLocationDashboard'
  );
  const { default: MobileMetricStrip } = await import(
    '../../src/client-web/src/components/mobile/MobileMetricStrip'
  );
  const { default: LocationPointList } = await import(
    '../../src/client-web/src/components/mobile/LocationPointList'
  );
  const { formatAccuracyLabel } = await import(
    '../../src/client-web/src/pages/HistoricalLocationPage'
  );
  const { default: MobileDiagnosticsPanel } = await import(
    '../../src/client-web/src/components/status/MobileDiagnosticsPanel'
  );

  test('mobile records dashboard renders Chinese usage, ranking, sync, quality, and fallback UI', () => {
    const html = renderToStaticMarkup(
      React.createElement(MobileRecordsDashboard, {
        date: '2026-07-06',
        selectedDeviceId: device.deviceId,
        devices: [device],
        summary: {
          date: '2026-07-06',
          deviceId: device.deviceId,
          generatedAt: '2026-07-06T09:05:00Z',
          totalForegroundSeconds: 7265,
          fallbackForegroundSeconds: 1800,
          appSwitchCount: 42,
          appsUsed: 7,
          completeness: 0.82,
          lastSyncAt: '2026-07-06T10:42:00+08:00',
          appRanking: ranking,
          syncBatches: [syncBatch],
          qualityIssueCount: 2,
        },
        timeline: {
          date: '2026-07-06',
          deviceId: device.deviceId,
          generatedAt: '2026-07-06T09:05:00Z',
          sessions: [timelineItems[0]],
          fallbackSummaries: [timelineItems[1]],
          items: timelineItems,
        },
        quality,
        isLoading: false,
        isFetching: false,
        errorMessage: null,
        onDateChange: () => undefined,
        onDeviceChange: () => undefined,
        onRefresh: () => undefined,
      })
    );

    for (const text of [
      '手机记录',
      '日期',
      '设备',
      '刷新',
      '总前台时长',
      '切换次数',
      '使用 App 数',
      '完整度',
      '质量问题',
      '最后同步',
      '时间线',
      'App 排行',
      '同步批次',
      '质量面板',
      '回退汇总',
      '微信',
      '阅读器',
      '部分接受',
    ]) {
      assert.equal(html.includes(text), true, `mobile records UI should include: ${text}`);
    }

    assert.equal(html.includes('data-summary-mode="fallback"'), true);
    assert.equal(html.includes('Timeline'), false);
    assert.equal(html.includes('App ranking'), false);
  });

  test('mobile metric strip renders stable Chinese labels and fallback summary mode', () => {
    const html = renderToStaticMarkup(
      React.createElement(MobileMetricStrip, {
        totalForegroundSeconds: 3661,
        appSwitchCount: 12,
        appsUsed: 3,
        completeness: 0.95,
        qualityIssueCount: 1,
        lastSyncAt: '2026-07-06T10:42:00+08:00',
        fallbackForegroundSeconds: 600,
      })
    );

    assert.equal(html.includes('1 小时 1 分钟'), true);
    assert.equal(html.includes('95%'), true);
    assert.equal(html.includes('回退汇总 10 分钟'), true);
  });

  test('historical location dashboard renders Chinese controls, map shell, point details, and list metadata', () => {
    const html = renderToStaticMarkup(
      React.createElement(HistoricalLocationDashboard, {
        start: '2026-07-06T00:00',
        end: '2026-07-06T23:59',
        selectedDeviceId: device.deviceId,
        devices: [device],
        maxAccuracyMeters: 100,
        points: locationPoints,
        selectedPointId: 'point-1',
        isLoading: false,
        isFetching: false,
        errorMessage: null,
        onStartChange: () => undefined,
        onEndChange: () => undefined,
        onDeviceChange: () => undefined,
        onMaxAccuracyChange: () => undefined,
        onRefresh: () => undefined,
        onSelectPoint: () => undefined,
      })
    );

    for (const text of [
      '历史位置',
      '时间范围',
      '设备',
      '最大误差',
      '刷新',
      'OpenStreetMap',
      '按时间连线',
      '定位点列表',
      '选中点详情',
      '记录时间',
      '提交时间',
      '误差',
      '提供方',
      '来源',
      '坐标',
      '质量',
      '可信',
      '31.230400, 121.473700',
      '9.4 m',
    ]) {
      assert.equal(html.includes(text), true, `historical location UI should include: ${text}`);
    }

    assert.equal(html.includes('data-point-count="2"'), true);
    assert.equal(html.includes('recorded'), false);
    assert.equal(html.includes('submitted'), false);
  });

  test('location point list renders selected details and meter formatting', () => {
    const html = renderToStaticMarkup(
      React.createElement(LocationPointList, {
        points: locationPoints,
        selectedPointId: 'point-2',
        onSelectPoint: () => undefined,
      })
    );

    assert.equal(html.includes('选中点详情'), true);
    assert.equal(html.includes('58 m'), true);
    assert.equal(html.includes('网络定位'), true);
  });

  test('formatAccuracyLabel formats one decimal meter values', () => {
    assert.equal(formatAccuracyLabel(9.4), '9.4 m');
    assert.equal(formatAccuracyLabel(58), '58 m');
    assert.equal(formatAccuracyLabel(null), '-');
  });

  test('historical location map source references Leaflet tiles and markers', () => {
    const mapSource = readFileSync(
      path.join(process.cwd(), 'src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx'),
      'utf8',
    );

    assert.equal(mapSource.includes('TileLayer'), true);
    assert.equal(mapSource.includes('Marker'), true);
    assert.equal(mapSource.includes('Polyline'), true);
    assert.equal(mapSource.includes('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'), true);
  });

  test('mobile diagnostics panel accepts canonical and legacy component keys', () => {
    const canonicalHtml = renderToStaticMarkup(
      React.createElement(MobileDiagnosticsPanel, {
        quality: canonicalDiagnostics,
      })
    );
    const legacyHtml = renderToStaticMarkup(
      React.createElement(MobileDiagnosticsPanel, {
        quality: legacyDiagnostics,
      })
    );

    assert.equal(canonicalHtml.includes('canonical usage message'), true);
    assert.equal(canonicalHtml.includes('canonical sync message'), true);
    assert.equal(canonicalHtml.includes('canonical location message'), true);
    assert.equal(canonicalHtml.includes('canonical metadata message'), true);
    assert.equal(legacyHtml.includes('legacy usage message'), true);
    assert.equal(legacyHtml.includes('legacy sync message'), true);
    assert.equal(legacyHtml.includes('legacy location message'), true);
    assert.equal(legacyHtml.includes('legacy metadata message'), true);
  });
}

main().catch(error => {
  throw error;
});

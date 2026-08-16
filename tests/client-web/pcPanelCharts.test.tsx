import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import { chartColors } from '../../src/client-web/src/components/charts/chartColors';
import {
  buildAppUsageBarOption,
  buildReviewMetrics,
} from '../../src/client-web/src/components/charts/pcPanelOptions';
import type { PcSummaryResponse } from '../../src/client-web/src/types';
import type {
  PcAppUsageResponse,
  PcCategoryDistributionResponse,
  PcFocusBlocksResponse,
  PcLateNightResponse,
} from '../../src/client-web/src/api/pcTracker';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

const PcQualitySummary = require('../../src/client-web/src/components/pc-tracker/PcQualitySummary').default;
const PcReviewSummary = require('../../src/client-web/src/components/pc-tracker/PcReviewSummary').default;
const DailyActivityPanel = require('../../src/client-web/src/components/pc-tracker/DailyActivityPanel').default;

function test(name: string, run: () => void) { run(); }

const appUsage: PcAppUsageResponse = {
  totalMinutes: 620,
  items: [
    { appName: 'Code.exe', displayName: 'Visual Studio Code', totalMinutes: 120, percentage: 40 },
    { appName: '学习', displayName: '学习 App', totalMinutes: 90, percentage: 30 },
    { appName: 'msedge.exe', displayName: 'Edge', totalMinutes: 70, percentage: 25 },
    { appName: 'wps.exe', displayName: 'WPS', totalMinutes: 50, percentage: 15 },
    { appName: 'notion.exe', displayName: 'Notion', totalMinutes: 40, percentage: 12 },
    { appName: 'wechat.exe', displayName: '微信', totalMinutes: 30, percentage: 9 },
    { appName: 'music.exe', displayName: '音乐', totalMinutes: 20, percentage: 6 },
    { appName: 'game.exe', displayName: '游戏', totalMinutes: 10, percentage: 3 },
    { appName: 'extra.exe', displayName: '额外', totalMinutes: 5, percentage: 2 },
    { appName: 'extra2.exe', displayName: '额外二', totalMinutes: 4, percentage: 1 },
  ],
};

test('buildAppUsageBarOption renders horizontal top-8 bar with category colors and right minutes label', () => {
  const option = buildAppUsageBarOption(appUsage) as any;
  assert.equal(option.yAxis[0].type, 'category');
  assert.equal(option.yAxis[0].inverse, true);
  assert.equal(option.yAxis[0].data.length, 8, 'should keep only the first 8 items by totalMinutes');
  assert.equal(option.yAxis[0].data[0], 'Visual Studio Code', 'should use displayName ?? appName');
  assert.equal(option.series[0].type, 'bar');
  assert.equal(option.series[0].data.length, 8);
  assert.equal(option.series[0].label.show, true);
  assert.equal(option.series[0].label.position, 'right');
  assert.equal(option.series[0].label.formatter, '{c} 分钟');
  assert.equal(option.xAxis[0].type, 'value');
  assert.equal(option.xAxis[0].show, false);
  assert.deepEqual(option.grid, { left: 8, right: 56, top: 8, bottom: 8, containLabel: true });
  assert.equal(option.series[0].data[0].itemStyle.color, chartColors.primary, 'unknown app falls back to primary');
  assert.equal(
    option.series[0].data[1].itemStyle.color,
    chartColors.category['学习'],
    'app matching a category name uses the category color'
  );
  assert.ok(option.tooltip, 'tooltip should exist');
});

test('buildAppUsageBarOption returns empty option for empty input', () => {
  const option = buildAppUsageBarOption(undefined) as any;
  assert.equal(option.yAxis[0].data.length, 0);
  assert.equal(option.series[0].data.length, 0);
});

const summary: PcSummaryResponse = {
  keystats: null,
  heatmap: [],
  appRanking: [],
  timeline: [],
  sessions: [],
  metrics: {
    totalRecordedDuration: '8h 12m',
    activeInputDuration: '5h 40m',
    idleDuration: '2h 32m',
    sessionCount: 4,
    activeAppCount: 9,
    totalKeyPresses: 1234,
    totalClicks: 456,
    appSwitchCount: 78,
    switchFrequency: 9.5,
    mostFocusedApp: 'Code.exe',
    keyClickRatio: 2.7,
  },
  categories: [
    { categoryName: '编程/折腾', color: '#6B5EE4', share: 0.62, keyPresses: 1000, totalClicks: 300 },
  ],
};

const focusBlocks: PcFocusBlocksResponse = {
  items: [
    { startUtc: '', endUtc: '', startLocal: '', endLocal: '', durationMinutes: 30, mainApp: 'A', topApps: [] },
    { startUtc: '', endUtc: '', startLocal: '', endLocal: '', durationMinutes: 82, mainApp: 'B', topApps: [] },
  ],
};

const lateNight: PcLateNightResponse = {
  items: [
    { date: '2026-08-14', minutes: 0, hadActivity: false },
    { date: '2026-08-15', minutes: 25, hadActivity: true },
    { date: '2026-08-16', minutes: 10, hadActivity: false },
  ],
};

const distribution: PcCategoryDistributionResponse = {
  items: [
    { categoryName: '编程/折腾', color: '#6B5EE4', minutes: 100, percentage: 50 },
    { categoryName: '学习', color: '#14b8a6', minutes: 80, percentage: 40 },
    { categoryName: '其他', color: '#64748b', minutes: 20, percentage: 10 },
  ],
};

test('buildReviewMetrics returns 6+ cards with focus, late night and coverage', () => {
  const metrics = buildReviewMetrics(summary, focusBlocks, lateNight, distribution);
  assert.ok(metrics.length >= 6, `expected at least 6 cards, got ${metrics.length}`);
  const byLabel = Object.fromEntries(metrics.map(m => [m.label, m]));
  assert.equal(byLabel['记录时长'].value, '8h 12m');
  assert.equal(byLabel['活跃输入'].value, '5h 40m');
  assert.equal(byLabel['主要分类'].value, '编程/折腾');
  assert.equal(byLabel['上下文切换'].value, '78');
  assert.equal(byLabel['专注块'].value, '2 段');
  assert.equal(byLabel['专注块'].helper, '最长 82 分钟');
  assert.equal(byLabel['深夜使用'].value, '25 分钟');
  assert.equal(byLabel['深夜使用'].helper, '23:30 后');
  assert.equal(byLabel['分类覆盖率'].value, '90%', 'coverage should be 100 - 其他 percentage');
});

test('buildReviewMetrics falls back to last item for late night when nothing had activity', () => {
  const metrics = buildReviewMetrics(
    summary,
    focusBlocks,
    { items: [{ date: '2026-08-15', minutes: 8, hadActivity: false }] },
    distribution
  );
  const lateCard = metrics.find(m => m.label === '深夜使用');
  assert.equal(lateCard?.value, '8 分钟');
});

test('buildReviewMetrics shows em dash placeholders when aggregation data is missing', () => {
  const metrics = buildReviewMetrics(summary, undefined, undefined, undefined);
  const byLabel = Object.fromEntries(metrics.map(m => [m.label, m]));
  assert.equal(byLabel['专注块'].value, '—');
  assert.equal(byLabel['专注块'].helper, '等待同步');
  assert.equal(byLabel['深夜使用'].value, '—');
  assert.equal(byLabel['分类覆盖率'].value, '—');
  assert.equal(byLabel['记录时长'].value, '8h 12m', 'summary metrics still render');
});

test('buildReviewMetrics handles distribution without 其他 entry as 100% coverage', () => {
  const noOther = buildReviewMetrics(
    summary,
    focusBlocks,
    lateNight,
    { items: [{ categoryName: '编程/折腾', color: '#6B5EE4', minutes: 100, percentage: 100 }] }
  );
  assert.equal(noOther.find(m => m.label === '分类覆盖率')?.value, '100%');
});

test('PcReviewSummary statically renders focus, late night and coverage card labels', () => {
  const html = renderToStaticMarkup(
    React.createElement(PcReviewSummary, {
      summary,
      pendingSuggestions: [],
      focusBlocks,
      lateNight,
      categoryDistribution: distribution,
    })
  );
  assert.equal(html.includes('专注块'), true);
  assert.equal(html.includes('深夜使用'), true);
  assert.equal(html.includes('分类覆盖率'), true);
  assert.equal(html.includes('2 段'), true);
  assert.equal(html.includes('25 分钟'), true);
});

test('PcQualitySummary statically renders a quality ring placeholder in normal and compact modes', () => {
  const quality = {
    overallStatus: 'Healthy',
    label: '正常',
    message: 'PC 数据质量检查完成',
    checkedAt: '2026-08-15T10:00:00',
    components: [
      { key: 'c1', name: '组件一', status: 'Healthy', message: '', details: {} },
      { key: 'c2', name: '组件二', status: 'Warning', message: '', details: {} },
    ],
    issues: [],
    nextSteps: [],
  } as any;
  const normalHtml = renderToStaticMarkup(React.createElement(PcQualitySummary, { quality }));
  const compactHtml = renderToStaticMarkup(React.createElement(PcQualitySummary, { quality, compact: true }));
  assert.ok((normalHtml.match(/role="img"/g) || []).length >= 1, `normal mode should render ring placeholder, got ${normalHtml}`);
  assert.ok((compactHtml.match(/role="img"/g) || []).length >= 1, `compact mode should render ring placeholder, got ${compactHtml}`);
});

test('DailyActivityPanel statically renders app usage bar title with appUsage present', () => {
  const html = renderToStaticMarkup(
    React.createElement(DailyActivityPanel, {
      metrics: summary.metrics,
      categories: summary.categories,
      appRanking: [],
      selectedCategory: null,
      onSelectCategory: () => undefined,
      selectedApp: null,
      onSelectApp: () => undefined,
      appUsage,
    })
  );
  assert.equal(html.includes('应用时长排行'), true);
  assert.equal(html.includes('分类排行'), true, 'category ranking list must stay');
  assert.ok((html.match(/role="img"/g) || []).length >= 1, `should render chart placeholder, got ${html}`);
});

console.log('pcPanelCharts tests passed');

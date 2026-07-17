/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/no-require-imports */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';

// ── helpers ────────────────────────────────────────────────────────
const _tests: { name: string; fn: () => void | Promise<void> }[] = [];

function test(name: string, fn: () => void | Promise<void>) {
  _tests.push({ name, fn });
}

async function _runAll(): Promise<void> {
  let exitCode = 0;
  for (const { name, fn } of _tests) {
    try {
      await fn();
      console.error(`PASS: ${name}`);
    } catch (err) {
      console.error(`FAIL: ${name}`);
      console.error(err);
      exitCode = 1;
    }
  }
  process.exit(exitCode);
}

// ────────────────────────────────────────────────────────────────────
//  Import production pure functions from state module
// ────────────────────────────────────────────────────────────────────
import {
  hasRealData,
  latestGeneratedAt,
  buildPageReport,
  formatNativeBoolean,
  formatNativeField,
  staleStatusLabel,
  nativeErrorMessage,
  generatedAtEntries,
  NATIVE_STATE_REFRESH_INTERVAL_MS,
} from '../../src/client-web/src/pages/androidTodayEmbedState';
import type { PageReportInput } from '../../src/client-web/src/pages/androidTodayEmbedState';

// ────────────────────────────────────────────────────────────────────
//  Constants
// ────────────────────────────────────────────────────────────────────

test('NATIVE_STATE_REFRESH_INTERVAL_MS equals 30000', () => {
  assert.equal(NATIVE_STATE_REFRESH_INTERVAL_MS, 30_000);
});

test('page uses NATIVE_STATE_REFRESH_INTERVAL_MS as refetchInterval for native state query', () => {
  const pageSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/AndroidTodayEmbedPage.tsx'),
    'utf8',
  );
  assert.ok(pageSource.includes('refetchInterval'),
    'page must use refetchInterval');
  assert.ok(pageSource.includes('NATIVE_STATE_REFRESH_INTERVAL_MS'),
    'page must reference NATIVE_STATE_REFRESH_INTERVAL_MS');
  assert.ok(pageSource.includes('native-state') || pageSource.includes('nativeState'),
    'page must have React Query for native state');
});

// ────────────────────────────────────────────────────────────────────
//  hasRealData – real/empty data determination
// ────────────────────────────────────────────────────────────────────

test('hasRealData true when pointCount > 0', () => {
  assert.equal(hasRealData({ pointCount: 5 } as any, [], {} as any, {} as any), true);
});

test('hasRealData true when tracks have content', () => {
  assert.equal(hasRealData({} as any, [{ id: 't1' }] as any, {} as any, {} as any), true);
});

test('hasRealData true when totalForegroundSeconds > 0', () => {
  assert.equal(hasRealData({} as any, [] as any, { totalForegroundSeconds: 100 } as any, {} as any), true);
});

test('hasRealData true when appRanking has entries', () => {
  assert.equal(hasRealData({} as any, [] as any, {} as any, { appRanking: [{ packageName: 'x' }] } as any), true);
});

test('hasRealData false when all fields empty', () => {
  assert.equal(hasRealData(null, null, null, null), false);
  assert.equal(hasRealData({ pointCount: 0 } as any, [], { totalForegroundSeconds: 0 } as any, { appRanking: [] } as any), false);
});

test('hasRealData treats undefined as empty', () => {
  assert.equal(hasRealData(undefined, undefined, undefined, undefined), false);
});

test('hasRealData true when summary.totalForegroundSeconds > 0 but no appRanking', () => {
  assert.equal(hasRealData({} as any, [] as any, {} as any, { totalForegroundSeconds: 100, appRanking: [] } as any), true);
});

// ────────────────────────────────────────────────────────────────────
//  latestGeneratedAt
// ────────────────────────────────────────────────────────────────────

test('latestGeneratedAt null for empty input', () => {
  assert.equal(latestGeneratedAt([]), null);
  assert.equal(latestGeneratedAt([null, undefined, {}]), null);
});

test('latestGeneratedAt picks the latest time', () => {
  const result = latestGeneratedAt([
    { generatedAt: '2026-07-17T01:00:00Z' },
    { generatedAt: '2026-07-17T02:00:00Z' },
    { generatedAt: '2026-07-17T00:00:00Z' },
  ]);
  assert.equal(result, '2026-07-17T02:00:00.000Z');
});

test('latestGeneratedAt ignores items without generatedAt', () => {
  const result = latestGeneratedAt([
    null,
    { generatedAt: '2026-07-17T01:00:00Z' },
    {} as any,
    undefined,
  ]);
  assert.equal(result, '2026-07-17T01:00:00.000Z');
});

// ────────────────────────────────────────────────────────────────────
//  generatedAtEntries
// ────────────────────────────────────────────────────────────────────

test('generatedAtEntries returns entries for each query that has generatedAt', () => {
  const entries = generatedAtEntries(
    { generatedAt: '2026-07-17T01:00:00Z', pointCount: 5 } as any,
    { generatedAt: '2026-07-17T02:00:00Z', totalForegroundSeconds: 3600, isStale: false } as any,
    { generatedAt: '2026-07-17T00:00:00Z', appRanking: [] } as any,
  );
  assert.equal(entries.length, 3);
  assert.ok(entries[0].label.includes('位置概况'));
  assert.ok(entries[1].label.includes('手机使用'));
  assert.ok(entries[2].label.includes('App 摘要'));
  assert.equal(entries[1].generatedAt, '2026-07-17T02:00:00Z');
});

test('generatedAtEntries null for missing generatedAt', () => {
  const entries = generatedAtEntries(
    null,
    { generatedAt: '2026-07-17T02:00:00Z', totalForegroundSeconds: 100, isStale: false } as any,
    null,
  );
  assert.equal(entries.length, 1);
  assert.ok(entries[0].label.includes('手机使用'));
});

test('generatedAtEntries no longer appends stale text to label', () => {
  const entries = generatedAtEntries(
    null,
    { generatedAt: '2026-07-17T02:00:00Z', totalForegroundSeconds: 100, isStale: true } as any,
    null,
  );
  assert.equal(entries.length, 1);
  assert.equal(entries[0].label, '手机使用');
});

test('generatedAtEntries does NOT mark stale for location or summary', () => {
  const entries = generatedAtEntries(
    { generatedAt: '2026-07-17T01:00:00Z', pointCount: 5 } as any,
    null,
    { generatedAt: '2026-07-17T00:00:00Z', appRanking: [] } as any,
  );
  const labels = entries.map(e => e.label).join(' ');
  // neither label should contain "过期"
  assert.ok(!labels.includes('过期'));
});

// ────────────────────────────────────────────────────────────────────
//  buildPageReport
// ────────────────────────────────────────────────────────────────────

test('buildPageReport hasServerData=true no error when all data present', () => {
  const input: PageReportInput = {
    locationOverview: { generatedAt: '2026-07-17T01:00:00Z', pointCount: 10 } as any,
    tracks: [{ id: 't1' }] as any,
    usageOverview: { generatedAt: '2026-07-17T02:00:00Z', totalForegroundSeconds: 3600 } as any,
    summary: { generatedAt: '2026-07-17T00:00:00Z', appRanking: [{ packageName: 'com.t' }] } as any,
  };
  const r = buildPageReport(input);
  assert.equal(r.hasServerData, true);
  assert.equal(r.generatedAt, '2026-07-17T02:00:00.000Z');
  assert.equal(r.error, null);
});

test('buildPageReport hasServerData=false when no real content', () => {
  const input: PageReportInput = {
    locationOverview: { generatedAt: '2026-07-17T01:00:00Z', pointCount: 0 } as any,
    tracks: [],
    usageOverview: { generatedAt: '2026-07-17T02:00:00Z', totalForegroundSeconds: 0 } as any,
    summary: { generatedAt: '2026-07-17T00:00:00Z', appRanking: [] } as any,
  };
  const r = buildPageReport(input);
  assert.equal(r.hasServerData, false);
});

test('buildPageReport Chinese error on partial failures', () => {
  const input: PageReportInput = {
    locationError: new Error('timeout'),
    tracksError: new Error('network'),
    usageError: new Error('server error'),
  };
  const r = buildPageReport(input);
  assert.equal(r.hasServerData, false);
  assert.ok(r.error!.includes('位置数据获取失败'), `should mention 位置, got: ${r.error}`);
  assert.ok(r.error!.includes('使用数据获取失败'), `should mention 使用, got: ${r.error}`);
  assert.ok(r.error!.includes('轨迹数据获取失败'), `should mention 轨迹, got: ${r.error}`);
});

test('buildPageReport error null when no failures', () => {
  const input: PageReportInput = {
    locationOverview: { generatedAt: '2026-07-17T01:00:00Z', pointCount: 0 } as any,
    tracks: [],
    usageOverview: { generatedAt: '2026-07-17T02:00:00Z', totalForegroundSeconds: 0 } as any,
    summary: { generatedAt: '2026-07-17T00:00:00Z', appRanking: [] } as any,
  };
  const r = buildPageReport(input);
  assert.equal(r.error, null);
});

test('buildPageReport generatedAt null when all queries fail', () => {
  const input: PageReportInput = {
    locationError: new Error('x'),
    tracksError: new Error('x'),
    usageError: new Error('x'),
    summaryError: new Error('x'),
  };
  const r = buildPageReport(input);
  assert.equal(r.generatedAt, null);
});

test('buildPageReport picks latest generatedAt across sources', () => {
  const input: PageReportInput = {
    locationOverview: { generatedAt: '2026-07-17T01:00:00Z', pointCount: 5 } as any,
    tracks: [],
    usageOverview: { generatedAt: '2026-07-17T05:00:00Z', totalForegroundSeconds: 0 } as any,
    summary: { generatedAt: '2026-07-16T23:00:00Z', appRanking: [] } as any,
  };
  const r = buildPageReport(input);
  assert.equal(r.generatedAt, '2026-07-17T05:00:00.000Z');
});

// ────────────────────────────────────────────────────────────────────
//  formatNativeBoolean / formatNativeField
// ────────────────────────────────────────────────────────────────────

test('formatNativeBoolean correct text', () => {
  assert.equal(formatNativeBoolean(true), '已开启');
  assert.equal(formatNativeBoolean(false), '已关闭');
  assert.equal(formatNativeBoolean(null), '暂无');
  assert.equal(formatNativeBoolean(undefined), '暂无');
});

test('formatNativeField shows value or 暂无', () => {
  assert.equal(formatNativeField('GPS'), 'GPS');
  assert.equal(formatNativeField(null), '暂无');
  assert.equal(formatNativeField(undefined), '暂无');
  assert.equal(formatNativeField(''), '暂无');
});

// ────────────────────────────────────────────────────────────────────
//  staleStatusLabel – 根据 isStale 返回中文标签
// ────────────────────────────────────────────────────────────────────

test('staleStatusLabel true returns 可能过期', () => {
  assert.equal(staleStatusLabel(true), '可能过期');
});

test('staleStatusLabel false returns null', () => {
  assert.equal(staleStatusLabel(false), null);
});

test('staleStatusLabel undefined returns null', () => {
  assert.equal(staleStatusLabel(undefined), null);
});

// ────────────────────────────────────────────────────────────────────
//  nativeErrorMessage – 接受 unknown 始终返回固定中文
// ────────────────────────────────────────────────────────────────────

test('nativeErrorMessage accepts unknown and returns Chinese', () => {
  assert.equal(nativeErrorMessage('bridge_unavailable'), '无法读取原生采集状态');
  assert.equal(nativeErrorMessage(null), '无法读取原生采集状态');
  assert.equal(nativeErrorMessage(undefined), '无法读取原生采集状态');
  assert.equal(nativeErrorMessage(new Error('timeout')), '无法读取原生采集状态');
});

// ────────────────────────────────────────────────────────────────────
//  App.tsx – embed route must NOT reference TodayPage directly
// ────────────────────────────────────────────────────────────────────

test('/embed/android/today route no longer references TodayPage', () => {
  const appSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/App.tsx'),
    'utf8',
  );
  const lines = appSource.split('\n');
  const embedTodayLine = lines.find(line =>
    line.includes('/embed/android/today') && line.includes('<'));
  assert.ok(embedTodayLine, 'embed today route must exist');
  assert.ok(!embedTodayLine!.includes('TodayPage'),
    `embed route should not reference TodayPage, got: ${embedTodayLine}`);
});

// ────────────────────────────────────────────────────────────────────
//  AndroidTodayEmbedPage only exports the component (no warning fix)
// ────────────────────────────────────────────────────────────────────

// ────────────────────────────────────────────────────────────────────
//  Task 2: buildPageReport all four errors use Chinese separator
// ────────────────────────────────────────────────────────────────────

test('buildPageReport all four errors produce Chinese text separated by Chinese semicolon', () => {
  const input: PageReportInput = {
    locationError: new Error('timeout'),
    tracksError: new Error('network error'),
    usageError: new Error('server error'),
    summaryError: new Error('parse error'),
  };
  const r = buildPageReport(input);
  assert.equal(r.hasServerData, false);
  assert.equal(r.generatedAt, null);
  assert.ok(r.error!.includes('位置数据获取失败'));
  assert.ok(r.error!.includes('轨迹数据获取失败'));
  assert.ok(r.error!.includes('使用数据获取失败'));
  assert.ok(r.error!.includes('摘要数据获取失败'));
  assert.ok(r.error!.includes('；'));
});

// ────────────────────────────────────────────────────────────────────
//  Task 2: source check – page no longer extracts raw Error.message
// ────────────────────────────────────────────────────────────────────

test('page no longer maps query errors with .message for display', () => {
  const pageSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/AndroidTodayEmbedPage.tsx'),
    'utf8',
  );
  assert.ok(!pageSource.includes('.error as Error)?.message'),
    'page must not extract raw Error.message for display');
  assert.ok(pageSource.includes('buildPageReport('),
    'page should use buildPageReport for error display');
});

// ────────────────────────────────────────────────────────────────────
//  Task 5.2: source check – page no longer shows （最新
// ────────────────────────────────────────────────────────────────────

test('page no longer shows latestGeneratedAt in UI', () => {
  const pageSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/AndroidTodayEmbedPage.tsx'),
    'utf8',
  );
  assert.ok(!pageSource.includes('（最新 '),
    'page should not display latestGeneratedAt in UI');
});

// ────────────────────────────────────────────────────────────────────
//  Task 5.5: source check – stable key for genEntries
// ────────────────────────────────────────────────────────────────────

test('generatedAtEntries uses stable label as key', () => {
  const pageSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/AndroidTodayEmbedPage.tsx'),
    'utf8',
  );
  assert.ok(pageSource.includes('key={entry.label}'),
    'genEntries should use stable label as key');
});

// ────────────────────────────────────────────────────────────────────
//  Task 5.4: source check – empty catch has comment
// ────────────────────────────────────────────────────────────────────

test('empty catch block in page has one-line comment', () => {
  const pageSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/AndroidTodayEmbedPage.tsx'),
    'utf8',
  );
  assert.ok(pageSource.includes('catch {'),
    'page must have an empty catch block');
  const lines = pageSource.split('\n');
  const catchLineIdx = lines.findIndex(l => l.trim() === '} catch {');
  assert.ok(catchLineIdx >= 0, 'catch { must be on its own line');
  // The line after catch should have a comment (// or /*)
  const afterCatch = lines.slice(catchLineIdx + 1).find(l => l.trim() !== '');
  assert.ok(afterCatch && (afterCatch.includes('//') || afterCatch.includes('/*')),
    'empty catch block should have a one-line comment');
});

test('AndroidTodayEmbedPage only exports default component', () => {
  const pageSource = readFileSync(
    path.join(process.cwd(), 'src/client-web/src/pages/AndroidTodayEmbedPage.tsx'),
    'utf8',
  );
  // Should not have named exports of helpers (those moved to state module)
  const namedExportCount = (pageSource.match(/^export (function|const|interface|type) /gm) || []).length;
  assert.equal(namedExportCount, 0,
    `expected 0 named exports (only default), got ${namedExportCount}`);
  assert.ok(pageSource.includes('export default function AndroidTodayEmbedPage'));
});

void _runAll();

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
//  Import production pure functions
// ────────────────────────────────────────────────────────────────────
import {
  hasRealData,
  latestGeneratedAt,
  buildPageReport,
  formatNativeBoolean,
  formatNativeField,
  staleStatusLabel,
  nativeErrorMessage,
} from '../../src/client-web/src/pages/AndroidTodayEmbedPage';
import type { PageReportInput } from '../../src/client-web/src/pages/AndroidTodayEmbedPage';

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
//  nativeErrorMessage – 机器码映射为中文错误消息
// ────────────────────────────────────────────────────────────────────

test('nativeErrorMessage maps bridge_unavailable to Chinese', () => {
  assert.equal(nativeErrorMessage('bridge_unavailable'), '无法读取原生采集状态');
});

test('nativeErrorMessage maps native_state_error to Chinese', () => {
  assert.equal(nativeErrorMessage('native_state_error'), '无法读取原生采集状态');
});

test('nativeErrorMessage maps unknown code to safe generic message', () => {
  const msg = nativeErrorMessage('SOME_RAW_MACHINE_CODE');
  assert.ok(msg.length > 0);
  assert.equal(msg, '无法读取原生采集状态');
});

test('nativeErrorMessage maps empty string to safe generic message', () => {
  assert.equal(nativeErrorMessage(''), '无法读取原生采集状态');
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

void _runAll();

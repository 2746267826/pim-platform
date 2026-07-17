import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getEmbedBridgeClient } from '../api/client';
import {
  getMobileLocationAnalyticsOverview,
  getMobileLocationAnalyticsTracks,
  getMobileAnalyticsOverview,
  getMobileSummary,
} from '../api/mobile';
import {
  buildMobileAnalyticsDateRange,
  toMobileAnalyticsUtcRange,
  formatShanghaiDateInput,
  formatDateTime,
} from '../components/mobile/mobileFormatting';
import LocationMetricStrip from '../components/mobile/LocationMetricStrip';
import LocationHistoryMap from '../components/mobile/LocationHistoryMap';
import MobileInsightStrip from '../components/mobile/MobileInsightStrip';
import MobileAppRanking from '../components/mobile/MobileAppRanking';
import type { NativeState } from '../embed/androidBridge';
import type {
  MobileLocationAnalyticsOverview,
  MobileLocationTrack,
  MobileAnalyticsOverview,
  MobileSummary,
} from '../api/mobile';

// ── Pure helper functions (exported for testing) ────────────────────

export function hasRealData(
  locationOverview: MobileLocationAnalyticsOverview | null | undefined,
  tracks: MobileLocationTrack[] | null | undefined,
  usageOverview: MobileAnalyticsOverview | null | undefined,
  summary: MobileSummary | null | undefined,
): boolean {
  return (
    (locationOverview?.pointCount ?? 0) > 0 ||
    (tracks?.length ?? 0) > 0 ||
    (usageOverview?.totalForegroundSeconds ?? 0) > 0 ||
    (summary?.appRanking?.length ?? 0) > 0 ||
    (summary?.totalForegroundSeconds ?? 0) > 0
  );
}

export function latestGeneratedAt(
  items: Array<{ generatedAt?: string } | null | undefined>,
): string | null {
  const dates = items
    .filter((i): i is { generatedAt: string } => !!i?.generatedAt)
    .map(i => new Date(i.generatedAt).getTime())
    .filter(t => !Number.isNaN(t));
  if (dates.length === 0) return null;
  return new Date(Math.max(...dates)).toISOString();
}

export interface PageReportInput {
  locationOverview?: MobileLocationAnalyticsOverview | null;
  tracks?: MobileLocationTrack[] | null;
  usageOverview?: MobileAnalyticsOverview | null;
  summary?: MobileSummary | null;
  locationError?: Error | null;
  tracksError?: Error | null;
  usageError?: Error | null;
  summaryError?: Error | null;
}

export interface PageReport {
  hasServerData: boolean;
  generatedAt: string | null;
  error: string | null;
}

export function buildPageReport(input: PageReportInput): PageReport {
  const errors: string[] = [];
  if (input.locationError) errors.push('位置数据获取失败');
  if (input.tracksError) errors.push('轨迹数据获取失败');
  if (input.usageError) errors.push('使用数据获取失败');
  if (input.summaryError) errors.push('摘要数据获取失败');

  const data = hasRealData(
    input.locationOverview,
    input.tracks,
    input.usageOverview,
    input.summary,
  );

  const gen = latestGeneratedAt([
    input.locationOverview,
    input.usageOverview,
    input.summary,
  ]);

  return {
    hasServerData: data,
    generatedAt: gen,
    error: errors.length > 0 ? errors.join('；') : null,
  };
}

export function formatNativeBoolean(value: boolean | null | undefined): string {
  if (value === true) return '已开启';
  if (value === false) return '已关闭';
  return '暂无';
}

export function formatNativeField(value: string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '暂无';
  return value;
}

export function staleStatusLabel(isStale: boolean | undefined | null): string | null {
  if (isStale === true) return '可能过期';
  return null;
}

export function nativeErrorMessage(code: string): string {
  if (code === 'bridge_unavailable' || code === 'native_state_error') return '无法读取原生采集状态';
  return '无法读取原生采集状态';
}

// ── Component ──────────────────────────────────────────────────────

export default function AndroidTodayEmbedPage() {
  const utcRange = useMemo(() => {
    const range = buildMobileAnalyticsDateRange('today');
    return toMobileAnalyticsUtcRange(range);
  }, []);

  const localDate = useMemo(() => formatShanghaiDateInput(), []);

  const [nativeState, setNativeState] = useState<NativeState | null>(null);
  const [nativeError, setNativeError] = useState<string | null>(null);
  const prevReportRef = useRef<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const bridge = await getEmbedBridgeClient();
        if (!bridge) {
          if (!cancelled) setNativeError('bridge_unavailable');
          return;
        }
        const state = await bridge.requestNativeState();
        if (!cancelled) setNativeState(state);
      } catch (err) {
        if (!cancelled) setNativeError((err as Error).message || 'native_state_error');
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const locationOverviewQ = useQuery({
    queryKey: ['android-today-location-overview', utcRange],
    queryFn: () => getMobileLocationAnalyticsOverview(utcRange),
    retry: 1,
  });

  const tracksQ = useQuery({
    queryKey: ['android-today-tracks', utcRange],
    queryFn: () => getMobileLocationAnalyticsTracks(utcRange),
    retry: 1,
  });

  const usageOverviewQ = useQuery({
    queryKey: ['android-today-usage-overview', utcRange],
    queryFn: () => getMobileAnalyticsOverview(utcRange),
    retry: 1,
  });

  const summaryQ = useQuery({
    queryKey: ['android-today-summary', localDate],
    queryFn: () => getMobileSummary(localDate),
    retry: 1,
  });

  const isLoading = locationOverviewQ.isLoading || tracksQ.isLoading || usageOverviewQ.isLoading || summaryQ.isLoading;
  const fetchErrors = [locationOverviewQ, tracksQ, usageOverviewQ, summaryQ]
    .filter(q => q.isError)
    .map(q => (q.error as Error)?.message || '未知错误');

  const reportInput = useMemo<PageReportInput>(() => ({
    locationOverview: locationOverviewQ.data ?? null,
    tracks: tracksQ.data ?? null,
    usageOverview: usageOverviewQ.data ?? null,
    summary: summaryQ.data ?? null,
    locationError: locationOverviewQ.isError ? (locationOverviewQ.error as Error) : null,
    tracksError: tracksQ.isError ? (tracksQ.error as Error) : null,
    usageError: usageOverviewQ.isError ? (usageOverviewQ.error as Error) : null,
    summaryError: summaryQ.isError ? (summaryQ.error as Error) : null,
  }), [
    locationOverviewQ.data, locationOverviewQ.isError, locationOverviewQ.error,
    tracksQ.data, tracksQ.isError, tracksQ.error,
    usageOverviewQ.data, usageOverviewQ.isError, usageOverviewQ.error,
    summaryQ.data, summaryQ.isError, summaryQ.error,
  ]);

  useEffect(() => {
    if (isLoading) return;

    const report = buildPageReport(reportInput);
    const key = JSON.stringify(report);
    if (key === prevReportRef.current) return;
    prevReportRef.current = key;

    (async () => {
      try {
        const bridge = await getEmbedBridgeClient();
        if (bridge) {
          await bridge.sendPageReport(report);
        }
      } catch {
        // 正常页面销毁产生的 bridge 错误不制造 unhandled rejection
      }
    })();
  }, [reportInput, isLoading]);

  const latestGenAt = useMemo(() => latestGeneratedAt([
    locationOverviewQ.data ?? null,
    usageOverviewQ.data ?? null,
    summaryQ.data ?? null,
  ]), [locationOverviewQ.data, usageOverviewQ.data, summaryQ.data]);

  const isStale = usageOverviewQ.data?.isStale === true;

  const settled = !isLoading;
  const anyData = hasRealData(
    locationOverviewQ.data ?? null,
    tracksQ.data ?? null,
    usageOverviewQ.data ?? null,
    summaryQ.data ?? null,
  );

  return (
    <div className="mx-auto max-w-[1500px] space-y-4 p-4 pb-8">
      <h1 className="text-xl font-semibold text-slate-950">今日数据</h1>

      {/* ── Native State ── */}
      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="text-sm font-semibold text-slate-950">采集状态</h2>
        {nativeError ? (
          <p className="mt-2 text-xs text-red-600">{nativeErrorMessage(nativeError)}</p>
        ) : nativeState ? (
          <dl className="mt-2 grid grid-cols-1 gap-3 text-xs sm:grid-cols-3">
            <div>
              <dt className="text-slate-500">连续采集</dt>
              <dd className="mt-1 font-medium text-slate-900">{formatNativeBoolean(nativeState.collectionMode)}</dd>
            </div>
            <div>
              <dt className="text-slate-500">触发原因</dt>
              <dd className="mt-1 font-medium text-slate-900">{formatNativeField(nativeState.triggerReason ?? null)}</dd>
            </div>
            <div>
              <dt className="text-slate-500">下次定位</dt>
              <dd className="mt-1 font-medium text-slate-900">{formatNativeField(nativeState.nextLocationAt ?? null)}</dd>
            </div>
          </dl>
        ) : (
          <p className="mt-2 text-xs text-slate-500">正在加载采集状态...</p>
        )}
      </section>

      {/* ── GeneratedAt Information ── */}
      {latestGenAt && (
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
            <span>服务端数据生成于 {formatDateTime(latestGenAt)}</span>
            {staleStatusLabel(isStale) && (
              <span className="rounded bg-amber-50 px-1.5 py-0.5 font-medium text-amber-700">{staleStatusLabel(isStale)}</span>
            )}
          </div>
        </section>
      )}

      {/* ── Loading ── */}
      {!settled && (
        <p className="text-sm text-slate-500">正在加载今日数据...</p>
      )}

      {/* ── Errors ── */}
      {fetchErrors.length > 0 && (
        <section className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700" role="alert">
          <p className="font-medium">部分数据获取失败</p>
          <ul className="mt-1 list-inside list-disc text-xs">
            {fetchErrors.map((msg, i) => <li key={i}>{msg}</li>)}
          </ul>
          <button
            type="button"
            onClick={() => {
              locationOverviewQ.refetch();
              tracksQ.refetch();
              usageOverviewQ.refetch();
              summaryQ.refetch();
            }}
            className="mt-2 rounded bg-red-100 px-3 py-1 text-xs font-medium text-red-700 hover:bg-red-200"
          >
            重试
          </button>
        </section>
      )}

      {/* ── Data ── */}
      {settled && (
        <>
          {locationOverviewQ.data && (
            <section>
              <h2 className="mb-2 text-sm font-semibold text-slate-950">位置概况</h2>
              <LocationMetricStrip overview={locationOverviewQ.data} />
            </section>
          )}

          {tracksQ.data && tracksQ.data.length > 0 && (
            <LocationHistoryMap tracks={tracksQ.data} />
          )}

          {usageOverviewQ.data && (
            <MobileInsightStrip overview={usageOverviewQ.data} />
          )}

          {summaryQ.data && summaryQ.data.appRanking.length > 0 && (
            <MobileAppRanking
              apps={summaryQ.data.appRanking}
              totalForegroundSeconds={summaryQ.data.totalForegroundSeconds}
            />
          )}

          {!anyData && !fetchErrors.length && (
            <section className="rounded-lg border border-dashed border-slate-200 p-8 text-center text-sm text-slate-500">
              暂无今日数据。
            </section>
          )}
        </>
      )}
    </div>
  );
}

import { useMemo, useRef, useState, useCallback, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import {
  getMobileDevices,
  getMobileLocationAnalyticsOverview,
  getMobileLocationAnalyticsSegmentPoints,
  getMobileLocationAnalyticsTracks,
  type MobileLocationAnalyticsParams,
} from '../api/mobile';
import HistoricalLocationDashboard from '../components/mobile/HistoricalLocationDashboard';
import {
  buildMobileAnalyticsDateRange,
  toMobileAnalyticsUtcRange,
} from '../components/mobile/mobileFormatting';
import {
  parseTracksUrlFilters,
  serializeTracksUrlFilters,
  tracksUrlFiltersToParams,
  canAdvanceRawPointPage,
  advanceRawPointCursorStack,
} from './historicalLocationQuery';

function errorMessage(error: unknown) {
  if (error) return '历史位置数据获取失败，请重试。';
  return null;
}

export default function HistoricalLocationPage({ embedded }: { embedded?: boolean }) {
  const [searchParams, setSearchParams] = useSearchParams();

  const urlFilters = useMemo(() => parseTracksUrlFilters(searchParams), [searchParams]);

  const [rangeShortcut, setRangeShortcut] = useState(urlFilters.range);
  const [rangeStartDate, setRangeStartDate] = useState(urlFilters.startDate);
  const [rangeEndDate, setRangeEndDate] = useState(urlFilters.endDate);
  const [selectedDeviceId, setSelectedDeviceId] = useState(urlFilters.deviceId);
  const [maxAccuracyMeters, setMaxAccuracyMeters] = useState(urlFilters.maxAccuracyMeters);
  const [includeRejected, setIncludeRejected] = useState(urlFilters.includeRejected);
  const [selectedSegmentId, setSelectedSegmentId] = useState<string | null>(null);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);

  const utcRange = useMemo(
    () => toMobileAnalyticsUtcRange({ startDate: rangeStartDate, endDate: rangeEndDate }),
    [rangeStartDate, rangeEndDate],
  );

  const locationQuery: MobileLocationAnalyticsParams = useMemo(
    () => ({
      ...utcRange,
      ...tracksUrlFiltersToParams({
        range: rangeShortcut,
        startDate: rangeStartDate,
        endDate: rangeEndDate,
        deviceId: selectedDeviceId,
        maxAccuracyMeters,
        includeRejected,
      }),
    }),
    [utcRange, rangeShortcut, rangeStartDate, rangeEndDate, selectedDeviceId, maxAccuracyMeters, includeRejected],
  );

  const devicesQuery = useQuery({
    queryKey: ['mobile-devices'],
    queryFn: getMobileDevices,
    staleTime: 60000,
  });

  const overviewQuery = useQuery({
    queryKey: ['mobile-location-analytics-overview', locationQuery],
    queryFn: () => getMobileLocationAnalyticsOverview(locationQuery),
    refetchInterval: 30000,
  });

  const tracksQuery = useQuery({
    queryKey: ['mobile-location-analytics-tracks', locationQuery],
    queryFn: () => getMobileLocationAnalyticsTracks(locationQuery),
    refetchInterval: 30000,
  });

  const tracks = useMemo(() => tracksQuery.data ?? [], [tracksQuery.data]);
  const segments = useMemo(() => tracks.flatMap(track => track.segments), [tracks]);
  const effectiveSelectedSegmentId = selectedSegmentId && segments.some(segment => segment.id === selectedSegmentId)
    ? selectedSegmentId
    : segments[0]?.id ?? null;

  const cursorStack = useRef<string[]>([]);
  const [pageIndex, setPageIndex] = useState(0);
  const pageSize = 200;
  const previousEffectiveSegmentId = useRef<string | null>(null);

  useEffect(() => {
    if (previousEffectiveSegmentId.current === effectiveSelectedSegmentId) return;
    previousEffectiveSegmentId.current = effectiveSelectedSegmentId;
    cursorStack.current = [];
    setPageIndex(0);
    setSelectedPointId(null);
  }, [effectiveSelectedSegmentId]);

  const pointsQuery = useQuery({
    queryKey: ['mobile-location-analytics-segment-points', effectiveSelectedSegmentId, locationQuery, pageIndex],
    queryFn: async () => {
      const cursor = pageIndex > 0 && pageIndex - 1 < cursorStack.current.length
        ? cursorStack.current[pageIndex - 1]
        : undefined;
      const result = await getMobileLocationAnalyticsSegmentPoints(effectiveSelectedSegmentId!, {
        ...locationQuery,
        cursor: cursor || undefined,
        pageSize,
      });
      return result;
    },
    enabled: Boolean(effectiveSelectedSegmentId),
    refetchInterval: 30000,
  });

  const points = useMemo(() => pointsQuery.data?.items ?? [], [pointsQuery.data]);
  const hasMore = pointsQuery.data?.hasMore ?? false;
  const currentNextCursor = pointsQuery.data?.nextCursor ?? null;

  const effectiveSelectedPointId = selectedPointId && points.some(point => point.id === selectedPointId)
    ? selectedPointId
    : points[0]?.id ?? null;

  const rawPointsCurrentPage = pageIndex + 1;
  const rawPointsHasPreviousPage = pageIndex > 0;
  const rawPointsHasNextPage = canAdvanceRawPointPage({ hasMore, nextCursor: currentNextCursor });

  const syncUrl = useCallback((rangeValue: string, startDate: string, endDate: string, device: string, accuracy: number, rejected: boolean) => {
    const filters = {
      range: rangeValue as '7d' | '30d' | 'today' | 'custom',
      startDate,
      endDate,
      deviceId: device,
      maxAccuracyMeters: accuracy,
      includeRejected: rejected,
    };
    const next = serializeTracksUrlFilters(filters, searchParams);
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);

  function resetPagination() {
    cursorStack.current = [];
    setPageIndex(0);
    setSelectedPointId(null);
  }

  function applyShortcut(shortcut: string) {
    const range = buildMobileAnalyticsDateRange(shortcut as '7d' | '30d' | 'today' | 'custom');
    setRangeShortcut(range.shortcut);
    setRangeStartDate(range.startDate);
    setRangeEndDate(range.endDate);
    setSelectedSegmentId(null);
    resetPagination();
    syncUrl(range.shortcut, range.startDate, range.endDate, selectedDeviceId, maxAccuracyMeters, includeRejected);
  }

  function applyCustomRange(range: { startDate: string; endDate: string }) {
    setRangeShortcut('custom');
    setRangeStartDate(range.startDate);
    setRangeEndDate(range.endDate);
    setSelectedSegmentId(null);
    resetPagination();
    syncUrl('custom', range.startDate, range.endDate, selectedDeviceId, maxAccuracyMeters, includeRejected);
  }

  function refresh() {
    void Promise.all([
      devicesQuery.refetch(),
      overviewQuery.refetch(),
      tracksQuery.refetch(),
      pointsQuery.refetch(),
    ]);
  }

  function updateMaxAccuracy(value: number) {
    const next = Math.max(1, Math.round(value));
    setMaxAccuracyMeters(next);
    setSelectedSegmentId(null);
    resetPagination();
    syncUrl(rangeShortcut, rangeStartDate, rangeEndDate, selectedDeviceId, next, includeRejected);
  }

  function updateDevice(value: string) {
    setSelectedDeviceId(value);
    setSelectedSegmentId(null);
    resetPagination();
    syncUrl(rangeShortcut, rangeStartDate, rangeEndDate, value, maxAccuracyMeters, includeRejected);
  }

  function updateIncludeRejected(value: boolean) {
    setIncludeRejected(value);
    setSelectedSegmentId(null);
    resetPagination();
    syncUrl(rangeShortcut, rangeStartDate, rangeEndDate, selectedDeviceId, maxAccuracyMeters, value);
  }

  function handleRawPointsPreviousPage() {
    if (pageIndex <= 0) return;
    setPageIndex(prev => prev - 1);
    setSelectedPointId(null);
  }

  function handleRawPointsNextPage() {
    const advanced = advanceRawPointCursorStack({
      cursorStack: cursorStack.current,
      pageIndex,
      hasMore,
      nextCursor: currentNextCursor,
    });
    if (!advanced.didAdvance) return;
    cursorStack.current = advanced.cursorStack;
    setPageIndex(advanced.nextPageIndex);
    setSelectedPointId(null);
  }

  function handleSegmentSelect(segmentId: string) {
    setSelectedSegmentId(segmentId);
    resetPagination();
  }

  return (
    <HistoricalLocationDashboard
      rangeShortcut={rangeShortcut}
      rangeStartDate={rangeStartDate}
      rangeEndDate={rangeEndDate}
      selectedDeviceId={selectedDeviceId}
      devices={devicesQuery.data ?? []}
      maxAccuracyMeters={maxAccuracyMeters}
      includeRejected={includeRejected}
      overview={overviewQuery.data}
      tracks={tracks}
      selectedSegmentId={effectiveSelectedSegmentId}
      selectedPointId={effectiveSelectedPointId}
      points={points}
      isLoading={devicesQuery.isLoading || overviewQuery.isLoading || tracksQuery.isLoading}
      isFetching={devicesQuery.isFetching || overviewQuery.isFetching || tracksQuery.isFetching || pointsQuery.isFetching}
      errorMessage={
        errorMessage(devicesQuery.error)
        ?? errorMessage(overviewQuery.error)
        ?? errorMessage(tracksQuery.error)
        ?? null
      }
      rawPointsLoading={pointsQuery.isFetching}
      rawPointsError={pointsQuery.error ? '原始点加载失败' : null}
      rawPointsCurrentPage={rawPointsCurrentPage}
      rawPointsHasNextPage={rawPointsHasNextPage}
      rawPointsHasPreviousPage={rawPointsHasPreviousPage}
      onShortcutChange={applyShortcut}
      onCustomRangeChange={applyCustomRange}
      onDeviceChange={updateDevice}
      onMaxAccuracyChange={updateMaxAccuracy}
      onIncludeRejectedChange={updateIncludeRejected}
      onRefresh={refresh}
      onSelectSegment={handleSegmentSelect}
      onSelectPoint={setSelectedPointId}
      onRawPointsPreviousPage={handleRawPointsPreviousPage}
      onRawPointsNextPage={handleRawPointsNextPage}
      onRawPointsRetry={() => pointsQuery.refetch()}
      embedded={embedded}
    />
  );
}

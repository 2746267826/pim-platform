import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
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
  type MobileRangeShortcut,
} from '../components/mobile/mobileFormatting';
export { formatAccuracyLabel } from '../components/mobile/locationFormatting';

function errorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message;
  return error ? '历史位置加载失败，请稍后刷新。' : null;
}

export default function HistoricalLocationPage() {
  const defaultRange = useMemo(() => buildMobileAnalyticsDateRange('7d'), []);
  const [rangeShortcut, setRangeShortcut] = useState<MobileRangeShortcut>('7d');
  const [rangeStartDate, setRangeStartDate] = useState(defaultRange.startDate);
  const [rangeEndDate, setRangeEndDate] = useState(defaultRange.endDate);
  const [selectedDeviceId, setSelectedDeviceId] = useState('');
  const [maxAccuracyMeters, setMaxAccuracyMeters] = useState(50);
  const [includeRejected, setIncludeRejected] = useState(false);
  const [selectedSegmentId, setSelectedSegmentId] = useState<string | null>(null);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);
  const deviceId = selectedDeviceId || undefined;

  const utcRange = useMemo(
    () => toMobileAnalyticsUtcRange({ startDate: rangeStartDate, endDate: rangeEndDate }),
    [rangeStartDate, rangeEndDate],
  );

  const locationQuery: MobileLocationAnalyticsParams = useMemo(
    () => ({
      ...utcRange,
      deviceId,
      maxAccuracyMeters,
      includeRejected,
    }),
    [utcRange, deviceId, maxAccuracyMeters, includeRejected],
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

  const pointsQuery = useQuery({
    queryKey: ['mobile-location-analytics-segment-points', selectedSegmentId, locationQuery],
    queryFn: () => getMobileLocationAnalyticsSegmentPoints(selectedSegmentId!, {
      ...locationQuery,
      pageSize: 100,
    }),
    enabled: Boolean(selectedSegmentId),
    refetchInterval: 30000,
  });

  const tracks = tracksQuery.data ?? [];
  const points = pointsQuery.data?.items ?? [];

  useEffect(() => {
    const segments = tracks.flatMap(track => track.segments);
    if (segments.length === 0) {
      setSelectedSegmentId(null);
      return;
    }

    if (!selectedSegmentId || !segments.some(segment => segment.id === selectedSegmentId)) {
      setSelectedSegmentId(segments[0].id);
    }
  }, [tracks, selectedSegmentId]);

  useEffect(() => {
    if (points.length === 0) {
      setSelectedPointId(null);
      return;
    }

    if (!selectedPointId || !points.some(point => point.id === selectedPointId)) {
      setSelectedPointId(points[0].id);
    }
  }, [points, selectedPointId]);

  function applyShortcut(shortcut: MobileRangeShortcut) {
    const nextRange = buildMobileAnalyticsDateRange(shortcut);
    setRangeShortcut(nextRange.shortcut);
    setRangeStartDate(nextRange.startDate);
    setRangeEndDate(nextRange.endDate);
    setSelectedSegmentId(null);
    setSelectedPointId(null);
  }

  function applyCustomRange(range: { startDate: string; endDate: string }) {
    setRangeShortcut('custom');
    setRangeStartDate(range.startDate);
    setRangeEndDate(range.endDate);
    setSelectedSegmentId(null);
    setSelectedPointId(null);
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
    setMaxAccuracyMeters(Math.max(1, Math.round(value)));
    setSelectedSegmentId(null);
    setSelectedPointId(null);
  }

  function updateDevice(value: string) {
    setSelectedDeviceId(value);
    setSelectedSegmentId(null);
    setSelectedPointId(null);
  }

  function updateIncludeRejected(value: boolean) {
    setIncludeRejected(value);
    setSelectedSegmentId(null);
    setSelectedPointId(null);
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
      selectedSegmentId={selectedSegmentId}
      selectedPointId={selectedPointId}
      points={points}
      isLoading={devicesQuery.isLoading || overviewQuery.isLoading || tracksQuery.isLoading}
      isFetching={devicesQuery.isFetching || overviewQuery.isFetching || tracksQuery.isFetching || pointsQuery.isFetching}
      errorMessage={errorMessage(devicesQuery.error) ?? errorMessage(overviewQuery.error) ?? errorMessage(tracksQuery.error) ?? errorMessage(pointsQuery.error)}
      onShortcutChange={applyShortcut}
      onCustomRangeChange={applyCustomRange}
      onDeviceChange={updateDevice}
      onMaxAccuracyChange={updateMaxAccuracy}
      onIncludeRejectedChange={updateIncludeRejected}
      onRefresh={refresh}
      onSelectSegment={setSelectedSegmentId}
      onSelectPoint={setSelectedPointId}
    />
  );
}

import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMobileDevices, getMobileLocationHistory } from '../api/mobile';
import HistoricalLocationDashboard from '../components/mobile/HistoricalLocationDashboard';
export { formatAccuracyLabel } from '../components/mobile/locationFormatting';

function toDateTimeInput(date: Date) {
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function startOfTodayInput() {
  const now = new Date();
  return toDateTimeInput(new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0));
}

function endOfTodayInput() {
  const now = new Date();
  return toDateTimeInput(new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 0));
}

function toApiDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toISOString();
}

function errorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message;
  return error ? '历史位置加载失败，请稍后刷新。' : null;
}

export default function HistoricalLocationPage() {
  const [start, setStart] = useState(startOfTodayInput);
  const [end, setEnd] = useState(endOfTodayInput);
  const [selectedDeviceId, setSelectedDeviceId] = useState('');
  const [maxAccuracyMeters, setMaxAccuracyMeters] = useState(50);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);
  const deviceId = selectedDeviceId || undefined;

  const devicesQuery = useQuery({
    queryKey: ['mobile-devices'],
    queryFn: getMobileDevices,
    staleTime: 60000,
  });

  const historyQuery = useQuery({
    queryKey: ['mobile-location-history', start, end, deviceId ?? 'all', maxAccuracyMeters],
    queryFn: () => getMobileLocationHistory({
      start: toApiDateTime(start),
      end: toApiDateTime(end),
      deviceId,
      maxAccuracyMeters,
    }),
    enabled: Boolean(start && end),
    refetchInterval: 30000,
  });

  const points = historyQuery.data?.points ?? [];

  useEffect(() => {
    if (points.length === 0) {
      setSelectedPointId(null);
      return;
    }

    if (!selectedPointId || !points.some(point => point.id === selectedPointId)) {
      setSelectedPointId(points[0].id);
    }
  }, [points, selectedPointId]);

  function refresh() {
    void Promise.all([
      devicesQuery.refetch(),
      historyQuery.refetch(),
    ]);
  }

  function updateMaxAccuracy(value: number) {
    setMaxAccuracyMeters(Math.max(1, Math.round(value)));
  }

  return (
    <HistoricalLocationDashboard
      start={start}
      end={end}
      selectedDeviceId={selectedDeviceId}
      devices={devicesQuery.data ?? []}
      maxAccuracyMeters={maxAccuracyMeters}
      points={points}
      selectedPointId={selectedPointId}
      isLoading={devicesQuery.isLoading || historyQuery.isLoading}
      isFetching={devicesQuery.isFetching || historyQuery.isFetching}
      errorMessage={errorMessage(devicesQuery.error) ?? errorMessage(historyQuery.error)}
      onStartChange={setStart}
      onEndChange={setEnd}
      onDeviceChange={setSelectedDeviceId}
      onMaxAccuracyChange={updateMaxAccuracy}
      onRefresh={refresh}
      onSelectPoint={setSelectedPointId}
    />
  );
}

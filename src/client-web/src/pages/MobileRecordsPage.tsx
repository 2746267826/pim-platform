import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  getMobileDevices,
  getMobileQuality,
  getMobileSummary,
  getMobileTimeline,
} from '../api/mobile';
import MobileRecordsDashboard from '../components/mobile/MobileRecordsDashboard';

function todayInputValue() {
  return new Date().toISOString().slice(0, 10);
}

function errorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message;
  return error ? '手机记录加载失败，请稍后刷新。' : null;
}

export default function MobileRecordsPage() {
  const [date, setDate] = useState(todayInputValue);
  const [selectedDeviceId, setSelectedDeviceId] = useState('');
  const deviceId = selectedDeviceId || undefined;

  const devicesQuery = useQuery({
    queryKey: ['mobile-devices'],
    queryFn: getMobileDevices,
    staleTime: 60000,
  });

  const summaryQuery = useQuery({
    queryKey: ['mobile-summary', date, deviceId ?? 'all'],
    queryFn: () => getMobileSummary(date, deviceId),
    enabled: Boolean(date),
    refetchInterval: 30000,
  });

  const timelineQuery = useQuery({
    queryKey: ['mobile-timeline', date, deviceId ?? 'all'],
    queryFn: () => getMobileTimeline(date, deviceId),
    enabled: Boolean(date),
    refetchInterval: 30000,
  });

  const qualityQuery = useQuery({
    queryKey: ['mobile-quality', date, deviceId ?? 'all'],
    queryFn: () => getMobileQuality(date, deviceId),
    enabled: Boolean(date),
    refetchInterval: 30000,
  });

  function refresh() {
    void Promise.all([
      devicesQuery.refetch(),
      summaryQuery.refetch(),
      timelineQuery.refetch(),
      qualityQuery.refetch(),
    ]);
  }

  return (
    <MobileRecordsDashboard
      date={date}
      selectedDeviceId={selectedDeviceId}
      devices={devicesQuery.data ?? []}
      summary={summaryQuery.data}
      timeline={timelineQuery.data}
      quality={qualityQuery.data}
      isLoading={devicesQuery.isLoading || summaryQuery.isLoading || timelineQuery.isLoading || qualityQuery.isLoading}
      isFetching={devicesQuery.isFetching || summaryQuery.isFetching || timelineQuery.isFetching || qualityQuery.isFetching}
      errorMessage={
        errorMessage(devicesQuery.error)
          ?? errorMessage(summaryQuery.error)
          ?? errorMessage(timelineQuery.error)
          ?? errorMessage(qualityQuery.error)
      }
      onDateChange={setDate}
      onDeviceChange={setSelectedDeviceId}
      onRefresh={refresh}
    />
  );
}

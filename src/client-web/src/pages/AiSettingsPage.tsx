import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  getAiRequestDetail,
  getAiRequests,
  getAiStatus,
  getAiUsageSummary,
} from '../api/ai';
import AiRequestDetailPanel from '../components/ai/AiRequestDetailPanel';
import AiRequestLogTable from '../components/ai/AiRequestLogTable';
import AiStatusPanel from '../components/ai/AiStatusPanel';
import AiUsageOverview from '../components/ai/AiUsageOverview';
import PageHeader from '../ui/PageHeader';

const requestFilters = { page: 1, pageSize: 50 };

function asError(error: unknown) {
  return error instanceof Error ? error : null;
}

export default function AiSettingsPage() {
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const statusQuery = useQuery({
    queryKey: ['ai-status'],
    queryFn: getAiStatus,
    refetchInterval: 60_000,
  });

  const usageQuery = useQuery({
    queryKey: ['ai-usage-summary'],
    queryFn: getAiUsageSummary,
    refetchInterval: 60_000,
  });

  const requestsQuery = useQuery({
    queryKey: ['ai-requests', requestFilters],
    queryFn: () => getAiRequests(requestFilters),
    refetchInterval: 30_000,
  });

  useEffect(() => {
    if (!selectedId && requestsQuery.data?.items.length) {
      setSelectedId(requestsQuery.data.items[0].id);
    }
  }, [requestsQuery.data, selectedId]);

  const detailQuery = useQuery({
    queryKey: ['ai-request-detail', selectedId],
    queryFn: () => getAiRequestDetail(selectedId as string),
    enabled: !!selectedId,
  });

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
      <PageHeader title="AI 设置" subtitle="LiteLLM 状态、用量、请求日志与详情" />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
        <AiStatusPanel
          status={statusQuery.data}
          isLoading={statusQuery.isLoading}
          error={asError(statusQuery.error)}
        />
        <AiUsageOverview
          summary={usageQuery.data}
          isLoading={usageQuery.isLoading || usageQuery.isFetching}
          error={asError(usageQuery.error)}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.85fr)]">
        <AiRequestLogTable
          data={requestsQuery.data}
          selectedId={selectedId}
          isLoading={requestsQuery.isLoading}
          error={asError(requestsQuery.error)}
          onSelect={setSelectedId}
        />
        <AiRequestDetailPanel
          detail={detailQuery.data}
          isLoading={detailQuery.isLoading}
          error={asError(detailQuery.error)}
        />
      </div>
    </div>
  );
}

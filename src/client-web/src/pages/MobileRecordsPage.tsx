import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createMobileAppCategoryRule,
  deleteMobileAppCatalogOverride,
  deleteMobileAppCategoryRule,
  getMobileAnalyticsCharts,
  getMobileAnalyticsHeatmap,
  getMobileAnalyticsOverview,
  getMobileAnalyticsTimelineBlocks,
  getMobileAppCatalogOverrides,
  getMobileAppCategoryRules,
  getMobileDevices,
  getMobileSessionEvents,
  getMobileTimelineBlockSessions,
  MOBILE_DEFAULT_TIMEZONE,
  saveMobileAppCatalogOverride,
  updateMobileAppCategoryRule,
  type MobileAnalyticsQuery,
  type MobileAppCatalogOverride,
  type MobileAppCategoryRule,
  type MobileAppCategoryRuleUpsertRequest,
  type MobileHeatmapBucket,
} from '../api/mobile';
import MobileAnalyticsHeader from '../components/mobile/MobileAnalyticsHeader';
import MobileAnomalyPanel from '../components/mobile/MobileAnomalyPanel';
import MobileAppCatalogManager from '../components/mobile/MobileAppCatalogManager';
import MobileChartsGrid from '../components/mobile/MobileChartsGrid';
import MobileInsightStrip from '../components/mobile/MobileInsightStrip';
import MobileTimelineBlocks from '../components/mobile/MobileTimelineBlocks';
import MobileUsageBucketDetail from '../components/mobile/MobileUsageBucketDetail';
import MobileUsageHeatmap, { type MobileHeatmapGranularity } from '../components/mobile/MobileUsageHeatmap';
import { buildHeatmapMatrix } from '../components/mobile/mobileHeatmapMatrix';
import {
  buildMobileAnalyticsDateRange,
  toMobileAnalyticsUtcRange,
  type MobileRangeShortcut,
} from '../components/mobile/mobileFormatting';
import { getDeferredAutoRefreshInterval } from '../lib/autoRefresh';

function errorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message;
  return error ? '手机记录加载失败，请稍后刷新。' : null;
}

function catalogMutationKeys() {
  return [
    ['mobile-app-catalog-overrides'],
    ['mobile-app-category-rules'],
    ['mobile-analytics-overview'],
    ['mobile-analytics-charts'],
    ['mobile-analytics-heatmap'],
    ['mobile-analytics-timeline-blocks'],
  ];
}

export default function MobileRecordsPage() {
  const queryClient = useQueryClient();
  const forceRef = useRef(false);
  const refreshSeq = useRef(0);
  const defaultRange = useMemo(() => buildMobileAnalyticsDateRange('7d'), []);
  const [rangeShortcut, setRangeShortcut] = useState<MobileRangeShortcut>('7d');
  const [rangeStartDate, setRangeStartDate] = useState(defaultRange.startDate);
  const [rangeEndDate, setRangeEndDate] = useState(defaultRange.endDate);
  const [selectedDeviceId, setSelectedDeviceId] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [packageName, setPackageName] = useState('');
  const [includeSystemNoise, setIncludeSystemNoise] = useState(false);
  const [granularity, setGranularity] = useState<MobileHeatmapGranularity>('hour');
  const [selectedBucketStartUtc, setSelectedBucketStartUtc] = useState<string | null>(null);
  const [expandedBlockId, setExpandedBlockId] = useState<string | null>(null);
  const [expandedSessionId, setExpandedSessionId] = useState<string | null>(null);
  const [timelinePage, setTimelinePage] = useState(1);
  const [timelinePageSize, setTimelinePageSize] = useState(20);

  const utcRange = useMemo(
    () => toMobileAnalyticsUtcRange({ startDate: rangeStartDate, endDate: rangeEndDate }),
    [rangeStartDate, rangeEndDate],
  );

  const analyticsQuery = useMemo<MobileAnalyticsQuery>(() => ({
    rangeStartUtc: utcRange.rangeStartUtc,
    rangeEndUtc: utcRange.rangeEndUtc,
    timezone: MOBILE_DEFAULT_TIMEZONE,
    deviceId: selectedDeviceId || null,
    category: selectedCategory || null,
    packageName: packageName.trim() || null,
    includeSystemNoise,
    minDurationSeconds: includeSystemNoise ? 0 : 1,
  }), [
    includeSystemNoise,
    packageName,
    selectedCategory,
    selectedDeviceId,
    utcRange.rangeEndUtc,
    utcRange.rangeStartUtc,
  ]);

  const devicesQuery = useQuery({
    queryKey: ['mobile-devices'],
    queryFn: getMobileDevices,
    staleTime: 60000,
  });

  const overviewQuery = useQuery({
    queryKey: ['mobile-analytics-overview', analyticsQuery],
    queryFn: () => getMobileAnalyticsOverview({ ...analyticsQuery, force: forceRef.current }),
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const heatmapQuery = useQuery({
    queryKey: ['mobile-analytics-heatmap', analyticsQuery, granularity],
    queryFn: () => getMobileAnalyticsHeatmap({ ...analyticsQuery, granularity, force: forceRef.current }),
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const chartsQuery = useQuery({
    queryKey: ['mobile-analytics-charts', analyticsQuery],
    queryFn: () => getMobileAnalyticsCharts({ ...analyticsQuery, force: forceRef.current }),
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const timelineBlocksQuery = useQuery({
    queryKey: ['mobile-analytics-timeline-blocks', analyticsQuery, timelinePage, timelinePageSize],
    queryFn: () => getMobileAnalyticsTimelineBlocks({
      ...analyticsQuery,
      page: timelinePage,
      pageSize: timelinePageSize,
      force: forceRef.current,
    }),
  });

  const overridesQuery = useQuery({
    queryKey: ['mobile-app-catalog-overrides'],
    queryFn: getMobileAppCatalogOverrides,
  });

  const rulesQuery = useQuery({
    queryKey: ['mobile-app-category-rules'],
    queryFn: getMobileAppCategoryRules,
  });

  const sessionsQuery = useQuery({
    queryKey: ['mobile-timeline-block-sessions', expandedBlockId, analyticsQuery],
    queryFn: () => getMobileTimelineBlockSessions(expandedBlockId ?? '', analyticsQuery),
    enabled: Boolean(expandedBlockId),
  });

  const eventsQuery = useQuery({
    queryKey: ['mobile-session-events', expandedSessionId],
    queryFn: () => getMobileSessionEvents(expandedSessionId ?? ''),
    enabled: Boolean(expandedSessionId),
  });

  function invalidateCatalogData() {
    for (const queryKey of catalogMutationKeys()) {
      void queryClient.invalidateQueries({ queryKey });
    }
  }

  const saveOverrideMutation = useMutation({
    mutationFn: (override: MobileAppCatalogOverride) => saveMobileAppCatalogOverride(override),
    onSuccess: invalidateCatalogData,
  });

  const deleteOverrideMutation = useMutation({
    mutationFn: (overridePackageName: string) => deleteMobileAppCatalogOverride(overridePackageName),
    onSuccess: invalidateCatalogData,
  });

  const saveRuleMutation = useMutation({
    mutationFn: (rule: MobileAppCategoryRule | MobileAppCategoryRuleUpsertRequest) => {
      const payload: MobileAppCategoryRuleUpsertRequest = {
        id: 'id' in rule ? rule.id : undefined,
        ruleType: rule.ruleType,
        pattern: rule.pattern,
        lifeCategory: rule.lifeCategory,
        priority: rule.priority,
        isEnabled: rule.isEnabled,
        displayNameOverride: rule.displayNameOverride ?? null,
        isSystemNoise: rule.isSystemNoise ?? null,
      };

      return payload.id
        ? updateMobileAppCategoryRule(payload.id, payload)
        : createMobileAppCategoryRule(payload);
    },
    onSuccess: invalidateCatalogData,
  });

  const deleteRuleMutation = useMutation({
    mutationFn: (ruleId: string) => deleteMobileAppCategoryRule(ruleId),
    onSuccess: invalidateCatalogData,
  });

  const timelineBlocks = timelineBlocksQuery.data?.items ?? [];
  const timelinePageData = timelineBlocksQuery.data;
  const heatmapMatrix = useMemo(
    () => buildHeatmapMatrix(heatmapQuery.data ?? []),
    [heatmapQuery.data],
  );
  const selectedHeatmapCell = useMemo(
    () => heatmapMatrix.days
      .flatMap(day => day.cells)
      .find(cell => cell.bucketStartUtc === selectedBucketStartUtc)
      ?? null,
    [heatmapMatrix, selectedBucketStartUtc],
  );
  const sessionsByBlock = expandedBlockId && sessionsQuery.data
    ? { [expandedBlockId]: sessionsQuery.data }
    : {};
  const eventsBySession = expandedSessionId && eventsQuery.data
    ? { [expandedSessionId]: eventsQuery.data }
    : {};

  useEffect(() => {
    setExpandedBlockId(null);
    setExpandedSessionId(null);
  }, [analyticsQuery, timelinePage, timelinePageSize]);

  function handleShortcutChange(shortcut: Exclude<MobileRangeShortcut, 'custom'>) {
    const nextRange = buildMobileAnalyticsDateRange(shortcut);
    setRangeShortcut(shortcut);
    setRangeStartDate(nextRange.startDate);
    setRangeEndDate(nextRange.endDate);
    setSelectedBucketStartUtc(null);
    setTimelinePage(1);
  }

  function handleCustomRangeChange(range: { startDate: string; endDate: string }) {
    setRangeShortcut('custom');
    setRangeStartDate(range.startDate);
    setRangeEndDate(range.endDate);
    setSelectedBucketStartUtc(null);
    setTimelinePage(1);
  }

  function handleHeatmapBucketSelect(bucket: MobileHeatmapBucket) {
    setSelectedBucketStartUtc(bucket.bucketStartUtc);
    setExpandedBlockId(null);
    setExpandedSessionId(null);
  }

  function handleChartCategorySelect(category: string) {
    setSelectedCategory(category);
    setPackageName('');
    setSelectedBucketStartUtc(null);
    setExpandedBlockId(null);
    setExpandedSessionId(null);
    setTimelinePage(1);
  }

  function handleChartAppSelect(packageNameValue: string) {
    setPackageName(packageNameValue);
    setSelectedCategory('');
    setSelectedBucketStartUtc(null);
    setExpandedBlockId(null);
    setExpandedSessionId(null);
    setTimelinePage(1);
  }

  function handleDeviceChange(deviceId: string) {
    setSelectedDeviceId(deviceId);
    setSelectedBucketStartUtc(null);
    setTimelinePage(1);
  }

  function handleHeaderCategoryChange(category: string) {
    setSelectedCategory(category);
    setSelectedBucketStartUtc(null);
    setTimelinePage(1);
  }

  function handlePackageNameChange(value: string) {
    setPackageName(value);
    setSelectedBucketStartUtc(null);
    setTimelinePage(1);
  }

  function handleIncludeSystemNoiseChange(value: boolean) {
    setIncludeSystemNoise(value);
    setSelectedBucketStartUtc(null);
    setTimelinePage(1);
  }

  function handleTimelinePageSizeChange(pageSize: number) {
    setTimelinePageSize(pageSize);
    setTimelinePage(1);
  }

  function refresh() {
    const seq = ++refreshSeq.current;
    forceRef.current = true;
    void Promise.all([
      devicesQuery.refetch(),
      overviewQuery.refetch(),
      heatmapQuery.refetch(),
      chartsQuery.refetch(),
      timelineBlocksQuery.refetch(),
      overridesQuery.refetch(),
      rulesQuery.refetch(),
    ]).finally(() => {
      if (refreshSeq.current === seq) {
        forceRef.current = false;
      }
    });
  }

  const loading = overviewQuery.isLoading
    || heatmapQuery.isLoading
    || chartsQuery.isLoading
    || timelineBlocksQuery.isLoading;
  const fetching = devicesQuery.isFetching
    || overviewQuery.isFetching
    || heatmapQuery.isFetching
    || chartsQuery.isFetching
    || timelineBlocksQuery.isFetching
    || overridesQuery.isFetching
    || rulesQuery.isFetching;
  const pageError = errorMessage(devicesQuery.error)
    ?? errorMessage(overviewQuery.error)
    ?? errorMessage(heatmapQuery.error)
    ?? errorMessage(chartsQuery.error)
    ?? errorMessage(timelineBlocksQuery.error)
    ?? errorMessage(overridesQuery.error)
    ?? errorMessage(rulesQuery.error);

  return (
    <div className="min-h-full bg-slate-50 pb-8">
      <MobileAnalyticsHeader
        rangeShortcut={rangeShortcut}
        rangeStartDate={rangeStartDate}
        rangeEndDate={rangeEndDate}
        selectedDeviceId={selectedDeviceId}
        devices={devicesQuery.data ?? []}
        selectedCategory={selectedCategory}
        packageName={packageName}
        includeSystemNoise={includeSystemNoise}
        isFetching={fetching}
        errorMessage={pageError}
        onShortcutChange={handleShortcutChange}
        onCustomRangeChange={handleCustomRangeChange}
        onDeviceChange={handleDeviceChange}
        onCategoryChange={handleHeaderCategoryChange}
        onPackageNameChange={handlePackageNameChange}
        onIncludeSystemNoiseChange={handleIncludeSystemNoiseChange}
        onRefresh={refresh}
      />

      <main className="space-y-4 pt-4">
        <MobileInsightStrip overview={overviewQuery.data} isLoading={loading} />
        <section className="mx-auto grid max-w-[1500px] grid-cols-1 gap-4 px-4 sm:px-6 xl:grid-cols-[minmax(0,1fr)_360px]">
          <MobileUsageHeatmap
            buckets={heatmapQuery.data ?? []}
            granularity={granularity}
            selectedBucketStartUtc={selectedBucketStartUtc}
            isLoading={heatmapQuery.isLoading}
            onGranularityChange={setGranularity}
            onBucketSelect={handleHeatmapBucketSelect}
          />
          <MobileUsageBucketDetail cell={selectedHeatmapCell} />
        </section>
        <section className="mx-auto max-w-[1500px] px-4 sm:px-6">
          <MobileChartsGrid
            charts={chartsQuery.data ?? []}
            isLoading={chartsQuery.isLoading}
            onCategorySelect={handleChartCategorySelect}
            onAppSelect={handleChartAppSelect}
          />
        </section>
        <section className="mx-auto max-w-[1500px] px-4 sm:px-6">
            <MobileTimelineBlocks
              blocks={timelineBlocks}
              sessionsByBlock={sessionsByBlock}
              eventsBySession={eventsBySession}
              expandedBlockId={expandedBlockId}
              expandedSessionId={expandedSessionId}
              page={timelinePageData?.page ?? timelinePage}
              pageSize={timelinePageData?.pageSize ?? timelinePageSize}
              totalCount={timelinePageData?.totalCount ?? 0}
              totalPages={timelinePageData?.totalPages ?? 0}
              isLoading={timelineBlocksQuery.isLoading}
              isLoadingSessions={sessionsQuery.isFetching}
              isLoadingEvents={eventsQuery.isFetching}
              onToggleBlock={blockId => {
                setExpandedBlockId(blockId);
                setExpandedSessionId(null);
              }}
              onToggleSession={setExpandedSessionId}
              onPageChange={setTimelinePage}
              onPageSizeChange={handleTimelinePageSizeChange}
            />
        </section>
        <div className="mx-auto grid max-w-[1500px] grid-cols-1 gap-4 px-4 sm:px-6 xl:grid-cols-2">
            <MobileAnomalyPanel
              anomalies={overviewQuery.data?.anomalies ?? []}
              suggestions={overviewQuery.data?.suggestions ?? []}
              quality={overviewQuery.data?.quality}
              isLoading={overviewQuery.isLoading}
            />
            <MobileAppCatalogManager
              overrides={overridesQuery.data ?? []}
              rules={rulesQuery.data ?? []}
              isLoading={overridesQuery.isLoading || rulesQuery.isLoading}
              isSaving={
                saveOverrideMutation.isPending
                || deleteOverrideMutation.isPending
                || saveRuleMutation.isPending
                || deleteRuleMutation.isPending
              }
              onSaveOverride={override => saveOverrideMutation.mutate(override)}
              onDeleteOverride={overridePackageName => deleteOverrideMutation.mutate(overridePackageName)}
              onSaveRule={rule => saveRuleMutation.mutate(rule)}
              onDeleteRule={ruleId => deleteRuleMutation.mutate(ruleId)}
            />
        </div>
      </main>
    </div>
  );
}

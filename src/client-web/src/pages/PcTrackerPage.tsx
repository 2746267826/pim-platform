import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { format, subDays, subMonths } from 'date-fns';
import {
  applyActivityClassificationSuggestion,
  getActivityClassificationSuggestions,
  getCategoryTree,
  getPcActivityAnalysis,
  getPcHeatmapGrid,
  getPcQuality,
  getPcSummary,
  previewActivityClassificationSuggestion,
  rejectActivityClassificationSuggestion,
} from '../api/pcTracker';
import DateDimensionBar from '../components/pc-tracker/DateDimensionBar';
import ActivityHeatmap from '../components/pc-tracker/ActivityHeatmap';
import ActivityAnalysisHeatmap from '../components/pc-tracker/ActivityAnalysisHeatmap';
import CategoryTimeline from '../components/pc-tracker/CategoryTimeline';
import DailyActivityPanel from '../components/pc-tracker/DailyActivityPanel';
import KeyboardHeatmap from '../components/pc-tracker/KeyboardHeatmap';
import PcQualitySummary from '../components/pc-tracker/PcQualitySummary';
import PcReviewSummary from '../components/pc-tracker/PcReviewSummary';
import ClassificationActionQueue from '../components/pc-tracker/ClassificationActionQueue';
import ProductivityDashboardPanel from '../components/pc-tracker/ProductivityDashboard';
import ClassificationPreviewDialog from '../components/pc-tracker/ClassificationPreviewDialog';
import EventTimelineDialog from '../components/pc-tracker/EventTimelineDialog';
import MetricCard from '../ui/MetricCard';
import PageHeader from '../ui/PageHeader';
import type {
  ActivityClassificationSuggestion,
  ActivityClassificationSuggestionPreview,
  SuggestionClassificationApplyRequest,
  SuggestionClassificationPreviewRequest,
} from '../types';
import { getPcBusinessDate } from '../utils/pcBusinessDay';

export function nextPcRoute3RequestId(current: number) {
  return current + 1;
}

export function isCurrentPcRoute3Request(requestId: number, current: number) {
  return requestId === current;
}

function AnalysisCard({
  title,
  subtitle,
  actions,
  children,
}: {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="pim-panel min-w-0 overflow-visible p-4">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-slate-950">{title}</h2>
          {subtitle && <p className="mt-1 text-xs text-slate-500">{subtitle}</p>}
        </div>
        {actions && <div className="shrink-0">{actions}</div>}
      </div>
      {children}
    </section>
  );
}

export default function PcTrackerPage() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(() => getPcBusinessDate());
  const [dimension, setDimension] = useState<'hour' | 'day' | 'month' | 'year'>('day');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);
  const [timelineDialogOpen, setTimelineDialogOpen] = useState(false);
  const [activeSuggestion, setActiveSuggestion] = useState<ActivityClassificationSuggestion | null>(null);
  const [preview, setPreview] = useState<ActivityClassificationSuggestionPreview | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [selectedAnalysisBlockStart, setSelectedAnalysisBlockStart] = useState<string | null>(null);
  const previewRequestIdRef = useRef(0);
  const applyRequestIdRef = useRef(0);

  const dateStr = format(selectedDate, 'yyyy-MM-dd');

  const { data } = useQuery({
    queryKey: ['pc-summary', dateStr],
    queryFn: () => getPcSummary(dateStr),
    refetchInterval: 30000,
  });

  const { data: quality, isLoading: qualityLoading, error: qualityError } = useQuery({
    queryKey: ['pc-quality', dateStr],
    queryFn: () => getPcQuality({ date: dateStr }),
    refetchInterval: 30000,
  });

  const { data: suggestions = [], isLoading: suggestionsLoading } = useQuery({
    queryKey: ['pc-classification-suggestions', dateStr],
    queryFn: () => getActivityClassificationSuggestions(dateStr),
    refetchInterval: 30000,
  });

  const { data: activityAnalysis } = useQuery({
    queryKey: ['pc-activity-analysis', dateStr, 60],
    queryFn: () => getPcActivityAnalysis(dateStr, 60),
    refetchInterval: 30000,
  });

  const { data: categoryTree = [] } = useQuery({
    queryKey: ['pc-category-tree'],
    queryFn: getCategoryTree,
    staleTime: 60000,
  });

  const heatmapRange = dimension === 'hour'
    ? { start: dateStr, end: dateStr }
    : dimension === 'day'
      ? { start: format(subDays(selectedDate, 30), 'yyyy-MM-dd'), end: dateStr }
      : dimension === 'month'
        ? { start: format(subMonths(selectedDate, 12), 'yyyy-MM-dd'), end: dateStr }
        : { start: format(subMonths(selectedDate, 60), 'yyyy-MM-dd'), end: dateStr };

  const { data: heatmapData, isLoading: heatmapLoading } = useQuery({
    queryKey: ['pc-heatmap-grid', heatmapRange.start, heatmapRange.end, dimension],
    queryFn: () => getPcHeatmapGrid(heatmapRange.start, heatmapRange.end, dimension),
  });

  const previewMutation = useMutation({
    mutationFn: ({
      id,
      request,
      requestId,
    }: {
      id: string;
      request: SuggestionClassificationPreviewRequest;
      requestId: number;
    }) => previewActivityClassificationSuggestion(id, request).then(result => ({ result, requestId })),
    onSuccess: ({ result, requestId }) => {
      if (isCurrentPcRoute3Request(requestId, previewRequestIdRef.current)) {
        setPreview(result);
        setPreviewError(null);
      }
    },
    onError: (error, variables) => {
      if (isCurrentPcRoute3Request(variables.requestId, previewRequestIdRef.current)) {
        setPreviewError(error instanceof Error ? error.message : '预览失败');
      }
    },
  });

  const applyMutation = useMutation({
    mutationFn: ({
      id,
      request,
      requestId,
    }: {
      id: string;
      request: SuggestionClassificationApplyRequest;
      requestId: number;
    }) => applyActivityClassificationSuggestion(id, request).then(result => ({ result, requestId })),
    onSuccess: ({ requestId }) => {
      if (!isCurrentPcRoute3Request(requestId, applyRequestIdRef.current)) return;

      queryClient.invalidateQueries({ queryKey: ['pc-summary'] });
      queryClient.invalidateQueries({ queryKey: ['pc-activity-analysis'] });
      queryClient.invalidateQueries({ queryKey: ['pc-classification-suggestions'] });
      queryClient.invalidateQueries({ queryKey: ['pc-recent-project-tags'] });
      queryClient.invalidateQueries({ queryKey: ['productivity-dashboard'] });
      setActiveSuggestion(null);
      setPreview(null);
      setPreviewError(null);
    },
    onError: (error, variables) => {
      if (isCurrentPcRoute3Request(variables.requestId, applyRequestIdRef.current)) {
        setPreviewError(error instanceof Error ? error.message : '应用失败');
      }
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (id: string) => rejectActivityClassificationSuggestion(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pc-classification-suggestions'] });
    },
  });

  function handleCorrectSuggestion(suggestion: ActivityClassificationSuggestion) {
    setActiveSuggestion(suggestion);
    handleDraftChange();
  }

  function handleCloseDialog() {
    setActiveSuggestion(null);
    handleDraftChange();
    applyRequestIdRef.current = nextPcRoute3RequestId(applyRequestIdRef.current);
    setPreviewError(null);
    previewMutation.reset();
    applyMutation.reset();
  }

  function handleDraftChange() {
    previewRequestIdRef.current = nextPcRoute3RequestId(previewRequestIdRef.current);
    setPreview(null);
    setPreviewError(null);
    previewMutation.reset();
  }

  function handlePreview(request: SuggestionClassificationPreviewRequest) {
    if (!activeSuggestion) return;
    const requestId = nextPcRoute3RequestId(previewRequestIdRef.current);
    previewRequestIdRef.current = requestId;
    setPreview(null);
    setPreviewError(null);
    previewMutation.mutate({ id: activeSuggestion.id, request, requestId });
  }

  function handleApply(request: SuggestionClassificationApplyRequest) {
    if (!activeSuggestion) return;
    const requestId = nextPcRoute3RequestId(applyRequestIdRef.current);
    applyRequestIdRef.current = requestId;
    setPreviewError(null);
    applyMutation.mutate({ id: activeSuggestion.id, request, requestId });
  }

  const metrics = [
    ['记录时长', data?.metrics?.totalRecordedDuration ?? '-'],
    ['输入时长', data?.metrics?.activeInputDuration ?? '-'],
    ['空闲时长', data?.metrics?.idleDuration ?? '-'],
    ['输入总量', ((data?.metrics?.totalKeyPresses ?? 0) + (data?.metrics?.totalClicks ?? 0)).toLocaleString('zh-CN')],
    ['应用数', data?.metrics?.activeAppCount ?? '-'],
    ['切换频率', data?.metrics ? data.metrics.switchFrequency.toFixed(1) : '-'],
  ] as const;

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
      <PageHeader
        title="PC 记录"
        subtitle="从全局概览进入应用、分类与输入行为 drilldown"
        actions={
          <div className="min-w-0 max-w-full">
            <DateDimensionBar
              date={selectedDate}
              dimension={dimension}
              onDateChange={setSelectedDate}
              onDimensionChange={setDimension}
            />
          </div>
        }
      />

      <PcQualitySummary quality={quality} isLoading={qualityLoading} error={qualityError} />

      <PcReviewSummary summary={data} pendingSuggestions={suggestions} />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(360px,1fr)]">
        <ProductivityDashboardPanel />
      </div>

      <AnalysisCard title="分类建议" subtitle="处理高置信度聚类，快速写入纠错规则">
        <ClassificationActionQueue
          suggestions={suggestions}
          isLoading={suggestionsLoading}
          onPreview={handleCorrectSuggestion}
          onReject={suggestion => rejectMutation.mutate(suggestion.id)}
        />
      </AnalysisCard>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        {metrics.map(([label, value], index) => (
          <MetricCard
            key={label}
            label={label}
            value={value}
            tone={index === 3 ? 'activity' : index === 5 ? 'primary' : 'neutral'}
          />
        ))}
      </div>

      <AnalysisCard title="活动分析" subtitle="按时间块查看活动强度、切换频率和待分类缺口">
        <ActivityAnalysisHeatmap
          analysis={activityAnalysis}
          selectedStart={selectedAnalysisBlockStart}
          onSelectBlock={block => setSelectedAnalysisBlockStart(block.start)}
        />
      </AnalysisCard>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.7fr)_minmax(360px,0.9fr)]">
        <AnalysisCard title="活动热力图" subtitle="按所选时间维度汇总输入强度">
          <ActivityHeatmap data={heatmapData} isLoading={heatmapLoading} />
        </AnalysisCard>

        <AnalysisCard title="当日活动排行" subtitle="分类和应用支持点击筛选">
          <DailyActivityPanel
            metrics={data?.metrics || null}
            categories={data?.categories || []}
            appRanking={data?.appRanking || []}
            selectedCategory={selectedCategory}
            onSelectCategory={setSelectedCategory}
            selectedApp={selectedApp}
            onSelectApp={setSelectedApp}
          />
        </AnalysisCard>
      </div>

      <div className="space-y-4">
        <AnalysisCard
          title="分类时间线"
          subtitle="按小时分组的甘特图时间线，悬停查看详情"
          actions={
            <button
              type="button"
              onClick={() => setTimelineDialogOpen(true)}
              className="pim-button-primary h-8 px-3 text-xs font-medium"
            >
              查看详情
            </button>
          }
        >
          <CategoryTimeline
            timeline={data?.timeline || []}
          />
        </AnalysisCard>

        <AnalysisCard title="键盘鼠标热力图" subtitle="108 键键盘、鼠标按键与快捷键统计">
          <KeyboardHeatmap keystats={data?.keystats || null} />
        </AnalysisCard>
      </div>

      <ClassificationPreviewDialog
        suggestion={activeSuggestion}
        date={dateStr}
        preview={preview}
        isPreviewing={previewMutation.isPending}
        isApplying={applyMutation.isPending}
        errorMessage={previewError}
        categories={categoryTree}
        onClose={handleCloseDialog}
        onPreview={handlePreview}
        onApply={handleApply}
      />

      <EventTimelineDialog
        open={timelineDialogOpen}
        timeline={data?.timeline || []}
        dateStr={dateStr}
        onClose={() => setTimelineDialogOpen(false)}
      />
    </div>
  );
}

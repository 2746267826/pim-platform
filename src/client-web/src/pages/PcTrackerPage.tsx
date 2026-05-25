import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format, subDays, subMonths } from 'date-fns';
import { getPcSummary, getPcHeatmapGrid, getPcQuality } from '../api/pcTracker';
import DateDimensionBar from '../components/pc-tracker/DateDimensionBar';
import ActivityHeatmap from '../components/pc-tracker/ActivityHeatmap';
import CategoryTimeline from '../components/pc-tracker/CategoryTimeline';
import DailyActivityPanel from '../components/pc-tracker/DailyActivityPanel';
import KeyboardHeatmap from '../components/pc-tracker/KeyboardHeatmap';
import PcQualitySummary from '../components/pc-tracker/PcQualitySummary';
import MetricCard from '../ui/MetricCard';
import PageHeader from '../ui/PageHeader';
import { getPcBusinessDate } from '../utils/pcBusinessDay';

function AnalysisCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="pim-panel min-w-0 overflow-visible p-4">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-slate-950">{title}</h2>
          {subtitle && <p className="mt-1 text-xs text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {children}
    </section>
  );
}

export default function PcTrackerPage() {
  const [selectedDate, setSelectedDate] = useState(() => getPcBusinessDate());
  const [dimension, setDimension] = useState<'hour' | 'day' | 'month' | 'year'>('day');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);

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
        <AnalysisCard title="分类时间线" subtitle="按 ActivityWatch 时间片聚合分类">
          <CategoryTimeline
            timeline={data?.timeline || []}
          />
        </AnalysisCard>

        <AnalysisCard title="键盘鼠标热力图" subtitle="108 键键盘、鼠标按键与快捷键统计">
          <KeyboardHeatmap keystats={data?.keystats || null} />
        </AnalysisCard>
      </div>
    </div>
  );
}

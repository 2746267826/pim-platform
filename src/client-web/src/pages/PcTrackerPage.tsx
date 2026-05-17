// src/client-web/src/pages/PcTrackerPage.tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format, subDays, subMonths } from 'date-fns';
import { getPcSummary, getPcHeatmapGrid, getPcCategories } from '../api/pcTracker';
import DateDimensionBar from '../components/pc-tracker/DateDimensionBar';
import ActivityHeatmap from '../components/pc-tracker/ActivityHeatmap';
import CategoryTimeline from '../components/pc-tracker/CategoryTimeline';
import DailyActivityPanel from '../components/pc-tracker/DailyActivityPanel';
import KeyboardHeatmap from '../components/pc-tracker/KeyboardHeatmap';

function PanelCard({ title, subtitle, icon, children }: { title: string; subtitle: string; icon: string; children: React.ReactNode }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border p-5">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-lg">{icon}</span>
        <span className="font-semibold text-gray-800">{title}</span>
        <span className="text-xs text-gray-400 ml-2">{subtitle}</span>
      </div>
      {children}
    </div>
  );
}

export default function PcTrackerPage() {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [dimension, setDimension] = useState<'hour' | 'day' | 'month' | 'year'>('day');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);

  const dateStr = format(selectedDate, 'yyyy-MM-dd');

  // Summary query
  const { data, isLoading } = useQuery({
    queryKey: ['pc-summary', dateStr],
    queryFn: () => getPcSummary(dateStr),
    refetchInterval: 30000,
  });

  // Heatmap grid query
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

  // Category rules query (for timeline classification)
  const { data: catRulesData } = useQuery({
    queryKey: ['pc-categories'],
    queryFn: () => getPcCategories(),
    staleTime: 300000,
  });

  return (
    <div className="max-w-[960px] mx-auto space-y-4 pb-8">
      {/* Module 1: Date + Dimension */}
      <DateDimensionBar date={selectedDate} dimension={dimension}
        onDateChange={setSelectedDate} onDimensionChange={setDimension} />

      {/* Module 2: Heatmap */}
      <PanelCard title="活动热力图" subtitle="按键频率分布（线性绿阶）" icon="📊">
        <ActivityHeatmap data={heatmapData} isLoading={heatmapLoading} />
      </PanelCard>

      {/* Timeline: Category Aggregation */}
      <PanelCard title="时间线" subtitle="按活动分类聚合（悬浮查看详情）" icon="⏱">
        <CategoryTimeline timeline={data?.timeline || []} categories={data?.categories || []}
          rules={catRulesData ?? undefined} />
      </PanelCard>

      {/* Module 3: Daily Activity */}
      <PanelCard title="当日活动分析" subtitle="综合衍生指标" icon="📈">
        <DailyActivityPanel metrics={data?.metrics || null} categories={data?.categories || []}
          appRanking={data?.appRanking || []} selectedCategory={selectedCategory}
          onSelectCategory={setSelectedCategory} selectedApp={selectedApp}
          onSelectApp={setSelectedApp} />
      </PanelCard>

      {/* Module 4: Keyboard Heatmap */}
      <PanelCard title="键盘鼠标热力图" subtitle="标准 ANSI 布局 + 快捷键统计" icon="⌨">
        <KeyboardHeatmap keystats={data?.keystats || null} />
      </PanelCard>
    </div>
  );
}

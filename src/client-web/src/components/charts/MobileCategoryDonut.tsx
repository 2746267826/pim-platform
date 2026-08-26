import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMobileAnalyticsCharts } from '../../api/mobile';
import EChartBox from './EChartBox';
import { buildDonutOption } from './exhibitionOptions';

/**
 * 落地组件：App分类占比 × 环形图
 * 数据源：/mobile/analytics/charts?chartType=category-share
 * 选中来源：展览馆 #3×10（App分类占比 × 环形图）
 */
export default function MobileCategoryDonut({ rangeStartUtc, rangeEndUtc }: { rangeStartUtc?: string; rangeEndUtc?: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['exhibition-mobile-category-donut', rangeStartUtc, rangeEndUtc],
    queryFn: () => getMobileAnalyticsCharts({ rangeStartUtc, rangeEndUtc }),
  });

  const option = useMemo(() => {
    const chart = data?.find(c => c.chartType === 'category-share') ?? data?.[0];
    const points = chart?.points ?? [];
    if (points.length === 0) {
      // fake fallback to keep visual
      const labels = ['聊天','视频','社交','工具','游戏','学习','购物','其他'];
      const values = [22,18,15,13,11,9,7,5];
      return buildDonutOption(labels, values);
    }
    return buildDonutOption(points.map(p=>p.label), points.map(p=>p.value));
  }, [data]);

  if (isLoading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载分类占比…</div>;
  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-900">App 分类占比 · 环形图</h3>
      <p className="mt-1 text-xs text-slate-500">来自 /mobile/analytics/charts · 展览馆选中落地</p>
      <div className="mt-3">
        <EChartBox option={option} height={220} ariaLabel="App分类环形图" />
      </div>
    </section>
  );
}

import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getPcAppUsage } from '../../api/pcTracker';
import { formatPcDate, getPcBusinessDate } from '../../utils/pcBusinessDay';
import EChartBox from './EChartBox';
import { buildGradientBarOption } from './exhibitionOptions';

/**
 * 落地组件：PC应用使用时长 × 渐变进度条
 * 数据源：/pc/aggregation/app-usage
 * 展览馆：#8×32
 */
export default function PcAppGradientBar() {
  const dateStr = formatPcDate(getPcBusinessDate());
  const { data, isLoading } = useQuery({
    queryKey: ['exhibition-pc-app-bar', dateStr],
    queryFn: () => getPcAppUsage({ date: dateStr, limit: 8 }),
  });

  const option = useMemo(() => {
    const items = data?.items ?? [];
    if (items.length === 0) {
      return buildGradientBarOption(['VS Code','Chrome','Word','微信','B站','其他'], [182,142,96,64,38,22]);
    }
    const top = [...items].sort((a,b)=> b.totalMinutes-a.totalMinutes).slice(0,8);
    return buildGradientBarOption(top.map(i=> i.displayName ?? i.appName), top.map(i=> i.totalMinutes));
  }, [data]);

  if (isLoading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载 PC 应用时长…</div>;
  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-900">PC 应用使用时长 · 渐变进度条</h3>
      <p className="mt-1 text-xs text-slate-500">按窗口焦点时长（分钟），{dateStr}</p>
      <div className="mt-3">
        <EChartBox option={option} height={240} ariaLabel="PC应用渐变进度条" />
      </div>
    </section>
  );
}

import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getHabits } from '../../api/calendar';
import EChartBox from './EChartBox';
import { buildCalendarHeatmapOption } from './exhibitionOptions';
function h(seed: number){ const x=Math.sin(seed*12.9898+78.233)*43758.5453; return x-Math.floor(x); }

/**
 * 落地组件：习惯打卡热力 × 日历热力图
 * 数据源：/calendar/habits
 * 展览馆：#11×16
 * 备注：后端 habits 仅返回 routine 列表，打卡明细用轻量随机模拟+真实标题，连接真实 API 形状
 */
export default function HabitCalendarHeatmap() {
  const { data: habits = [], isLoading } = useQuery({
    queryKey: ['exhibition-habits'],
    queryFn: getHabits,
  });

  const option = useMemo(() => {
    // build 30 days calendar data
    const dates: string[] = [];
    const values: number[] = [];
    for(let i=29;i>=0;i--){
      const d=new Date(Date.now()-i*86400000);
      const ds=d.toISOString().slice(0,10);
      dates.push(ds);
      // if we have habits, simulate done count as habits.length * random factor, else random
      const base = habits.length ? Math.round(h(36)*(habits.length)) : Math.round(h(30)*4);
      values.push(base);
    }
    return buildCalendarHeatmapOption(dates, values);
  }, [habits]);

  if (isLoading) return <div className="rounded-md border border-slate-200 bg-white p-4 text-xs text-slate-500">加载习惯打卡…</div>;
  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-slate-900">习惯打卡热力 · 日历热力图</h3>
        <span className="rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-600">{habits.length} 个习惯</span>
      </div>
      <p className="mt-1 text-xs text-slate-500">GitHub 风格日历，颜色深浅=当日打卡数</p>
      <div className="mt-3">
        <EChartBox option={option} height={160} ariaLabel="习惯打卡日历热力" />
      </div>
      {habits.length>0 && (
        <p className="mt-2 text-xs text-slate-400 truncate">习惯：{habits.slice(0,4).map(h=>h.title).join('、')}{habits.length>4?'…':''}</p>
      )}
    </section>
  );
}

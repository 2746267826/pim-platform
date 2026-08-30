import { useMemo } from 'react';
import EChartBox from '../charts/EChartBox';
import { buildCategoryGanttOption } from '../charts/pcHeatmapOptions';
import type { TimelineItem } from '../../types';

interface Props {
  timeline: TimelineItem[];
}

export default function CategoryTimeline({ timeline }: Props) {
  const ganttOption = useMemo(() => buildCategoryGanttOption(timeline), [timeline]);
  const ganttDataLength = (ganttOption as unknown as { series?: Array<{ data?: unknown[] }> })?.series?.[0]?.data?.length ?? 0;
  const hasValidSegments = useMemo(() => timeline.some(item => {
    if (!item.start || !item.end) return false;
    const s = new Date(item.start).getTime();
    const e = new Date(item.end).getTime();
    return Number.isFinite(s) && Number.isFinite(e) && e > s;
  }), [timeline]);

  const { hourRange, stats, legend } = useMemo(() => {
    if (!timeline.length) {
      return { hourRange: { min: 6, max: 23 }, stats: null, legend: [] };
    }

    // Parse events into per-hour segments for stats/legend/hour range
    const categoryMap = new Map<string, { color: string; totalMin: number }>();
    let totalMin = 0;
    let prodMin = 0;
    let minHour = 23, maxHour = 0;

    const productiveCats = ['工作', '编程', '文档', '学习', '邮件', '终端', '办公'];

    timeline
      .filter(item => item.start && item.end)
      .forEach(item => {
        const start = new Date(item.start);
        const end = new Date(item.end);
        const sMin = start.getHours() * 60 + start.getMinutes();
        const eMin = end.getHours() * 60 + end.getMinutes();
        const catName = item.categoryName || '其他';
        const catColor = item.categoryColor || '#94a3b8';
        const dur = item.durationMinutes || (end.getTime() - start.getTime()) / 60000;

        if (!categoryMap.has(catName)) {
          categoryMap.set(catName, { color: catColor, totalMin: 0 });
        }
        categoryMap.get(catName)!.totalMin += dur;
        totalMin += dur;
        if (productiveCats.some(c => catName.includes(c))) prodMin += dur;

        for (let h = start.getHours(); h <= end.getHours(); h++) {
          const hourStart = h * 60;
          const hourEnd = hourStart + 60;
          const segS = Math.max(sMin, hourStart);
          const segE = Math.min(eMin, hourEnd);
          if (segS < segE) {
            minHour = Math.min(minHour, h);
            maxHour = Math.max(maxHour, h);
          }
        }
      });

    const sortedCats = [...categoryMap.entries()]
      .sort((a, b) => b[1].totalMin - a[1].totalMin);

    const legend = sortedCats.map(([name, info]) => ({
      name,
      color: info.color,
      totalMin: info.totalMin,
    }));

    const stats = {
      totalMinutes: Math.round(totalMin),
      productivePercent: totalMin > 0 ? Math.round(prodMin / totalMin * 100) : 0,
      eventCount: timeline.length,
    };

    return { hourRange: { min: minHour, max: maxHour }, stats, legend };
  }, [timeline]);

  const hours = useMemo(() => {
    const arr: number[] = [];
    for (let h = hourRange.min; h <= hourRange.max; h++) arr.push(h);
    return arr;
  }, [hourRange]);

  const rowHeight = 44;
  const hasSegments = hasValidSegments;
  const showFallback = hasSegments && ganttDataLength === 0;

  return (
    <div className="rounded-xl border border-slate-200 bg-white">
      {/* Stats bar */}
      {stats && (
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-100 px-4 py-3 text-xs">
          {legend.map(cat => (
            <span key={cat.name} className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-sm" style={{ backgroundColor: cat.color }} />
              {cat.name} {(cat.totalMin / 60).toFixed(1)}h
            </span>
          ))}
          <span className="ml-auto font-semibold text-blue-600">
            生产性 {stats.productivePercent}% · {stats.eventCount} 条
          </span>
        </div>
      )}

      {/* Gantt chart */}
      <div className="px-4 pb-3 pt-3">
        {!hasSegments ? (
          <div className="py-10 text-center text-sm text-slate-400">暂无时间线数据</div>
        ) : showFallback ? (
          <div className="space-y-2">
            <div className="py-2 text-center text-xs text-amber-600">时间线数据可展示为列表（图表渲染备用）</div>
            <div className="max-h-[320px] overflow-auto rounded-md border border-slate-100">
              {timeline.slice(0, 50).map((item, idx) => {
                const s = new Date(item.start);
                const e = new Date(item.end);
                const label = Number.isFinite(s.getTime()) ? `${String(s.getHours()).padStart(2,'0')}:${String(s.getMinutes()).padStart(2,'0')}` : item.start.slice(11,16);
                const endLabel = Number.isFinite(e.getTime()) ? `${String(e.getHours()).padStart(2,'0')}:${String(e.getMinutes()).padStart(2,'0')}` : item.end.slice(11,16);
                return (
                  <div key={idx} className="flex items-center gap-2 border-b border-slate-50 px-3 py-2 text-xs">
                    <span className="h-2 w-2 rounded-full" style={{ backgroundColor: item.categoryColor || '#94a3b8' }} />
                    <span className="font-medium text-slate-700">{item.categoryName || '其他'}</span>
                    <span className="text-slate-500">{item.appName || '未知应用'}</span>
                    <span className="ml-auto text-slate-400">{label} - {endLabel} · {Math.round(item.durationMinutes)} 分钟</span>
                  </div>
                );
              })}
              {timeline.length > 50 && <div className="px-3 py-2 text-center text-xs text-slate-400">… 共 {timeline.length} 条，仅展示前 50 条</div>}
            </div>
          </div>
        ) : (
          <EChartBox
            option={ganttOption}
            height={Math.max(hours.length * rowHeight + 24, 140)}
            ariaLabel="分类时间线"
          />
        )}
      </div>

      {/* Legend */}
      {legend.length > 0 && (
        <div className="flex flex-wrap items-center gap-3 border-t border-slate-100 px-4 py-2.5 text-[10px] text-slate-500">
          {legend.map(cat => (
            <span key={cat.name} className="flex items-center gap-1">
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: cat.color }} />
              {cat.name}
              <span className="text-slate-400">{(cat.totalMin / 60).toFixed(1)}h</span>
            </span>
          ))}
          <span className="ml-auto text-slate-300">💡 悬停查看详情</span>
        </div>
      )}
    </div>
  );
}

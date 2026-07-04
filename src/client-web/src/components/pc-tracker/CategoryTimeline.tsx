import { useMemo, useState } from 'react';
import type { TimelineItem } from '../../types';

interface GanttSegment {
  startH: number;
  startM: number;
  endH: number;
  endM: number;
  appName: string;
  categoryName: string;
  categoryColor: string;
  leftPct: number;
  widthPct: number;
  icon: string;
}

interface ActiveTooltip {
  appName: string;
  timeRange: string;
  categoryName: string;
  categoryColor: string;
  x: number;
  y: number;
}

const CATEGORY_ICONS: Record<string, string> = {
  编程: '💻', 工作: '🖥️', 社交: '💬', 学习: '📄',
  视频: '🎮', 娱乐: '🎮', 文档: '📄', 游戏: '🎮',
  会议: '📞', 终端: '🖥️', 沟通: '💬', 浏览: '🌐',
  办公: '📋', 文件: '📁',
};

function getIcon(category: string): string {
  for (const [key, icon] of Object.entries(CATEGORY_ICONS)) {
    if (category.includes(key)) return icon;
  }
  return '⏳';
}

interface Props {
  timeline: TimelineItem[];
}

export default function CategoryTimeline({ timeline }: Props) {
  const [tooltip, setTooltip] = useState<ActiveTooltip | null>(null);

  const { segments, hourRange, stats, legend } = useMemo(() => {
    if (!timeline.length) {
      return { segments: [], hourRange: { min: 6, max: 23 }, stats: null, legend: [] };
    }

    // Parse events and split into hourly segments
    const allSegments: GanttSegment[] = [];
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

        // Update per-category totals
        if (!categoryMap.has(catName)) {
          categoryMap.set(catName, { color: catColor, totalMin: 0 });
        }
        categoryMap.get(catName)!.totalMin += dur;
        totalMin += dur;
        if (productiveCats.some(c => catName.includes(c))) prodMin += dur;

        // Split into hourly segments
        for (let h = start.getHours(); h <= end.getHours(); h++) {
          const hourStart = h * 60;
          const hourEnd = hourStart + 60;
          const segS = Math.max(sMin, hourStart);
          const segE = Math.min(eMin, hourEnd);
          if (segS < segE) {
            allSegments.push({
              startH: start.getHours(),
              startM: start.getMinutes(),
              endH: end.getHours(),
              endM: end.getMinutes(),
              appName: item.appName || '',
              categoryName: catName,
              categoryColor: catColor,
              leftPct: Math.max(((segS - hourStart) / 60) * 100, 0),
              widthPct: Math.max(((segE - segS) / 60) * 100, 0.3),
              icon: getIcon(catName),
            });
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

    return { segments: allSegments, hourRange: { min: minHour, max: maxHour }, stats, legend };
  }, [timeline]);

  const hours = useMemo(() => {
    const arr: number[] = [];
    for (let h = hourRange.min; h <= hourRange.max; h++) arr.push(h);
    return arr;
  }, [hourRange]);

  // Group segments by hour
  const segmentsByHour = useMemo(() => {
    const map = new Map<number, GanttSegment[]>();
    hours.forEach(h => map.set(h, []));
    segments.forEach(seg => {
      // Determine which hours this segment belongs to
      for (let h = hourRange.min; h <= hourRange.max; h++) {
        if (seg.startH <= h && seg.endH >= h) {
          // Check overlap
          const segStartMin = seg.startH * 60 + seg.startM;
          const segEndMin = seg.endH * 60 + seg.endM;
          const hrStart = h * 60;
          const hrEnd = hrStart + 60;
          if (segEndMin > hrStart && segStartMin < hrEnd) {
            const overlapS = Math.max(segStartMin, hrStart);
            const overlapE = Math.min(segEndMin, hrEnd);
            const leftPct = Math.max(((overlapS - hrStart) / 60) * 100, 0);
            const widthPct = Math.max(((overlapE - overlapS) / 60) * 100, 0.3);
            map.get(h)!.push({ ...seg, leftPct, widthPct });
          }
        }
      }
    });
    return map;
  }, [segments, hours, hourRange]);

  const rowHeight = 44;

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
        {segments.length === 0 ? (
          <div className="py-10 text-center text-sm text-slate-400">暂无时间线数据</div>
        ) : (
          <div className="relative pl-10">
            {/* Time labels on the left */}
            <div className="absolute left-0 top-0 flex flex-col justify-between" style={{ height: hours.length * rowHeight }}>
              {hours.map(h => (
                <span key={h} className="flex items-start text-[10px] font-medium text-slate-400" style={{ height: rowHeight }}>
                  {String(h).padStart(2, '0')}:00
                </span>
              ))}
            </div>

            {/* Track area */}
            <div className="overflow-hidden rounded-lg bg-slate-50" style={{ height: hours.length * rowHeight }}>
              {hours.map(h => (
                <div key={h} className="relative border-b border-slate-100/80 last:border-b-0" style={{ height: rowHeight }}>
                  {(segmentsByHour.get(h) || []).map((seg, i) => (
                    <div
                      key={i}
                      className="absolute flex cursor-pointer items-center overflow-hidden whitespace-nowrap rounded-md px-1.5 text-[11px] font-medium text-white shadow-sm transition-all hover:z-10 hover:scale-y-110 hover:shadow-md"
                      style={{
                        left: `${seg.leftPct}%`,
                        width: `${Math.max(seg.widthPct, 0.5)}%`,
                        height: rowHeight - 10,
                        top: 5,
                        background: `linear-gradient(135deg, ${seg.categoryColor}, ${seg.categoryColor}dd)`,
                        minWidth: seg.widthPct > 1 ? undefined : '4px',
                      }}
                      title={`${seg.appName} · ${String(seg.startH).padStart(2, '0')}:${String(seg.startM).padStart(2, '0')} - ${String(seg.endH).padStart(2, '0')}:${String(seg.endM).padStart(2, '0')}`}
                      onMouseEnter={(e) => {
                        const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
                        setTooltip({
                          appName: seg.appName,
                          timeRange: `${String(seg.startH).padStart(2, '0')}:${String(seg.startM).padStart(2, '0')} - ${String(seg.endH).padStart(2, '0')}:${String(seg.endM).padStart(2, '0')}`,
                          categoryName: seg.categoryName,
                          categoryColor: seg.categoryColor,
                          x: rect.left + rect.width / 2,
                          y: rect.top - 8,
                        });
                      }}
                      onMouseLeave={() => setTooltip(null)}
                    >
                      {seg.widthPct > 8 && (
                        <span className="truncate">{seg.icon} {seg.appName}</span>
                      )}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
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

      {/* Tooltip */}
      {tooltip && (
        <div
          className="fixed z-50 rounded-lg bg-slate-950 px-3 py-2 text-[11px] text-white shadow-xl pointer-events-none"
          style={{ left: tooltip.x, top: tooltip.y, transform: 'translate(-50%, -100%)' }}
        >
          <div className="font-semibold">{tooltip.appName}</div>
          <div className="mt-0.5 text-slate-300">{tooltip.timeRange}</div>
          <div className="mt-1 flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: tooltip.categoryColor }} />
            <span>{tooltip.categoryName}</span>
          </div>
        </div>
      )}
    </div>
  );
}

import { useMemo, useState } from 'react';
import { format } from 'date-fns';
import type { TimelineItem } from '../../types';
import { getPcBusinessDate } from '../../utils/pcBusinessDay';

interface TimelineEntry {
  start: Date;
  end: Date;
  categoryName: string;
  color: string;
  appName: string;
  windowTitle: string | null;
  confidence: number;
  durationMinutes: number;
}

interface ActiveTooltip {
  entry: TimelineEntry;
  x: number;
  y: number;
}

function getProductivityBadge(category: string): { label: string; color: string } {
  const productive = ['工作', '编程', '文档', '学习', '邮件'];
  const distracting = ['游戏', '视频', '娱乐', '社交'];
  if (productive.some(k => category.includes(k))) return { label: 'P', color: 'bg-emerald-400' };
  if (distracting.some(k => category.includes(k))) return { label: 'D', color: 'bg-rose-400' };
  return { label: 'N', color: 'bg-slate-300' };
}

interface Props {
  timeline: TimelineItem[];
}

export default function CategoryTimeline({ timeline }: Props) {
  const [tooltip, setTooltip] = useState<ActiveTooltip | null>(null);

  const { entries, dayStart } = useMemo(() => {
    if (!timeline.length) return { entries: [], dayStart: new Date() };

    const dayStart = new Date(getPcBusinessDate(new Date()).toISOString().slice(0, 10) + 'T00:00:00Z');

    const entries = timeline
      .filter(item => item.start && item.end)
      .map(item => {
        const start = new Date(item.start);
        const end = new Date(item.end);
        const durMs = end.getTime() - start.getTime();
        return {
          start,
          end,
          categoryName: item.categoryName || '其他',
          color: item.categoryColor || '#64748b',
          appName: item.appName || '',
          windowTitle: item.windowTitle,
          confidence: item.classificationConfidence,
          durationMinutes: item.durationMinutes || durMs / 60000,
        };
      })
      .sort((a, b) => a.start.getTime() - b.start.getTime());

    return { entries, dayStart };
  }, [timeline]);

  const totalMinutes = entries.reduce((s, e) => s + e.durationMinutes, 0);
  const productiveMin = entries
    .filter(e => getProductivityBadge(e.categoryName).label === 'P')
    .reduce((s, e) => s + e.durationMinutes, 0);

  const maxDuration = Math.max(...entries.map(e => e.durationMinutes), 1);

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      {/* Summary bar */}
      <div className="mb-4 flex items-center justify-between text-xs">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-slate-700">
            {format(dayStart, 'M月d日 EEEE')}
          </span>
          <span className="text-slate-400">
            总计 {totalMinutes.toFixed(0)} 分钟
          </span>
          {totalMinutes > 0 && (
            <span className="text-emerald-600 font-medium">
              生产性 {Math.round(productiveMin / totalMinutes * 100)}%
            </span>
          )}
        </div>
        <div className="flex items-center gap-3 text-[10px]">
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded bg-emerald-400" /> 生产性</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded bg-slate-300" /> 中性</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded bg-rose-400" /> 分心</span>
        </div>
      </div>

      {/* Vertical Timeline */}
      <div className="relative">
        {/* Vertical connector line */}
        <div className="absolute left-[71px] top-2 bottom-2 w-0.5 bg-slate-200 rounded-full" />

        {entries.length === 0 ? (
          <div className="py-8 text-center text-sm text-slate-400">暂无时间线数据</div>
        ) : (
          <div className="space-y-1">
            {entries.map((entry, i) => (
              <div
                key={i}
                className="group relative flex items-start gap-3 py-1.5 cursor-pointer transition-all hover:bg-slate-50 rounded-lg px-2 -mx-2"
                onMouseEnter={(e) => {
                  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
                  setTooltip({ entry, x: rect.left + 80, y: rect.top - 8 });
                }}
                onMouseLeave={() => setTooltip(null)}
              >
                {/* Time column */}
                <div className="w-[60px] shrink-0 pt-0.5 text-right">
                  <span className="text-[11px] font-medium text-slate-600">
                    {format(entry.start, 'HH:mm')}
                  </span>
                  <span className="text-[10px] text-slate-400 mx-0.5">-</span>
                  <span className="text-[11px] text-slate-500">
                    {format(entry.end, 'HH:mm')}
                  </span>
                </div>

                {/* Timeline dot */}
                <div className="relative shrink-0 pt-1.5">
                  <div
                    className="w-2.5 h-2.5 rounded-full border-2 border-white shadow-sm"
                    style={{ backgroundColor: entry.color }}
                  />
                </div>

                {/* Content */}
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-slate-800 truncate">
                      {entry.appName}
                    </span>
                    <span
                      className={`shrink-0 w-3.5 h-3.5 rounded-full text-[7px] flex items-center justify-center font-bold text-white ${
                        getProductivityBadge(entry.categoryName).color
                      }`}
                    >
                      {getProductivityBadge(entry.categoryName).label}
                    </span>
                    <span className="shrink-0 text-[10px] text-slate-400">
                      {entry.durationMinutes.toFixed(0)}m
                    </span>
                  </div>
                  {entry.windowTitle && (
                    <div className="text-[10px] text-slate-400 truncate mt-0.5 leading-tight">
                      {entry.windowTitle}
                    </div>
                  )}

                  {/* Duration bar */}
                  <div className="mt-1 h-1.5 w-full max-w-[300px] bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-200"
                      style={{
                        width: `${Math.max((entry.durationMinutes / maxDuration) * 100, 4)}%`,
                        backgroundColor: entry.color,
                      }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Category breakdown */}
      <div className="mt-4 flex flex-wrap gap-1.5">
        {Array.from(new Map(entries.map(e => [e.categoryName, { color: e.color, totalMin: 0 }]))).map(([name, info]) => {
          const mins = entries.filter(e => e.categoryName === name).reduce((s, e) => s + e.durationMinutes, 0);
          info.totalMin = mins;
          return { name, color: info.color, totalMin: mins };
        })
          .filter(item => item.totalMin > 0)
          .sort((a, b) => b.totalMin - a.totalMin)
          .map(item => (
            <div key={item.name} className="flex items-center gap-1 rounded-full bg-slate-50 px-2.5 py-1 text-[10px]">
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: item.color }} />
              <span className="text-slate-600">{item.name}</span>
              <span className="text-slate-400 ml-0.5">{item.totalMin.toFixed(0)}m</span>
            </div>
          ))}
      </div>

      {/* Tooltip */}
      {tooltip && (
        <div
          className="fixed z-50 rounded-xl bg-slate-950 px-3 py-2 text-[11px] text-white shadow-2xl pointer-events-none"
          style={{ left: tooltip.x, top: tooltip.y, transform: 'translate(0, -100%)' }}
        >
          <div className="font-semibold">{tooltip.entry.appName}</div>
          <div className="text-slate-300 mt-0.5">
            {format(tooltip.entry.start, 'HH:mm')} - {format(tooltip.entry.end, 'HH:mm')}
          </div>
          <div className="flex items-center gap-1.5 mt-1">
            <span className="w-2 h-2 rounded-full" style={{ backgroundColor: tooltip.entry.color }} />
            <span>{tooltip.entry.categoryName}</span>
            <span className={`ml-1 w-3.5 h-3.5 rounded-full text-[8px] flex items-center justify-center font-bold text-white ${
              getProductivityBadge(tooltip.entry.categoryName).color
            }`}>
              {getProductivityBadge(tooltip.entry.categoryName).label}
            </span>
          </div>
          <div className="text-slate-300 mt-0.5">
            {tooltip.entry.durationMinutes.toFixed(1)} 分钟
            {tooltip.entry.confidence > 0 && ` · ${(tooltip.entry.confidence * 100).toFixed(0)}% 置信`}
          </div>
        </div>
      )}
    </div>
  );
}

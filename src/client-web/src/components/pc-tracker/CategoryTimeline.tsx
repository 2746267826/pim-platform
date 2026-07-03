import { useMemo, useState } from 'react';
import { format } from 'date-fns';
import type { TimelineItem } from '../../types';
import { getPcBusinessDayStart } from '../../utils/pcBusinessDay';

interface GanttBlock {
  start: Date;
  end: Date;
  leftPct: number;
  widthPct: number;
  categoryName: string;
  color: string;
  appName: string;
  windowTitle: string | null;
  confidence: number;
  durationMinutes: number;
}

interface ActiveTooltip {
  block: GanttBlock;
  x: number;
  y: number;
}

const BLOCK_HEIGHT = 28;
const HOUR_WIDTH_PCT = 100 / 24;
const MIN_WIDTH_PX = 4;

function buildGanttBlocks(timeline: TimelineItem[]): { blocks: GanttBlock[]; dayStart: Date; dayEnd: Date } {
  if (!timeline.length) return { blocks: [], dayStart: new Date(), dayEnd: new Date() };

  const dayStart = getPcBusinessDayStart();
  const dayEnd = new Date(dayStart);
  dayEnd.setHours(dayEnd.getHours() + 24);

  const totalMs = dayEnd.getTime() - dayStart.getTime();
  const blocks = timeline
    .filter(item => item.start && item.end)
    .map(item => {
      const start = new Date(item.start);
      const end = new Date(item.end);
      // Clamp to business day
      const clampedStart = start < dayStart ? dayStart : start;
      const clampedEnd = end > dayEnd ? dayEnd : end;
      const leftMs = clampedStart.getTime() - dayStart.getTime();
      const durMs = clampedEnd.getTime() - clampedStart.getTime();
      return {
        start: clampedStart,
        end: clampedEnd,
        leftPct: (leftMs / totalMs) * 100,
        widthPct: Math.max((durMs / totalMs) * 100, 0.1),
        categoryName: item.categoryName || '其他',
        color: item.categoryColor || '#64748b',
        appName: item.appName || '',
        windowTitle: item.windowTitle,
        confidence: item.classificationConfidence,
        durationMinutes: item.durationMinutes || durMs / 60000,
      };
    })
    .filter(b => b.widthPct > 0);

  return { blocks, dayStart, dayEnd };
}

function getProductivityBadge(category: string): { label: string; color: string } {
  const productive = ['工作', '编程', '文档', '学习', '邮件'];
  const distracting = ['游戏', '视频', '娱乐', '社交'];
  if (productive.some(k => category.includes(k))) return { label: 'P', color: 'bg-emerald-400' };
  if (distracting.some(k => category.includes(k))) return { label: 'D', color: 'bg-rose-400' };
  return { label: 'N', color: 'bg-slate-300' };
}

const HOUR_LABELS = Array.from({ length: 24 }, (_, i) => ({
  label: `${String(i).padStart(2, '0')}:00`,
  hour: i,
}));

interface Props {
  timeline: TimelineItem[];
}

export default function CategoryTimeline({ timeline }: Props) {
  const [tooltip, setTooltip] = useState<ActiveTooltip | null>(null);

  const { blocks, dayStart } = useMemo(() => buildGanttBlocks(timeline), [timeline]);

  // Group by hour
  const hourBuckets = useMemo(() => {
    const buckets = Array.from({ length: 24 }, (_, i) => ({
      hour: i,
      blocks: [] as GanttBlock[],
      totalMin: 0,
    }));
    for (const block of blocks) {
      const startHour = block.start.getHours();
      for (let h = startHour; h <= block.end.getHours() && h < 24; h++) {
        if (buckets[h]) {
          buckets[h].blocks.push(block);
          buckets[h].totalMin += block.durationMinutes / Math.max(block.end.getHours() - block.start.getHours(), 1);
        }
      }
    }
    return buckets;
  }, [blocks]);

  const totalMinutes = blocks.reduce((s, b) => s + b.durationMinutes, 0);
  const productiveMin = blocks.filter(b => getProductivityBadge(b.categoryName).label === 'P').reduce((s, b) => s + b.durationMinutes, 0);

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

      {/* Gantt chart */}
      <div className="relative">
        {/* Hour grid lines */}
        <div className="flex border-b border-slate-100 mb-1">
          {HOUR_LABELS.filter((_, i) => i % 3 === 0).map(({ label, hour }) => (
            <div
              key={hour}
              className="text-[9px] text-slate-400 font-medium"
              style={{ width: `${HOUR_WIDTH_PCT * 3}%` }}
            >
              {label}
            </div>
          ))}
        </div>

        {/* Timeline rows - group by overlapping blocks */}
        <div className="relative" style={{ minHeight: BLOCK_HEIGHT * 2 + 8 }}>
          {/* Hour background stripes */}
          {HOUR_LABELS.map(({ hour }) => (
            <div
              key={hour}
              className="absolute top-0 bottom-0 border-l border-slate-50"
              style={{ left: `${hour * HOUR_WIDTH_PCT}%`, width: `${HOUR_WIDTH_PCT}%` }}
            />
          ))}

          {/* Blocks */}
          {blocks.length === 0 ? (
            <div className="py-8 text-center text-sm text-slate-400">暂无时间线数据</div>
          ) : (
            blocks.map((block, i) => (
              <div
                key={i}
                className="absolute flex items-center rounded-md px-1.5 cursor-pointer transition-all hover:opacity-80 hover:shadow-md overflow-hidden"
                style={{
                  left: `${block.leftPct}%`,
                  width: `${Math.max(block.widthPct, 0.3)}%`,
                  height: BLOCK_HEIGHT - 4 + 'px',
                  top: (i % 5) * (BLOCK_HEIGHT + 4) + 4 + 'px',
                  backgroundColor: block.color + '30',
                  borderLeft: `3px solid ${block.color}`,
                  minWidth: MIN_WIDTH_PX + 'px',
                  zIndex: 10 - i,
                }}
                onMouseEnter={(e) => {
                  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
                  setTooltip({ block, x: rect.left + rect.width / 2, y: rect.top - 8 });
                }}
                onMouseLeave={() => setTooltip(null)}
              >
                {block.widthPct > 1.5 && (
                  <>
                    <span className="text-[9px] font-medium text-slate-700 truncate">
                      {block.appName}
                    </span>
                    <span className="ml-1 text-[8px] text-slate-500 whitespace-nowrap">
                      {block.durationMinutes.toFixed(0)}m
                    </span>
                  </>
                )}
              </div>
            ))
          )}
        </div>

        {/* Hour labels at bottom */}
        <div className="flex mt-1 pt-1 border-t border-slate-100">
          {HOUR_LABELS.filter((_, i) => i % 3 === 0).map(({ label, hour }) => (
            <div key={hour} className="text-[9px] text-slate-300" style={{ width: `${HOUR_WIDTH_PCT * 3}%` }}>
              {label}
            </div>
          ))}
        </div>
      </div>

      {/* Category breakdown */}
      <div className="mt-4 flex flex-wrap gap-1.5">
        {Array.from(new Map(blocks.map(b => [b.categoryName, { color: b.color, totalMin: 0 }]))
        ).map(([name, info]) => {
          const mins = blocks.filter(b => b.categoryName === name).reduce((s, b) => s + b.durationMinutes, 0);
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
          style={{ left: tooltip.x, top: tooltip.y, transform: 'translate(-50%, -100%)' }}
        >
          <div className="font-semibold">{tooltip.block.appName}</div>
          <div className="text-slate-300 mt-0.5">
            {format(tooltip.block.start, 'HH:mm')} - {format(tooltip.block.end, 'HH:mm')}
          </div>
          <div className="flex items-center gap-1.5 mt-1">
            <span className="w-2 h-2 rounded-full" style={{ backgroundColor: tooltip.block.color }} />
            <span>{tooltip.block.categoryName}</span>
            <span className={`ml-1 w-3.5 h-3.5 rounded-full text-[8px] flex items-center justify-center font-bold text-white ${
              getProductivityBadge(tooltip.block.categoryName).color
            }`}>
              {getProductivityBadge(tooltip.block.categoryName).label}
            </span>
          </div>
          <div className="text-slate-300 mt-0.5">
            {tooltip.block.durationMinutes.toFixed(1)} 分钟
            {tooltip.block.confidence > 0 && ` · ${(tooltip.block.confidence * 100).toFixed(0)}% 置信`}
          </div>
        </div>
      )}
    </div>
  );
}

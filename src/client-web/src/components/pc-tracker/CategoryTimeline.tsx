import { useMemo, useState } from 'react';
import { format } from 'date-fns';
import type { TimelineItem } from '../../types';
import { getPcBusinessDayStart } from '../../utils/pcBusinessDay';

interface CategoryBlock {
  start: Date;
  end: Date;
  categoryName: string;
  color: string;
  projectTag: string | null;
  confidence: number | null;
  source: string | null;
  explanation: string | null;
  apps: { name: string; share: number }[];
  totalMinutes: number;
}

interface ActiveTooltip {
  block: CategoryBlock;
  left: number;
  top: number;
  placement: 'top' | 'bottom';
}

interface TimelineTick {
  label: string;
  leftPct: number;
  major: boolean;
}

const MAX_CATEGORY_MERGE_GAP_MS = 60_000;
const TOOLTIP_WIDTH = 280;
const TOOLTIP_GAP = 12;

function buildCategoryBlocks(timeline: TimelineItem[]): CategoryBlock[] {
  if (!timeline.length) return [];

  const sorted = [...timeline]
    .map(item => ({
      item,
      start: new Date(item.start),
      end: new Date(item.end),
    }))
    .filter(({ start, end }) => !Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime()) && end > start)
    .sort((a, b) => a.start.getTime() - b.start.getTime() || a.end.getTime() - b.end.getTime());

  const blocks: CategoryBlock[] = [];
  let current: CategoryBlock | null = null;

  for (const { item, start, end } of sorted) {
    const categoryName = item.categoryName || '其他';
    const projectTag = item.projectTag || null;
    const color = item.categoryColor || '#64748b';
    const appName = item.appName || 'unknown';

    if (current && current.categoryName === categoryName && current.projectTag === projectTag) {
      const gapMs = start.getTime() - current.end.getTime();
      if (gapMs > MAX_CATEGORY_MERGE_GAP_MS) {
        pushBlock(blocks, current);
        current = createBlock(item, start, end, categoryName, color, projectTag, appName);
        continue;
      }

      const previousEnd = current.end;
      if (end > current.end) current.end = end;

      const addedMinutes = Math.max(
        0,
        (current.end.getTime() - Math.max(previousEnd.getTime(), start.getTime())) / 60000
      );
      current.totalMinutes += addedMinutes;
      current.confidence = mergeConfidence(current.confidence, item.classificationConfidence);
      current.source = mergeText(current.source, item.classificationSource);
      current.explanation = mergeText(current.explanation, item.classificationExplanation);
      addAppShare(current, appName, addedMinutes);
      continue;
    }

    if (current) {
      if (start < current.end) {
        current.end = start;
        current.totalMinutes = Math.max(0, (current.end.getTime() - current.start.getTime()) / 60000);
      }
      pushBlock(blocks, current);
    }

    current = createBlock(item, start, end, categoryName, color, projectTag, appName);
  }

  if (current) pushBlock(blocks, current);

  for (const block of blocks) {
    const total = block.apps.reduce((sum, app) => sum + app.share, 0);
    for (const app of block.apps) app.share = total > 0 ? Math.round((app.share / total) * 100) : 0;
  }

  return blocks;
}

function createBlock(
  item: TimelineItem,
  start: Date,
  end: Date,
  categoryName: string,
  color: string,
  projectTag: string | null,
  appName: string
): CategoryBlock {
  return {
    start,
    end,
    categoryName,
    color,
    projectTag,
    confidence: item.classificationConfidence ?? null,
    source: item.classificationSource || null,
    explanation: item.classificationExplanation || null,
    apps: [{ name: appName, share: Math.max(0, (end.getTime() - start.getTime()) / 60000) }],
    totalMinutes: Math.max(0, (end.getTime() - start.getTime()) / 60000),
  };
}

function pushBlock(blocks: CategoryBlock[], block: CategoryBlock) {
  if (block.end > block.start) blocks.push(block);
}

function addAppShare(block: CategoryBlock, appName: string, minutes: number) {
  if (minutes <= 0) return;

  const existing = block.apps.find(app => app.name === appName);
  if (existing) existing.share += minutes;
  else block.apps.push({ name: appName, share: minutes });
}

function mergeConfidence(current: number | null, next: number | null) {
  if (current === null || current === undefined) return next ?? null;
  if (next === null || next === undefined) return current;
  return Math.min(current, next);
}

function mergeText(current: string | null, next: string | null) {
  if (!current) return next || null;
  if (!next || next === current) return current;
  return `${current} / ${next}`;
}

function fmtTime(iso: string) {
  try {
    return format(new Date(iso), 'HH:mm');
  } catch {
    return iso;
  }
}

function businessDayStart(date: Date) {
  return getPcBusinessDayStart(date);
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function getTickIntervalMinutes(zoom: number) {
  if (zoom >= 5) return 60;
  if (zoom >= 3) return 120;
  if (zoom >= 2) return 180;
  return 360;
}

function buildTimelineTicks(dayStart: Date, totalMs: number, zoom: number): TimelineTick[] {
  const totalMinutes = Math.round(totalMs / 60000);
  const intervalMinutes = getTickIntervalMinutes(zoom);
  const ticks: TimelineTick[] = [];

  for (let offsetMinutes = 0; offsetMinutes <= totalMinutes; offsetMinutes += intervalMinutes) {
    const timestamp = new Date(dayStart.getTime() + offsetMinutes * 60000);
    ticks.push({
      label: offsetMinutes === totalMinutes ? `${format(timestamp, 'HH:mm')}+1` : format(timestamp, 'HH:mm'),
      leftPct: (offsetMinutes / totalMinutes) * 100,
      major: offsetMinutes % 360 === 0,
    });
  }

  if (ticks[ticks.length - 1]?.leftPct !== 100) {
    const timestamp = new Date(dayStart.getTime() + totalMs);
    ticks.push({
      label: `${format(timestamp, 'HH:mm')}+1`,
      leftPct: 100,
      major: true,
    });
  }

  return ticks;
}

function tickLabelClass(leftPct: number) {
  if (leftPct <= 0) return 'translate-x-0 text-left';
  if (leftPct >= 100) return '-translate-x-full text-right';
  return '-translate-x-1/2 text-center';
}

interface Props {
  timeline: TimelineItem[];
}

export default function CategoryTimeline({ timeline }: Props) {
  const [zoom, setZoom] = useState(1);
  const [activeTooltip, setActiveTooltip] = useState<ActiveTooltip | null>(null);
  const blocks = useMemo(() => buildCategoryBlocks(timeline), [timeline]);

  if (!blocks.length) {
    return <div className="rounded-2xl border border-slate-200 bg-slate-50 py-10 text-center text-sm text-slate-400">暂无时间线数据</div>;
  }

  const dayStart = businessDayStart(blocks[0].start);
  const dayEnd = new Date(dayStart);
  dayEnd.setDate(dayEnd.getDate() + 1);
  const totalMs = dayEnd.getTime() - dayStart.getTime();
  const ticks = buildTimelineTicks(dayStart, totalMs, zoom);

  function showTooltip(block: CategoryBlock, element: HTMLElement) {
    const rect = element.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const wantsTop = rect.top >= 150;

    setActiveTooltip({
      block,
      left: clamp(centerX, TOOLTIP_WIDTH / 2 + 12, window.innerWidth - TOOLTIP_WIDTH / 2 - 12),
      top: wantsTop ? rect.top - TOOLTIP_GAP : rect.bottom + TOOLTIP_GAP,
      placement: wantsTop ? 'top' : 'bottom',
    });
  }

  return (
    <div className="relative overflow-visible rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="mb-3 flex flex-wrap items-center gap-3 text-[11px] text-slate-500">
        <label htmlFor="category-timeline-zoom" className="font-medium text-slate-600">缩放</label>
        <input
          id="category-timeline-zoom"
          type="range"
          min="1"
          max="6"
          step="0.5"
          value={zoom}
          onChange={event => setZoom(Number(event.target.value))}
          className="h-2 w-40 accent-blue-600"
        />
        <span className="tabular-nums text-slate-600">{zoom.toFixed(1)}x</span>
      </div>

      <div className="overflow-x-auto overflow-y-hidden pb-1" onScroll={() => setActiveTooltip(null)}>
        <div className="min-w-full" style={{ width: `${zoom * 100}%` }}>
          <div className="relative h-36 overflow-visible rounded-2xl border border-slate-200 bg-white px-1">
            {ticks.map(tick => (
              <div
                key={`grid-${tick.label}-${tick.leftPct}`}
                className={`pointer-events-none absolute top-4 bottom-4 w-px ${tick.major ? 'bg-slate-200' : 'bg-slate-100'}`}
                style={{ left: `${tick.leftPct}%` }}
              />
            ))}
            <div className="absolute inset-x-1 top-[72px] h-px bg-slate-100" />
            {blocks.map((block, index) => {
              const blockStart = Math.max(block.start.getTime(), dayStart.getTime());
              const blockEnd = Math.min(block.end.getTime(), dayEnd.getTime());
              if (blockEnd <= blockStart) return null;

              const leftPct = ((blockStart - dayStart.getTime()) / totalMs) * 100;
              const widthPct = Math.max(((blockEnd - blockStart) / totalMs) * 100, 0.45);
              const showInlineLabel = widthPct * zoom >= 6;

              return (
                <div
                  key={`${block.categoryName}-${block.start.toISOString()}-${index}`}
                  tabIndex={0}
                  className="absolute top-[52px] flex h-10 items-center justify-center rounded-lg px-1 text-[10px] font-medium text-white shadow-sm outline-none ring-offset-2 transition-transform hover:-translate-y-0.5 focus:ring-2 focus:ring-blue-300"
                  style={{ left: `${leftPct}%`, width: `${widthPct}%`, backgroundColor: block.color }}
                  aria-label={`${block.categoryName}: ${fmtTime(block.start.toISOString())} 到 ${fmtTime(block.end.toISOString())}`}
                  onMouseEnter={event => showTooltip(block, event.currentTarget)}
                  onFocus={event => showTooltip(block, event.currentTarget)}
                  onMouseLeave={() => setActiveTooltip(null)}
                  onBlur={() => setActiveTooltip(null)}
                >
                  {showInlineLabel && <span className="truncate">{block.categoryName}</span>}
                </div>
              );
            })}
          </div>

          <div className="relative mt-3 h-5 text-[10px] text-slate-400">
            {ticks.map(tick => (
              <span
                key={`label-${tick.label}-${tick.leftPct}`}
                className={`absolute top-0 min-w-10 ${tickLabelClass(tick.leftPct)}`}
                style={{ left: `${tick.leftPct}%` }}
              >
                {tick.label}
              </span>
            ))}
          </div>
        </div>
      </div>

      {activeTooltip && (
        <div
          className={`pointer-events-none fixed z-[9999] w-[280px] rounded-xl bg-slate-950 px-3 py-2 text-left text-[11px] text-white shadow-2xl ${
            activeTooltip.placement === 'top' ? '-translate-x-1/2 -translate-y-full' : '-translate-x-1/2'
          }`}
          style={{ left: activeTooltip.left, top: activeTooltip.top }}
        >
          <div className="mb-1 font-semibold">{activeTooltip.block.categoryName}</div>
          <div>{fmtTime(activeTooltip.block.start.toISOString())} - {fmtTime(activeTooltip.block.end.toISOString())}</div>
          <div className="text-slate-300">{Math.round(activeTooltip.block.totalMinutes)} 分钟</div>
          {activeTooltip.block.projectTag && <div className="text-slate-300">项目: {activeTooltip.block.projectTag}</div>}
          <div className="text-slate-300">
            来源: {activeTooltip.block.source || '未知'}{activeTooltip.block.confidence !== null ? ` · 置信度 ${Math.round(activeTooltip.block.confidence * 100)}%` : ''}
          </div>
          {activeTooltip.block.explanation && <div className="mt-1 whitespace-normal text-slate-300">{activeTooltip.block.explanation}</div>}
          <div className="mt-1 text-slate-300">
            {activeTooltip.block.apps.map(app => (
              <div key={app.name}>{app.name} {app.share}%</div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

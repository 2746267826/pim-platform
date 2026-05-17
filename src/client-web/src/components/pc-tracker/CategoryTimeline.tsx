import { useMemo } from 'react';
import { format } from 'date-fns';
import type { TimelineItem, CategorySummary } from '../../types';

interface CategoryBlock {
  start: Date;
  end: Date;
  categoryName: string;
  color: string;
  apps: { name: string; share: number }[];
  totalMinutes: number;
}

function buildCategoryBlocks(timeline: TimelineItem[], categories: CategorySummary[]): CategoryBlock[] {
  if (!timeline.length) return [];
  const catMap = new Map(categories.map(c => [c.categoryName, c.color]));
  const sorted = [...timeline].sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());

  const blocks: CategoryBlock[] = [];
  let current: CategoryBlock | null = null;

  for (const item of sorted) {
    // Determine category from the appName (simple heuristic: match app name against category keywords)
    const cat = categories.find(c =>
      item.appName.toLowerCase().includes(c.categoryName.toLowerCase())
    )?.categoryName || '其他';
    const color = catMap.get(cat) || '#8B5CF6';

    if (current && current.categoryName === cat) {
      current.end = new Date(item.end);
      current.totalMinutes += item.durationMinutes;
      const existing = current.apps.find(a => a.name === item.appName);
      if (existing) existing.share += item.durationMinutes;
      else current.apps.push({ name: item.appName, share: item.durationMinutes });
    } else {
      if (current) blocks.push(current);
      current = {
        start: new Date(item.start),
        end: new Date(item.end),
        categoryName: cat,
        color,
        apps: [{ name: item.appName, share: item.durationMinutes }],
        totalMinutes: item.durationMinutes,
      };
    }
  }
  if (current) blocks.push(current);

  for (const block of blocks) {
    const total = block.apps.reduce((s, a) => s + a.share, 0);
    for (const app of block.apps) app.share = Math.round((app.share / total) * 100);
  }

  return blocks;
}

function fmtTime(iso: string) {
  try { return format(new Date(iso), 'HH:mm'); } catch { return iso; }
}

interface Props {
  timeline: TimelineItem[];
  categories: CategorySummary[];
}

export default function CategoryTimeline({ timeline, categories }: Props) {
  const blocks = useMemo(() => buildCategoryBlocks(timeline, categories), [timeline, categories]);

  if (!blocks.length) return <div className="py-8 text-center text-gray-400">暂无时间线数据</div>;

  const dayStart = new Date(blocks[0].start);
  dayStart.setHours(0, 0, 0, 0);
  const dayEnd = new Date(dayStart);
  dayEnd.setDate(dayEnd.getDate() + 1);
  const totalMs = dayEnd.getTime() - dayStart.getTime();

  return (
    <div className="relative h-14 bg-gray-50 rounded-lg overflow-hidden">
      {blocks.map((block, i) => {
        const leftPct = ((block.start.getTime() - dayStart.getTime()) / totalMs) * 100;
        const widthPct = Math.max(((block.end.getTime() - block.start.getTime()) / totalMs) * 100, 0.5);
        return (
          <div key={i} className="absolute top-2 h-10 rounded-lg group flex items-center justify-center text-[10px] font-medium text-white truncate px-1"
            style={{ left: `${leftPct}%`, width: `${widthPct}%`, backgroundColor: block.color, opacity: 0.85 }}>
            {block.categoryName}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover:block bg-gray-800 text-white text-[10px] px-3 py-2 rounded-lg whitespace-nowrap z-10 min-w-[160px]">
              <div className="font-semibold mb-1">{block.categoryName}</div>
              <div>{fmtTime(block.start.toISOString())} — {fmtTime(block.end.toISOString())}</div>
              <div className="text-gray-300 mt-1">
                {block.apps.map(a => (
                  <div key={a.name}>{a.name} {a.share}%</div>
                ))}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

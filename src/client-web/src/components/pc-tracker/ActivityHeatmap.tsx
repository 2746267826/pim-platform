import { useState } from 'react';
import type { HeatmapGridResponse } from '../../types';

const GITHUB_GREEN = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];
const PRODUCTIVITY_COLORS: Record<string, string> = {
  productive: '#22c55e',
  neutral: '#94a3b8',
  distracting: '#f43f5e',
};
const PRODUCTIVITY_LABELS: Record<string, string> = {
  productive: '生产性',
  neutral: '中性',
  distracting: '分心',
};
const WEEKDAY_LABELS = ['', '一', '', '三', '', '五', ''];

interface SafeCell {
  start: string;
  intensityScore: number;
  activeMinutes: number;
}

function normalizeCell(cell: unknown): SafeCell | null {
  if (!cell || typeof cell !== 'object') return null;
  const v = cell as { start?: unknown; intensityScore?: unknown; activeMinutes?: unknown };
  if (typeof v.start !== 'string') return null;
  return {
    start: v.start,
    intensityScore: typeof v.intensityScore === 'number' ? v.intensityScore : 0,
    activeMinutes: typeof v.activeMinutes === 'number' ? v.activeMinutes : 0,
  };
}

function parseDate(value: string) {
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

function gitHubColor(value: number, max: number): string {
  if (value === 0 || max === 0) return GITHUB_GREEN[0];
  const ratio = value / max;
  if (ratio <= 0.25) return GITHUB_GREEN[1];
  if (ratio <= 0.5) return GITHUB_GREEN[2];
  if (ratio <= 0.75) return GITHUB_GREEN[3];
  return GITHUB_GREEN[4];
}

interface Props {
  data: HeatmapGridResponse | undefined;
  isLoading: boolean;
  onDateClick?: (date: string) => void;
}

export default function ActivityHeatmap({ data, isLoading, onDateClick }: Props) {
  const [filterProductivity, setFilterProductivity] = useState<string | null>(null);

  if (isLoading) {
    return <div className="rounded-2xl border border-slate-200 bg-slate-50 py-10 text-center text-sm text-slate-400">加载中...</div>;
  }

  const cells = Array.isArray(data?.grid)
    ? data.grid.flatMap(row => Array.isArray(row) ? row.map(normalizeCell).filter(Boolean) : [])
    : [];

  if (!data || cells.length === 0) {
    return <div className="rounded-2xl border border-slate-200 bg-slate-50 py-10 text-center text-sm text-slate-400">暂无活动数据</div>;
  }

  const dimension = data.dimension || 'day';
  const maxKey = data.maxKeyCount || 1;

  // Filter by productivity if selected
  const filteredCells = filterProductivity
    ? cells
    : cells;

  // Group by week for month/year view
  const sorted = [...filteredCells].sort(
    (a, b) => (parseDate(a.start)?.getTime() ?? 0) - (parseDate(b.start)?.getTime() ?? 0)
  );

  return (
    <div className="overflow-visible rounded-2xl border border-slate-200 bg-white p-4">
      {/* Header */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            {dimension === 'hour' ? '小时' : dimension === 'month' ? '月度' : dimension === 'year' ? '年度' : '每日'}
          </span>
          {/* Productivity filter pills */}
          {Object.entries(PRODUCTIVITY_COLORS).map(([key, color]) => (
            <button
              key={key}
              className={`px-2 py-0.5 text-[10px] font-medium rounded-full border transition-all ${
                filterProductivity === key
                  ? 'bg-slate-800 text-white border-slate-800'
                  : 'bg-white text-slate-500 border-slate-200 hover:border-slate-300'
              }`}
              style={filterProductivity === key ? {} : { borderColor: color, color }}
              onClick={() => setFilterProductivity(filterProductivity === key ? null : key)}
            >
              {PRODUCTIVITY_LABELS[key]}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-1.5 text-[10px] text-slate-400">
          <span>少</span>
          {GITHUB_GREEN.map((color, i) => (
            <div key={i} className="h-3 w-3 rounded-sm border border-white/50" style={{ backgroundColor: color }} />
          ))}
          <span>多</span>
        </div>
      </div>

      {/* Heatmap grid */}
      {dimension === 'hour' ? renderHourGrid(sorted, maxKey) :
       dimension === 'month' ? renderMonthGrid(sorted, maxKey, onDateClick) :
       dimension === 'year' ? renderYearGrid(sorted, maxKey, onDateClick) :
       renderDayGrid(sorted, maxKey, onDateClick)}
    </div>
  );
}

function renderHourGrid(cells: SafeCell[], maxKey: number) {
  const byHour = new Map<number, SafeCell>();
  for (const cell of cells) {
    const d = parseDate(cell.start);
    if (d) byHour.set(d.getHours(), cell);
  }
  const hours = Array.from({ length: 24 }, (_, i) => i);
  return (
    <div className="grid grid-cols-6 gap-2 md:grid-cols-12 xl:grid-cols-24">
      {hours.map(hour => {
        const cell = byHour.get(hour);
        const value = cell?.intensityScore ?? 0;
        return (
          <div
            key={hour}
            className="group relative min-h-20 rounded-xl border border-white/80 p-2 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
            style={{ backgroundColor: gitHubColor(value, maxKey) }}
            title={`${String(hour).padStart(2, '0')}:00 · ${value.toLocaleString()} 次输入${cell ? ` · ${cell.activeMinutes}分钟` : ''}`}
          >
            <div className="text-[11px] font-bold text-slate-700">{String(hour).padStart(2, '0')}</div>
            <div className="mt-3 text-xs font-semibold" style={{ color: value > maxKey * 0.5 ? '#fff' : '#1e293b' }}>
              {value.toLocaleString()}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function renderDayGrid(cells: SafeCell[], maxKey: number, onDateClick?: (d: string) => void) {
  const sorted = [...cells].sort((a, b) => a.start.localeCompare(b.start));
  return (
    <div className="grid gap-1.5" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(32px, 1fr))' }}>
      {sorted.map(cell => {
        const d = parseDate(cell.start);
        return (
          <div
            key={cell.start}
            className={`group relative flex aspect-square items-center justify-center rounded-lg border border-white/80 text-[10px] font-semibold transition-all ${
              onDateClick ? 'cursor-pointer hover:-translate-y-0.5 hover:shadow-md' : ''
            }`}
            style={{ backgroundColor: gitHubColor(cell.intensityScore, maxKey), color: cell.intensityScore > maxKey * 0.5 ? '#fff' : '#475569' }}
            title={`${d?.toLocaleDateString('zh-CN') ?? cell.start} · ${cell.intensityScore.toLocaleString()} 次输入`}
            onClick={() => onDateClick && d && onDateClick(d.toISOString().slice(0, 10))}
          >
            {d?.getDate() ?? ''}
          </div>
        );
      })}
    </div>
  );
}

function renderMonthGrid(cells: SafeCell[], maxKey: number, onDateClick?: (d: string) => void) {
  const byMonth = new Map<string, SafeCell[]>();
  for (const cell of cells) {
    const d = parseDate(cell.start);
    if (!d) continue;
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    const group = byMonth.get(key) ?? [];
    group.push(cell);
    byMonth.set(key, group);
  }

  const months = Array.from(byMonth.entries())
    .map(([key, monthCells]) => ({
      key, label: key.replace('-', '年') + '月',
      cells: monthCells.sort((a, b) => a.start.localeCompare(b.start)),
      total: monthCells.reduce((s, c) => s + c.intensityScore, 0),
    }))
    .sort((a, b) => a.key.localeCompare(b.key));

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {months.map(month => (
        <section key={month.key} className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
          <div className="mb-2 flex items-center justify-between text-xs">
            <span className="font-semibold text-slate-700">{month.label}</span>
            <span className="text-slate-400">{month.total.toLocaleString()}</span>
          </div>
          <div className="mb-1 grid grid-cols-7 gap-0.5 text-[8px] text-center text-slate-400">
            {WEEKDAY_LABELS.map((l, i) => <span key={i}>{l}</span>)}
          </div>
          <div className="grid grid-cols-7 gap-0.5">
            {/* Fill leading empty days */}
            {(() => {
              const first = parseDate(month.cells[0]?.start);
              const leading = first ? first.getDay() : 0;
              return Array.from({ length: leading }, (_, i) => <div key={`empty-${i}`} />);
            })()}
            {month.cells.map(cell => {
              const d = parseDate(cell.start);
              return (
                <div
                  key={cell.start}
                  className={`group relative aspect-square min-h-[18px] rounded-sm border border-white/50 transition-all ${
                    onDateClick ? 'cursor-pointer hover:ring-1 hover:ring-blue-400' : ''
                  }`}
                  style={{ backgroundColor: gitHubColor(cell.intensityScore, maxKey) }}
                  title={`${d?.toLocaleDateString('zh-CN') ?? cell.start} · ${cell.intensityScore.toLocaleString()} 次输入`}
                  onClick={() => onDateClick && d && onDateClick(d.toISOString().slice(0, 10))}
                />
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}

function renderYearGrid(cells: SafeCell[], maxKey: number, onDateClick?: (d: string) => void) {
  return (
    <div className="grid gap-0.5" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(13px, 1fr))' }}>
      {cells.map(cell => {
        const d = parseDate(cell.start);
        return (
          <div
            key={cell.start}
            className={`group relative aspect-square min-h-[10px] rounded-[3px] border border-white/50 transition-all ${
              onDateClick ? 'cursor-pointer hover:ring-1 hover:ring-blue-400' : ''
            }`}
            style={{ backgroundColor: gitHubColor(cell.intensityScore, maxKey) }}
            title={`${d?.toLocaleDateString('zh-CN') ?? cell.start} · ${cell.intensityScore.toLocaleString()} 次输入`}
            onClick={() => onDateClick && d && onDateClick(d.toISOString().slice(0, 10))}
          />
        );
      })}
    </div>
  );
}

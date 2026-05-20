import type { HeatmapGridResponse } from '../../types';
import { PC_BUSINESS_HOURS, pcHourLabel } from '../../utils/pcBusinessDay';

const COLOR_STOPS = ['#f8fafc', '#ccfbf1', '#5eead4', '#14b8a6', '#0f766e'];
const BUSINESS_HOURS = PC_BUSINESS_HOURS;
const WEEKDAY_LABELS = ['日', '一', '二', '三', '四', '五', '六'];

interface SafeHeatmapCell {
  start: string;
  intensityScore: number;
}

function linearColor(value: number, max: number): string {
  if (value === 0 || max === 0) return COLOR_STOPS[0];
  const ratio = Math.min(value / max, 1);
  const idx = ratio * (COLOR_STOPS.length - 1);
  const low = Math.floor(idx);
  const high = Math.min(low + 1, COLOR_STOPS.length - 1);
  const t = idx - low;
  const l = parseInt(COLOR_STOPS[low].slice(1), 16);
  const h = parseInt(COLOR_STOPS[high].slice(1), 16);
  const r = Math.round(((l >> 16) & 0xff) + t * (((h >> 16) & 0xff) - ((l >> 16) & 0xff)));
  const g = Math.round(((l >> 8) & 0xff) + t * (((h >> 8) & 0xff) - ((l >> 8) & 0xff)));
  const b = Math.round((l & 0xff) + t * ((h & 0xff) - (l & 0xff)));
  return `rgb(${r},${g},${b})`;
}

function normalizeCell(cell: unknown): SafeHeatmapCell | null {
  if (!cell || typeof cell !== 'object') return null;
  const value = cell as { start?: unknown; intensityScore?: unknown };
  if (typeof value.start !== 'string' || value.start.length === 0) return null;
  return {
    start: value.start,
    intensityScore: typeof value.intensityScore === 'number' && Number.isFinite(value.intensityScore)
      ? value.intensityScore
      : 0,
  };
}

function parseDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function cellKey(cell: SafeHeatmapCell) {
  const date = parseDate(cell.start);
  return date ? date.toISOString().slice(0, 10) : cell.start;
}

function cellTitle(cell: SafeHeatmapCell, dimension: string) {
  const date = parseDate(cell.start);
  const label = date
    ? dimension === 'hour'
      ? pcHourLabel(date.getHours())
      : date.toLocaleDateString('zh-CN')
    : cell.start;

  return `${label} · ${cell.intensityScore.toLocaleString('zh-CN')} 次输入`;
}

function HeatTooltip({ label, value }: { label: string; value: number }) {
  return (
    <div className="pointer-events-none absolute bottom-full left-1/2 z-50 mb-2 hidden -translate-x-1/2 whitespace-nowrap rounded-xl bg-slate-950 px-3 py-2 text-[11px] font-medium text-white shadow-2xl group-hover:block group-focus:block">
      {label} · {value.toLocaleString('zh-CN')} 次输入
    </div>
  );
}

function renderHourHeatmap(cells: SafeHeatmapCell[], maxKey: number) {
  const byHour = new Map<number, SafeHeatmapCell>();
  for (const cell of cells) {
    const date = parseDate(cell.start);
    if (date) byHour.set(date.getHours(), cell);
  }

  return (
    <div className="grid grid-cols-6 gap-2 md:grid-cols-12 xl:grid-cols-24">
      {BUSINESS_HOURS.map(hour => {
        const cell = byHour.get(hour);
        const value = cell?.intensityScore ?? 0;
        const label = pcHourLabel(hour);
        return (
          <div
            key={hour}
            tabIndex={0}
            className="group relative min-h-20 rounded-2xl border border-white/80 p-2 shadow-sm outline-none transition-transform hover:-translate-y-0.5 hover:ring-2 hover:ring-blue-300 focus:ring-2 focus:ring-blue-300"
            style={{ backgroundColor: linearColor(value, maxKey) }}
            title={`${label} · ${value.toLocaleString('zh-CN')} 次输入`}
          >
            <div className="text-[11px] font-bold text-slate-700">{String(hour).padStart(2, '0')}</div>
            <div className="mt-4 text-xs font-semibold text-slate-950">{value.toLocaleString('zh-CN')}</div>
            <HeatTooltip label={label} value={value} />
          </div>
        );
      })}
    </div>
  );
}

function renderDayHeatmap(cells: SafeHeatmapCell[], maxKey: number) {
  return (
    <div className="grid gap-1.5 [grid-template-columns:repeat(auto-fit,minmax(30px,1fr))]">
      {cells.map(cell => (
        <div
          key={cellKey(cell)}
          tabIndex={0}
          className="group relative flex aspect-square min-h-8 items-center justify-center rounded-lg border border-white/80 text-[10px] font-semibold text-slate-700 outline-none transition-transform hover:-translate-y-0.5 hover:ring-2 hover:ring-blue-300 focus:ring-2 focus:ring-blue-300"
          style={{ backgroundColor: linearColor(cell.intensityScore, maxKey) }}
          title={cellTitle(cell, 'day')}
        >
          {parseDate(cell.start)?.getDate() ?? ''}
          <HeatTooltip label={parseDate(cell.start)?.toLocaleDateString('zh-CN') ?? cell.start} value={cell.intensityScore} />
        </div>
      ))}
    </div>
  );
}

function groupCellsByMonth(cells: SafeHeatmapCell[]) {
  const groups = new Map<string, SafeHeatmapCell[]>();
  for (const cell of cells) {
    const date = parseDate(cell.start);
    if (!date) continue;
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const group = groups.get(key) ?? [];
    group.push(cell);
    groups.set(key, group);
  }

  return Array.from(groups.entries())
    .map(([key, monthCells]) => ({
      key,
      label: key.replace('-', '年') + '月',
      cells: monthCells.sort((a, b) => (parseDate(a.start)?.getTime() ?? 0) - (parseDate(b.start)?.getTime() ?? 0)),
      total: monthCells.reduce((sum, cell) => sum + cell.intensityScore, 0),
    }))
    .sort((a, b) => a.key.localeCompare(b.key));
}

function renderMonthHeatmap(cells: SafeHeatmapCell[], maxKey: number) {
  const months = groupCellsByMonth(cells);

  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {months.map(month => (
        <section key={month.key} className="rounded-2xl border border-slate-200 bg-white/85 p-3 shadow-sm">
          <div className="mb-2 flex items-center justify-between text-xs">
            <span className="font-semibold text-slate-800">{month.label}</span>
            <span className="text-slate-400">{month.total.toLocaleString('zh-CN')}</span>
          </div>
          <div className="mb-1 grid grid-cols-7 gap-1 text-center text-[9px] font-semibold text-slate-400">
            {WEEKDAY_LABELS.map(label => <span key={label}>{label}</span>)}
          </div>
          <div className="grid grid-cols-7 gap-1">
            {month.cells.map(cell => {
              const date = parseDate(cell.start);
              return (
                <div
                  key={cellKey(cell)}
                  tabIndex={0}
                  className="group relative flex aspect-square items-center justify-center rounded-md border border-white/80 text-[9px] font-semibold text-slate-700 outline-none hover:ring-2 hover:ring-blue-300 focus:ring-2 focus:ring-blue-300"
                  style={{ backgroundColor: linearColor(cell.intensityScore, maxKey) }}
                  title={cellTitle(cell, 'month')}
                >
                  {date?.getDate() ?? ''}
                  <HeatTooltip label={date?.toLocaleDateString('zh-CN') ?? cell.start} value={cell.intensityScore} />
                </div>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}

function renderYearHeatmap(cells: SafeHeatmapCell[], maxKey: number) {
  return (
    <div className="grid gap-1 [grid-template-columns:repeat(auto-fit,minmax(14px,1fr))]">
      {cells.map(cell => (
        <div
          key={cellKey(cell)}
          tabIndex={0}
          className="group relative aspect-square min-h-3 rounded-[4px] border border-white/80 outline-none hover:ring-2 hover:ring-blue-300 focus:ring-2 focus:ring-blue-300"
          style={{ backgroundColor: linearColor(cell.intensityScore, maxKey) }}
          title={cellTitle(cell, 'year')}
        >
          <HeatTooltip label={parseDate(cell.start)?.toLocaleDateString('zh-CN') ?? cell.start} value={cell.intensityScore} />
        </div>
      ))}
    </div>
  );
}

interface Props {
  data: HeatmapGridResponse | undefined;
  isLoading: boolean;
}

export default function ActivityHeatmap({ data, isLoading }: Props) {
  if (isLoading) {
    return <div className="rounded-2xl border border-slate-200 bg-slate-50 py-10 text-center text-sm text-slate-400">加载中...</div>;
  }

  const cells = Array.isArray(data?.grid)
    ? data.grid.flatMap(row => Array.isArray(row) ? row.map(normalizeCell).filter(cell => cell !== null) : [])
    : [];

  if (!data || cells.length === 0) {
    return <div className="rounded-2xl border border-slate-200 bg-slate-50 py-10 text-center text-sm text-slate-400">暂无活动数据</div>;
  }

  const dimension = data.dimension || 'day';
  const maxKey = data.maxKeyCount || 1;
  const content = dimension === 'hour'
    ? renderHourHeatmap(cells, maxKey)
    : dimension === 'month'
      ? renderMonthHeatmap(cells, maxKey)
      : dimension === 'year'
        ? renderYearHeatmap(cells, maxKey)
        : renderDayHeatmap(cells, maxKey);

  return (
    <div className="overflow-visible rounded-2xl border border-slate-200 bg-slate-50/90 p-4">
      <div className="mb-4 flex items-center justify-between gap-3 text-[11px] text-slate-500">
        <span className="font-semibold uppercase tracking-[0.18em]">{dimension} 维度</span>
        <div className="flex items-center gap-1">
          <span>低</span>
          {COLOR_STOPS.map((color, index) => (
            <div key={index} className="h-3 w-3 rounded-sm border border-white/70" style={{ backgroundColor: color }} />
          ))}
          <span>高</span>
        </div>
      </div>
      {content}
    </div>
  );
}

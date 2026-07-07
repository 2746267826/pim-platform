import type { MobileAnalyticsChart } from '../../api/mobile';
import { formatDuration, formatNumber } from './mobileFormatting';

export interface MobileChartsGridProps {
  charts: MobileAnalyticsChart[];
  isLoading?: boolean;
  onCategorySelect?: (category: string) => void;
  onAppSelect?: (packageName: string) => void;
}

function valueLabel(unit: string, value: number) {
  return unit === 'seconds' ? formatDuration(value) : formatNumber(value);
}

export default function MobileChartsGrid({
  charts,
  isLoading = false,
  onCategorySelect,
  onAppSelect,
}: MobileChartsGridProps) {
  return (
    <section className="grid grid-cols-1 gap-4 xl:grid-cols-2">
      {charts.map(chart => {
        const maxValue = Math.max(1, ...chart.points.map(point => point.value));
        return (
          <div key={chart.key} className="rounded-md border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold text-slate-950">{chart.title}</h2>
              <span className="text-xs text-slate-500">{chart.chartType}</span>
            </div>
            <div className="mt-4 space-y-2">
              {chart.points.slice(0, 8).map(point => {
                const width = `${Math.max(3, (point.value / maxValue) * 100)}%`;
                const handleSelect = point.lifeCategory && onCategorySelect
                  ? () => onCategorySelect(point.lifeCategory!)
                  : point.packageName && onAppSelect
                    ? () => onAppSelect(point.packageName!)
                    : null;
                const content = (
                  <>
                    <span className="min-w-0">
                      <span className="block truncate text-slate-600">{point.label}</span>
                      {point.packageName && (
                        <span className="mt-0.5 block truncate font-mono text-[11px] text-slate-400">
                          {point.packageName}
                        </span>
                      )}
                    </span>
                    <span className="h-3 overflow-hidden rounded bg-slate-100">
                      <span className="block h-full rounded bg-teal-500" style={{ width }} />
                    </span>
                    <span className="tabular-nums text-slate-500">{valueLabel(chart.unit, point.value)}</span>
                  </>
                );
                return handleSelect ? (
                  <button
                    key={point.key}
                    type="button"
                    onClick={handleSelect}
                    className="grid w-full grid-cols-[minmax(96px,0.35fr)_1fr_auto] items-center gap-3 text-left text-xs"
                  >
                    {content}
                  </button>
                ) : (
                  <div
                    key={point.key}
                    className="grid w-full grid-cols-[minmax(96px,0.35fr)_1fr_auto] items-center gap-3 text-left text-xs"
                  >
                    {content}
                  </div>
                );
              })}
            </div>
            {isLoading && <p className="mt-3 text-xs text-slate-500">正在加载图表</p>}
            {!isLoading && chart.points.length === 0 && <p className="mt-3 text-xs text-slate-500">暂无图表数据</p>}
          </div>
        );
      })}
    </section>
  );
}

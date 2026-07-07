import type { MobileAnalyticsGranularity, MobileHeatmapBucket } from '../../api/mobile';
import { formatDuration } from './mobileFormatting';
import { buildHeatmapMatrix, type HeatmapMatrixCell } from './mobileHeatmapMatrix';

export type MobileHeatmapGranularity = Extract<MobileAnalyticsGranularity, 'hour' | '30m' | '15m'>;

export interface MobileUsageHeatmapProps {
  buckets: MobileHeatmapBucket[];
  granularity: MobileHeatmapGranularity;
  selectedBucketStartUtc?: string | null;
  isLoading?: boolean;
  onGranularityChange: (granularity: MobileHeatmapGranularity) => void;
  onBucketSelect: (bucket: MobileHeatmapBucket) => void;
}

const granularities: Array<{ key: MobileHeatmapGranularity; label: string }> = [
  { key: 'hour', label: '小时' },
  { key: '30m', label: '30m' },
  { key: '15m', label: '15m' },
];

function cellBackground(cell: HeatmapMatrixCell, maxSeconds: number) {
  if (cell.foregroundSeconds <= 0) return 'rgba(248, 250, 252, 1)';
  const intensity = Math.max(0.12, cell.foregroundSeconds / maxSeconds);
  return `rgba(20, 184, 166, ${Math.min(0.88, intensity)})`;
}

export default function MobileUsageHeatmap({
  buckets,
  granularity,
  selectedBucketStartUtc = null,
  isLoading = false,
  onGranularityChange,
  onBucketSelect,
}: MobileUsageHeatmapProps) {
  const matrix = buildHeatmapMatrix(buckets);

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-slate-950">使用热力图</h2>
          <p className="mt-1 text-xs text-slate-500">
            左侧是日期，顶部是小时。格子不再重复写小时，点击后在右侧查看分类和 App 明细。
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span>少</span>
            <span className="h-2 w-20 rounded-full bg-gradient-to-r from-slate-100 via-teal-100 to-teal-600" />
            <span>多</span>
          </div>
          <div className="flex rounded-md border border-slate-200 bg-slate-50 p-1">
            {granularities.map(item => (
              <button
                key={item.key}
                type="button"
                onClick={() => onGranularityChange(item.key)}
                className={`h-8 rounded px-3 text-xs font-medium ${
                  granularity === item.key ? 'bg-slate-950 text-white' : 'text-slate-600 hover:bg-white'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-4 overflow-x-auto">
        <div
          className="grid min-w-[920px] gap-1"
          style={{ gridTemplateColumns: '92px repeat(24, minmax(24px, 1fr))' }}
        >
          <div className="h-7" aria-hidden="true" />
          {matrix.hours.map(hour => (
            <div key={hour} className="flex h-7 items-center justify-center text-[10px] font-medium text-slate-500">
              {hour}
            </div>
          ))}

          {matrix.days.map(day => (
            <div key={day.localDate} className="contents">
              <div className="flex h-7 items-center truncate pr-2 text-xs font-medium text-slate-600">
                {day.label}
              </div>
              {day.cells.map(cell => {
                const primaryBucket = cell.sourceBuckets[0];
                const isSelected = Boolean(cell.bucketStartUtc && selectedBucketStartUtc === cell.bucketStartUtc);
                return (
                  <button
                    key={`${cell.localDate}-${cell.localHour}`}
                    type="button"
                    data-bucket-start={cell.bucketStartUtc ?? ''}
                    disabled={!primaryBucket}
                    onClick={() => {
                      if (primaryBucket) onBucketSelect(primaryBucket);
                    }}
                    className={`relative h-7 rounded border text-[0px] transition disabled:cursor-default ${
                      isSelected ? 'border-slate-950 ring-2 ring-slate-300' : 'border-slate-100'
                    }`}
                    style={{ backgroundColor: cellBackground(cell, matrix.maxSeconds) }}
                    title={`${cell.localDate} ${cell.localHour}:00 ${cell.categories.map(category => category.lifeCategory).join(' / ') || '空闲'} ${formatDuration(cell.foregroundSeconds)}`}
                    aria-label={`${cell.localDate} ${cell.localHour}:00 ${formatDuration(cell.foregroundSeconds)}`}
                  >
                    {cell.qualityFlags.length > 0 && (
                      <span className="absolute inset-x-1 bottom-0 h-0.5 rounded bg-amber-400" />
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      </div>

      {isLoading && <p className="mt-3 text-xs text-slate-500">正在加载热力图</p>}
      {!isLoading && buckets.length === 0 && <p className="mt-3 text-xs text-slate-500">所选范围暂无热力图数据</p>}
    </section>
  );
}

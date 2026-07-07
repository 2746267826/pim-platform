import type { MobileAnalyticsGranularity, MobileHeatmapBucket } from '../../api/mobile';
import { formatDuration } from './mobileFormatting';

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

export default function MobileUsageHeatmap({
  buckets,
  granularity,
  selectedBucketStartUtc = null,
  isLoading = false,
  onGranularityChange,
  onBucketSelect,
}: MobileUsageHeatmapProps) {
  const maxSeconds = Math.max(1, ...buckets.map(bucket => bucket.foregroundSeconds));

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-slate-950">热力图</h2>
          <p className="mt-1 text-xs text-slate-500">北京时间，点击色块钻取时间段</p>
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

      <div className="mt-4 overflow-x-auto">
        <div
          className="grid min-w-[720px] gap-1"
          style={{ gridTemplateColumns: 'repeat(24, minmax(24px, 1fr))' }}
        >
          {buckets.map(bucket => {
            const intensity = Math.max(0.08, bucket.foregroundSeconds / maxSeconds);
            const isSelected = selectedBucketStartUtc === bucket.bucketStartUtc;
            return (
              <button
                key={`${bucket.bucketStartUtc}-${bucket.lifeCategory}`}
                type="button"
                data-bucket-start={bucket.bucketStartUtc}
                onClick={() => onBucketSelect(bucket)}
                className={`h-11 rounded border text-[10px] font-medium transition ${
                  isSelected ? 'border-slate-950 ring-2 ring-slate-300' : 'border-slate-100'
                }`}
                style={{ backgroundColor: `rgba(20, 184, 166, ${intensity})` }}
                title={`${bucket.localDate} ${bucket.localHour}:00 ${bucket.lifeCategory} ${formatDuration(bucket.foregroundSeconds)}`}
              >
                {bucket.localHour}
              </button>
            );
          })}
        </div>
      </div>

      {isLoading && <p className="mt-3 text-xs text-slate-500">正在加载热力图</p>}
      {!isLoading && buckets.length === 0 && <p className="mt-3 text-xs text-slate-500">所选范围暂无热力图数据</p>}
    </section>
  );
}

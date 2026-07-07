import type { MobileAnalyticsOverview } from '../../api/mobile';
import {
  formatCompactDuration,
  formatDateTime,
  formatDuration,
  formatNumber,
  formatPercent,
  formatSignedPercent,
} from './mobileFormatting';

export interface MobileInsightStripProps {
  overview?: MobileAnalyticsOverview;
  isLoading?: boolean;
}

function Metric({
  label,
  value,
  helper,
  tone = 'neutral',
}: {
  label: string;
  value: string;
  helper: string;
  tone?: 'neutral' | 'good' | 'warn';
}) {
  const toneClass = tone === 'good'
    ? 'border-teal-200 bg-teal-50'
    : tone === 'warn'
      ? 'border-amber-200 bg-amber-50'
      : 'border-slate-200 bg-white';

  return (
    <div className={`min-h-28 min-w-0 rounded-md border p-4 ${toneClass}`}>
      <dt className="truncate text-xs font-medium text-slate-500">{label}</dt>
      <dd className="mt-2 truncate text-2xl font-semibold tracking-normal text-slate-950">{value}</dd>
      <p className="mt-2 truncate text-xs text-slate-500">{helper}</p>
    </div>
  );
}

export default function MobileInsightStrip({ overview, isLoading = false }: MobileInsightStripProps) {
  const goal = overview?.goalProgress;
  const goalTone = goal?.isOverLimit ? 'warn' : goal ? 'good' : 'neutral';

  return (
    <section className="mx-auto max-w-[1500px] px-4 sm:px-6">
      <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-6">
        <Metric
          label="总使用时长"
          value={isLoading ? '加载中' : formatDuration(overview?.totalForegroundSeconds)}
          helper={overview ? `较上期 ${formatSignedPercent(overview.previousPeriodChange)}` : '等待分析数据'}
        />
        <Metric
          label="日均"
          value={formatDuration(overview?.dailyAverageSeconds)}
          helper={overview?.highestUseLocalDate ? `峰值日 ${overview.highestUseLocalDate}` : '近 7 天均值'}
        />
        <Metric
          label="目标"
          value={goal ? `${formatCompactDuration(goal.usedSeconds)} / ${formatCompactDuration(goal.limitSeconds)}` : '未设置'}
          helper={goal ? goal.label : '可配置使用目标'}
          tone={goalTone}
        />
        <Metric
          label="应用数"
          value={formatNumber(overview?.appCount)}
          helper={`切换 ${formatNumber(overview?.switchOrPickupCount)} 次`}
        />
        <Metric
          label="完整度"
          value={formatPercent(overview?.completeness)}
          helper={`事件覆盖 ${formatPercent(overview?.quality.usageEventsCoverage)}`}
          tone={(overview?.completeness ?? 0) >= 0.9 ? 'good' : 'warn'}
        />
        <Metric
          label="最近同步"
          value={formatDateTime(overview?.quality.lastSyncAt).split(' ')[0] ?? '-'}
          helper={overview?.isStale ? '数据可能过期' : `生成 ${formatDateTime(overview?.generatedAt)}`}
        />
      </dl>
    </section>
  );
}

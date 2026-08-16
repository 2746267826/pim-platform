import type { PcQualityResponse, PimHealthStatus } from '../../types';
import StatusBadge from '../../ui/StatusBadge';
import EChartBox from '../charts/EChartBox';
import { buildQualityRingOption } from '../charts/pcPanelOptions';

type StatusTone = 'primary' | 'warning' | 'danger' | 'neutral';

const toneByStatus: Record<PimHealthStatus, StatusTone> = {
  Unknown: 'neutral',
  Healthy: 'primary',
  Warning: 'warning',
  Critical: 'danger',
};

const fallbackLabel: Record<PimHealthStatus, string> = {
  Unknown: '未知',
  Healthy: '正常',
  Warning: '警告',
  Critical: '严重',
};

function formatCheckedAt(value?: string) {
  if (!value) return '未知';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN');
}

function errorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message;
  return 'PC 数据质量暂不可用，请稍后刷新重试。';
}

export default function PcQualitySummary({
  quality,
  isLoading = false,
  error,
  compact = false,
}: {
  quality: PcQualityResponse | undefined;
  isLoading?: boolean;
  error?: unknown;
  compact?: boolean;
}) {
  const panelClass = 'rounded-lg border border-slate-200 bg-white p-4';
  const issues = quality?.issues.slice(0, compact ? 2 : 4) ?? [];
  const nextSteps = quality?.nextSteps.slice(0, compact ? 2 : 3) ?? [];

  if (isLoading) {
    return (
      <section className={panelClass}>
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-slate-950">PC 数据质量</h2>
          <StatusBadge tone="neutral">检查中</StatusBadge>
        </div>
        <p className="mt-2 text-sm text-slate-500">正在检查 PC 数据质量...</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="rounded-lg border border-red-200 bg-white p-4">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-slate-950">PC 数据质量</h2>
          <StatusBadge tone="danger">不可用</StatusBadge>
        </div>
        <p className="mt-2 text-sm text-red-600">{errorMessage(error)}</p>
      </section>
    );
  }

  if (!quality) {
    return (
      <section className={panelClass}>
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-slate-950">PC 数据质量</h2>
          <StatusBadge tone="neutral">无数据</StatusBadge>
        </div>
        <p className="mt-2 text-sm text-slate-500">暂无 PC 数据质量信息。</p>
      </section>
    );
  }

  const status = quality.overallStatus;
  const statusLabel = quality.label || fallbackLabel[status];
  const healthyCount = quality.components.filter(component => component.status === 'Healthy').length;
  const ringOption = buildQualityRingOption(healthyCount, quality.components.length);

  return (
    <section className={panelClass}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-slate-950">PC 数据质量</h2>
          <p className="mt-1 text-sm text-slate-600">
            {quality.message || '暂无质量检查说明。'}
          </p>
        </div>
        <StatusBadge tone={toneByStatus[status]}>{statusLabel}</StatusBadge>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-slate-100 pt-3">
        <EChartBox
          option={ringOption}
          height={96}
          className="w-24 shrink-0"
          ariaLabel="数据质量完成率"
        />
        <dl className="grid flex-1 min-w-0 grid-cols-2 gap-3 text-xs">
          <div className="min-w-0">
            <dt className="text-slate-400">健康组件</dt>
            <dd className="mt-1 font-medium text-slate-700">
              {healthyCount}/{quality.components.length}
            </dd>
          </div>
          <div className="min-w-0">
            <dt className="text-slate-400">问题数</dt>
            <dd className="mt-1 font-medium text-slate-700">{quality.issues.length}</dd>
          </div>
        </dl>
      </div>

      <dl className="mt-4 grid grid-cols-3 gap-3 border-t border-slate-100 pt-3 text-xs">
        <div className="min-w-0">
          <dt className="text-slate-400">检查时间</dt>
          <dd className="mt-1 truncate font-medium text-slate-700">{formatCheckedAt(quality.checkedAt)}</dd>
        </div>
        <div className="min-w-0">
          <dt className="text-slate-400">问题数</dt>
          <dd className="mt-1 font-medium text-slate-700">{quality.issues.length}</dd>
        </div>
        <div className="min-w-0">
          <dt className="text-slate-400">组件数</dt>
          <dd className="mt-1 font-medium text-slate-700">{quality.components.length}</dd>
        </div>
      </dl>

      {issues.length > 0 && (
        <div className="mt-4">
          <h3 className="text-xs font-semibold text-slate-500">问题</h3>
          <ul className="mt-2 space-y-2">
            {issues.map(issue => (
              <li key={`${issue.code}-${issue.componentKey}`} className="rounded-md bg-slate-50 px-3 py-2">
                <div className="flex items-start justify-between gap-2">
                  <p className="min-w-0 break-words text-sm text-slate-700">{issue.message || issue.code}</p>
                  <StatusBadge tone={toneByStatus[issue.severity]}>{fallbackLabel[issue.severity]}</StatusBadge>
                </div>
                {issue.nextStep && <p className="mt-1 text-xs text-slate-500">{issue.nextStep}</p>}
              </li>
            ))}
          </ul>
        </div>
      )}

      {nextSteps.length > 0 && (
        <div className="mt-4">
          <h3 className="text-xs font-semibold text-slate-500">下一步</h3>
          <ul className="mt-2 space-y-1">
            {nextSteps.map((step, index) => (
              <li key={`${step}-${index}`} className="text-sm text-slate-600">
                {step}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

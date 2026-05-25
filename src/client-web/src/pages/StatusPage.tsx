import { useQuery } from '@tanstack/react-query';
import { getStatusDetail } from '../api/status';
import type { PimHealthStatus, StatusComponent } from '../types';
import PageHeader from '../ui/PageHeader';

const statusStyles: Record<PimHealthStatus, { text: string; bg: string; border: string; dot: string }> = {
  Healthy: {
    text: 'text-emerald-700',
    bg: 'bg-emerald-50',
    border: 'border-emerald-200',
    dot: 'bg-emerald-500',
  },
  Warning: {
    text: 'text-amber-700',
    bg: 'bg-amber-50',
    border: 'border-amber-200',
    dot: 'bg-amber-500',
  },
  Critical: {
    text: 'text-red-700',
    bg: 'bg-red-50',
    border: 'border-red-200',
    dot: 'bg-red-500',
  },
  Unknown: {
    text: 'text-slate-600',
    bg: 'bg-slate-50',
    border: 'border-slate-200',
    dot: 'bg-slate-400',
  },
};

function formatCheckedAt(value?: string) {
  if (!value) return '未知';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN');
}

function StatusPill({ status, label }: { status: PimHealthStatus; label: string }) {
  const styles = statusStyles[status];
  return (
    <span className={`inline-flex max-w-full items-center gap-2 rounded-full border px-2.5 py-1 text-xs font-semibold ${styles.bg} ${styles.border} ${styles.text}`}>
      <span className={`h-2 w-2 shrink-0 rounded-full ${styles.dot}`} aria-hidden="true" />
      <span className="truncate">{label}</span>
    </span>
  );
}

function ComponentCard({ component }: { component: StatusComponent }) {
  const detailEntries = Object.entries(component.details || {});

  return (
    <section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="truncate text-sm font-semibold text-slate-950">{component.name}</h2>
          <p className="mt-1 truncate text-xs text-slate-500">{component.kind}</p>
        </div>
        <StatusPill status={component.status} label={component.status} />
      </div>

      <p className="mt-3 text-sm text-slate-600">{component.message || '系统状态暂不可用'}</p>
      <p className="mt-2 text-xs text-slate-400">检查时间：{formatCheckedAt(component.checkedAt)}</p>

      {detailEntries.length > 0 && (
        <dl className="mt-4 grid grid-cols-1 gap-2 border-t border-slate-100 pt-3 sm:grid-cols-2">
          {detailEntries.map(([key, value]) => (
            <div key={key} className="min-w-0">
              <dt className="truncate text-[11px] font-medium uppercase text-slate-400">{key}</dt>
              <dd className="mt-0.5 break-words text-xs text-slate-700">{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </section>
  );
}

export default function StatusPage() {
  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['status-detail'],
    queryFn: getStatusDetail,
    refetchInterval: 60_000,
  });

  const summary = data?.summary;
  const summaryStatus = summary?.status ?? 'Unknown';

  return (
    <div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
      <PageHeader
        title="状态信息"
        subtitle="查看 API、数据库、daemon、采集源和后台任务状态。"
        actions={
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            className="pim-button-secondary px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            刷新
          </button>
        }
      />

      {isLoading && (
        <section className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-500">
          正在检查系统状态...
        </section>
      )}

      {isError && (
        <section className="rounded-lg border border-red-200 bg-red-50 p-6">
          <p className="text-sm font-semibold text-red-700">系统状态暂不可用</p>
          <p className="mt-1 text-sm text-red-600">请稍后刷新重试。</p>
        </section>
      )}

      {!isLoading && !isError && summary && (
        <>
          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Overall</p>
                <h2 className="mt-2 text-2xl font-semibold text-slate-950">{summary.label || '未知'}</h2>
                <p className="mt-2 text-sm text-slate-600">{summary.message || '系统状态暂不可用'}</p>
              </div>
              <StatusPill status={summaryStatus} label={summaryStatus} />
            </div>
            <p className="mt-4 text-xs text-slate-400">检查时间：{formatCheckedAt(summary.checkedAt)}</p>
          </section>

          {data.nextSteps.length > 0 && (
            <section className="rounded-lg border border-amber-200 bg-amber-50 p-4">
              <h2 className="text-sm font-semibold text-amber-800">需要关注</h2>
              <ul className="mt-2 space-y-1">
                {data.nextSteps.map((step, index) => (
                  <li key={`${step}-${index}`} className="text-sm text-amber-800">
                    {step}
                  </li>
                ))}
              </ul>
            </section>
          )}

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {data.components.map(component => (
              <ComponentCard key={component.key} component={component} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

import type { ReactNode } from 'react';

type MetricTone = 'primary' | 'activity' | 'warning' | 'danger' | 'neutral';

const valueClass: Record<MetricTone, string> = {
  primary: 'text-blue-600',
  activity: 'text-teal-600',
  warning: 'text-amber-600',
  danger: 'text-red-600',
  neutral: 'text-slate-950',
};

export default function MetricCard({
  label,
  value,
  helper,
  tone = 'neutral',
}: {
  label: string;
  value: ReactNode;
  helper?: ReactNode;
  tone?: MetricTone;
}) {
  return (
    <section className="pim-card p-4 min-w-0">
      <p className="text-xs text-slate-500 mb-2 truncate">{label}</p>
      <div className={`min-w-0 break-words text-xl font-semibold ${valueClass[tone]}`}>{value}</div>
      {helper && <p className="text-xs text-slate-400 mt-2 truncate">{helper}</p>}
    </section>
  );
}

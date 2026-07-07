import { useState } from 'react';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';

type ReportTab = 'overview' | 'sync' | 'confirmations' | 'exports';

const reportTabs: Array<{ value: ReportTab; label: string }> = [
  { value: 'overview', label: 'Overview' },
  { value: 'sync', label: 'Sync' },
  { value: 'confirmations', label: 'Confirmations' },
  { value: 'exports', label: 'Exports' },
];

export default function ReportsPage() {
  const [tab, setTab] = useState<ReportTab>('overview');
  const [range, setRange] = useState('7d');
  const [source, setSource] = useState('all');

  return (
    <div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
      <PageHeader
        title="Reports"
        subtitle="Operational reporting shell for sync health, confirmations, exports, and workbench history."
        actions={<SegmentedControl value={tab} options={reportTabs} onChange={setTab} ariaLabel="Report view" />}
      />

      <section className="pim-panel p-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <label>
            <span className="text-xs font-semibold text-slate-500">Range</span>
            <select value={range} onChange={event => setRange(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="24h">Last 24 hours</option>
              <option value="7d">Last 7 days</option>
              <option value="30d">Last 30 days</option>
            </select>
          </label>
          <label>
            <span className="text-xs font-semibold text-slate-500">Source</span>
            <select value={source} onChange={event => setSource(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="all">All sources</option>
              <option value="outlook">Outlook</option>
              <option value="pim">PIM</option>
              <option value="ai">AI</option>
            </select>
          </label>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {['Run count', 'Failure count', 'Pending reviews'].map(metric => (
          <section key={metric} className="pim-card p-4">
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">{metric}</p>
            <p className="mt-2 text-2xl font-semibold text-slate-950">0</p>
            <p className="mt-1 text-xs text-slate-500">No report data for {range} / {source}.</p>
          </section>
        ))}
      </div>

      <section className="pim-panel p-4">
        <h2 className="text-sm font-semibold text-slate-950">{tab} report</h2>
        <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-500">
          No report rows are available for the selected filters.
        </p>
      </section>
    </div>
  );
}

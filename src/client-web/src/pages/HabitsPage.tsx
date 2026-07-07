import { useState } from 'react';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';

type HabitTab = 'active' | 'planning' | 'archive';

const habitTabs: Array<{ value: HabitTab; label: string }> = [
  { value: 'active', label: 'Active' },
  { value: 'planning', label: 'Planning' },
  { value: 'archive', label: 'Archive' },
];

export default function HabitsPage() {
  const [tab, setTab] = useState<HabitTab>('active');
  const [cadence, setCadence] = useState('all');
  const [source, setSource] = useState('all');

  return (
    <div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
      <PageHeader
        title="Habits"
        subtitle="Habit planning shell for cadence filters, active routines, and schedule-layer readiness."
        actions={<SegmentedControl value={tab} options={habitTabs} onChange={setTab} ariaLabel="Habit view" />}
      />

      <section className="pim-panel p-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <label>
            <span className="text-xs font-semibold text-slate-500">Cadence</span>
            <select value={cadence} onChange={event => setCadence(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="all">All cadences</option>
              <option value="daily">Daily</option>
              <option value="weekly">Weekly</option>
              <option value="custom">Custom</option>
            </select>
          </label>
          <label>
            <span className="text-xs font-semibold text-slate-500">Source</span>
            <select value={source} onChange={event => setSource(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="all">All sources</option>
              <option value="manual">Manual</option>
              <option value="template">Template</option>
              <option value="ai">AI</option>
            </select>
          </label>
        </div>
      </section>

      <section className="pim-panel p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-slate-950">{tab} habits</h2>
          <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
            {cadence} / {source}
          </span>
        </div>
        <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-500">
          No habit records are available for the selected filters.
        </p>
      </section>

      <section className="pim-panel p-4">
        <h2 className="text-sm font-semibold text-slate-950">Schedule Layer Readiness</h2>
        <p className="mt-3 text-sm text-slate-500">
          Habit instances will flow into the calendar layer controls after habit data endpoints are available.
        </p>
      </section>
    </div>
  );
}

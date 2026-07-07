import { useState } from 'react';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';

type ReminderTab = 'due' | 'rules' | 'delivery';

const reminderTabs: Array<{ value: ReminderTab; label: string }> = [
  { value: 'due', label: 'Due' },
  { value: 'rules', label: 'Rules' },
  { value: 'delivery', label: 'Delivery' },
];

export default function RemindersPage() {
  const [tab, setTab] = useState<ReminderTab>('due');
  const [horizon, setHorizon] = useState('today');
  const [channel, setChannel] = useState('all');
  const [status, setStatus] = useState('open');

  return (
    <div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
      <PageHeader
        title="Reminders"
        subtitle="Operational reminder queue with filters for horizon, channel, and delivery status."
        actions={<SegmentedControl value={tab} options={reminderTabs} onChange={setTab} ariaLabel="Reminder view" />}
      />

      <section className="pim-panel p-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <label>
            <span className="text-xs font-semibold text-slate-500">Horizon</span>
            <select value={horizon} onChange={event => setHorizon(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="today">Today</option>
              <option value="week">Next 7 days</option>
              <option value="overdue">Overdue</option>
            </select>
          </label>
          <label>
            <span className="text-xs font-semibold text-slate-500">Channel</span>
            <select value={channel} onChange={event => setChannel(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="all">All channels</option>
              <option value="desktop">Desktop</option>
              <option value="email">Email</option>
              <option value="web">Web</option>
            </select>
          </label>
          <label>
            <span className="text-xs font-semibold text-slate-500">Status</span>
            <select value={status} onChange={event => setStatus(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
              <option value="open">Open</option>
              <option value="snoozed">Snoozed</option>
              <option value="sent">Sent</option>
            </select>
          </label>
        </div>
      </section>

      <section className="pim-panel p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-slate-950">{tab === 'due' ? 'Due Queue' : tab === 'rules' ? 'Reminder Rules' : 'Delivery Log'}</h2>
          <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
            {horizon} / {channel} / {status}
          </span>
        </div>
        <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-500">
          No reminder records are available for the current filters.
        </p>
      </section>
    </div>
  );
}

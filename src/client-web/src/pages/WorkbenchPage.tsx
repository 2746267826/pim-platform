import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  calendarApiPaths,
  getCalendarLayers,
  getOutlookSettings,
  getOutlookSyncBatches,
} from '../api/calendar';
import { getPendingConfirmations, operationsApiPaths } from '../api/operations';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';

type DensityMode = 'standard' | 'dense' | 'focus';

const densityOptions: Array<{ value: DensityMode; label: string }> = [
  { value: 'standard', label: 'Standard' },
  { value: 'dense', label: 'Dense' },
  { value: 'focus', label: 'Focus' },
];

const dashboardLayers = ['events', 'task-segments', 'habits', 'availability', 'ai-placeholders'];

function todayRange() {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(start.getDate() + 1);

  return {
    start: start.toISOString(),
    end: end.toISOString(),
  };
}

function formatDateTime(value?: string | null) {
  if (!value) return 'Not available';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function compactNumber(value: number | undefined) {
  return String(value ?? 0);
}

function DashboardMetric({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <section className="pim-card min-w-0 p-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-slate-950">{value}</p>
      <p className="mt-1 truncate text-xs text-slate-500">{detail}</p>
    </section>
  );
}

export default function WorkbenchPage() {
  const [densityMode, setDensityMode] = useState<DensityMode>('standard');
  const range = useMemo(todayRange, []);

  const { data: layerData, isLoading: layersLoading } = useQuery({
    queryKey: ['workbench-calendar-layers', range.start, range.end],
    queryFn: () => getCalendarLayers({ start: range.start, end: range.end, layers: dashboardLayers }),
    refetchInterval: 60_000,
  });

  const { data: confirmations = [], isLoading: confirmationsLoading } = useQuery({
    queryKey: ['workbench-pending-confirmations'],
    queryFn: getPendingConfirmations,
    refetchInterval: 30_000,
  });

  const { data: settings } = useQuery({
    queryKey: ['workbench-outlook-settings'],
    queryFn: getOutlookSettings,
    refetchInterval: 60_000,
  });

  const { data: syncBatches = [] } = useQuery({
    queryKey: ['workbench-outlook-sync-batches'],
    queryFn: getOutlookSyncBatches,
    refetchInterval: 45_000,
  });

  const layerCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const item of layerData?.items ?? []) {
      counts.set(item.layer, (counts.get(item.layer) ?? 0) + 1);
    }
    return counts;
  }, [layerData?.items]);

  const latestBatch = syncBatches[0];
  const compact = densityMode === 'dense';
  const focus = densityMode === 'focus';
  const pageSpacingClassName = compact ? 'space-y-3' : 'space-y-4';

  return (
    <div className={`mx-auto w-full max-w-[1500px] ${pageSpacingClassName} pb-8`}>
      <PageHeader
        title="Schedule Workbench"
        subtitle="Operational dashboard for calendar layers, confirmations, Outlook sync, reminders, and reporting."
        beforeActions={
          <SegmentedControl
            value={densityMode}
            options={densityOptions}
            onChange={setDensityMode}
            ariaLabel="Workbench density"
          />
        }
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Link to="/status" className="pim-button-secondary px-3 py-2 text-sm">
              Status
            </Link>
            <Link to="/data-center" className="pim-button-primary px-3 py-2 text-sm">
              Data Center
            </Link>
          </div>
        }
      />

      <section className={`grid grid-cols-1 gap-3 ${focus ? 'lg:grid-cols-3' : 'md:grid-cols-2 xl:grid-cols-4'}`}>
        <DashboardMetric
          label="Schedule layers"
          value={compactNumber(layerData?.items.length)}
          detail={layersLoading ? 'Loading layer index' : `${dashboardLayers.length} configured layers`}
        />
        <DashboardMetric
          label="Pending confirmations"
          value={compactNumber(confirmations.length)}
          detail={confirmationsLoading ? 'Loading operations queue' : 'Operations waiting for review'}
        />
        <DashboardMetric
          label="Outlook sync"
          value={settings?.status ?? 'Unknown'}
          detail={`Token: ${settings?.tokenHealth ?? 'Unknown'}`}
        />
        {!focus && (
          <DashboardMetric
            label="Last sync batch"
            value={latestBatch?.status ?? 'None'}
            detail={latestBatch ? formatDateTime(latestBatch.startedAt) : 'No sync batches returned'}
          />
        )}
      </section>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <section className="pim-panel min-w-0 p-4 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">Schedule Layers</h2>
              <p className="mt-1 text-xs text-slate-500">Today range: {formatDateTime(range.start)} to {formatDateTime(range.end)}</p>
            </div>
            <Link to="/calendar" className="pim-button-secondary px-3 py-1.5 text-sm">
              Open Calendar
            </Link>
          </div>
          <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {dashboardLayers.map(layer => (
              <div key={layer} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                <p className="truncate text-xs font-semibold text-slate-700">{layer}</p>
                <p className="mt-1 text-lg font-semibold text-slate-950">{layerCounts.get(layer) ?? 0}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">Pending Confirmations</h2>
            <Link to="/confirmations" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              Review all
            </Link>
          </div>
          <div className="mt-3 space-y-2">
            {confirmations.slice(0, compact ? 3 : 5).map(item => (
              <Link
                key={item.id}
                to="/confirmations"
                className="block rounded-lg border border-slate-200 bg-white px-3 py-2 transition-colors hover:border-blue-200 hover:bg-blue-50"
              >
                <div className="flex items-start justify-between gap-2">
                  <p className="min-w-0 truncate text-sm font-medium text-slate-800">{item.summary}</p>
                  <span className="shrink-0 rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-semibold text-amber-700">
                    {item.riskLevel}
                  </span>
                </div>
                <p className="mt-1 truncate text-xs text-slate-500">{item.source} / {item.operationType}</p>
              </Link>
            ))}
            {confirmations.length === 0 && (
              <p className="rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
                No pending confirmations.
              </p>
            )}
          </div>
        </section>
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">Outlook Sync</h2>
            <Link to="/sync" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              Configure
            </Link>
          </div>
          <dl className="mt-3 grid grid-cols-1 gap-2 text-sm">
            <div className="rounded-lg bg-slate-50 px-3 py-2">
              <dt className="text-xs text-slate-400">Provider</dt>
              <dd className="font-medium text-slate-800">{settings?.provider ?? 'outlook'}</dd>
            </div>
            <div className="rounded-lg bg-slate-50 px-3 py-2">
              <dt className="text-xs text-slate-400">Last synced</dt>
              <dd className="font-medium text-slate-800">{formatDateTime(settings?.lastSyncedAt)}</dd>
            </div>
            {settings?.lastError && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-red-700">
                <dt className="text-xs font-semibold">Last error</dt>
                <dd className="mt-1 text-sm">{settings.lastError}</dd>
              </div>
            )}
          </dl>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">Reminders</h2>
            <Link to="/reminders" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              Open
            </Link>
          </div>
          <p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
            Reminder rules and delivery queues will appear here when configured.
          </p>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">Reports</h2>
            <Link to="/reports" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              Open
            </Link>
          </div>
          <p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
            Operational exports and report runs will appear here when data is available.
          </p>
        </section>
      </div>

      <section className="pim-panel p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold text-slate-950">Endpoints And Status Links</h2>
            <p className="mt-1 text-xs text-slate-500">Current shell links to the API contracts used by this dashboard.</p>
          </div>
          <Link to="/status" className="pim-button-secondary px-3 py-1.5 text-sm">
            System Status
          </Link>
        </div>
        <div className="mt-3 grid grid-cols-1 gap-2 md:grid-cols-2 xl:grid-cols-4">
          {[
            ['Calendar layers', calendarApiPaths.calendarLayers({ start: range.start, end: range.end, layers: dashboardLayers })],
            ['Outlook settings', calendarApiPaths.outlookSettings()],
            ['Sync batches', calendarApiPaths.outlookSyncBatches()],
            ['Pending confirmations', operationsApiPaths.pendingConfirmations()],
          ].map(([label, endpoint]) => (
            <div key={label} className="min-w-0 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <p className="text-xs font-semibold text-slate-600">{label}</p>
              <code className="mt-1 block truncate text-[11px] text-slate-500">{endpoint}</code>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { queryDataCenter } from '../api/calendar';
import type { DataCenterItem } from '../types';
import PageHeader from '../ui/PageHeader';

function formatDateTime(value?: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

export default function DataCenterPage() {
  const [search, setSearch] = useState('');
  const [objectType, setObjectType] = useState('');
  const [source, setSource] = useState('');
  const [pendingOnly, setPendingOnly] = useState(false);
  const [selectedObjectId, setSelectedObjectId] = useState<string | null>(null);

  const request = useMemo(() => ({
    search: search.trim() || null,
    objectType: objectType || null,
    source: source || null,
    pendingOnly,
    page: 1,
    pageSize: 50,
  }), [objectType, pendingOnly, search, source]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['data-center-query', request],
    queryFn: () => queryDataCenter(request),
  });

  const items = data?.items ?? [];
  const selected = items.find(item => item.objectId === selectedObjectId) ?? items[0];

  function selectRow(item: DataCenterItem) {
    setSelectedObjectId(item.objectId);
  }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
      <PageHeader
        title="Data Center"
        subtitle="Search operational schedule objects by source, status, and pending state."
      />

      <section className="pim-panel p-4">
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(220px,1fr)_180px_180px_auto]">
          <label className="min-w-0">
            <span className="text-xs font-semibold text-slate-500">Search</span>
            <input
              value={search}
              onChange={event => setSearch(event.target.value)}
              placeholder="Title, summary, source object"
              className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400"
            />
          </label>
          <label>
            <span className="text-xs font-semibold text-slate-500">Object</span>
            <select
              value={objectType}
              onChange={event => setObjectType(event.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400"
            >
              <option value="">All objects</option>
              <option value="event">Events</option>
              <option value="task">Tasks</option>
              <option value="task-segment">Task segments</option>
              <option value="habit">Habits</option>
            </select>
          </label>
          <label>
            <span className="text-xs font-semibold text-slate-500">Source</span>
            <select
              value={source}
              onChange={event => setSource(event.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400"
            >
              <option value="">All sources</option>
              <option value="pim">PIM</option>
              <option value="outlook">Outlook</option>
              <option value="manual">Manual</option>
              <option value="ai">AI</option>
            </select>
          </label>
          <label className="flex items-end gap-2 pb-2 text-sm font-medium text-slate-700">
            <input
              type="checkbox"
              checked={pendingOnly}
              onChange={event => setPendingOnly(event.target.checked)}
              className="h-4 w-4 rounded border-slate-300"
            />
            Pending only
          </label>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
        <section className="pim-panel min-w-0 overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
            <h2 className="text-sm font-semibold text-slate-950">Results</h2>
            <span className="text-xs text-slate-500">{data?.totalCount ?? 0} objects</span>
          </div>

          {isLoading ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">Loading data center objects.</p>
          ) : isError ? (
            <p className="px-4 py-8 text-center text-sm text-red-600">
              {error instanceof Error ? error.message : 'Data center query failed'}
            </p>
          ) : items.length === 0 ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">No objects match the current filters.</p>
          ) : (
            <div className="overflow-auto">
              <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-[0.12em] text-slate-400">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Title</th>
                    <th className="px-4 py-3 font-semibold">Object</th>
                    <th className="px-4 py-3 font-semibold">Source</th>
                    <th className="px-4 py-3 font-semibold">Status</th>
                    <th className="px-4 py-3 font-semibold">Start</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {items.map(item => (
                    <tr
                      key={`${item.objectType}-${item.objectId}`}
                      onClick={() => selectRow(item)}
                      className={`cursor-pointer transition-colors hover:bg-blue-50 ${
                        selected?.objectId === item.objectId ? 'bg-blue-50' : 'bg-white'
                      }`}
                    >
                      <td className="max-w-[280px] px-4 py-3">
                        <p className="truncate font-medium text-slate-800">{item.title}</p>
                        <p className="mt-1 truncate text-xs text-slate-500">{item.summary}</p>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{item.objectType}</td>
                      <td className="px-4 py-3 text-slate-600">{item.source}</td>
                      <td className="px-4 py-3 text-slate-600">{item.status}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-500">{formatDateTime(item.startsAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="pim-panel min-w-0 p-4">
          <h2 className="text-sm font-semibold text-slate-950">Selected Batch Preview</h2>
          {selected ? (
            <dl className="mt-4 space-y-2 text-sm">
              {[
                ['Title', selected.title],
                ['Object ID', selected.objectId],
                ['Object type', selected.objectType],
                ['Source', selected.source],
                ['Status', selected.status],
                ['Starts', formatDateTime(selected.startsAt)],
                ['Ends', formatDateTime(selected.endsAt)],
                ['Summary', selected.summary],
              ].map(([label, value]) => (
                <div key={label} className="rounded-lg bg-slate-50 px-3 py-2">
                  <dt className="text-xs font-semibold text-slate-400">{label}</dt>
                  <dd className="mt-1 break-words text-slate-800">{value}</dd>
                </div>
              ))}
            </dl>
          ) : (
            <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-8 text-center text-sm text-slate-500">
              Select a row to inspect its batch preview.
            </p>
          )}
        </section>
      </div>
    </div>
  );
}

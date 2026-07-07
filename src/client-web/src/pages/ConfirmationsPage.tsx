import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  confirmOperation,
  getConfirmationDetail,
  getPendingConfirmations,
  rejectOperation,
} from '../api/operations';
import PageHeader from '../ui/PageHeader';

function formatDateTime(value?: string | null) {
  if (!value) return 'Not available';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function actionError(error: unknown) {
  return error instanceof Error ? error.message : 'Operation failed';
}

export default function ConfirmationsPage() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const { data: confirmations = [], isLoading } = useQuery({
    queryKey: ['pending-confirmations'],
    queryFn: getPendingConfirmations,
    refetchInterval: 30_000,
  });

  useEffect(() => {
    if (!selectedId && confirmations[0]) {
      setSelectedId(confirmations[0].id);
    }
  }, [confirmations, selectedId]);

  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ['confirmation-detail', selectedId],
    queryFn: () => getConfirmationDetail(selectedId!),
    enabled: selectedId !== null,
  });

  const confirmMutation = useMutation({
    mutationFn: confirmOperation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-confirmations'] });
      queryClient.invalidateQueries({ queryKey: ['workbench-pending-confirmations'] });
      queryClient.invalidateQueries({ queryKey: ['confirmation-detail'] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: rejectOperation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-confirmations'] });
      queryClient.invalidateQueries({ queryKey: ['workbench-pending-confirmations'] });
      queryClient.invalidateQueries({ queryKey: ['confirmation-detail'] });
    },
  });

  const active = detail ?? confirmations.find(item => item.id === selectedId);
  const busy = confirmMutation.isPending || rejectMutation.isPending;

  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-4 pb-8">
      <PageHeader
        title="Confirmations"
        subtitle="Review pending operations before schedule, sync, or data-center changes are executed."
      />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(320px,0.9fr)_minmax(0,1.6fr)]">
        <section className="pim-panel min-w-0 overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
            <h2 className="text-sm font-semibold text-slate-950">Pending List</h2>
            <span className="text-xs text-slate-500">{confirmations.length} pending</span>
          </div>
          {isLoading ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">Loading confirmations.</p>
          ) : confirmations.length === 0 ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">No pending confirmations.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {confirmations.map(item => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setSelectedId(item.id)}
                  className={`block w-full px-4 py-3 text-left transition-colors hover:bg-blue-50 ${
                    selectedId === item.id ? 'bg-blue-50' : 'bg-white'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <p className="min-w-0 truncate text-sm font-semibold text-slate-800">{item.summary}</p>
                    <span className="shrink-0 rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-semibold text-amber-700">
                      {item.riskLevel}
                    </span>
                  </div>
                  <p className="mt-1 truncate text-xs text-slate-500">{item.source} / {item.operationType}</p>
                  <p className="mt-1 text-[11px] text-slate-400">Expires {formatDateTime(item.expiresAt)}</p>
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">Detail Panel</h2>
              <p className="mt-1 text-xs text-slate-500">
                {detailLoading ? 'Loading selected operation' : active?.id ?? 'No operation selected'}
              </p>
            </div>
            {active && (
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => confirmMutation.mutate(active.id)}
                  disabled={busy}
                  className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
                >
                  Confirm
                </button>
                <button
                  type="button"
                  onClick={() => rejectMutation.mutate(active.id)}
                  disabled={busy}
                  className="pim-button-secondary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
                >
                  Reject
                </button>
              </div>
            )}
          </div>

          {active ? (
            <div className="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-2">
              <div className="rounded-lg border border-slate-200 bg-white p-3 lg:col-span-2">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Summary</p>
                <p className="mt-2 text-sm font-medium text-slate-800">{active.summary}</p>
              </div>
              {[
                ['Risk', active.riskLevel],
                ['Source', active.source],
                ['Operation type', active.operationType],
                ['Status', active.status],
                ['Object type', active.objectType ?? 'Not available'],
                ['Object ID', active.objectId ?? 'Not available'],
                ['Correlation ID', active.correlationId ?? 'Not available'],
                ['Second-level marker', active.requiresSecondLevelConfirmation ? 'Required' : 'Not required'],
              ].map(([label, value]) => (
                <div key={label} className="rounded-lg bg-slate-50 px-3 py-2">
                  <p className="text-xs font-semibold text-slate-400">{label}</p>
                  <p className="mt-1 break-words text-sm text-slate-800">{value}</p>
                </div>
              ))}
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <p className="text-xs font-semibold text-slate-400">Changed fields</p>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {(active.changedFields ?? []).length > 0 ? (
                    active.changedFields?.map(field => (
                      <span key={field} className="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
                        {field}
                      </span>
                    ))
                  ) : (
                    <span className="text-sm text-slate-500">No changed fields reported.</span>
                  )}
                </div>
              </div>
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <p className="text-xs font-semibold text-slate-400">Allowed actions</p>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {(active.allowedActions ?? []).length > 0 ? (
                    active.allowedActions?.map(action => (
                      <span key={action} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700">
                        {action}
                      </span>
                    ))
                  ) : (
                    <span className="text-sm text-slate-500">Confirm and reject controls are available for pending items.</span>
                  )}
                </div>
              </div>
              <div className="rounded-lg border border-slate-200 bg-white p-3 lg:col-span-2">
                <p className="text-xs font-semibold text-slate-400">Preview JSON</p>
                <pre className="mt-2 max-h-64 overflow-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-100">
                  {active.previewJson || '{}'}
                </pre>
              </div>
            </div>
          ) : (
            <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-500">
              Select a pending confirmation to inspect the requested changes.
            </p>
          )}

          {(confirmMutation.isError || rejectMutation.isError) && (
            <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {actionError(confirmMutation.error ?? rejectMutation.error)}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}

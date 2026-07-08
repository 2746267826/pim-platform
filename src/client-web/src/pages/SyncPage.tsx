import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createOutlookDeviceCode,
  getOutlookSettings,
  getOutlookSyncBatches,
  queryDataCenter,
  runOutlookSync,
  updateOutlookSettings,
} from '../api/calendar';
import OutlookConflictResolver from '../components/schedule/OutlookConflictResolver';
import type { UpdateOutlookSettingsRequest } from '../types';
import PageHeader from '../ui/PageHeader';

function formatDateTime(value?: string | null) {
  if (!value) return 'Not available';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function mutationError(error: unknown) {
  return error instanceof Error ? error.message : '请求失败';
}

export const outlookSyncInvalidationKeys = [
  ['outlook-settings'],
  ['workbench-outlook-settings'],
  ['outlook-sync-batches'],
  ['workbench-outlook-sync-batches'],
  ['today-outlook-sync-batches'],
  ['pending-confirmations'],
  ['workbench-pending-confirmations'],
  ['today-pending-confirmations'],
  ['workbench-calendar-layers'],
  ['calendar-layers'],
  ['today-sections'],
  ['today-section'],
  ['data-center-query'],
] as const;

export default function SyncPage() {
  const queryClient = useQueryClient();
  const [tenantId, setTenantId] = useState('common');
  const [clientId, setClientId] = useState('');
  const [scopes, setScopes] = useState('Calendars.ReadWrite offline_access');

  const { data: settings, isLoading: settingsLoading } = useQuery({
    queryKey: ['outlook-settings'],
    queryFn: getOutlookSettings,
  });

  const { data: syncBatches = [], isLoading: batchesLoading } = useQuery({
    queryKey: ['outlook-sync-batches'],
    queryFn: getOutlookSyncBatches,
    refetchInterval: 45_000,
  });

  const { data: conflictData } = useQuery({
    queryKey: ['sync-conflicts', 'outlook'],
    queryFn: () => queryDataCenter({
      search: null,
      objectType: 'sync-conflict',
      source: 'outlook',
      pendingOnly: false,
      page: 1,
      pageSize: 25,
    }),
    refetchInterval: 45_000,
  });

  const conflictItems = conflictData?.items ?? [];

  useEffect(() => {
    if (!settings) return;
    setTenantId(settings.tenantId || 'common');
    setClientId(settings.clientId ?? '');
    setScopes(settings.scopes || 'Calendars.ReadWrite offline_access');
  }, [settings]);

  const settingsMutation = useMutation({
    mutationFn: (data: UpdateOutlookSettingsRequest) => updateOutlookSettings(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['outlook-settings'] });
      queryClient.invalidateQueries({ queryKey: ['workbench-outlook-settings'] });
    },
  });

  const deviceCodeMutation = useMutation({
    mutationFn: createOutlookDeviceCode,
  });

  const syncMutation = useMutation({
    mutationFn: runOutlookSync,
    onSuccess: () => {
      for (const queryKey of outlookSyncInvalidationKeys) {
        queryClient.invalidateQueries({ queryKey });
      }
    },
  });

  function submitSettings(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    settingsMutation.mutate({
      tenantId: tenantId.trim() || 'common',
      clientId: clientId.trim() || null,
      scopes: scopes.trim(),
    });
  }

  return (
    <div className="mx-auto w-full max-w-[1300px] space-y-4 pb-8">
      <PageHeader
        title="微软同步"
        subtitle="配置设备代码连接、令牌健康、增量状态、回写默认值与冲突治理。"
        actions={
          <button
            type="button"
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            {syncMutation.isPending ? '同步中' : '运行同步'}
          </button>
        }
      />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <section className="pim-panel min-w-0 p-4 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">同步设置</h2>
              <p className="mt-1 text-xs text-slate-500">
                状态：{settingsLoading ? '加载中' : settings?.status ?? '未知'} / tokenHealth：{settings?.tokenHealth ?? '未知'}
              </p>
            </div>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
              来源：Outlook
            </span>
          </div>

          <form onSubmit={submitSettings} className="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-3">
            <label className="min-w-0 text-sm">
              <span className="text-xs font-semibold text-slate-500">Tenant</span>
              <input
                value={tenantId}
                onChange={event => setTenantId(event.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-400"
                placeholder="common"
              />
            </label>
            <label className="min-w-0 text-sm">
              <span className="text-xs font-semibold text-slate-500">Client ID</span>
              <input
                value={clientId}
                onChange={event => setClientId(event.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-400"
                placeholder="Azure app client ID"
              />
            </label>
            <label className="min-w-0 text-sm">
              <span className="text-xs font-semibold text-slate-500">Scopes</span>
              <input
                value={scopes}
                onChange={event => setScopes(event.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-400"
                placeholder="Calendars.ReadWrite offline_access"
              />
            </label>
            <div className="flex flex-wrap items-center gap-2 lg:col-span-3">
              <button
                type="submit"
                disabled={settingsMutation.isPending}
                className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
              >
                {settingsMutation.isPending ? '保存中' : '保存设置'}
              </button>
              <p className="text-xs text-slate-500">上次同步：{formatDateTime(settings?.lastSyncedAt)}</p>
            </div>
            {settingsMutation.isError && (
              <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 lg:col-span-3">
                {mutationError(settingsMutation.error)}
              </p>
            )}
          </form>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">设备代码</h2>
            <button
              type="button"
              onClick={() => deviceCodeMutation.mutate()}
              disabled={deviceCodeMutation.isPending}
              className="pim-button-secondary px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-60"
            >
              请求代码
            </button>
          </div>
          {deviceCodeMutation.data ? (
            <div className="mt-4 space-y-3">
              <a
                href={deviceCodeMutation.data.verificationUri}
                target="_blank"
                rel="noreferrer"
                className="block rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-100"
              >
                打开微软验证链接
              </a>
              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                <p className="text-xs font-semibold text-slate-500">用户代码</p>
                <p className="mt-1 font-mono text-xl font-semibold tracking-[0.18em] text-slate-950">
                  {deviceCodeMutation.data.userCode}
                </p>
              </div>
              <p className="text-xs leading-5 text-slate-500">{deviceCodeMutation.data.message}</p>
              <p className="text-xs text-slate-400">过期时间：{formatDateTime(deviceCodeMutation.data.expiresAt)}</p>
            </div>
          ) : (
            <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
              请求设备代码以连接微软账号。
            </p>
          )}
          {deviceCodeMutation.isError && (
            <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {mutationError(deviceCodeMutation.error)}
            </p>
          )}
        </section>
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
        {[
          ['令牌健康', settings?.tokenHealth ?? '未知'],
          ['deltaLink 增量状态', settings?.deltaLink ?? '未记录'],
          ['writeback 回写默认值', settings?.writebackDefault ?? '需要确认'],
          ['冲突策略', settings?.conflictPolicy ?? '人工复核'],
        ].map(([label, value]) => (
          <section key={label} className="pim-card p-4">
            <p className="text-[11px] font-semibold text-slate-400">{label}</p>
            <p className="mt-2 truncate text-sm font-semibold text-slate-900">{value}</p>
          </section>
        ))}
      </div>

      <OutlookConflictResolver conflicts={conflictItems} />

      <section className="pim-panel min-w-0 overflow-hidden p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-semibold text-slate-950">同步批次</h2>
            <p className="mt-1 text-xs text-slate-500">同步窗口、来源标签、步骤计数与错误轨迹。</p>
          </div>
          {syncMutation.isError && (
            <span className="rounded-full bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700">
              {mutationError(syncMutation.error)}
            </span>
          )}
        </div>

        {batchesLoading ? (
          <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
            正在加载同步批次。
          </p>
        ) : syncBatches.length === 0 ? (
          <p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
            暂无微软同步批次。
          </p>
        ) : (
          <div className="mt-4 space-y-3">
            {syncBatches.map(batch => (
              <article key={batch.id} className="rounded-lg border border-slate-200 bg-white p-3">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-slate-950">{batch.status}</p>
                    <p className="mt-1 text-xs text-slate-500">来源标签：{batch.provider} / 开始：{formatDateTime(batch.startedAt)}</p>
                  </div>
                  <div className="flex flex-wrap gap-1.5 text-[11px] font-semibold">
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-slate-600">读取 {batch.readCount}</span>
                    <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-emerald-700">创建 {batch.createdCount}</span>
                    <span className="rounded-full bg-blue-50 px-2 py-0.5 text-blue-700">更新 {batch.updatedCount}</span>
                    <span className="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">冲突 {batch.conflictCount}</span>
                    <span className="rounded-full bg-red-50 px-2 py-0.5 text-red-700">错误 {batch.failureCount}</span>
                  </div>
                </div>
                {batch.errorSummary && (
                  <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{batch.errorSummary}</p>
                )}
                <div className="mt-3 grid grid-cols-1 gap-2 lg:grid-cols-2">
                  {batch.steps.map(step => (
                    <div key={`${batch.id}-${step.name}-${step.at}`} className="rounded-lg bg-slate-50 px-3 py-2">
                      <div className="flex items-center justify-between gap-2">
                        <p className="truncate text-xs font-semibold text-slate-700">{step.name}</p>
                        <span className="shrink-0 text-[11px] font-semibold text-slate-500">{step.status}</span>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">{step.detail}</p>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

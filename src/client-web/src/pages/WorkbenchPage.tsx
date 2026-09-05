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
import { getDeferredAutoRefreshInterval } from '../lib/autoRefresh';

type DensityMode = 'standard' | 'dense' | 'focus';
type WorkbenchView = 'schedule' | 'execute' | 'feedback';

const densityOptions: Array<{ value: DensityMode; label: string }> = [
  { value: 'standard', label: '标准' },
  { value: 'dense', label: '紧凑' },
  { value: 'focus', label: '专注' },
];

const workbenchViewOptions: Array<{ value: WorkbenchView; label: string }> = [
  { value: 'schedule', label: '排程' },
  { value: 'execute', label: '执行' },
  { value: 'feedback', label: '反馈' },
];

const dashboardLayers = ['events', 'task-segments', 'habits', 'availability', 'ai-placeholders'];
const layerLabels: Record<string, string> = {
  events: '日程事件',
  'task-segments': '任务时间段',
  habits: '习惯',
  availability: '可用时间',
  'ai-placeholders': '智能占位',
};
const statusLabels: Record<string, string> = {
  Unknown: '未知',
  None: '无',
  Healthy: '正常',
  Warning: '警告',
  Failed: '失败',
  missing: '缺失',
  healthy: '正常',
  connected: '已连接',
  'not-connected': '未连接',
  pending: '等待中',
  completed: '已完成',
  failed: '失败',
};

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
  if (!value) return '不可用';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN');
}

function formatStatus(value?: string | null) {
  if (!value) return '未知';
  return statusLabels[value] ?? value;
}

function formatProvider(value?: string | null) {
  if (!value) return '微软日历';
  return value.toLowerCase() === 'outlook' ? '微软日历' : value;
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
  const [workbenchView, setWorkbenchView] = useState<WorkbenchView>('schedule');
  const range = useMemo(todayRange, []);

  const { data: layerData, isLoading: layersLoading } = useQuery({
    queryKey: ['workbench-calendar-layers', range.start, range.end],
    queryFn: () => getCalendarLayers({ start: range.start, end: range.end, layers: dashboardLayers }),
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const { data: confirmations = [], isLoading: confirmationsLoading } = useQuery({
    queryKey: ['workbench-pending-confirmations'],
    queryFn: getPendingConfirmations,
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const { data: settings } = useQuery({
    queryKey: ['workbench-outlook-settings'],
    queryFn: getOutlookSettings,
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const { data: syncBatches = [] } = useQuery({
    queryKey: ['workbench-outlook-sync-batches'],
    queryFn: getOutlookSyncBatches,
    refetchInterval: getDeferredAutoRefreshInterval,
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
    <div className={`mx-auto w-full max-w-[1500px] ${pageSpacingClassName} pb-20`}>
      <PageHeader
        title="日程工作台"
        subtitle="集中查看日程图层、确认队列、微软日历同步、提醒和报告运行状态。"
        beforeActions={
          <SegmentedControl
            value={densityMode}
            options={densityOptions}
            onChange={setDensityMode}
            ariaLabel="工作台密度"
          />
        }
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Link to="/status" className="pim-button-secondary inline-flex min-h-[44px] items-center px-3 py-2 text-sm">
              状态
            </Link>
            <Link to="/data-center" className="pim-button-primary inline-flex min-h-[44px] items-center px-3 py-2 text-sm">
              数据中心
            </Link>
          </div>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <SegmentedControl
          value={workbenchView}
          options={workbenchViewOptions}
          onChange={setWorkbenchView}
          ariaLabel="工作台视图"
        />
      </div>

      <section className={`grid grid-cols-1 gap-3 items-start ${focus ? 'lg:grid-cols-3' : 'md:grid-cols-2 xl:grid-cols-4'}`}>
        <DashboardMetric
          label="日程图层"
          value={compactNumber(layerData?.items.length)}
          detail={layersLoading ? '正在加载图层索引' : `${dashboardLayers.length} 个图层已配置`}
        />
        <DashboardMetric
          label="待确认操作"
          value={compactNumber(confirmations.length)}
          detail={confirmationsLoading ? '正在加载确认队列' : '等待复核的操作'}
        />
        <DashboardMetric
          label="微软日历同步"
          value={formatStatus(settings?.status)}
          detail={`令牌：${formatStatus(settings?.tokenHealth)}`}
        />
        {!focus && (
          <DashboardMetric
            label="最近同步批次"
            value={formatStatus(latestBatch?.status)}
            detail={latestBatch ? formatDateTime(latestBatch.startedAt) : '暂无同步批次'}
          />
        )}
      </section>

      <div className="grid grid-cols-1 gap-4 items-start lg:grid-cols-2 xl:grid-cols-3">
        <section className="pim-panel min-w-0 p-4 xl:col-span-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">日程图层</h2>
              <p className="mt-1 text-xs text-slate-500">今日范围：{formatDateTime(range.start)} 至 {formatDateTime(range.end)}</p>
            </div>
            <Link to="/calendar" className="pim-button-secondary inline-flex min-h-[44px] items-center px-3 py-1.5 text-sm">
              打开日历
            </Link>
          </div>
          <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {dashboardLayers.map(layer => (
              <div key={layer} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                <p className="truncate text-xs font-semibold text-slate-700">{layerLabels[layer]}</p>
                <p className="mt-1 text-lg font-semibold text-slate-950">{layerCounts.get(layer) ?? 0}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">待确认操作</h2>
            <Link to="/confirmations" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              查看全部
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
                暂无待确认操作。
              </p>
            )}
          </div>
        </section>
      </div>

      <div className="grid grid-cols-1 gap-4 items-start lg:grid-cols-2 xl:grid-cols-3">
        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">微软日历同步</h2>
            <Link to="/settings/sync" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              配置
            </Link>
          </div>
          <dl className="mt-3 grid grid-cols-1 gap-2 text-sm">
            <div className="rounded-lg bg-slate-50 px-3 py-2">
              <dt className="text-xs text-slate-400">提供方</dt>
              <dd className="font-medium text-slate-800">{formatProvider(settings?.provider)}</dd>
            </div>
            <div className="rounded-lg bg-slate-50 px-3 py-2">
              <dt className="text-xs text-slate-400">最近同步</dt>
              <dd className="font-medium text-slate-800">{formatDateTime(settings?.lastSyncedAt)}</dd>
            </div>
            {settings?.lastError && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-red-700">
                <dt className="text-xs font-semibold">最近错误</dt>
                <dd className="mt-1 text-sm">{settings.lastError}</dd>
              </div>
            )}
          </dl>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">提醒</h2>
            <Link to="/reminders" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              打开
            </Link>
          </div>
          <p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
            配置提醒规则后，这里会显示触发规则和发送队列。
          </p>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-950">报告</h2>
            <Link to="/reports" className="text-xs font-semibold text-blue-600 hover:text-blue-700">
              打开
            </Link>
          </div>
          <p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
            有报告数据后，这里会显示导出记录和报告运行情况。
          </p>
        </section>
      </div>

      <section className="pim-panel p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold text-slate-950">端点与状态链接</h2>
            <p className="mt-1 text-xs text-slate-500">当前界面使用的接口契约与状态入口。</p>
          </div>
          <Link to="/status" className="pim-button-secondary inline-flex min-h-[44px] items-center px-3 py-1.5 text-sm">
            系统状态
          </Link>
        </div>
        <div className="mt-3 overflow-x-auto">
          <div className="grid min-w-[640px] grid-cols-1 gap-2 md:grid-cols-2 xl:grid-cols-4">
          {[
            ['日程图层', calendarApiPaths.calendarLayers({ start: range.start, end: range.end, layers: dashboardLayers })],
            ['微软日历设置', calendarApiPaths.outlookSettings()],
            ['同步批次', calendarApiPaths.outlookSyncBatches()],
            ['待确认操作', operationsApiPaths.pendingConfirmations()],
          ].map(([label, endpoint]) => (
            <div key={label} className="min-w-0 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <p className="text-xs font-semibold text-slate-600">{label}</p>
              <code className="mt-1 block truncate text-[11px] text-slate-500">{endpoint}</code>
            </div>
          ))}
          </div>
        </div>
      </section>
    </div>
  );
}

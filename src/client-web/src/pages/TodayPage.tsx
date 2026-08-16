import { useEffect, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { getOutlookSyncBatches } from '../api/calendar';
import { getPendingConfirmations } from '../api/operations';
import { getTodaySectionRegistry } from '../api/today';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import PageHeader from '../ui/PageHeader';
import EmptyState from '../ui/EmptyState';
import SegmentedControl from '../ui/SegmentedControl';
import TodaySectionHost, {
  isKnownTodaySectionKind,
  todaySectionOrder,
} from '../components/today/TodaySectionHost';
import type { ScheduledItem } from '../components/today/TodayScheduleList';
import type { EventResponse, TaskResponse, TodaySectionKind, TodaySectionRegistryItem } from '../types';
import { getDeferredAutoRefreshInterval } from '../lib/autoRefresh';

type DensityMode = 'standard' | 'dense' | 'focus';

const densityModeOptions: Array<{ value: DensityMode; label: string }> = [
  { value: 'standard', label: '标准' },
  { value: 'dense', label: '高密度' },
  { value: 'focus', label: '专注' },
];

function useTodayDate() {
  const [today, setToday] = useState(() => new Date());

  useEffect(() => {
    const now = new Date();
    const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const delayMs = nextMidnight.getTime() - now.getTime() + 1000;
    const timerId = window.setTimeout(() => setToday(new Date()), delayMs);

    return () => window.clearTimeout(timerId);
  }, [today]);

  return today;
}

function errorMessage(error: Error | null) {
  return error?.message || '请稍后重试。';
}

function formatDateTime(value?: string | null) {
  if (!value) return 'Not available';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function RegistryErrorPanel({ error }: { error: Error | null }) {
  if (!error) return null;

  return (
    <section className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
      <p className="font-medium">今日区块加载失败</p>
      <p className="mt-1 text-xs leading-5">{errorMessage(error)}</p>
    </section>
  );
}

function sortSections(sections: TodaySectionRegistryItem[]) {
  const orderIndex = new Map<TodaySectionKind, number>(
    todaySectionOrder.map((kind, index) => [kind, index]),
  );

  return [...sections].sort((a, b) => {
    const aIndex = isKnownTodaySectionKind(a.kind)
      ? orderIndex.get(a.kind as TodaySectionKind)!
      : Number.POSITIVE_INFINITY;
    const bIndex = isKnownTodaySectionKind(b.kind)
      ? orderIndex.get(b.kind as TodaySectionKind)!
      : Number.POSITIVE_INFINITY;
    return aIndex - bIndex;
  });
}

export default function TodayPage() {
  const today = useTodayDate();
  const dateStr = format(today, 'yyyy-MM-dd');
  const queryClient = useQueryClient();
  const [taskEditorOpen, setTaskEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();
  const [eventEditorOpen, setEventEditorOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventResponse | undefined>();
  const [densityMode, setDensityMode] = useState<DensityMode>('standard');

  const {
    data: registry,
    error: registryError,
    isLoading: registryLoading,
  } = useQuery({
    queryKey: ['today-sections', dateStr],
    queryFn: () => getTodaySectionRegistry(dateStr),
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const { data: pendingConfirmations = [] } = useQuery({
    queryKey: ['today-pending-confirmations'],
    queryFn: getPendingConfirmations,
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const { data: outlookSyncBatches = [] } = useQuery({
    queryKey: ['today-outlook-sync-batches'],
    queryFn: getOutlookSyncBatches,
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  const sections = useMemo(() => sortSections(registry?.sections ?? []), [registry?.sections]);
  const compactItemLimit = densityMode === 'dense' ? 2 : 3;
  const sectionGridClassName = densityMode === 'focus'
    ? 'grid grid-cols-1 gap-4 xl:grid-cols-3'
    : densityMode === 'dense'
      ? 'grid grid-cols-1 gap-3 xl:grid-cols-4'
      : 'grid grid-cols-1 gap-4 xl:grid-cols-4';

  function openTask(task: TaskResponse) {
    setEditingTask(task);
    setTaskEditorOpen(true);
  }

  function openScheduledItem(item: ScheduledItem) {
    if (item.type === 'task') {
      openTask(item.task);
      return;
    }

    setEditingEvent(item.event);
    setEventEditorOpen(true);
  }

  function closeEventEditor() {
    setEventEditorOpen(false);
    setEditingEvent(undefined);
    queryClient.invalidateQueries({ queryKey: ['today-sections'] });
    queryClient.invalidateQueries({ queryKey: ['today-section'] });
  }

  return (
    <div className="mx-auto max-w-[1500px] space-y-4 pb-8">
      <PageHeader
        title="日程任务工作台"
        subtitle={`${dateStr} · 日程承诺、任务执行、提醒队列与报告`}
        beforeActions={
          <SegmentedControl
            value={densityMode}
            options={densityModeOptions}
            onChange={setDensityMode}
            ariaLabel="今日密度"
          />
        }
        actions={
          <button
            type="button"
            onClick={() => {
              setEditingTask(undefined);
              setTaskEditorOpen(true);
            }}
            className="pim-button-primary px-4 py-2 text-sm"
          >
            新建任务
          </button>
        }
      />

      <RegistryErrorPanel error={registryError} />

      <div className="grid grid-cols-1 gap-3 xl:grid-cols-4">
        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">待确认</h2>
              <p className="mt-1 text-xs text-slate-500">{pendingConfirmations.length} 个操作等待复核</p>
            </div>
            <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
              {densityMode}
            </span>
          </div>
          <div className="mt-3 space-y-2">
            {pendingConfirmations.slice(0, compactItemLimit).map(item => (
              <div key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                <div className="flex items-start justify-between gap-2">
                  <p className="min-w-0 truncate text-sm font-medium text-slate-800">{item.summary}</p>
                  <span className="shrink-0 text-[11px] font-semibold text-slate-500">{item.riskLevel}</span>
                </div>
                {densityMode !== 'focus' && (
                  <p className="mt-1 truncate text-xs text-slate-500">{item.source} / {item.operationType}</p>
                )}
              </div>
            ))}
            {pendingConfirmations.length === 0 && (
              <p className="rounded-lg border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">
                暂无待确认操作。
              </p>
            )}
          </div>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">微软同步</h2>
              <p className="mt-1 text-xs text-slate-500">{outlookSyncBatches.length} 个最近批次</p>
            </div>
            <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">同步</span>
          </div>
          <div className="mt-3 space-y-2">
            {outlookSyncBatches.slice(0, compactItemLimit).map(batch => (
              <div key={batch.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                <div className="flex items-start justify-between gap-2">
                  <p className="min-w-0 truncate text-sm font-medium text-slate-800">{batch.status}</p>
                  <span className="shrink-0 text-[11px] font-semibold text-slate-500">
                    {batch.failureCount} 个错误
                  </span>
                </div>
                {densityMode !== 'focus' && (
                  <p className="mt-1 truncate text-xs text-slate-500">
                    {batch.provider} / 读取 {batch.readCount} / 确认 {batch.confirmationCount} / {formatDateTime(batch.startedAt)}
                  </p>
                )}
              </div>
            ))}
            {outlookSyncBatches.length === 0 && (
              <p className="rounded-lg border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">
                暂无微软同步批次。
              </p>
            )}
          </div>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">提醒队列</h2>
              <p className="mt-1 text-xs text-slate-500">展示即将触发、已暂停与需升级的提醒。</p>
            </div>
            <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">提醒</span>
          </div>
          <p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-4 text-sm text-slate-500">
            提醒区块会从今日注册表加载，支持低风险直接处理与高风险打开详情。
          </p>
        </section>

        <section className="pim-panel min-w-0 p-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-950">报告</h2>
              <p className="mt-1 text-xs text-slate-500">日报、周报、月报与项目报告可在此进入。</p>
            </div>
            <span className="rounded-full bg-violet-50 px-2.5 py-1 text-xs font-semibold text-violet-700">报告</span>
          </div>
          <p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-4 text-sm text-slate-500">
            报告建议不会直接改动事实，后续动作会进入确认中心。
          </p>
        </section>
      </div>

      {registryLoading ? (
        <EmptyState title="正在加载今日区块" description="今日页面会按区块独立加载数据。" />
      ) : (
        <div className={sectionGridClassName}>
          {sections.map(section => (
            <div key={section.id} className={section.kind === 'pc.activity' ? 'xl:col-span-2' : undefined}>
              <TodaySectionHost
                item={section}
                date={dateStr}
                todayPrefix={dateStr}
                onSelectScheduled={openScheduledItem}
                onSelectTask={openTask}
              />
            </div>
          ))}
        </div>
      )}

      <TaskEditorDialog
        open={taskEditorOpen}
        onClose={() => setTaskEditorOpen(false)}
        task={editingTask}
      />
      <EventEditorDialog
        open={eventEditorOpen}
        onClose={closeEventEditor}
        event={editingEvent}
      />
    </div>
  );
}

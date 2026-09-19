import { useEffect, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { getOutlookSyncBatches } from '../api/calendar';
import { getPendingConfirmations } from '../api/operations';
import { getTodaySectionRegistry } from '../api/today';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import PageHeader from '../ui/PageHeader';
import MobilePageHeader from '../ui/MobilePageHeader';
import EmptyState from '../ui/EmptyState';
import TodaySectionHost, {
  isKnownTodaySectionKind,
  todaySectionOrder,
} from '../components/today/TodaySectionHost';
import type { ScheduledItem } from '../components/today/TodayScheduleList';
import WeekTrendLine from '../components/charts/WeekTrendLine';
import HabitCalendarHeatmap from '../components/charts/HabitCalendarHeatmap';
import { useExhibitionData } from '../components/charts/hooks/useExhibitionData';
import type { EventResponse, TaskResponse, TodaySectionKind, TodaySectionRegistryItem } from '../types';
import { getDeferredAutoRefreshInterval } from '../lib/autoRefresh';
import { todayZoneOf } from './todaySectionLayout';

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
    queryFn: () => getOutlookSyncBatches(),
    refetchInterval: getDeferredAutoRefreshInterval,
  });

  // 只渲染 Web 端已注册的区块：服务端注册表包含尚未在 Web 实现的模块（如 sync.outlook、
  // reports.available 等），未注册的一律静默忽略，不再渲染「未知区块」占位。
  const sections = useMemo(
    () => sortSections((registry?.sections ?? []).filter(section => isKnownTodaySectionKind(section.kind))),
    [registry?.sections],
  );
  const compactItemLimit = 3;

  // 三层分区（v2 信息架构重排）：行动（首屏）/ 数据（回顾）/ 状态（折叠收纳）。
  // 每个区内部是独立 grid + items-start，区与区之间互不拉伸；
  // 长列表板块（任务 / 分类建议）在自身组件内独立滚动（#285 约束在新布局下同样成立）。
  const actionSections = useMemo(
    () => sections.filter(section => todayZoneOf(section.kind) === 'action'),
    [sections],
  );
  const dataSections = useMemo(
    () => sections.filter(section => todayZoneOf(section.kind) === 'data'),
    [sections],
  );
  const statusSections = useMemo(
    () => sections.filter(section => todayZoneOf(section.kind) === 'status'),
    [sections],
  );

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
    <div className="mx-auto max-w-[1500px] space-y-4 overflow-x-auto pb-20 md:pb-4">
      <MobilePageHeader title="今日" action={<span className="md:hidden text-xs text-slate-500">{dateStr}</span>} />
      <PageHeader
        title="日程任务工作台"
        subtitle={`${dateStr} · 日程承诺、任务执行与报告`}
      />

      <RegistryErrorPanel error={registryError} />

      {registryLoading ? (
        <EmptyState title="正在加载今日区块" description="今日页面会按区块独立加载数据。" />
      ) : (
        <div className="space-y-4">
          {/* 行动区：今天要处理的（日程 / 待办任务 / 分类建议）——首屏最高优先级 */}
          <div className="grid grid-cols-1 items-start gap-4 md:grid-cols-2 xl:grid-cols-3">
            {actionSections.map(section => (
              <TodaySectionHost
                key={section.id}
                item={section}
                date={dateStr}
                todayPrefix={dateStr}
                onSelectScheduled={openScheduledItem}
                onSelectTask={openTask}
              />
            ))}
          </div>

          {/* 数据区：PC 记录概览（整行，行内只有它自己，不存在同行拉伸） */}
          {dataSections.map(section => (
            <TodaySectionHost
              key={section.id}
              item={section}
              date={dateStr}
              todayPrefix={dateStr}
              onSelectScheduled={openScheduledItem}
              onSelectTask={openTask}
            />
          ))}
        </div>
      )}

      {/* 数据区（续）：周趋势 + 习惯打卡（真实数据 via useExhibitionData） */}
      <TodayExhibitionEmbed dateStr={dateStr} />

      {/* 运维与状态：默认折叠收纳——系统健康 / 数据质量 / 待确认 / 同步等，不占首屏 */}
      <details className="pim-collapsible pim-panel min-w-0">
        <summary className="flex cursor-pointer items-center justify-between gap-3 px-4 py-3">
          <div>
            <h2 className="text-sm font-semibold text-slate-950">运维与状态</h2>
            <p className="mt-1 text-xs text-slate-500">
              系统健康、数据质量、待确认操作与同步记录
              {pendingConfirmations.length > 0 ? ` · ${pendingConfirmations.length} 个待确认` : ''}
            </p>
          </div>
          <span className="pim-collapsible-chevron text-slate-400" aria-hidden="true">▸</span>
        </summary>

        <div className="space-y-4 px-4 pb-4">
          {statusSections.length > 0 && (
            <div className="grid grid-cols-1 items-start gap-3 md:grid-cols-2 xl:grid-cols-3">
              {statusSections.map(section => (
                <TodaySectionHost
                  key={section.id}
                  item={section}
                  date={dateStr}
                  todayPrefix={dateStr}
                  onSelectScheduled={openScheduledItem}
                  onSelectTask={openTask}
                />
              ))}
            </div>
          )}

          {/* 运营信息卡（待确认 / 微软同步 / 提醒队列 / 报告）——收纳保留，未删除 */}
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
            <section className="pim-panel min-w-0 p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h2 className="text-sm font-semibold text-slate-950">待确认</h2>
                  <p className="mt-1 text-xs text-slate-500">{pendingConfirmations.length} 个操作等待复核</p>
                </div>
                <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
                  复核
                </span>
              </div>
              <div className="mt-3 space-y-2">
                {pendingConfirmations.slice(0, compactItemLimit).map(item => (
                  <div key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                    <div className="flex items-start justify-between gap-2">
                      <p className="min-w-0 truncate text-sm font-medium text-slate-800">{item.summary}</p>
                      <span className="shrink-0 text-[11px] font-semibold text-slate-500">{item.riskLevel}</span>
                    </div>
                    <p className="mt-1 truncate text-xs text-slate-500">{item.source} / {item.operationType}</p>
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
                    <p className="mt-1 truncate text-xs text-slate-500">
                      {batch.provider} / 读取 {batch.readCount} / 确认 {batch.confirmationCount} / {formatDateTime(batch.startedAt)}
                    </p>
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
        </div>
      </details>

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

function TodayExhibitionEmbed({ dateStr }: { dateStr: string }) {
  const q2 = useExhibitionData(2, { real: true, date: dateStr });
  return (
    <section className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <div className="pim-card p-4">
        <h3 className="text-sm font-semibold text-slate-900">周趋势 · 折线图</h3>
        <p className="mt-1 text-xs text-slate-500">近 4 周使用时长走势 · {q2.isReal ? '🔗真实数据' : '🔮模拟数据'}</p>
        <div className="mt-3">{q2.loading ? <div className="h-[168px] animate-pulse rounded-md bg-slate-100" /> : q2.error ? <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-600">加载失败</div> : <WeekTrendLine data={q2.data as never} />}</div>
      </div>
      <HabitCalendarHeatmap />
    </section>
  );
}

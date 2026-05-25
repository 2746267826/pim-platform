import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { getTodaySectionRegistry } from '../api/today';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import PageHeader from '../ui/PageHeader';
import EmptyState from '../ui/EmptyState';
import TodaySectionHost, {
  isKnownTodaySectionKind,
  todaySectionOrder,
} from '../components/today/TodaySectionHost';
import type { TaskResponse, TodaySectionKind, TodaySectionRegistryItem } from '../types';

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
  const [taskEditorOpen, setTaskEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();

  const {
    data: registry,
    error: registryError,
    isLoading: registryLoading,
  } = useQuery({
    queryKey: ['today-sections', dateStr],
    queryFn: () => getTodaySectionRegistry(dateStr),
    refetchInterval: 30000,
  });

  const sections = useMemo(() => sortSections(registry?.sections ?? []), [registry?.sections]);

  function openTask(task: TaskResponse) {
    setEditingTask(task);
    setTaskEditorOpen(true);
  }

  return (
    <div className="mx-auto max-w-[1500px] space-y-4 pb-8">
      <PageHeader
        title="今日工作台"
        subtitle={`${dateStr} · 安排、PC 活动与待办任务`}
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

      {registryLoading ? (
        <EmptyState title="正在加载今日区块" description="今日页面会按区块独立加载数据。" />
      ) : (
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
          {sections.map(section => (
            <div key={section.id} className={section.kind === 'pc.activity' ? 'xl:col-span-2' : undefined}>
              <TodaySectionHost
                item={section}
                date={dateStr}
                todayPrefix={dateStr}
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
    </div>
  );
}

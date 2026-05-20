import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { addDays, format } from 'date-fns';
import { getEvents, getTasks } from '../api/calendar';
import { getPcSummary } from '../api/pcTracker';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import PageHeader from '../ui/PageHeader';
import TodayPcOverview from '../components/today/TodayPcOverview';
import TodayScheduleList, {
  buildScheduledItems,
  type ScheduledItem,
} from '../components/today/TodayScheduleList';
import TodayTaskColumn from '../components/today/TodayTaskColumn';
import type { EventResponse, TaskResponse } from '../types';
import { getPcBusinessDate } from '../utils/pcBusinessDay';

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

function DataErrorPanel({
  eventsError,
  tasksError,
}: {
  eventsError: Error | null;
  tasksError: Error | null;
}) {
  if (!eventsError && !tasksError) return null;

  return (
    <section className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
      <p className="font-medium">今日数据加载不完整</p>
      <div className="mt-1 space-y-1 text-xs leading-5">
        {eventsError && <p>日程加载失败：{errorMessage(eventsError)}</p>}
        {tasksError && <p>任务加载失败：{errorMessage(tasksError)}</p>}
      </div>
    </section>
  );
}

export default function TodayPage() {
  const today = useTodayDate();
  const dateStr = format(today, 'yyyy-MM-dd');
  const pcDateStr = format(getPcBusinessDate(today), 'yyyy-MM-dd');
  const tomorrowStr = format(addDays(today, 1), 'yyyy-MM-dd');
  const [eventEditorOpen, setEventEditorOpen] = useState(false);
  const [taskEditorOpen, setTaskEditorOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventResponse | undefined>();
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();

  const {
    data: events = [],
    error: eventsError,
  } = useQuery({
    queryKey: ['events', dateStr, tomorrowStr],
    queryFn: () => getEvents(dateStr, tomorrowStr),
  });

  const {
    data: tasks = [],
    error: tasksError,
  } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks(),
  });

  const {
    data: pcSummary,
    error: pcError,
    isLoading: pcLoading,
  } = useQuery({
    queryKey: ['pc-summary', pcDateStr],
    queryFn: () => getPcSummary(pcDateStr),
    refetchInterval: 30000,
  });

  const scheduledItems = useMemo(
    () => buildScheduledItems(events, tasks, dateStr),
    [events, tasks, dateStr],
  );

  function openEvent(event: EventResponse | undefined) {
    if (!event) return;
    setEditingEvent(event);
    setEventEditorOpen(true);
  }

  function openTask(task: TaskResponse | undefined) {
    if (!task) return;
    setEditingTask(task);
    setTaskEditorOpen(true);
  }

  function handleScheduledSelect(item: ScheduledItem) {
    if (item.type === 'event') {
      openEvent(events.find(event => event.id === item.id));
      return;
    }
    openTask(tasks.find(task => task.id === item.id));
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

      <DataErrorPanel eventsError={eventsError} tasksError={tasksError} />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
        <TodayScheduleList items={scheduledItems} onSelect={handleScheduledSelect} />
        <div className="xl:col-span-2">
          <TodayPcOverview summary={pcSummary} isLoading={pcLoading} error={pcError} />
        </div>
        <TodayTaskColumn tasks={tasks} todayPrefix={dateStr} onSelect={openTask} />
      </div>

      <EventEditorDialog
        open={eventEditorOpen}
        onClose={() => setEventEditorOpen(false)}
        event={editingEvent}
      />
      <TaskEditorDialog
        open={taskEditorOpen}
        onClose={() => setTaskEditorOpen(false)}
        task={editingTask}
      />
    </div>
  );
}

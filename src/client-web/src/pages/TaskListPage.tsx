import { useEffect, useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getTasks, updateTask, taskToMutationData } from '../api/calendar';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import { sortTasksByDue } from '../components/today/TodayTaskColumn';
import EmptyState from '../ui/EmptyState';
import StatusBadge from '../ui/StatusBadge';
import type { TaskMutationData } from '../api/calendar';
import type { TaskResponse } from '../types';

const filters = [
  { key: 'all', label: '全部' },
  { key: 'inbox', label: '收集箱' },
  { key: 'high', label: '高优先' },
  { key: 'today', label: '今日' },
] as const;

function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function useLocalDate() {
  const [dateStr, setDateStr] = useState(() => formatLocalDate(new Date()));

  useEffect(() => {
    const now = new Date();
    const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const delayMs = nextMidnight.getTime() - now.getTime() + 1000;
    const timerId = window.setTimeout(() => setDateStr(formatLocalDate(new Date())), delayMs);

    return () => window.clearTimeout(timerId);
  }, [dateStr]);

  return dateStr;
}

function priorityDot(priority: number) {
  if (priority === 1) return 'bg-red-500';
  if (priority === 3) return 'bg-teal-500';
  return 'bg-amber-500';
}

function priorityLabel(priority: number) {
  if (priority === 1) return '高优先级';
  if (priority === 3) return '低优先级';
  return '普通优先级';
}

function formatDue(value?: string) {
  if (!value) return '无截止';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '截止时间无效';
  return parsed.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function TaskListPage() {
  const todayStr = useLocalDate();
  const [filter, setFilter] = useState<string>('all');
  const [search, setSearch] = useState('');
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();
  const queryClient = useQueryClient();

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks()
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: TaskMutationData }) =>
      updateTask(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] })
  });

  const filtered = useMemo(() => {
    let result = tasks;
    if (filter === 'inbox') result = result.filter(t => t.isInbox);
    if (filter === 'high') result = result.filter(t => t.priority === 1);
    if (filter === 'today') result = result.filter(t => t.dtStart && t.dtStart.startsWith(todayStr));
    if (search) result = result.filter(t => t.title.toLowerCase().includes(search.toLowerCase()));
    return sortTasksByDue(result);
  }, [tasks, filter, search, todayStr]);

  if (isLoading) return <div className="p-4 text-sm text-slate-500">加载中...</div>;

  return (
    <div className="mx-auto max-w-3xl space-y-4 pb-8">
      <section className="pim-panel p-4">
        <div className="flex flex-wrap gap-2">
          {filters.map(f => (
            <button
              key={f.key}
              type="button"
              onClick={() => setFilter(f.key)}
              className={`rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
                filter === f.key
                  ? 'border-blue-600 bg-blue-600 text-white'
                  : 'border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-slate-50'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        <label className="mt-4 block">
          <span className="sr-only">搜索任务</span>
          <input
            type="text"
            placeholder="搜索任务..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
          />
        </label>
      </section>

      {filtered.length === 0 ? (
        <EmptyState title="没有任务" description="调整筛选或搜索条件后再看看。" />
      ) : (
        <div className="space-y-2">
          {filtered.map(task => (
            <article
              key={task.id}
              className="pim-card flex items-center gap-3 p-3 transition-colors hover:border-blue-200 hover:bg-slate-50"
            >
              <button
                type="button"
                className="flex min-w-0 flex-1 items-center gap-3 rounded-xl text-left focus:outline-none focus:ring-2 focus:ring-blue-200"
                onClick={() => { setEditingTask(task); setEditorOpen(true); }}
                aria-label={`打开任务：${task.title}，${priorityLabel(task.priority)}，${formatDue(task.due)}`}
              >
                <span
                  className={`h-9 w-1 shrink-0 rounded-full ${priorityDot(task.priority)}`}
                  aria-hidden="true"
                />
                <span className="sr-only">{priorityLabel(task.priority)}</span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-slate-900">{task.title}</p>
                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    <StatusBadge tone="neutral">{formatDue(task.due)}</StatusBadge>
                    {task.dtStart && <StatusBadge tone="primary">已排程</StatusBadge>}
                  </div>
                </div>
              </button>
              <button
                type="button"
                onClick={() => {
                  toggleMutation.mutate({
                    id: task.id,
                    data: taskToMutationData(task, {
                      status: task.status === 'COMPLETED' ? 'NEEDS-ACTION' : 'COMPLETED',
                    }),
                  });
                }}
                className={`shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
                  task.status === 'COMPLETED'
                    ? 'border-teal-200 bg-teal-50 text-teal-700'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-slate-50'
                }`}
              >
                {task.status === 'COMPLETED' ? '已完成' : '标记完成'}
              </button>
            </article>
          ))}
        </div>
      )}

      <TaskEditorDialog
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        task={editingTask}
      />
    </div>
  );
}

import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getTasks, updateTask } from '../api/calendar';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import type { TaskResponse } from '../types';

const filters = [
  { key: 'all', label: '全部' },
  { key: 'inbox', label: '收集箱' },
  { key: 'high', label: '高优先' },
  { key: 'today', label: '今日' },
] as const;

export default function TaskListPage() {
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
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      updateTask(id, { status } as Partial<TaskResponse>),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] })
  });

  const filtered = useMemo(() => {
    let result = tasks;
    if (filter === 'inbox') result = result.filter(t => t.isInbox);
    if (filter === 'high') result = result.filter(t => t.priority === 1);
    if (filter === 'today') result = result.filter(t => t.dtStart && t.dtStart.startsWith(new Date().toISOString().split('T')[0]));
    if (search) result = result.filter(t => t.title.toLowerCase().includes(search.toLowerCase()));
    return result;
  }, [tasks, filter, search]);

  if (isLoading) return <div className="p-4 text-gray-400">加载中...</div>;

  return (
    <div className="p-4 max-w-2xl mx-auto">
      <div className="flex gap-2 mb-4">
        {filters.map(f => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`px-3 py-1 text-sm rounded-full border transition-colors ${
              filter === f.key ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <input
        type="text" placeholder="搜索任务..." value={search}
        onChange={e => setSearch(e.target.value)}
        className="w-full border rounded px-3 py-2 mb-4 text-sm"
      />

      <div className="space-y-2">
        {filtered.map(task => (
          <div
            key={task.id}
            className="flex items-center gap-3 p-3 bg-white rounded-lg border hover:shadow-sm transition-shadow cursor-pointer"
            onClick={() => { setEditingTask(task); setEditorOpen(true); }}
          >
            <span
              className="w-3 h-3 rounded-full flex-shrink-0"
              style={{ backgroundColor: task.priority === 1 ? '#E53935' : task.priority === 3 ? '#43A047' : '#FFA726' }}
            />
            <span className="flex-1 text-sm text-gray-800">{task.title}</span>
            {task.dtStart && (
              <span className="text-xs text-gray-400">{new Date(task.dtStart).toLocaleDateString('zh-CN')}</span>
            )}
            <button
              onClick={(e) => { e.stopPropagation(); toggleMutation.mutate({ id: task.id, status: task.status === 'COMPLETED' ? 'NEEDS-ACTION' : 'COMPLETED' }); }}
              className={`text-xs px-2 py-1 rounded border transition-colors ${
                task.status === 'COMPLETED' ? 'bg-green-50 border-green-300 text-green-600' : 'border-gray-300 text-gray-500 hover:bg-gray-50'
              }`}
            >
              {task.status === 'COMPLETED' ? '已完成' : '标记完成'}
            </button>
          </div>
        ))}
      </div>

      {filtered.length === 0 && (
        <p className="text-center text-gray-400 py-12">没有任务</p>
      )}

      <TaskEditorDialog
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        task={editingTask}
      />
    </div>
  );
}

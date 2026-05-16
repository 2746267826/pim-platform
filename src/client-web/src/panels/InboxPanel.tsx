import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getTasks } from '../api/calendar';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import type { TaskResponse } from '../types';

export default function InboxPanel() {
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks()
  });

  const unscheduled = tasks.filter(t => t.isInbox || !t.dtStart);

  return (
    <div className="w-[280px] bg-gray-50 border-l flex flex-col h-full">
      <div className="p-4 border-b">
        <h3 className="font-semibold text-sm text-gray-600">收集箱</h3>
        <p className="text-xs text-gray-400 mt-0.5">
          {unscheduled.length} 个未排程任务
        </p>
      </div>

      <div className="flex-1 overflow-auto p-3 space-y-2">
        {isLoading ? (
          <p className="text-xs text-gray-400 text-center py-8">加载中...</p>
        ) : unscheduled.length === 0 ? (
          <p className="text-xs text-gray-400 text-center py-8">所有任务均已排入日程</p>
        ) : (
          unscheduled.map(task => (
            <div
              key={task.id}
              className="bg-white rounded-lg p-3 border hover:shadow-sm transition-shadow cursor-pointer"
              draggable
              onClick={() => { setEditingTask(task); setEditorOpen(true); }}
            >
              <div className="flex items-start gap-2">
                <span
                  className="w-2 h-2 rounded-full mt-1.5 flex-shrink-0"
                  style={{ backgroundColor: task.priority === 1 ? '#E53935' : task.priority === 3 ? '#43A047' : '#FFA726' }}
                />
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-800 truncate">{task.title}</p>
                  {task.due && (
                    <p className="text-xs text-red-400 mt-1">
                      截止: {new Date(task.due).toLocaleDateString('zh-CN')}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      <div className="p-3 border-t space-y-2">
        <button
          className="w-full py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
          onClick={() => { setEditingTask(undefined); setEditorOpen(true); }}
        >
          + 新建任务
        </button>
        <button className="w-full py-2 text-sm border border-gray-300 text-gray-600 rounded hover:bg-gray-100">
          一键重排
        </button>
      </div>

      <TaskEditorDialog
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        task={editingTask}
      />
    </div>
  );
}

import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { createTask, updateTask, deleteTask, getCalendars, moveTask, taskToMutationData } from '../api/calendar';
import type { TaskMutationData } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import { Field } from './common';
import type { TaskResponse } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  task?: TaskResponse;
  defaultDtStart?: string;
}

export default function TaskEditorDialog(props: Props) {
  const formKey = [
    props.open ? 'open' : 'closed',
    props.task?.id || 'new',
    props.defaultDtStart || 'none',
  ].join(':');

  return <TaskEditorForm key={formKey} {...props} />;
}

function TaskEditorForm({ open, onClose, task, defaultDtStart }: Props) {
  const [title, setTitle] = useState(task?.title || '');
  const [description, setDescription] = useState(task?.description || '');
  const [priority, setPriority] = useState(task?.priority || 0);
  const [dtStart, setDtStart] = useState(defaultDtStart || task?.dtStart || '');
  const [due, setDue] = useState(task?.due || '');
  const [duration, setDuration] = useState(task?.estimatedDuration || '');
  const [calendarId, setCalendarId] = useState(task?.calendarId || '');
  const queryClient = useQueryClient();

  const { data: calendars } = useQuery({
    queryKey: ['calendars', 'task'],
    queryFn: () => getCalendars('task'),
    enabled: open
  });

  const createMut = useMutation({
    mutationFn: (data: Partial<TaskMutationData>) => createTask(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  const updateMut = useMutation({
    mutationFn: async ({ data, confirmSchedule }: { data: TaskMutationData; confirmSchedule?: boolean }) => {
      if (confirmSchedule && data.dtStart) {
        await moveTask(task!.id, { scheduledStart: data.dtStart });
      }
      const updated = await updateTask(task!.id, data);
      return updated;
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  const deleteMut = useMutation({
    mutationFn: () => deleteTask(task!.id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  const mutationError = createMut.error || updateMut.error || deleteMut.error;
  const mutationErrorMessage = mutationError instanceof Error ? mutationError.message : null;

  function handleDelete() {
    if (confirm(`确定删除任务 "${task?.title}"？`)) {
      deleteMut.mutate();
    }
  }

  function handleToggleComplete() {
    if (!task) return;
    const newStatus = task?.status === 'COMPLETED' ? 'NEEDS-ACTION' : 'COMPLETED';
    updateMut.mutate({ data: taskToMutationData(task, { status: newStatus }) });
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data: TaskMutationData = {
      title, description, priority,
      dtStart: dtStart || undefined,
      due: due || undefined,
      estimatedDuration: duration || undefined,
      calendarId: calendarId || undefined
    };
    if (task) {
      updateMut.mutate({
        data: taskToMutationData(task, data),
        confirmSchedule: Boolean(defaultDtStart && data.dtStart),
      });
    }
    else createMut.mutate(data);
  }

  const isCompleted = task?.status === 'COMPLETED';
  const priorityOptions = [
    { value: 1, label: '高', className: 'bg-red-50 text-red-600 border-red-200' },
    { value: 0, label: '普通', className: 'bg-amber-50 text-amber-700 border-amber-200' },
    { value: 3, label: '低', className: 'bg-teal-50 text-teal-700 border-teal-200' },
  ];

  const footer = (
    <>
      <div className="flex flex-wrap gap-2">
        {task && (
          <button type="button" onClick={handleDelete}
            disabled={deleteMut.isPending}
            className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600 disabled:opacity-50">
            删除
          </button>
        )}
        {task && (
          <button type="button" onClick={handleToggleComplete}
            disabled={updateMut.isPending}
            className={`rounded-lg border px-3 py-2 text-sm disabled:opacity-50 ${
              isCompleted
                ? 'border-gray-300 text-gray-600 hover:bg-gray-50'
                : 'border-green-300 text-green-600 hover:bg-green-50'
            }`}>
            {isCompleted ? '标记未完成' : '标记完成'}
          </button>
        )}
      </div>
      <div className="flex gap-2">
        <button type="button" onClick={onClose}
          className="pim-button-secondary px-4 py-2 text-sm">取消</button>
        <button type="submit" form="task-editor-form" disabled={createMut.isPending || updateMut.isPending}
          className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50">
          {task ? '保存' : '创建'}
        </button>
      </div>
    </>
  );

  return (
    <EditorDrawer open={open} onClose={onClose} title={task ? '编辑任务' : '新建任务'} footer={footer}>
      <form id="task-editor-form" onSubmit={handleSubmit} className="space-y-4">
        {mutationErrorMessage && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {mutationErrorMessage}
          </div>
        )}
        <Field label="任务本">
          <select value={calendarId} onChange={e => setCalendarId(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm">
            <option value="">未分类</option>
            {calendars?.map(cal => (
              <option key={cal.id} value={cal.id}>{cal.name}</option>
            ))}
          </select>
        </Field>
        <Field label="标题">
          <input type="text" value={title} onChange={e => setTitle(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="描述">
          <textarea value={description} onChange={e => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" rows={3} />
        </Field>
        <Field label="优先级">
          <div className="flex gap-2">
            {priorityOptions.map(item => (
              <button
                key={item.value}
                type="button"
                onClick={() => setPriority(item.value)}
                className={`rounded-full border px-3 py-1.5 text-sm ${
                  priority === item.value ? item.className : 'border-slate-200 bg-white text-slate-500'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </Field>
        <Field label="计划时间">
          <input type="datetime-local" value={dtStart} onChange={e => setDtStart(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="截止日期">
          <input type="datetime-local" value={due} onChange={e => setDue(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="预估时长">
          <input type="text" value={duration} onChange={e => setDuration(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="例如：PT1H30M" />
        </Field>
      </form>
    </EditorDrawer>
  );
}

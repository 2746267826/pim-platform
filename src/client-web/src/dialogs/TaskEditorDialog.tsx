import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient, useQuery, type QueryClient } from '@tanstack/react-query';
import { createTask, updateTask, deleteTask, getCalendars, moveTask, taskToMutationData } from '../api/calendar';
import type { TaskMutationData } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
import { Field } from './common';
import type { TaskResponse } from '../types';
import { isoToDatetimeLocal, datetimeLocalToUtcIso, isEndAfterStart } from '../utils/dateTimeInput';
import { dotnetDurationToHoursMinutes, hoursMinutesToIsoDuration, isValidDuration, durationErrorMessage } from '../utils/durationInput';

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

function invalidateTaskRelatedQueries(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: ['tasks'] });
  queryClient.invalidateQueries({ queryKey: ['tasks-paged'] });
  queryClient.invalidateQueries({ queryKey: ['today-sections'] });
  queryClient.invalidateQueries({ queryKey: ['today-section'] });
}

function TaskEditorForm({ open, onClose, task, defaultDtStart }: Props) {
  const [title, setTitle] = useState(task?.title || '');
  const [description, setDescription] = useState(task?.description || '');
  const [priority, setPriority] = useState(task?.priority || 0);
  const [dtStart, setDtStart] = useState(defaultDtStart || isoToDatetimeLocal(task?.dtStart ?? '') || '');
  const [plannedEnd, setPlannedEnd] = useState(isoToDatetimeLocal(task?.plannedEnd ?? '') || '');
  const [due, setDue] = useState(isoToDatetimeLocal(task?.due ?? '') || '');
  const [durationHours, setDurationHours] = useState(() => {
    if (!task) return '0';
    if (!task.estimatedDuration) return '';
    return String(dotnetDurationToHoursMinutes(task.estimatedDuration).hours);
  });
  const [durationMinutes, setDurationMinutes] = useState(() => {
    if (!task) return '30';
    if (!task.estimatedDuration) return '';
    return String(dotnetDurationToHoursMinutes(task.estimatedDuration).minutes);
  });
  const [calendarId, setCalendarId] = useState(task?.calendarId || '');
  const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
  const [validationErrorMessage, setValidationErrorMessage] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data: calendars } = useQuery({
    queryKey: ['calendars', 'task'],
    queryFn: () => getCalendars('task'),
    enabled: open
  });

  const createMut = useMutation({
    mutationFn: (data: Partial<TaskMutationData>) => createTask(data),
    onSuccess: () => { invalidateTaskRelatedQueries(queryClient); onClose(); }
  });

  const updateMut = useMutation({
    mutationFn: async ({ data, confirmSchedule }: { data: TaskMutationData; confirmSchedule?: boolean }) => {
      if (confirmSchedule && data.dtStart) {
        await moveTask(task!.id, { scheduledStart: data.dtStart });
      }
      const updated = await updateTask(task!.id, data);
      return updated;
    },
    onSuccess: () => { invalidateTaskRelatedQueries(queryClient); onClose(); }
  });

  const deleteMut = useMutation({
    mutationFn: () => deleteTask(task!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
      invalidateTaskRelatedQueries(queryClient);
      setDeleteInput(null);
      onClose();
    },
    onError: () => setDeleteInput(null),
  });

  const mutationError = createMut.error || updateMut.error || deleteMut.error;
  const mutationErrorMessage = validationErrorMessage || (mutationError instanceof Error ? mutationError.message : null);

  function handleDelete() {
    if (!task) return;
    deleteMut.reset();
    setDeleteInput({
      targetType: 'task',
      title: task.title,
      affectedCount: 1,
      samples: [{
        id: task.id,
        type: 'task',
        title: task.title,
        start: task.dtStart,
        end: task.plannedEnd || task.due,
        bookName: undefined,
      }],
    });
  }

  function confirmDelete() {
    if (!task) return;
    deleteMut.mutate();
  }

  function cancelDelete() {
    if (deleteMut.isPending) return;
    setDeleteInput(null);
  }

  function handleToggleComplete() {
    if (!task) return;
    const newStatus = task?.status === 'COMPLETED' ? 'NEEDS-ACTION' : 'COMPLETED';
    updateMut.mutate({ data: taskToMutationData(task, { status: newStatus }) });
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (task?.plannedEnd && !plannedEnd) {
      setValidationErrorMessage('当前接口暂不支持清空计划结束时间，可改成新的结束时间。');
      return;
    }

    setValidationErrorMessage(null);

    if (durationHours !== '' || durationMinutes !== '') {
      if (!isValidDuration(durationHours, durationMinutes)) {
        setValidationErrorMessage(durationErrorMessage());
        return;
      }
    }

    if (dtStart && plannedEnd && !isEndAfterStart(dtStart, plannedEnd)) {
      setValidationErrorMessage('计划结束时间必须晚于开始时间');
      return;
    }

    const estimatedDuration = (durationHours !== '' || durationMinutes !== '')
      ? hoursMinutesToIsoDuration(Number(durationHours), Number(durationMinutes))
      : undefined;

    const data: TaskMutationData = {
      title, description, priority,
      dtStart: datetimeLocalToUtcIso(dtStart) || undefined,
      plannedEnd: datetimeLocalToUtcIso(plannedEnd) || undefined,
      due: datetimeLocalToUtcIso(due) || undefined,
      estimatedDuration,
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
    <>
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
        <Field label="计划结束">
          <input type="datetime-local" value={plannedEnd} onChange={e => {
            setPlannedEnd(e.target.value);
            setValidationErrorMessage(null);
          }}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="截止日期">
          <input type="datetime-local" value={due} onChange={e => setDue(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="预估时长">
          <div className="flex items-center gap-2">
            <input id="task-duration-hours" type="number"
              aria-label="时"
              value={durationHours}
              onChange={e => setDurationHours(e.target.value)}
              min={0} step={1}
              className="w-20 border rounded px-3 py-2 text-sm" />
            <span className="text-sm text-gray-500">时</span>
            <input id="task-duration-minutes" type="number"
              aria-label="分钟"
              value={durationMinutes}
              onChange={e => setDurationMinutes(e.target.value)}
              min={0} max={59} step={1}
              className="w-20 border rounded px-3 py-2 text-sm" />
            <span className="text-sm text-gray-500">分钟</span>
          </div>
        </Field>
      </form>
    </EditorDrawer>
    <ConfirmActionDialog
      open={deleteInput !== null}
      input={deleteInput}
      isPending={deleteMut.isPending}
      onCancel={cancelDelete}
      onConfirm={confirmDelete}
    />
    </>
  );
}

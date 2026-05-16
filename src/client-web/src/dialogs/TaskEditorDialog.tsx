import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createTask, updateTask } from '../api/calendar';
import { Dialog, Field } from './common';
import type { TaskResponse } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  task?: TaskResponse;
}

export default function TaskEditorDialog({ open, onClose, task }: Props) {
  const [title, setTitle] = useState(task?.title || '');
  const [description, setDescription] = useState(task?.description || '');
  const [priority, setPriority] = useState(task?.priority || 0);
  const [dtStart, setDtStart] = useState(task?.dtStart || '');
  const [due, setDue] = useState(task?.due || '');
  const [duration, setDuration] = useState(task?.estimatedDuration || 'PT1H');
  const queryClient = useQueryClient();

  const createMut = useMutation({
    mutationFn: (data: Partial<TaskResponse>) => createTask(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  const updateMut = useMutation({
    mutationFn: (data: Partial<TaskResponse>) => updateTask(task!.id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['tasks'] }); onClose(); }
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data = { title, description, priority, dtStart: dtStart || undefined, due: due || undefined, estimatedDuration: duration };
    if (task) updateMut.mutate(data);
    else createMut.mutate(data);
  }

  return (
    <Dialog open={open} onClose={onClose} title={task ? '编辑任务' : '新建任务'}>
      <form onSubmit={handleSubmit}>
        <Field label="标题">
          <input type="text" value={title} onChange={e => setTitle(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="描述">
          <textarea value={description} onChange={e => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" rows={3} />
        </Field>
        <Field label="优先级">
          <select value={priority} onChange={e => setPriority(Number(e.target.value))}
            className="w-full border rounded px-3 py-2 text-sm">
            <option value={0}>普通</option>
            <option value={1}>高</option>
            <option value={3}>低</option>
          </select>
        </Field>
        <Field label="计划时间">
          <input type="datetime-local" value={dtStart} onChange={e => setDtStart(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="截止日期">
          <input type="datetime-local" value={due} onChange={e => setDue(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="预估时长 (ISO 8601)">
          <input type="text" value={duration} onChange={e => setDuration(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" placeholder="PT1H30M" />
        </Field>
        <div className="flex justify-end gap-3 mt-4">
          <button type="button" onClick={onClose}
            className="px-4 py-2 text-sm border rounded hover:bg-gray-50">取消</button>
          <button type="submit" disabled={createMut.isPending || updateMut.isPending}
            className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
            {task ? '保存' : '创建'}
          </button>
        </div>
      </form>
    </Dialog>
  );
}

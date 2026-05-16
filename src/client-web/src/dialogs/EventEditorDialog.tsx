import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createEvent, updateEvent } from '../api/calendar';
import { Dialog, Field } from './common';
import type { EventResponse } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  event?: EventResponse;
  defaultStart?: string;
  defaultEnd?: string;
}

export default function EventEditorDialog({ open, onClose, event, defaultStart, defaultEnd }: Props) {
  const [title, setTitle] = useState(event?.title || '');
  const [description, setDescription] = useState(event?.description || '');
  const [location, setLocation] = useState(event?.location || '');
  const [dtStart, setDtStart] = useState(event?.dtStart || defaultStart || '');
  const [dtEnd, setDtEnd] = useState(event?.dtEnd || defaultEnd || '');
  const queryClient = useQueryClient();

  const createMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => createEvent(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); onClose(); }
  });

  const updateMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => updateEvent(event!.id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); onClose(); }
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data = { title, description, location, dtStart, dtEnd };
    if (event) updateMut.mutate(data);
    else createMut.mutate(data);
  }

  return (
    <Dialog open={open} onClose={onClose} title={event ? '编辑日程' : '新建日程'}>
      <form onSubmit={handleSubmit}>
        <Field label="标题">
          <input type="text" value={title} onChange={e => setTitle(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="开始时间">
          <input type="datetime-local" value={dtStart} onChange={e => setDtStart(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="结束时间">
          <input type="datetime-local" value={dtEnd} onChange={e => setDtEnd(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" required />
        </Field>
        <Field label="地点">
          <input type="text" value={location} onChange={e => setLocation(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" />
        </Field>
        <Field label="描述">
          <textarea value={description} onChange={e => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm" rows={3} />
        </Field>
        <div className="flex justify-end gap-3 mt-4">
          <button type="button" onClick={onClose}
            className="px-4 py-2 text-sm border rounded hover:bg-gray-50">取消</button>
          <button type="submit" disabled={createMut.isPending || updateMut.isPending}
            className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
            {event ? '保存' : '创建'}
          </button>
        </div>
      </form>
    </Dialog>
  );
}

import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { createEvent, updateEvent, deleteEvent, getCalendars } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import { Field } from './common';
import type { EventResponse } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  event?: EventResponse;
  defaultStart?: string;
  defaultEnd?: string;
}

export default function EventEditorDialog(props: Props) {
  const formKey = [
    props.open ? 'open' : 'closed',
    props.event?.id || 'new',
    props.defaultStart || 'none',
    props.defaultEnd || 'none',
  ].join(':');

  return <EventEditorForm key={formKey} {...props} />;
}

function EventEditorForm({ open, onClose, event, defaultStart, defaultEnd }: Props) {
  const [title, setTitle] = useState(event?.title || '');
  const [description, setDescription] = useState(event?.description || '');
  const [location, setLocation] = useState(event?.location || '');
  const [dtStart, setDtStart] = useState(event?.dtStart || defaultStart || '');
  const [dtEnd, setDtEnd] = useState(event?.dtEnd || defaultEnd || '');
  const [calendarId, setCalendarId] = useState(event?.calendarId || '');
  const queryClient = useQueryClient();

  const { data: calendars } = useQuery({
    queryKey: ['calendars', 'calendar'],
    queryFn: () => getCalendars('calendar'),
    enabled: open
  });

  const createMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => createEvent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      onClose();
    }
  });

  const updateMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => updateEvent(event!.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      onClose();
    }
  });

  const deleteMut = useMutation({
    mutationFn: () => deleteEvent(event!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      onClose();
    }
  });

  const mutationError = createMut.error || updateMut.error || deleteMut.error;
  const mutationErrorMessage = mutationError instanceof Error ? mutationError.message : null;

  useEffect(() => {
    if (calendarId || event || !calendars || calendars.length !== 1) return;
    setCalendarId(calendars[0].id);
  }, [calendarId, calendars, event]);

  function handleDelete() {
    if (confirm(`确定删除日程 "${event?.title}"？此操作不可撤销。`)) {
      deleteMut.mutate();
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data = { title, description, location, dtStart, dtEnd, calendarId: calendarId || undefined };
    if (event) updateMut.mutate(data);
    else createMut.mutate(data);
  }

  const footer = (
    <>
      <div>
        {event && (
          <button type="button" onClick={handleDelete}
            disabled={deleteMut.isPending}
            className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600 disabled:opacity-50">
            删除
          </button>
        )}
      </div>
      <div className="flex gap-2">
        <button type="button" onClick={onClose}
          className="pim-button-secondary px-4 py-2 text-sm">取消</button>
        <button type="submit" form="event-editor-form" disabled={createMut.isPending || updateMut.isPending}
          className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50">
          {event ? '保存' : '创建'}
        </button>
      </div>
    </>
  );

  return (
    <EditorDrawer open={open} onClose={onClose} title={event ? '编辑日程' : '新建日程'} footer={footer}>
      <form id="event-editor-form" onSubmit={handleSubmit} className="space-y-4">
        {mutationErrorMessage && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {mutationErrorMessage}
          </div>
        )}
        <Field label="日历本">
          <select value={calendarId} onChange={e => setCalendarId(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm">
            <option value="">默认日历</option>
            {calendars?.map(cal => (
              <option key={cal.id} value={cal.id}>{cal.name}</option>
            ))}
          </select>
        </Field>
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
      </form>
    </EditorDrawer>
  );
}

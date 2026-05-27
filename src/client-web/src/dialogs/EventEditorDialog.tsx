import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { createEvent, updateEvent, deleteEvent, getCalendars } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
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
  const [isAllDay, setIsAllDay] = useState(Boolean(event?.isAllDay));
  const [calendarId, setCalendarId] = useState(event?.calendarId || '');
  const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
  const queryClient = useQueryClient();

  const { data: calendars } = useQuery({
    queryKey: ['calendars', 'calendar'],
    queryFn: () => getCalendars('calendar'),
    enabled: open
  });
  const selectedCalendarId = calendarId || (!event && calendars?.length === 1 ? calendars[0].id : '');

  const createMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => createEvent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      onClose();
    }
  });

  function invalidateEventDeleteQueries() {
    queryClient.invalidateQueries({ queryKey: ['events'] });
    queryClient.invalidateQueries({ queryKey: ['events-paged'] });
    queryClient.invalidateQueries({ queryKey: ['calendars'] });
    queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
  }

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
      invalidateEventDeleteQueries();
      setDeleteInput(null);
      onClose();
    },
    onError: () => setDeleteInput(null),
  });

  const mutationError = createMut.error || updateMut.error || deleteMut.error;
  const mutationErrorMessage = mutationError instanceof Error ? mutationError.message : null;

  function handleDelete() {
    if (!event) return;
    deleteMut.reset();
    setDeleteInput({
      targetType: 'event',
      title: event.title,
      affectedCount: 1,
      samples: [{
        id: event.id,
        type: 'event',
        title: event.title,
        start: event.dtStart,
        end: event.dtEnd,
      }],
    });
  }

  function confirmDelete() {
    if (!event) return;
    deleteMut.mutate();
  }

  function cancelDelete() {
    if (deleteMut.isPending) return;
    setDeleteInput(null);
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const data = { title, description, location, dtStart, dtEnd, isAllDay, calendarId: selectedCalendarId || undefined };
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
    <>
    <EditorDrawer open={open} onClose={onClose} title={event ? '编辑日程' : '新建日程'} footer={footer}>
      <form id="event-editor-form" onSubmit={handleSubmit} className="space-y-4">
        {mutationErrorMessage && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {mutationErrorMessage}
          </div>
        )}
        {event?.source === 'outlook-ics' && (
          <div className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm leading-6 text-blue-700">
            这是从 Outlook ICS 导入的事件，会议上下文已保留，PIM 暂不处理会议接受/拒绝/参会状态。
          </div>
        )}
        <Field label="日历本">
          <select value={selectedCalendarId} onChange={e => setCalendarId(e.target.value)}
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
        <label className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={isAllDay}
            onChange={e => setIsAllDay(e.target.checked)}
            className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-200"
          />
          全天事件
        </label>
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

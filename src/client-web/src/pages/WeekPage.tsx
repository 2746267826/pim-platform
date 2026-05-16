import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { getEvents, getTasks } from '../api/calendar';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';
import type { EventResponse, TaskResponse } from '../types';

export default function WeekPage() {
  const [currentRange, setCurrentRange] = useState(() => {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const start = new Date(now);
    start.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
    const end = new Date(start);
    end.setDate(start.getDate() + 7);
    return { start: toISODate(start), end: toISODate(end) };
  });

  const { data: events = [] } = useQuery({
    queryKey: ['events', currentRange.start, currentRange.end],
    queryFn: () => getEvents(currentRange.start, currentRange.end)
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks-week'],
    queryFn: () => getTasks()
  });

  const [editorOpen, setEditorOpen] = useState(false);
  const [editEvent, setEditEvent] = useState<EventResponse | undefined>();
  const [selectStart, setSelectStart] = useState<string | undefined>();
  const [selectEnd, setSelectEnd] = useState<string | undefined>();

  const fcEvents = buildFcEvents(events, tasks);

  function handleDatesSet(arg: { start: Date; end: Date }) {
    setCurrentRange({ start: arg.start.toISOString(), end: arg.end.toISOString() });
  }

  const handleDateSelect = useCallback((selectInfo: DateSelectArg) => {
    setEditEvent(undefined);
    setSelectStart(selectInfo.startStr);
    setSelectEnd(selectInfo.endStr);
    setEditorOpen(true);
  }, []);

  const handleEventClick = useCallback((clickInfo: EventClickArg) => {
    const raw = clickInfo.event.extendedProps as unknown as EventResponse;
    setEditEvent(raw);
    setEditorOpen(true);
  }, []);

  return (
    <div className="h-full">
      <FullCalendar
        plugins={[timeGridPlugin, interactionPlugin]}
        initialView="timeGridWeek"
        events={fcEvents}
        locale="zh-cn"
        height="100%"
        allDaySlot={false}
        slotMinTime="06:00:00"
        slotMaxTime="24:00:00"
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: ''
        }}
        datesSet={handleDatesSet}
        selectable={true}
        select={handleDateSelect}
        eventClick={handleEventClick}
        selectMirror={true}
      />
      <EventEditorDialog
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        event={editEvent}
        defaultStart={selectStart}
        defaultEnd={selectEnd}
      />
    </div>
  );
}

function buildFcEvents(events: EventResponse[], tasks: TaskResponse[]) {
  const fcEvents: Array<{
    id: string; title: string; start: string; end: string;
    backgroundColor: string; borderColor: string; extendedProps: Record<string, unknown>;
  }> = [];

  for (const e of events) {
    fcEvents.push({
      id: e.id, title: e.title, start: e.dtStart, end: e.dtEnd,
      backgroundColor: '#1565c0', borderColor: '#1565c0',
      extendedProps: { type: 'event', ...e }
    });
  }

  for (const t of tasks) {
    if (!t.dtStart) continue;
    const start = new Date(t.dtStart);
    const end = new Date(start.getTime() + 60 * 60 * 1000);
    const color = t.priority === 1 ? '#E53935' : t.priority === 3 ? '#43A047' : '#FFA726';
    fcEvents.push({
      id: t.id, title: t.title,
      start: t.dtStart,
      end: end.toISOString(),
      backgroundColor: color, borderColor: color,
      extendedProps: { type: 'task', ...t }
    });
  }

  return fcEvents;
}

function toISODate(d: Date): string {
  return d.toISOString();
}

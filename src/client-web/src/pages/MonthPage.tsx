import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { getEvents, getTasks } from '../api/calendar';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';
import type { EventResponse } from '../types';

export default function MonthPage() {
  const [currentRange, setCurrentRange] = useState(() => {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return { start: toDateStr(start), end: toDateStr(end) };
  });

  const { data: events = [] } = useQuery({
    queryKey: ['events', currentRange.start, currentRange.end],
    queryFn: () => getEvents(currentRange.start, currentRange.end)
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks', currentRange.start, currentRange.end],
    queryFn: () => getTasks()
  });

  const [editorOpen, setEditorOpen] = useState(false);
  const [editEvent, setEditEvent] = useState<EventResponse | undefined>();
  const [selectStart, setSelectStart] = useState<string | undefined>();
  const [selectEnd, setSelectEnd] = useState<string | undefined>();

  const fcEvents = [
    ...events.map(e => ({
      id: e.id,
      title: e.title,
      start: e.dtStart,
      end: e.dtEnd,
      backgroundColor: '#1565c0',
      borderColor: '#1565c0',
      extendedProps: { type: 'event', ...e }
    })),
    ...tasks.filter(t => t.dtStart).map(t => ({
      id: t.id,
      title: t.title,
      start: t.dtStart,
      backgroundColor: t.priority === 1 ? '#E53935' : t.priority === 3 ? '#43A047' : '#FFA726',
      borderColor: 'transparent',
      extendedProps: { type: 'task', ...t }
    }))
  ];

  function handleDatesSet(arg: { start: Date; end: Date }) {
    setCurrentRange({ start: toDateStr(arg.start), end: toDateStr(arg.end) });
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
        plugins={[dayGridPlugin, interactionPlugin]}
        initialView="dayGridMonth"
        events={fcEvents}
        locale="zh-cn"
        height="100%"
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: ''
        }}
        datesSet={handleDatesSet}
        selectable={true}
        select={handleDateSelect}
        eventClick={handleEventClick}
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

function toDateStr(d: Date): string {
  return d.toISOString().split('T')[0];
}

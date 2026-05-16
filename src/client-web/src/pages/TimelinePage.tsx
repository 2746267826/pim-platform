import { useState, useCallback } from 'react';
import FullCalendar from '@fullcalendar/react';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { getEvents, getTasks } from '../api/calendar';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import { format } from 'date-fns';
import type { DateSelectArg, EventClickArg } from '@fullcalendar/core';
import type { EventResponse } from '../types';

export default function TimelinePage() {
  const [selectedDate, setSelectedDate] = useState(new Date());

  const startStr = format(selectedDate, 'yyyy-MM-dd');
  const endStr = format(new Date(selectedDate.getTime() + 86400000), 'yyyy-MM-dd');

  const { data: events = [] } = useQuery({
    queryKey: ['events', startStr, endStr],
    queryFn: () => getEvents(startStr, endStr)
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks-timeline', startStr],
    queryFn: () => getTasks()
  });

  const [editorOpen, setEditorOpen] = useState(false);
  const [editEvent, setEditEvent] = useState<EventResponse | undefined>();
  const [selectStart, setSelectStart] = useState<string | undefined>();
  const [selectEnd, setSelectEnd] = useState<string | undefined>();

  const fcEvents = buildFcEvents(events, tasks.filter(t =>
    t.dtStart && t.dtStart.startsWith(startStr)
  ));

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
    <div className="h-full flex flex-col">
      <div className="flex items-center justify-between mb-2 px-4 py-2 bg-white border-b">
        <div className="flex items-center gap-2">
          <button
            className="px-2 py-1 text-sm border rounded hover:bg-gray-50"
            onClick={() => setSelectedDate(new Date())}
          >
            今日
          </button>
          <button
            className="px-2 py-1 text-sm border rounded hover:bg-gray-50"
            onClick={() => setSelectedDate(d => new Date(d.getTime() - 86400000))}
          >
            ‹
          </button>
          <button
            className="px-2 py-1 text-sm border rounded hover:bg-gray-50"
            onClick={() => setSelectedDate(d => new Date(d.getTime() + 86400000))}
          >
            ›
          </button>
          <span className="font-bold text-lg ml-3">
            {format(selectedDate, 'M月d日')}
          </span>
        </div>
      </div>

      <div className="flex-1">
        <FullCalendar
          plugins={[timeGridPlugin, interactionPlugin]}
          initialView="timeGridDay"
          initialDate={format(selectedDate, 'yyyy-MM-dd')}
          events={fcEvents}
          locale="zh-cn"
          height="100%"
          allDaySlot={false}
          slotMinTime="00:00:00"
          slotMaxTime="24:00:00"
          headerToolbar={false}
          selectable={true}
          select={handleDateSelect}
          eventClick={handleEventClick}
          selectMirror={true}
        />
      </div>
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

function buildFcEvents(events: Array<{ id: string; title: string; dtStart: string; dtEnd: string }>, tasks: Array<{ id: string; title: string; dtStart?: string; priority: number }>) {
  const result: Array<{ id: string; title: string; start: string; end: string; backgroundColor: string; borderColor: string }> = [];
  for (const e of events) {
    result.push({ id: e.id, title: e.title, start: e.dtStart, end: e.dtEnd, backgroundColor: '#1565c0', borderColor: '#1565c0' });
  }
  for (const t of tasks) {
    if (!t.dtStart) continue;
    const end = new Date(new Date(t.dtStart).getTime() + 3600000).toISOString();
    const color = t.priority === 1 ? '#E53935' : t.priority === 3 ? '#43A047' : '#FFA726';
    result.push({ id: t.id, title: t.title, start: t.dtStart, end, backgroundColor: color, borderColor: color });
  }
  return result;
}

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin, { Draggable } from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import type { DateSelectArg, DatesSetArg, EventClickArg, EventContentArg, EventInput } from '@fullcalendar/core';
import { format } from 'date-fns';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { getEvents, getTasks } from '../api/calendar';
import { useCalendarVisibility } from '../context/CalendarVisibilityContext';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';
import type { EventResponse, TaskResponse } from '../types';

type CalendarMode = 'timeline' | 'month';

type CalendarDropArg = {
  draggedEl: HTMLElement;
  date: Date;
};

type CalendarEventInput = EventInput & {
  extendedProps: {
    type: 'event' | 'task';
    raw: EventResponse | TaskResponse;
  };
};

const CALENDAR_MODE_OPTIONS: Array<{ value: CalendarMode; label: string }> = [
  { value: 'timeline', label: '时间轴' },
  { value: 'month', label: '月视图' },
];

export default function CalendarPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const mode = normalizeMode(searchParams.get('view'));
  const [activeDate, setActiveDate] = useState(() => new Date());
  const [visibleRange, setVisibleRange] = useState(() => rangeForDate(activeDate, mode));
  const [taskEditorOpen, setTaskEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();
  const [taskDefaultDtStart, setTaskDefaultDtStart] = useState<string | undefined>();
  const [visibleTitle, setVisibleTitle] = useState(() => formatCalendarTitle(new Date(), mode));
  const [eventEditorOpen, setEventEditorOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventResponse | undefined>();
  const [eventDefaultStart, setEventDefaultStart] = useState<string | undefined>();
  const [eventDefaultEnd, setEventDefaultEnd] = useState<string | undefined>();
  const calendarRef = useRef<FullCalendar>(null);
  const pageRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (searchParams.get('view') === mode) return;

    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('view', mode);
    setSearchParams(nextParams, { replace: true });
  }, [mode, searchParams, setSearchParams]);

  useEffect(() => {
    const root = pageRef.current?.closest('.pim-shell') as HTMLElement | null;
    if (!root) return;

    const draggable = new Draggable(root, {
      itemSelector: '.js-draggable-task',
      eventData: draggedEl => ({
        create: false,
        title: draggedEl.dataset.taskTitle || '未命名任务',
      }),
    });

    return () => draggable.destroy();
  }, []);

  const { data: events = [] } = useQuery({
    queryKey: ['events', visibleRange.start, visibleRange.end],
    queryFn: () => getEvents(visibleRange.start, visibleRange.end),
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks(),
  });

  const { hiddenCalendarIds } = useCalendarVisibility();
  const calendarEvents = useMemo(() => {
    const visibleEvents = hiddenCalendarIds.size > 0
      ? events.filter(event => !hiddenCalendarIds.has(event.calendarId))
      : events;

    return buildCalendarEvents(visibleEvents, tasks);
  }, [events, hiddenCalendarIds, tasks]);

  function handleModeChange(nextMode: CalendarMode) {
    const currentDate = calendarRef.current?.getApi().getDate() ?? new Date();
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('view', nextMode);
    setActiveDate(currentDate);
    setVisibleRange(rangeForDate(currentDate, nextMode));
    setVisibleTitle(formatCalendarTitle(currentDate, nextMode));
    setSearchParams(nextParams);
  }

  function handleCalendarPrev() {
    calendarRef.current?.getApi().prev();
  }

  function handleCalendarToday() {
    calendarRef.current?.getApi().today();
  }

  function handleCalendarNext() {
    calendarRef.current?.getApi().next();
  }

  function handleDatesSet(arg: DatesSetArg) {
    const nextActiveDate = arg.view.currentStart || arg.start;
    const nextRange = {
      start: toDateStr(arg.start),
      end: toDateStr(arg.end),
    };

    setActiveDate(nextActiveDate);
    setVisibleTitle(arg.view.title);
    setVisibleRange(currentRange => (
      currentRange.start === nextRange.start && currentRange.end === nextRange.end
        ? currentRange
        : nextRange
    ));
  }

  const handleDateSelect = useCallback((selectInfo: DateSelectArg) => {
    setEditingEvent(undefined);
    setEventDefaultStart(toLocalDateTimeInputValue(selectInfo.start));
    setEventDefaultEnd(toLocalDateTimeInputValue(selectInfo.end));
    setEventEditorOpen(true);
  }, []);

  const handleEventClick = useCallback((clickInfo: EventClickArg) => {
    const props = clickInfo.event.extendedProps as CalendarEventInput['extendedProps'];

    if (props.type === 'task') {
      setEditingTask(props.raw as TaskResponse);
      setTaskDefaultDtStart(undefined);
      setTaskEditorOpen(true);
      return;
    }

    setEditingEvent(props.raw as EventResponse);
    setEventDefaultStart(undefined);
    setEventDefaultEnd(undefined);
    setEventEditorOpen(true);
  }, []);

  const handleExternalDrop = useCallback((dropInfo: CalendarDropArg) => {
    const taskId = dropInfo.draggedEl.dataset.taskId;
    const task = tasks.find(item => item.id === taskId);
    if (!task) return;

    const scheduledStart = toLocalDateTimeInputValue(dropInfo.date);
    setEditingTask(task);
    setTaskDefaultDtStart(scheduledStart);
    setTaskEditorOpen(true);
  }, [tasks]);

  return (
    <div ref={pageRef} className="flex h-full min-h-0 flex-col gap-4">
      <PageHeader
        title="日历"
        subtitle={mode === 'timeline' ? '按时间轴安排今天的任务和日程' : '按月查看任务和日程分布'}
        beforeActions={
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="pim-button-secondary px-3 py-1.5 text-sm"
              onClick={handleCalendarPrev}
              aria-label="上一段时间范围"
            >
              上一段
            </button>
            <button
              type="button"
              className="pim-button-secondary px-3 py-1.5 text-sm"
              onClick={handleCalendarToday}
            >
              今天
            </button>
            <button
              type="button"
              className="pim-button-secondary px-3 py-1.5 text-sm"
              onClick={handleCalendarNext}
              aria-label="下一段时间范围"
            >
              下一段
            </button>
            <span className="min-w-24 text-sm font-semibold text-slate-700">{visibleTitle}</span>
          </div>
        }
        actions={
          <SegmentedControl
            value={mode}
            options={CALENDAR_MODE_OPTIONS}
            onChange={handleModeChange}
            ariaLabel="切换日历视图"
          />
        }
      />

      <section className="calendar-board pim-panel min-h-0 flex-1 overflow-hidden p-3">
        <FullCalendar
          key={mode}
          ref={calendarRef}
          plugins={mode === 'timeline'
            ? [timeGridPlugin, interactionPlugin]
            : [dayGridPlugin, interactionPlugin]}
          initialView={mode === 'timeline' ? 'timeGridDay' : 'dayGridMonth'}
          initialDate={toDateStr(activeDate)}
          events={calendarEvents}
          locale="zh-cn"
          height="100%"
          headerToolbar={false}
          eventContent={renderCalendarEvent}
          dayMaxEvents={mode === 'month' ? 3 : undefined}
          slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
          eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
          allDaySlot={mode === 'timeline' ? false : undefined}
          slotMinTime={mode === 'timeline' ? '00:00:00' : undefined}
          slotMaxTime={mode === 'timeline' ? '24:00:00' : undefined}
          selectable
          selectMirror
          editable={false}
          droppable
          datesSet={handleDatesSet}
          select={handleDateSelect}
          eventClick={handleEventClick}
          drop={handleExternalDrop}
        />
      </section>

      <TaskEditorDialog
        open={taskEditorOpen}
        onClose={() => setTaskEditorOpen(false)}
        task={editingTask}
        defaultDtStart={taskDefaultDtStart}
      />
      <EventEditorDialog
        open={eventEditorOpen}
        onClose={() => setEventEditorOpen(false)}
        event={editingEvent}
        defaultStart={eventDefaultStart}
        defaultEnd={eventDefaultEnd}
      />
    </div>
  );
}

function normalizeMode(value: string | null): CalendarMode {
  return value === 'month' ? 'month' : 'timeline';
}

function rangeForDate(date: Date, mode: CalendarMode) {
  if (mode === 'month') {
    return {
      start: toDateStr(new Date(date.getFullYear(), date.getMonth(), 1)),
      end: toDateStr(new Date(date.getFullYear(), date.getMonth() + 1, 1)),
    };
  }

  return {
    start: toDateStr(date),
    end: toDateStr(new Date(date.getTime() + 86400000)),
  };
}

function formatCalendarTitle(date: Date, mode: CalendarMode): string {
  return mode === 'month' ? format(date, 'yyyy年M月') : format(date, 'yyyy年M月d日');
}

function renderCalendarEvent(arg: EventContentArg) {
  const props = arg.event.extendedProps as CalendarEventInput['extendedProps'];
  const isTask = props.type === 'task';
  const raw = props.raw as Partial<TaskResponse & EventResponse>;
  const priority = isTask ? (raw.priority ?? 0) : 0;
  const toneClass = isTask
    ? priority === 1
      ? 'calendar-event--danger'
      : priority === 3
        ? 'calendar-event--quiet'
        : 'calendar-event--warning'
    : 'calendar-event--primary';

  return (
    <div className={`calendar-event-card ${toneClass}`}>
      <span className="calendar-event-dot" />
      <span className="calendar-event-title">{arg.event.title}</span>
      {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
    </div>
  );
}

function toDateStr(date: Date): string {
  return format(date, 'yyyy-MM-dd');
}

function toLocalDateTimeInputValue(date: Date): string {
  return format(date, "yyyy-MM-dd'T'HH:mm");
}

function taskColor(priority: number): string {
  if (priority === 1) return '#E53935';
  if (priority === 3) return '#14B8A6';
  return '#F59E0B';
}

function buildCalendarEvents(events: EventResponse[], tasks: TaskResponse[]): CalendarEventInput[] {
  return [
    ...events.map(event => ({
      id: event.id,
      title: event.title,
      start: event.dtStart,
      end: event.dtEnd,
      backgroundColor: '#2563EB',
      borderColor: '#2563EB',
      extendedProps: {
        type: 'event' as const,
        raw: event,
      },
    })),
    ...tasks.filter(task => task.dtStart).map(task => {
      const color = taskColor(task.priority);

      return {
        id: task.id,
        title: task.title,
        start: task.dtStart,
        end: task.due,
        backgroundColor: color,
        borderColor: color,
        extendedProps: {
          type: 'task' as const,
          raw: task,
        },
      };
    }),
  ];
}

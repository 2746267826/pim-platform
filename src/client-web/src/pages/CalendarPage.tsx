import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin, { Draggable } from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import luxon3Plugin from '@fullcalendar/luxon3';
import type { DateSelectArg, DatesSetArg, EventClickArg, EventContentArg, EventInput, EventMountArg } from '@fullcalendar/core';
import { format } from 'date-fns';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Repeat2 } from 'lucide-react';
import { getCalendarLayers, getCalendars, getEvents, getTasks, planTask } from '../api/calendar';
import { useCalendarVisibility } from '../context/CalendarVisibilityContext';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import CalendarLayerToolbar from '../components/schedule/CalendarLayerToolbar';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';
import type { CalendarLayerId, CalendarLayerItem, CalendarResponse, EventResponse, TaskResponse } from '../types';
import { looksLikeHtml, sanitizeDescriptionHtml } from '../utils/safeHtml';

type CalendarMode = 'timeline' | 'month';
type CalendarLayerToggleId = CalendarLayerId;

type CalendarDropArg = {
  draggedEl: HTMLElement;
  date: Date;
};

type CalendarEventProps =
  | {
      type: 'event';
      raw: EventResponse;
      accentColor: string;
      calendarLabel: string;
    }
  | {
      type: 'task';
      raw: TaskResponse;
      accentColor: string;
      calendarLabel: string;
    }
  | {
      type: 'layer';
      raw: CalendarLayerItem;
      accentColor: string;
    };

type CalendarEventInput = EventInput & {
  extendedProps: CalendarEventProps;
};

const CALENDAR_MODE_OPTIONS: Array<{ value: CalendarMode; label: string }> = [
  { value: 'timeline', label: '时间轴' },
  { value: 'month', label: '月视图' },
];

const CALENDAR_LAYER_OPTIONS: Array<{ value: CalendarLayerToggleId; label: string }> = [
  { value: 'events', label: '日程' },
  { value: 'task-segments', label: '任务时间段' },
  { value: 'habits', label: '习惯' },
  { value: 'availability', label: '可用时间' },
  { value: 'ai-placeholders', label: '智能占位' },
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
  const [planTaskError, setPlanTaskError] = useState<string | null>(null);
  const [enabledLayerIds, setEnabledLayerIds] = useState<CalendarLayerToggleId[]>(['events', 'task-segments']);
  const [outlookOnly, setOutlookOnly] = useState(false);
  const calendarRef = useRef<FullCalendar>(null);
  const pageRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();

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

  const { data: calendars = [] } = useQuery({
    queryKey: ['calendars'],
    queryFn: () => getCalendars(),
  });

  const enabledLayerKey = enabledLayerIds.join(',');
  const enabledLayerSet = useMemo(() => new Set(enabledLayerIds), [enabledLayerIds]);

  const { data: calendarLayerData } = useQuery({
    queryKey: ['calendar-layers', visibleRange.start, visibleRange.end, enabledLayerKey, outlookOnly],
    queryFn: () => getCalendarLayers({
      start: visibleRange.start,
      end: visibleRange.end,
      layers: enabledLayerIds,
      outlookOnly,
    }),
    refetchInterval: 60_000,
  });

  const { hiddenCalendarIds } = useCalendarVisibility();
  const planTaskMutation = useMutation({
    mutationFn: ({ task, plannedStart }: { task: TaskResponse; plannedStart: string }) =>
      planTask(task.id, {
        plannedStart,
        plannedEnd: getPlannedEndForDrop(task, plannedStart),
      }),
    onMutate: () => setPlanTaskError(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['tasks-paged'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['today-sections'] });
      queryClient.invalidateQueries({ queryKey: ['today-section'] });
    },
    onError: error => {
      setPlanTaskError(error instanceof Error ? error.message : '任务计划失败，请稍后再试。');
    },
  });
  const { mutate: mutatePlanTask } = planTaskMutation;

  const calendarEvents = useMemo(() => {
    const visibleEvents = hiddenCalendarIds.size > 0
      ? events.filter(event => !hiddenCalendarIds.has(event.calendarId))
      : events;
    const layerItems = calendarLayerData?.items ?? [];

    return buildCalendarEvents(
      enabledLayerSet.has('events') ? visibleEvents : [],
      tasks,
      layerItems,
      enabledLayerSet,
      calendars,
    );
  }, [calendarLayerData?.items, calendars, enabledLayerSet, events, hiddenCalendarIds, tasks]);

  function toggleCalendarLayer(layerId: CalendarLayerToggleId) {
    setEnabledLayerIds(current => (
      current.includes(layerId)
        ? current.filter(item => item !== layerId)
        : [...current, layerId]
    ));
  }

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

    if (props.type === 'layer') {
      return;
    }

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

  const cardObserverMap = useRef(new WeakMap<HTMLElement, () => void>());

  const handleEventMount = useCallback((info: EventMountArg) => {
    cardObserverMap.current.get(info.el)?.();
    cardObserverMap.current.delete(info.el);

    const props = info.event.extendedProps as CalendarEventProps;
    if (props.type === 'layer') return;
    let mountObserver: MutationObserver | undefined;
    let resizeObserver: ResizeObserver | undefined;

    const attachResizeObserver = () => {
      if (resizeObserver) return true;
      const cardEl = info.el.querySelector<HTMLElement>('[data-calendar-event-card]');
      if (!cardEl) return false;

      const computeLevel = () => {
        const height = info.el.clientHeight;
        let level = 1;
        if (height >= 80) level = 5;
        else if (height >= 64) level = 4;
        else if (height >= 48) level = 3;
        else if (height >= 32) level = 2;
        cardEl.dataset.contentLevel = String(level);
      };

      computeLevel();
      resizeObserver = new ResizeObserver(computeLevel);
      resizeObserver.observe(info.el);
      mountObserver?.disconnect();
      return true;
    };

    if (!attachResizeObserver()) {
      mountObserver = new MutationObserver(attachResizeObserver);
      mountObserver.observe(info.el, { childList: true, subtree: true });
    }

    cardObserverMap.current.set(info.el, () => {
      mountObserver?.disconnect();
      resizeObserver?.disconnect();
    });
  }, []);

  const handleEventUnmount = useCallback((info: EventMountArg) => {
    const cleanup = cardObserverMap.current.get(info.el);
    if (cleanup) {
      cleanup();
      cardObserverMap.current.delete(info.el);
    }
  }, []);

  const handleExternalDrop = useCallback((dropInfo: CalendarDropArg) => {
    const taskId = dropInfo.draggedEl.dataset.taskId;
    const task = tasks.find(item => item.id === taskId);
    if (!task) return;

    const plannedStart = toLocalDateTimeInputValue(dropInfo.date);
    mutatePlanTask({ task, plannedStart });
  }, [mutatePlanTask, tasks]);

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

      <CalendarLayerToolbar
        options={CALENDAR_LAYER_OPTIONS}
        activeLayerIds={enabledLayerIds}
        outlookOnly={outlookOnly}
        onToggleLayer={toggleCalendarLayer}
        onToggleOutlookOnly={setOutlookOnly}
      />

      <section className="calendar-board pim-panel min-h-0 flex-1 overflow-hidden p-3">
        {planTaskError && (
          <div className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {planTaskError}
          </div>
        )}
        <FullCalendar
          key={mode}
          ref={calendarRef}
          plugins={mode === 'timeline'
            ? [timeGridPlugin, interactionPlugin, luxon3Plugin]
            : [dayGridPlugin, interactionPlugin, luxon3Plugin]}
          initialView={mode === 'timeline' ? 'timeGridDay' : 'dayGridMonth'}
          initialDate={toDateStr(activeDate)}
          events={calendarEvents}
          locale="zh-cn"
          timeZone="local"
          height="100%"
          headerToolbar={false}
          eventContent={renderCalendarEvent}
          dayMaxEvents={mode === 'month' ? true : undefined}
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
          eventDidMount={handleEventMount}
          eventWillUnmount={handleEventUnmount}
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

function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength - 1) + '…';
}

function htmlToPlainText(html: string): string {
  if (!looksLikeHtml(html)) return html;
  const sanitized = sanitizeDescriptionHtml(html);
  const parsed = new DOMParser().parseFromString(sanitized, 'text/html');
  return (parsed.body.textContent ?? '').replace(/\s+/g, ' ').trim();
}

function renderCalendarEvent(arg: EventContentArg) {
  const props = arg.event.extendedProps as CalendarEventInput['extendedProps'];
  if (props.type === 'layer') {
    const raw = props.raw;
    const isTaskSegment = raw.layer === 'task-segments';
    const toneClass = isTaskSegment
      ? 'calendar-event--quiet'
      : raw.requiresConfirmation
        ? 'calendar-event--warning'
        : 'calendar-event--primary';

    return (
      <div className={`calendar-event-card ${toneClass} ${isTaskSegment ? 'pim-calendar-layer-task-segment' : 'pim-calendar-layer'}`}>
        <span className="calendar-event-dot" />
        <span className="calendar-event-title">{arg.event.title}</span>
        {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
      </div>
    );
  }

  const { accentColor, calendarLabel } = props;
  const style = { '--calendar-accent': accentColor } as CSSProperties;

  if (props.type === 'event') {
    const raw = props.raw;
    const description = raw.description
      ? truncateText(htmlToPlainText(raw.description), 80)
      : undefined;

    return (
      <div className="calendar-event-card" data-calendar-event-card style={style}>
        <span className="calendar-event-dot" />
        <span className="calendar-event-title">{arg.event.title}</span>
        {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
        {raw.location && <span className="calendar-event-location">{raw.location}</span>}
        {calendarLabel && <span className="calendar-event-source">{calendarLabel}</span>}
        {description && <span className="calendar-event-description">{description}</span>}
        {raw.rrule && (
          <span className="calendar-event-rrule">
            <Repeat2 size={10} aria-label="重复" />
          </span>
        )}
      </div>
    );
  }

  const raw = props.raw;
  const description = raw.description
    ? truncateText(htmlToPlainText(raw.description), 80)
    : undefined;

  return (
    <div className="calendar-event-card" data-calendar-event-card style={style}>
      <span className="calendar-event-dot" />
      <span className="calendar-event-title">{arg.event.title}</span>
      {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
      {calendarLabel && <span className="calendar-event-source">{calendarLabel}</span>}
      {description && <span className="calendar-event-description">{description}</span>}
    </div>
  );
}

function toDateStr(date: Date): string {
  return format(date, 'yyyy-MM-dd');
}

function toLocalDateTimeInputValue(date: Date): string {
  return format(date, "yyyy-MM-dd'T'HH:mm");
}

function calendarDisplayName(cal: CalendarResponse | undefined): string | undefined {
  if (!cal) return undefined;
  return cal.outlookCalendarBindingId ? `${cal.name} (Outlook)` : cal.name;
}

function eventSourceDisplayName(source: string): string {
  if (source === 'outlook') return 'Outlook';
  if (source === 'outlook-ics') return 'Outlook ICS';
  return '日程';
}

export function buildCalendarEvents(
  events: EventResponse[],
  tasks: TaskResponse[],
  layerItems: CalendarLayerItem[],
  enabledLayerSet: Set<CalendarLayerToggleId>,
  calendars: CalendarResponse[] = [],
): CalendarEventInput[] {
  const calMap = new Map(calendars.map(c => [c.id, c]));

  return [
    ...events.map(event => {
      const cal = calMap.get(event.calendarId);
      const accentColor = cal?.color ?? '#2563eb';
      const calendarLabel = calendarDisplayName(cal) ?? eventSourceDisplayName(event.source);

      return {
        id: event.id,
        title: event.title,
        start: event.dtStart,
        end: event.dtEnd,
        allDay: event.isAllDay,
        extendedProps: {
          type: 'event' as const,
          raw: event,
          accentColor,
          calendarLabel,
        },
      };
    }),
    ...(enabledLayerSet.has('task-segments') ? tasks : []).filter(task => task.dtStart).map(task => {
      const cal = task.calendarId ? calMap.get(task.calendarId) : undefined;
      const accentColor = task.priority === 1 ? '#ef4444'
        : task.priority === 3 ? '#14b8a6'
        : '#f59e0b';
      const calendarLabel = calendarDisplayName(cal) ?? '任务';

      return {
        id: task.id,
        title: task.title,
        start: task.dtStart,
        end: task.plannedEnd || task.due,
        extendedProps: {
          type: 'task' as const,
          raw: task,
          accentColor,
          calendarLabel,
        },
      };
    }),
    ...layerItems
      .filter(item => enabledLayerSet.has(item.layer as CalendarLayerToggleId))
      .filter(item => item.layer !== 'events')
      .map(item => ({
        id: `layer-${item.layer}-${item.id}`,
        title: item.title,
        start: item.startsAt,
        end: item.endsAt,
        backgroundColor: 'transparent',
        borderColor: 'transparent',
        classNames: item.layer === 'task-segments'
          ? ['pim-calendar-layer-task-segment']
          : ['pim-calendar-layer'],
        extendedProps: {
          type: 'layer' as const,
          raw: item,
          accentColor: item.color,
        },
      })),
  ];
}

function getPlannedEndForDrop(task: TaskResponse, plannedStart: string): string | undefined {
  const plannedStartDate = parseCalendarDate(plannedStart);
  if (!plannedStartDate) return task.due;

  const existingPlannedEnd = parseCalendarDate(task.plannedEnd);
  const existingPlannedStart = parseCalendarDate(task.dtStart);
  if (existingPlannedEnd && existingPlannedStart) {
    const durationMs = existingPlannedEnd.getTime() - existingPlannedStart.getTime();
    if (durationMs > 0) return toLocalDateTimeInputValue(new Date(plannedStartDate.getTime() + durationMs));
  }

  const estimatedDurationMs = parseTimeSpanMs(task.estimatedDuration);
  if (estimatedDurationMs && estimatedDurationMs > 0) {
    return toLocalDateTimeInputValue(new Date(plannedStartDate.getTime() + estimatedDurationMs));
  }

  return task.due;
}

function parseCalendarDate(value?: string): Date | null {
  if (!value) return null;

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function parseTimeSpanMs(value?: string): number | null {
  if (!value) return null;

  const match = /^(?:(\d+)\.)?(\d+):([0-5]\d):([0-5]\d)(?:\.(\d{1,7}))?$/.exec(value);
  if (!match) return null;

  const [, days = '0', hours, minutes, seconds, fraction = ''] = match;
  const baseMs = (
    Number(days) * 24 * 60 * 60
    + Number(hours) * 60 * 60
    + Number(minutes) * 60
    + Number(seconds)
  ) * 1000;
  const fractionMs = fraction
    ? Number(`0.${fraction}`) * 1000
    : 0;

  return baseMs + fractionMs;
}

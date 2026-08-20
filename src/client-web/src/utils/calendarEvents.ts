import type { EventInput } from '@fullcalendar/core';
import type { CalendarLayerId, CalendarLayerItem, CalendarResponse, EventResponse, TaskResponse } from '../types';

export type CalendarEventProps =
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

export type CalendarEventInput = EventInput & {
  extendedProps: CalendarEventProps;
};

type CalendarLayerToggleId = CalendarLayerId;

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

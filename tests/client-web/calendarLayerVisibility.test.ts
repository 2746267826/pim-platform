import assert from 'node:assert/strict';
import { buildCalendarEvents } from '../../src/client-web/src/pages/CalendarPage';
import type { CalendarLayerItem, CalendarResponse, EventResponse, TaskResponse } from '../../src/client-web/src/types';

const event: EventResponse = {
  id: 'event-1',
  calendarId: 'calendar-1',
  uid: 'event-uid-1',
  title: 'Planning review',
  dtStart: '2026-07-08T09:00:00',
  dtEnd: '2026-07-08T09:30:00',
  status: 'Confirmed',
  source: 'manual',
};

const plannedTask: TaskResponse = {
  id: 'task-1',
  title: 'Draft agenda',
  priority: 2,
  dtStart: '2026-07-08T10:00:00',
  plannedEnd: '2026-07-08T10:30:00',
  status: 'Todo',
  isInbox: false,
};

const taskSegmentLayerItem: CalendarLayerItem = {
  id: 'segment-1',
  layer: 'task-segments',
  objectType: 'task-segment',
  objectId: 'segment-1',
  title: 'Draft agenda segment',
  startsAt: '2026-07-08T11:00:00',
  endsAt: '2026-07-08T11:30:00',
  source: 'manual',
  status: 'Planned',
  color: '#2563eb',
  requiresConfirmation: false,
};

const calendars: CalendarResponse[] = [
  { id: 'calendar-1', name: 'Work', color: '#2563eb', kind: 'calendar', isDefault: true, canEdit: true },
  {
    id: 'calendar-outlook',
    name: 'Team',
    color: '#0f766e',
    kind: 'calendar',
    isDefault: false,
    outlookCalendarBindingId: 'binding-1',
    canEdit: true,
  },
];

const outlookEvent: EventResponse = {
  ...event,
  id: 'event-outlook',
  calendarId: 'calendar-outlook',
  uid: 'event-outlook-uid',
  title: 'Outlook planning review',
  source: 'outlook',
};

const onlyEvents = buildCalendarEvents(
  [event],
  [plannedTask],
  [taskSegmentLayerItem],
  new Set(['events']),
  calendars,
);

assert.deepEqual(
  onlyEvents.map(item => item.title),
  ['Planning review'],
);

const evtResult = onlyEvents[0];
const evtProps = evtResult.extendedProps as Record<string, unknown>;
assert.equal(evtResult.backgroundColor, undefined,
  'Native event must not set hardcoded backgroundColor');
assert.equal(evtResult.borderColor, undefined,
  'Native event must not set hardcoded borderColor');
assert.equal(evtProps.type, 'event');
assert.equal(evtProps.accentColor, '#2563eb',
  'Event must carry calendar accent color in extendedProps');
assert.equal(evtProps.calendarLabel, 'Work',
  'Event must carry calendar label in extendedProps');

const [outlookResult] = buildCalendarEvents(
  [outlookEvent],
  [],
  [],
  new Set(['events']),
  calendars,
);
const outlookProps = outlookResult.extendedProps as Record<string, unknown>;
assert.equal(outlookProps.calendarLabel, 'Team (Outlook)',
  'Outlook calendar label must extend the native calendar label');

const [missingCalendarResult] = buildCalendarEvents(
  [{ ...event, calendarId: 'missing-calendar' }],
  [],
  [],
  new Set(['events']),
  calendars,
);
const missingCalendarProps = missingCalendarResult.extendedProps as Record<string, unknown>;
assert.equal(missingCalendarProps.calendarLabel, '日程',
  'Missing calendar metadata must not expose the manual source identifier');

const eventsAndTaskSegments = buildCalendarEvents(
  [event],
  [plannedTask],
  [taskSegmentLayerItem],
  new Set(['events', 'task-segments']),
  calendars,
);

assert.deepEqual(
  eventsAndTaskSegments.map(item => item.title),
  ['Planning review', 'Draft agenda', 'Draft agenda segment'],
);

const taskResult = eventsAndTaskSegments[1];
const taskProps = taskResult.extendedProps as Record<string, unknown>;
assert.equal(taskResult.backgroundColor, undefined,
  'Task must not set hardcoded backgroundColor');
assert.equal(taskResult.borderColor, undefined,
  'Task must not set hardcoded borderColor');
assert.equal(taskProps.type, 'task');
assert.equal(taskProps.accentColor, '#f59e0b',
  'Priority-2 task must carry priority accent in extendedProps');
assert.equal(taskProps.calendarLabel, '任务',
  'Task without calendarId must carry task label');

const layerResult = eventsAndTaskSegments[2];
const layerProps = layerResult.extendedProps as Record<string, unknown>;
assert.equal(layerResult.backgroundColor, 'transparent',
  'Layer item must keep transparent backgroundColor');
assert.equal(layerResult.borderColor, 'transparent',
  'Layer item must keep transparent borderColor');
assert.equal(layerProps.type, 'layer');
assert.equal(layerProps.accentColor, '#2563eb',
  'Layer item must carry its fixture color as accentColor');

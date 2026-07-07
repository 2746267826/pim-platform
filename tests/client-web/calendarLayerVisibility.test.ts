import assert from 'node:assert/strict';
import { buildCalendarEvents } from '../../src/client-web/src/pages/CalendarPage';
import type { CalendarLayerItem, EventResponse, TaskResponse } from '../../src/client-web/src/types';

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

const onlyEvents = buildCalendarEvents(
  [event],
  [plannedTask],
  [taskSegmentLayerItem],
  new Set(['events']),
);

assert.deepEqual(
  onlyEvents.map(item => item.title),
  ['Planning review'],
);

const eventsAndTaskSegments = buildCalendarEvents(
  [event],
  [plannedTask],
  [taskSegmentLayerItem],
  new Set(['events', 'task-segments']),
);

assert.deepEqual(
  eventsAndTaskSegments.map(item => item.title),
  ['Planning review', 'Draft agenda', 'Draft agenda segment'],
);

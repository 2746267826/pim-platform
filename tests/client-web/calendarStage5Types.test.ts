import type {
  CalendarOperationResult,
  CalendarRecycleBinItem,
  CalendarRestorePreviewResponse,
  EventResponse,
  ImportReport,
  TaskResponse,
} from '../../src/client-web/src/types';

const operationResult: CalendarOperationResult = {
  operation: 'delete',
  operationId: 'op-1',
  affectedCount: 1,
  affectedIds: ['event-1'],
  samples: [
    {
      id: 'event-1',
      title: 'Planning',
      type: 'event',
    },
  ],
};

const recycleItem: CalendarRecycleBinItem = {
  id: 'trash-1',
  itemId: 'event-1',
  type: 'event',
  title: 'Planning',
  deletedAt: '2026-05-27T00:00:00Z',
  deletedBy: 'user-1',
  calendarId: 'cal-1',
  calendarName: 'Work',
};

const restorePreview: CalendarRestorePreviewResponse = {
  item: recycleItem,
  canRestore: false,
  conflicts: [
    {
      code: 'calendar_missing',
      message: 'Calendar is missing',
      severity: 'warning',
    },
  ],
};

const event: EventResponse = {
  id: 'event-1',
  calendarId: 'cal-1',
  uid: 'uid-1',
  title: 'Planning',
  dtStart: '2026-05-27T00:00:00Z',
  dtEnd: '2026-05-27T01:00:00Z',
  status: 'confirmed',
  source: 'ics',
  isAllDay: false,
  timeZoneId: 'Asia/Shanghai',
  sourceTimeZoneId: 'UTC',
  sourceUid: 'source-uid-1',
  externalMetadataJson: '{}',
  recurrenceId: 'recurrence-1',
  exDatesJson: '[]',
  recurrenceMetadataJson: '{}',
};

const importReport: ImportReport = {
  imported: 1,
  updated: 0,
  skipped: 1,
  skippedItems: [
    {
      uid: 'uid-2',
      reason: 'duplicate',
      title: 'Duplicate event',
    },
  ],
};

const task: TaskResponse = {
  id: 'task-1',
  calendarId: 'cal-1',
  title: 'Draft plan',
  priority: 1,
  status: 'todo',
  isInbox: false,
  plannedEnd: '2026-05-27T01:00:00Z',
};

// @ts-expect-error EventResponse intentionally excludes raw ICS payloads.
event.sourceIcsComponent = 'BEGIN:VEVENT';

void operationResult;
void restorePreview;
void importReport;
void task;

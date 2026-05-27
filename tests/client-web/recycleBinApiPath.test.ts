import assert from 'node:assert/strict';
import { calendarApiPaths } from '../../src/client-web/src/api/calendar';

assert.equal(calendarApiPaths.recycleBin(), '/calendar/recycle-bin');
assert.equal(
  calendarApiPaths.recycleBin({ type: 'event', search: 'plan', page: 2, pageSize: 20 }),
  '/calendar/recycle-bin?type=event&search=plan&page=2&pageSize=20'
);
assert.equal(
  calendarApiPaths.recycleRestorePreview('event', 'abc'),
  '/calendar/recycle-bin/event/abc/restore-preview'
);
assert.equal(
  calendarApiPaths.recycleRestore('event', 'abc'),
  '/calendar/recycle-bin/event/abc/restore'
);
assert.equal(calendarApiPaths.taskPlan('abc'), '/calendar/tasks/abc/plan');
assert.equal(calendarApiPaths.taskBatchUpdate(), '/calendar/tasks/batch-update');
assert.equal(calendarApiPaths.taskBatchDelete(), '/calendar/tasks/batch-delete');

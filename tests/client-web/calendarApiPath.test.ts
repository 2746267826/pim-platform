import assert from 'node:assert/strict';
import { buildTasksPath, calendarApiPaths } from '../../src/client-web/src/api/calendar';

assert.equal(buildTasksPath(), '/calendar/tasks');
assert.equal(buildTasksPath(false), '/calendar/tasks');
assert.equal(buildTasksPath(true), '/calendar/tasks?inbox=true');
assert.equal(calendarApiPaths.calendarDeletePreview('abc'), '/calendar/calendars/abc/delete-preview');
assert.equal(calendarApiPaths.eventBatchDelete(), '/calendar/events/batch-delete');

import assert from 'node:assert/strict';
import { buildTasksPath } from '../../src/client-web/src/api/calendar';

assert.equal(buildTasksPath(), '/calendar/tasks');
assert.equal(buildTasksPath(false), '/calendar/tasks');
assert.equal(buildTasksPath(true), '/calendar/tasks?inbox=true');

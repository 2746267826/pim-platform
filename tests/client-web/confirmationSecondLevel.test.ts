import assert from 'node:assert/strict';
import { getConfirmActionState } from '../../src/client-web/src/pages/ConfirmationsPage';

assert.deepEqual(
  getConfirmActionState(false, false),
  { label: 'Confirm', requiresArm: false },
);

assert.deepEqual(
  getConfirmActionState(true, false),
  { label: 'Confirm', requiresArm: true },
);

assert.deepEqual(
  getConfirmActionState(true, true),
  { label: 'Confirm final', requiresArm: false },
);

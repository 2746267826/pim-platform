import assert from 'node:assert/strict';
import { mobileApiPaths } from '../../src/client-web/src/api/mobile';

const day = '2026-07-06';
const deviceId = 'phone/main';
const start = '2026-07-06T00:00:00Z';
const end = '2026-07-06T23:59:59Z';

assert.equal(mobileApiPaths.devices, '/mobile/devices');
assert.equal(mobileApiPaths.summary(day), '/mobile/summary?date=2026-07-06');
assert.equal(
  mobileApiPaths.summary(day, deviceId),
  '/mobile/summary?date=2026-07-06&deviceId=phone%2Fmain',
);
assert.equal(mobileApiPaths.timeline(day), '/mobile/timeline?date=2026-07-06');
assert.equal(
  mobileApiPaths.timeline(day, deviceId),
  '/mobile/timeline?date=2026-07-06&deviceId=phone%2Fmain',
);
assert.equal(
  mobileApiPaths.locations(start, end),
  '/mobile/location/history?start=2026-07-06T00%3A00%3A00Z&end=2026-07-06T23%3A59%3A59Z&maxAccuracyMeters=50',
);
assert.equal(
  mobileApiPaths.locations(start, end, deviceId, 10),
  '/mobile/location/history?start=2026-07-06T00%3A00%3A00Z&end=2026-07-06T23%3A59%3A59Z&maxAccuracyMeters=10&deviceId=phone%2Fmain',
);
assert.equal(
  mobileApiPaths.locationHistory({ start, end, deviceId, maxAccuracyMeters: 25 }),
  '/mobile/location/history?start=2026-07-06T00%3A00%3A00Z&end=2026-07-06T23%3A59%3A59Z&maxAccuracyMeters=25&deviceId=phone%2Fmain',
);
assert.equal(mobileApiPaths.quality(), '/mobile/quality');
assert.equal(
  mobileApiPaths.quality(day, deviceId),
  '/mobile/quality?date=2026-07-06&deviceId=phone%2Fmain',
);

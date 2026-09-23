import assert from 'node:assert/strict';
import type { MobileLivenessOverview } from '../../src/client-web/src/api/mobile';
import {
  formatCoverage,
  formatPayload,
  silenceSeverityClass,
  groupDeviceBlocks,
} from '../../src/client-web/src/components/mobile/deviceLivenessModel';

assert.equal(formatCoverage(0.107), '10.7%');
assert.equal(formatCoverage(null), '—');
assert.equal(silenceSeverityClass('warning'), 'bg-amber-100 text-amber-900');
assert.equal(silenceSeverityClass('critical'), 'bg-red-100 text-red-900');
assert.equal(silenceSeverityClass('none'), '');
assert.equal(formatPayload('{"a":1}'), '{\n  "a": 1\n}');
assert.equal(formatPayload('不是 JSON'), '不是 JSON');

const groups = groupDeviceBlocks({
  rangeStartUtc: '',
  rangeEndUtc: '',
  expectedHeartbeatIntervalMinutes: 15,
  phones: [],
  tablets: [],
  unclassified: [],
} satisfies MobileLivenessOverview);
assert.deepEqual(groups.map(group => group.title), ['手机', '平板', '未分类机型']);
assert.deepEqual(groups.map(group => group.devices.length), [0, 0, 0]);

console.error('PASS: mobileLivenessModel');

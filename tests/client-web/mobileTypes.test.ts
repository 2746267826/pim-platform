import assert from 'node:assert/strict';
import {
  getMobileDevices,
  getMobileLocationHistory,
  getMobileQuality,
  getMobileSummary,
  getMobileTimeline,
} from '../../src/client-web/src/api/mobile';
import type {
  MobileAppUsageSummary,
  MobileDevice,
  MobileLocationHistory,
  MobileLocationHistoryParams,
  MobileLocationPoint,
  MobileQuality,
  MobileQualityComponent,
  MobileQualityIssue,
  MobileSummary,
  MobileSyncBatchSummary,
  MobileTimeline,
  MobileTimelineFallback,
  MobileTimelineItem,
  MobileTimelineSession,
} from '../../src/client-web/src/api/mobile';

function acceptsDeviceReturn(result: ReturnType<typeof getMobileDevices>): Promise<MobileDevice[]> {
  return result;
}

function acceptsSummaryReturn(result: ReturnType<typeof getMobileSummary>): Promise<MobileSummary> {
  return result;
}

function acceptsTimelineReturn(result: ReturnType<typeof getMobileTimeline>): Promise<MobileTimeline> {
  return result;
}

function acceptsLocationHistoryReturn(
  result: ReturnType<typeof getMobileLocationHistory>,
): Promise<MobileLocationHistory> {
  return result;
}

function acceptsQualityReturn(result: ReturnType<typeof getMobileQuality>): Promise<MobileQuality> {
  return result;
}

void acceptsDeviceReturn;
void acceptsSummaryReturn;
void acceptsTimelineReturn;
void acceptsLocationHistoryReturn;
void acceptsQualityReturn;

const device: MobileDevice = {
  id: 'device-row-1',
  deviceId: 'phone-main',
  androidIdHash: 'hash-1',
  displayName: 'Pixel 9',
  manufacturer: 'Google',
  brand: 'google',
  model: 'Pixel 9',
  androidVersion: '16',
  sdkInt: 36,
  appVersion: '1.0.0',
  metadataJson: '{}',
  firstSeenAt: '2026-07-06T00:00:00Z',
  lastSeenAt: '2026-07-06T08:00:00Z',
  lastHeartbeatAt: '2026-07-06T08:01:00Z',
  lastSyncAt: '2026-07-06T08:02:00Z',
  isActive: true,
};

const app: MobileAppUsageSummary = {
  packageName: 'com.example.reader',
  displayName: 'Reader',
  categoryName: 'Reading',
  foregroundSeconds: 1800,
  sessionCount: 2,
  launchCount: 3,
  lastUsedAt: '2026-07-06T08:00:00Z',
  source: 'events',
  share: 0.5,
};

const syncBatch: MobileSyncBatchSummary = {
  id: 'batch-1',
  deviceId: device.deviceId,
  clientBatchId: 'client-batch-1',
  sourceWindowStartUtc: '2026-07-06T00:00:00Z',
  sourceWindowEndUtc: '2026-07-06T08:00:00Z',
  submittedAtUtc: '2026-07-06T08:02:00Z',
  status: 'succeeded',
  acceptedEventCount: 4,
  skippedEventCount: 0,
  acceptedLocationCount: 1,
  rejectedLocationCount: 0,
  errorMessage: null,
};

const summary: MobileSummary = {
  date: '2026-07-06',
  deviceId: device.deviceId,
  generatedAt: '2026-07-06T08:05:00Z',
  totalForegroundSeconds: 3600,
  fallbackForegroundSeconds: 600,
  appSwitchCount: 5,
  appsUsed: 2,
  completeness: 0.95,
  lastSyncAt: syncBatch.submittedAtUtc,
  appRanking: [app],
  syncBatches: [syncBatch],
  qualityIssueCount: 1,
};

const session: MobileTimelineSession = {
  kind: 'session',
  id: 'session-1',
  deviceId: device.deviceId,
  packageName: app.packageName,
  displayName: app.displayName,
  start: '2026-07-06T07:00:00Z',
  end: '2026-07-06T07:20:00Z',
  durationSeconds: 1200,
  source: 'events',
  confidence: 0.98,
};

const fallback: MobileTimelineFallback = {
  kind: 'fallback',
  id: 'fallback-1',
  deviceId: device.deviceId,
  packageName: 'com.example.music',
  displayName: 'Music',
  start: '2026-07-06T07:20:00Z',
  end: '2026-07-06T07:30:00Z',
  durationSeconds: 600,
  source: 'fallback',
  reason: 'usage-events-missing',
};

const timelineItems: MobileTimelineItem[] = [session, fallback];
const timeline: MobileTimeline = {
  date: '2026-07-06',
  deviceId: device.deviceId,
  generatedAt: '2026-07-06T08:05:00Z',
  sessions: [session],
  fallbackSummaries: [fallback],
  items: timelineItems,
};

const locationPoint: MobileLocationPoint = {
  id: 'point-1',
  deviceId: device.deviceId,
  recordedAtUtc: '2026-07-06T07:30:00Z',
  submittedAtUtc: '2026-07-06T07:31:00Z',
  latitude: 31.2304,
  longitude: 121.4737,
  horizontalAccuracyMeters: 9.4,
  provider: 'gps',
  sourceKind: 'manual',
  altitudeMeters: 10,
  verticalAccuracyMeters: 3,
  speedMetersPerSecond: null,
  speedAccuracyMetersPerSecond: null,
  bearingDegrees: null,
  bearingAccuracyDegrees: null,
  isAutoSubmitted: false,
  quality: 'high',
  rawJson: '{}',
};

const historyParams: MobileLocationHistoryParams = {
  start: '2026-07-06T00:00:00Z',
  end: '2026-07-06T23:59:59Z',
  deviceId: device.deviceId,
  maxAccuracyMeters: 50,
};

const history: MobileLocationHistory = {
  start: historyParams.start,
  end: historyParams.end,
  deviceId: historyParams.deviceId ?? null,
  maxAccuracyMeters: historyParams.maxAccuracyMeters ?? 50,
  points: [locationPoint],
};

const component: MobileQualityComponent = {
  key: 'mobile-location',
  name: 'Mobile location capture',
  status: 'Warning',
  message: 'One rejected point',
  checkedAt: '2026-07-06T08:05:00Z',
  details: { rejectedLocationCount: '1' },
};

const issue: MobileQualityIssue = {
  code: 'mobile-location-rejected',
  severity: 'Warning',
  componentKey: component.key,
  message: 'A location point was rejected by accuracy policy.',
  nextStep: 'Submit a point with accuracy <= 50m.',
};

const quality: MobileQuality = {
  overallStatus: 'Warning',
  label: 'Warning',
  message: 'Mobile data needs attention.',
  checkedAt: '2026-07-06T08:05:00Z',
  components: [
    { ...component, key: 'android-heartbeat' },
    { ...component, key: 'mobile-sync' },
    { ...component, key: 'mobile-usage-coverage' },
    component,
  ],
  issues: [issue],
  nextSteps: ['Open the Android app and sync again.'],
};

assert.equal(summary.appRanking[0].packageName, app.packageName);
assert.equal(timeline.items[1].kind, 'fallback');
assert.equal(history.points[0].quality, 'high');
assert.equal(quality.components.some(item => item.key === 'mobile-location'), true);

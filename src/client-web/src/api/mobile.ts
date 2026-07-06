import { apiGet } from './client';
import type { ApiResponse, PimHealthStatus } from '../types';

type QueryValue = string | number | boolean | null | undefined;

function withQuery(path: string, entries: Array<[string, QueryValue]>) {
  const searchParams = new URLSearchParams();

  entries.forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, String(value));
    }
  });

  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}

export interface MobileLocationHistoryParams {
  start: string;
  end: string;
  deviceId?: string;
  maxAccuracyMeters?: number;
}

export const mobileApiPaths = {
  devices: '/mobile/devices',
  summary: (date: string, deviceId?: string) =>
    withQuery('/mobile/summary', [
      ['date', date],
      ['deviceId', deviceId],
    ]),
  timeline: (date: string, deviceId?: string) =>
    withQuery('/mobile/timeline', [
      ['date', date],
      ['deviceId', deviceId],
    ]),
  locations: (start: string, end: string, deviceId?: string, maxAccuracyMeters = 50) =>
    withQuery('/mobile/location/history', [
      ['start', start],
      ['end', end],
      ['maxAccuracyMeters', maxAccuracyMeters],
      ['deviceId', deviceId],
    ]),
  locationHistory: (params: MobileLocationHistoryParams) =>
    withQuery('/mobile/location/history', [
      ['start', params.start],
      ['end', params.end],
      ['maxAccuracyMeters', params.maxAccuracyMeters ?? 50],
      ['deviceId', params.deviceId],
    ]),
  quality: (date?: string, deviceId?: string) =>
    withQuery('/mobile/quality', [
      ['date', date],
      ['deviceId', deviceId],
    ]),
} as const;

export interface MobileDevice {
  id: string;
  deviceId: string;
  androidIdHash: string | null;
  displayName: string;
  manufacturer: string;
  brand: string;
  model: string;
  androidVersion: string;
  sdkInt: number;
  appVersion: string;
  metadataJson: string;
  firstSeenAt: string;
  lastSeenAt: string;
  lastHeartbeatAt: string | null;
  lastSyncAt: string | null;
  isActive: boolean;
}

export type MobileUsageSource = 'events' | 'fallback' | string;

export interface MobileAppUsageSummary {
  packageName: string;
  displayName: string;
  categoryName: string | null;
  foregroundSeconds: number;
  sessionCount: number;
  launchCount: number;
  lastUsedAt: string | null;
  source: MobileUsageSource;
  share: number;
}

export interface MobileSyncBatchSummary {
  id: string;
  deviceId: string;
  clientBatchId: string;
  sourceWindowStartUtc: string;
  sourceWindowEndUtc: string;
  submittedAtUtc: string;
  status: string;
  acceptedEventCount: number;
  skippedEventCount: number;
  acceptedLocationCount: number;
  rejectedLocationCount: number;
  errorMessage: string | null;
}

export interface MobileSummary {
  date: string;
  deviceId: string | null;
  generatedAt: string;
  totalForegroundSeconds: number;
  fallbackForegroundSeconds: number;
  appSwitchCount: number;
  appsUsed: number;
  completeness: number;
  lastSyncAt: string | null;
  appRanking: MobileAppUsageSummary[];
  syncBatches: MobileSyncBatchSummary[];
  qualityIssueCount: number;
}

interface MobileTimelineBase {
  id: string;
  deviceId: string;
  packageName: string;
  displayName: string;
  start: string;
  end: string;
  durationSeconds: number;
}

export interface MobileTimelineSession extends MobileTimelineBase {
  kind: 'session';
  source: 'events' | string;
  confidence: number;
}

export interface MobileTimelineFallback extends MobileTimelineBase {
  kind: 'fallback';
  source: 'fallback' | string;
  reason: string;
}

export type MobileTimelineItem = MobileTimelineSession | MobileTimelineFallback;

export interface MobileTimeline {
  date: string;
  deviceId: string | null;
  generatedAt: string;
  sessions: MobileTimelineSession[];
  fallbackSummaries: MobileTimelineFallback[];
  items: MobileTimelineItem[];
}

export type MobileLocationQuality = 'high' | 'usable' | 'rejected' | string;

export interface MobileLocationPoint {
  id: string;
  deviceId: string;
  recordedAtUtc: string;
  submittedAtUtc: string;
  latitude: number;
  longitude: number;
  horizontalAccuracyMeters: number;
  provider: string;
  sourceKind: string;
  altitudeMeters: number | null;
  verticalAccuracyMeters: number | null;
  speedMetersPerSecond: number | null;
  speedAccuracyMetersPerSecond: number | null;
  bearingDegrees: number | null;
  bearingAccuracyDegrees: number | null;
  isAutoSubmitted: boolean;
  quality: MobileLocationQuality;
  rawJson: string;
}

export interface MobileLocationHistory {
  start: string;
  end: string;
  deviceId: string | null;
  maxAccuracyMeters: number;
  points: MobileLocationPoint[];
}

export interface MobileQualityComponent {
  key: 'android-heartbeat' | 'mobile-sync' | 'mobile-usage-coverage' | 'mobile-location' | string;
  name: string;
  status: PimHealthStatus;
  message: string;
  checkedAt: string;
  details: Record<string, string>;
}

export interface MobileQualityIssue {
  code: string;
  severity: PimHealthStatus;
  componentKey: string;
  message: string;
  nextStep: string | null;
}

export interface MobileQuality {
  overallStatus: PimHealthStatus;
  label: string;
  message: string;
  checkedAt: string;
  components: MobileQualityComponent[];
  issues: MobileQualityIssue[];
  nextSteps: string[];
}

export function getMobileDevices(): Promise<MobileDevice[]> {
  return apiGet<ApiResponse<MobileDevice[]>>(mobileApiPaths.devices).then(r => r.data);
}

export function getMobileSummary(date: string, deviceId?: string): Promise<MobileSummary> {
  return apiGet<ApiResponse<MobileSummary>>(mobileApiPaths.summary(date, deviceId)).then(r => r.data);
}

export function getMobileTimeline(date: string, deviceId?: string): Promise<MobileTimeline> {
  return apiGet<ApiResponse<MobileTimeline>>(mobileApiPaths.timeline(date, deviceId)).then(r => r.data);
}

export function getMobileLocationHistory(params: MobileLocationHistoryParams): Promise<MobileLocationHistory> {
  return apiGet<ApiResponse<MobileLocationHistory>>(mobileApiPaths.locationHistory(params)).then(r => r.data);
}

export function getMobileQuality(date?: string, deviceId?: string): Promise<MobileQuality> {
  return apiGet<ApiResponse<MobileQuality>>(mobileApiPaths.quality(date, deviceId)).then(r => r.data);
}

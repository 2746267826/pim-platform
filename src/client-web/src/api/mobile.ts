import { apiDelete, apiGet, apiPost, apiPut } from './client';
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

function pathSegment(value: string) {
  return encodeURIComponent(value);
}

export const MOBILE_DEFAULT_TIMEZONE = 'Asia/Shanghai';

export const MOBILE_LIFE_CATEGORIES = [
  '社交沟通',
  '短视频/娱乐',
  '游戏',
  '音乐/音频',
  '阅读/资讯',
  '学习',
  '工作/生产力',
  '工具/系统',
  '浏览器/搜索',
  '出行/地图',
  '购物/外卖',
  '金融/支付',
  '健康/运动',
  '相机/创作',
  '生活服务',
  '未分类',
] as const;

export type MobileLifeCategory = (typeof MOBILE_LIFE_CATEGORIES)[number];
export type MobileAnalyticsGranularity = 'hour' | '30m' | '15m' | 'day';

export interface MobileAnalyticsQuery {
  rangeStartUtc?: string | null;
  rangeEndUtc?: string | null;
  timezone?: string | null;
  deviceId?: string | null;
  category?: MobileLifeCategory | string | null;
  packageName?: string | null;
  source?: string | null;
  includeSystemNoise?: boolean | null;
  minDurationSeconds?: number | null;
  granularity?: MobileAnalyticsGranularity | null;
  cursor?: string | null;
  pageSize?: number | null;
}

function withAnalyticsQuery(path: string, query: MobileAnalyticsQuery = {}) {
  return withQuery(path, [
    ['rangeStartUtc', query.rangeStartUtc],
    ['rangeEndUtc', query.rangeEndUtc],
    ['timezone', query.timezone],
    ['deviceId', query.deviceId],
    ['category', query.category],
    ['packageName', query.packageName],
    ['source', query.source],
    ['includeSystemNoise', query.includeSystemNoise],
    ['minDurationSeconds', query.minDurationSeconds],
    ['granularity', query.granularity],
    ['cursor', query.cursor],
    ['pageSize', query.pageSize],
  ]);
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
  analyticsOverview: (query: MobileAnalyticsQuery = {}) =>
    withAnalyticsQuery('/mobile/analytics/overview', query),
  analyticsHeatmap: (query: MobileAnalyticsQuery = {}) =>
    withAnalyticsQuery('/mobile/analytics/heatmap', query),
  analyticsCharts: (query: MobileAnalyticsQuery = {}) =>
    withAnalyticsQuery('/mobile/analytics/charts', query),
  analyticsTimelineBlocks: (query: MobileAnalyticsQuery = {}) =>
    withAnalyticsQuery('/mobile/analytics/timeline-blocks', query),
  timelineBlockSessions: (blockId: string, query: MobileAnalyticsQuery = {}) =>
    withAnalyticsQuery(`/mobile/analytics/timeline-blocks/${pathSegment(blockId)}/sessions`, query),
  sessionEvents: (sessionId: string) =>
    `/mobile/analytics/sessions/${pathSegment(sessionId)}/events`,
  appCatalogOverrides: () => '/mobile/apps/catalog-overrides',
  appCatalogOverride: (packageName: string) =>
    `/mobile/apps/catalog-overrides/${pathSegment(packageName)}`,
  appCategoryRules: () => '/mobile/apps/category-rules',
  appCategoryRule: (id: string) => `/mobile/apps/category-rules/${pathSegment(id)}`,
  usageGoals: () => '/mobile/analytics/goals',
  usageGoal: (id: string) => `/mobile/analytics/goals/${pathSegment(id)}`,
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

export interface MobileAnalyticsRange {
  rangeStartUtc: string;
  rangeEndUtc: string;
  timezone: string;
  localStartDate: string;
  localEndDate: string;
}

export interface MobileAnalyticsQuality {
  usageEventsCoverage: number;
  fallbackShare: number;
  missingMetadataAppCount: number;
  systemNoiseShare: number;
  shortEventShare: number;
  failedOrPartialSyncBatchCount: number;
  lastSyncAt: string | null;
  qualityFlags: string[];
}

export interface MobileAnalyticsGoal {
  key: string;
  label: string;
  limitSeconds: number;
  usedSeconds: number;
  isOverLimit: boolean;
  remainingSeconds: number;
}

export interface MobileAnalyticsAnomaly {
  code: string;
  severity: PimHealthStatus | 'Info' | string;
  title: string;
  evidence: string;
  drilldownTarget: string;
}

export interface MobileAnalyticsSuggestion {
  code: string;
  text: string;
  drilldownTarget: string;
}

export interface MobileAnalyticsOverview {
  range: MobileAnalyticsRange;
  generatedAt: string;
  isStale: boolean;
  totalForegroundSeconds: number;
  dailyAverageSeconds: number;
  previousPeriodChange: number;
  highestUseLocalDate: string | null;
  peakLocalHour: number | null;
  appCount: number;
  switchOrPickupCount: number;
  completeness: number;
  quality: MobileAnalyticsQuality;
  goalProgress: MobileAnalyticsGoal | null;
  anomalies: MobileAnalyticsAnomaly[];
  suggestions: MobileAnalyticsSuggestion[];
}

export interface MobileHeatmapBucket {
  bucketStartUtc: string;
  bucketEndUtc: string;
  localDate: string;
  localHour: number;
  lifeCategory: MobileLifeCategory | string;
  foregroundSeconds: number;
  qualityFlags: string[];
}

export type MobileAnalyticsChartType =
  | 'category-share'
  | 'category-trend'
  | 'daily-total'
  | 'hour-distribution'
  | 'top-apps'
  | 'switch-trend'
  | 'comparison'
  | 'goal-marker'
  | string;

export type MobileAnalyticsChartUnit = 'seconds' | 'count' | 'ratio' | string;

export interface MobileAnalyticsChartPoint {
  key: string;
  label: string;
  value: number;
  foregroundSeconds?: number;
  lifeCategory?: MobileLifeCategory | string | null;
  packageName?: string | null;
  localDate?: string | null;
  localHour?: number | null;
}

export interface MobileAnalyticsChart {
  key: string;
  title: string;
  chartType: MobileAnalyticsChartType;
  unit: MobileAnalyticsChartUnit;
  points: MobileAnalyticsChartPoint[];
}

export interface MobileTimelineBlockApp {
  packageName: string;
  displayName: string;
  foregroundSeconds: number;
}

export interface MobileTimelineBlock {
  id: string;
  startUtc: string;
  endUtc: string;
  localStart: string;
  localEnd: string;
  lifeCategory: MobileLifeCategory | string;
  foregroundSeconds: number;
  sessionCount: number;
  appCount: number;
  topApps: MobileTimelineBlockApp[];
  qualityFlags: string[];
  sourceMix?: Record<string, number>;
  includesSystemNoise?: boolean;
}

export interface MobileTimelineBlockPage {
  items: MobileTimelineBlock[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface MobileTimelineBlockSession {
  id: string;
  deviceId: string;
  packageName: string;
  displayName: string;
  startUtc: string;
  endUtc: string;
  durationSeconds: number;
  lifeCategory: MobileLifeCategory | string;
  source: MobileUsageSource;
  confidence: number;
  qualityFlags: string[];
}

export interface MobileSessionEvent {
  id: string;
  sessionId: string;
  deviceId: string;
  packageName: string;
  eventType: string;
  eventTimeUtc: string;
  className: string | null;
  rawJson: string;
}

export interface MobileAppCatalogOverride {
  packageName: string;
  displayNameOverride: string | null;
  lifeCategory: MobileLifeCategory | string;
  isSystemNoise: boolean;
  hideShortEvents: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface MobileAppCategoryRule {
  id: string;
  ruleType: string;
  pattern: string;
  lifeCategory: MobileLifeCategory | string;
  priority: number;
  isEnabled: boolean;
  displayNameOverride?: string | null;
  isSystemNoise?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export type MobileAppCategoryRuleUpsertRequest = Omit<
  MobileAppCategoryRule,
  'id' | 'createdAt' | 'updatedAt'
> & {
  id?: string;
};

export type MobileUsageGoalScope = 'total-daily' | 'category-daily' | 'app-daily' | string;

export interface MobileUsageGoal {
  id: string;
  scope: MobileUsageGoalScope;
  packageName: string | null;
  lifeCategory: MobileLifeCategory | string | null;
  label: string;
  limitSeconds: number;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MobileUsageGoalUpsertRequest {
  scope: MobileUsageGoalScope;
  packageName?: string | null;
  lifeCategory?: MobileLifeCategory | string | null;
  label: string;
  limitSeconds: number;
  isEnabled: boolean;
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

export function getMobileAnalyticsOverview(
  query: MobileAnalyticsQuery = {},
): Promise<MobileAnalyticsOverview> {
  return apiGet<ApiResponse<MobileAnalyticsOverview>>(mobileApiPaths.analyticsOverview(query)).then(r => r.data);
}

export function getMobileAnalyticsHeatmap(query: MobileAnalyticsQuery = {}): Promise<MobileHeatmapBucket[]> {
  return apiGet<ApiResponse<MobileHeatmapBucket[]>>(mobileApiPaths.analyticsHeatmap(query)).then(r => r.data);
}

export const getMobileHeatmap = getMobileAnalyticsHeatmap;

export function getMobileAnalyticsCharts(query: MobileAnalyticsQuery = {}): Promise<MobileAnalyticsChart[]> {
  return apiGet<ApiResponse<MobileAnalyticsChart[]>>(mobileApiPaths.analyticsCharts(query)).then(r => r.data);
}

export function getMobileAnalyticsTimelineBlocks(
  query: MobileAnalyticsQuery = {},
): Promise<MobileTimelineBlockPage> {
  return apiGet<ApiResponse<MobileTimelineBlockPage>>(mobileApiPaths.analyticsTimelineBlocks(query)).then(r => r.data);
}

export function getMobileTimelineBlockSessions(
  blockId: string,
  query: MobileAnalyticsQuery = {},
): Promise<MobileTimelineBlockSession[]> {
  return apiGet<ApiResponse<MobileTimelineBlockSession[]>>(
    mobileApiPaths.timelineBlockSessions(blockId, query),
  ).then(r => r.data);
}

export function getMobileSessionEvents(sessionId: string): Promise<MobileSessionEvent[]> {
  return apiGet<ApiResponse<MobileSessionEvent[]>>(mobileApiPaths.sessionEvents(sessionId)).then(r => r.data);
}

export function getMobileAppCatalogOverrides(): Promise<MobileAppCatalogOverride[]> {
  return apiGet<ApiResponse<MobileAppCatalogOverride[]>>(mobileApiPaths.appCatalogOverrides()).then(r => r.data);
}

export function saveMobileAppCatalogOverride(
  override: MobileAppCatalogOverride,
): Promise<MobileAppCatalogOverride> {
  return apiPut<ApiResponse<MobileAppCatalogOverride>>(
    mobileApiPaths.appCatalogOverride(override.packageName),
    override,
  ).then(r => r.data);
}

export function deleteMobileAppCatalogOverride(packageName: string): Promise<string> {
  return apiDelete<ApiResponse<string>>(mobileApiPaths.appCatalogOverride(packageName)).then(r => r.data);
}

export function getMobileAppCategoryRules(): Promise<MobileAppCategoryRule[]> {
  return apiGet<ApiResponse<MobileAppCategoryRule[]>>(mobileApiPaths.appCategoryRules()).then(r => r.data);
}

export function createMobileAppCategoryRule(
  rule: MobileAppCategoryRuleUpsertRequest,
): Promise<MobileAppCategoryRule> {
  return apiPost<ApiResponse<MobileAppCategoryRule>>(mobileApiPaths.appCategoryRules(), rule).then(r => r.data);
}

export function updateMobileAppCategoryRule(
  id: string,
  rule: MobileAppCategoryRuleUpsertRequest,
): Promise<MobileAppCategoryRule> {
  return apiPut<ApiResponse<MobileAppCategoryRule>>(mobileApiPaths.appCategoryRule(id), rule).then(r => r.data);
}

export function deleteMobileAppCategoryRule(id: string): Promise<string> {
  return apiDelete<ApiResponse<string>>(mobileApiPaths.appCategoryRule(id)).then(r => r.data);
}

export function getMobileUsageGoals(): Promise<MobileUsageGoal[]> {
  return apiGet<ApiResponse<MobileUsageGoal[]>>(mobileApiPaths.usageGoals()).then(r => r.data);
}

export function saveMobileUsageGoal(goal: MobileUsageGoalUpsertRequest): Promise<MobileUsageGoal> {
  return apiPost<ApiResponse<MobileUsageGoal>>(mobileApiPaths.usageGoals(), goal).then(r => r.data);
}

export function deleteMobileUsageGoal(id: string): Promise<string> {
  return apiDelete<ApiResponse<string>>(mobileApiPaths.usageGoal(id)).then(r => r.data);
}

import assert from 'node:assert/strict';
import {
  MOBILE_DEFAULT_TIMEZONE,
  MOBILE_LIFE_CATEGORIES,
  createMobileAppCategoryRule,
  deleteMobileAppCatalogOverride,
  deleteMobileAppCategoryRule,
  deleteMobileUsageGoal,
  getMobileAnalyticsCharts,
  getMobileAnalyticsHeatmap,
  getMobileAnalyticsOverview,
  getMobileAnalyticsTimelineBlocks,
  getMobileAppCatalogOverrides,
  getMobileAppCategoryRules,
  getMobileDevices,
  getMobileLocationHistory,
  getMobileQuality,
  getMobileSessionEvents,
  getMobileSummary,
  getMobileTimeline,
  getMobileTimelineBlockSessions,
  getMobileUsageGoals,
  saveMobileAppCatalogOverride,
  saveMobileUsageGoal,
  updateMobileAppCategoryRule,
} from '../../src/client-web/src/api/mobile';
import type {
  MobileAnalyticsChart,
  MobileAnalyticsOverview,
  MobileAnalyticsQuery,
  MobileAppCatalogOverride,
  MobileAppCategoryRule,
  MobileDevice,
  MobileHeatmapBucket,
  MobileLifeCategory,
  MobileTimelineBlockPage,
  MobileUsageGoal,
  MobileUsageGoalUpsertRequest,
} from '../../src/client-web/src/api/mobile';

function acceptsDeviceReturn(result: ReturnType<typeof getMobileDevices>): Promise<MobileDevice[]> {
  return result;
}
function acceptsOverviewReturn(result: ReturnType<typeof getMobileAnalyticsOverview>): Promise<MobileAnalyticsOverview> {
  return result;
}
function acceptsHeatmapReturn(result: ReturnType<typeof getMobileAnalyticsHeatmap>): Promise<MobileHeatmapBucket[]> {
  return result;
}
function acceptsChartsReturn(result: ReturnType<typeof getMobileAnalyticsCharts>): Promise<MobileAnalyticsChart[]> {
  return result;
}
function acceptsTimelineBlocksReturn(result: ReturnType<typeof getMobileAnalyticsTimelineBlocks>): Promise<MobileTimelineBlockPage> {
  return result;
}
function acceptsOverridesReturn(result: ReturnType<typeof getMobileAppCatalogOverrides>): Promise<MobileAppCatalogOverride[]> {
  return result;
}
function acceptsRulesReturn(result: ReturnType<typeof getMobileAppCategoryRules>): Promise<MobileAppCategoryRule[]> {
  return result;
}
function acceptsGoalsReturn(result: ReturnType<typeof getMobileUsageGoals>): Promise<MobileUsageGoal[]> {
  return result;
}

void acceptsDeviceReturn;
void acceptsOverviewReturn;
void acceptsHeatmapReturn;
void acceptsChartsReturn;
void acceptsTimelineBlocksReturn;
void acceptsOverridesReturn;
void acceptsRulesReturn;
void acceptsGoalsReturn;
void getMobileSummary;
void getMobileTimeline;
void getMobileLocationHistory;
void getMobileQuality;
void getMobileTimelineBlockSessions;
void getMobileSessionEvents;
void saveMobileAppCatalogOverride;
void deleteMobileAppCatalogOverride;
void createMobileAppCategoryRule;
void updateMobileAppCategoryRule;
void deleteMobileAppCategoryRule;
void saveMobileUsageGoal;
void deleteMobileUsageGoal;

const typedLifeCategory: MobileLifeCategory = '短视频/娱乐';
const analyticsQuery: MobileAnalyticsQuery = {
  rangeStartUtc: '2026-07-01T16:00:00Z',
  rangeEndUtc: '2026-07-08T16:00:00Z',
  timezone: MOBILE_DEFAULT_TIMEZONE,
  category: '社交通讯',
  includeSystemNoise: false,
  minDurationSeconds: 1,
  granularity: 'hour',
  pageSize: 50,
};

const overview: MobileAnalyticsOverview = {
  range: {
    rangeStartUtc: '2026-07-01T16:00:00Z',
    rangeEndUtc: '2026-07-08T16:00:00Z',
    timezone: MOBILE_DEFAULT_TIMEZONE,
    localStartDate: '2026-07-02',
    localEndDate: '2026-07-08',
  },
  generatedAt: '2026-07-08T10:00:00Z',
  isStale: false,
  totalForegroundSeconds: 3600,
  dailyAverageSeconds: 600,
  previousPeriodChange: 0.25,
  highestUseLocalDate: '2026-07-08',
  peakLocalHour: 21,
  appCount: 12,
  switchOrPickupCount: 42,
  completeness: 0.94,
  quality: {
    usageEventsCoverage: 0.92,
    fallbackShare: 0.08,
    missingMetadataAppCount: 1,
    systemNoiseShare: 0.03,
    shortEventShare: 0.02,
    failedOrPartialSyncBatchCount: 0,
    lastSyncAt: '2026-07-08T09:59:00Z',
    qualityFlags: [],
  },
  goalProgress: {
    key: 'total-daily',
    label: '每日手机总时长',
    limitSeconds: 14400,
    usedSeconds: 3600,
    isOverLimit: false,
    remainingSeconds: 10800,
  },
  anomalies: [{ code: 'night-use', severity: 'Warning', title: '夜间使用偏高', evidence: '22:00 后使用增加', drilldownTarget: 'heatmap:night' }],
  suggestions: [{ code: 'short-video-night', text: '短视频/娱乐集中在 22:00 后', drilldownTarget: 'category:短视频/娱乐' }],
};

const heatmapBucket: MobileHeatmapBucket = {
  bucketStartUtc: '2026-07-06T13:00:00Z',
  bucketEndUtc: '2026-07-06T14:00:00Z',
  localDate: '2026-07-06',
  localHour: 21,
  lifeCategory: '社交通讯',
  foregroundSeconds: 1800,
  qualityFlags: [],
};

const chart: MobileAnalyticsChart = {
  key: 'category-share',
  title: '分类占比',
  chartType: 'category-share',
  unit: 'seconds',
  points: [{ key: '社交通讯', label: '社交通讯', value: 1800, foregroundSeconds: 1800, lifeCategory: '社交通讯' }],
};

const page: MobileTimelineBlockPage = {
  items: [{
    id: 'block-1',
    startUtc: '2026-07-06T13:00:00Z',
    endUtc: '2026-07-06T14:00:00Z',
    localStart: '21:00',
    localEnd: '22:00',
    lifeCategory: '社交通讯',
    foregroundSeconds: 1800,
    sessionCount: 2,
    appCount: 1,
    topApps: [{ packageName: 'com.tencent.mobileqq', displayName: 'QQ', foregroundSeconds: 1200 }],
    qualityFlags: [],
  }],
  nextCursor: null,
  hasMore: false,
};

const override: MobileAppCatalogOverride = {
  packageName: 'com.tencent.mobileqq',
  displayNameOverride: 'QQ',
  lifeCategory: '社交通讯',
  isSystemNoise: false,
  hideShortEvents: false,
};

const rule: MobileAppCategoryRule = {
  id: 'rule-1',
  ruleType: 'package-prefix',
  pattern: 'com.tencent.',
  lifeCategory: '社交通讯',
  priority: 100,
  isEnabled: true,
};

const usageGoal: MobileUsageGoal = {
  id: 'goal-1',
  scope: 'total-daily',
  packageName: null,
  lifeCategory: null,
  label: '每日手机总时长',
  limitSeconds: 14400,
  isEnabled: true,
  createdAt: '2026-07-08T10:00:00Z',
  updatedAt: '2026-07-08T10:00:00Z',
};

const usageGoalRequest: MobileUsageGoalUpsertRequest = {
  scope: usageGoal.scope,
  packageName: usageGoal.packageName,
  lifeCategory: usageGoal.lifeCategory,
  label: usageGoal.label,
  limitSeconds: usageGoal.limitSeconds,
  isEnabled: usageGoal.isEnabled,
};

assert.equal(MOBILE_DEFAULT_TIMEZONE, 'Asia/Shanghai');
assert.equal(MOBILE_LIFE_CATEGORIES.includes('生活服务'), true);
assert.equal(typedLifeCategory, '短视频/娱乐');
assert.equal(analyticsQuery.category, '社交通讯');
assert.equal(overview.goalProgress?.label, '每日手机总时长');
assert.equal(overview.suggestions[0].drilldownTarget, 'category:短视频/娱乐');
assert.equal(heatmapBucket.lifeCategory, '社交通讯');
assert.equal(chart.points[0].label, '社交通讯');
assert.equal(page.items[0].topApps[0].displayName, 'QQ');
assert.equal(override.displayNameOverride, 'QQ');
assert.equal(rule.priority, 100);
assert.equal(usageGoalRequest.label, '每日手机总时长');

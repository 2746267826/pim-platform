export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
  userInfo: { id: string; username: string; displayName: string };
}

export interface CalendarResponse {
  id: string;
  name: string;
  color: string;
  kind: string;
  isDefault: boolean;
}

export interface EventResponse {
  id: string;
  calendarId: string;
  uid: string;
  title: string;
  description?: string;
  location?: string;
  dtStart: string;
  dtEnd: string;
  rrule?: string;
  status: string;
  source: string;
  originalEventId?: string;
}

export interface TaskResponse {
  id: string;
  calendarId?: string;
  title: string;
  description?: string;
  priority: number;
  estimatedDuration?: string;
  minimumSegment?: string;
  dtStart?: string;
  due?: string;
  status: string;
  isInbox: boolean;
}

export interface PagedResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

export interface ImportResult {
  imported: number;
  skipped: number;
}

export type PimHealthStatus = 'Unknown' | 'Healthy' | 'Warning' | 'Critical';

export interface SystemStatusSummary {
  status: PimHealthStatus;
  label: string;
  message: string;
  checkedAt: string;
}

export interface StatusComponent {
  key: string;
  name: string;
  kind: string;
  status: PimHealthStatus;
  message: string;
  checkedAt: string;
  details: Record<string, string>;
}

export interface SystemStatusDetail {
  summary: SystemStatusSummary;
  components: StatusComponent[];
  nextSteps: string[];
}

export interface PcQualityComponent {
  key: string;
  name: string;
  status: PimHealthStatus;
  message: string;
  details: Record<string, string>;
}

export interface PcQualityIssue {
  code: string;
  severity: PimHealthStatus;
  componentKey: string;
  message: string;
  nextStep: string | null;
}

export interface PcQualityResponse {
  overallStatus: PimHealthStatus;
  label: string;
  message: string;
  checkedAt: string;
  components: PcQualityComponent[];
  issues: PcQualityIssue[];
  nextSteps: string[];
}

export interface PcQualityQueryParams {
  date?: string;
  dateFrom?: string;
  dateTo?: string;
}

// PC Tracker types
export interface PcSummaryResponse {
  keystats: KeystatsSummary | null;
  heatmap: HeatmapBucket[];
  appRanking: AppRankingItem[];
  timeline: TimelineItem[];
  sessions: WorkSessionItem[];
  metrics: DerivedMetrics | null;
  categories: CategorySummary[];
}

export interface KeystatsSummary {
  date: string;
  keyPresses: number;
  totalClicks: number;
  leftClicks: number;
  rightClicks: number;
  middleClicks: number;
  sideBackClicks: number;
  sideForwardClicks: number;
  mouseDistance: number;
  scrollDistance: number;
  peakKps: number;
  peakCps: number;
  keyPressCounts: Record<string, number>;
  topKeys: KeyCountItem[];
}

export interface KeyCountItem {
  keyName: string;
  count: number;
  share: number;
}

export interface HeatmapBucket {
  start: string;
  end: string;
  hour: number;
  activeMinutes: number;
  totalEvents: number;
  intensityScore: number;
}

export interface AppRankingItem {
  appName: string;
  displayName: string;
  keyPresses: number;
  totalClicks: number;
  scrollDistance: number;
  share: number;
}

export interface TimelineItem {
  start: string;
  end: string;
  durationMinutes: number;
  appName: string;
  windowTitle: string | null;
  categoryName: string;
  categoryColor: string;
  projectTag: string | null;
  classificationConfidence: number;
  classificationSource: string;
  classificationExplanation: string;
}

export interface WorkSessionItem {
  start: string;
  end: string;
  durationMinutes: number;
  mainApp: string;
  appSwitchCount: number;
}

export interface DerivedMetrics {
  totalRecordedDuration: string;
  activeInputDuration: string;
  idleDuration: string;
  sessionCount: number;
  activeAppCount: number;
  totalKeyPresses: number;
  totalClicks: number;
  appSwitchCount: number;
  switchFrequency: number;
  mostFocusedApp: string;
  keyClickRatio: number;
}

export interface CategorySummary {
  categoryName: string;
  color: string;
  share: number;
  keyPresses: number;
  totalClicks: number;
}

export interface AppCategoryRule {
  id: string;
  appPattern: string;
  categoryName: string;
  color: string;
  priority: number;
  isBuiltin: boolean;
}

export interface ActivityClassificationRule {
  id: string;
  ruleName: string;
  scope: string;
  categoryName: string | null;
  projectTag: string | null;
  color: string;
  priority: number;
  source: string;
  status: string;
  conditionsJson: string;
  confidence: number;
  explanation: string | null;
}

export interface ActivityClassificationSuggestion {
  id: string;
  clusterKey: string;
  sampleCount: number;
  totalDurationSeconds: number;
  sampleRecordsJson: string;
  sanitizedContextJson: string;
  currentCategory: string | null;
  suggestedCategory: string | null;
  suggestedProjectTag: string | null;
  suggestedRulesJson: string | null;
  userFeedback: string | null;
  llmResponseJson: string | null;
  status: string;
}

export interface ActivityClassificationApplyRange {
  mode: 'today' | 'range' | 'all';
  dateFrom?: string | null;
  dateTo?: string | null;
}

export interface SaveActivityClassificationRuleRequest {
  ruleName: string;
  scope: string;
  categoryName: string | null;
  projectTag: string | null;
  color: string;
  priority: number;
  conditionsJson: string;
  confidence: number;
  explanation: string | null;
}

export interface ActivityClassificationPreview {
  affectedRecordCount: number;
  affectedDurationSeconds: number;
  currentCategoryCounts: Record<string, number>;
  newCategoryCounts: Record<string, number>;
  samples: PcDetailRecord[];
  requiresConfirmation: boolean;
  summary: string;
}

export interface ActivityClassificationSettings {
  recommendedMinimumClassificationDurationMinutes: number;
  supportedRecommendedMinimumDurations: number[];
}

export interface DetailQueryParams {
  dateFrom?: string;
  dateTo?: string;
  dimension?: 'hour' | 'day' | 'month' | 'year';
  deviceId?: string;
  appName?: string;
  categoryName?: string;
  keyName?: string;
  domain?: string;
  title?: string;
  url?: string;
  view?: 'raw' | 'interpreted' | string;
  eventType?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
}

export type PcDetailRecordType = 'window' | 'afk' | 'input-minute' | 'app-input' | 'key-input' | 'web' | 'web-page';

export interface PcDetailRecord {
  recordType: PcDetailRecordType | string;
  start: string;
  end: string | null;
  durationSeconds: number | null;
  deviceId: string;
  appName: string | null;
  displayName: string | null;
  categoryName: string | null;
  categoryColor?: string | null;
  projectTag?: string | null;
  classificationConfidence?: number | null;
  classificationSource?: string | null;
  classificationExplanation?: string | null;
  title: string | null;
  url?: string | null;
  domain?: string | null;
  path?: string | null;
  isLocalFile?: boolean | null;
  browserAppName?: string | null;
  browserWindowTitle?: string | null;
  audible?: boolean | null;
  incognito?: boolean | null;
  tabCount?: number | null;
  absorbedShortEventsCount?: number | null;
  absorbedDurationSeconds?: number | null;
  sourceWebEventIds?: number[] | null;
  sourceWindowEventIds?: number[] | null;
  keyPresses: number | null;
  totalClicks: number | null;
  mouseDistance: number | null;
  scrollDistance: number | null;
  keyCounts: Record<string, number> | null;
  raw: unknown;
}

export interface DetailQueryResponse {
  items: PcDetailRecord[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

export interface HeatmapGridResponse {
  grid: HeatmapBucket[][];
  dimension: string;
  maxKeyCount: number;
}

export type TodaySectionStatus =
  | 'available'
  | 'normal'
  | 'empty'
  | 'warning'
  | 'critical'
  | 'unavailable';

export interface TodayLink {
  rel: 'self' | 'details' | 'api' | string;
  href: string;
}

export interface TodaySectionError {
  code: string;
  message: string;
}

export interface TodaySectionRegistryItem {
  id: string;
  kind: TodaySectionKind | string;
  status: TodaySectionStatus;
  links: TodayLink[];
}

export interface TodaySectionRegistry {
  date: string;
  pcBusinessDate: string;
  generatedAt: string;
  sections: TodaySectionRegistryItem[];
}

export interface TodaySection<TData = unknown> {
  id: string;
  kind: TodaySectionKind | string;
  status: TodaySectionStatus;
  generatedAt: string;
  data: TData;
  links: TodayLink[];
  error: TodaySectionError | null;
}

export type TodaySectionKind =
  | 'calendar.schedule'
  | 'calendar.tasks'
  | 'pc.activity'
  | 'pc.quality'
  | 'operations.health'
  | 'pc.classification_suggestions';

export interface CalendarScheduleTodayData {
  events: EventResponse[];
  scheduledTasks: TaskResponse[];
}

export interface CalendarTasksTodayData {
  incompleteCount: number;
  dueTodayTasks: TaskResponse[];
  overdueTasks: TaskResponse[];
  unscheduledTasks: TaskResponse[];
}

export interface PcActivityTodayData {
  summary: PcSummaryResponse;
}

export interface PcQualityTodayData {
  quality: PcQualityResponse;
  issueCount: number;
}

export interface OperationsHealthTodayData {
  detail: SystemStatusDetail;
  summary: SystemStatusSummary;
}

export interface ClassificationSuggestionsTodayData {
  pendingCount: number;
  suggestions: ActivityClassificationSuggestion[];
}

export type QuickNoteStatus = 'inbox' | 'processed' | 'archived';

export interface QuickNoteAttachment {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
  previewUrl: string | null;
  createdAt: string;
}

export interface QuickNoteListItem {
  id: string;
  contentPreview: string;
  status: QuickNoteStatus;
  source: string;
  attachmentCount: number;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
}

export interface QuickNoteDetail extends QuickNoteListItem {
  contentMarkdown: string;
  attachments: QuickNoteAttachment[];
  metadataJson: string;
}

export interface CreateQuickNoteRequest {
  contentMarkdown: string;
  source?: string;
  attachmentIds?: string[];
}

export interface UpdateQuickNoteRequest {
  contentMarkdown: string;
  status?: QuickNoteStatus;
  attachmentIds?: string[];
}

export interface QuickNoteAttachmentUpload {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
  previewUrl: string | null;
}

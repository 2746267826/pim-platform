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
  isAllDay?: boolean;
  timeZoneId?: string;
  sourceTimeZoneId?: string;
  sourceUid?: string;
  externalMetadataJson?: string;
  recurrenceId?: string;
  exDatesJson?: string;
  recurrenceMetadataJson?: string;
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
  plannedEnd?: string;
  status: string;
  isInbox: boolean;
}

export type CalendarLayerId = 'events' | 'tasks' | 'task-segments' | 'habits' | 'availability' | 'ai-placeholders';

export type WorkbenchDensityMode = 'standard' | 'dense' | 'focus';

export interface CreateTaskExecutionSegmentRequest {
  startsAt: string;
  endsAt: string;
  status: string;
  source: string;
  planningReason?: string | null;
}

export interface TaskExecutionSegmentResponse {
  id: string;
  taskId: string;
  taskTitle: string;
  startsAt: string;
  endsAt: string;
  status: string;
  source: string;
  planningReason?: string | null;
  confirmationId?: string | null;
}

export interface CalendarLayerQueryRequest {
  start: string;
  end: string;
  layers?: Array<CalendarLayerId | string>;
  outlookOnly?: boolean;
}

export interface CalendarLayerItem {
  id: string;
  layer: CalendarLayerId | string;
  objectType: string;
  objectId: string;
  title: string;
  startsAt: string;
  endsAt: string;
  source: string;
  status: string;
  color: string;
  requiresConfirmation: boolean;
}

export interface CalendarLayerResponse {
  start: string;
  end: string;
  items: CalendarLayerItem[];
}

export interface DataCenterQueryRequest {
  search?: string | null;
  objectType?: string | null;
  source?: string | null;
  pendingOnly: boolean;
  page?: number;
  pageSize?: number;
}

export interface DataCenterItem {
  objectType: string;
  objectId: string;
  title: string;
  source: string;
  status: string;
  startsAt?: string | null;
  endsAt?: string | null;
  summary: string;
}

export interface DataCenterQueryResponse {
  items: DataCenterItem[];
  page: number;
  pageSize: number;
  totalCount: number;
}

export interface OutlookSettingsResponse {
  provider: string;
  tenantId: string;
  clientId?: string | null;
  scopes: string;
  status: string;
  tokenHealth: string;
  lastSyncedAt?: string | null;
  lastError?: string | null;
}

export interface UpdateOutlookSettingsRequest {
  tenantId: string;
  clientId?: string | null;
  scopes: string;
}

export interface OutlookDeviceCodeRequestResponse {
  endpoint: string;
  verificationUri: string;
  userCode: string;
  expiresAt: string;
  message: string;
}

export interface OutlookSyncStep {
  name: string;
  status: string;
  detail: string;
  at: string;
}

export interface OutlookSyncBatchResponse {
  id: string;
  provider: string;
  status: string;
  readCount: number;
  createdCount: number;
  updatedCount: number;
  conflictCount: number;
  confirmationCount: number;
  failureCount: number;
  steps: OutlookSyncStep[];
  errorSummary?: string | null;
  startedAt: string;
  finishedAt?: string | null;
}

export type OperationRiskLevel =
  | 'Low'
  | 'Medium'
  | 'High'
  | 'L0AutomaticArtifact'
  | 'L1LowRiskAction'
  | 'L2PimFactChange'
  | 'L3ExternalSourceOrWriteback'
  | 'L4BatchOrDestructiveGovernance';

export type OperationConfirmationStatus =
  | 'Pending'
  | 'Confirmed'
  | 'Rejected'
  | 'Expired'
  | 'Executed';

export interface OperationConfirmation {
  id: string;
  requestedByUserId?: string | null;
  operationType: string;
  summary: string;
  riskLevel: OperationRiskLevel;
  source: string;
  payloadJson: string;
  previewJson: string;
  status: OperationConfirmationStatus;
  expiresAt: string;
  createdAt: string;
  confirmedAt?: string | null;
  executedAt?: string | null;
  resultJson?: string | null;
  correlationId?: string | null;
  changedFields?: string[] | null;
  allowedActions?: string[] | null;
  objectType?: string | null;
  objectId?: string | null;
  requiresSecondLevelConfirmation: boolean;
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

export interface CalendarOperationSample {
  id: string;
  type: string;
  title: string;
  start?: string;
  end?: string;
  bookName?: string;
}

export interface CalendarDeletePreviewResponse {
  targetType: string;
  targetId: string;
  title: string;
  operationKind: string;
  affectedCount: number;
  samples: CalendarOperationSample[];
  summary: string;
  requiresStrictConfirmation: boolean;
}

export interface CalendarOperationResult {
  operation: string;
  operationId: string;
  affectedCount: number;
  affectedIds: string[];
  samples: CalendarOperationSample[];
  message: string;
}

export interface CalendarRestoreConflict {
  deletedId: string;
  deletedType: string;
  activeId: string;
  activeType: string;
  reason: string;
  title: string;
}

export interface CalendarRestorePreviewResponse {
  targetType: string;
  targetId: string;
  title: string;
  restoreCount: number;
  samples: CalendarOperationSample[];
  conflicts: CalendarRestoreConflict[];
  canRestoreWithoutConflict: boolean;
}

export interface CalendarRecycleBinItem {
  id: string;
  type: string;
  title: string;
  deletedAt: string;
  bookName?: string;
  start?: string;
  end?: string;
  source: string;
  deletedByOperationId?: string;
  deletedByOperationKind?: string;
}

export interface ImportSkippedItem {
  reason: string;
  title: string;
  start?: string;
  uid?: string;
}

export interface ImportReport {
  imported: number;
  skipped: number;
  skippedReasons: Record<string, number>;
  samples: ImportSkippedItem[];
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
  appDisplayName?: string | null;
  appIcon?: string | null;
  recognitionSource?: string | null;
}

export interface ActivityClassificationApplyRange {
  mode: 'today' | 'range';
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

export interface SuggestionClassificationPreviewRequest {
  categoryName: string | null;
  projectTag: string | null;
  range: ActivityClassificationApplyRange;
}

export interface ActivityClassificationSuggestionPreview {
  rule: SaveActivityClassificationRuleRequest;
  preview: ActivityClassificationPreview;
}

export interface SuggestionClassificationApplyRequest {
  categoryName: string | null;
  projectTag: string | null;
  range: ActivityClassificationApplyRange;
}

export interface ActivityClassificationSuggestionApply {
  rule: ActivityClassificationRule;
  preview: ActivityClassificationPreview;
  auditId: string;
  suggestionStatus: string;
}

export interface PcActivityAnalysisResponse {
  date: string;
  blockMinutes: number;
  blocks: PcActivityAnalysisBlock[];
}

export interface PcActivityAnalysisBlock {
  start: string;
  end: string;
  intensityScore: number;
  activeDurationSeconds: number;
  pendingClassificationCount: number;
  contextSwitchCount: number;
  categoryChangeCount: number;
  categories: PcActivityAnalysisCategory[];
  apps: PcActivityAnalysisApp[];
}

export interface PcActivityAnalysisCategory {
  categoryName: string;
  color: string;
  durationSeconds: number;
}

export interface PcActivityAnalysisApp {
  appName: string;
  durationSeconds: number;
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
  recordKey?: string | null;
  recordKeyVersion?: string | null;
  recordKeyStability?: string | null;
  sourceBucketIds?: string[] | null;
  sourceType?: string | null;
  interpretationVersion?: string | null;
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

export type AiRequestStatus = 'Succeeded' | 'Failed' | 'Blocked' | 'TimedOut' | 'FailedValidation';

export interface AiStatus {
  enabled: boolean;
  provider: string;
  baseUrl: string;
  defaultModel: string;
  lastHealthCheckAt?: string | null;
  lastError?: string | null;
  recentSuccessfulCallAt?: string | null;
}

export interface AiRequestLogListItem {
  id: string;
  startedAt: string;
  module: string;
  purpose: string;
  model: string;
  status: AiRequestStatus;
  totalTokens?: number | null;
  estimatedCost?: number | null;
  durationMs?: number | null;
  sourceObjectType: string;
  sourceObjectId: string;
  errorSummary?: string | null;
}

export interface AiRequestLogDetail extends AiRequestLogListItem {
  userId?: string | null;
  provider: string;
  liteLlmRequestId?: string | null;
  correlationId: string;
  attemptNumber: number;
  maxAttempts: number;
  finishedAt?: string | null;
  requestMessagesJson: string;
  requestPayloadJson: string;
  responseRawJson: string;
  responseText?: string | null;
  parsedOutputJson?: string | null;
  schemaName?: string | null;
  schemaVersion?: string | null;
  schemaJsonSnapshot?: string | null;
  schemaValidationErrorsJson: string;
  usage: {
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
    estimatedCost?: number | null;
    currency?: string | null;
  };
  errorCode?: string | null;
  errorMessage?: string | null;
  metadataJson: string;
}

export interface AiUsageGroup {
  groupKey: string;
  requestCount: number;
  successCount: number;
  failureCount: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number;
}

export interface AiUsageSummary {
  requestCount: number;
  successCount: number;
  failureCount: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number;
  byModule: AiUsageGroup[];
  byPurpose: AiUsageGroup[];
  byModel: AiUsageGroup[];
  byStatus: AiUsageGroup[];
}

// Files module types
export interface FileProvider {
  id: string;
  provider: string;
  baseUrl: string;
  internalBaseUrl: string | null;
  username: string;
  status: string;
  lastSyncAt: string | null;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BindNextcloudProviderRequest {
  baseUrl: string;
  internalBaseUrl: string | null;
  username: string;
  appPassword: string;
}

export interface FileProviderTest {
  success: boolean;
  status: string;
  errorMessage: string | null;
}

export interface FileItem {
  id: string;
  providerId: string;
  externalFileId: string;
  parentExternalFileId: string | null;
  path: string;
  name: string;
  itemType: string;
  mimeType: string | null;
  size: number | null;
  etag: string | null;
  contentHash: string | null;
  currentVersionId: string | null;
  permissions: string | null;
  isDeleted: boolean;
  deletedAt: string | null;
  lastSeenAt: string | null;
  createdAt: string;
  modifiedAt: string;
  syncedAt: string;
  indexStatus: string;
  ai: FileAiResult | null;
}

export interface FileVersion {
  id: string;
  fileItemId: string;
  externalVersionId: string;
  etag: string | null;
  size: number | null;
  modifiedAt: string;
  source: string;
  isCurrent: boolean;
  syncedAt: string;
}

export interface FileAiResult {
  id: string;
  fileItemId: string;
  versionId: string;
  summary: string;
  tags: string[];
  language: string | null;
  sensitivity: string | null;
  generatedAt: string;
  model: string | null;
  aiRequestLogId: string | null;
  evidenceChunkIds: string[];
}

export interface FileSuggestion {
  id: string;
  fileItemId: string;
  suggestionType: string;
  title: string;
  reason: string;
  confidence: number;
  payloadJson: string;
  status: string;
  aiRequestLogId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FileListResponse {
  result: PagedResult<FileItem>;
}

export type FileSearchMode = 'keyword' | 'semantic' | 'hybrid';

export interface FileSearchResult {
  items: FileItem[];
  chunks: FileChunkSearchHit[];
}

export interface FileChunkSearchHit {
  chunkId: string;
  fileItemId: string;
  versionId: string;
  text: string;
  score: number;
}

export interface MoveFileRequest {
  destinationPath: string;
}

export interface RenameFileRequest {
  name: string;
}

export interface FileOpenLink {
  url: string;
  mode: string;
}

export type FileOpenLinkMode = 'view' | 'edit' | 'nextcloud';

export interface VersionRestorePreview {
  fileItemId: string;
  versionId: string;
  currentVersionLabel: string;
  restoreVersionLabel: string;
  requiresConfirmation: boolean;
  summary: string;
}

export interface FileIndexJob {
  id: string;
  fileItemId: string;
  versionId: string | null;
  status: string;
  stage: string;
  attemptCount: number;
  lastError: string | null;
}

export interface FileSuggestionStatusRequest {
  status: string;
}

export interface FileTrashItem {
  trashId: string;
  originalLocation: string;
  name: string;
  itemType: string;
  size: number | null;
  deletedAt: string;
}

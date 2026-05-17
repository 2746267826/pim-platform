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
  isDefault: boolean;
}

export interface EventResponse {
  id: string;
  calendarId: string;
  title: string;
  description?: string;
  location?: string;
  dtStart: string;
  dtEnd: string;
  rrule?: string;
  status: string;
}

export interface TaskResponse {
  id: string;
  calendarId?: string;
  title: string;
  description?: string;
  priority: number;
  estimatedDuration?: string;
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

// PC Tracker types
export interface PcSummaryResponse {
  keystats: KeystatsSummary | null;
  heatmap: HeatmapBucket[];
  appRanking: AppRankingItem[];
  timeline: TimelineItem[];
  sessions: WorkSessionItem[];
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
}

export interface WorkSessionItem {
  start: string;
  end: string;
  durationMinutes: number;
  mainApp: string;
  appSwitchCount: number;
}

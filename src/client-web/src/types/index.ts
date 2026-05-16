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

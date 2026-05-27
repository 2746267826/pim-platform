import { apiGet, apiPost, apiPut, apiDelete } from './client';
import type {
  ApiResponse,
  CalendarDeletePreviewResponse,
  CalendarOperationResult,
  CalendarRecycleBinItem,
  CalendarResponse,
  CalendarRestorePreviewResponse,
  EventResponse,
  ImportReport,
  PagedResult,
  TaskResponse,
} from '../types';

export type TaskMutationData = {
  calendarId?: string;
  title: string;
  description?: string;
  priority: number;
  estimatedDuration?: string;
  minimumSegment?: string;
  dtStart?: string;
  plannedEnd?: string;
  due?: string;
  status?: string;
};

export type RecycleBinParams = {
  type?: string;
  search?: string;
  page?: number;
  pageSize?: number;
};

export interface GetTasksParams {
  inbox?: boolean;
  search?: string;
  calendarId?: string;
  status?: string;
  priority?: number;
  plannedFrom?: string;
  plannedTo?: string;
  dueFrom?: string;
  dueTo?: string;
  page?: number;
  pageSize?: number;
}

function appendQuery(path: string, params: Record<string, string | number | undefined>) {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) searchParams.set(key, String(value));
  }

  const qs = searchParams.toString();
  return qs ? `${path}?${qs}` : path;
}

export const calendarApiPaths = {
  recycleBin(params: RecycleBinParams = {}) {
    return appendQuery('/calendar/recycle-bin', {
      type: params.type,
      search: params.search,
      page: params.page,
      pageSize: params.pageSize,
    });
  },
  recycleRestorePreview(type: string, id: string) {
    return `/calendar/recycle-bin/${encodeURIComponent(type)}/${encodeURIComponent(id)}/restore-preview`;
  },
  recycleRestore(type: string, id: string) {
    return `/calendar/recycle-bin/${encodeURIComponent(type)}/${encodeURIComponent(id)}/restore`;
  },
  calendarDeletePreview(id: string) {
    return `/calendar/calendars/${encodeURIComponent(id)}/delete-preview`;
  },
  eventBatchDelete() {
    return '/calendar/events/batch-delete';
  },
  taskPlan(id: string) {
    return `/calendar/tasks/${encodeURIComponent(id)}/plan`;
  },
  taskBatchUpdate() {
    return '/calendar/tasks/batch-update';
  },
  taskBatchDelete() {
    return '/calendar/tasks/batch-delete';
  },
};

export async function getCalendars(kind?: string) {
  const qs = kind ? `?kind=${encodeURIComponent(kind)}` : '';
  const r = await apiGet<ApiResponse<CalendarResponse[]>>(`/calendar/calendars${qs}`);
  return r.data;
}

export async function createCalendar(data: { name: string; color?: string; kind?: string }) {
  const r = await apiPost<ApiResponse<CalendarResponse>>('/calendar/calendars', data);
  return r.data;
}

export async function updateCalendar(id: string, data: { name?: string; color?: string }) {
  const r = await apiPut<ApiResponse<CalendarResponse>>(`/calendar/calendars/${id}`, data);
  return r.data;
}

export async function deleteCalendar(id: string) {
  await apiDelete(`/calendar/calendars/${id}`);
}

export async function getEvents(start: string, end: string) {
  const r = await apiGet<ApiResponse<EventResponse[]>>(
    `/calendar/events?start=${start}&end=${end}`
  );
  return r.data;
}

export async function createEvent(data: Partial<EventResponse>) {
  const r = await apiPost<ApiResponse<EventResponse>>('/calendar/events', data);
  return r.data;
}

export async function updateEvent(id: string, data: Partial<EventResponse>) {
  const r = await apiPut<ApiResponse<EventResponse>>(`/calendar/events/${id}`, data);
  return r.data;
}

export async function deleteEvent(id: string) {
  await apiDelete(`/calendar/events/${id}`);
}

export async function batchDeleteEvents(ids: string[]): Promise<CalendarOperationResult> {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(calendarApiPaths.eventBatchDelete(), { ids });
  return r.data;
}

export function buildTasksPath(inboxOnly?: boolean) {
  return inboxOnly ? '/calendar/tasks?inbox=true' : '/calendar/tasks';
}

export async function getTasks(inboxOnly?: boolean) {
  const r = await apiGet<ApiResponse<TaskResponse[]>>(
    buildTasksPath(inboxOnly)
  );
  return r.data;
}

export async function getTasksPaged(params: GetTasksParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.inbox !== undefined) searchParams.set('inbox', String(params.inbox));
  if (params.search) searchParams.set('search', params.search);
  if (params.calendarId) searchParams.set('calendarId', params.calendarId);
  if (params.status) searchParams.set('status', params.status);
  if (params.priority !== undefined) searchParams.set('priority', String(params.priority));
  if (params.plannedFrom) searchParams.set('plannedFrom', params.plannedFrom);
  if (params.plannedTo) searchParams.set('plannedTo', params.plannedTo);
  if (params.dueFrom) searchParams.set('dueFrom', params.dueFrom);
  if (params.dueTo) searchParams.set('dueTo', params.dueTo);
  searchParams.set('page', String(params.page ?? 1));
  searchParams.set('pageSize', String(params.pageSize ?? 50));

  const qs = searchParams.toString();
  const r = await apiGet<ApiResponse<PagedResult<TaskResponse>>>(
    qs ? `/calendar/tasks?${qs}` : '/calendar/tasks'
  );
  return r.data;
}

export async function createTask(data: Partial<TaskMutationData>) {
  const r = await apiPost<ApiResponse<TaskResponse>>('/calendar/tasks', data);
  return r.data;
}

export async function updateTask(id: string, data: TaskMutationData) {
  const r = await apiPut<ApiResponse<TaskResponse>>(`/calendar/tasks/${id}`, data);
  return r.data;
}

export async function moveTask(id: string, data: { scheduledStart?: string; duration?: string; newSortOrder?: number }) {
  await apiPost<ApiResponse<string>>(`/calendar/tasks/${id}/move`, data);
}

export function taskToMutationData(task: TaskResponse, overrides: Partial<TaskMutationData> = {}): TaskMutationData {
  return {
    calendarId: task.calendarId,
    title: task.title,
    description: task.description,
    priority: task.priority,
    estimatedDuration: task.estimatedDuration,
    minimumSegment: task.minimumSegment,
    dtStart: task.dtStart,
    plannedEnd: task.plannedEnd,
    due: task.due,
    status: task.status,
    ...overrides,
  };
}

export async function deleteTask(id: string) {
  await apiDelete(`/calendar/tasks/${id}`);
}

export async function getRecycleBin(params: RecycleBinParams = {}) {
  const r = await apiGet<ApiResponse<PagedResult<CalendarRecycleBinItem>>>(
    calendarApiPaths.recycleBin(params)
  );
  return r.data;
}

export async function previewRecycleRestore(type: string, id: string) {
  const r = await apiPost<ApiResponse<CalendarRestorePreviewResponse>>(
    calendarApiPaths.recycleRestorePreview(type, id)
  );
  return r.data;
}

export async function restoreRecycleItem(type: string, id: string, restoreAsCopy = false) {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(
    calendarApiPaths.recycleRestore(type, id),
    { restoreAsCopy }
  );
  return r.data;
}

export async function previewCalendarDelete(id: string) {
  const r = await apiPost<ApiResponse<CalendarDeletePreviewResponse>>(
    calendarApiPaths.calendarDeletePreview(id),
    {}
  );
  return r.data;
}

export async function planTask(
  id: string,
  data: { plannedStart: string; plannedEnd?: string; estimatedDuration?: string }
) {
  const r = await apiPost<ApiResponse<TaskResponse>>(calendarApiPaths.taskPlan(id), data);
  return r.data;
}

export async function batchDeleteTasks(ids: string[]) {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(
    calendarApiPaths.taskBatchDelete(),
    { ids }
  );
  return r.data;
}

export async function batchUpdateTasks(data: {
  ids: string[];
  status?: string;
  priority?: number;
  calendarId?: string;
}) {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(
    calendarApiPaths.taskBatchUpdate(),
    data
  );
  return r.data;
}

interface GetEventsParams {
  search?: string;
  calendarId?: string;
  start?: string;
  end?: string;
  page?: number;
  pageSize?: number;
}

export async function getEventsPaged(params: GetEventsParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.search) searchParams.set('search', params.search);
  if (params.calendarId) searchParams.set('calendarId', params.calendarId);
  if (params.start) searchParams.set('start', params.start);
  if (params.end) searchParams.set('end', params.end);
  if (params.page) searchParams.set('page', String(params.page));
  if (params.pageSize) searchParams.set('pageSize', String(params.pageSize));

  const qs = searchParams.toString();
  const r = await apiGet<ApiResponse<PagedResult<EventResponse>>>(
    `/calendar/events?${qs}`
  );
  return r.data;
}

export async function exportIcs(ids?: string[], start?: string, end?: string) {
  const params = new URLSearchParams();
  if (ids?.length) params.set('ids', ids.join(','));
  if (start) params.set('start', start);
  if (end) params.set('end', end);

  const resp = await fetch(`/api/v1/calendar/export-ics?${params.toString()}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
  });
  if (!resp.ok) throw new Error(`Export failed: ${resp.status}`);
  const blob = await resp.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'pim-events.ics';
  a.click();
  URL.revokeObjectURL(url);
}

export async function importIcs(file: File, calendarId?: string) {
  const formData = new FormData();
  formData.append('file', file);
  if (calendarId) formData.append('calendarId', calendarId);

  const resp = await fetch('/api/v1/calendar/import-ics', {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
    body: formData
  });
  if (!resp.ok) throw new Error(`Import failed: ${resp.status}`);
  const json = await resp.json() as ApiResponse<ImportReport>;
  return json.data;
}

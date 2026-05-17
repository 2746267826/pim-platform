import { apiGet, apiPost, apiPut, apiDelete } from './client';
import type { ApiResponse, CalendarResponse, EventResponse, TaskResponse, PagedResult, ImportResult } from '../types';

export async function getCalendars() {
  const r = await apiGet<ApiResponse<CalendarResponse[]>>('/calendar/calendars');
  return r.data;
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

export async function getTasks(inboxOnly = false) {
  const r = await apiGet<ApiResponse<TaskResponse[]>>(
    `/calendar/tasks?inbox=${inboxOnly}`
  );
  return r.data;
}

export async function createTask(data: Partial<TaskResponse>) {
  const r = await apiPost<ApiResponse<TaskResponse>>('/calendar/tasks', data);
  return r.data;
}

export async function updateTask(id: string, data: Partial<TaskResponse>) {
  const r = await apiPut<ApiResponse<TaskResponse>>(`/calendar/tasks/${id}`, data);
  return r.data;
}

export async function deleteTask(id: string) {
  await apiDelete(`/calendar/tasks/${id}`);
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

export async function importIcs(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  const resp = await fetch('/api/v1/calendar/import-ics', {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
    body: formData
  });
  if (!resp.ok) throw new Error(`Import failed: ${resp.status}`);
  const json = await resp.json() as ApiResponse<ImportResult>;
  return json.data;
}

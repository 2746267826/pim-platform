import { apiGet, apiPost, apiPut, apiDelete } from './client';
import type { ApiResponse, CalendarResponse, EventResponse, TaskResponse } from '../types';

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

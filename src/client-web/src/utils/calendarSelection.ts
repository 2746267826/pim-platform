import type { CalendarResponse } from '../types';

function isWritable(cal: CalendarResponse): boolean {
  return cal.canEdit !== false;
}

export function resolveCalendarId(
  calendars: CalendarResponse[],
  currentId: string,
  hiddenIds: string[],
): string {
  if (currentId && calendars.some(c => c.id === currentId)) {
    return currentId;
  }

  const visible = calendars.filter(c => !hiddenIds.includes(c.id));
  const writable = visible.filter(isWritable);

  const defaultCal = writable.find(c => c.isDefault);
  if (defaultCal) return defaultCal.id;

  const first = writable[0];
  if (first) return first.id;

  return '';
}

export function hasWritableCalendar(calendars: CalendarResponse[]): boolean {
  return calendars.some(isWritable);
}

export function noWritableCalendarMessage(): string {
  return '没有可用的可写日历，请先在设置中添加或启用日历';
}

import { DateTime } from 'luxon';

const FORMAT = "yyyy-MM-dd'T'HH:mm";

export function isoToDatetimeLocal(iso: string, timeZoneId?: string): string {
  if (!iso) return '';
  const dt = DateTime.fromISO(iso);
  if (!dt.isValid) return '';
  const zoned = timeZoneId ? dt.setZone(timeZoneId) : dt.setZone(DateTime.local().zoneName);
  if (!zoned.isValid) return '';
  return zoned.toFormat(FORMAT);
}

export function datetimeLocalToUtcIso(local: string, timeZoneId?: string): string {
  if (!local) return '';
  const zone = timeZoneId ?? DateTime.local().zoneName;
  const dt = DateTime.fromFormat(local, FORMAT, { zone });
  if (!dt.isValid) return '';
  return dt.toUTC().toFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
}

export function minimumEndValue(startValue: string): string {
  if (!startValue) return '';
  const dt = DateTime.fromFormat(startValue, FORMAT);
  if (!dt.isValid) return '';
  return dt.plus({ minutes: 1 }).toFormat(FORMAT);
}

export function isEndAfterStart(startValue: string, endValue: string): boolean {
  if (!startValue || !endValue) return false;
  const start = DateTime.fromFormat(startValue, FORMAT);
  const end = DateTime.fromFormat(endValue, FORMAT);
  if (!start.isValid || !end.isValid) return false;
  return end > start;
}

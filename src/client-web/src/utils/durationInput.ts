const DOTNET_DURATION_RE = /^(?:(\d+)\.)?(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$/;

export function dotnetDurationToHoursMinutes(value?: string): { hours: number; minutes: number } {
  if (!value) {
    return { hours: 0, minutes: 30 };
  }
  const match = DOTNET_DURATION_RE.exec(value);
  if (!match) {
    return { hours: 0, minutes: 30 };
  }
  const days = match[1] ? parseInt(match[1], 10) : 0;
  const hours = parseInt(match[2], 10);
  const minutes = parseInt(match[3], 10);
  return { hours: days * 24 + hours, minutes };
}

export function hoursMinutesToIsoDuration(hours: number, minutes: number): string {
  const h = Math.floor(hours);
  const m = Math.floor(minutes);
  const totalMinutes = h * 60 + m;
  if (totalMinutes <= 0) return '';
  const parts: string[] = ['PT'];
  if (h > 0) parts.push(`${h}H`);
  if (m > 0) parts.push(`${m}M`);
  return parts.join('');
}

export function isValidDuration(hoursText: string, minutesText: string): boolean {
  if (!/^-?\d+$/.test(hoursText)) return false;
  if (!/^-?\d+$/.test(minutesText)) return false;
  const h = parseInt(hoursText, 10);
  const m = parseInt(minutesText, 10);
  if (h < 0) return false;
  if (m < 0 || m > 59) return false;
  return h * 60 + m >= 1;
}

export function durationErrorMessage(): string {
  return '请至少设置 1 分钟';
}

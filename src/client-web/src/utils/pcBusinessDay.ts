export const PC_BUSINESS_DAY_START_HOUR = 4;

export const PC_BUSINESS_HOURS = Array.from(
  { length: 24 },
  (_, index) => (index + PC_BUSINESS_DAY_START_HOUR) % 24,
);

export function getPcBusinessDate(date = new Date()) {
  const businessDate = new Date(date);
  if (businessDate.getHours() < PC_BUSINESS_DAY_START_HOUR) {
    businessDate.setDate(businessDate.getDate() - 1);
  }
  return businessDate;
}

export function pcHourLabel(hour: number) {
  return `${String(hour).padStart(2, '0')}:00`;
}

export function getPcBusinessDayStart(date: Date) {
  const start = new Date(date);
  if (start.getHours() < PC_BUSINESS_DAY_START_HOUR) {
    start.setDate(start.getDate() - 1);
  }
  start.setHours(PC_BUSINESS_DAY_START_HOUR, 0, 0, 0);
  return start;
}

export const PC_BUSINESS_DAY_START_HOUR = 4;

// 后端分类快照/聚合口径固定 Asia/Shanghai（无夏令时，UTC+8），业务日 04:00 起。
// 前端业务日计算必须与该口径一致，不能用浏览器时区（异地客户端会在 04:00 边界错一天）。
const SHANGHAI_OFFSET_MS = 8 * 60 * 60 * 1000;

export const PC_BUSINESS_HOURS = Array.from(
  { length: 24 },
  (_, index) => (index + PC_BUSINESS_DAY_START_HOUR) % 24,
);

/** 上海时区当前的业务日，返回 UTC 午夜 Date（表示该业务日）。格式化请用 formatPcDate。 */
export function getPcBusinessDate(date = new Date()) {
  const shanghai = new Date(date.getTime() + SHANGHAI_OFFSET_MS);
  const year = shanghai.getUTCFullYear();
  const month = shanghai.getUTCMonth();
  const day = shanghai.getUTCDate();
  const hour = shanghai.getUTCHours();
  const businessDay = hour < PC_BUSINESS_DAY_START_HOUR ? day - 1 : day;
  return new Date(Date.UTC(year, month, businessDay));
}

/** 业务日日期 → yyyy-MM-dd（UTC 字段，与后端 date 参数口径一致，不受浏览器时区影响）。 */
export function formatPcDate(date: Date) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  const day = String(date.getUTCDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
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

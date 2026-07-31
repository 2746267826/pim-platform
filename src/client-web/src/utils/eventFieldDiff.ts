import { htmlToTextSummary, looksLikeHtml } from './safeHtml';

export const EVENT_FIELD_LABELS: Record<string, string> = {
  title: '标题',
  description: '描述',
  descriptionFormat: '描述格式',
  location: '地点',
  dtStart: '开始时间',
  dtEnd: '结束时间',
  isAllDay: '全天事件',
  timeZoneId: '时区',
  showAs: '显示状态',
  importance: '重要性',
  sensitivity: '敏感度',
  categories: '分类',
  isReminderOn: '提醒',
  reminderMinutesBeforeStart: '提醒提前量',
  organizer: '组织者',
  attendees: '参会者',
  isOnlineMeeting: '在线会议',
  onlineMeetingProvider: '会议提供方',
  onlineMeetingUrl: '会议链接',
  externalLink: '外部链接',
  attachmentReferences: '附件',
};

const FIELD_KEYS = Object.keys(EVENT_FIELD_LABELS);

export type EventFieldDiffKind = 'added' | 'removed' | 'modified';

export interface EventFieldDiffEntry {
  key: string;
  label: string;
  kind: EventFieldDiffKind;
  before: unknown;
  after: unknown;
}

export type EventFieldDiffInput = Record<string, unknown>;

function isEmptyValue(value: unknown): boolean {
  if (value === null || value === undefined || value === '') return true;
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object') return Object.keys(value as object).length === 0;
  return false;
}

function deepEqual(a: unknown, b: unknown): boolean {
  if (a === b) return true;
  if (Array.isArray(a) && Array.isArray(b)) {
    if (a.length !== b.length) return false;
    return a.every((item, index) => deepEqual(item, b[index]));
  }
  if (a !== null && b !== null && typeof a === 'object' && typeof b === 'object') {
    const aKeys = Object.keys(a as object);
    const bKeys = Object.keys(b as object);
    if (aKeys.length !== bKeys.length) return false;
    return aKeys.every((key) =>
      deepEqual((a as Record<string, unknown>)[key], (b as Record<string, unknown>)[key])
    );
  }
  return false;
}

function normalizeFieldValue(record: EventFieldDiffInput, key: string, value: unknown): unknown {
  if (key !== 'description' || typeof value !== 'string') return value;
  if (value.trim() === '') return '';
  const format = typeof record.descriptionFormat === 'string'
    ? record.descriptionFormat.trim().toLowerCase()
    : '';
  if (format === 'plain') return value;
  if (format === 'html' || looksLikeHtml(value)) return htmlToTextSummary(value);
  return value;
}

export function diffEventFields(
  before: EventFieldDiffInput,
  after: EventFieldDiffInput,
): EventFieldDiffEntry[] {
  const entries: EventFieldDiffEntry[] = [];
  for (const key of FIELD_KEYS) {
    const beforeNormalized = normalizeFieldValue(before, key, before[key]);
    const afterNormalized = normalizeFieldValue(after, key, after[key]);
    const beforeValue = isEmptyValue(beforeNormalized) ? undefined : beforeNormalized;
    const afterValue = isEmptyValue(afterNormalized) ? undefined : afterNormalized;
    if (beforeValue === undefined && afterValue === undefined) continue;
    if (deepEqual(beforeValue, afterValue)) continue;
    let kind: EventFieldDiffKind;
    if (beforeValue === undefined) kind = 'added';
    else if (afterValue === undefined) kind = 'removed';
    else kind = 'modified';
    entries.push({ key, label: EVENT_FIELD_LABELS[key], kind, before: beforeValue, after: afterValue });
  }
  return entries;
}

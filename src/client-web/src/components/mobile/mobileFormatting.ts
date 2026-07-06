import type { PimHealthStatus } from '../../types';

export function formatDuration(seconds: number | null | undefined) {
  const safeSeconds = Math.max(0, Math.round(seconds ?? 0));
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);

  if (hours > 0) {
    return minutes > 0 ? `${hours} 小时 ${minutes} 分钟` : `${hours} 小时`;
  }

  if (minutes > 0) return `${minutes} 分钟`;
  return safeSeconds > 0 ? `${safeSeconds} 秒` : '0 分钟';
}

export function formatPercent(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  return `${Math.round(value * 100)}%`;
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
}

export function formatShortTime(value: string | null | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false });
}

export function statusLabel(status: string | null | undefined) {
  const normalized = status?.trim().toLowerCase();
  if (normalized === 'succeeded' || normalized === 'success' || normalized === 'completed') return '已完成';
  if (normalized === 'partial' || normalized === 'warning') return '部分接受';
  if (normalized === 'failed' || normalized === 'error') return '失败';
  if (normalized === 'pending') return '等待中';
  return status || '未知';
}

export function healthStatusLabel(status: PimHealthStatus | string | null | undefined) {
  if (status === 'Healthy') return '正常';
  if (status === 'Warning') return '需要关注';
  if (status === 'Critical') return '严重';
  return '未知';
}

export function healthToneClass(status: PimHealthStatus | string | null | undefined) {
  if (status === 'Healthy') return 'border-teal-200 bg-teal-50 text-teal-700';
  if (status === 'Warning') return 'border-amber-200 bg-amber-50 text-amber-800';
  if (status === 'Critical') return 'border-red-200 bg-red-50 text-red-700';
  return 'border-slate-200 bg-slate-50 text-slate-600';
}

import type { MobileLivenessDeviceBlock, MobileLivenessOverview, MobileLivenessSeverity } from '../../api/mobile';

export function formatCoverage(value: number | null): string {
  if (value === null || !Number.isFinite(value)) return '—';
  return `${(value * 100).toFixed(1)}%`;
}

export function silenceSeverityClass(severity: MobileLivenessSeverity): string {
  if (severity === 'warning') return 'bg-amber-100 text-amber-900';
  if (severity === 'critical') return 'bg-red-100 text-red-900';
  return '';
}

export function formatUtc(value: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false });
}

export function formatPayload(payloadJson: string): string {
  try {
    return JSON.stringify(JSON.parse(payloadJson), null, 2);
  } catch {
    return payloadJson;
  }
}

export interface MobileLivenessGroup {
  title: string;
  devices: MobileLivenessDeviceBlock[];
}

export function groupDeviceBlocks(overview: MobileLivenessOverview): MobileLivenessGroup[] {
  return [
    { title: '手机', devices: overview.phones },
    { title: '平板', devices: overview.tablets },
    { title: '未分类机型', devices: overview.unclassified },
  ];
}

export function formatAccuracyLabel(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  const rounded = Math.round(value * 10) / 10;
  return `${Number.isInteger(rounded) ? rounded.toFixed(0) : rounded.toFixed(1)} m`;
}

export function formatCoordinate(latitude: number, longitude: number) {
  return `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
}

export function providerLabel(provider: string | null | undefined) {
  const normalized = provider?.trim().toLowerCase();
  if (normalized === 'gps') return 'GPS';
  if (normalized === 'network') return '网络定位';
  if (normalized === 'fused') return '融合定位';
  if (normalized === 'passive') return '被动定位';
  return provider || '未知';
}

export function sourceKindLabel(source: string | null | undefined) {
  const normalized = source?.trim().toLowerCase();
  if (normalized === 'android') return 'Android';
  if (normalized === 'manual') return '手动';
  if (normalized === 'auto') return '自动';
  return source || '未知';
}

export function locationQualityLabel(quality: string | null | undefined) {
  const normalized = quality?.trim().toLowerCase();
  if (normalized === 'high') return '可信';
  if (normalized === 'usable') return '可用';
  if (normalized === 'rejected') return '已拒绝';
  return quality || '未知';
}

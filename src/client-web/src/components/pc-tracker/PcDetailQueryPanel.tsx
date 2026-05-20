import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { queryPcDetail } from '../../api/pcTracker';
import type { DetailQueryParams, PcDetailRecord } from '../../types';

const detailCsvColumns: { key: keyof PcDetailRecord; label: string }[] = [
  { key: 'recordType', label: 'recordType' },
  { key: 'start', label: 'start' },
  { key: 'end', label: 'end' },
  { key: 'durationSeconds', label: 'durationSeconds' },
  { key: 'deviceId', label: 'deviceId' },
  { key: 'appName', label: 'appName' },
  { key: 'displayName', label: 'displayName' },
  { key: 'categoryName', label: 'categoryName' },
  { key: 'title', label: 'title' },
  { key: 'keyPresses', label: 'keyPresses' },
  { key: 'totalClicks', label: 'totalClicks' },
  { key: 'mouseDistance', label: 'mouseDistance' },
  { key: 'scrollDistance', label: 'scrollDistance' },
  { key: 'keyCounts', label: 'keyCounts' },
  { key: 'raw', label: 'raw' },
];

function formatCsvValue(row: PcDetailRecord, key: keyof PcDetailRecord) {
  if (key === 'raw') return row.raw == null ? '' : JSON.stringify(row.raw);
  if (key === 'keyCounts') return row.keyCounts == null ? '' : JSON.stringify(row.keyCounts);
  return row[key] ?? '';
}

function downloadCSV(items: PcDetailRecord[], filename: string) {
  if (!items.length) return;
  const header = detailCsvColumns.map(c => c.label).join(',');
  const rows = items.map(row =>
    detailCsvColumns.map(c => JSON.stringify(formatCsvValue(row, c.key))).join(',')
  );
  const csv = [header, ...rows].join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function formatDate(value: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
}

function formatNumber(value: number | null) {
  return value ?? '-';
}

function formatDurationSeconds(value: number | null | undefined) {
  return value == null ? '-' : `${Math.round(value)}s`;
}

export default function PcDetailQueryPanel() {
  const [params, setParams] = useState<DetailQueryParams>({ page: 1, pageSize: 20 });

  const { data, isLoading } = useQuery({
    queryKey: ['pc-detail', params],
    queryFn: () => queryPcDetail(params),
  });

  const update = (key: keyof DetailQueryParams, value: unknown) =>
    setParams(p => ({ ...p, [key]: value, page: key === 'page' ? Number(value) : 1 }));

  return (
    <div className="space-y-4">
      {/* Filter bar */}
      <div className="grid grid-cols-4 gap-3">
        <div>
          <label className="text-xs text-gray-500">日期起</label>
          <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('dateFrom', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">日期止</label>
          <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('dateTo', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">维度</label>
          <select className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('dimension', e.target.value || undefined)}>
            <option value="">全部</option>
            <option value="hour">小时</option>
            <option value="day">天</option>
            <option value="month">月</option>
            <option value="year">年</option>
          </select>
        </div>
        <div>
          <label className="text-xs text-gray-500">事件类型</label>
          <select className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('eventType', e.target.value || undefined)}>
            <option value="">全部</option>
            <option value="window">window</option>
            <option value="afk">afk</option>
            <option value="input-minute">input-minute</option>
            <option value="app-input">app-input</option>
            <option value="key-input">key-input</option>
          </select>
        </div>
        <div>
          <label className="text-xs text-gray-500">设备</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="输入 device_id"
            onChange={e => update('deviceId', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">应用</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="进程名"
            onChange={e => update('appName', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">分类</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="分类名"
            onChange={e => update('categoryName', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">按键名</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="如 Space"
            onChange={e => update('keyName', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">排序</label>
          <select className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('sortBy', e.target.value || undefined)}>
            <option value="">默认</option>
            <option value="keyPresses">按键数</option>
            <option value="totalClicks">点击数</option>
            <option value="date">日期</option>
          </select>
        </div>
      </div>

      {/* Export buttons */}
      {data && data.items.length > 0 && (
        <div className="flex gap-2">
          <button className="px-3 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-lg hover:bg-green-100"
            onClick={() => downloadCSV(data.items, `pc-detail-${new Date().toISOString().slice(0, 10)}.csv`)}>
            导出 CSV
          </button>
          <button className="px-3 py-1 text-xs bg-blue-50 text-blue-700 border border-blue-200 rounded-lg hover:bg-blue-100"
            onClick={() => {
              const json = JSON.stringify(data.items, null, 2);
              const blob = new Blob([json], { type: 'application/json' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = `pc-detail-${new Date().toISOString().slice(0, 10)}.json`;
              a.click();
              URL.revokeObjectURL(url);
            }}>
            导出 JSON
          </button>
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto">
        {isLoading ? (
          <div className="py-8 text-center text-gray-400">查询中...</div>
        ) : !data || !data.items.length ? (
          <div className="py-8 text-center text-gray-400">暂无数据</div>
        ) : (
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Type</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Start</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">End</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Device</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">App</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Title</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Keys</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Clicks</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Scroll</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Duration</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((row, i) => (
                <tr key={`${row.recordType}-${row.start}-${i}`} className="border-b hover:bg-gray-50">
                  <td className="px-3 py-2 text-xs text-gray-700">{row.recordType}</td>
                  <td className="px-3 py-2 text-xs text-gray-700 whitespace-nowrap">{formatDate(row.start)}</td>
                  <td className="px-3 py-2 text-xs text-gray-700 whitespace-nowrap">{formatDate(row.end)}</td>
                  <td className="px-3 py-2 text-xs text-gray-700">{row.deviceId || '-'}</td>
                  <td className="px-3 py-2 text-xs text-gray-700 max-w-[160px] truncate">
                    {row.displayName || row.appName || '-'}
                  </td>
                  <td className="px-3 py-2 text-xs text-gray-700 max-w-[260px] truncate" title={row.title ?? undefined}>
                    {row.title || '-'}
                  </td>
                  <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatNumber(row.keyPresses)}</td>
                  <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatNumber(row.totalClicks)}</td>
                  <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatNumber(row.scrollDistance)}</td>
                  <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatDurationSeconds(row.durationSeconds)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button className="px-2 py-1 text-xs border rounded disabled:opacity-30"
            disabled={(params.page || 1) <= 1}
            onClick={() => update('page', Math.max(1, (params.page || 1) - 1))}>上一页</button>
          <span className="text-xs text-gray-500">第 {data.page} / {data.totalPages} 页（共 {data.totalCount} 条）</span>
          <button className="px-2 py-1 text-xs border rounded disabled:opacity-30"
            disabled={(params.page || 1) >= data.totalPages}
            onClick={() => update('page', (params.page || 1) + 1)}>下一页</button>
        </div>
      )}
    </div>
  );
}

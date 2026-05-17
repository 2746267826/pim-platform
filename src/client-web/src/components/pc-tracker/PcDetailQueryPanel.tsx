import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { queryPcDetail } from '../../api/pcTracker';
import type { DetailQueryParams } from '../../types';

function downloadCSV(items: Record<string, unknown>[], filename: string) {
  if (!items.length) return;
  const keys = Object.keys(items[0]);
  const csv = [keys.join(','), ...items.map(row => keys.map(k => JSON.stringify(row[k] ?? '')).join(','))].join('\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

export default function PcDetailQueryPanel() {
  const [params, setParams] = useState<DetailQueryParams>({ page: 1, pageSize: 20 });

  const { data, isLoading } = useQuery({
    queryKey: ['pc-detail', params],
    queryFn: () => queryPcDetail(params),
  });

  const update = (key: string, value: unknown) =>
    setParams(p => ({ ...p, [key]: value, page: key === 'page' ? p.page : 1 }));

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
            onChange={e => update('dimension', e.target.value)}>
            <option value="">全部</option>
            <option value="hour">时</option>
            <option value="day">日</option>
            <option value="month">月</option>
            <option value="year">年</option>
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
            onChange={e => update('sortBy', e.target.value)}>
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
              a.href = url; a.download = `pc-detail-${new Date().toISOString().slice(0, 10)}.json`;
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
                {Object.keys(data.items[0]).map(k => (
                  <th key={k} className="text-left px-3 py-2 text-xs text-gray-500 font-medium">{k}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.items.map((row, i) => (
                <tr key={i} className="border-b hover:bg-gray-50">
                  {Object.keys(data.items[0]).map(k => (
                    <td key={k} className="px-3 py-2 text-xs text-gray-700 max-w-[200px] truncate">
                      {String(row[k] ?? '—')}
                    </td>
                  ))}
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
            onClick={() => update('page', Math.max(1, (params.page || 1) - 1))}>‹</button>
          <span className="text-xs text-gray-500">第 {data.page} / {data.totalPages} 页（共 {data.totalCount} 条）</span>
          <button className="px-2 py-1 text-xs border rounded disabled:opacity-30"
            disabled={(params.page || 1) >= data.totalPages}
            onClick={() => update('page', (params.page || 1) + 1)}>›</button>
        </div>
      )}
    </div>
  );
}

import type { MobileLocationPoint } from '../../api/mobile';
import { formatDateTime } from './mobileFormatting';
import {
  formatAccuracyLabel,
  formatCoordinate,
  locationQualityLabel,
  providerLabel,
  sourceKindLabel,
} from './locationFormatting';

export interface LocationRawPointTableProps {
  points: MobileLocationPoint[];
  selectedPointId?: string | null;
  onSelectPoint?: (pointId: string) => void;
}

export default function LocationRawPointTable({
  points,
  selectedPointId,
  onSelectPoint,
}: LocationRawPointTableProps) {
  return (
    <section className="rounded-md border border-slate-200 bg-white">
      <div className="flex items-start justify-between gap-3 border-b border-slate-100 p-4">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-slate-950">原始点明细</h2>
          <p className="mt-1 text-xs text-slate-500">只展示当前片段内的定位点，完整原始数据继续分页读取。</p>
        </div>
        <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-600">
          {points.length} 点
        </span>
      </div>

      <div className="overflow-x-auto p-4">
        {points.length === 0 ? (
          <p className="text-sm text-slate-500">选择片段后显示原始点。</p>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="text-xs text-slate-500">
              <tr>
                <th className="pb-2 pr-3 font-medium">时间</th>
                <th className="pb-2 pr-3 font-medium">来源</th>
                <th className="pb-2 pr-3 font-medium">误差</th>
                <th className="pb-2 pr-3 font-medium">质量</th>
                <th className="pb-2 font-medium">坐标</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {points.map(point => (
                <tr
                  key={point.id}
                  data-point-id={point.id}
                  onClick={() => onSelectPoint?.(point.id)}
                  className={`cursor-pointer ${point.id === selectedPointId ? 'bg-blue-50' : 'hover:bg-slate-50'}`}
                >
                  <td className="whitespace-nowrap py-2 pr-3 text-slate-700">{formatDateTime(point.recordedAtUtc)}</td>
                  <td className="whitespace-nowrap py-2 pr-3 text-slate-600">
                    {providerLabel(point.provider)} / {sourceKindLabel(point.sourceKind)}
                  </td>
                  <td className="whitespace-nowrap py-2 pr-3 text-slate-600">{formatAccuracyLabel(point.horizontalAccuracyMeters)}</td>
                  <td className="whitespace-nowrap py-2 pr-3 text-slate-600">{locationQualityLabel(point.quality)}</td>
                  <td className="whitespace-nowrap py-2 font-mono text-xs text-slate-500">
                    {formatCoordinate(point.latitude, point.longitude)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}

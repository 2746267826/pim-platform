import { useEffect, useState } from 'react';
import type { ComponentType } from 'react';
import type { MobileLocationPoint } from '../../api/mobile';
import { formatAccuracyLabel, formatCoordinate } from './locationFormatting';
import type { HistoricalLocationLeafletMapProps } from './HistoricalLocationLeafletMap';

export interface LocationHistoryMapProps {
  points: MobileLocationPoint[];
  selectedPointId?: string | null;
  onSelectPoint?: (pointId: string) => void;
}

export default function LocationHistoryMap({
  points,
  selectedPointId,
  onSelectPoint,
}: LocationHistoryMapProps) {
  const [LeafletMap, setLeafletMap] = useState<ComponentType<HistoricalLocationLeafletMapProps> | null>(null);
  const selectedPoint = points.find(point => point.id === selectedPointId) ?? points[0];

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    let mounted = true;
    void import('./HistoricalLocationLeafletMap').then(module => {
      if (mounted) setLeafletMap(() => module.default);
    });

    return () => {
      mounted = false;
    };
  }, []);

  return (
    <section
      className="overflow-hidden rounded-lg border border-slate-200 bg-white"
      data-point-count={points.length}
    >
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 p-4">
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-slate-950">历史位置</h2>
          <p className="mt-1 text-xs text-slate-500">OpenStreetMap 底图，按时间连线</p>
        </div>
        <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-600">
          {points.length} 个定位点
        </span>
      </div>

      <div className="h-[420px] min-h-[360px] bg-slate-100">
        {LeafletMap ? (
          <LeafletMap points={points} selectedPointId={selectedPointId} onSelectPoint={onSelectPoint} />
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center text-sm text-slate-600">
            <p className="font-medium text-slate-950">OpenStreetMap</p>
            <p>浏览器中加载地图瓦片、标记和按时间连线。</p>
            {selectedPoint && (
              <p className="font-mono text-xs">
                {formatCoordinate(selectedPoint.latitude, selectedPoint.longitude)} · {formatAccuracyLabel(selectedPoint.horizontalAccuracyMeters)}
              </p>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

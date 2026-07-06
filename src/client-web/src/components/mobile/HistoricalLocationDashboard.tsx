import type { MobileDevice, MobileLocationPoint } from '../../api/mobile';
import LocationHistoryMap from './LocationHistoryMap';
import LocationPointList from './LocationPointList';

export interface HistoricalLocationDashboardProps {
  start: string;
  end: string;
  selectedDeviceId: string;
  devices: MobileDevice[];
  maxAccuracyMeters: number;
  points: MobileLocationPoint[];
  selectedPointId?: string | null;
  isLoading: boolean;
  isFetching: boolean;
  errorMessage: string | null;
  onStartChange: (value: string) => void;
  onEndChange: (value: string) => void;
  onDeviceChange: (value: string) => void;
  onMaxAccuracyChange: (value: number) => void;
  onRefresh: () => void;
  onSelectPoint: (pointId: string) => void;
}

export default function HistoricalLocationDashboard({
  start,
  end,
  selectedDeviceId,
  devices,
  maxAccuracyMeters,
  points,
  selectedPointId,
  isLoading,
  isFetching,
  errorMessage,
  onStartChange,
  onEndChange,
  onDeviceChange,
  onMaxAccuracyChange,
  onRefresh,
  onSelectPoint,
}: HistoricalLocationDashboardProps) {
  return (
    <div className="mx-auto max-w-[1500px] space-y-4 pb-8">
      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="text-xl font-semibold text-slate-950">历史位置</h1>
            <p className="mt-1 text-sm text-slate-500">查看 Android 客户端提交的历史定位点。</p>
          </div>
          <button
            type="button"
            onClick={onRefresh}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {isFetching ? '刷新中...' : '刷新'}
          </button>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-4">
          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs text-slate-500">时间范围</span>
            <input
              type="datetime-local"
              value={start}
              onChange={event => onStartChange(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            />
          </label>
          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs text-slate-500">结束时间</span>
            <input
              type="datetime-local"
              value={end}
              onChange={event => onEndChange(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            />
          </label>
          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs text-slate-500">设备</span>
            <select
              value={selectedDeviceId}
              onChange={event => onDeviceChange(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            >
              <option value="">全部设备</option>
              {devices.map(device => (
                <option key={device.deviceId} value={device.deviceId}>
                  {device.displayName || device.model || device.deviceId}
                </option>
              ))}
            </select>
          </label>
          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs text-slate-500">最大误差</span>
            <input
              type="number"
              min={1}
              value={maxAccuracyMeters}
              onChange={event => onMaxAccuracyChange(Number(event.target.value) || 50)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            />
          </label>
        </div>

        {errorMessage && (
          <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {errorMessage}
          </p>
        )}
      </section>

      {isLoading ? (
        <section className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
          正在加载历史位置...
        </section>
      ) : (
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
          <LocationHistoryMap points={points} selectedPointId={selectedPointId} onSelectPoint={onSelectPoint} />
          <LocationPointList points={points} selectedPointId={selectedPointId} onSelectPoint={onSelectPoint} />
        </div>
      )}
    </div>
  );
}

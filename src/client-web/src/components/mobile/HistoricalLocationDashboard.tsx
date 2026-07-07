import type {
  MobileDevice,
  MobileLocationAnalyticsOverview,
  MobileLocationPoint,
  MobileLocationTrack,
} from '../../api/mobile';
import type { MobileRangeShortcut } from './mobileFormatting';
import LocationHistoryMap from './LocationHistoryMap';
import LocationMetricStrip from './LocationMetricStrip';
import LocationRawPointTable from './LocationRawPointTable';
import LocationSegmentDetail from './LocationSegmentDetail';
import LocationStayMoveTimeline from './LocationStayMoveTimeline';

export interface HistoricalLocationDashboardProps {
  rangeShortcut: MobileRangeShortcut;
  rangeStartDate: string;
  rangeEndDate: string;
  selectedDeviceId: string;
  devices: MobileDevice[];
  maxAccuracyMeters: number;
  includeRejected: boolean;
  overview: MobileLocationAnalyticsOverview | null | undefined;
  tracks: MobileLocationTrack[];
  selectedSegmentId?: string | null;
  selectedPointId?: string | null;
  points: MobileLocationPoint[];
  isLoading: boolean;
  isFetching: boolean;
  errorMessage: string | null;
  onShortcutChange: (shortcut: MobileRangeShortcut) => void;
  onCustomRangeChange: (range: { startDate: string; endDate: string }) => void;
  onDeviceChange: (value: string) => void;
  onMaxAccuracyChange: (value: number) => void;
  onIncludeRejectedChange: (value: boolean) => void;
  onRefresh: () => void;
  onSelectSegment: (segmentId: string) => void;
  onSelectPoint: (pointId: string) => void;
}

const shortcuts: Array<{ value: MobileRangeShortcut; label: string }> = [
  { value: 'today', label: '今天' },
  { value: '7d', label: '7天' },
  { value: '30d', label: '30天' },
  { value: 'custom', label: '自定义' },
];

function deviceLabel(device: MobileDevice) {
  return device.displayName || device.model || device.deviceId;
}

export default function HistoricalLocationDashboard({
  rangeShortcut,
  rangeStartDate,
  rangeEndDate,
  selectedDeviceId,
  devices,
  maxAccuracyMeters,
  includeRejected,
  overview,
  tracks,
  selectedSegmentId,
  selectedPointId,
  points,
  isLoading,
  isFetching,
  errorMessage,
  onShortcutChange,
  onCustomRangeChange,
  onDeviceChange,
  onMaxAccuracyChange,
  onIncludeRejectedChange,
  onRefresh,
  onSelectSegment,
  onSelectPoint,
}: HistoricalLocationDashboardProps) {
  return (
    <div className="mx-auto max-w-[1500px] space-y-4 pb-8">
      <section className="rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="text-xl font-semibold text-slate-950">历史位置</h1>
            <p className="mt-1 text-sm text-slate-500">
              从“地图 + 点列表”升级为“轨迹、停留、质量、选中详情”一屏完成。
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex overflow-hidden rounded-md border border-slate-200 bg-slate-50">
              {shortcuts.map(shortcut => (
                <button
                  key={shortcut.value}
                  type="button"
                  onClick={() => onShortcutChange(shortcut.value)}
                  className={`px-3 py-2 text-sm font-medium ${
                    rangeShortcut === shortcut.value
                      ? 'bg-slate-950 text-white'
                      : 'text-slate-700 hover:bg-white'
                  }`}
                >
                  {shortcut.label}
                </button>
              ))}
            </div>
            <span className="rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">北京时间</span>
            <button
              type="button"
              onClick={onRefresh}
              className="rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              {isFetching ? '刷新中...' : '刷新'}
            </button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-6">
          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs font-semibold text-slate-500">设备</span>
            <select
              value={selectedDeviceId}
              onChange={event => onDeviceChange(event.target.value)}
              className="h-9 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-700"
            >
              <option value="">全部设备</option>
              {devices.map(device => (
                <option key={device.deviceId} value={device.deviceId}>
                  {deviceLabel(device)}
                </option>
              ))}
            </select>
          </label>

          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs font-semibold text-slate-500">范围</span>
            <span className="grid grid-cols-2 gap-2">
              <input
                aria-label="开始日期"
                type="date"
                value={rangeStartDate}
                onChange={event => onCustomRangeChange({ startDate: event.target.value, endDate: rangeEndDate })}
                className="h-9 min-w-0 rounded-md border border-slate-200 px-2 text-sm text-slate-700"
              />
              <input
                aria-label="结束日期"
                type="date"
                value={rangeEndDate}
                onChange={event => onCustomRangeChange({ startDate: rangeStartDate, endDate: event.target.value })}
                className="h-9 min-w-0 rounded-md border border-slate-200 px-2 text-sm text-slate-700"
              />
            </span>
          </label>

          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs font-semibold text-slate-500">最大误差</span>
            <input
              type="number"
              min={1}
              value={maxAccuracyMeters}
              onChange={event => onMaxAccuracyChange(Number(event.target.value) || 50)}
              className="h-9 w-full rounded-md border border-slate-200 px-3 text-sm text-slate-700"
            />
          </label>

          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs font-semibold text-slate-500">展示</span>
            <select className="h-9 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-700" value="tracks-stays" onChange={() => undefined}>
              <option value="tracks-stays">轨迹 + 停留点</option>
            </select>
          </label>

          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs font-semibold text-slate-500">质量</span>
            <span className="flex h-9 items-center justify-between gap-2 rounded-md border border-slate-200 px-3 text-sm text-slate-700">
              <span>隐藏已拒绝点</span>
              <input
                aria-label="隐藏已拒绝点"
                type="checkbox"
                checked={!includeRejected}
                onChange={event => onIncludeRejectedChange(!event.target.checked)}
                className="h-4 w-4"
              />
            </span>
          </label>

          <label className="min-w-0 text-sm">
            <span className="mb-1 block text-xs font-semibold text-slate-500">搜索地点</span>
            <input
              type="search"
              placeholder="地点或坐标"
              className="h-9 w-full rounded-md border border-slate-200 px-3 text-sm text-slate-700"
            />
          </label>
        </div>

        <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
          <span>{rangeStartDate} 至 {rangeEndDate}</span>
          <span>最大误差 {maxAccuracyMeters} m</span>
          <span>{includeRejected ? '显示已拒绝点' : '隐藏已拒绝点'}</span>
        </div>

        {errorMessage && (
          <p className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {errorMessage}
          </p>
        )}
      </section>

      <LocationMetricStrip overview={overview} />

      {isLoading ? (
        <section className="rounded-md border border-slate-200 bg-white p-4 text-sm text-slate-500">
          正在加载历史位置...
        </section>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_390px]">
            <LocationHistoryMap
              tracks={tracks}
              selectedSegmentId={selectedSegmentId}
              selectedPointId={selectedPointId}
              onSelectSegment={onSelectSegment}
              onSelectPoint={onSelectPoint}
            />
            <LocationSegmentDetail tracks={tracks} selectedSegmentId={selectedSegmentId} />
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <LocationStayMoveTimeline
              tracks={tracks}
              selectedSegmentId={selectedSegmentId}
              onSelectSegment={onSelectSegment}
            />
            <LocationRawPointTable
              points={points}
              selectedPointId={selectedPointId}
              onSelectPoint={onSelectPoint}
            />
          </div>
        </>
      )}
    </div>
  );
}

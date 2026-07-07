import type { MobileDevice } from '../../api/mobile';
import { MOBILE_LIFE_CATEGORIES } from '../../api/mobile';
import type { MobileRangeShortcut } from './mobileFormatting';

export interface MobileAnalyticsHeaderProps {
  rangeShortcut: MobileRangeShortcut;
  rangeStartDate: string;
  rangeEndDate: string;
  selectedDeviceId: string;
  devices: MobileDevice[];
  selectedCategory: string;
  packageName: string;
  includeSystemNoise: boolean;
  isFetching: boolean;
  errorMessage?: string | null;
  onShortcutChange: (shortcut: Exclude<MobileRangeShortcut, 'custom'>) => void;
  onCustomRangeChange: (range: { startDate: string; endDate: string }) => void;
  onDeviceChange: (deviceId: string) => void;
  onCategoryChange: (category: string) => void;
  onPackageNameChange: (packageName: string) => void;
  onIncludeSystemNoiseChange: (include: boolean) => void;
  onRefresh: () => void;
}

const shortcuts: Array<{ key: Exclude<MobileRangeShortcut, 'custom'>; label: string }> = [
  { key: 'today', label: '今天' },
  { key: '7d', label: '7天' },
  { key: '30d', label: '30天' },
];

export default function MobileAnalyticsHeader({
  rangeShortcut,
  rangeStartDate,
  rangeEndDate,
  selectedDeviceId,
  devices,
  selectedCategory,
  packageName,
  includeSystemNoise,
  isFetching,
  errorMessage = null,
  onShortcutChange,
  onCustomRangeChange,
  onDeviceChange,
  onCategoryChange,
  onPackageNameChange,
  onIncludeSystemNoiseChange,
  onRefresh,
}: MobileAnalyticsHeaderProps) {
  return (
    <section className="border-b border-slate-200 bg-white px-4 py-4 sm:px-6">
      <div className="mx-auto flex max-w-[1500px] flex-col gap-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="text-2xl font-semibold tracking-normal text-slate-950">手机记录</h1>
            <p className="mt-1 text-sm text-slate-500">北京时间使用分析、异常提醒与应用分类治理</p>
          </div>
          <button
            type="button"
            onClick={onRefresh}
            className="h-10 rounded-md border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {isFetching ? '刷新中...' : '刷新'}
          </button>
        </div>

        <div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(0,1.25fr)_minmax(360px,0.85fr)]">
          <div className="flex min-w-0 flex-wrap items-end gap-3">
            <div className="flex h-10 rounded-md border border-slate-200 bg-slate-50 p-1" aria-label="日期快捷范围">
              {shortcuts.map(shortcut => {
                const isActive = rangeShortcut === shortcut.key;
                return (
                  <button
                    key={shortcut.key}
                    type="button"
                    onClick={() => onShortcutChange(shortcut.key)}
                    className={`min-w-14 rounded px-3 text-sm font-medium ${
                      isActive ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-600 hover:bg-white'
                    }`}
                    aria-pressed={isActive}
                  >
                    {shortcut.label}
                  </button>
                );
              })}
              <span className={`flex min-w-16 items-center justify-center rounded px-3 text-sm font-medium ${
                rangeShortcut === 'custom' ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-500'
              }`}>
                自定义
              </span>
            </div>

            <label className="min-w-36 text-xs font-medium text-slate-500">
              开始
              <input
                aria-label="开始日期"
                type="date"
                value={rangeStartDate}
                onChange={event => onCustomRangeChange({ startDate: event.target.value, endDate: rangeEndDate })}
                className="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900"
              />
            </label>
            <label className="min-w-36 text-xs font-medium text-slate-500">
              结束
              <input
                aria-label="结束日期"
                type="date"
                value={rangeEndDate}
                onChange={event => onCustomRangeChange({ startDate: rangeStartDate, endDate: event.target.value })}
                className="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900"
              />
            </label>

            <label className="min-w-48 flex-1 text-xs font-medium text-slate-500">
              设备
              <select
                value={selectedDeviceId}
                onChange={event => onDeviceChange(event.target.value)}
                className="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900"
              >
                <option value="">全部设备</option>
                {devices.map(device => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.displayName || device.model || device.deviceId}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-[minmax(160px,0.9fr)_minmax(180px,1fr)_auto]">
            <label className="text-xs font-medium text-slate-500">
              分类
              <select
                value={selectedCategory}
                onChange={event => onCategoryChange(event.target.value)}
                className="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900"
              >
                <option value="">全部分类</option>
                {MOBILE_LIFE_CATEGORIES.map(category => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </label>

            <label className="text-xs font-medium text-slate-500">
              应用包名
              <input
                type="search"
                value={packageName}
                onChange={event => onPackageNameChange(event.target.value)}
                placeholder="com.example.app"
                className="mt-1 h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900"
              />
            </label>

            <label className="flex h-10 items-center gap-2 self-end whitespace-nowrap rounded-md border border-slate-200 px-3 text-sm text-slate-700">
              <input
                aria-label="显示系统与短事件"
                type="checkbox"
                checked={includeSystemNoise}
                onChange={event => onIncludeSystemNoiseChange(event.target.checked)}
                className="h-4 w-4"
              />
              显示系统与短事件
            </label>
          </div>
        </div>

        {errorMessage && (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {errorMessage}
          </p>
        )}
      </div>
    </section>
  );
}

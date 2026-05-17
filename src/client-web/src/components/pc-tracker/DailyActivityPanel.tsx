// src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx
import type { DerivedMetrics, CategorySummary, AppRankingItem } from '../../types';

interface Props {
  metrics: DerivedMetrics | null;
  categories: CategorySummary[];
  appRanking: AppRankingItem[];
  selectedCategory: string | null;
  onSelectCategory: (cat: string | null) => void;
  selectedApp: string | null;
  onSelectApp: (app: string | null) => void;
}

function MetricCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-white rounded-lg p-3 text-center border border-gray-100">
      <div className="text-[11px] text-gray-400 mb-1">{label}</div>
      <div className="text-base font-bold text-gray-800">{value}</div>
    </div>
  );
}

export default function DailyActivityPanel({ metrics, categories, appRanking, selectedCategory, onSelectCategory, selectedApp, onSelectApp }: Props) {
  if (!metrics) return <div className="py-8 text-center text-gray-400">暂无活动数据</div>;

  const top5Categories = categories.slice(0, 5);
  const top5Apps = appRanking.slice(0, 5);
  const totalInput = top5Apps.reduce((s, a) => s + a.keyPresses + a.totalClicks, 0) || 1;

  return (
    <div className="space-y-4">
      {/* Metrics grid — 4+4+3 */}
      <div className="grid grid-cols-4 gap-3">
        <MetricCard label="累计记录时长" value={metrics.totalRecordedDuration} />
        <MetricCard label="有输入时长" value={metrics.activeInputDuration} />
        <MetricCard label="空闲时长" value={metrics.idleDuration} />
        <MetricCard label="独立工作会话" value={`${metrics.sessionCount} 个`} />
      </div>
      <div className="grid grid-cols-4 gap-3">
        <MetricCard label="活跃应用数" value={`${metrics.activeAppCount} 个`} />
        <MetricCard label="键盘按键总数" value={metrics.totalKeyPresses.toLocaleString()} />
        <MetricCard label="点击总数" value={metrics.totalClicks.toLocaleString()} />
        <MetricCard label="应用切换次数" value={`${metrics.appSwitchCount} 次`} />
      </div>
      <div className="grid grid-cols-3 gap-3">
        <MetricCard label="切换频率" value={`${metrics.switchFrequency} 次/10min`} />
        <MetricCard label="最专注应用" value={metrics.mostFocusedApp} />
        <MetricCard label="按键/点击比" value={`${metrics.keyClickRatio}:1`} />
      </div>

      {/* Top 5 categories */}
      <div>
        <div className="text-xs text-gray-400 mb-2">🏷️ 前五分类</div>
        <div className="flex flex-wrap gap-2">
          {top5Categories.map(c => (
            <button key={c.categoryName} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              selectedCategory === c.categoryName ? 'ring-2 ring-blue-400 bg-blue-50' : 'bg-gray-100 hover:bg-gray-200'
            }`}
              style={{ backgroundColor: selectedCategory === c.categoryName ? undefined : c.color + '18', color: c.color }}
              onClick={() => onSelectCategory(selectedCategory === c.categoryName ? null : c.categoryName)}>
              {c.categoryName} {c.share}%
            </button>
          ))}
        </div>
      </div>

      {/* Top 5 apps */}
      <div>
        <div className="text-xs text-gray-400 mb-2">⚙️ 前五应用（进程名）</div>
        <div className="flex flex-wrap gap-2">
          {top5Apps.map(a => {
            const share = Math.round((a.keyPresses + a.totalClicks) / totalInput * 100);
            return (
              <button key={a.appName} className={`px-3 py-1.5 rounded-lg text-xs transition-colors ${
                selectedApp === a.appName ? 'ring-2 ring-blue-400 bg-blue-50' : 'bg-gray-100 hover:bg-gray-200'
              }`}
                onClick={() => onSelectApp(selectedApp === a.appName ? null : a.appName)}>
                {a.appName} <span className="text-gray-400">{share}%</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

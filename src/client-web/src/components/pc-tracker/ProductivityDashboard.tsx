import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { getProductivityDashboard, type ProductivityDashboard } from '../../api/pcTracker';

function CircularScore({ score }: { score: number }) {
  const r = 40;
  const circumference = 2 * Math.PI * r;
  const offset = circumference - (score / 100) * circumference;
  const color = score >= 70 ? '#22C55E' : score >= 50 ? '#F59E0B' : '#EF4444';

  return (
    <div className="relative w-28 h-28 flex items-center justify-center">
      <svg className="w-28 h-28 -rotate-90" viewBox="0 0 100 100">
        <circle cx="50" cy="50" r={r} fill="none" stroke="#E2E8F0" strokeWidth="8" />
        <circle
          cx="50" cy="50" r={r}
          fill="none"
          stroke={color}
          strokeWidth="8"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-2xl font-bold" style={{ color }}>{Math.round(score)}</span>
        <span className="text-[10px] text-slate-400">分</span>
      </div>
    </div>
  );
}

export default function ProductivityDashboardPanel() {
  const today = format(new Date(), 'yyyy-MM-dd');

  const { data, isLoading } = useQuery({
    queryKey: ['productivity-dashboard', today],
    queryFn: () => getProductivityDashboard(today),
  });

  if (isLoading) {
    return (
      <div className="pim-panel p-4">
        <h3 className="text-sm font-semibold text-slate-800 mb-3">今日效率</h3>
        <div className="text-sm text-slate-400 text-center py-4">加载中...</div>
      </div>
    );
  }

  if (!data) return null;

  const maxWeekMinutes = Math.max(...data.weeklyTrend.map(d => d.totalMinutes), 1);
  const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

  return (
    <div className="pim-panel p-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-slate-800">今日效率</h3>
        {data.goalMet ? (
          <span className="text-xs bg-emerald-100 text-emerald-600 px-2 py-0.5 rounded">✅ 达标</span>
        ) : (
          <span className="text-xs bg-amber-100 text-amber-600 px-2 py-0.5 rounded">⏳ 未达标</span>
        )}
      </div>

      <div className="flex items-center gap-6">
        <CircularScore score={data.todayScore} />
        <div className="flex-1 space-y-1.5">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            <span className="text-xs text-slate-500">生产性</span>
            <span className="text-sm font-medium text-slate-800 ml-auto">{data.productiveHours.toFixed(1)}h</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-slate-400" />
            <span className="text-xs text-slate-500">中性</span>
            <span className="text-sm font-medium text-slate-800 ml-auto">{data.neutralHours.toFixed(1)}h</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-rose-400" />
            <span className="text-xs text-slate-500">分心</span>
            <span className="text-sm font-medium text-slate-800 ml-auto">{data.distractingHours.toFixed(1)}h</span>
          </div>
          <div className="border-t border-slate-100 pt-1.5 mt-1.5">
            <span className="text-xs text-slate-400">目标: {data.targetHours.toFixed(1)}h/天</span>
          </div>
        </div>
      </div>

      <div className="mt-4">
        <h4 className="text-xs font-medium text-slate-500 mb-2">本周趋势</h4>
        <div className="flex items-end gap-1.5 h-20">
          {data.weeklyTrend.map((day, i) => (
            <div key={day.date} className="flex-1 flex flex-col items-center gap-1">
              <div className="w-full flex flex-col-reverse rounded-t" style={{ height: `${Math.max((day.totalMinutes / maxWeekMinutes) * 100, 4)}%` }}>
                <div style={{ height: `${(day.productiveMinutes / Math.max(day.totalMinutes, 1)) * 100}%` }} className="w-full bg-emerald-400 rounded-t" title={`生产性: ${day.productiveMinutes.toFixed(0)}分钟`} />
              </div>
              <span className="text-[10px] text-slate-400">{dayNames[i]}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

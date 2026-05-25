import EmptyState from '../../ui/EmptyState';
import MetricCard from '../../ui/MetricCard';
import StatusBadge from '../../ui/StatusBadge';
import type { PcActivityTodayData, TodaySection } from '../../types';
import { PC_BUSINESS_HOURS, pcHourLabel } from '../../utils/pcBusinessDay';

function formatNumber(value: number | undefined) {
  return (value ?? 0).toLocaleString('zh-CN');
}

function intensityClass(score: number, max: number) {
  if (max <= 0 || score <= 0) return 'bg-slate-100';
  const ratio = score / max;
  if (ratio > 0.75) return 'bg-teal-600';
  if (ratio > 0.5) return 'bg-teal-500';
  if (ratio > 0.25) return 'bg-teal-300';
  return 'bg-teal-100';
}

export default function TodayPcOverview({ section }: { section: TodaySection<PcActivityTodayData> }) {
  const summary = section.data.summary;
  const metrics = summary.metrics;
  const keystats = summary.keystats;
  const heatmap = PC_BUSINESS_HOURS.map(hour => {
    const bucket = summary.heatmap.find(item => item.hour === hour);
    return {
      hour,
      activeMinutes: bucket?.activeMinutes ?? 0,
      totalEvents: bucket?.totalEvents ?? 0,
      intensityScore: bucket?.intensityScore ?? 0,
    };
  });
  const maxIntensity = Math.max(...heatmap.map(item => item.intensityScore), 0);

  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="font-semibold text-slate-900">PC 记录概览</h2>
          <p className="mt-1 text-xs text-slate-500">输入、应用活跃度与 24 小时热力分布</p>
        </div>
        <StatusBadge tone={section.status === 'empty' ? 'neutral' : 'activity'}>
          {section.status === 'empty' ? '暂无数据' : '今日'}
        </StatusBadge>
      </div>

      {section.status === 'empty' ? (
        <EmptyState title="暂无 PC 记录" description="守护程序同步后会显示今天的使用概览。" />
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <MetricCard
              label="记录时长"
              value={metrics?.totalRecordedDuration ?? '-'}
              helper={metrics?.mostFocusedApp ? `最专注：${metrics.mostFocusedApp}` : '等待同步'}
              tone="primary"
            />
            <MetricCard
              label="活跃输入"
              value={metrics?.activeInputDuration ?? '-'}
              helper={`会话 ${metrics?.sessionCount ?? 0} 次`}
              tone="activity"
            />
            <MetricCard
              label="按键"
              value={formatNumber(keystats?.keyPresses ?? metrics?.totalKeyPresses)}
              helper={`峰值 ${keystats?.peakKps ?? 0} KPS`}
              tone="neutral"
            />
            <MetricCard
              label="点击"
              value={formatNumber(keystats?.totalClicks ?? metrics?.totalClicks)}
              helper={`应用 ${metrics?.activeAppCount ?? 0} 个`}
              tone="warning"
            />
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="mb-3 flex items-center justify-between">
              <p className="text-sm font-medium text-slate-800">24 小时热力图</p>
              <p className="text-xs text-slate-500">04:00 起算，越深表示活动越集中</p>
            </div>
            <div className="grid grid-cols-12 gap-1.5" role="img" aria-label="今日 24 小时 PC 活跃热力图">
              {heatmap.map(item => (
                <div
                  key={item.hour}
                  className={`h-8 rounded-md ${intensityClass(item.intensityScore, maxIntensity)}`}
                  title={`${pcHourLabel(item.hour)}，活跃 ${item.activeMinutes} 分钟，事件 ${item.totalEvents} 次`}
                />
              ))}
            </div>
            <div className="mt-2 grid grid-cols-4 text-xs text-slate-400">
              <span>04:00</span>
              <span className="text-center">10:00</span>
              <span className="text-center">16:00</span>
              <span className="text-right">22:00</span>
            </div>
          </div>

          {summary.appRanking?.length ? (
            <div className="space-y-2">
              <p className="text-sm font-medium text-slate-800">主要应用</p>
              {summary.appRanking.slice(0, 4).map(app => (
                <div key={app.appName} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2">
                  <span className="min-w-0 truncate text-sm text-slate-700">{app.displayName || app.appName}</span>
                  <span className="text-xs font-medium text-slate-500">{Math.round(app.share * 100)}%</span>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      )}
    </section>
  );
}

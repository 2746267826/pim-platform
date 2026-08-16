import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import EmptyState from '../../ui/EmptyState';
import MetricCard from '../../ui/MetricCard';
import StatusBadge from '../../ui/StatusBadge';
import type { PcActivityTodayData, TodaySection } from '../../types';
import { getPcCategoryDistribution, getPcFocusBlocks } from '../../api/pcTracker';
import { getPcBusinessDate } from '../../utils/pcBusinessDay';
import EChartBox from '../charts/EChartBox';
import {
  buildCategoryDonutOption,
  buildFocusSummary,
  buildTodayActivityAreaOption,
} from '../charts/pcTodayOptions';

function formatNumber(value: number | undefined) {
  return (value ?? 0).toLocaleString('zh-CN');
}

export default function TodayPcOverview({ section }: { section: TodaySection<PcActivityTodayData> }) {
  const summary = section.data.summary;
  const metrics = summary.metrics;
  const keystats = summary.keystats;
  // 今日业务日期字符串（yyyy-MM-dd，与后端 date 参数一致）：避免 query key 固定 + 无 date 参数导致的跨日缓存陈旧。
  const dateStr = format(getPcBusinessDate(), 'yyyy-MM-dd');

  const categoryQuery = useQuery({
    queryKey: ['pc-category-distribution', dateStr],
    queryFn: () => getPcCategoryDistribution({ date: dateStr }),
    enabled: section.status !== 'empty',
  });
  const focusQuery = useQuery({
    queryKey: ['pc-focus-blocks', dateStr],
    queryFn: () => getPcFocusBlocks({ date: dateStr }),
    enabled: section.status !== 'empty',
  });

  const categoryItems = categoryQuery.data?.items ?? [];
  const focusSummary = focusQuery.data ? buildFocusSummary(focusQuery.data.items) : null;

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
            <EChartBox
              option={buildTodayActivityAreaOption(summary.heatmap)}
              height={160}
              ariaLabel="今日 24 小时 PC 活跃面积图"
            />
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="mb-2 text-sm font-medium text-slate-800">分类分布</p>
              {categoryQuery.isLoading && <p className="mb-1 text-xs text-slate-400">加载中</p>}
              {categoryQuery.isError && <p className="mb-1 text-xs text-slate-400">暂无数据</p>}
              {!categoryQuery.isLoading && !categoryQuery.isError && categoryItems.length === 0 && (
                <p className="mb-1 text-xs text-slate-400">暂无数据</p>
              )}
              <EChartBox
                option={buildCategoryDonutOption(categoryItems)}
                height={200}
                ariaLabel="今日 PC 分类分布环图"
              />
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="mb-2 text-sm font-medium text-slate-800">专注段</p>
              {focusQuery.isLoading && <p className="text-xs text-slate-400">加载中</p>}
              {focusQuery.isError && <p className="text-xs text-slate-400">暂无数据</p>}
              {!focusQuery.isLoading && !focusQuery.isError && (!focusSummary || focusSummary.count === 0) && (
                <p className="text-xs text-slate-400">暂无数据</p>
              )}
              {!focusQuery.isLoading && !focusQuery.isError && focusSummary && focusSummary.count > 0 && (
                <div className="grid grid-cols-3 gap-3">
                  <MetricCard label="专注段" value={`${focusSummary.count} 段`} helper="今日专注块数" tone="primary" />
                  <MetricCard label="最长" value={`${focusSummary.longestMinutes} 分钟`} helper="单段最长" tone="activity" />
                  <MetricCard label="合计" value={`${focusSummary.totalMinutes} 分钟`} helper="专注总时长" tone="warning" />
                </div>
              )}
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

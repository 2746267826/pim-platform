import { chartColors } from './chartColors';
import { buildFocusSummary } from './pcTodayOptions';
import type { EChartsOption } from '../../lib/echarts';
import type { PcSummaryResponse } from '../../types';
import type {
  PcAppUsageResponse,
  PcCategoryDistributionResponse,
  PcFocusBlocksResponse,
  PcLateNightResponse,
} from '../../api/pcTracker';

/** 电脑记录页图表 option 纯函数：输入数据、输出 EChartsOption，不依赖组件/页面。 */

export { buildQualityRingOption } from './pcTodayOptions';

export type MetricTone = 'primary' | 'activity' | 'warning' | 'danger' | 'neutral';

export interface ReviewMetric {
  label: string;
  value: string;
  helper: string;
  tone: MetricTone;
}

function formatCount(value: number) {
  return value.toLocaleString('zh-CN');
}

/** 应用时长横向 bar：yAxis category inverse（首行最高），色取分类色或 primary，label 右侧 `X 分钟`。 */
export function buildAppUsageBarOption(appUsage: PcAppUsageResponse | undefined): EChartsOption {
  const top = [...(appUsage?.items ?? [])]
    .sort((a, b) => b.totalMinutes - a.totalMinutes)
    .slice(0, 8);

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const axisParams = Array.isArray(params) ? params : [params];
        const p = axisParams[0] as { dataIndex?: number } | undefined;
        const item = p?.dataIndex !== undefined ? top[p.dataIndex] : undefined;
        if (!item) return '';
        return `${item.appName}：${item.totalMinutes} 分钟（${item.percentage}%）`;
      },
    },
    grid: { left: 8, right: 56, top: 8, bottom: 8, containLabel: true },
    xAxis: [{ type: 'value', show: false }],
    yAxis: [
      {
        type: 'category',
        inverse: true,
        data: top.map(item => item.displayName ?? item.appName),
        axisLabel: { fontSize: 11, color: chartColors.textMuted },
        axisLine: { lineStyle: { color: chartColors.borderSoft } },
        axisTick: { show: false },
      },
    ],
    series: [
      {
        type: 'bar',
        barWidth: 14,
        label: { show: true, position: 'right', formatter: '{c} 分钟', fontSize: 10, color: chartColors.textMuted },
        itemStyle: { borderRadius: [0, 7, 7, 0], color: chartColors.primary },
        data: top.map(item => ({
          value: item.totalMinutes,
          itemStyle: { color: chartColors.category[item.appName] ?? chartColors.primary },
        })),
      },
    ],
  };
}

/**
 * 复盘指标行：summary 指标 + 聚合数据（专注块/深夜使用/分类分布）驱动的 7 张 MetricCard。
 * 聚合数据缺失时该卡值显示 '—'、helper 显示「等待同步」，不留空。
 */
export function buildReviewMetrics(
  summary: PcSummaryResponse | undefined,
  focusBlocks: PcFocusBlocksResponse | undefined,
  lateNight: PcLateNightResponse | undefined,
  distribution: PcCategoryDistributionResponse | undefined,
): ReviewMetric[] {
  const metrics = summary?.metrics;
  const focus = focusBlocks ? buildFocusSummary(focusBlocks.items) : null;

  let lateMinutes: number | null = null;
  const lateItems = lateNight?.items ?? [];
  if (lateItems.length > 0) {
    const lastActivity = [...lateItems].reverse().find(item => item.hadActivity);
    const pick = lastActivity ?? lateItems[lateItems.length - 1];
    lateMinutes = pick.minutes;
  }

  let coverage: number | null = null;
  let otherPct = 0;
  const distItems = distribution?.items ?? [];
  if (distItems.length > 0) {
    const other = distItems.find(item => item.categoryName === '其他');
    otherPct = other?.percentage ?? 0;
    coverage = 100 - otherPct;
  }

  const mainCategoryName = summary?.categories?.[0]?.categoryName || '暂无';

  return [
    {
      label: '记录时长',
      value: metrics?.totalRecordedDuration ?? '—',
      helper: metrics ? '今日记录' : '等待同步',
      tone: 'primary',
    },
    {
      label: '活跃输入',
      value: metrics?.activeInputDuration ?? '—',
      helper: metrics ? '有效输入时长' : '等待同步',
      tone: 'activity',
    },
    {
      label: '主要分类',
      value: mainCategoryName,
      helper: '占比最高',
      tone: 'warning',
    },
    {
      label: '上下文切换',
      value: metrics ? formatCount(metrics.appSwitchCount) : '—',
      helper: metrics ? '今日切换次数' : '等待同步',
      tone: 'neutral',
    },
    {
      label: '专注块',
      value: focus ? `${focus.count} 段` : '—',
      helper: focus ? `最长 ${focus.longestMinutes} 分钟` : '等待同步',
      tone: 'primary',
    },
    {
      label: '深夜使用',
      value: lateMinutes !== null ? `${lateMinutes} 分钟` : '—',
      helper: lateMinutes !== null ? '23:30 后' : '等待同步',
      tone: 'warning',
    },
    {
      label: '分类覆盖率',
      value: coverage !== null ? `${coverage}%` : '—',
      helper: coverage !== null ? `100% - 其他 ${otherPct}%` : '等待同步',
      tone: 'activity',
    },
  ];
}

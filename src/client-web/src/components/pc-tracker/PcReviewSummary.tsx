import type { ActivityClassificationSuggestion, PcSummaryResponse } from '../../types';

interface Props {
  summary: PcSummaryResponse | undefined;
  pendingSuggestions: ActivityClassificationSuggestion[];
}

function formatCount(value: number) {
  return value.toLocaleString('zh-CN');
}

function mainCategory(summary: PcSummaryResponse | undefined) {
  const category = summary?.categories?.[0];
  return category?.categoryName || '暂无';
}

export default function PcReviewSummary({ summary, pendingSuggestions }: Props) {
  const metrics = summary?.metrics;
  const totalInputs = (metrics?.totalKeyPresses ?? 0) + (metrics?.totalClicks ?? 0);

  const cards = [
    { label: '记录时长', value: metrics?.totalRecordedDuration ?? '-' },
    { label: '有效输入', value: metrics?.activeInputDuration ?? '-' },
    { label: '主要分类', value: mainCategory(summary) },
    { label: '上下文切换', value: metrics ? formatCount(metrics.appSwitchCount) : '-' },
    { label: '待确认上下文', value: formatCount(pendingSuggestions.length) },
    { label: '输入活跃度', value: formatCount(totalInputs) },
  ];

  return (
    <section className="pim-panel p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-base font-semibold text-slate-950">今日复盘</h2>
          <p className="mt-1 text-sm text-slate-500">
            先看今天的活动结构，再确认需要写入 App 知识库的上下文。
          </p>
        </div>
        {metrics?.mostFocusedApp && (
          <div className="rounded-lg border border-blue-100 bg-blue-50 px-3 py-2 text-xs font-medium text-blue-700">
            最聚焦：{metrics.mostFocusedApp}
          </div>
        )}
      </div>
      <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        {cards.map(card => (
          <div key={card.label} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <div className="text-xs font-medium text-slate-500">{card.label}</div>
            <div className="mt-1 min-h-7 break-words text-lg font-semibold text-slate-950">{card.value}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

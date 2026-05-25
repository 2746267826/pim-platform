import { Link } from 'react-router-dom';
import EmptyState from '../../ui/EmptyState';
import StatusBadge from '../../ui/StatusBadge';
import type { ClassificationSuggestionsTodayData, TodaySection } from '../../types';

function formatMinutes(totalDurationSeconds: number) {
  return `${Math.round(totalDurationSeconds / 60)} 分钟`;
}

export default function TodayClassificationSuggestionsSection({
  section,
}: {
  section: TodaySection<ClassificationSuggestionsTodayData>;
}) {
  const { pendingCount, suggestions } = section.data;

  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">分类建议</h2>
        <StatusBadge tone={pendingCount > 0 ? 'warning' : 'neutral'}>{pendingCount} 待处理</StatusBadge>
      </div>

      <div className="space-y-3">
        {pendingCount === 0 ? (
          <EmptyState title="暂无分类建议" description="新的 PC 活动建议会显示在这里。" />
        ) : (
          <div className="space-y-2">
            {suggestions.slice(0, 3).map(suggestion => (
              <div key={suggestion.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                <p className="truncate text-sm font-medium text-slate-900">
                  {suggestion.suggestedCategory || suggestion.currentCategory || suggestion.clusterKey}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  {suggestion.sampleCount} 条样本 · {formatMinutes(suggestion.totalDurationSeconds)}
                </p>
              </div>
            ))}
          </div>
        )}
        <Link className="text-sm font-medium text-blue-600 hover:text-blue-700" to="/pc-tracker">
          查看分类建议
        </Link>
      </div>
    </section>
  );
}

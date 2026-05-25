import type { ActivityClassificationSuggestion } from '../../types';

interface Props {
  suggestions: ActivityClassificationSuggestion[];
  isLoading: boolean;
  onCorrect: (suggestion: ActivityClassificationSuggestion) => void;
  onReject: (suggestion: ActivityClassificationSuggestion) => void;
}

function formatMinutes(seconds: number) {
  const minutes = Math.round((seconds / 60) * 10) / 10;
  return `${minutes.toLocaleString('zh-CN')} 分钟`;
}

export default function ClassificationSuggestionPanel({
  suggestions,
  isLoading,
  onCorrect,
  onReject,
}: Props) {
  if (isLoading) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        正在加载分类建议...
      </div>
    );
  }

  const visibleSuggestions = suggestions.slice(0, 5);

  if (visibleSuggestions.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        暂无需要处理的分类建议。
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {visibleSuggestions.map(suggestion => (
        <div
          key={suggestion.id}
          className="flex min-w-0 flex-col gap-3 rounded-lg border border-slate-200 bg-white px-3 py-3 md:flex-row md:items-center md:justify-between"
        >
          <div className="min-w-0">
            <div className="flex min-w-0 flex-wrap items-center gap-2">
              <span className="min-w-0 max-w-full truncate text-sm font-semibold text-slate-950">
                {suggestion.clusterKey}
              </span>
              {suggestion.suggestedCategory && (
                <span className="shrink-0 rounded-full border border-teal-200 bg-teal-50 px-2 py-0.5 text-xs font-medium text-teal-700">
                  {suggestion.suggestedCategory}
                </span>
              )}
              {suggestion.suggestedProjectTag && (
                <span className="min-w-0 max-w-full truncate rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
                  {suggestion.suggestedProjectTag}
                </span>
              )}
            </div>
            <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-slate-500 sm:flex sm:flex-wrap">
              <div className="min-w-0">
                <dt className="inline">样本 </dt>
                <dd className="inline font-medium text-slate-700">{suggestion.sampleCount.toLocaleString('zh-CN')}</dd>
              </div>
              <div className="min-w-0">
                <dt className="inline">时长 </dt>
                <dd className="inline font-medium text-slate-700">{formatMinutes(suggestion.totalDurationSeconds)}</dd>
              </div>
              {suggestion.currentCategory && (
                <div className="min-w-0">
                  <dt className="inline">当前 </dt>
                  <dd className="inline font-medium text-slate-700">{suggestion.currentCategory}</dd>
                </div>
              )}
            </dl>
          </div>

          <div className="grid shrink-0 grid-cols-2 gap-2 md:w-[148px]">
            <button
              type="button"
              onClick={() => onCorrect(suggestion)}
              className="pim-button-primary h-9 px-3 text-sm font-medium"
            >
              纠正
            </button>
            <button
              type="button"
              onClick={() => onReject(suggestion)}
              className="pim-button-secondary h-9 px-3 text-sm font-medium"
            >
              忽略
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

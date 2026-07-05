import type { ActivityClassificationSuggestion } from '../../types';

interface Props {
  suggestions: ActivityClassificationSuggestion[];
  isLoading: boolean;
  onPreview: (suggestion: ActivityClassificationSuggestion) => void;
  onReject: (suggestion: ActivityClassificationSuggestion) => void;
  onLater?: (suggestion: ActivityClassificationSuggestion) => void;
}

function formatMinutes(seconds: number) {
  const minutes = Math.round((seconds / 60) * 10) / 10;
  return `${minutes.toLocaleString('zh-CN', { maximumFractionDigits: 1 })} minutes`;
}

function displayName(suggestion: ActivityClassificationSuggestion) {
  return suggestion.appDisplayName || suggestion.clusterKey || 'Unclassified activity';
}

function suggestionBadge(suggestion: ActivityClassificationSuggestion) {
  if (suggestion.recognitionSource === 'builtin' || suggestion.recognitionSource === 'manual') {
    return 'Recognized';
  }

  if (suggestion.suggestedCategory) {
    return 'Suggested';
  }

  return 'Needs review';
}

export default function ClassificationActionQueue({
  suggestions,
  isLoading,
  onPreview,
  onReject,
  onLater,
}: Props) {
  if (isLoading) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        Loading classification work...
      </div>
    );
  }

  const visibleSuggestions = suggestions.slice(0, 10);

  if (visibleSuggestions.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        No pending classification work.
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {visibleSuggestions.map(suggestion => (
        <article
          key={suggestion.id}
          className="rounded-lg border border-slate-200 bg-white px-3 py-3"
        >
          <div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div className="min-w-0">
              <div className="flex min-w-0 flex-wrap items-center gap-2">
                <h3 className="min-w-0 break-words text-sm font-semibold text-slate-950">
                  {displayName(suggestion)}
                </h3>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                  {suggestionBadge(suggestion)}
                </span>
              </div>

              <p className="mt-1 text-xs text-slate-600">
                {suggestion.sampleCount.toLocaleString('zh-CN')} samples |{' '}
                {formatMinutes(suggestion.totalDurationSeconds)}
                {suggestion.currentCategory ? ` | current ${suggestion.currentCategory}` : ''}
              </p>

              {(suggestion.suggestedCategory || suggestion.suggestedProjectTag) && (
                <p className="mt-1 text-xs text-cyan-700">
                  Proposed {suggestion.suggestedCategory || 'category unchanged'}
                  {suggestion.suggestedProjectTag ? ` | project ${suggestion.suggestedProjectTag}` : ''}
                </p>
              )}
            </div>

            <div className="flex shrink-0 flex-wrap gap-2 md:justify-end">
              <button
                type="button"
                onClick={() => onPreview(suggestion)}
                className="pim-button-primary min-h-8 px-3 py-1.5 text-xs font-medium"
              >
                Process and preview
              </button>
              {onLater && (
                <button
                  type="button"
                  onClick={() => onLater(suggestion)}
                  className="pim-button-secondary min-h-8 px-3 py-1.5 text-xs font-medium"
                >
                  Later
                </button>
              )}
              <button
                type="button"
                onClick={() => onReject(suggestion)}
                className="pim-button-secondary min-h-8 px-3 py-1.5 text-xs font-medium"
              >
                Ignore
              </button>
            </div>
          </div>
        </article>
      ))}

      {suggestions.length > visibleSuggestions.length && (
        <p className="px-1 text-xs text-slate-500">
          {suggestions.length - visibleSuggestions.length} more items are waiting.
        </p>
      )}
    </div>
  );
}

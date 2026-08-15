import StatusBadge from '../../ui/StatusBadge';
import LabelingQueue from '../labeling/LabelingQueue';
import type { ClassificationSuggestionsTodayData, TodaySection } from '../../types';

export default function TodayClassificationSuggestionsSection({
  section,
}: {
  section: TodaySection<ClassificationSuggestionsTodayData>;
}) {
  const { pendingCount } = section.data;

  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">分类建议</h2>
        <StatusBadge tone={pendingCount > 0 ? 'warning' : 'neutral'}>{pendingCount} 待处理</StatusBadge>
      </div>

      <div className="space-y-3">
        <LabelingQueue limit={5} compact />
      </div>
    </section>
  );
}

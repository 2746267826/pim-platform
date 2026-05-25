import { useQuery } from '@tanstack/react-query';
import { getTodaySection } from '../../api/today';
import EmptyState from '../../ui/EmptyState';
import type {
  CalendarScheduleTodayData,
  CalendarTasksTodayData,
  ClassificationSuggestionsTodayData,
  OperationsHealthTodayData,
  PcActivityTodayData,
  PcQualityTodayData,
  TodaySection,
  TodaySectionKind,
  TodaySectionRegistryItem,
  TaskResponse,
} from '../../types';
import TodayClassificationSuggestionsSection from './TodayClassificationSuggestionsSection';
import TodayHealthSection from './TodayHealthSection';
import TodayPcOverview from './TodayPcOverview';
import TodayPcQualitySection from './TodayPcQualitySection';
import TodayScheduleList from './TodayScheduleList';
import TodayTaskColumn from './TodayTaskColumn';

export const todaySectionOrder: TodaySectionKind[] = [
  'calendar.schedule',
  'pc.activity',
  'calendar.tasks',
  'operations.health',
  'pc.quality',
  'pc.classification_suggestions',
];

const todaySectionTitles: Record<TodaySectionKind, string> = {
  'calendar.schedule': '今日安排',
  'calendar.tasks': '任务关注',
  'pc.activity': 'PC 记录概览',
  'pc.quality': 'PC 数据质量',
  'operations.health': '系统健康',
  'pc.classification_suggestions': '分类建议',
};

export function getTodaySectionTitle(kind: TodaySectionKind | string) {
  return isKnownTodaySectionKind(kind) ? todaySectionTitles[kind] : kind;
}

export function isKnownTodaySectionKind(kind: TodaySectionKind | string): kind is TodaySectionKind {
  return todaySectionOrder.includes(kind as TodaySectionKind);
}

function SectionLoading({ title }: { title: string }) {
  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">{title}</h2>
      </div>
      <EmptyState title="加载中" description="正在加载这个区块的数据。" />
    </section>
  );
}

function SectionUnavailable({ title, message }: { title: string; message?: string }) {
  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">{title}</h2>
      </div>
      <EmptyState title="暂不可用" description={message || '这个区块暂时无法提供数据。'} />
    </section>
  );
}

export default function TodaySectionHost({
  item,
  date,
  todayPrefix,
  onSelectScheduled,
  onSelectTask,
}: {
  item: TodaySectionRegistryItem;
  date: string;
  todayPrefix: string;
  onSelectScheduled?: (item: { type: 'event' | 'task'; id: string }) => void;
  onSelectTask?: (task: TaskResponse) => void;
}) {
  const known = isKnownTodaySectionKind(item.kind);
  const title = getTodaySectionTitle(item.kind);
  const query = useQuery({
    queryKey: ['today-section', item.id, date],
    queryFn: () => getTodaySection(item.id, date),
    enabled: known,
    refetchInterval: item.kind.startsWith('pc.') || item.kind.startsWith('operations.') ? 30000 : false,
  });

  if (!known) {
    return <EmptyState title="未知区块" description={`${item.kind} 暂未在 Web 端注册。`} />;
  }

  if (query.isLoading) {
    return <SectionLoading title={title} />;
  }

  if (query.error) {
    return <SectionUnavailable title={title} message={query.error.message || '请稍后重试。'} />;
  }

  const data = query.data;
  if (!data?.data) {
    return <SectionUnavailable title={title} message="服务端没有返回这个区块的数据。" />;
  }

  if (data.status === 'unavailable') {
    return <SectionUnavailable title={title} message={data.error?.message} />;
  }

  switch (data.kind) {
    case 'calendar.schedule':
      return (
        <TodayScheduleList
          section={data as TodaySection<CalendarScheduleTodayData>}
          onSelect={item => {
            if (item.type === 'task') {
              onSelectTask?.(item.task);
            }
            onSelectScheduled?.(item);
          }}
        />
      );
    case 'calendar.tasks':
      return (
        <TodayTaskColumn
          section={data as TodaySection<CalendarTasksTodayData>}
          todayPrefix={todayPrefix}
          onSelect={onSelectTask}
        />
      );
    case 'pc.activity':
      return <TodayPcOverview section={data as TodaySection<PcActivityTodayData>} />;
    case 'pc.quality':
      return <TodayPcQualitySection section={data as TodaySection<PcQualityTodayData>} />;
    case 'operations.health':
      return <TodayHealthSection section={data as TodaySection<OperationsHealthTodayData>} />;
    case 'pc.classification_suggestions':
      return (
        <TodayClassificationSuggestionsSection
          section={data as TodaySection<ClassificationSuggestionsTodayData>}
        />
      );
    default:
      return <EmptyState title="未知区块" description={`${data.kind} 暂未在 Web 端注册。`} />;
  }
}

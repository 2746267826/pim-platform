import type {
  MobileSessionEvent,
  MobileTimelineBlock,
  MobileTimelineBlockSession,
} from '../../api/mobile';
import { formatDuration, formatShortTime, sourceLabel } from './mobileFormatting';

export interface MobileTimelineBlocksProps {
  blocks: MobileTimelineBlock[];
  sessionsByBlock: Record<string, MobileTimelineBlockSession[]>;
  eventsBySession: Record<string, MobileSessionEvent[]>;
  expandedBlockId?: string | null;
  expandedSessionId?: string | null;
  hasMore: boolean;
  isLoading?: boolean;
  isLoadingMore?: boolean;
  isLoadingSessions?: boolean;
  isLoadingEvents?: boolean;
  onToggleBlock: (blockId: string) => void;
  onToggleSession: (sessionId: string) => void;
  onLoadMore: () => void;
}

export default function MobileTimelineBlocks({
  blocks,
  sessionsByBlock,
  eventsBySession,
  expandedBlockId = null,
  expandedSessionId = null,
  hasMore,
  isLoading = false,
  isLoadingMore = false,
  isLoadingSessions = false,
  isLoadingEvents = false,
  onToggleBlock,
  onToggleSession,
  onLoadMore,
}: MobileTimelineBlocksProps) {
  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-slate-950">时间块</h2>
        <span className="text-xs text-slate-500">块 - 会话 - 原始事件</span>
      </div>
      <div className="mt-4 space-y-3">
        {blocks.map(block => {
          const expanded = expandedBlockId === block.id;
          const sessions = sessionsByBlock[block.id] ?? [];
          return (
            <div key={block.id} className="rounded-md border border-slate-200">
              <button
                type="button"
                onClick={() => onToggleBlock(block.id)}
                className="grid w-full grid-cols-[minmax(120px,0.35fr)_1fr_auto] items-center gap-3 px-3 py-3 text-left"
              >
                <span className="text-sm font-medium text-slate-900">{formatShortTime(block.startUtc)} - {formatShortTime(block.endUtc)}</span>
                <span className="min-w-0">
                  <span className="block truncate text-sm text-slate-700">{block.lifeCategory}</span>
                  <span className="block truncate text-xs text-slate-500">{block.topApps.map(app => app.displayName).join(' / ')}</span>
                </span>
                <span className="text-sm font-semibold text-slate-900">{formatDuration(block.foregroundSeconds)}</span>
              </button>
              {expanded && (
                <div className="border-t border-slate-100 px-3 py-3">
                  {sessions.length === 0 && (
                    <p className="text-xs text-slate-500">
                      {isLoadingSessions ? '正在加载会话' : '暂无会话'}
                    </p>
                  )}
                  <div className="space-y-2">
                    {sessions.map(session => {
                      const sessionExpanded = expandedSessionId === session.id;
                      return (
                        <div key={session.id} className="rounded border border-slate-100 bg-slate-50">
                          <button
                            type="button"
                            onClick={() => onToggleSession(session.id)}
                            className="grid w-full grid-cols-[1fr_auto] gap-3 px-3 py-2 text-left text-sm"
                          >
                            <span className="min-w-0">
                              <span className="block truncate font-medium text-slate-800">{session.displayName}</span>
                              <span className="block truncate text-xs text-slate-500">{session.packageName} · {sourceLabel(session.source)}</span>
                            </span>
                            <span className="text-slate-600">{formatDuration(session.durationSeconds)}</span>
                          </button>
                          {sessionExpanded && (
                            <div className="border-t border-slate-200 px-3 py-2">
                              <p className="mb-2 text-xs font-medium text-slate-500">原始事件</p>
                              {isLoadingEvents && <p className="text-xs text-slate-500">正在加载原始事件</p>}
                              {(eventsBySession[session.id] ?? []).map(event => (
                                <div key={event.id} className="grid grid-cols-[120px_1fr] gap-2 py-1 text-xs text-slate-600">
                                  <span>{formatShortTime(event.eventTimeUtc)}</span>
                                  <span className="truncate">{event.eventType} · {event.className ?? event.packageName}</span>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
      {isLoading && <p className="mt-3 text-xs text-slate-500">正在加载时间块</p>}
      {!isLoading && blocks.length === 0 && <p className="mt-3 text-xs text-slate-500">暂无时间块数据</p>}
      {hasMore && (
        <button
          type="button"
          onClick={onLoadMore}
          disabled={isLoadingMore}
          className="mt-4 h-9 rounded-md border border-slate-200 px-3 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {isLoadingMore ? '加载中' : '加载更多'}
        </button>
      )}
    </section>
  );
}

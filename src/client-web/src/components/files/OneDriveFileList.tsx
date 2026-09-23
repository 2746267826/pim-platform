import { AlertCircle, ArrowDown, ArrowUp, ChevronLeft, ChevronRight, FileText, Folder, LayoutGrid, List, Loader2, RefreshCw, Search } from 'lucide-react';
import type { FileItem, FileSearchScope, FileSortKey, FileSortOrder } from '../../types';
import { breadcrumbSegments, displayParentPath, pageCountLabel, parentDirPath } from './fileBrowserState';

export interface OneDriveFileListProps {
  items: FileItem[];
  /** 加载中（AC-10.1/10.2：必须有可见反馈，不得白屏）。 */
  loading?: boolean;
  /** 可读的失败原因（AC-10.3：不得出现「未知错误」，必须能重试）。 */
  error?: string | null;
  onRetry?: () => void;

  query: string;
  onQueryChange: (value: string) => void;
  searchScope: FileSearchScope;
  onSearchScopeChange: (scope: FileSearchScope) => void;
  /** 全局搜索结果需要展示完整路径（AC-8.1）。 */
  showFullPath?: boolean;
  /** 全局搜索命中某条时跳转到它所在目录。 */
  onRevealInFolder?: (path: string) => void;

  /** 当前目录路径（面包屑/上一级/当前文件夹搜索的范围）。 */
  currentPath: string;
  /** 面包屑或「上一级」导航。 */
  onNavigate: (path: string) => void;

  sort: FileSortKey;
  order: FileSortOrder;
  onSortChange: (sort: FileSortKey, order: FileSortOrder) => void;

  page: number;
  totalPages: number;
  totalCount: number;
  onPageChange: (page: number) => void;

  view: 'list' | 'grid';
  onViewChange: (view: 'list' | 'grid') => void;

  selectedItem: FileItem | null;
  onSelect: (item: FileItem) => void;
  onOpenFolder: (path: string) => void;
}

const SORT_LABELS: Record<FileSortKey, string> = {
  name: '名称',
  modified: '修改时间',
  size: '大小',
};

function formatSize(size: number | null): string {
  if (size === null || size === undefined) return '—';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`;
}

function formatTime(value: string): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export function typeBadge(item: FileItem): { label: string; className: string } {
  const mime = item.mimeType ?? '';
  const name = item.name.toLowerCase();
  if (item.itemType === 'folder') return { label: 'DIR', className: 'bg-[var(--pim-surface-muted)] text-[var(--pim-text-muted)]' };
  if (mime.includes('pdf') || name.endsWith('.pdf')) return { label: 'PDF', className: 'bg-red-50 text-red-600' };
  if (mime.includes('word') || name.endsWith('.docx') || name.endsWith('.doc')) return { label: 'DOC', className: 'bg-blue-50 text-blue-600' };
  if (mime.includes('sheet') || name.endsWith('.xlsx') || name.endsWith('.xls')) return { label: 'XLS', className: 'bg-green-50 text-green-600' };
  if (mime.includes('presentation') || name.endsWith('.pptx') || name.endsWith('.ppt')) return { label: 'PPT', className: 'bg-orange-50 text-orange-600' };
  if (mime.startsWith('image/')) return { label: 'IMG', className: 'bg-purple-50 text-purple-600' };
  return { label: 'TXT', className: 'bg-zinc-100 text-zinc-600' };
}

/**
 * 中栏：工具条（搜索/排序/视图）+ 面包屑与「上一级」+ 列表/网格 + 分页。
 *
 * 列表是主导航（决策 D-27）：列表负责进入目录、选中预览；树只负责目录层级。
 * 计数、翻页、搜索与排序全部由服务端驱动（REQ-3/REQ-8/REQ-9），
 * 因此这里展示的「共 N 项」是真实总数，不会像旧实现那样把「已加载 2000 项」当成全部（现状-2）。
 */
export default function OneDriveFileList({
  items,
  loading = false,
  error = null,
  onRetry,
  query,
  onQueryChange,
  searchScope,
  onSearchScopeChange,
  showFullPath = false,
  onRevealInFolder,
  currentPath,
  onNavigate,
  sort,
  order,
  onSortChange,
  page,
  totalPages,
  totalCount,
  onPageChange,
  view,
  onViewChange,
  selectedItem,
  onSelect,
  onOpenFolder,
}: OneDriveFileListProps) {
  const keyword = query.trim();
  const parent = parentDirPath(currentPath);
  const segments = breadcrumbSegments(currentPath);
  const canPage = totalPages > 1;

  const toggleOrder = () => onSortChange(sort, order === 'asc' ? 'desc' : 'asc');

  return (
    <section className="flex min-w-0 flex-1 flex-col" aria-label="文件列表">
      {/* 导航行：面包屑（逐级可点）+ 上一级 */}
      <div className="flex flex-wrap items-center gap-1 border-b border-[var(--pim-border)] px-3 py-2 text-xs">
        <button
          type="button"
          className="pim-button-secondary mr-1 inline-flex items-center gap-1 px-2 py-1 text-xs disabled:opacity-40"
          aria-label="返回上一级"
          disabled={parent === null}
          onClick={() => parent !== null && onNavigate(parent)}
        >
          <ChevronLeft size={13} /> 上一级
        </button>
        <nav className="flex min-w-0 flex-wrap items-center gap-1 text-[var(--pim-text-muted)]" aria-label="目录路径">
          {segments.map((segment, index) => (
            <span key={segment.path} className="flex items-center gap-1">
              {index > 0 && <span aria-hidden="true">/</span>}
              <button
                type="button"
                className={`truncate rounded px-1 py-0.5 hover:bg-[var(--pim-surface-muted)] ${
                  index === segments.length - 1 ? 'text-[var(--pim-text)]' : 'text-[var(--pim-primary)]'
                }`}
                onClick={() => onNavigate(segment.path)}
              >
                {segment.name}
              </button>
            </span>
          ))}
        </nav>
      </div>

      {/* 工具条：搜索 + 排序 + 视图 */}
      <div className="flex flex-wrap items-center gap-2 border-b border-[var(--pim-border)] px-3 py-2">
        <div className="flex items-center gap-2 rounded-full border border-[var(--pim-border)] px-3 py-1.5 text-sm text-[var(--pim-text-muted)]">
          <Search size={14} />
          <input
            className="w-40 bg-transparent outline-none placeholder:text-[var(--pim-text-muted)] sm:w-52"
            placeholder={searchScope === 'global' ? '全盘搜索文件名…' : '搜索当前文件夹…'}
            aria-label="搜索文件名"
            value={query}
            onChange={e => onQueryChange(e.target.value)}
          />
        </div>

        <div className="flex items-center gap-0.5 rounded-lg border border-[var(--pim-border)] p-0.5 text-xs">
          {(['folder', 'global'] as const).map(scope => (
            <button
              key={scope}
              type="button"
              className={`rounded-md px-2 py-1 ${searchScope === scope ? 'bg-[var(--pim-primary-soft)] text-[var(--pim-primary)]' : ''}`}
              aria-pressed={searchScope === scope}
              onClick={() => onSearchScopeChange(scope)}
            >
              {scope === 'folder' ? '当前文件夹' : '全盘'}
            </button>
          ))}
        </div>

        <label className="flex items-center gap-1 text-xs text-[var(--pim-text-muted)]">
          <span className="sr-only">排序字段</span>
          <select
            aria-label="排序字段"
            className="rounded-lg border border-[var(--pim-border)] bg-white px-2 py-1 text-xs"
            value={sort}
            onChange={e => onSortChange(e.target.value as FileSortKey, order)}
          >
            {(Object.keys(SORT_LABELS) as FileSortKey[]).map(key => (
              <option key={key} value={key}>
                {SORT_LABELS[key]}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          aria-label="切换排序方向"
          title={order === 'asc' ? '升序' : '降序'}
          className="rounded-lg border border-[var(--pim-border)] p-1.5 text-[var(--pim-text-muted)]"
          onClick={toggleOrder}
        >
          {order === 'asc' ? <ArrowUp size={13} /> : <ArrowDown size={13} />}
        </button>

        <span
          className="rounded-full border border-[var(--pim-border)] px-2.5 py-1 text-xs text-[var(--pim-text-muted)]"
          data-testid="file-page-count"
        >
          {pageCountLabel(page, totalPages, totalCount)}
        </span>

        <div className="ml-auto flex items-center gap-1 rounded-lg border border-[var(--pim-border)] p-0.5">
          <button
            type="button"
            aria-label="列表视图"
            className={`rounded-md px-2 py-1 ${view === 'list' ? 'bg-[var(--pim-surface-muted)]' : ''}`}
            onClick={() => onViewChange('list')}
          >
            <List size={14} />
          </button>
          <button
            type="button"
            aria-label="网格视图"
            className={`rounded-md px-2 py-1 ${view === 'grid' ? 'bg-[var(--pim-surface-muted)]' : ''}`}
            onClick={() => onViewChange('grid')}
          >
            <LayoutGrid size={14} />
          </button>
        </div>
      </div>

      {/* 失败：就地错误条 + 重试（AC-10.3） */}
      {error && (
        <div
          className="flex items-center gap-2 border-b border-[var(--pim-border)] bg-[var(--pim-danger-soft)] px-3 py-2 text-xs text-[var(--pim-danger)]"
          role="alert"
          data-testid="file-list-error"
        >
          <AlertCircle size={14} />
          <span className="min-w-0 flex-1">{error}</span>
          <button
            type="button"
            className="pim-button-secondary inline-flex items-center gap-1 px-2 py-1 text-xs"
            onClick={() => onRetry?.()}
          >
            <RefreshCw size={12} /> 重试
          </button>
        </div>
      )}

      {loading && (
        <div
          className="flex items-center gap-2 border-b border-[var(--pim-border)] bg-[var(--pim-surface-muted)] px-3 py-2 text-xs text-[var(--pim-text-muted)]"
          data-testid="file-list-loading"
        >
          <Loader2 size={13} className="animate-spin" /> 加载中…
        </div>
      )}

      {view === 'list' ? (
        <div className="min-h-0 flex-1 overflow-auto px-2 pb-4">
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-[var(--pim-text-muted)]">
                <th className="px-3 py-2 font-medium">名称</th>
                {showFullPath && <th className="px-3 py-2 font-medium">所在目录</th>}
                <th className="px-3 py-2 font-medium">大小</th>
                <th className="px-3 py-2 font-medium">修改时间</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => {
                const badge = typeBadge(item);
                return (
                  <tr
                    key={item.id}
                    data-testid="file-row"
                    className={`cursor-pointer border-b border-[var(--pim-border-soft)] ${
                      selectedItem?.id === item.id ? 'bg-[var(--pim-primary-soft)]' : 'hover:bg-[var(--pim-surface-muted)]'
                    }`}
                    onClick={() => (item.itemType === 'folder' ? onOpenFolder(item.path) : onSelect(item))}
                  >
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-2.5">
                        <span className={`flex h-7 w-7 items-center justify-center rounded-lg text-[10px] font-bold ${badge.className}`}>
                          {badge.label}
                        </span>
                        <span className="flex items-center gap-1.5 truncate">
                          {item.itemType === 'folder' && <Folder size={14} className="shrink-0 text-[var(--pim-text-muted)]" />}
                          {item.name}
                        </span>
                      </div>
                    </td>
                    {showFullPath && (
                      <td className="px-3 py-2 text-xs text-[var(--pim-text-muted)]">
                        <button
                          type="button"
                          className="truncate text-left text-[var(--pim-primary)] hover:underline"
                          title={displayParentPath(item.path)}
                          onClick={event => {
                            event.stopPropagation();
                            if (onRevealInFolder) onRevealInFolder(displayParentPath(item.path));
                            else onNavigate(displayParentPath(item.path));
                          }}
                        >
                          {displayParentPath(item.path)}
                        </button>
                      </td>
                    )}
                    <td className="px-3 py-2 text-xs text-[var(--pim-text-muted)]">{formatSize(item.size)}</td>
                    <td className="px-3 py-2 text-xs text-[var(--pim-text-muted)]">{formatTime(item.modifiedAt)}</td>
                  </tr>
                );
              })}
              {items.length === 0 && !loading && (
                <tr>
                  <td colSpan={showFullPath ? 4 : 3} className="px-3 py-10 text-center text-sm text-[var(--pim-text-muted)]">
                    {keyword && searchScope === 'folder' ? (
                      <span className="inline-flex flex-col items-center gap-2">
                        <span>本文件夹无匹配，试试全盘搜索</span>
                        <button
                          type="button"
                          className="pim-button-secondary px-3 py-1 text-xs"
                          onClick={() => onSearchScopeChange('global')}
                        >
                          去全盘搜索
                        </button>
                      </span>
                    ) : keyword ? (
                      '没有匹配的文件'
                    ) : (
                      '此文件夹为空'
                    )}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="grid min-h-0 flex-1 grid-cols-[repeat(auto-fill,minmax(150px,1fr))] content-start gap-3 overflow-auto p-4">
          {items.map(item => {
            const badge = typeBadge(item);
            return (
              <button
                key={item.id}
                type="button"
                data-testid="file-card"
                className={`overflow-hidden rounded-xl border text-left transition ${
                  selectedItem?.id === item.id
                    ? 'border-[var(--pim-primary)] bg-[var(--pim-primary-soft)]'
                    : 'border-[var(--pim-border)] bg-white hover:-translate-y-px hover:shadow-sm'
                }`}
                onClick={() => (item.itemType === 'folder' ? onOpenFolder(item.path) : onSelect(item))}
              >
                <div className="flex h-20 items-center justify-center bg-[var(--pim-surface-muted)] text-2xl">
                  {item.itemType === 'folder' ? (
                    <Folder size={26} className="text-[var(--pim-text-muted)]" />
                  ) : (
                    <FileText size={26} className="text-[var(--pim-text-muted)]" />
                  )}
                </div>
                <div className="truncate px-2.5 py-2 text-xs text-[var(--pim-text-muted)]">{item.name}</div>
                {showFullPath && (
                  <div className="truncate px-2.5 text-[10px] text-[var(--pim-text-muted)]">{displayParentPath(item.path)}</div>
                )}
                <div className={`mx-2.5 mb-2.5 inline-flex rounded px-1.5 py-0.5 text-[10px] font-bold ${badge.className}`}>{badge.label}</div>
              </button>
            );
          })}
          {items.length === 0 && !loading && (
            <div className="col-span-full py-10 text-center text-sm text-[var(--pim-text-muted)]">
              {keyword ? '没有匹配的文件' : '此文件夹为空'}
            </div>
          )}
        </div>
      )}

      {canPage && (
        <div className="flex items-center justify-center gap-2 border-t border-[var(--pim-border)] px-3 py-2 text-xs">
          <button
            type="button"
            className="pim-button-secondary inline-flex items-center gap-1 px-2 py-1 disabled:opacity-40"
            aria-label="上一页"
            disabled={page <= 1}
            onClick={() => onPageChange(page - 1)}
          >
            <ChevronLeft size={13} /> 上一页
          </button>
          <span className="text-[var(--pim-text-muted)]" data-testid="file-page-indicator">
            {Math.min(Math.max(1, page), totalPages)} / {totalPages}
          </span>
          <button
            type="button"
            className="pim-button-secondary inline-flex items-center gap-1 px-2 py-1 disabled:opacity-40"
            aria-label="下一页"
            disabled={page >= totalPages}
            onClick={() => onPageChange(page + 1)}
          >
            下一页 <ChevronRight size={13} />
          </button>
        </div>
      )}
    </section>
  );
}

import { FileText, Folder, LayoutGrid, List, Search } from 'lucide-react';
import type { FileItem } from '../../types';

export interface OneDriveFileListProps {
  items: FileItem[];
  loading?: boolean;
  breadcrumb: string;
  searchQuery: string;
  onSearchChange: (value: string) => void;
  view: 'list' | 'grid';
  onViewChange: (view: 'list' | 'grid') => void;
  selectedItem: FileItem | null;
  onSelect: (item: FileItem) => void;
  onOpenFolder: (path: string) => void;
}

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

/** 方案 A 中栏：工具条（搜索/视图切换）+ 列表/网格。 */
export default function OneDriveFileList({
  items,
  loading = false,
  breadcrumb,
  searchQuery,
  onSearchChange,
  view,
  onViewChange,
  selectedItem,
  onSelect,
  onOpenFolder,
}: OneDriveFileListProps) {
  const keyword = searchQuery.trim().toLowerCase();
  const visible = keyword
    ? items.filter(item => item.name.toLowerCase().includes(keyword))
    : items;

  return (
    <section className="flex min-w-0 flex-1 flex-col" aria-label="文件列表">
      <div className="flex flex-wrap items-center gap-2 border-b border-[var(--pim-border)] px-4 py-3">
        <div className="flex items-center gap-2 rounded-full border border-[var(--pim-border)] px-3 py-1.5 text-sm text-[var(--pim-text-muted)]">
          <Search size={14} />
          <input
            className="w-44 bg-transparent outline-none placeholder:text-[var(--pim-text-muted)]"
            placeholder="搜索当前文件夹…"
            aria-label="搜索文件名"
            value={searchQuery}
            onChange={e => onSearchChange(e.target.value)}
          />
        </div>
        <span className="rounded-full border border-[var(--pim-border)] px-2.5 py-1 text-xs text-[var(--pim-text-muted)]">
          {visible.length} 项
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

      <div className="px-4 pt-2 text-xs text-[var(--pim-text-muted)]">{breadcrumb}</div>

      {view === 'list' ? (
        <div className="min-h-0 flex-1 overflow-auto px-2 pb-4">
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-[var(--pim-text-muted)]">
                <th className="px-3 py-2 font-medium">名称</th>
                <th className="px-3 py-2 font-medium">大小</th>
                <th className="px-3 py-2 font-medium">修改时间</th>
              </tr>
            </thead>
            <tbody>
              {visible.map(item => {
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
                    <td className="px-3 py-2 text-xs text-[var(--pim-text-muted)]">{formatSize(item.size)}</td>
                    <td className="px-3 py-2 text-xs text-[var(--pim-text-muted)]">{formatTime(item.modifiedAt)}</td>
                  </tr>
                );
              })}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={3} className="px-3 py-8 text-center text-sm text-[var(--pim-text-muted)]">
                    {loading ? '加载中…' : keyword ? '没有匹配的文件' : '此文件夹为空'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="grid min-h-0 flex-1 grid-cols-[repeat(auto-fill,minmax(150px,1fr))] content-start gap-3 overflow-auto p-4">
          {visible.map(item => {
            const badge = typeBadge(item);
            return (
              <button
                key={item.id}
                type="button"
                data-testid="file-card"
                className={`overflow-hidden rounded-xl border text-left transition ${
                  selectedItem?.id === item.id
                    ? 'border-[var(--pim-primary)] bg-[var(--pim-primary-soft)]'
                    : 'border-[var(--pim-border)] bg-white hover:-translate-y-px hover:border-[var(--pim-border)] hover:shadow-sm'
                }`}
                onClick={() => (item.itemType === 'folder' ? onOpenFolder(item.path) : onSelect(item))}
              >
                <div className="flex h-20 items-center justify-center bg-[var(--pim-surface-muted)] text-2xl">
                  {item.itemType === 'folder' ? <Folder size={26} className="text-[var(--pim-text-muted)]" /> : <FileText size={26} className="text-[var(--pim-text-muted)]" />}
                </div>
                <div className="truncate px-2.5 py-2 text-xs text-[var(--pim-text-muted)]">{item.name}</div>
                <div className={`mx-2.5 mb-2.5 inline-flex rounded px-1.5 py-0.5 text-[10px] font-bold ${badge.className}`}>{badge.label}</div>
              </button>
            );
          })}
          {visible.length === 0 && (
            <div className="col-span-full py-8 text-center text-sm text-[var(--pim-text-muted)]">
              {loading ? '加载中…' : keyword ? '没有匹配的文件' : '此文件夹为空'}
            </div>
          )}
        </div>
      )}
    </section>
  );
}

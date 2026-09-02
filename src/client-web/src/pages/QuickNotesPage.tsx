import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useShellShare } from '../hooks/useShellShare';
import {
  Calendar,
  CheckSquare,
  File,
  Paperclip,
  Pencil,
  Plus,
  Upload,
  X,
} from 'lucide-react';

import {
  archiveQuickNote,
  createQuickNote,
  deleteQuickNote,
  getQuickNote,
  getQuickNotes,
  processQuickNote,
  restoreQuickNote,
  updateQuickNote,
  uploadQuickNoteAttachment,
} from '../api/quickNotes';
import QuickNoteEditor from '../components/quick-notes/QuickNoteEditor';
import type { QuickNoteAttachment, QuickNoteListItem, QuickNoteStatus } from '../types';
import EmptyState from '../ui/EmptyState';
import MobilePageHeader from '../ui/MobilePageHeader';
import PageHeader from '../ui/PageHeader';

const statusFilters: Array<{ key: QuickNoteStatus | 'all'; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'inbox', label: '收集箱' },
  { key: 'processed', label: '已处理' },
  { key: 'archived', label: '已归档' },
];

const statusLabels: Record<QuickNoteStatus, string> = {
  inbox: '收集箱',
  processed: '已处理',
  archived: '已归档',
};

function formatDateTime(value: string | null | undefined) {
  if (!value) return '未知时间';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function noteTitle(note: Pick<QuickNoteListItem, 'contentPreview'>) {
  const preview = note.contentPreview?.trim();
  return preview || '空白记录';
}

function StatusBadge({ status }: { status: QuickNoteStatus }) {
  const tone =
    status === 'inbox'
      ? 'border-blue-200 bg-blue-50 text-blue-700'
      : status === 'processed'
        ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
        : 'border-slate-200 bg-slate-100 text-slate-600';

  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium ${tone}`}>
      {statusLabels[status]}
    </span>
  );
}

// ─── NoteDialog ───────────────────────────────────────────────────────────────

interface NoteDialogProps {
  open: boolean;
  mode: 'create' | 'edit';
  noteId: string | null;
  onClose: () => void;
  onSaved: () => void;
}

function NoteDialog({ open, mode, noteId, onClose, onSaved }: NoteDialogProps) {
  const queryClient = useQueryClient();

  const [content, setContent] = useState('');
  const [attachmentIds, setAttachmentIds] = useState<string[]>([]);
  const [localAttachments, setLocalAttachments] = useState<QuickNoteAttachment[]>([]);
  const [isArchived, setIsArchived] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);

  // Fetch detail for edit mode
  const detailQuery = useQuery({
    queryKey: ['quick-notes', 'detail', noteId],
    queryFn: () => getQuickNote(noteId as string),
    enabled: mode === 'edit' && Boolean(noteId),
  });

  const selected = detailQuery.data;

  // Populate state when detail loads
  useEffect(() => {
    if (mode === 'edit' && selected) {
      setContent(selected.contentMarkdown);
      setAttachmentIds(selected.attachments.map(a => a.id));
      setLocalAttachments(selected.attachments);
      setIsArchived(selected.status === 'archived');
    }
  }, [mode, selected]);

  // Reset state when opening
  useEffect(() => {
    if (open && mode === 'create') {
      setContent('');
      setAttachmentIds([]);
      setLocalAttachments([]);
      setIsArchived(false);
      setEditError(null);
    }
  }, [open, mode]);

  const createMutation = useMutation({
    mutationFn: (markdown: string) =>
      createQuickNote({
        contentMarkdown: markdown,
        source: 'web-page',
        attachmentIds: attachmentIds.length > 0 ? attachmentIds : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      onSaved();
      onClose();
    },
    onError: () => {
      setEditError('创建失败，请稍后重试。');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      id,
      markdown,
      attIds,
    }: {
      id: string;
      markdown: string;
      attIds: string[];
    }) => updateQuickNote(id, { contentMarkdown: markdown, attachmentIds: attIds }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      queryClient.invalidateQueries({ queryKey: ['quick-notes', 'detail', noteId] });
      onSaved();
      onClose();
    },
    onError: () => {
      setEditError('保存失败，请稍后重试。');
    },
  });

  const archiveMutation = useMutation({
    mutationFn: archiveQuickNote,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      queryClient.invalidateQueries({ queryKey: ['quick-notes', 'detail', noteId] });
      setIsArchived(true);
    },
    onError: () => {
      setEditError('归档失败，请稍后重试。');
    },
  });

  const restoreMutation = useMutation({
    mutationFn: (id: string) => restoreQuickNote(id, 'inbox'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      queryClient.invalidateQueries({ queryKey: ['quick-notes', 'detail', noteId] });
      setIsArchived(false);
    },
    onError: () => {
      setEditError('恢复失败，请稍后重试。');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteQuickNote,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      onSaved();
      onClose();
    },
    onError: () => {
      setEditError('删除失败，请稍后重试。');
    },
  });

  const busy =
    createMutation.isPending ||
    updateMutation.isPending ||
    archiveMutation.isPending ||
    restoreMutation.isPending ||
    deleteMutation.isPending;

  function handleSave() {
    const trimmed = content.trim();
    if (!trimmed) return;

    if (mode === 'create') {
      createMutation.mutate(content);
    } else if (noteId) {
      updateMutation.mutate({ id: noteId, markdown: content, attIds: attachmentIds });
    }
  }

  function handleArchiveToggle() {
    if (!noteId) return;
    if (isArchived) {
      restoreMutation.mutate(noteId);
    } else {
      archiveMutation.mutate(noteId);
    }
  }

  function handleDelete() {
    if (!noteId) return;
    const confirmed = window.confirm('确定删除这条快速记录？此操作无法撤销。');
    if (confirmed) {
      deleteMutation.mutate(noteId);
    }
  }

  async function handleFileUpload(file: File) {
    if (!noteId && mode === 'edit') return;
    if (mode === 'create') {
      // Upload then associate on save
      try {
        const result = await uploadQuickNoteAttachment(file);
        setAttachmentIds(prev => [...prev, result.id]);
        setLocalAttachments(prev => [
          ...prev,
          {
            id: result.id,
            fileName: result.fileName,
            contentType: result.contentType,
            sizeBytes: result.sizeBytes,
            downloadUrl: result.downloadUrl,
            previewUrl: result.previewUrl,
            createdAt: new Date().toISOString(),
          },
        ]);
      } catch {
        setEditError('附件上传失败');
      }
      return;
    }

    // Edit mode: upload then update note immediately
    try {
      const result = await uploadQuickNoteAttachment(file);
      const newIds = [...attachmentIds, result.id];
      await updateMutation.mutateAsync({ id: noteId!, markdown: content, attIds: newIds });
      setAttachmentIds(newIds);
      setLocalAttachments(prev => [
        ...prev,
        {
          id: result.id,
          fileName: result.fileName,
          contentType: result.contentType,
          sizeBytes: result.sizeBytes,
          downloadUrl: result.downloadUrl,
          previewUrl: result.previewUrl,
          createdAt: new Date().toISOString(),
        },
      ]);
      queryClient.invalidateQueries({ queryKey: ['quick-notes', 'detail', noteId] });
    } catch {
      setEditError('附件上传失败');
    }
  }

  function handleFileInputChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) {
      void handleFileUpload(file);
    }
    event.target.value = '';
  }

  function handleDrop(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragOver(false);
    const file = event.dataTransfer.files?.[0];
    if (file) {
      void handleFileUpload(file);
    }
  }

  function handleDragOver(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragOver(true);
  }

  function handleDragLeave(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragOver(false);
  }

  function removeAttachment(attId: string) {
    const newIds = attachmentIds.filter(id => id !== attId);
    setAttachmentIds(newIds);
    setLocalAttachments(prev => prev.filter(a => a.id !== attId));

    if (mode === 'edit' && noteId) {
      updateMutation.mutate({ id: noteId, markdown: content, attIds: newIds });
    }
  }

  function formatFileSize(bytes: number) {
    if (bytes < 1024) return `${bytes}B`;
    return `${(bytes / 1024).toFixed(0)}KB`;
  }

  if (!open) return null;

  const isLoading = mode === 'edit' && detailQuery.isLoading;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/40 backdrop-blur-xs"
      onClick={onClose}
    >
      <div
        className="flex w-full max-w-2xl flex-col rounded-xl border border-zinc-200 bg-white shadow-dialog"
        onClick={e => e.stopPropagation()}
      >
        <header className="flex shrink-0 items-center justify-between border-b border-zinc-200 px-5 py-4">
          <h2 className="text-base font-semibold text-zinc-900">
            {mode === 'edit' ? '编辑记录' : '写闪念'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-600"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        {isLoading ? (
          <div className="flex items-center justify-center py-20 text-sm text-zinc-500">
            加载中...
          </div>
        ) : (
          <div className="max-h-[75vh] space-y-4 overflow-y-auto px-5 py-4">
            {editError && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {editError}
              </div>
            )}

            <QuickNoteEditor value={content} onChange={setContent} minHeight={200} />

            <div className="flex items-center gap-4">
              <label className="flex cursor-pointer items-center gap-2 text-sm text-zinc-700">
                <input
                  type="checkbox"
                  checked={isArchived}
                  onChange={handleArchiveToggle}
                  disabled={busy || mode === 'create'}
                  className="rounded border-zinc-300 text-zinc-900 focus:ring-zinc-500"
                />
                已归档
              </label>
              {mode === 'edit' && selected && selected.status !== 'processed' && selected.status !== 'archived' && (
                <button
                  type="button"
                  onClick={() => {
                    if (noteId) {
                      processQuickNote(noteId)
                        .then(() => {
                          queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
                          queryClient.invalidateQueries({ queryKey: ['quick-notes', 'detail', noteId] });
                        })
                        .catch(() => setEditError('处理失败'));
                    }
                  }}
                  disabled={busy}
                  className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-60"
                >
                  标记处理
                </button>
              )}
            </div>

            {/* Attachments */}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-zinc-700">附件</h3>

              {localAttachments.length > 0 && (
                <div className="mb-2 space-y-1">
                  {localAttachments.map(att => (
                    <div key={att.id} className="flex items-center gap-2 rounded-md border border-zinc-200 px-3 py-2 text-sm">
                      <File className="h-4 w-4 shrink-0 text-zinc-400" />
                      <span className="min-w-0 flex-1 truncate text-zinc-700">{att.fileName}</span>
                      <span className="shrink-0 text-xs text-zinc-400">{formatFileSize(att.sizeBytes)}</span>
                      <button
                        type="button"
                        onClick={() => removeAttachment(att.id)}
                        disabled={busy}
                        className="shrink-0 rounded p-0.5 text-zinc-400 hover:text-red-600 disabled:opacity-50"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <label className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-dashed border-zinc-300 px-3 py-2 text-sm text-zinc-500 hover:border-blue-300 hover:text-blue-600">
                <Upload className="h-4 w-4" />
                上传附件
                <input type="file" hidden onChange={handleFileInputChange} />
              </label>

              <div
                className={`mt-2 rounded-lg border-2 border-dashed p-4 text-center text-sm transition-colors ${
                  dragOver
                    ? 'border-blue-400 bg-blue-50 text-blue-600'
                    : 'border-zinc-200 text-zinc-400'
                }`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                拖拽文件到此处上传
              </div>
            </div>
          </div>
        )}

        <footer className="flex shrink-0 items-center justify-between border-t border-zinc-200 px-5 py-4">
          {mode === 'edit' && (
            <button
              type="button"
              onClick={handleDelete}
              disabled={busy}
              className="text-sm text-red-600 hover:text-red-800 disabled:opacity-50"
            >
              删除
            </button>
          )}
          {mode === 'create' && <div />}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-zinc-200 px-4 py-2 text-sm text-zinc-600 hover:bg-zinc-50"
            >
              取消
            </button>
            <button
              type="button"
              onClick={handleSave}
              disabled={!content.trim() || busy}
              className="rounded-lg bg-zinc-900 px-4 py-2 text-sm text-white hover:bg-zinc-800 disabled:opacity-50"
            >
              {busy ? '保存中...' : '保存'}
            </button>
          </div>
        </footer>
      </div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function QuickNotesPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<QuickNoteStatus | 'all'>('all');
  const [search, setSearch] = useState('');
  const [searchParams] = useSearchParams();
  const prefill = searchParams.get('prefill') ?? searchParams.get('text') ?? '';
  const isEmbed = searchParams.get('embed') === '1';
  const [error, setError] = useState<string | null>(null);
  const [deletedIds, setDeletedIds] = useState<Set<string>>(() => new Set());

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<'create' | 'edit'>('create');
  const [dialogNoteId, setDialogNoteId] = useState<string | null>(null);

  // FAB state
  const [showFabMenu, setShowFabMenu] = useState(false);
  const fabRef = useRef<HTMLDivElement>(null);

  const hasPrefilled = useRef(false);

  useEffect(() => {
    if (prefill && !hasPrefilled.current) {
      hasPrefilled.current = true;
      openCreateDialog();
    }
  }, [prefill]);

  useShellShare(useCallback(() => {
    openCreateDialog();
  }, []));

  const listParams = useMemo(() => ({
    status: statusFilter === 'all' ? undefined : statusFilter,
    search: search.trim() || undefined,
    page: 1,
    pageSize: 50,
  }), [search, statusFilter]);

  const listQuery = useQuery({
    queryKey: ['quick-notes', 'list', listParams],
    queryFn: () => getQuickNotes(listParams),
  });

  const notes = useMemo(
    () => (listQuery.data?.items ?? []).filter(note => !deletedIds.has(note.id)),
    [deletedIds, listQuery.data?.items],
  );

  useEffect(() => {
    if (listQuery.data) {
      const idsInList = new Set(listQuery.data.items.map(note => note.id));
      setDeletedIds(current => {
        const pendingIds = Array.from(current).filter(id => idsInList.has(id));
        if (pendingIds.length === current.size) return current;
        return new Set(pendingIds);
      });
    }
  }, [listQuery.data]);

  function invalidateQuickNotes() {
    void queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
  }

  function openCreateDialog() {
    setDialogMode('create');
    setDialogNoteId(null);
    setDialogOpen(true);
    setShowFabMenu(false);
    setError(null);
  }

  function openEditDialog(noteId: string) {
    setDialogMode('edit');
    setDialogNoteId(noteId);
    setDialogOpen(true);
    setShowFabMenu(false);
    setError(null);
  }

  function closeDialog() {
    setDialogOpen(false);
    setDialogNoteId(null);
  }

  // Close FAB menu on outside click
  useEffect(() => {
    if (!showFabMenu) return;
    function handleClick(e: MouseEvent) {
      if (fabRef.current && !fabRef.current.contains(e.target as Node)) {
        setShowFabMenu(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [showFabMenu]);

  return (
    <div className="mx-auto flex h-full max-w-[1440px] flex-col gap-4 overflow-auto pb-24 md:pb-4">
      {!isEmbed && (
        <MobilePageHeader title="快速记录" action={<span className="text-xs text-slate-500 md:hidden">收集</span>} />
      )}
      {!isEmbed && (
        <PageHeader
          title="快速记录"
          subtitle="收集、整理、处理和归档临时想法。"
          actions={
            <button
              type="button"
              onClick={() => void listQuery.refetch()}
              disabled={listQuery.isFetching}
              className="pim-button-secondary min-h-[44px] px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
            >
              刷新
            </button>
          }
        />
      )}

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {/* Filters & Search */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex flex-wrap gap-2">
          {statusFilters.map(item => (
            <button
              key={item.key}
              type="button"
              onClick={() => setStatusFilter(item.key)}
              className={`min-h-[36px] rounded-full border px-3 py-1 text-xs font-semibold transition-colors ${
                statusFilter === item.key
                  ? 'border-zinc-900 bg-zinc-900 text-white'
                  : 'border-zinc-200 bg-white text-zinc-600 hover:border-zinc-300 hover:bg-zinc-50'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
        <label className="block flex-1 min-w-[200px] max-w-xs">
          <span className="sr-only">搜索快速记录</span>
          <input
            type="search"
            value={search}
            onChange={event => setSearch(event.target.value)}
            placeholder="搜索内容..."
            className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none transition-colors placeholder:text-zinc-400 focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100"
          />
        </label>
      </div>

      {/* Masonry Card List */}
      {listQuery.isLoading ? (
        <div className="py-10 text-center text-sm text-zinc-500">加载中...</div>
      ) : notes.length === 0 ? (
        <EmptyState title="没有快速记录" description="调整筛选或新建一条记录。" />
      ) : (
        <div className="columns-1 gap-4 sm:columns-2 lg:columns-3 xl:columns-4">
          {notes.map(note => (
            <div
              key={note.id}
              className="mb-4 break-inside-avoid cursor-pointer rounded-xl border border-zinc-200 bg-white shadow-card transition-shadow hover:shadow-subtle"
              onClick={() => openEditDialog(note.id)}
            >
              <div className="p-4">
                <div className="mb-2 flex items-start justify-between gap-2">
                  <p className="line-clamp-3 min-w-0 text-sm leading-relaxed text-zinc-800">
                    {noteTitle(note)}
                  </p>
                  <StatusBadge status={note.status} />
                </div>

                <div className="text-xs text-zinc-400">
                  {formatDateTime(note.createdAt)}
                </div>

                {note.attachmentCount > 0 && (
                  <div className="mt-2 flex items-center gap-1.5 border-t border-zinc-100 pt-2 text-[10px] text-zinc-400">
                    <Paperclip className="h-3 w-3" />
                    <span>{note.attachmentCount} 个附件</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* FAB */}
      <div ref={fabRef} className="fixed bottom-6 right-6 z-40 flex flex-col items-end gap-2">
        {showFabMenu && (
          <div className="animate-dialog rounded-xl border border-zinc-200 bg-white p-1 shadow-dialog">
            <button
              type="button"
              onClick={openCreateDialog}
              className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-zinc-700 hover:bg-zinc-50"
            >
              <Pencil className="h-4 w-4" /> 写闪念
            </button>
            <button
              type="button"
              onClick={() => navigate('/tasks')}
              className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-zinc-700 hover:bg-zinc-50"
            >
              <CheckSquare className="h-4 w-4" /> 建任务
            </button>
            <button
              type="button"
              onClick={() => navigate('/calendar')}
              className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-zinc-700 hover:bg-zinc-50"
            >
              <Calendar className="h-4 w-4" /> 排日程
            </button>
          </div>
        )}
        <button
          type="button"
          onClick={() => setShowFabMenu(prev => !prev)}
          className="flex h-14 w-14 items-center justify-center rounded-full bg-zinc-900 text-white shadow-lg transition-transform hover:scale-105 hover:bg-zinc-800"
        >
          <Plus className={`h-6 w-6 transition-transform ${showFabMenu ? 'rotate-45' : ''}`} />
        </button>
      </div>

      {/* NoteDialog */}
      <NoteDialog
        open={dialogOpen}
        mode={dialogMode}
        noteId={dialogNoteId}
        onClose={closeDialog}
        onSaved={invalidateQuickNotes}
      />
    </div>
  );
}
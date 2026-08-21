import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useShellShare } from '../hooks/useShellShare';

import {
  archiveQuickNote,
  createQuickNote,
  deleteQuickNote,
  getQuickNote,
  getQuickNotes,
  processQuickNote,
  restoreQuickNote,
  updateQuickNote,
} from '../api/quickNotes';
import { buildQuickNoteUpdatePayload } from '../components/quick-notes/quickNoteAttachmentBlobUrls';
import QuickNoteEditor from '../components/quick-notes/QuickNoteEditor';
import QuickNoteMarkdownPreview from '../components/quick-notes/QuickNoteMarkdownPreview';
import type { QuickNoteDetail, QuickNoteListItem, QuickNoteStatus } from '../types';
import EmptyState from '../ui/EmptyState';
import MobilePageHeader from '../ui/MobilePageHeader';
import PageHeader from '../ui/PageHeader';

const statusFilters: Array<{ key: QuickNoteStatus; label: string }> = [
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

export default function QuickNotesPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<QuickNoteStatus>('inbox');
  const [search, setSearch] = useState('');
  const [draft, setDraft] = useState('');
  const [searchParams] = useSearchParams();
  const prefill = searchParams.get('prefill') ?? searchParams.get('text') ?? '';
  const isEmbed = searchParams.get('embed') === '1';
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editMarkdown, setEditMarkdown] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [deletedIds, setDeletedIds] = useState<Set<string>>(() => new Set());
  const selectedIdRef = useRef<string | null>(null);

  const hasPrefilled = useRef(false);

  useEffect(() => {
    if (prefill && !hasPrefilled.current) {
      hasPrefilled.current = true;
      setDraft(prefill);
    }
  }, [prefill]);

  useShellShare(useCallback((detail) => {
    const text = detail.text ?? detail.url ?? '';
    if (text) setDraft((prev) => (prev ? `${prev}\n\n${text}` : text));
  }, []));

  const listParams = useMemo(() => ({
    status,
    search: search.trim() || undefined,
    page: 1,
    pageSize: 50,
  }), [search, status]);

  const listQuery = useQuery({
    queryKey: ['quick-notes', 'list', listParams],
    queryFn: () => getQuickNotes(listParams),
  });

  const notes = useMemo(
    () => (listQuery.data?.items ?? []).filter(note => !deletedIds.has(note.id)),
    [deletedIds, listQuery.data?.items],
  );

  const detailQuery = useQuery({
    queryKey: ['quick-notes', 'detail', selectedId],
    queryFn: () => getQuickNote(selectedId as string),
    enabled: Boolean(selectedId),
  });

  const selected = detailQuery.data;

  const setSelection = useCallback((nextId: string | null) => {
    selectedIdRef.current = nextId;
    setSelectedId(nextId);
  }, []);

  const updateSelection = useCallback((updater: (current: string | null) => string | null) => {
    setSelectedId(current => {
      const next = updater(current);
      selectedIdRef.current = next;
      return next;
    });
  }, []);

  useEffect(() => {
    if (listQuery.data) {
      const idsInList = new Set(listQuery.data.items.map(note => note.id));
      setDeletedIds(current => {
        const pendingIds = Array.from(current).filter(id => idsInList.has(id));

        if (pendingIds.length === current.size) {
          return current;
        }

        return new Set(pendingIds);
      });
    }
  }, [listQuery.data]);

  useEffect(() => {
    if (selected) {
      setEditMarkdown(selected.contentMarkdown);
    }
  }, [selected]);

  useEffect(() => {
    if (!selectedId) {
      setEditMarkdown('');
    }
  }, [selectedId]);

  useEffect(() => {
    if (listQuery.isLoading) return;

    updateSelection(current => {
      if (notes.length === 0) {
        return null;
      }

      if (current && notes.some(note => note.id === current)) {
        return current;
      }

      return notes[0].id;
    });
  }, [listQuery.isLoading, notes, updateSelection]);

  function clearSelectionIfCurrent(targetId: string) {
    updateSelection(current => {
      if (current === targetId) {
        return null;
      }

      return current;
    });
  }

  function hideDeletedNote(id: string) {
    setDeletedIds(current => {
      if (current.has(id)) return current;

      const next = new Set(current);
      next.add(id);
      return next;
    });
  }

  function changeStatusIfCurrent(targetId: string, nextStatus: QuickNoteStatus) {
    if (selectedIdRef.current === targetId) {
      setStatus(nextStatus);
      setSelection(targetId);
    }
  }

  function selectCreatedNote(note: QuickNoteDetail) {
    setDraft('');
    setSelection(note.id);
    setStatus(note.status);
    setError(null);
    invalidateQuickNotes(note.id);
  }

  function selectNote(note: QuickNoteListItem) {
    if (deletedIds.has(note.id)) {
      setDeletedIds(current => {
        if (!current.has(note.id)) return current;

        const next = new Set(current);
        next.delete(note.id);
        return next;
      });
    }
    setSelection(note.id);
    setError(null);
  }

  function invalidateQuickNotes(id?: string | null) {
    void queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
    if (id) {
      void queryClient.invalidateQueries({ queryKey: ['quick-notes', 'detail', id] });
    }
  }

  const createMutation = useMutation({
    mutationFn: (contentMarkdown: string) => createQuickNote({ contentMarkdown, source: 'web-page' }),
    onSuccess: selectCreatedNote,
    onError: () => setError('创建失败，请稍后重试。'),
  });

  const updateMutation = useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string;
      payload: ReturnType<typeof buildQuickNoteUpdatePayload>;
    }) => updateQuickNote(id, payload),
    onSuccess: note => {
      setError(null);
      invalidateQuickNotes(note.id);
    },
    onError: () => setError('保存失败，请稍后重试。'),
  });

  const processMutation = useMutation({
    mutationFn: processQuickNote,
    onSuccess: note => {
      changeStatusIfCurrent(note.id, note.status);
      invalidateQuickNotes(note.id);
    },
    onError: () => setError('处理失败，请稍后重试。'),
  });

  const archiveMutation = useMutation({
    mutationFn: archiveQuickNote,
    onSuccess: note => {
      changeStatusIfCurrent(note.id, note.status);
      invalidateQuickNotes(note.id);
    },
    onError: () => setError('归档失败，请稍后重试。'),
  });

  const restoreMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: string; nextStatus: QuickNoteStatus }) => restoreQuickNote(id, nextStatus),
    onSuccess: note => {
      changeStatusIfCurrent(note.id, note.status);
      invalidateQuickNotes(note.id);
    },
    onError: () => setError('恢复失败，请稍后重试。'),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteQuickNote,
    onSuccess: (_deletedId, id) => {
      hideDeletedNote(id);
      clearSelectionIfCurrent(id);
      setError(null);
      invalidateQuickNotes(id);
    },
    onError: () => setError('删除失败，请稍后重试。'),
  });

  const busy =
    createMutation.isPending ||
    updateMutation.isPending ||
    processMutation.isPending ||
    archiveMutation.isPending ||
    restoreMutation.isPending ||
    deleteMutation.isPending;

  function handleCreate() {
    const trimmed = draft.trim();
    if (!trimmed || createMutation.isPending) return;
    createMutation.mutate(draft);
  }

  function handleSave() {
    if (!selected || updateMutation.isPending) return;
    const trimmed = editMarkdown.trim();
    if (!trimmed || editMarkdown === selected.contentMarkdown) return;
    updateMutation.mutate({
      id: selected.id,
      payload: buildQuickNoteUpdatePayload(editMarkdown),
    });
  }

  function handleDelete(note: QuickNoteDetail) {
    if (deleteMutation.isPending) return;
    const confirmed = window.confirm('确定删除这条快速记录？此操作无法撤销。');
    if (confirmed) {
      deleteMutation.mutate(note.id);
    }
  }

  return (
    <div className="mx-auto flex h-full max-w-[1440px] flex-col gap-4 overflow-auto pb-20 md:pb-4">
      {!isEmbed && <MobilePageHeader title="快速记录" action={<span className="md:hidden text-xs text-slate-500">收集</span>} />}
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
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 xl:grid-cols-[420px_minmax(320px,0.75fr)_minmax(420px,1.25fr)]">
        <section className="flex min-h-[420px] flex-col rounded-lg border border-slate-200 bg-white">
          <div className="border-b border-slate-200 px-4 py-3">
            <h2 className="text-sm font-semibold text-slate-900">新建记录</h2>
            <p className="mt-1 text-xs text-slate-500">支持 Markdown 和图片上传。</p>
          </div>
          <div className="flex min-h-0 flex-1 flex-col gap-3 p-3">
            <div className="min-h-0 flex-1 overflow-auto">
              <QuickNoteEditor value={draft} onChange={setDraft} minHeight={320} />
            </div>
            <div className="flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
              <span className="text-xs text-slate-400">{draft.trim().length} 字符</span>
              <button
                type="button"
                onClick={handleCreate}
                disabled={!draft.trim() || createMutation.isPending}
                className="min-h-[44px] rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-300"
              >
                {createMutation.isPending ? '保存中...' : '保存记录'}
              </button>
            </div>
          </div>
        </section>

        <section className="flex min-h-[420px] flex-col rounded-lg border border-slate-200 bg-white">
          <div className="space-y-3 border-b border-slate-200 p-3">
            <div className="flex flex-wrap gap-2">
              {statusFilters.map(item => (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => {
                    setStatus(item.key);
                    setSelection(null);
                  }}
                  className={`min-h-[44px] rounded-full border px-3 py-1.5 text-xs font-semibold transition-colors ${
                    status === item.key
                      ? 'border-blue-600 bg-blue-600 text-white'
                      : 'border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-slate-50'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
            <label className="block">
              <span className="sr-only">搜索快速记录</span>
              <input
                type="search"
                value={search}
                onChange={event => {
                  setSearch(event.target.value);
                  setSelection(null);
                }}
                placeholder="搜索内容..."
                className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
              />
            </label>
          </div>

          <div className="min-h-0 flex-1 overflow-auto p-2">
            {listQuery.isLoading ? (
              <div className="p-4 text-sm text-slate-500">加载中...</div>
            ) : notes.length === 0 ? (
              <EmptyState title="没有快速记录" description="调整筛选或新建一条记录。" />
            ) : (
              <div className="space-y-2">
                {notes.map(note => {
                  const active = note.id === selectedId;
                  return (
                    <button
                      key={note.id}
                      type="button"
                      onClick={() => selectNote(note)}
                      className={`w-full rounded-lg border p-3 text-left transition-colors ${
                        active
                          ? 'border-blue-300 bg-blue-50'
                          : 'border-slate-200 bg-white hover:border-blue-200 hover:bg-slate-50'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <p className="line-clamp-2 min-w-0 text-sm font-medium text-slate-900">{noteTitle(note)}</p>
                        <StatusBadge status={note.status} />
                      </div>
                      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                        <span>{formatDateTime(note.updatedAt)}</span>
                        <span>{note.source}</span>
                        {note.attachmentCount > 0 && <span>{note.attachmentCount} 个附件</span>}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </section>

        <section className="flex min-h-[420px] flex-col rounded-lg border border-slate-200 bg-white">
          {!selectedId ? (
            <div className="p-4">
              <EmptyState title="选择一条记录" description="从列表中打开记录后可预览、编辑和处理。" />
            </div>
          ) : detailQuery.isLoading ? (
            <div className="p-4 text-sm text-slate-500">加载详情中...</div>
          ) : !selected ? (
            <div className="p-4">
              <EmptyState title="记录不可用" description="这条记录可能已被删除，请刷新列表。" />
            </div>
          ) : (
            <>
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-4 py-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate text-sm font-semibold text-slate-900">记录详情</h2>
                    <StatusBadge status={selected.status} />
                  </div>
                  <p className="mt-1 text-xs text-slate-500">
                    创建 {formatDateTime(selected.createdAt)} · 更新 {formatDateTime(selected.updatedAt)}
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  {selected.status !== 'processed' && selected.status !== 'archived' && (
                    <button
                      type="button"
                      onClick={() => processMutation.mutate(selected.id)}
                      disabled={busy}
                      className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      标记处理
                    </button>
                  )}
                  {selected.status !== 'archived' ? (
                    <button
                      type="button"
                      onClick={() => archiveMutation.mutate(selected.id)}
                      disabled={busy}
                      className="rounded-md border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      归档
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => restoreMutation.mutate({ id: selected.id, nextStatus: 'inbox' })}
                      disabled={busy}
                      className="rounded-md border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      恢复到收集箱
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => handleDelete(selected)}
                    disabled={busy}
                    className="rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    删除
                  </button>
                </div>
              </div>

              <div className="grid min-h-0 flex-1 grid-cols-1 gap-0 overflow-hidden 2xl:grid-cols-2">
                <div className="flex min-h-[360px] flex-col border-b border-slate-200 p-3 2xl:border-b-0 2xl:border-r">
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <h3 className="text-xs font-semibold uppercase text-slate-500">编辑</h3>
                    <button
                      type="button"
                      onClick={handleSave}
                      disabled={!editMarkdown.trim() || editMarkdown === selected.contentMarkdown || updateMutation.isPending}
                      className="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300"
                    >
                      {updateMutation.isPending ? '保存中...' : '保存修改'}
                    </button>
                  </div>
                  <div className="min-h-0 flex-1 overflow-auto">
                    <QuickNoteEditor value={editMarkdown} onChange={setEditMarkdown} minHeight={300} />
                  </div>
                </div>

                <div className="min-h-[360px] overflow-auto p-3">
                  <h3 className="mb-2 text-xs font-semibold uppercase text-slate-500">预览</h3>
                  <QuickNoteMarkdownPreview markdown={editMarkdown} attachments={selected.attachments} minHeight={300} />
                </div>
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

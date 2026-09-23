import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { FolderTree, Loader2, RefreshCw, X } from 'lucide-react';
import { toast } from 'sonner';
import PageHeader from '../ui/PageHeader';
import OneDriveBindDialog from '../components/files/OneDriveBindDialog';
import OneDriveFileList from '../components/files/OneDriveFileList';
import OneDriveFileTree, { type FolderLoadState } from '../components/files/OneDriveFileTree';
import OneDrivePreviewPane from '../components/files/OneDrivePreviewPane';
import { loadFolderTree } from '../components/files/loadFolderTree';
import {
  FILE_PAGE_SIZE,
  breadcrumbSegments,
  normalizeDirPath,
  readFileBrowserMemory,
  writeFileBrowserMemory,
  type FileBrowserMemory,
} from '../components/files/fileBrowserState';
import {
  disconnectFileProvider,
  getFileItems,
  getFileProviders,
  getOneDriveSyncResult,
  searchFiles,
} from '../api/files';
import type { FileItem, FileSearchScope, FileSortKey, FileSortOrder } from '../types';

const EMPTY_ITEMS: FileItem[] = [];

/**
 * 文件页（REQ-1 ~ REQ-10）：左栏目录树 ｜ 中栏列表/网格 + 搜索 + 分页 ｜ 右栏预览。
 *
 * 与上一版的区别（工单 §1）：
 * - 列表数据由服务端分页提供，不再「逐页拉到 2000 条就停且不提示」（现状-2）；
 * - 树与列表共用同一条导航状态：单击树只展开/收起，双击/列表点击才切换目录（现状-3）；
 * - 打开时回到上次离开的目录（REQ-1）；三栏撑满整屏（REQ-6）；窄屏为「列表 → 全屏预览 + 树抽屉」（REQ-7）。
 *
 * 服务端只存元数据；预览与下载经 PIM 稳定端点 302 到 OneDrive 直链（内容不经服务器）。
 */
export default function FilesPage() {
  const queryClient = useQueryClient();
  const [bindDialogOpen, setBindDialogOpen] = useState(false);

  // 记忆：上次离开的目录 / 视图 / 排序 / 搜索范围（REQ-1、REQ-9）
  const [memory, setMemory] = useState<FileBrowserMemory>(() =>
    readFileBrowserMemory(typeof window === 'undefined' ? null : window.localStorage),
  );
  const [currentPath, setCurrentPath] = useState(memory.path);
  const [page, setPage] = useState(1);
  const [queryInput, setQueryInput] = useState('');
  const [selectedItem, setSelectedItem] = useState<FileItem | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [mobilePreviewOpen, setMobilePreviewOpen] = useState(false);
  const [syncing, setSyncing] = useState(false);
  /** 曾经展开过（或需要为其准备数据）的目录集合；只增不减，收起后不重新拉取。 */
  const [expandedPaths, setExpandedPaths] = useState<string[]>(['/']);

  const providersQuery = useQuery({
    queryKey: ['files', 'providers'],
    queryFn: getFileProviders,
  });

  const oneDriveProvider = useMemo(
    () => providersQuery.data?.find(p => p.provider === 'onedrive') ?? null,
    [providersQuery.data],
  );
  const connected = oneDriveProvider?.status === 'connected';

  const updateMemory = useCallback((patch: Partial<FileBrowserMemory>) => {
    setMemory(prev => {
      const next = { ...prev, ...patch };
      writeFileBrowserMemory(next, typeof window === 'undefined' ? null : window.localStorage);
      return next;
    });
  }, []);

  const { sort, order, view, searchScope } = memory;

  // ---- 中栏数据：当前目录（服务端过滤/排序/分页）或全盘搜索结果 ----
  const debouncedQuery = useDebouncedValue(queryInput.trim(), 250);
  const globalSearchActive = searchScope === 'global' && debouncedQuery.length > 0;

  const listQuery = useQuery({
    queryKey: ['files', 'items', oneDriveProvider?.id ?? 'none', currentPath, page, sort, order, searchScope === 'folder' ? debouncedQuery : ''],
    queryFn: () =>
      getFileItems({
        path: currentPath,
        page,
        pageSize: FILE_PAGE_SIZE,
        sort,
        order,
        q: searchScope === 'folder' ? debouncedQuery || undefined : undefined,
      }),
    enabled: connected && !globalSearchActive,
  });

  const searchQuery = useQuery({
    queryKey: ['files', 'search', oneDriveProvider?.id ?? 'none', debouncedQuery, page],
    queryFn: () => searchFiles(debouncedQuery, 'keyword', page, FILE_PAGE_SIZE),
    enabled: connected && globalSearchActive,
  });

  const activeQuery = globalSearchActive ? searchQuery : listQuery;
  const items = useMemo(() => {
    if (globalSearchActive) return searchQuery.data?.items ?? EMPTY_ITEMS;
    return listQuery.data?.result.items ?? EMPTY_ITEMS;
  }, [globalSearchActive, searchQuery.data, listQuery.data]);

  const totalCount = globalSearchActive
    ? searchQuery.data?.totalCount ?? 0
    : listQuery.data?.result.totalCount ?? 0;
  const totalPages = globalSearchActive
    ? searchQuery.data?.totalPages ?? 0
    : listQuery.data?.result.totalPages ?? 0;

  const listError = activeQuery.isError ? describeError(activeQuery.error) : null;
  const listLoading = activeQuery.isPending || activeQuery.isFetching;

  // 页码越界（例如切目录后页数变少）时收敛回有效页，避免停在空白页
  useEffect(() => {
    if (totalPages > 0 && page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  // ---- 左栏数据：目录树（只取目录，按目录懒加载）----
  const foldersToLoad = useMemo(() => {
    const wanted = new Set<string>(['/']);
    for (const segment of breadcrumbSegments(currentPath)) wanted.add(segment.path);
    for (const path of expandedPaths) wanted.add(path);
    return [...wanted].sort();
  }, [currentPath, expandedPaths]);

  const folderQueries = useQueries({
    queries: foldersToLoad.map(path => ({
      queryKey: ['files', 'folders', oneDriveProvider?.id ?? 'none', path],
      queryFn: () => loadFolderTree(path),
      enabled: connected,
    })),
  });

  const { foldersByPath, folderStates, folderTruncation } = useMemo(() => {
    const byPath: Record<string, FileItem[]> = {};
    const states: Record<string, FolderLoadState> = {};
    const truncation: Record<string, { loaded: number; total: number }> = {};

    foldersToLoad.forEach((path, index) => {
      const query = folderQueries[index];
      if (!query) return;
      if (query.data) {
        byPath[path] = query.data.items;
        states[path] = 'loaded';
        if (query.data.truncated) truncation[path] = { loaded: query.data.items.length, total: query.data.totalCount };
      } else if (query.isError) {
        // 失败时不能把该目录当成空目录：给出明确的失败态（AC-4.2）
        states[path] = 'error';
        byPath[path] = [];
      } else {
        states[path] = 'loading';
      }
    });

    return { foldersByPath: byPath, folderStates: states, folderTruncation: truncation };
  }, [folderQueries, foldersToLoad]);

  const currentTruncation = folderTruncation[currentPath] ?? null;

  // ---- 导航 ----
  const navigateTo = useCallback(
    (path: string) => {
      const normalized = normalizeDirPath(path);
      setCurrentPath(normalized);
      setPage(1);
      setSelectedItem(null);
      setMobilePreviewOpen(false);
      updateMemory({ path: normalized });
    },
    [updateMemory],
  );

  const handleOpenFolder = useCallback(
    (path: string) => {
      navigateTo(path);
      setExpandedPaths(prev => (prev.includes(path) ? prev : [...prev, path]));
    },
    [navigateTo],
  );

  /** 树：单击只展开/收起（数据按需加载），不改变中栏（AC-5.1）。 */
  const handleToggleFolder = useCallback((path: string) => {
    setExpandedPaths(prev => (prev.includes(path) ? prev : [...prev, path]));
  }, []);

  /** 手机抽屉：点一下直接进入并收起抽屉（AC-5.3）。 */
  const handleEnterFolderFromDrawer = useCallback(
    (path: string) => {
      handleOpenFolder(path);
      setDrawerOpen(false);
    },
    [handleOpenFolder],
  );

  const handleScopeChange = useCallback(
    (scope: FileSearchScope) => {
      setPage(1);
      updateMemory({ searchScope: scope });
    },
    [updateMemory],
  );

  const handleSortChange = useCallback(
    (nextSort: FileSortKey, nextOrder: FileSortOrder) => {
      setPage(1);
      updateMemory({ sort: nextSort, order: nextOrder });
    },
    [updateMemory],
  );

  /** 全盘搜索命中某条 → 跳到它所在目录（AC-8.1）；跳出搜索态，避免中栏继续显示旧结果。 */
  const handleRevealInFolder = useCallback(
    (path: string) => {
      setQueryInput('');
      updateMemory({ searchScope: 'folder', path: normalizeDirPath(path) });
      setCurrentPath(normalizeDirPath(path));
      setPage(1);
      setSelectedItem(null);
      setMobilePreviewOpen(false);
    },
    [updateMemory],
  );

  const handleRetry = useCallback(() => {
    void activeQuery.refetch();
  }, [activeQuery]);

  const handleSync = useCallback(async () => {
    if (!oneDriveProvider) return;
    setSyncing(true);
    try {
      const result = await getOneDriveSyncResult(oneDriveProvider.id);
      toast.success(
        `同步完成：${result.pagesProcessed} 页，应用 ${result.itemsApplied} 项${result.fullRecrawl ? '（全量重扫）' : ''}`,
      );
      await queryClient.invalidateQueries({ queryKey: ['files'] });
    } catch (e) {
      toast.error(describeError(e));
    } finally {
      setSyncing(false);
    }
  }, [oneDriveProvider, queryClient]);

  const handleDisconnect = useCallback(async () => {
    if (!oneDriveProvider) return;
    try {
      await disconnectFileProvider(oneDriveProvider.id);
      toast.success('已断开 OneDrive 绑定（仅清除本地元数据）');
      setExpandedPaths(['/']);
      navigateTo('/');
      await queryClient.invalidateQueries({ queryKey: ['files'] });
    } catch (e) {
      toast.error(describeError(e));
    }
  }, [navigateTo, oneDriveProvider, queryClient]);

  const handleConnected = useCallback(() => {
    setBindDialogOpen(false);
    toast.success('OneDrive 绑定成功');
    queryClient.invalidateQueries({ queryKey: ['files'] });
  }, [queryClient]);

  // 同步后元数据可能变化：按 id 从当前列表里取最新对象派生
  const selectedItemLive = useMemo(() => {
    if (!selectedItem) return null;
    return items.find(item => item.id === selectedItem.id) ?? selectedItem;
  }, [selectedItem, items]);

  const syncChip = useMemo(() => {
    if (!oneDriveProvider) return null;
    const status = oneDriveProvider.syncStatus ?? 'idle';
    if (status === 'syncing') return { dot: 'bg-amber-500', text: '正在同步…' };
    if (status === 'error') return { dot: 'bg-red-500', text: '同步出错' };
    if (oneDriveProvider.lastSyncAt) {
      // 刻意读取墙钟判断同步新鲜度；memo 依赖 lastSyncAt 变化时刷新
      // eslint-disable-next-line react-hooks/purity
      const ageMs = Date.now() - new Date(oneDriveProvider.lastSyncAt).getTime();
      if (ageMs > 45 * 60 * 1000) {
        return { dot: 'bg-amber-500', text: '同步落后（超过 45 分钟）' };
      }
      return {
        dot: 'bg-green-500',
        text: `增量同步 ${new Date(oneDriveProvider.lastSyncAt).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`,
      };
    }
    return { dot: 'bg-zinc-400', text: '尚未同步' };
  }, [oneDriveProvider]);

  const tree = (
    <OneDriveFileTree
      foldersByPath={foldersByPath}
      folderStates={folderStates}
      currentPath={currentPath}
      onToggleFolder={handleToggleFolder}
      onEnterFolder={handleOpenFolder}
      onRetryFolder={path => {
        void queryClient.invalidateQueries({ queryKey: ['files', 'folders', oneDriveProvider?.id ?? 'none', path] });
      }}
    />
  );

  const treeDrawerTree = (
    <OneDriveFileTree
      foldersByPath={foldersByPath}
      folderStates={folderStates}
      currentPath={currentPath}
      // 手机抽屉：点一下 = 直接进入（AC-5.3）
      onToggleFolder={handleEnterFolderFromDrawer}
      onEnterFolder={handleEnterFolderFromDrawer}
      onRetryFolder={path => {
        void queryClient.invalidateQueries({ queryKey: ['files', 'folders', oneDriveProvider?.id ?? 'none', path] });
      }}
    />
  );

  return (
    <div className="flex h-full flex-col">
      <PageHeader title="文件" subtitle="OneDrive 个人版 · 只存元数据，内容留在云端" />

      {/* 三栏撑满可用宽度（REQ-6）：不再居中限宽，窗口变化时按比例分配 */}
      <div className="flex w-full flex-1 flex-col overflow-hidden px-2 pb-2 md:px-4 md:pb-4">
        {!connected ? (
          <EmptyBindingState
            loading={providersQuery.isLoading}
            hasPending={oneDriveProvider?.status === 'pending'
              || oneDriveProvider?.status === 'expired'
              || oneDriveProvider?.status === 'denied'}
            onBind={() => setBindDialogOpen(true)}
          />
        ) : (
          <div className="pim-card flex min-h-0 flex-1 overflow-hidden">
            {/* 左栏：目录树（~240px；手机收入抽屉） */}
            <aside
              className="hidden w-60 shrink-0 flex-col border-r border-[var(--pim-border)] bg-[var(--pim-surface)] md:flex"
              data-testid="tree-pane"
            >
              <div className="flex items-center justify-between gap-2 border-b border-[var(--pim-border)] px-3 py-2 text-xs text-[var(--pim-text-muted)]">
                <span className="truncate">{oneDriveProvider?.accountName ?? 'OneDrive'}</span>
                <button type="button" className="shrink-0 text-[var(--pim-primary)] underline" onClick={handleDisconnect}>
                  断开
                </button>
              </div>
              <div className="min-h-0 flex-1 overflow-auto">{tree}</div>
              {currentTruncation && (
                <div className="border-t border-[var(--pim-border)] px-3 py-1.5 text-[11px] text-[var(--pim-warning)]" data-testid="tree-truncated">
                  已加载 {currentTruncation.loaded} / 共 {currentTruncation.total} 个文件夹
                </div>
              )}
            </aside>

            {/* 中栏 */}
            <div className="flex min-w-0 flex-1 flex-col">
              <div className="flex items-center gap-2 border-b border-[var(--pim-border)] px-3 py-2">
                <button
                  type="button"
                  className="pim-button-secondary inline-flex items-center gap-1 px-2 py-1 text-xs md:hidden"
                  aria-label="打开目录抽屉"
                  onClick={() => setDrawerOpen(true)}
                >
                  <FolderTree size={14} /> 目录
                </button>
                {syncChip && (
                  <span
                    className="inline-flex items-center gap-1.5 rounded-full border border-[var(--pim-border)] px-2.5 py-1 text-xs text-[var(--pim-text-muted)]"
                    data-testid="sync-chip"
                  >
                    <span className={`h-1.5 w-1.5 rounded-full ${syncChip.dot}`} />
                    {syncChip.text}
                  </span>
                )}
                <button
                  type="button"
                  className="pim-button-secondary ml-auto inline-flex items-center gap-1.5 px-3 text-sm"
                  onClick={handleSync}
                  disabled={syncing}
                >
                  {syncing ? <Loader2 size={14} className="animate-spin" /> : <RefreshCw size={14} />}
                  立即同步
                </button>
              </div>
              <OneDriveFileList
                items={items}
                loading={listLoading && items.length === 0}
                error={listError}
                onRetry={handleRetry}
                query={queryInput}
                onQueryChange={value => {
                  setQueryInput(value);
                  setPage(1);
                }}
                searchScope={searchScope}
                onSearchScopeChange={handleScopeChange}
                showFullPath={globalSearchActive}
                onRevealInFolder={handleRevealInFolder}
                currentPath={currentPath}
                onNavigate={navigateTo}
                sort={sort}
                order={order}
                onSortChange={handleSortChange}
                page={page}
                totalPages={totalPages}
                totalCount={totalCount}
                onPageChange={setPage}
                view={view}
                onViewChange={nextView => updateMemory({ view: nextView })}
                selectedItem={selectedItemLive}
                onSelect={item => {
                  setSelectedItem(item);
                  setMobilePreviewOpen(true);
                }}
                onOpenFolder={handleOpenFolder}
              />
            </div>

            {/* 右栏：预览（~340px；窄屏为全屏浮层）。
                无选中时保留同宽占位说明，不得塌缩成 0 宽、也不得遮挡列表（AC-6.2）。 */}
            <div
              className={
                mobilePreviewOpen && selectedItem
                  ? 'fixed inset-0 z-40 flex flex-col overflow-auto bg-[var(--pim-surface)] md:static md:z-auto md:w-[340px] md:shrink-0 md:overflow-y-auto md:border-l md:border-[var(--pim-border)] md:bg-transparent'
                  : 'hidden md:flex md:w-[340px] md:shrink-0 md:flex-col md:border-l md:border-[var(--pim-border)]'
              }
              data-testid="preview-column"
            >
              <button
                type="button"
                className="border-b border-[var(--pim-border)] px-4 py-2 text-left text-sm text-[var(--pim-primary)] md:hidden"
                onClick={() => setMobilePreviewOpen(false)}
              >
                ← 返回列表
              </button>
              <div className="min-h-0 flex-1 md:flex">
                {selectedItemLive ? (
                  <OneDrivePreviewPane item={selectedItemLive} onToast={message => toast(message)} />
                ) : (
                  <div
                    className="flex w-full flex-1 items-center justify-center p-6 text-center text-sm text-[var(--pim-text-muted)]"
                    data-testid="preview-placeholder"
                  >
                    选择一个文件查看预览
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 手机：目录抽屉（AC-5.3 / AC-7.1） */}
      {connected && drawerOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden" data-testid="tree-drawer">
          <div className="flex h-full w-64 flex-col bg-[var(--pim-surface)] shadow-[var(--pim-shadow-pop)]">
            <div className="flex items-center justify-between border-b border-[var(--pim-border)] px-3 py-2 text-sm">
              <span className="truncate">{oneDriveProvider?.accountName ?? 'OneDrive'}</span>
              <button type="button" aria-label="关闭目录抽屉" onClick={() => setDrawerOpen(false)}>
                <X size={16} />
              </button>
            </div>
            <div className="min-h-0 flex-1 overflow-auto">{treeDrawerTree}</div>
          </div>
          <button
            type="button"
            className="h-full flex-1 bg-black/30"
            aria-label="关闭目录抽屉遮罩"
            onClick={() => setDrawerOpen(false)}
          />
        </div>
      )}

      {bindDialogOpen && (
        <OneDriveBindDialog
          onClose={() => setBindDialogOpen(false)}
          onConnected={handleConnected}
          initialClientId={oneDriveProvider?.clientId ?? ''}
        />
      )}
    </div>
  );
}

/** 把任意异常翻译成可读原因（AC-10.3：不得出现「未知错误」）。 */
function describeError(error: unknown): string {
  if (error instanceof Error && error.message.trim()) return error.message;
  return '加载失败，请稍后重试';
}

/** 输入去抖：避免每敲一个字就打一次服务端搜索。 */
function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => setDebounced(value), delayMs);
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
  }, [value, delayMs]);

  return debounced;
}

function EmptyBindingState({ loading, hasPending, onBind }: { loading: boolean; hasPending: boolean; onBind: () => void }) {
  useEffect(() => {
    // 绑定未完成（pending/expired/denied）时自动弹出对话框，免去手动点击
    if (hasPending) onBind();
  }, [hasPending, onBind]);
  if (loading) {
    return (
      <div className="flex flex-1 items-center justify-center p-16 text-sm text-[var(--pim-text-muted)]" data-testid="files-loading">
        <Loader2 size={18} className="mr-2 animate-spin" /> 加载中…
      </div>
    );
  }
  return (
    <div className="pim-card mt-6 flex flex-col items-center gap-3 p-12 text-center" data-testid="files-empty">
      <p className="text-sm text-[var(--pim-text-muted)]">
        {hasPending
          ? 'OneDrive 绑定尚未完成授权，请重新获取设备码并登录'
          : '还没有绑定 OneDrive。绑定后文件树与元数据自动同步，内容留在云端。'}
      </p>
      <button type="button" className="pim-button-primary px-4" onClick={onBind}>
        {hasPending ? '重新绑定' : '绑定 OneDrive'}
      </button>
    </div>
  );
}

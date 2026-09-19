import { useCallback, useEffect, useMemo, useState } from 'react';
import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import PageHeader from '../ui/PageHeader';
import OneDriveBindDialog from '../components/files/OneDriveBindDialog';
import OneDriveFileList from '../components/files/OneDriveFileList';
import OneDriveFileTree from '../components/files/OneDriveFileTree';
import OneDrivePreviewPane from '../components/files/OneDrivePreviewPane';
import {
  disconnectFileProvider,
  getOneDriveSyncResult,
  getFileItems,
  getFileProviders,
} from '../api/files';
import type { FileItem } from '../types';

const EMPTY_CHILDREN: FileItem[] = [];

/**
 * 文件页（方案 A 浅色三栏，designs/onedrive-files-v2.md §11）：
 * 左栏懒加载文件树 ｜ 中栏列表/网格 + 搜索 ｜ 右栏预览与文本编辑。
 * 服务端只存元数据；预览与下载经 PIM 稳定端点 302 到 OneDrive 直链。
 */
export default function FilesPage() {
  const queryClient = useQueryClient();
  const [bindDialogOpen, setBindDialogOpen] = useState(false);
  const [currentPath, setCurrentPath] = useState('/');
  const [selectedItem, setSelectedItem] = useState<FileItem | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [view, setView] = useState<'list' | 'grid'>('list');
  const [syncing, setSyncing] = useState(false);
  const [mobilePreviewOpen, setMobilePreviewOpen] = useState(false);

  const providersQuery = useQuery({
    queryKey: ['files', 'providers'],
    queryFn: getFileProviders,
  });

  const oneDriveProvider = useMemo(
    () => providersQuery.data?.find(p => p.provider === 'onedrive') ?? null,
    [providersQuery.data],
  );
  const connected = oneDriveProvider?.status === 'connected';

  const loadChildren = useCallback(async (path: string): Promise<FileItem[]> => {
    const first = await getFileItems(path, 1, 100);
    const items = [...first.result.items];
    let page = 1;
    while (page < first.result.totalPages && items.length < 2000) {
      page += 1;
      const next = await getFileItems(path, page, 100);
      items.push(...next.result.items);
    }
    return items;
  }, []);

  // 已加载目录集合（根目录默认）：每个目录一个独立 query，树/列表从查询结果派生
  const [loadedPaths, setLoadedPaths] = useState<string[]>(['/']);
  const pathQueries = useQueries({
    queries: loadedPaths.map(path => ({
      queryKey: ['files', 'children', oneDriveProvider?.id ?? 'none', path],
      queryFn: () => loadChildren(path),
      enabled: connected,
    })),
  });
  const childrenByPath = useMemo(() => {
    const map: Record<string, FileItem[]> = {};
    loadedPaths.forEach((path, index) => {
      const data = pathQueries[index]?.data;
      if (data) map[path] = data;
    });
    return map;
  }, [loadedPaths, pathQueries]);

  const handleExpandFolder = useCallback((path: string) => {
    setCurrentPath(path);
    setSelectedItem(null);
    setMobilePreviewOpen(false);
    setLoadedPaths(prev => (prev.includes(path) ? prev : [...prev, path]));
  }, []);

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
      toast.error(e instanceof Error ? e.message : '同步失败');
    } finally {
      setSyncing(false);
    }
  }, [oneDriveProvider, queryClient]);

  const handleDisconnect = useCallback(async () => {
    if (!oneDriveProvider) return;
    try {
      await disconnectFileProvider(oneDriveProvider.id);
      toast.success('已断开 OneDrive 绑定（仅清除本地元数据）');
      setLoadedPaths(['/']);
      setCurrentPath('/');
      setSelectedItem(null);
      await queryClient.invalidateQueries({ queryKey: ['files'] });
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '断开失败');
    }
  }, [oneDriveProvider, queryClient]);

  const handleConnected = useCallback(() => {
    setBindDialogOpen(false);
    toast.success('OneDrive 绑定成功');
    queryClient.invalidateQueries({ queryKey: ['files'] });
  }, [queryClient]);

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

  const currentChildren = childrenByPath[currentPath] ?? EMPTY_CHILDREN;

  // 同步后元数据可能变化：按 id 从全部已加载目录里取最新对象派生
  const selectedItemLive = useMemo(() => {
    if (!selectedItem) return null;
    for (const children of Object.values(childrenByPath)) {
      const fresh = children.find(child => child.id === selectedItem.id);
      if (fresh) return fresh;
    }
    return selectedItem;
  }, [selectedItem, childrenByPath]);

  const currentLoading =
    pathQueries.find((_, index) => loadedPaths[index] === currentPath)?.isLoading ?? false;

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="文件"
        subtitle="OneDrive 个人版 · 只存元数据，内容留在云端"
      />

      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col overflow-auto px-4 pb-20 md:pb-6">
        {!connected ? (
          <EmptyBindingState
            loading={providersQuery.isLoading}
            hasPending={oneDriveProvider?.status === 'pending'
              || oneDriveProvider?.status === 'expired'
              || oneDriveProvider?.status === 'denied'}
            onBind={() => setBindDialogOpen(true)}
          />
        ) : (
          <div className="pim-card flex min-h-[560px] flex-1 overflow-hidden">
            {/* 左栏：文件树（移动端隐藏） */}
            <aside className="hidden w-64 shrink-0 border-r border-[var(--pim-border)] bg-[var(--pim-surface)] md:block" data-testid="tree-pane">
              <div className="flex items-center justify-between border-b border-[var(--pim-border)] px-3 py-2 text-xs text-[var(--pim-text-muted)]">
                <span>{oneDriveProvider?.accountName ?? 'OneDrive'}</span>
                <button type="button" className="text-[var(--pim-primary)] underline" onClick={handleDisconnect}>
                  断开
                </button>
              </div>
              <div className="h-[calc(100%-41px)] overflow-auto">
                <OneDriveFileTree
                  childrenByPath={childrenByPath}
                  selectedItemId={selectedItem?.id ?? null}
                  onSelect={item => {
                    if (item.itemType === 'folder') {
                      handleExpandFolder(item.path);
                    } else {
                      // 树回传的是精简行，从缓存解析出真实 FileItem（元数据完整）
                      const real = Object.values(childrenByPath)
                        .flat()
                        .find(child => child.id === item.id);
                      setSelectedItem(real ?? item);
                      setMobilePreviewOpen(true);
                    }
                  }}
                  onExpandFolder={handleExpandFolder}
                />
              </div>
            </aside>

            {/* 中栏 */}
            <div className="flex min-w-0 flex-1 flex-col">
              <div className="flex items-center gap-2 border-b border-[var(--pim-border)] px-4 py-2">
                {syncChip && (
                  <span
                    className="inline-flex items-center gap-1.5 rounded-full border border-[var(--pim-border)] px-2.5 py-1 text-xs text-[var(--pim-text-muted)]"
                    data-testid="sync-chip"
                  >
                    <span className={`h-1.5 w-1.5 rounded-full ${syncChip.dot}`} />
                    {syncChip.text}
                  </span>
                )}
                <span className="rounded-full border border-[var(--pim-border)] px-2.5 py-1 text-xs text-[var(--pim-text-muted)]">
                  {currentPath}
                </span>
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
                items={currentChildren}
                loading={currentLoading}
                breadcrumb={`OneDrive / ${currentPath === '/' ? '' : currentPath.slice(1)}`}
                searchQuery={searchQuery}
                onSearchChange={setSearchQuery}
                view={view}
                onViewChange={setView}
                selectedItem={selectedItemLive}
                onSelect={item => {
                  setSelectedItem(item);
                  setMobilePreviewOpen(true);
                }}
                onOpenFolder={handleExpandFolder}
              />
            </div>

            {/* 右栏：预览（窄屏为浮层） */}
            <div
              className={
                mobilePreviewOpen && selectedItem
                  ? 'fixed inset-0 z-40 overflow-auto bg-[var(--pim-surface)] md:static md:z-auto md:flex md:overflow-y-auto md:bg-transparent'
                  : 'hidden md:flex'
              }
            >
              <div className="flex h-full w-full flex-col">
                <button
                  type="button"
                  className="border-b border-[var(--pim-border)] px-4 py-2 text-left text-sm text-[var(--pim-primary)] md:hidden"
                  onClick={() => setMobilePreviewOpen(false)}
                >
                  ← 返回列表
                </button>
                <div className="min-h-0 flex-1 md:flex">
                  <OneDrivePreviewPane item={selectedItemLive} onToast={message => toast(message)} />
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

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

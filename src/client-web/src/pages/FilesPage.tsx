import {
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  acceptFileSuggestion,
  bindNextcloudProvider,
  deleteFile,
  dismissFileSuggestion,
  downloadFileBlob,
  downloadFileVersionBlob,
  getFileItem,
  getFileItems,
  getFileOpenLink,
  getFileProviders,
  getFileSuggestions,
  getFileTrash,
  getFileVersions,
  indexFile,
  moveFile,
  renameFile,
  restoreFileTrash,
  restoreFileVersion,
  restoreFileVersionPreview,
  searchFiles,
  syncFileProvider,
  testFileProvider,
  uploadFile,
} from '../api/files';
import type {
  FileItem,
  FileOpenLinkMode,
  FileProvider,
  FileSearchMode,
  FileSuggestion,
  FileTrashItem,
  FileVersion,
} from '../types';
import PageHeader from '../ui/PageHeader';

type SortKey = 'name' | 'modifiedAt' | 'size';
type SortDirection = 'asc' | 'desc';

const emptyProviders: FileProvider[] = [];
const emptyItems: FileItem[] = [];
const emptySuggestions: FileSuggestion[] = [];
const emptyTrash: FileTrashItem[] = [];
const emptyVersions: FileVersion[] = [];

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试。';
}

function normalizeFolderPath(path: string) {
  const trimmed = path.trim();
  if (!trimmed || trimmed === '/') return '/';
  const withSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return withSlash.replace(/\/+$/, '') || '/';
}

function joinPath(folder: string, name: string) {
  const base = normalizeFolderPath(folder);
  return base === '/' ? `/${name}` : `${base}/${name}`;
}

function parentPath(path: string) {
  const normalized = normalizeFolderPath(path);
  if (normalized === '/') return '/';
  const index = normalized.lastIndexOf('/');
  return index <= 0 ? '/' : normalized.slice(0, index);
}

function breadcrumb(path: string) {
  const normalized = normalizeFolderPath(path);
  if (normalized === '/') return [{ label: 'Root', path: '/' }];

  const parts = normalized.split('/').filter(Boolean);
  return [
    { label: 'Root', path: '/' },
    ...parts.map((part, index) => ({
      label: part,
      path: `/${parts.slice(0, index + 1).join('/')}`,
    })),
  ];
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatBytes(value: number | null | undefined) {
  if (value == null) return '-';
  if (value < 1024) return `${value} B`;
  const units = ['KB', 'MB', 'GB', 'TB'];
  let size = value / 1024;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit += 1;
  }
  return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unit]}`;
}

function isOoxmlFile(item: FileItem | null) {
  if (!item) return false;
  const name = item.name.toLowerCase();
  const mime = item.mimeType?.toLowerCase() ?? '';
  return (
    mime.includes('officedocument')
    || name.endsWith('.docx')
    || name.endsWith('.xlsx')
    || name.endsWith('.pptx')
  );
}

function statusTone(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();
  if (['connected', 'completed', 'indexed', 'accepted', 'success'].includes(normalized)) {
    return 'border-emerald-200 bg-emerald-50 text-emerald-700';
  }
  if (['error', 'failed', 'deleted', 'dismissed'].includes(normalized)) {
    return 'border-red-200 bg-red-50 text-red-700';
  }
  if (['pending', 'queued', 'running', 'processing'].includes(normalized)) {
    return 'border-blue-200 bg-blue-50 text-blue-700';
  }
  return 'border-slate-200 bg-slate-50 text-slate-600';
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

function Section({ title, children, actions }: { title: string; children: ReactNode; actions?: ReactNode }) {
  return (
    <section className="min-w-0 rounded-lg border border-slate-200 bg-white">
      <div className="flex min-h-[44px] items-center justify-between gap-3 border-b border-slate-200 px-3 py-2">
        <h2 className="truncate text-sm font-semibold text-slate-900">{title}</h2>
        {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
      </div>
      {children}
    </section>
  );
}

function StatusBadge({ label }: { label: string | null | undefined }) {
  return (
    <span className={`inline-flex max-w-full items-center rounded-full border px-2 py-0.5 text-xs font-medium ${statusTone(label)}`}>
      <span className="truncate">{label || '-'}</span>
    </span>
  );
}

function itemIcon(item: FileItem) {
  if (item.itemType === 'folder') return 'DIR';
  const ext = item.name.includes('.') ? item.name.split('.').pop()?.toUpperCase() : null;
  return ext?.slice(0, 4) || 'FILE';
}

export default function FilesPage() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [activeProviderId, setActiveProviderId] = useState<string | null>(null);
  const [currentPath, setCurrentPath] = useState('/');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [searchText, setSearchText] = useState('');
  const [submittedSearch, setSubmittedSearch] = useState('');
  const [searchMode, setSearchMode] = useState<FileSearchMode>('hybrid');
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [providerMessage, setProviderMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [bindForm, setBindForm] = useState({
    baseUrl: '',
    internalBaseUrl: '',
    username: '',
    appPassword: '',
  });

  const providersQuery = useQuery({
    queryKey: ['files', 'providers'],
    queryFn: getFileProviders,
  });

  const providers = providersQuery.data ?? emptyProviders;
  const activeProvider = useMemo(
    () => providers.find(provider => provider.id === activeProviderId) ?? providers[0] ?? null,
    [activeProviderId, providers],
  );

  useEffect(() => {
    if (providers.length === 0) {
      setActiveProviderId(null);
      return;
    }

    if (!activeProviderId || !providers.some(provider => provider.id === activeProviderId)) {
      setActiveProviderId(providers[0].id);
    }
  }, [activeProviderId, providers]);

  const itemsQuery = useQuery({
    queryKey: ['files', 'items', currentPath],
    queryFn: () => getFileItems(currentPath),
    enabled: providers.length > 0,
  });

  const listItems = itemsQuery.data?.result.items ?? emptyItems;
  const folderTreeRows = useMemo(() => {
    const trail = breadcrumb(currentPath).map((crumb, index) => ({
      id: `path:${crumb.path}`,
      label: crumb.label,
      path: crumb.path,
      depth: index,
      current: crumb.path === currentPath,
      item: null as FileItem | null,
    }));

    const children = listItems
      .filter(item => item.itemType === 'folder')
      .map(folder => ({
        id: `folder:${folder.id}`,
        label: folder.name,
        path: folder.path,
        depth: trail.length,
        current: false,
        item: folder,
      }));

    return [...trail, ...children];
  }, [currentPath, listItems]);

  const searchQuery = useQuery({
    queryKey: ['files', 'search', submittedSearch, searchMode],
    queryFn: () => searchFiles(submittedSearch, searchMode),
    enabled: submittedSearch.length > 0,
  });

  const visibleItems = submittedSearch ? (searchQuery.data?.items ?? emptyItems) : listItems;
  const semanticHits = submittedSearch ? (searchQuery.data?.chunks ?? []) : [];

  const sortedItems = useMemo(() => {
    const multiplier = sortDirection === 'asc' ? 1 : -1;
    return [...visibleItems].sort((a, b) => {
      if (a.itemType !== b.itemType) return a.itemType === 'folder' ? -1 : 1;

      if (sortKey === 'size') {
        return ((a.size ?? -1) - (b.size ?? -1)) * multiplier;
      }

      const left = sortKey === 'modifiedAt' ? a.modifiedAt : a.name;
      const right = sortKey === 'modifiedAt' ? b.modifiedAt : b.name;
      return left.localeCompare(right, 'zh-CN') * multiplier;
    });
  }, [sortDirection, sortKey, visibleItems]);

  const detailQuery = useQuery({
    queryKey: ['files', 'item', selectedId],
    queryFn: () => getFileItem(selectedId as string),
    enabled: Boolean(selectedId),
  });

  const selectedFromList = visibleItems.find(item => item.id === selectedId) ?? null;
  const selected = detailQuery.data ?? selectedFromList;

  const versionsQuery = useQuery({
    queryKey: ['files', 'versions', selectedId],
    queryFn: () => getFileVersions(selectedId as string),
    enabled: Boolean(selectedId),
  });

  const versions = versionsQuery.data ?? emptyVersions;

  const suggestionsQuery = useQuery({
    queryKey: ['files', 'suggestions'],
    queryFn: getFileSuggestions,
    enabled: providers.length > 0,
  });

  const suggestions = suggestionsQuery.data ?? emptySuggestions;
  const selectedSuggestions = selected
    ? suggestions.filter(suggestion => suggestion.fileItemId === selected.id)
    : emptySuggestions;

  const trashQuery = useQuery({
    queryKey: ['files', 'trash'],
    queryFn: getFileTrash,
    enabled: providers.length > 0,
  });

  const trashItems = trashQuery.data ?? emptyTrash;

  function invalidateFiles(id?: string | null) {
    void queryClient.invalidateQueries({ queryKey: ['files'] });
    if (id) {
      void queryClient.invalidateQueries({ queryKey: ['files', 'item', id] });
      void queryClient.invalidateQueries({ queryKey: ['files', 'versions', id] });
    }
  }

  const bindMutation = useMutation({
    mutationFn: () => bindNextcloudProvider({
      baseUrl: bindForm.baseUrl.trim(),
      internalBaseUrl: bindForm.internalBaseUrl.trim() || null,
      username: bindForm.username.trim(),
      appPassword: bindForm.appPassword,
    }),
    onSuccess: provider => {
      setActiveProviderId(provider.id);
      setBindForm(current => ({ ...current, appPassword: '' }));
      setProviderMessage('Nextcloud provider saved.');
      setError(null);
      invalidateFiles();
    },
    onError: error => setError(errorMessage(error)),
  });

  const testMutation = useMutation({
    mutationFn: (providerId: string) => testFileProvider(providerId),
    onSuccess: result => {
      setProviderMessage(result.success ? 'Connection test passed.' : result.errorMessage || 'Connection test failed.');
      setError(null);
    },
    onError: error => setError(errorMessage(error)),
  });

  const syncMutation = useMutation({
    mutationFn: (providerId: string) => syncFileProvider(providerId),
    onSuccess: () => {
      setProviderMessage('Sync completed.');
      setSubmittedSearch('');
      setSearchText('');
      setError(null);
      invalidateFiles();
    },
    onError: error => setError(errorMessage(error)),
  });

  const uploadMutation = useMutation({
    mutationFn: ({ providerId, path, file }: { providerId: string; path: string; file: File }) => uploadFile(providerId, path, file),
    onSuccess: item => {
      setSelectedId(item.id);
      setError(null);
      invalidateFiles(item.id);
    },
    onError: error => setError(errorMessage(error)),
  });

  const moveMutation = useMutation({
    mutationFn: ({ id, destinationPath }: { id: string; destinationPath: string }) => moveFile(id, { destinationPath }),
    onSuccess: item => {
      setSelectedId(item.id);
      setError(null);
      invalidateFiles(item.id);
    },
    onError: error => setError(errorMessage(error)),
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => renameFile(id, { name }),
    onSuccess: item => {
      setSelectedId(item.id);
      setError(null);
      invalidateFiles(item.id);
    },
    onError: error => setError(errorMessage(error)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteFile(id),
    onSuccess: () => {
      setSelectedId(null);
      setError(null);
      invalidateFiles();
    },
    onError: error => setError(errorMessage(error)),
  });

  const indexMutation = useMutation({
    mutationFn: (id: string) => indexFile(id),
    onSuccess: job => {
      setProviderMessage(`Index job ${job.status}.`);
      setError(null);
      invalidateFiles(job.fileItemId);
    },
    onError: error => setError(errorMessage(error)),
  });

  const openLinkMutation = useMutation({
    mutationFn: ({ id, mode }: { id: string; mode: FileOpenLinkMode }) => getFileOpenLink(id, mode),
    onSuccess: link => {
      window.open(link.url, '_blank', 'noopener,noreferrer');
      setError(null);
    },
    onError: error => setError(errorMessage(error)),
  });

  const downloadMutation = useMutation({
    mutationFn: async (item: FileItem) => ({
      filename: item.name,
      blob: await downloadFileBlob(item.id),
    }),
    onSuccess: ({ blob, filename }) => {
      saveBlob(blob, filename);
      setError(null);
    },
    onError: error => setError(errorMessage(error)),
  });

  const versionDownloadMutation = useMutation({
    mutationFn: async ({ item, version }: { item: FileItem; version: FileVersion }) => ({
      filename: `${version.modifiedAt.slice(0, 10)}-${item.name}`,
      blob: await downloadFileVersionBlob(item.id, version.id),
    }),
    onSuccess: ({ blob, filename }) => {
      saveBlob(blob, filename);
      setError(null);
    },
    onError: error => setError(errorMessage(error)),
  });

  const restoreVersionMutation = useMutation({
    mutationFn: async ({ item, version }: { item: FileItem; version: FileVersion }) => {
      const preview = await restoreFileVersionPreview(item.id, version.id);
      const confirmed = window.confirm(`${preview.summary}\n${preview.currentVersionLabel} -> ${preview.restoreVersionLabel}`);
      if (!confirmed) return null;
      await restoreFileVersion(item.id, version.id);
      return item.id;
    },
    onSuccess: itemId => {
      if (itemId) {
        setProviderMessage('Version restored.');
        setError(null);
        invalidateFiles(itemId);
      }
    },
    onError: error => setError(errorMessage(error)),
  });

  const suggestionMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'accept' | 'dismiss' }) => (
      action === 'accept' ? acceptFileSuggestion(id) : dismissFileSuggestion(id)
    ),
    onSuccess: suggestion => {
      setError(null);
      invalidateFiles(suggestion.fileItemId);
    },
    onError: error => setError(errorMessage(error)),
  });

  const restoreTrashMutation = useMutation({
    mutationFn: (trashId: string) => {
      if (!activeProvider) throw new Error('Select a provider before restoring trash.');
      return restoreFileTrash(activeProvider.id, trashId);
    },
    onSuccess: () => {
      setProviderMessage('Trash item restored.');
      setError(null);
      invalidateFiles();
    },
    onError: error => setError(errorMessage(error)),
  });

  function submitBind(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!bindForm.baseUrl.trim() || !bindForm.username.trim() || !bindForm.appPassword.trim()) {
      setError('Base URL, username, and app password are required.');
      return;
    }
    bindMutation.mutate();
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmittedSearch(searchText.trim());
    setSelectedId(null);
  }

  function clearSearch() {
    setSearchText('');
    setSubmittedSearch('');
    setSelectedId(null);
  }

  function toggleSort(nextKey: SortKey) {
    if (sortKey === nextKey) {
      setSortDirection(current => (current === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(nextKey);
      setSortDirection(nextKey === 'name' ? 'asc' : 'desc');
    }
  }

  function openFolder(item: FileItem) {
    setCurrentPath(item.path);
    clearSearch();
  }

  function selectItem(item: FileItem) {
    if (item.itemType === 'folder') {
      setSelectedId(item.id);
      return;
    }
    setSelectedId(item.id);
  }

  function handleUploadChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !activeProvider) return;
    uploadMutation.mutate({
      providerId: activeProvider.id,
      path: joinPath(currentPath, file.name),
      file,
    });
  }

  function handleMove(item: FileItem) {
    const destinationPath = window.prompt('Move to path', item.path);
    if (!destinationPath?.trim()) return;
    moveMutation.mutate({ id: item.id, destinationPath: destinationPath.trim() });
  }

  function handleRename(item: FileItem) {
    const name = window.prompt('New name', item.name);
    if (!name?.trim() || name.trim() === item.name) return;
    renameMutation.mutate({ id: item.id, name: name.trim() });
  }

  function handleDelete(item: FileItem) {
    if (!window.confirm(`Move "${item.name}" to trash?`)) return;
    deleteMutation.mutate(item.id);
  }

  function handleOpen(item: FileItem, mode: FileOpenLinkMode) {
    openLinkMutation.mutate({ id: item.id, mode });
  }

  const busy =
    bindMutation.isPending
    || testMutation.isPending
    || syncMutation.isPending
    || uploadMutation.isPending
    || moveMutation.isPending
    || renameMutation.isPending
    || deleteMutation.isPending
    || indexMutation.isPending
    || openLinkMutation.isPending
    || downloadMutation.isPending
    || versionDownloadMutation.isPending
    || restoreVersionMutation.isPending
    || suggestionMutation.isPending
    || restoreTrashMutation.isPending;

  return (
    <div className="mx-auto flex h-full max-w-[1600px] flex-col gap-4 pb-4">
      <PageHeader
        title="文件"
        subtitle="Nextcloud 文件、版本、索引和 AI 建议"
        actions={
          <button
            type="button"
            onClick={() => invalidateFiles(selectedId)}
            disabled={providersQuery.isFetching || itemsQuery.isFetching}
            className="pim-button-secondary px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            刷新
          </button>
        }
      />

      {(error || providerMessage) && (
        <div className={`rounded-lg border px-4 py-3 text-sm ${error ? 'border-red-200 bg-red-50 text-red-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'}`}>
          {error || providerMessage}
        </div>
      )}

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 xl:grid-cols-[320px_minmax(520px,1fr)_400px]">
        <div className="flex min-h-0 flex-col gap-4 overflow-auto">
          <Section title="Provider">
            <div className="space-y-3 p-3">
              {providersQuery.isLoading ? (
                <p className="text-sm text-slate-500">Loading providers...</p>
              ) : providers.length === 0 ? (
                <p className="text-sm text-slate-500">No provider connected.</p>
              ) : (
                <div className="space-y-2">
                  {providers.map(provider => {
                    const active = provider.id === activeProvider?.id;
                    return (
                      <button
                        key={provider.id}
                        type="button"
                        onClick={() => setActiveProviderId(provider.id)}
                        className={`w-full rounded-lg border px-3 py-2 text-left transition-colors ${
                          active ? 'border-blue-300 bg-blue-50' : 'border-slate-200 bg-white hover:border-blue-200 hover:bg-slate-50'
                        }`}
                      >
                        <span className="flex items-center justify-between gap-2">
                          <span className="truncate text-sm font-semibold text-slate-900">{provider.username}</span>
                          <StatusBadge label={provider.status} />
                        </span>
                        <span className="mt-1 block truncate text-xs text-slate-500">{provider.baseUrl}</span>
                      </button>
                    );
                  })}
                </div>
              )}

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => activeProvider && testMutation.mutate(activeProvider.id)}
                  disabled={!activeProvider || testMutation.isPending}
                  className="rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  测试
                </button>
                <button
                  type="button"
                  onClick={() => activeProvider && syncMutation.mutate(activeProvider.id)}
                  disabled={!activeProvider || syncMutation.isPending}
                  className="rounded-md border border-blue-200 bg-blue-50 px-3 py-2 text-xs font-semibold text-blue-700 hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  同步
                </button>
              </div>
            </div>
          </Section>

          <Section title="Bind Nextcloud">
            <form className="space-y-2 p-3" onSubmit={submitBind}>
              <input
                type="url"
                value={bindForm.baseUrl}
                onChange={event => setBindForm(current => ({ ...current, baseUrl: event.target.value }))}
                placeholder="https://cloud.example.com"
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
              />
              <input
                type="url"
                value={bindForm.internalBaseUrl}
                onChange={event => setBindForm(current => ({ ...current, internalBaseUrl: event.target.value }))}
                placeholder="Internal URL"
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
              />
              <input
                type="text"
                value={bindForm.username}
                onChange={event => setBindForm(current => ({ ...current, username: event.target.value }))}
                placeholder="Username"
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
              />
              <input
                type="password"
                value={bindForm.appPassword}
                onChange={event => setBindForm(current => ({ ...current, appPassword: event.target.value }))}
                placeholder="App password"
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
              />
              <button
                type="submit"
                disabled={bindMutation.isPending}
                className="w-full rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              >
                保存 Provider
              </button>
            </form>
          </Section>

          <Section
            title="Folder tree"
            actions={
              <button
                type="button"
                onClick={() => {
                  setCurrentPath(parentPath(currentPath));
                  setSelectedId(null);
                }}
                disabled={currentPath === '/'}
                className="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Up
              </button>
            }
          >
            <div className="max-h-[240px] overflow-auto p-2">
              <div className="space-y-1">
                {folderTreeRows.map(row => (
                  <button
                    key={row.id}
                    type="button"
                    onClick={() => {
                      setCurrentPath(row.path);
                      setSelectedId(row.item?.id ?? null);
                      clearSearch();
                    }}
                    className={`flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors ${
                      row.current ? 'bg-blue-50 font-semibold text-blue-700' : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    <span aria-hidden="true" style={{ width: `${row.depth * 14}px` }} className="shrink-0" />
                    <span className={`h-px w-3 shrink-0 ${row.depth === 0 ? 'bg-transparent' : 'bg-slate-200'}`} />
                    <span className="rounded border border-blue-100 bg-blue-50 px-1.5 py-0.5 text-[10px] font-semibold text-blue-700">
                      {row.item ? 'DIR' : row.current ? 'OPEN' : 'PATH'}
                    </span>
                    <span className="truncate">{row.label}</span>
                  </button>
                ))}
                {folderTreeRows.length === 1 && (
                  <p className="px-2 py-2 text-xs text-slate-500">No child folders here.</p>
                )}
              </div>
            </div>
          </Section>

          <Section title="Trash">
            <div className="max-h-[220px] overflow-auto p-2">
              {trashQuery.isLoading ? (
                <p className="px-2 py-3 text-sm text-slate-500">Loading trash...</p>
              ) : trashItems.length === 0 ? (
                <p className="px-2 py-3 text-sm text-slate-500">Trash is empty.</p>
              ) : (
                <div className="space-y-2">
                  {trashItems.slice(0, 8).map(item => (
                    <div key={`${item.trashId}-${item.deletedAt}`} className="rounded-md border border-slate-200 p-2">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-xs font-semibold text-slate-800">{item.name}</p>
                          <p className="mt-1 truncate text-[11px] text-slate-500">{item.originalLocation}</p>
                        </div>
                        <button
                          type="button"
                          onClick={() => restoreTrashMutation.mutate(item.trashId)}
                          disabled={!activeProvider || restoreTrashMutation.isPending}
                          className="rounded border border-emerald-200 bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          恢复
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Section>
        </div>

        <Section
          title="Files"
          actions={
            <>
              <input ref={fileInputRef} type="file" className="hidden" onChange={handleUploadChange} />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={!activeProvider || uploadMutation.isPending}
                className="rounded-md border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                上传
              </button>
            </>
          }
        >
          <div className="flex min-h-0 flex-col">
            <div className="space-y-3 border-b border-slate-200 p-3">
              <div className="flex flex-wrap items-center gap-1 text-sm text-slate-600">
                {breadcrumb(currentPath).map((crumb, index) => (
                  <span key={crumb.path} className="flex items-center gap-1">
                    {index > 0 && <span className="text-slate-300">/</span>}
                    <button
                      type="button"
                      onClick={() => {
                        setCurrentPath(crumb.path);
                        setSelectedId(null);
                        clearSearch();
                      }}
                      className="rounded px-1.5 py-0.5 font-medium text-slate-700 hover:bg-slate-100"
                    >
                      {crumb.label}
                    </button>
                  </span>
                ))}
              </div>

              <form className="flex flex-wrap gap-2" onSubmit={submitSearch}>
                <input
                  type="search"
                  value={searchText}
                  onChange={event => setSearchText(event.target.value)}
                  placeholder="Search files"
                  className="min-w-[220px] flex-1 rounded-md border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
                />
                <select
                  value={searchMode}
                  onChange={event => setSearchMode(event.target.value as FileSearchMode)}
                  className="rounded-md border border-slate-200 bg-white px-2 py-2 text-sm text-slate-700 outline-none focus:border-blue-300"
                >
                  <option value="hybrid">Hybrid</option>
                  <option value="keyword">Keyword</option>
                  <option value="semantic">Semantic</option>
                </select>
                <button type="submit" className="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-700">
                  搜索
                </button>
                {submittedSearch && (
                  <button type="button" onClick={clearSearch} className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50">
                    清除
                  </button>
                )}
              </form>
            </div>

            <div className="grid grid-cols-[minmax(220px,1fr)_110px_150px_110px] border-b border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold uppercase text-slate-500">
              <button type="button" onClick={() => toggleSort('name')} className="text-left">Name {sortKey === 'name' ? (sortDirection === 'asc' ? '↑' : '↓') : ''}</button>
              <button type="button" onClick={() => toggleSort('size')} className="text-right">Size {sortKey === 'size' ? (sortDirection === 'asc' ? '↑' : '↓') : ''}</button>
              <button type="button" onClick={() => toggleSort('modifiedAt')} className="text-right">Modified {sortKey === 'modifiedAt' ? (sortDirection === 'asc' ? '↑' : '↓') : ''}</button>
              <span className="text-right">Index</span>
            </div>

            <div className="min-h-[420px] overflow-auto">
              {(itemsQuery.isLoading || searchQuery.isLoading) ? (
                <p className="p-4 text-sm text-slate-500">Loading files...</p>
              ) : sortedItems.length === 0 ? (
                <p className="p-4 text-sm text-slate-500">{submittedSearch ? 'No search results.' : 'No files in this folder.'}</p>
              ) : (
                sortedItems.map(item => {
                  const active = item.id === selectedId;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onDoubleClick={() => item.itemType === 'folder' && openFolder(item)}
                      onClick={() => selectItem(item)}
                      className={`grid w-full grid-cols-[minmax(220px,1fr)_110px_150px_110px] items-center gap-3 border-b border-slate-100 px-3 py-2 text-left transition-colors ${
                        active ? 'bg-blue-50' : 'hover:bg-slate-50'
                      }`}
                    >
                      <span className="flex min-w-0 items-center gap-2">
                        <span className={`w-10 shrink-0 rounded border px-1.5 py-1 text-center text-[10px] font-semibold ${
                          item.itemType === 'folder' ? 'border-blue-100 bg-blue-50 text-blue-700' : 'border-slate-200 bg-white text-slate-500'
                        }`}>
                          {itemIcon(item)}
                        </span>
                        <span className="min-w-0">
                          <span className="block truncate text-sm font-medium text-slate-900">{item.name}</span>
                          <span className="block truncate text-xs text-slate-500">{item.path}</span>
                        </span>
                      </span>
                      <span className="text-right text-sm text-slate-600">{formatBytes(item.size)}</span>
                      <span className="text-right text-sm text-slate-600">{formatDateTime(item.modifiedAt)}</span>
                      <span className="flex justify-end"><StatusBadge label={item.indexStatus} /></span>
                    </button>
                  );
                })
              )}
            </div>

            {semanticHits.length > 0 && (
              <div className="border-t border-slate-200 bg-slate-50 p-3">
                <p className="text-xs font-semibold uppercase text-slate-500">Semantic hits</p>
                <div className="mt-2 space-y-2">
                  {semanticHits.slice(0, 4).map(hit => (
                    <button
                      key={hit.chunkId}
                      type="button"
                      onClick={() => setSelectedId(hit.fileItemId)}
                      className="block w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-left text-xs text-slate-600 hover:border-blue-200"
                    >
                      <span className="font-semibold text-slate-800">{Math.round(hit.score * 100)}%</span>
                      <span className="ml-2 line-clamp-2">{hit.text}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </Section>

        <Section title="Details">
          {!selectedId ? (
            <p className="p-4 text-sm text-slate-500">Select a file or folder.</p>
          ) : detailQuery.isLoading && !selected ? (
            <p className="p-4 text-sm text-slate-500">Loading details...</p>
          ) : !selected ? (
            <p className="p-4 text-sm text-slate-500">Selected item is unavailable.</p>
          ) : (
            <div className="flex max-h-full flex-col">
              <div className="space-y-3 border-b border-slate-200 p-3">
                <div className="min-w-0">
                  <p className="truncate text-base font-semibold text-slate-950">{selected.name}</p>
                  <p className="mt-1 break-all text-xs text-slate-500">{selected.path}</p>
                </div>

                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => selected.itemType === 'folder' ? openFolder(selected) : handleOpen(selected, 'view')}
                    disabled={busy}
                    className="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300"
                  >
                    View primary
                  </button>
                  {selected.itemType !== 'folder' && (
                    <>
                      <button
                        type="button"
                        onClick={() => handleOpen(selected, 'edit')}
                        disabled={busy}
                        className="rounded-md border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Edit separately
                      </button>
                      <button
                        type="button"
                        onClick={() => downloadMutation.mutate(selected)}
                        disabled={busy}
                        className="rounded-md border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Download
                      </button>
                    </>
                  )}
                  <button
                    type="button"
                    onClick={() => handleOpen(selected, 'nextcloud')}
                    disabled={busy}
                    className="rounded-md border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Open in Nextcloud
                  </button>
                </div>

                {isOoxmlFile(selected) && (
                  <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
                    OOXML edit opens the provider editor. Confirm version state before replacing shared documents.
                  </p>
                )}

                <dl className="grid grid-cols-2 gap-x-4 gap-y-2 border-y border-slate-100 py-3 text-xs">
                  <div className="min-w-0">
                    <dt className="text-slate-400">Type</dt>
                    <dd className="mt-1 truncate font-medium text-slate-800">{selected.itemType}</dd>
                  </div>
                  <div className="min-w-0">
                    <dt className="text-slate-400">Size</dt>
                    <dd className="mt-1 truncate font-medium text-slate-800">{formatBytes(selected.size)}</dd>
                  </div>
                  <div className="min-w-0">
                    <dt className="text-slate-400">Modified</dt>
                    <dd className="mt-1 truncate font-medium text-slate-800">{formatDateTime(selected.modifiedAt)}</dd>
                  </div>
                  <div className="min-w-0">
                    <dt className="text-slate-400">Synced</dt>
                    <dd className="mt-1 truncate font-medium text-slate-800">{formatDateTime(selected.syncedAt)}</dd>
                  </div>
                </dl>

                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => handleRename(selected)}
                    disabled={busy}
                    className="rounded-md border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Rename
                  </button>
                  <button
                    type="button"
                    onClick={() => handleMove(selected)}
                    disabled={busy}
                    className="rounded-md border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Move
                  </button>
                  {selected.itemType !== 'folder' && (
                    <button
                      type="button"
                      onClick={() => indexMutation.mutate(selected.id)}
                      disabled={busy}
                      className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Index
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => handleDelete(selected)}
                    disabled={busy}
                    className="rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Delete to trash
                  </button>
                </div>
              </div>

              <div className="min-h-0 flex-1 overflow-auto">
                <div className="space-y-3 border-b border-slate-200 p-3">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="text-xs font-semibold uppercase text-slate-500">AI</h3>
                    <StatusBadge label={selected.indexStatus} />
                  </div>
                  {selected.ai ? (
                    <div className="space-y-2">
                      <p className="text-sm text-slate-700">{selected.ai.summary}</p>
                      <div className="flex flex-wrap gap-1">
                        {selected.ai.tags.map(tag => (
                          <span key={tag} className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-600">{tag}</span>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-slate-500">No AI result for current version.</p>
                  )}
                </div>

                <div className="space-y-2 border-b border-slate-200 p-3">
                  <h3 className="text-xs font-semibold uppercase text-slate-500">Versions</h3>
                  {versionsQuery.isLoading ? (
                    <p className="text-sm text-slate-500">Loading versions...</p>
                  ) : versions.length === 0 ? (
                    <p className="text-sm text-slate-500">No versions.</p>
                  ) : (
                    versions.map(version => (
                      <div key={version.id} className="rounded-md border border-slate-200 p-2">
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className="text-xs font-semibold text-slate-800">{formatDateTime(version.modifiedAt)}</p>
                              {version.isCurrent && <StatusBadge label="current" />}
                            </div>
                            <p className="mt-1 text-xs text-slate-500">{formatBytes(version.size)} · {version.source}</p>
                          </div>
                          <div className="flex shrink-0 gap-1">
                            <button
                              type="button"
                              onClick={() => versionDownloadMutation.mutate({ item: selected, version })}
                              disabled={busy}
                              className="rounded border border-slate-200 px-2 py-1 text-[11px] font-semibold text-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              Down
                            </button>
                            {!version.isCurrent && (
                              <button
                                type="button"
                                onClick={() => restoreVersionMutation.mutate({ item: selected, version })}
                                disabled={busy}
                                className="rounded border border-blue-200 bg-blue-50 px-2 py-1 text-[11px] font-semibold text-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                Restore version
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>

                <div className="space-y-2 p-3">
                  <h3 className="text-xs font-semibold uppercase text-slate-500">Suggestions</h3>
                  {suggestionsQuery.isLoading ? (
                    <p className="text-sm text-slate-500">Loading suggestions...</p>
                  ) : selectedSuggestions.length === 0 ? (
                    <p className="text-sm text-slate-500">No suggestions for this item.</p>
                  ) : (
                    selectedSuggestions.map(suggestion => (
                      <div key={suggestion.id} className="rounded-md border border-slate-200 p-2">
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-slate-900">{suggestion.title}</p>
                            <p className="mt-1 text-xs text-slate-600">{suggestion.reason}</p>
                            <p className="mt-1 text-[11px] text-slate-400">
                              {suggestion.suggestionType} · {Math.round(suggestion.confidence * 100)}%
                            </p>
                          </div>
                          <StatusBadge label={suggestion.status} />
                        </div>
                        <div className="mt-2 flex flex-wrap gap-2">
                          <button
                            type="button"
                            onClick={() => suggestionMutation.mutate({ id: suggestion.id, action: 'accept' })}
                            disabled={busy}
                            className="rounded border border-emerald-200 bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            Accept - mark useful
                          </button>
                          <button
                            type="button"
                            onClick={() => suggestionMutation.mutate({ id: suggestion.id, action: 'dismiss' })}
                            disabled={busy}
                            className="rounded border border-slate-200 px-2 py-1 text-[11px] font-semibold text-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            Dismiss
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}
        </Section>
      </div>
    </div>
  );
}

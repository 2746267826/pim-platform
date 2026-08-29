import { apiDelete, apiDownloadBlob, apiGet, apiPost, apiUpload } from './client';
import type {
  ApiResponse,
  BindNextcloudProviderRequest,
  FileIndexJob,
  FileItem,
  FileListResponse,
  FileOpenLink,
  FileOpenLinkMode,
  FileProvider,
  FileProviderTest,
  FileSearchMode,
  FileSearchResult,
  FileSuggestion,
  FileTrashItem,
  FileVersion,
  MoveFileRequest,
  RenameFileRequest,
  VersionRestorePreview,
} from '../types';

export const fileApiPaths = {
  providers: () => '/files/providers',
  bindNextcloud: () => '/files/providers/nextcloud',
  providerTest: (id: string) => `/files/providers/${id}/test`,
  providerSync: (id: string) => `/files/providers/${id}/sync`,
  items: (path = '/', page?: number, pageSize?: number) => {
    const params: Record<string, string> = { path };
    if (page !== undefined) params.page = String(page);
    if (pageSize !== undefined) params.pageSize = String(pageSize);
    return `/files/items?${new URLSearchParams(params).toString()}`;
  },
  item: (id: string) => `/files/items/${id}`,
  upload: () => '/files/items/upload',
  itemDownload: (id: string) => `/files/items/${id}/download`,
  move: (id: string) => `/files/items/${id}/move`,
  rename: (id: string) => `/files/items/${id}/rename`,
  trash: () => '/files/trash',
  trashRestore: (providerId: string, trashId: string) => `/files/trash/${providerId}/restore?${new URLSearchParams({ trashId }).toString()}`,
  versions: (id: string) => `/files/items/${id}/versions`,
  versionDownload: (id: string, versionId: string) => `/files/items/${id}/versions/${versionId}/download`,
  versionRestorePreview: (id: string, versionId: string) => `/files/items/${id}/versions/${versionId}/restore-preview`,
  versionRestore: (id: string, versionId: string) => `/files/items/${id}/versions/${versionId}/restore`,
  index: (id: string) => `/files/items/${id}/index`,
  search: (q: string, mode: FileSearchMode) => `/files/search?${new URLSearchParams({ q, mode }).toString()}`,
  suggestions: () => '/files/suggestions',
  dismissSuggestion: (id: string) => `/files/suggestions/${id}/dismiss`,
  acceptSuggestion: (id: string) => `/files/suggestions/${id}/accept`,
  openLink: (id: string, mode: FileOpenLinkMode) => `/files/items/${id}/open-link?${new URLSearchParams({ mode }).toString()}`,
} as const;

export function getFileProviders() {
  return apiGet<ApiResponse<FileProvider[]>>(fileApiPaths.providers()).then(r => r.data);
}

export function bindNextcloudProvider(data: BindNextcloudProviderRequest) {
  return apiPost<ApiResponse<FileProvider>>(fileApiPaths.bindNextcloud(), data).then(r => r.data);
}

export function testFileProvider(id: string) {
  return apiPost<ApiResponse<FileProviderTest>>(fileApiPaths.providerTest(id), {}).then(r => r.data);
}

export function syncFileProvider(id: string) {
  return apiPost<ApiResponse<FileItem[]>>(fileApiPaths.providerSync(id), {}).then(r => r.data);
}

export function getFileItems(path = '/', page?: number, pageSize?: number) {
  return apiGet<ApiResponse<FileListResponse>>(fileApiPaths.items(path, page, pageSize)).then(r => r.data);
}

export function getFileItem(id: string) {
  return apiGet<ApiResponse<FileItem>>(fileApiPaths.item(id)).then(r => r.data);
}

export function uploadFile(providerId: string, path: string, file: File) {
  const formData = new FormData();
  formData.append('providerId', providerId);
  formData.append('path', path);
  formData.append('file', file);

  return apiUpload<ApiResponse<FileItem>>(fileApiPaths.upload(), formData).then(r => r.data);
}

export function downloadFileBlob(id: string): Promise<Blob> {
  return apiDownloadBlob(fileApiPaths.itemDownload(id));
}

export function moveFile(id: string, data: MoveFileRequest) {
  return apiPost<ApiResponse<FileItem>>(fileApiPaths.move(id), data).then(r => r.data);
}

export function renameFile(id: string, data: RenameFileRequest) {
  return apiPost<ApiResponse<FileItem>>(fileApiPaths.rename(id), data).then(r => r.data);
}

export function deleteFile(id: string) {
  return apiDelete<ApiResponse<string>>(fileApiPaths.item(id)).then(r => r.data);
}

export function getFileTrash() {
  return apiGet<ApiResponse<FileTrashItem[]>>(fileApiPaths.trash()).then(r => r.data);
}

export function restoreFileTrash(providerId: string, trashId: string) {
  return apiPost<ApiResponse<string>>(fileApiPaths.trashRestore(providerId, trashId), {}).then(r => r.data);
}

export function getFileVersions(id: string) {
  return apiGet<ApiResponse<FileVersion[]>>(fileApiPaths.versions(id)).then(r => r.data);
}

export function downloadFileVersionBlob(id: string, versionId: string): Promise<Blob> {
  return apiDownloadBlob(fileApiPaths.versionDownload(id, versionId));
}

export function restoreFileVersionPreview(id: string, versionId: string) {
  return apiPost<ApiResponse<VersionRestorePreview>>(fileApiPaths.versionRestorePreview(id, versionId), {}).then(r => r.data);
}

export function restoreFileVersion(id: string, versionId: string) {
  return apiPost<ApiResponse<string>>(fileApiPaths.versionRestore(id, versionId), {}).then(r => r.data);
}

export function indexFile(id: string) {
  return apiPost<ApiResponse<FileIndexJob>>(fileApiPaths.index(id), {}).then(r => r.data);
}

export function searchFiles(q: string, mode: FileSearchMode) {
  return apiGet<ApiResponse<FileSearchResult>>(fileApiPaths.search(q, mode)).then(r => r.data);
}

export function getFileSuggestions() {
  return apiGet<ApiResponse<FileSuggestion[]>>(fileApiPaths.suggestions()).then(r => r.data);
}

export function dismissFileSuggestion(id: string) {
  return apiPost<ApiResponse<FileSuggestion>>(fileApiPaths.dismissSuggestion(id), {}).then(r => r.data);
}

export function acceptFileSuggestion(id: string) {
  return apiPost<ApiResponse<FileSuggestion>>(fileApiPaths.acceptSuggestion(id), {}).then(r => r.data);
}

export function getFileOpenLink(id: string, mode: FileOpenLinkMode) {
  return apiGet<ApiResponse<FileOpenLink>>(fileApiPaths.openLink(id, mode)).then(r => r.data);
}

import type { FileItem } from '../../types';
import { getFileItems, type FileListParams } from '../../api/files';
import { FILE_PAGE_SIZE } from './fileBrowserState';

/** 树每个目录最多翻多少页目录（100/页）；超出时由调用方显式提示「已加载 X / 共 N」。 */
export const MAX_FOLDER_PAGES = 20;

export interface FolderListing {
  items: FileItem[];
  totalCount: number;
  /** 是否因页数上限而只加载了部分目录（AC-3.2：截断必须显式暴露）。 */
  truncated: boolean;
}

export type GetFileItems = (params: FileListParams) => Promise<{ result: { items: FileItem[]; totalCount: number; totalPages: number } }>;

/**
 * 取回某目录的**全部子目录**（REQ-4）。
 *
 * 树只承载目录：中栏列表已经分页承载全部条目，树若也镜像整份内容，在大目录上既慢、
 * 又会与列表的分页口径打架。这里按类型过滤 + 逐页取回，并在触到页数上限时如实报告截断。
 */
export async function loadFolderTree(path: string, getItems: GetFileItems = getFileItems): Promise<FolderListing> {
  const first = await getItems({ path, page: 1, pageSize: FILE_PAGE_SIZE, type: 'folder', sort: 'name', order: 'asc' });
  const items = [...first.result.items];
  let page = 1;
  while (page < first.result.totalPages && page < MAX_FOLDER_PAGES) {
    page += 1;
    const next = await getItems({ path, page, pageSize: FILE_PAGE_SIZE, type: 'folder', sort: 'name', order: 'asc' });
    items.push(...next.result.items);
  }
  return {
    items,
    totalCount: first.result.totalCount,
    truncated: items.length < first.result.totalCount,
  };
}

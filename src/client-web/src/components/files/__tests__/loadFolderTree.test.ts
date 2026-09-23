import { describe, expect, it, vi } from 'vitest';
import type { FileItem } from '../../../types';
import { loadFolderTree } from '../loadFolderTree';

/**
 * REQ-4：树的数据源。树只承载**目录**，并如实报告是否因上限被截断。
 *
 * 旧实现把整棵子树拉回内存再筛，单目录 8465 项时列表被静默截断到 2000 项（现状-2）；
 * 这里锁定「目录数据来自服务端的类型过滤 + 分页」以及「截断必须显式暴露」（AC-3.2 / AC-4.2）。
 */
function folder(id: string, path: string, name: string): FileItem {
  return {
    id,
    providerId: 'p-1',
    externalFileId: `ext-${id}`,
    parentExternalFileId: null,
    path,
    name,
    itemType: 'folder',
    mimeType: null,
    size: null,
    etag: null,
    contentHash: null,
    currentVersionId: null,
    permissions: null,
    isDeleted: false,
    deletedAt: null,
    lastSeenAt: null,
    createdAt: '2026-09-01T00:00:00Z',
    modifiedAt: '2026-09-01T00:00:00Z',
    syncedAt: '2026-09-01T00:00:00Z',
    indexStatus: 'not_indexed',
    ai: null,
  };
}

function page(items: FileItem[], totalCount: number, totalPages: number) {
  return { result: { items, page: 1, pageSize: 100, totalCount, totalPages } };
}

describe('loadFolderTree', () => {
  it('只请求目录（type=folder），并按 100/页 逐页取全', async () => {
    const first = Array.from({ length: 100 }, (_, i) => folder(`a${i}`, `/x/a${i}`, `a${i}`));
    const second = [folder('z', '/x/z', 'z')];
    const getItems = vi
      .fn()
      .mockResolvedValueOnce(page(first, 101, 2))
      .mockResolvedValueOnce(page(second, 101, 2));

    const listing = await loadFolderTree('/x', getItems);

    expect(getItems).toHaveBeenNthCalledWith(1, expect.objectContaining({ path: '/x', type: 'folder', page: 1 }));
    expect(getItems).toHaveBeenNthCalledWith(2, expect.objectContaining({ path: '/x', type: 'folder', page: 2 }));
    expect(listing.items).toHaveLength(101);
    expect(listing.totalCount).toBe(101);
    expect(listing.truncated).toBe(false);
  });

  it('AC-3.2 超过上限时如实标记截断，不假装已加载全部', async () => {
    // 上限 20 页 = 2000 个目录；造 5000 个 -> 必须 truncated=true 且给出真实总数
    const getItems = vi.fn(async ({ page: pageNumber }: { page?: number }) =>
      page(
        Array.from({ length: 100 }, (_, i) => folder(`f${pageNumber}-${i}`, `/big/f${pageNumber}-${i}`, `f${i}`)),
        5000,
        50,
      ),
    );

    const listing = await loadFolderTree('/big', getItems);

    expect(listing.items).toHaveLength(2000);
    expect(listing.totalCount).toBe(5000);
    expect(listing.truncated).toBe(true);
    expect(getItems).toHaveBeenCalledTimes(20);
  });

  it('AC-4.2 单页目录不再多发请求', async () => {
    const getItems = vi.fn().mockResolvedValueOnce(page([folder('a', '/x/a', 'a')], 1, 1));

    const listing = await loadFolderTree('/x', getItems);

    expect(getItems).toHaveBeenCalledTimes(1);
    expect(listing.truncated).toBe(false);
  });

  it('空目录返回 0 项且不报错（AC-4.2 反面）', async () => {
    const getItems = vi.fn().mockResolvedValueOnce(page([], 0, 0));

    const listing = await loadFolderTree('/empty', getItems);

    expect(listing.items).toEqual([]);
    expect(listing.truncated).toBe(false);
  });
});

import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import OneDriveFileList, { typeBadge } from '../OneDriveFileList';
import type { FileItem } from '../../../types';

function makeItem(overrides: Partial<FileItem>): FileItem {
  return {
    id: 'id-1',
    providerId: 'p-1',
    externalFileId: 'ext-1',
    parentExternalFileId: null,
    path: '/a.txt',
    name: 'a.txt',
    itemType: 'file',
    mimeType: 'text/plain',
    size: 2048,
    etag: null,
    contentHash: null,
    currentVersionId: null,
    permissions: null,
    isDeleted: false,
    deletedAt: null,
    lastSeenAt: null,
    createdAt: '2026-09-01T00:00:00Z',
    modifiedAt: '2026-09-02T00:00:00Z',
    syncedAt: '2026-09-02T00:00:00Z',
    indexStatus: '',
    ai: null,
    ...overrides,
  };
}

const folder = makeItem({ id: 'f-1', path: '/合同', name: '合同', itemType: 'folder', mimeType: null, size: null });
const pdf = makeItem({ id: 'f-2', path: '/合同/租赁.pdf', name: '租赁.pdf', mimeType: 'application/pdf', size: 1024 * 512 });
const note = makeItem({ id: 'f-3', path: '/笔记.md', name: '笔记.md', mimeType: 'text/markdown' });

describe('OneDriveFileList', () => {
  it('列表视图渲染行与类型徽章', () => {
    render(
      <OneDriveFileList
        items={[folder, pdf]}
        breadcrumb="OneDrive / "
        searchQuery=""
        onSearchChange={() => {}}
        view="list"
        onViewChange={() => {}}
        selectedItem={null}
        onSelect={() => {}}
        onOpenFolder={() => {}}
      />,
    );
    expect(screen.getByText('合同')).toBeTruthy();
    expect(screen.getByText('租赁.pdf')).toBeTruthy();
    expect(screen.getByText('DIR')).toBeTruthy();
    expect(screen.getByText('PDF')).toBeTruthy();
    expect(screen.getByText('512.0 KB')).toBeTruthy();
  });

  it('搜索过滤当前文件夹', () => {
    render(
      <OneDriveFileList
        items={[folder, pdf, note]}
        breadcrumb="OneDrive / "
        searchQuery="笔记"
        onSearchChange={() => {}}
        view="list"
        onViewChange={() => {}}
        selectedItem={null}
        onSelect={() => {}}
        onOpenFolder={() => {}}
      />,
    );
    expect(screen.getByText('笔记.md')).toBeTruthy();
    expect(screen.queryByText('租赁.pdf')).toBeNull();
    expect(screen.getByText('1 项')).toBeTruthy();
  });

  it('点击文件夹走 onOpenFolder，点击文件走 onSelect', () => {
    const onOpenFolder = vi.fn();
    const onSelect = vi.fn();
    render(
      <OneDriveFileList
        items={[folder, pdf]}
        breadcrumb="OneDrive / "
        searchQuery=""
        onSearchChange={() => {}}
        view="list"
        onViewChange={() => {}}
        selectedItem={null}
        onSelect={onSelect}
        onOpenFolder={onOpenFolder}
      />,
    );
    fireEvent.click(screen.getByText('合同'));
    expect(onOpenFolder).toHaveBeenCalledWith('/合同');
    fireEvent.click(screen.getByText('租赁.pdf'));
    expect(onSelect).toHaveBeenCalledWith(pdf);
  });

  it('网格视图渲染卡片并支持切换', () => {
    const onViewChange = vi.fn();
    render(
      <OneDriveFileList
        items={[pdf]}
        breadcrumb="OneDrive / "
        searchQuery=""
        onSearchChange={() => {}}
        view="grid"
        onViewChange={onViewChange}
        selectedItem={null}
        onSelect={() => {}}
        onOpenFolder={() => {}}
      />,
    );
    expect(screen.getByTestId('file-card')).toBeTruthy();
    fireEvent.click(screen.getByLabelText('列表视图'));
    expect(onViewChange).toHaveBeenCalledWith('list');
  });

  it('typeBadge 按类型分类', () => {
    expect(typeBadge(folder).label).toBe('DIR');
    expect(typeBadge(pdf).label).toBe('PDF');
    expect(typeBadge(makeItem({ mimeType: 'image/png', name: 'p.png' })).label).toBe('IMG');
    expect(typeBadge(makeItem({ mimeType: null, name: 'a.xlsx' })).label).toBe('XLS');
  });
});

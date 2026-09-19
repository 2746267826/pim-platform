import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import OneDrivePreviewPane, { isOfficeDocument, isTextEditable } from '../OneDrivePreviewPane';
import * as filesApi from '../../../api/files';
import type { FileItem } from '../../../types';

vi.mock('../../../api/files', async importOriginal => {
  const actual = await importOriginal<typeof import('../../../api/files')>();
  return {
    ...actual,
    getOneDriveText: vi.fn(),
    saveOneDriveText: vi.fn(),
    getOneDriveSnapshots: vi.fn(),
    restoreOneDriveSnapshot: vi.fn(),
    getOneDrivePreviewUrl: vi.fn(),
  };
});

const mockedText = vi.mocked(filesApi.getOneDriveText);
const mockedSave = vi.mocked(filesApi.saveOneDriveText);
const mockedSnapshots = vi.mocked(filesApi.getOneDriveSnapshots);
const mockedRestore = vi.mocked(filesApi.restoreOneDriveSnapshot);
const mockedPreviewUrl = vi.mocked(filesApi.getOneDrivePreviewUrl);

function makeItem(overrides: Partial<FileItem>): FileItem {
  return {
    id: 'item-1',
    providerId: 'p-1',
    externalFileId: 'ext-1',
    parentExternalFileId: null,
    path: '/a.txt',
    name: 'a.txt',
    itemType: 'file',
    mimeType: 'text/plain',
    size: 12,
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

describe('类型分类', () => {
  it('isTextEditable 判定文本可编辑', () => {
    expect(isTextEditable(makeItem({ mimeType: 'text/plain' }))).toBe(true);
    expect(isTextEditable(makeItem({ mimeType: null, name: 'a.md' }))).toBe(true);
    expect(isTextEditable(makeItem({ mimeType: 'application/pdf', name: 'a.pdf' }))).toBe(false);
  });

  it('isOfficeDocument 判定 Office 文档', () => {
    expect(isOfficeDocument(makeItem({ mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', name: 'a.docx' }))).toBe(true);
    expect(isOfficeDocument(makeItem({ mimeType: null, name: 'b.xlsx' }))).toBe(true);
    expect(isOfficeDocument(makeItem({ mimeType: 'text/plain', name: 'c.txt' }))).toBe(false);
  });
});

describe('OneDrivePreviewPane', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedSnapshots.mockResolvedValue([]);
  });

  it('未选中时显示空态', () => {
    render(<OneDrivePreviewPane item={null} />);
    expect(screen.getByText('选择一个文件查看预览')).toBeTruthy();
  });

  it('文本文件可编辑、保存并展示快照恢复', async () => {
    mockedText.mockResolvedValue({ content: '旧内容', mimeType: 'text/plain', size: 9, truncated: false });
    mockedSave.mockResolvedValue('');
    mockedSnapshots
      .mockResolvedValueOnce([{ id: 's1', path: '/a.txt', name: 'a.txt', content: '更早版本', byteSize: 12, reason: 'pre-edit', createdAt: '2026-09-01T00:00:00Z' }])
      .mockResolvedValue([{ id: 's2', path: '/a.txt', name: 'a.txt', content: '旧内容', byteSize: 9, reason: 'pre-edit', createdAt: '2026-09-02T00:00:00Z' }]);
    mockedRestore.mockResolvedValue('');
    mockedText.mockResolvedValueOnce({ content: '旧内容', mimeType: 'text/plain', size: 9, truncated: false });

    render(<OneDrivePreviewPane item={makeItem()} onToast={() => {}} />);
    expect(await screen.findByText('a.txt')).toBeTruthy();

    fireEvent.click(screen.getByText('编辑文本'));
    const editor = await screen.findByTestId('text-editor');
    expect(editor).toBeTruthy();

    const textarea = screen.getByLabelText('文本内容') as HTMLTextAreaElement;
    await waitFor(() => expect(textarea.value).toBe('旧内容'));
    fireEvent.change(textarea, { target: { value: '新内容' } });

    mockedText.mockResolvedValue({ content: '新内容', mimeType: 'text/plain', size: 9, truncated: false });
    fireEvent.click(screen.getByText('保存到 OneDrive'));
    await waitFor(() => expect(mockedSave).toHaveBeenCalledWith('item-1', '新内容'));

    // 快照列表展示并恢复
    expect(await screen.findByText(/旧内容/)).toBeTruthy();
    fireEvent.click(screen.getByText('恢复'));
    await waitFor(() => expect(mockedRestore).toHaveBeenCalledWith('item-1', 's2'));
  });

  it('Office 文档拉取预览地址并渲染 iframe', async () => {
    mockedPreviewUrl.mockResolvedValue('https://preview.example.com/embed');
    render(
      <OneDrivePreviewPane
        item={makeItem({ name: '报告.docx', mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })}
      />,
    );
    await waitFor(() => expect(mockedPreviewUrl).toHaveBeenCalledWith('item-1'));
    expect(await screen.findByTestId('office-preview')).toBeTruthy();
  });
});

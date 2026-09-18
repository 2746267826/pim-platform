/**
 * #300 回归护栏：全局「快速记录」入口与「快速记录」页行为必须一致。
 *
 * 现象（#280 落地后）：同一个黑色「+」按钮存在两种行为 ——
 *   - 除「快速记录」页外的所有页面：点击 → **直接弹出旧编辑面板**（无菜单）；
 *   - 「快速记录」页：点击 → 菜单（写闪念 / 建任务 / 排日程）→「写闪念」打开新编辑卡片。
 *
 * 需求（已与用户确认，见 docs/evidence/20260917/prototypes/quicknote-entry.html）：
 *   1. 所有页面「+」行为统一为与「快速记录」页一致：点击 → 菜单；
 *      写闪念 → 新版编辑卡片（可拖动、全量功能）；建任务 / 排日程 → 跳转对应页面；
 *   2. 旧样式编辑面板不再出现；
 *   3. 每个页面仍只有一个入口（/quick-notes 页不渲染全局入口）；
 *   4. 不造成功能缺项：草稿保留、位置记忆继续生效。
 *
 * 用真实渲染 + 真实点击验证交互，而不是只看源码字符串 —— 源码断言挡不住
 * 「按钮点下去弹的是旧面板」这类行为回归。
 */
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, Fragment, type ReactNode } from 'react';

import { QuickNoteFloatingEntry } from '../QuickNoteFloatingEntry';
import {
  clearQuickNoteDraft,
  loadQuickNoteDraft,
  saveQuickNoteDraft,
} from '../quickNoteFloatingState';

// 编辑卡片包含 Markdown 编辑器（体积大、懒加载），渲染它需要 API 客户端可用。
vi.mock('../../../api/quickNotes', () => ({
  createQuickNote: vi.fn().mockResolvedValue({ id: 'note-1' }),
  getQuickNote: vi.fn().mockResolvedValue(null),
  updateQuickNote: vi.fn().mockResolvedValue({ id: 'note-1' }),
  archiveQuickNote: vi.fn().mockResolvedValue(undefined),
  restoreQuickNote: vi.fn().mockResolvedValue(undefined),
  deleteQuickNote: vi.fn().mockResolvedValue(undefined),
  processQuickNote: vi.fn().mockResolvedValue(undefined),
  uploadQuickNoteAttachment: vi.fn().mockResolvedValue({ id: 'att-1' }),
  getQuickNotes: vi.fn().mockResolvedValue({ items: [], total: 0 }),
}));

afterEach(() => cleanup());

/** 用 MemoryRouter 提供真实的路由上下文（组件内的菜单用 useNavigate 跳转）。 */
/** 编辑卡片内部使用 react-query，必须提供 QueryClientProvider。 */
function withProviders(node: ReactNode, initialPath: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return createElement(
    QueryClientProvider,
    { client: queryClient },
    createElement(MemoryRouter, { initialEntries: [initialPath] }, node),
  );
}

function renderEntry(pathname: string) {
  return render(withProviders(createElement(QuickNoteFloatingEntry, { pathname }), pathname));
}

/** 打开菜单并等待菜单项渲染出来。 */
async function openMenu() {
  fireEvent.click(screen.getByLabelText('打开快速记录'));
  await waitFor(() => expect(screen.getByText('写闪念')).toBeTruthy());
}

describe('#300 全局快速记录入口与「快速记录」页行为一致', () => {
  it('点击「+」先弹出菜单（写闪念 / 建任务 / 排日程），而不是直接打开编辑面板', async () => {
    renderEntry('/today');

    expect(screen.queryByText('写闪念')).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();

    await openMenu();

    expect(screen.getByText('建任务')).toBeTruthy();
    expect(screen.getByText('排日程')).toBeTruthy();
    // #300 的旧行为：点「+」直接弹编辑面板。现在必须只弹菜单。
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  // 编辑卡片懒加载 + MDX 编辑器挂载较重，给足超时（首次 transform 可达数秒）。
  it('菜单「写闪念」打开新版编辑卡片（含分类 / 附件 / Markdown 全量功能）', async () => {
    renderEntry('/today');
    await openMenu();

    fireEvent.click(screen.getByText('写闪念'));

    await waitFor(() => expect(screen.getByRole('dialog')).toBeTruthy(), { timeout: 20000 });

    const dialogText = screen.getByRole('dialog').textContent ?? '';
    for (const marker of ['记录分类：', '附件', '支持 Markdown 语法']) {
      expect(dialogText).toContain(marker);
    }
  });

  it('旧样式编辑面板（QuickNoteGlobalPanel）不再出现', async () => {
    renderEntry('/today');
    await openMenu();

    // 旧面板的 aria-label 是「快速记录」，且没有分类 / 附件等新版功能。
    expect(screen.queryByLabelText('快速记录')).toBeNull();
  });

  it('菜单「建任务」跳转任务页，「排日程」跳转日历页', async () => {
    function Probe() {
      return createElement('div', { 'data-testid': 'probe' }, useLocation().pathname);
    }

    for (const [menuItem, expectedPath] of [
      ['建任务', '/tasks'],
      ['排日程', '/calendar'],
    ] as const) {
      cleanup();
      render(
        withProviders(
          createElement(
            Fragment,
            null,
            createElement(QuickNoteFloatingEntry, { pathname: '/today' }),
            createElement(Probe, null),
          ),
          '/today',
        ),
      );

      await openMenu();
      fireEvent.click(screen.getByText(menuItem));
      await waitFor(() => expect(screen.getByTestId('probe').textContent).toBe(expectedPath));
    }
  });

  it('每页仍只有一个入口：/quick-notes 页不渲染全局入口', () => {
    const onQuickNotes = renderEntry('/quick-notes');
    expect(
      onQuickNotes.container.querySelectorAll('[aria-label="打开快速记录"]').length,
    ).toBe(0);

    cleanup();
    const onToday = renderEntry('/today');
    expect(onToday.container.querySelectorAll('[aria-label="打开快速记录"]').length).toBe(1);
  });

  it('迁移不造成功能缺项：位置记忆仍在，且全局入口复用新编辑卡片', () => {
    // 相对本测试文件定位，避免依赖运行时的 cwd（tsx / vitest 的 cwd 不同）。
    const dir = path.dirname(new URL(import.meta.url).pathname);
    const read = (file: string) => readFileSync(path.join(dir, '..', file), 'utf8');

    const dialog = read('QuickNoteDialog.tsx');
    expect(dialog).toContain('loadPanelPosition');
    expect(dialog).toContain('savePanelPosition');

    // 全局入口复用「快速记录」页同款编辑卡片（而不是旧面板）。
    expect(read('QuickNoteFloatingEntry.tsx')).toContain('QuickNoteDialog');
  });

  // #300 需求 4「不造成功能缺项」：草稿必须真正保留。
  // 仅断言常量存在是不够的（review 发现旧面板删除后草稿能力实际丢失，
  // 而字符串断言仍通过 —— 假阳性）。这里验证真实恢复路径。
  it('重新打开卡片时恢复上次未保存的草稿（草稿能力真实生效）', async () => {
    const draft = '睡前想到的一个点子';
    localStorage.clear();
    localStorage.setItem('pim.quickNotes.floatingDraft', draft);

    renderEntry('/today');
    await openMenu();
    fireEvent.click(screen.getByText('写闪念'));
    await waitFor(() => expect(screen.getByRole('dialog')).toBeTruthy(), { timeout: 20000 });

    // 草稿内容必须出现在编辑卡片里（旧 QuickNoteGlobalPanel 有该行为，统一入口后不得丢失）。
    await waitFor(() => {
      expect(screen.getByRole('dialog').textContent ?? '').toContain(draft);
    }, { timeout: 20000 });

    localStorage.clear();
  });

  it('草稿读写工具按约定工作：空内容清除草稿，提交后草稿不再恢复', () => {
    localStorage.clear();
    saveQuickNoteDraft('abc');
    expect(loadQuickNoteDraft()).toBe('abc');
    // 空内容 -> 清除键（避免用空草稿覆盖已有内容）
    saveQuickNoteDraft('');
    expect(loadQuickNoteDraft()).toBe('');
    saveQuickNoteDraft('def');
    clearQuickNoteDraft();
    expect(loadQuickNoteDraft()).toBe('');
    expect(localStorage.getItem('pim.quickNotes.floatingDraft')).toBeNull();
  });

  it('进入 /quick-notes 时收起全局菜单，离开后不会残留为打开状态', async () => {
    localStorage.clear();
    const { rerender } = render(
      withProviders(createElement(QuickNoteFloatingEntry, { pathname: '/today' }), '/today'),
    );
    await openMenu();

    // 切到排除路径：组件返回 null，但状态必须被重置（review 发现）。
    rerender(withProviders(createElement(QuickNoteFloatingEntry, { pathname: '/quick-notes' }), '/quick-notes'));
    expect(screen.queryByText('写闪念')).toBeNull();

    // 再切回来：菜单不应「自己弹开」。
    rerender(withProviders(createElement(QuickNoteFloatingEntry, { pathname: '/today' }), '/today'));
    expect(screen.queryByText('写闪念')).toBeNull();
  });

  it('菜单支持 Escape 关闭，并带有正确的菜单语义', async () => {
    renderEntry('/today');
    await openMenu();

    // 语义：容器是 menu，各项是 menuitem。
    expect(screen.getByRole('menu')).toBeTruthy();
    expect(screen.getAllByRole('menuitem').length).toBe(3);

    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByText('写闪念')).toBeNull());
  });
}, 30000);

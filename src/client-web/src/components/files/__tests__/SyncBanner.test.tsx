import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import SyncBanner from '../SyncBanner';
import type { OneDriveSyncStatus } from '../../../types';

/**
 * REQ-25：同步状态横幅。AC-25.1「点击立即反馈已开始、不阻塞」、
 * AC-25.2「失败显示可读原因、恢复后自动消失」、AC-25.3「不得静默失败」。
 */
function status(overrides: Partial<OneDriveSyncStatus> = {}): OneDriveSyncStatus {
  return {
    syncStatus: 'idle',
    lastError: null,
    lastSyncAt: null,
    syncedItemCount: 0,
    ...overrides,
  };
}

describe('SyncBanner / REQ-25', () => {
  it('未同步时显示「尚未同步」与可点的立即同步', () => {
    render(<SyncBanner status={status()} onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner-text').textContent).toBe('尚未同步');
    expect(screen.getByTestId('sync-now-button').hasAttribute('disabled')).toBe(false);
  });

  it('AC-25.1 点击立即同步会回调（调用方负责立即反馈，不阻塞）', () => {
    const onSync = vi.fn();
    render(<SyncBanner status={status()} onSync={onSync} />);

    fireEvent.click(screen.getByTestId('sync-now-button'));

    expect(onSync).toHaveBeenCalledTimes(1);
  });

  it('AC-25.1 已开始但未完成时立即显示「已开始同步」，按钮禁用避免重复触发', () => {
    render(<SyncBanner status={status()} starting onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner-text').textContent).toContain('已开始同步');
    expect(screen.getByTestId('sync-now-button').hasAttribute('disabled')).toBe(true);
    expect(screen.getByTestId('sync-banner').getAttribute('data-sync-state')).toBe('syncing');
  });

  it('同步中显示进度量（已处理 X 项）', () => {
    render(<SyncBanner status={status({ syncStatus: 'syncing', syncedItemCount: 42 })} onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner-text').textContent).toContain('42');
    expect(screen.getByTestId('sync-banner').getAttribute('data-sync-state')).toBe('syncing');
  });

  it('AC-25.2 同步失败显示可读原因（来自 lastError）', () => {
    render(<SyncBanner status={status({ syncStatus: 'error', lastError: '凭据已过期，请重新授权' })} onSync={vi.fn()} />);

    const banner = screen.getByTestId('sync-banner');
    expect(banner.getAttribute('data-sync-state')).toBe('error');
    expect(banner.getAttribute('role')).toBe('alert');
    expect(screen.getByTestId('sync-banner-text').textContent).toBe('凭据已过期，请重新授权');
  });

  it('AC-25.3 反面：失败但无原因时也要给可读文案，不得空白/静默', () => {
    render(<SyncBanner status={status({ syncStatus: 'error', lastError: null })} onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner-text').textContent?.trim()).not.toBe('');
  });

  it('AC-25.2 恢复后（status 回到 idle）横幅自动回到正常态', () => {
    const { rerender } = render(
      <SyncBanner status={status({ syncStatus: 'error', lastError: '网络中断' })} onSync={vi.fn()} />,
    );
    expect(screen.getByTestId('sync-banner').getAttribute('data-sync-state')).toBe('error');

    rerender(<SyncBanner status={status({ lastSyncAt: '2026-09-24T10:00:00Z', syncedItemCount: 120 })} onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner').getAttribute('data-sync-state')).toBe('idle');
    expect(screen.getByTestId('sync-banner-text').textContent).toContain('120');
  });

  it('已同步时显示条目数与时间', () => {
    render(<SyncBanner status={status({ lastSyncAt: '2026-09-24T10:00:00Z', syncedItemCount: 7 })} onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner-text').textContent).toContain('已同步 7 项');
  });

  it('未绑定（status 为 null）时仍可渲染且不崩', () => {
    render(<SyncBanner status={null} onSync={vi.fn()} />);

    expect(screen.getByTestId('sync-banner-text').textContent).toBe('尚未同步');
  });

  it('外部错误优先展示（例如入队请求本身失败）', () => {
    render(<SyncBanner status={status()} onSync={vi.fn()} error="同步请求失败：文件来源不存在" />);

    expect(screen.getByTestId('sync-banner-text').textContent).toBe('同步请求失败：文件来源不存在');
    expect(screen.getByTestId('sync-banner').getAttribute('data-sync-state')).toBe('error');
  });
});

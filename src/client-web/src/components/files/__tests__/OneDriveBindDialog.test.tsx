import { describe, expect, it, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import OneDriveBindDialog from '../OneDriveBindDialog';
import * as filesApi from '../../../api/files';

vi.mock('../../../api/files', () => ({
  startOneDriveBinding: vi.fn(),
  getOneDriveBindingStatus: vi.fn(),
}));

const mockedStart = vi.mocked(filesApi.startOneDriveBinding);
const mockedStatus = vi.mocked(filesApi.getOneDriveBindingStatus);

describe('OneDriveBindDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('未填 Client ID 时提示错误而不发请求', async () => {
    render(<OneDriveBindDialog onClose={() => {}} onConnected={() => {}} />);
    fireEvent.click(screen.getByText('获取设备码'));
    expect(await screen.findByText('请填写 Azure 应用注册的 Client ID')).toBeTruthy();
    expect(mockedStart).not.toHaveBeenCalled();
  });

  it('提交后显示设备码与验证链接', async () => {
    mockedStart.mockResolvedValue({
      providerId: 'p-1',
      userCode: 'ABCD-1234',
      verificationUri: 'https://www.microsoft.com/link',
      expiresIn: 900,
    });
    render(<OneDriveBindDialog onClose={() => {}} onConnected={() => {}} />);
    fireEvent.change(screen.getByLabelText('Azure 应用注册的 Client ID（与 Outlook 同步使用的应用一致即可）'), {
      target: { value: 'cid-1' },
    });
    fireEvent.click(screen.getByText('获取设备码'));

    expect(await screen.findByText('ABCD-1234')).toBeTruthy();
    expect(mockedStart).toHaveBeenCalledWith('cid-1');
    expect(screen.getByText('https://www.microsoft.com/link')).toBeTruthy();
  });

  it('轮询到 connected 时回调 onConnected', async () => {
    vi.useFakeTimers();
    mockedStart.mockResolvedValue({
      providerId: 'p-1',
      userCode: 'ABCD-1234',
      verificationUri: 'https://www.microsoft.com/link',
      expiresIn: 900,
    });
    mockedStatus
      .mockResolvedValueOnce({
        status: 'pending', driveId: null, accountId: null, accountName: null,
        userCode: null, verificationUri: null, deviceCodeExpiresAt: null,
      })
      .mockResolvedValue({
        status: 'connected', driveId: 'd1', accountId: 'a1', accountName: 'User',
        userCode: null, verificationUri: null, deviceCodeExpiresAt: null,
      });
    const onConnected = vi.fn();
    render(<OneDriveBindDialog onClose={() => {}} onConnected={onConnected} />);

    fireEvent.change(screen.getByLabelText('Azure 应用注册的 Client ID（与 Outlook 同步使用的应用一致即可）'), {
      target: { value: 'cid-1' },
    });
    fireEvent.click(screen.getByText('获取设备码'));
    await vi.advanceTimersByTimeAsync(100);
    await vi.advanceTimersByTimeAsync(2100);
    await vi.advanceTimersByTimeAsync(2100);
    await vi.advanceTimersByTimeAsync(2100);

    expect(mockedStatus).toHaveBeenCalled();
    expect(onConnected).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });

  it('denied 状态展示失败并可重新绑定', async () => {
    mockedStart.mockResolvedValue({
      providerId: 'p-1',
      userCode: 'ABCD-1234',
      verificationUri: 'https://www.microsoft.com/link',
      expiresIn: 900,
    });
    mockedStatus.mockResolvedValue({
      status: 'denied', driveId: null, accountId: null, accountName: null,
      userCode: null, verificationUri: null, deviceCodeExpiresAt: null,
    });
    render(<OneDriveBindDialog onClose={() => {}} onConnected={() => {}} />);
    fireEvent.change(screen.getByLabelText('Azure 应用注册的 Client ID（与 Outlook 同步使用的应用一致即可）'), {
      target: { value: 'cid-1' },
    });
    fireEvent.click(screen.getByText('获取设备码'));
    expect(await screen.findByText('授权被拒绝')).toBeTruthy();
    fireEvent.click(screen.getByText('重新绑定'));
    expect(screen.getByText('获取设备码')).toBeTruthy();
  });
});

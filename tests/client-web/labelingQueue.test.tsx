/* eslint-disable @typescript-eslint/no-explicit-any */
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { LabelingQueue } from '../../src/client-web/src/components/labeling/LabelingQueue';

vi.mock('../../src/client-web/src/api/classificationLabeling', () => ({
  fetchLabelingQueue: vi.fn(),
  submitLabel: vi.fn(),
  fetchCategoryDictionary: vi.fn(),
}));

import { fetchLabelingQueue, submitLabel, fetchCategoryDictionary } from '../../src/client-web/src/api/classificationLabeling';

function mockQueue() {
  (fetchLabelingQueue as any).mockResolvedValue({
    items: [
      {
        targetType: 'app',
        target: 'mobaxterm',
        displayName: 'MobaXterm',
        minutes: 42,
        sampleTitles: ['ssh to 192.168.1.1'],
      },
    ],
  });
}

function mockDictionary() {
  (fetchCategoryDictionary as any).mockResolvedValue([
    { id: '1', name: '编程/折腾', color: '#6B5EE4', icon: '💻' },
    { id: '2', name: '学习', color: '#14b8a6', icon: '📚' },
    { id: '3', name: '其他', color: '#64748b', icon: '📋' },
  ]);
}

describe('LabelingQueue', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    mockQueue();
    mockDictionary();
    (submitLabel as any).mockResolvedValue({ ok: true, categoryId: '1', categoryName: '编程/折腾', created: 'app_mapping' });
  });

  it('renders queue item and submits preset category', async () => {
    render(<LabelingQueue limit={20} />);
    expect(await screen.findByText('MobaXterm')).toBeTruthy();
    fireEvent.click(screen.getByText('编程/折腾'));
    expect(await screen.findByText(/已归入/)).toBeTruthy();
    expect(submitLabel).toHaveBeenCalledWith(
      expect.objectContaining({ targetType: 'app', target: 'mobaxterm' })
    );
  });

  it('adds custom category via input and submits', async () => {
    render(<LabelingQueue limit={20} />);
    expect(await screen.findByText('MobaXterm')).toBeTruthy();
    const input = screen.getByPlaceholderText('自定义分类，回车添加…');
    fireEvent.change(input, { target: { value: '写日记' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(await screen.findByText(/已归入/)).toBeTruthy();
    expect(submitLabel).toHaveBeenCalledWith(
      expect.objectContaining({ categoryName: '写日记' })
    );
  });
});

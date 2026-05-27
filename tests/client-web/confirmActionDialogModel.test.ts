import assert from 'node:assert/strict';
import { buildDeleteConfirmationCopy } from '../../src/client-web/src/ui/ConfirmActionDialog';
import type { CalendarOperationSample } from '../../src/client-web/src/types';

const eventSamples: CalendarOperationSample[] = [
  {
    id: 'event-1',
    type: 'event',
    title: '项目周会',
    start: '2026-05-27T09:00:00Z',
    end: '2026-05-27T10:00:00Z',
    bookName: '工作',
  },
];

const singleEventCopy = buildDeleteConfirmationCopy({
  targetType: 'event',
  title: '项目周会',
  affectedCount: 1,
  samples: eventSamples,
});

assert.equal(singleEventCopy.title, '删除日程');
assert.equal(singleEventCopy.description, '项目周会 将移动到回收站，可以在设置中恢复。');
assert.equal(singleEventCopy.confirmLabel, '移动到回收站');
assert.equal(singleEventCopy.samples, eventSamples);

const calendarCascadeCopy = buildDeleteConfirmationCopy({
  targetType: 'calendar',
  title: '工作',
  affectedCount: 4,
  samples: eventSamples,
});

assert.equal(calendarCascadeCopy.title, '删除日历本');
assert.equal(calendarCascadeCopy.description, '工作 和 4 个关联项目将一起移动到回收站。');
assert.equal(calendarCascadeCopy.confirmLabel, '确认移动 4 项');

const taskBookCopy = buildDeleteConfirmationCopy({
  targetType: 'task-book',
  title: '个人任务',
  affectedCount: 2,
  samples: [
    {
      id: 'task-1',
      type: 'task',
      title: '整理计划',
      bookName: '个人任务',
    },
  ],
});

assert.equal(taskBookCopy.title, '删除任务本');
assert.equal(taskBookCopy.confirmLabel, '确认移动 2 项');

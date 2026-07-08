import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

function assertPageSourceContains(path: string, snippets: string[]) {
  const source = readFileSync(path, 'utf8');

  for (const snippet of snippets) {
    assert.ok(source.includes(snippet), `${path} should contain ${snippet}`);
  }
}

assertPageSourceContains('src/client-web/src/pages/SyncPage.tsx', [
  '设备代码',
  'tokenHealth',
  'OutlookConflictResolver',
  'deltaLink',
  'writeback',
]);

assertPageSourceContains('src/client-web/src/pages/ConfirmationsPage.tsx', [
  'BeforeAfterDiff',
  'StrictConfirmationPanel',
  '二级确认',
  'allowedActions',
]);

assertPageSourceContains('src/client-web/src/pages/DataCenterPage.tsx', [
  'DataCenterBatchPreview',
  '审计导出',
  '版本恢复',
  'Outlook-only',
]);

assertPageSourceContains('src/client-web/src/pages/RemindersPage.tsx', [
  '提醒中心',
  'DND',
  '发送历史',
  '操作按钮',
]);

assertPageSourceContains('src/client-web/src/pages/ReportsPage.tsx', [
  '日报',
  '周报',
  '月报',
  '项目报告',
  '后续确认',
]);

assertPageSourceContains('src/client-web/src/pages/AuditTimelinePage.tsx', [
  '恢复预览',
  '导出审计',
]);

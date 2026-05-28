import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const source = readFileSync(resolve('src/client-web/src/pages/FilesPage.tsx'), 'utf8');

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function hasVisibleText(text: string) {
  const escaped = escapeRegExp(text);
  const quoted = new RegExp(`(['"\`])${escaped}\\1`);
  const jsxText = new RegExp(`>\\s*${escaped}(?=\\s|[<{])`);
  const embeddedPrompt = text.includes('?') && source.includes(text);
  return quoted.test(source) || jsxText.test(source) || embeddedPrompt;
}

const forbiddenVisibleText = [
  'Provider',
  'Loading providers...',
  'No provider connected.',
  'Bind Nextcloud',
  'Internal URL',
  'Username',
  'App password',
  'Folder tree',
  'No child folders here.',
  'Trash',
  'Loading trash...',
  'Trash is empty.',
  'Files',
  'Search files',
  'Hybrid',
  'Keyword',
  'Semantic',
  'Name',
  'Size',
  'Modified',
  'Loading files...',
  'No search results.',
  'No files in this folder.',
  'Semantic hits',
  'Details',
  'Select a file or folder.',
  'Loading details...',
  'Selected item is unavailable.',
  'View primary',
  'Edit separately',
  'Download',
  'Open in Nextcloud',
  'OOXML edit opens the provider editor.',
  'Type',
  'Synced',
  'Rename',
  'Move',
  'Delete to trash',
  'No AI result for current version.',
  'Versions',
  'Loading versions...',
  'No versions.',
  'Restore version',
  'Suggestions',
  'Loading suggestions...',
  'No suggestions for this item.',
  'Accept - mark useful',
  'Dismiss',
  'Nextcloud provider saved.',
  'Connection test passed.',
  'Connection test failed.',
  'Sync completed.',
  'Version restored.',
  'Trash item restored.',
  'Select a provider before restoring trash.',
  'Base URL, username, and app password are required.',
  'Move to path',
  'New name',
  'to trash?',
];

for (const text of forbiddenVisibleText) {
  assert.equal(
    hasVisibleText(text),
    false,
    `FilesPage should not expose English UI text: ${text}`,
  );
}

const requiredChineseText = [
  '文件来源',
  '正在加载文件来源...',
  '尚未连接文件来源。',
  '绑定 Nextcloud',
  '内部访问地址',
  '用户名',
  '应用密码',
  '文件夹树',
  '回收站',
  '文件列表',
  '搜索文件',
  '混合',
  '关键词',
  '语义',
  '名称',
  '大小',
  '修改时间',
  '详细信息',
  '主要方式打开',
  '单独编辑',
  '下载',
  '在 Nextcloud 中打开',
  '版本',
  '建议',
];

for (const text of requiredChineseText) {
  assert.match(source, new RegExp(escapeRegExp(text)), `FilesPage should include Chinese UI text: ${text}`);
}

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const appLayout = readFileSync('src/client-web/src/layout/AppLayout.tsx', 'utf8');
const floatingEntry = readFileSync(
  'src/client-web/src/components/quick-notes/QuickNoteFloatingEntry.tsx',
  'utf8',
);

assert.match(
  appLayout,
  /const QuickNotesPage = lazy\(\(\) => import\('\.\.\/pages\/QuickNotesPage'\)\)/,
  'QuickNotesPage should be lazy-loaded so the Markdown editor is not part of the app entry bundle.',
);

assert.doesNotMatch(
  appLayout,
  /import QuickNotesPage from '\.\.\/pages\/QuickNotesPage'/,
  'AppLayout should not statically import QuickNotesPage.',
);

// #280：全站入口改为 QuickNoteFloatingEntry；其面板（含 Markdown 编辑器）必须懒加载，
// 否则体积很大的编辑器会被带进应用入口 bundle。
assert.match(
  floatingEntry,
  /const LazyQuickNoteGlobalPanel = lazy\(\(\) => import\('\.\/QuickNoteGlobalPanel'\)\)/,
  'QuickNoteGlobalPanel should be lazy-loaded from the global entry so the Markdown editor stays out of the app entry bundle.',
);

assert.doesNotMatch(
  floatingEntry,
  /^import QuickNoteGlobalPanel from '\.\/QuickNoteGlobalPanel';/m,
  'The global entry should not statically import QuickNoteGlobalPanel.',
);

assert.doesNotMatch(
  floatingEntry,
  /^import QuickNoteEditor from '\.\/QuickNoteEditor';/m,
  'The global entry should not statically import QuickNoteEditor.',
);

// 入口按钮本身要足够轻量（不应直接引入编辑器）。
assert.doesNotMatch(
  appLayout,
  /^import QuickNoteEditor from/m,
  'AppLayout should not statically import QuickNoteEditor.',
);

console.error('PASS: quickNotesEntryBundle');

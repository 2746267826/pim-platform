import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const appLayout = readFileSync('src/client-web/src/layout/AppLayout.tsx', 'utf8');

assert.match(
  appLayout,
  /const QuickNotesPage = lazy\(\(\) => import\('\.\.\/pages\/QuickNotesPage'\)\)/,
  'QuickNotesPage should be lazy-loaded so the Markdown editor is not part of the app entry bundle.',
);

assert.match(
  appLayout,
  /const QuickNoteFloatingPanel = lazy\(\(\) => import\('\.\.\/components\/quick-notes\/QuickNoteFloatingPanel'\)\)/,
  'QuickNoteFloatingPanel should be lazy-loaded so the Markdown editor is not part of the app entry bundle.',
);

assert.doesNotMatch(
  appLayout,
  /import QuickNotesPage from '\.\.\/pages\/QuickNotesPage'/,
  'AppLayout should not statically import QuickNotesPage.',
);

assert.doesNotMatch(
  appLayout,
  /import QuickNoteFloatingPanel from '\.\.\/components\/quick-notes\/QuickNoteFloatingPanel'/,
  'AppLayout should not statically import QuickNoteFloatingPanel.',
);

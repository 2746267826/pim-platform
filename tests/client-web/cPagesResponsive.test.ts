import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
function assertContains(p: string, ss: string[]) { const s=readFileSync(p,'utf8'); for(const x of ss) assert.ok(s.includes(x), `${p} should contain ${x}`); }
assertContains('src/client-web/src/pages/WorkbenchPage.tsx', ['pb-20', 'SegmentedControl', 'grid-cols-1']);
assertContains('src/client-web/src/pages/PcClassificationPage.tsx', ['pb-20', 'overflow-auto']);
assertContains('src/client-web/src/pages/CategoryTreePage.tsx', ['pb-20']);
assertContains('src/client-web/src/pages/FilesPage.tsx', ['pb-20', 'overflow-auto']);
console.error('PASS: cPagesResponsive classification/files');

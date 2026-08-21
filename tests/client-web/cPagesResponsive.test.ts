import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
function assertContains(p: string, ss: string[]) { const s=readFileSync(p,'utf8'); for(const x of ss) assert.ok(s.includes(x), `${p} should contain ${x}`); }
assertContains('src/client-web/src/pages/WorkbenchPage.tsx', ['pb-20', 'SegmentedControl', 'grid-cols-1']);
console.error('PASS: cPagesResponsive workbench');

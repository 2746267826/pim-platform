import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const outPath = join(__dirname, '..', 'public', 'catalog.json');

const catalog = {
  generatedAt: new Date().toISOString(),
  stub: true,
  message: 'Task 2 catalog builder not implemented yet; empty catalog written for scaffold.',
  nodes: [],
  edges: [],
};

mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, `${JSON.stringify(catalog, null, 2)}\n`, 'utf8');
console.log(`[catalog] wrote stub catalog to ${outPath} (nodes=0, edges=0)`);

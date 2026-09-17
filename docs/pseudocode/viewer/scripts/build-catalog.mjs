import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const viewerRoot = path.resolve(__dirname, '..');
const pseudoRoot = path.resolve(viewerRoot, '..');
const filesRoot = path.join(pseudoRoot, 'files');
const graphPath = path.join(pseudoRoot, 'graphs', 'interactive', 'graph-data.json');
const outPath = path.join(viewerRoot, 'public', 'catalog.json');

function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name);
    const st = fs.statSync(p);
    if (st.isDirectory()) walk(p, acc);
    else if (name.endsWith('.md')) acc.push(p);
  }
  return acc;
}

function layerOf(srcPath) {
  const s = srcPath.replace(/\\/g, '/');
  if (s.startsWith('src/Pim.Core/')) return 'core';
  if (s.startsWith('src/Pim.Infrastructure/')) return 'infrastructure';
  if (s.startsWith('src/Pim.Api/')) return 'api';
  if (s.includes('Pim.Module.Stats')) return 'module.stats';
  if (s.includes('Pim.Module.QuickNotes')) return 'module.quicknotes';
  if (s.includes('Pim.Module.Files')) return 'module.files';
  if (s.includes('Pim.Module.Mobile')) return 'module.mobile';
  if (s.includes('Pim.Module.PcTracker')) return 'module.pctracker';
  if (s.includes('Pim.Module.Calendar')) return 'module.calendar';
  if (s.startsWith('src/client-web/')) return 'client-web';
  if (s.startsWith('src/client-windows/')) return 'client-windows';
  if (s.startsWith('src/client-android/')) return 'client-android';
  if (s.startsWith('tests/')) return 'tests';
  return 'other';
}

function extractTitle(md, fallback) {
  const m = md.match(/^#\s+(.+)$/m);
  return m ? m[1].trim() : fallback;
}

function extractSummary(md) {
  const m = md.match(/职责[：:]\s*(.+)/);
  if (m) return m[1].trim().slice(0, 160);
  const lines = md.split(/\r?\n/).filter((l) => l.trim() && !l.startsWith('#'));
  return (lines[0] || '').replace(/^[-*]\s*/, '').slice(0, 160);
}

function extractFunctionBullets(md) {
  const idx = md.indexOf('## 函数级结构化伪代码');
  if (idx < 0) return [];
  const rest = md.slice(idx);
  const end = rest.search(/\n## /);
  const section = end > 0 ? rest.slice(0, end) : rest;
  return section
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => /^[-*\d.]/.test(l))
    .slice(0, 8)
    .map((l) => l.replace(/^[-*]\s*/, '').replace(/^\d+\.\s*/, ''));
}

if (!fs.existsSync(graphPath)) {
  console.error(`[catalog] missing graph-data: ${graphPath}`);
  process.exit(1);
}
if (!fs.existsSync(filesRoot)) {
  console.error(`[catalog] missing files root: ${filesRoot}`);
  process.exit(1);
}

const graphRaw = fs.readFileSync(graphPath, 'utf8').replace(/^\uFEFF/, '');
const graph = JSON.parse(graphRaw);
const nodeMap = new Map((graph.nodes || []).map((n) => [n.id, n]));
const edges = (graph.edges || []).filter((e) => e && e.from && e.to);

const mdFiles = walk(filesRoot);
const nodes = [];
for (const abs of mdFiles) {
  const relDoc = path.relative(pseudoRoot, abs).replace(/\\/g, '/'); // files/...
  const src = relDoc.replace(/^files\//, '').replace(/\.md$/, '');
  const md = fs.readFileSync(abs, 'utf8');
  const base = nodeMap.get(src) || {};
  const bullets = extractFunctionBullets(md);
  nodes.push({
    id: src,
    label: base.label || path.basename(src),
    path: src,
    doc: `docs/pseudocode/${relDoc}`,
    layer: base.layer || layerOf(src),
    kind: base.kind || (layerOf(src) === 'tests' ? 'test' : 'other'),
    title: extractTitle(md, path.basename(src)),
    summary: extractSummary(md),
    functionBullets: bullets,
  });
}

const apiIndex = [];
for (const e of edges) {
  if (e.type !== 'http') continue;
  const pathLike = String(e.to);
  if (!pathLike.includes('/')) continue;
  apiIndex.push({ path: pathLike, method: 'ANY', nodeId: e.from });
}

const catalog = {
  generated: new Date().toISOString(),
  nodes,
  edges,
  apiIndex,
  stats: {
    nodeCount: nodes.length,
    edgeCount: edges.length,
    docCount: mdFiles.length,
  },
};

fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, JSON.stringify(catalog), 'utf8');
console.log(
  `catalog nodes=${catalog.stats.nodeCount} edges=${catalog.stats.edgeCount} docs=${catalog.stats.docCount} -> ${outPath}`,
);

const docsOut = path.join(viewerRoot, 'public', 'docs-files');
fs.rmSync(docsOut, { recursive: true, force: true });
for (const abs of mdFiles) {
  const relDoc = path.relative(filesRoot, abs).replace(/\\/g, '/'); // path under files/
  const dest = path.join(docsOut, relDoc);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(abs, dest);
}
console.log(`copied ${mdFiles.length} md -> public/docs-files`);

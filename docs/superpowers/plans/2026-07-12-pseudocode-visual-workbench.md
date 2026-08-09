# Pseudocode Visual Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `docs/pseudocode/viewer/` 交付浅色三栏 + 双模式（阅读/关系图）+ 数据流水线摘取/导出的静态可视化工作台，只读消费现有 775 份伪代码与 graph-data。

**Architecture:** Vite + React + TypeScript 单页应用；`build-catalog.mjs` 从 `docs/pseudocode/files/**` 与 `graphs/interactive/graph-data.json` 生成 `public/catalog.json`；运行时懒加载单文件 Markdown；图用 WebGL（优先 `@antv/g6`）；流水线为对静态边的有限深度 BFS 子图 + 自研步骤 UI。

**Tech Stack:** Vite 5, React 18, TypeScript 5, markdown-it, @antv/g6 (or sigma.js fallback), Vitest, Node fs for catalog script

**Spec:** `docs/superpowers/specs/2026-07-12-pseudocode-visual-workbench-design.md`

---

## File Map

| Path | Responsibility |
|------|----------------|
| `docs/pseudocode/viewer/package.json` | deps & scripts (`dev`, `build`, `catalog`, `test`) |
| `docs/pseudocode/viewer/vite.config.ts` | alias, static copy of md files for dev/prod |
| `docs/pseudocode/viewer/index.html` | SPA shell |
| `docs/pseudocode/viewer/scripts/build-catalog.mjs` | scan md + graph-data → catalog.json |
| `docs/pseudocode/viewer/public/catalog.json` | generated index (commit after first successful catalog) |
| `docs/pseudocode/viewer/src/main.tsx` | React mount |
| `docs/pseudocode/viewer/src/App.tsx` | mode switch, shared selection state |
| `docs/pseudocode/viewer/src/styles/paper.css` | paper-light theme + three-pane layout |
| `docs/pseudocode/viewer/src/lib/types.ts` | Catalog, Node, Edge, Pipeline types |
| `docs/pseudocode/viewer/src/lib/catalog.ts` | load catalog, search, layer filter |
| `docs/pseudocode/viewer/src/lib/pipeline.ts` | BFS pipeline builder + markdown export |
| `docs/pseudocode/viewer/src/lib/mdSplit.ts` | split 函数级 / 近逐行 sections |
| `docs/pseudocode/viewer/src/modes/ReadMode.tsx` | three-pane reader |
| `docs/pseudocode/viewer/src/modes/GraphMode.tsx` | G6 graph |
| `docs/pseudocode/viewer/src/modes/PipelineCanvas.tsx` | pipeline overlay |
| `docs/pseudocode/viewer/src/components/TopBar.tsx` | search, modes, 摘流水线 |
| `docs/pseudocode/viewer/src/components/FileTree.tsx` | virtualized tree |
| `docs/pseudocode/viewer/src/components/DocViewer.tsx` | markdown render + section toggle |
| `docs/pseudocode/viewer/src/components/EdgePanel.tsx` | right-pane edges |
| `docs/pseudocode/viewer/src/lib/pipeline.test.ts` | unit tests for BFS/export |
| `docs/pseudocode/viewer/src/lib/mdSplit.test.ts` | unit tests for section split |
| `docs/pseudocode/viewer/README.md` | how to run |
| `docs/pseudocode/README.md` | link to viewer |
| `docs/pseudocode/graphs/interactive/index.html` | banner link to new viewer |

---

### Task 0: Branch And Workspace

**Files:** none (git only)

- [ ] **Step 1: Ensure base has docs/pseudocode assets**

```powershell
git fetch origin
# Prefer branch that already contains 775 docs, e.g. origin/codex/pseudocode-docs-b0-scaffold or merged master
git checkout -b codex/pseudocode-visual-workbench origin/codex/pseudocode-docs-b0-scaffold
# If design-only commits needed, cherry-pick design/plan commits from codex/pseudocode-visual-workbench-design
```

Expected: `docs/pseudocode/files` has ~775 md files; `graph-data.json` exists.

- [ ] **Step 2: Confirm Node available**

```powershell
node -v
npm -v
```

Expected: Node 18+ and npm present.

---

### Task 1: Scaffold Vite React TypeScript App

**Files:**
- Create: `docs/pseudocode/viewer/package.json`
- Create: `docs/pseudocode/viewer/vite.config.ts`
- Create: `docs/pseudocode/viewer/tsconfig.json`
- Create: `docs/pseudocode/viewer/tsconfig.app.json`
- Create: `docs/pseudocode/viewer/tsconfig.node.json`
- Create: `docs/pseudocode/viewer/index.html`
- Create: `docs/pseudocode/viewer/src/main.tsx`
- Create: `docs/pseudocode/viewer/src/App.tsx`
- Create: `docs/pseudocode/viewer/src/styles/paper.css`
- Create: `docs/pseudocode/viewer/src/vite-env.d.ts`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "pim-pseudocode-viewer",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "catalog": "node scripts/build-catalog.mjs",
    "dev": "npm run catalog && vite",
    "build": "npm run catalog && tsc -b && vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
  "dependencies": {
    "@antv/g6": "^5.0.44",
    "markdown-it": "^14.1.0",
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  },
  "devDependencies": {
    "@types/markdown-it": "^14.1.2",
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.4",
    "typescript": "~5.6.3",
    "vite": "^5.4.11",
    "vitest": "^2.1.8"
  }
}
```

- [ ] **Step 2: Create vite.config.ts**

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  base: './',
  publicDir: 'public',
  server: {
    fs: {
      allow: [path.resolve(__dirname, '..'), path.resolve(__dirname)],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  test: {
    environment: 'node',
  },
});
```

- [ ] **Step 3: Create index.html + main.tsx + minimal App shell**

`index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>PIM 伪代码工作台</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link href="https://fonts.googleapis.com/css2?family=Literata:opsz,wght@7..72,500;7..72,700&family=IBM+Plex+Mono:wght@400;500&display=swap" rel="stylesheet" />
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

`src/main.tsx`:

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/paper.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

`src/App.tsx` (temporary shell):

```tsx
export default function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <strong>PIM 伪代码工作台</strong>
        <span className="muted">scaffold</span>
      </header>
      <main className="three-pane">
        <aside className="pane pane-left">树</aside>
        <section className="pane pane-center">文档</section>
        <aside className="pane pane-right">关系</aside>
      </main>
    </div>
  );
}
```

- [ ] **Step 4: paper.css three-pane base**

```css
:root {
  --paper: #f7f3eb;
  --panel: #fffefb;
  --ink: #1c1917;
  --muted: #78716c;
  --line: #e7e0d5;
  --accent: #b45309;
  --mono: 'IBM Plex Mono', ui-monospace, monospace;
  --serif: 'Literata', 'Songti SC', serif;
}

* { box-sizing: border-box; }
html, body, #root { height: 100%; margin: 0; }
body {
  font-family: var(--serif);
  color: var(--ink);
  background: var(--paper);
}

.app-shell { display: flex; flex-direction: column; height: 100%; }
.topbar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; border-bottom: 1px solid var(--line);
  background: var(--panel);
}
.muted { color: var(--muted); font-size: 13px; }
.three-pane {
  flex: 1; display: grid;
  grid-template-columns: minmax(200px, 22%) 1fr minmax(200px, 24%);
  min-height: 0;
}
.pane {
  min-height: 0; overflow: auto;
  background: var(--panel);
  border-right: 1px solid var(--line);
  padding: 10px;
}
.pane-right { border-right: 0; border-left: 1px solid var(--line); }
.pane-center { font-family: var(--mono); font-size: 13px; line-height: 1.55; }
```

- [ ] **Step 5: Install and verify scaffold**

```powershell
cd docs/pseudocode/viewer
npm install
npm run dev
```

Expected: dev server starts; browser shows three-pane shell.

- [ ] **Step 6: Commit**

```powershell
git add docs/pseudocode/viewer
git commit -m "feat: scaffold pseudocode viewer vite app"
```

---

### Task 2: Types And Catalog Builder

**Files:**
- Create: `docs/pseudocode/viewer/src/lib/types.ts`
- Create: `docs/pseudocode/viewer/scripts/build-catalog.mjs`
- Create: `docs/pseudocode/viewer/public/catalog.json` (generated)
- Create: `docs/pseudocode/viewer/src/lib/catalog.ts`

- [ ] **Step 1: types.ts**

```typescript
export type EdgeType = 'depends_on' | 'calls' | 'implements' | 'extends' | 'tests' | 'http' | string;

export interface GraphNode {
  id: string;
  label: string;
  path: string;
  doc: string;
  layer: string;
  kind: string;
  title?: string;
  summary?: string;
}

export interface GraphEdge {
  from: string;
  to: string;
  type: EdgeType;
}

export interface ApiIndexEntry {
  path: string;
  method: string;
  nodeId: string;
}

export interface Catalog {
  generated: string;
  nodes: GraphNode[];
  edges: GraphEdge[];
  apiIndex: ApiIndexEntry[];
  stats: { nodeCount: number; edgeCount: number; docCount: number };
}

export type WorkbenchMode = 'read' | 'graph';
export type DocSection = 'function' | 'line';
```

- [ ] **Step 2: build-catalog.mjs (full script)**

```javascript
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

const graph = JSON.parse(fs.readFileSync(graphPath, 'utf8'));
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
console.log(`catalog nodes=${catalog.stats.nodeCount} edges=${catalog.stats.edgeCount} docs=${catalog.stats.docCount} -> ${outPath}`);
```

- [ ] **Step 3: Run catalog**

```powershell
cd docs/pseudocode/viewer
npm run catalog
```

Expected: `public/catalog.json` with nodeCount ≈ 775.

- [ ] **Step 4: catalog.ts loader**

```typescript
import type { Catalog, GraphNode } from './types';

let cached: Catalog | null = null;

export async function loadCatalog(): Promise<Catalog> {
  if (cached) return cached;
  const res = await fetch('./catalog.json');
  if (!res.ok) throw new Error('catalog.json 缺失，请先运行 npm run catalog');
  cached = (await res.json()) as Catalog;
  return cached;
}

export function searchNodes(catalog: Catalog, q: string, layer?: string): GraphNode[] {
  const query = q.trim().toLowerCase();
  return catalog.nodes.filter((n) => {
    if (layer && n.layer !== layer) return false;
    if (!query) return true;
    return (
      n.id.toLowerCase().includes(query) ||
      n.label.toLowerCase().includes(query) ||
      (n.title || '').toLowerCase().includes(query)
    );
  });
}

export function getNode(catalog: Catalog, id: string): GraphNode | undefined {
  return catalog.nodes.find((n) => n.id === id);
}

export function edgesFor(catalog: Catalog, id: string) {
  return catalog.edges.filter((e) => e.from === id || e.to === id);
}
```

- [ ] **Step 5: Commit**

```powershell
git add docs/pseudocode/viewer
git commit -m "feat: build catalog from pseudocode files and graph-data"
```

---

### Task 3: Markdown Split And Doc Viewer

**Files:**
- Create: `docs/pseudocode/viewer/src/lib/mdSplit.ts`
- Create: `docs/pseudocode/viewer/src/lib/mdSplit.test.ts`
- Create: `docs/pseudocode/viewer/src/components/DocViewer.tsx`

- [ ] **Step 1: Write failing tests for mdSplit**

```typescript
import { describe, it, expect } from 'vitest';
import { splitPseudocodeSections } from './mdSplit';

describe('splitPseudocodeSections', () => {
  it('splits function and line sections', () => {
    const md = `# a.cs

## 元信息
- 职责：x

## 函数级结构化伪代码
### Foo
- 步骤：1

## 近逐行中文伪代码
1. 做 A

## 关系边
\`\`\`json
{}\`\`\`
`;
    const s = splitPseudocodeSections(md);
    expect(s.meta).toContain('职责');
    expect(s.functionBody).toContain('Foo');
    expect(s.lineBody).toContain('做 A');
  });
});
```

- [ ] **Step 2: Run test (expect fail)**

```powershell
cd docs/pseudocode/viewer
npx vitest run src/lib/mdSplit.test.ts
```

Expected: FAIL module not found or function missing.

- [ ] **Step 3: Implement mdSplit.ts**

```typescript
export interface SplitDoc {
  title: string;
  meta: string;
  functionBody: string;
  lineBody: string;
  raw: string;
}

export function splitPseudocodeSections(md: string): SplitDoc {
  const title = (md.match(/^#\s+(.+)$/m) || [, ''])[1].trim();
  const parts = md.split(/^## /m);
  let meta = '';
  let functionBody = '';
  let lineBody = '';
  for (const p of parts) {
    if (p.startsWith('元信息')) meta = p.replace(/^元信息\s*/, '').trim();
    else if (p.startsWith('函数级')) functionBody = p.replace(/^函数级结构化伪代码\s*/, '').trim();
    else if (p.startsWith('近逐行')) lineBody = p.replace(/^近逐行中文伪代码\s*/, '').trim();
  }
  return { title, meta, functionBody, lineBody, raw: md };
}
```

- [ ] **Step 4: Re-run tests**

```powershell
npx vitest run src/lib/mdSplit.test.ts
```

Expected: PASS.

- [ ] **Step 5: DocViewer component**

```tsx
import { useEffect, useMemo, useState } from 'react';
import MarkdownIt from 'markdown-it';
import { splitPseudocodeSections } from '../lib/mdSplit';
import type { DocSection } from '../lib/types';

const md = new MarkdownIt({ html: false, linkify: true, breaks: true });

export function DocViewer({ fileId, section, onSection }: {
  fileId: string | null;
  section: DocSection;
  onSection: (s: DocSection) => void;
}) {
  const [raw, setRaw] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!fileId) return;
    setError('');
    // served via vite public mirror: public/docs-files/<id>.md
    const url = `./docs-files/${fileId}.md`;
    fetch(url)
      .then((r) => {
        if (!r.ok) throw new Error(`无法加载 ${url}`);
        return r.text();
      })
      .then(setRaw)
      .catch((e) => setError(String(e.message || e)));
  }, [fileId]);

  const split = useMemo(() => (raw ? splitPseudocodeSections(raw) : null), [raw]);
  const body = section === 'function' ? split?.functionBody : split?.lineBody;

  if (!fileId) return <div className="muted">从左侧选择文件</div>;
  if (error) return <div className="error">{error}</div>;
  if (!split) return <div className="muted">加载中…</div>;

  return (
    <div className="doc-viewer">
      <h1 className="doc-title">{split.title || fileId}</h1>
      <pre className="doc-meta">{split.meta}</pre>
      <div className="seg">
        <button className={section === 'function' ? 'active' : ''} onClick={() => onSection('function')}>函数级</button>
        <button className={section === 'line' ? 'active' : ''} onClick={() => onSection('line')}>近逐行</button>
      </div>
      <div className="md-body" dangerouslySetInnerHTML={{ __html: md.render(body || '_（本节为空）_') }} />
    </div>
  );
}
```

- [ ] **Step 6: Extend build-catalog to copy md into public/docs-files**

Append to `build-catalog.mjs` after writing catalog:

```javascript
const docsOut = path.join(viewerRoot, 'public', 'docs-files');
fs.rmSync(docsOut, { recursive: true, force: true });
for (const abs of mdFiles) {
  const relDoc = path.relative(filesRoot, abs).replace(/\\/g, '/'); // path under files/
  const dest = path.join(docsOut, relDoc);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(abs, dest);
}
console.log(`copied ${mdFiles.length} md -> public/docs-files`);
```

Note: DocViewer URL uses `./docs-files/${fileId}.md` where `fileId` is source path like `src/Pim.Api/Program.cs` → file `public/docs-files/src/Pim.Api/Program.cs.md`.

Fix DocViewer fetch:

```typescript
const url = `./docs-files/${fileId}.md`;
```

- [ ] **Step 7: Commit**

```powershell
git add docs/pseudocode/viewer
git commit -m "feat: split and render dual-granularity pseudocode docs"
```

---

### Task 4: Read Mode Three Pane Wiring

**Files:**
- Create: `docs/pseudocode/viewer/src/components/TopBar.tsx`
- Create: `docs/pseudocode/viewer/src/components/FileTree.tsx`
- Create: `docs/pseudocode/viewer/src/components/EdgePanel.tsx`
- Create: `docs/pseudocode/viewer/src/modes/ReadMode.tsx`
- Modify: `docs/pseudocode/viewer/src/App.tsx`

- [ ] **Step 1: FileTree (simple virtual window by slice)**

```tsx
import type { GraphNode } from '../lib/types';

export function FileTree({
  nodes,
  selectedId,
  onSelect,
}: {
  nodes: GraphNode[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  // group by layer
  const layers = new Map<string, GraphNode[]>();
  for (const n of nodes) {
    const arr = layers.get(n.layer) || [];
    arr.push(n);
    layers.set(n.layer, arr);
  }
  const layerNames = [...layers.keys()].sort();

  return (
    <div className="file-tree">
      {layerNames.map((layer) => (
        <details key={layer} open={layer !== 'tests'}>
          <summary>
            <span className="layer-dot" data-layer={layer} />
            {layer} ({layers.get(layer)!.length})
          </summary>
          <ul>
            {layers.get(layer)!.map((n) => (
              <li key={n.id}>
                <button
                  className={selectedId === n.id ? 'tree-item active' : 'tree-item'}
                  onClick={() => onSelect(n.id)}
                  title={n.id}
                >
                  {n.label}
                </button>
              </li>
            ))}
          </ul>
        </details>
      ))}
    </div>
  );
}
```

- [ ] **Step 2: EdgePanel**

```tsx
import type { Catalog } from '../lib/types';
import { edgesFor, getNode } from '../lib/catalog';

export function EdgePanel({
  catalog,
  fileId,
  onOpen,
  onPipeline,
}: {
  catalog: Catalog;
  fileId: string | null;
  onOpen: (id: string) => void;
  onPipeline: () => void;
}) {
  if (!fileId) return <div className="muted">无选中文件</div>;
  const list = edgesFor(catalog, fileId);
  return (
    <div className="edge-panel">
      <button className="primary" onClick={onPipeline}>从这里摘流水线</button>
      <h3>关系边 ({list.length})</h3>
      <ul>
        {list.map((e, i) => {
          const other = e.from === fileId ? e.to : e.from;
          const dir = e.from === fileId ? '→' : '←';
          const node = getNode(catalog, other);
          return (
            <li key={i}>
              <span className="edge-type">{e.type}</span> {dir}{' '}
              <button className="linkish" onClick={() => onOpen(other)}>
                {node?.label || other}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
```

- [ ] **Step 3: TopBar**

```tsx
import type { WorkbenchMode } from '../lib/types';

export function TopBar({
  mode,
  onMode,
  query,
  onQuery,
  layer,
  onLayer,
  layers,
  onPipeline,
  stats,
}: {
  mode: WorkbenchMode;
  onMode: (m: WorkbenchMode) => void;
  query: string;
  onQuery: (q: string) => void;
  layer: string;
  onLayer: (l: string) => void;
  layers: string[];
  onPipeline: () => void;
  stats?: { nodeCount: number; edgeCount: number };
}) {
  return (
    <header className="topbar">
      <strong>PIM 伪代码工作台</strong>
      <div className="seg">
        <button className={mode === 'read' ? 'active' : ''} onClick={() => onMode('read')}>阅读</button>
        <button className={mode === 'graph' ? 'active' : ''} onClick={() => onMode('graph')}>关系图</button>
      </div>
      <input
        className="search"
        placeholder="搜索路径 / 名称 / API"
        value={query}
        onChange={(e) => onQuery(e.target.value)}
      />
      <select value={layer} onChange={(e) => onLayer(e.target.value)}>
        <option value="">全部 layer</option>
        {layers.map((l) => (
          <option key={l} value={l}>{l}</option>
        ))}
      </select>
      <button className="primary" onClick={onPipeline}>摘流水线</button>
      {stats && <span className="muted">{stats.nodeCount} 节点 · {stats.edgeCount} 边</span>}
    </header>
  );
}
```

- [ ] **Step 4: Wire App.tsx state**

Load catalog once; hold `mode`, `selectedId`, `query`, `layer`, `section`, `pipelineOpen`.

```tsx
import { useEffect, useMemo, useState } from 'react';
import { loadCatalog, searchNodes } from './lib/catalog';
import type { Catalog, DocSection, WorkbenchMode } from './lib/types';
import { TopBar } from './components/TopBar';
import { FileTree } from './components/FileTree';
import { DocViewer } from './components/DocViewer';
import { EdgePanel } from './components/EdgePanel';

export default function App() {
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [error, setError] = useState('');
  const [mode, setMode] = useState<WorkbenchMode>('read');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [layer, setLayer] = useState('');
  const [section, setSection] = useState<DocSection>('function');
  const [pipelineOpen, setPipelineOpen] = useState(false);

  useEffect(() => {
    loadCatalog().then(setCatalog).catch((e) => setError(String(e.message || e)));
  }, []);

  const layers = useMemo(
    () => (catalog ? [...new Set(catalog.nodes.map((n) => n.layer))].sort() : []),
    [catalog],
  );
  const visible = useMemo(
    () => (catalog ? searchNodes(catalog, query, layer || undefined) : []),
    [catalog, query, layer],
  );

  if (error) return <div className="error-page">{error}</div>;
  if (!catalog) return <div className="muted">加载 catalog…</div>;

  return (
    <div className="app-shell">
      <TopBar
        mode={mode}
        onMode={setMode}
        query={query}
        onQuery={setQuery}
        layer={layer}
        onLayer={setLayer}
        layers={layers}
        onPipeline={() => setPipelineOpen(true)}
        stats={catalog.stats}
      />
      {mode === 'read' ? (
        <main className="three-pane">
          <aside className="pane pane-left">
            <FileTree nodes={visible} selectedId={selectedId} onSelect={setSelectedId} />
          </aside>
          <section className="pane pane-center">
            <DocViewer fileId={selectedId} section={section} onSection={setSection} />
          </section>
          <aside className="pane pane-right">
            <EdgePanel
              catalog={catalog}
              fileId={selectedId}
              onOpen={setSelectedId}
              onPipeline={() => setPipelineOpen(true)}
            />
          </aside>
        </main>
      ) : (
        <div className="graph-placeholder muted">关系图模式（下一任务）</div>
      )}
      {/* PipelineCanvas mounted in Task 6 when pipelineOpen */}
    </div>
  );
}
```

- [ ] **Step 5: Manual verify**

```powershell
cd docs/pseudocode/viewer
npm run catalog
npm run dev
```

Open app: pick `src/Pim.Api/Program.cs`, toggle 函数级/近逐行, click edge to navigate.

- [ ] **Step 6: Commit**

```powershell
git add docs/pseudocode/viewer
git commit -m "feat: wire read mode three-pane pseudocode browser"
```

---

### Task 5: Graph Mode With G6

**Files:**
- Create: `docs/pseudocode/viewer/src/modes/GraphMode.tsx`
- Modify: `docs/pseudocode/viewer/src/App.tsx`
- Modify: `docs/pseudocode/viewer/src/styles/paper.css`

- [ ] **Step 1: GraphMode.tsx**

```tsx
import { useEffect, useRef } from 'react';
import { Graph } from '@antv/g6';
import type { Catalog } from '../lib/types';

const LAYER_COLOR: Record<string, string> = {
  core: '#1d4ed8',
  infrastructure: '#7c3aed',
  api: '#be123c',
  'client-web': '#047857',
  'client-windows': '#b45309',
  'client-android': '#0e7490',
  tests: '#78716c',
};

function colorFor(layer: string) {
  if (LAYER_COLOR[layer]) return LAYER_COLOR[layer];
  if (layer.startsWith('module.')) return '#c2410c';
  return '#57534e';
}

export function GraphMode({
  catalog,
  selectedId,
  hideTests,
  onSelect,
}: {
  catalog: Catalog;
  selectedId: string | null;
  hideTests: boolean;
  onSelect: (id: string) => void;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph | null>(null);

  useEffect(() => {
    if (!ref.current) return;
    const nodes = catalog.nodes.filter((n) => !(hideTests && n.layer === 'tests'));
    const ids = new Set(nodes.map((n) => n.id));
    const edges = catalog.edges.filter((e) => ids.has(e.from) && ids.has(e.to));

    const graph = new Graph({
      container: ref.current,
      autoFit: 'view',
      data: {
        nodes: nodes.map((n) => ({
          id: n.id,
          data: { label: n.label, layer: n.layer },
          style: {
            fill: colorFor(n.layer),
            size: selectedId === n.id ? 14 : 8,
            labelText: n.label,
            labelFontSize: 10,
          },
        })),
        edges: edges.map((e, i) => ({
          id: `e${i}`,
          source: e.from,
          target: e.to,
          data: { type: e.type },
          style: { stroke: '#d6d3d1', lineWidth: 1 },
        })),
      },
      layout: {
        type: 'force',
        preventOverlap: true,
        animated: false,
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    });

    graph.on('node:click', (evt: any) => {
      const id = evt?.target?.id;
      if (id) onSelect(String(id));
    });

    graph.render();
    graphRef.current = graph;
    return () => {
      graph.destroy();
      graphRef.current = null;
    };
  }, [catalog, hideTests]); // re-create when filter changes

  useEffect(() => {
    // optional: focus selected node via graph API if available
  }, [selectedId]);

  return (
    <div className="graph-mode">
      <div className="graph-canvas" ref={ref} />
      <aside className="graph-side">
        <h3>选中</h3>
        <p className="mono">{selectedId || '点击节点'}</p>
        {selectedId && (
          <button className="primary" onClick={() => onSelect(selectedId)}>在阅读中打开</button>
        )}
      </aside>
    </div>
  );
}
```

If G6 v5 API differs in the installed version, adjust import/`Graph` constructor to match package docs while preserving behaviors: force layout, zoom/drag, node click → select.

- [ ] **Step 2: Mount GraphMode in App when mode==='graph'**

Also pass `hideTests={layer !== 'tests'}` or a dedicated checkbox default true for tests hidden unless layer filter is tests.

- [ ] **Step 3: CSS for graph-mode**

```css
.graph-mode { flex: 1; display: grid; grid-template-columns: 1fr 280px; min-height: 0; }
.graph-canvas { min-height: 0; background: #fffefb; }
.graph-side { border-left: 1px solid var(--line); padding: 12px; background: var(--panel); }
.mono { font-family: var(--mono); font-size: 12px; word-break: break-all; }
```

- [ ] **Step 4: Manual verify**

```powershell
npm run dev
```

Switch to 关系图: pan/zoom; click node; open in read mode.

- [ ] **Step 5: Commit**

```powershell
git add docs/pseudocode/viewer
git commit -m "feat: add WebGL relationship graph mode"
```

---

### Task 6: Pipeline Engine And Canvas (Data Flow C)

**Files:**
- Create: `docs/pseudocode/viewer/src/lib/pipeline.ts`
- Create: `docs/pseudocode/viewer/src/lib/pipeline.test.ts`
- Create: `docs/pseudocode/viewer/src/modes/PipelineCanvas.tsx`
- Modify: `docs/pseudocode/viewer/src/App.tsx`

- [ ] **Step 1: Failing tests for pipeline BFS**

```typescript
import { describe, it, expect } from 'vitest';
import { buildPipeline, exportPipelineMarkdown } from './pipeline';
import type { Catalog } from './types';

const catalog: Catalog = {
  generated: 't',
  nodes: [
    { id: 'A', label: 'A', path: 'A', doc: '', layer: 'api', kind: 'endpoint', summary: '入口' },
    { id: 'B', label: 'B', path: 'B', doc: '', layer: 'module.mobile', kind: 'service', summary: '处理' },
    { id: 'C', label: 'C', path: 'C', doc: '', layer: 'infrastructure', kind: 'entity', summary: '存储' },
  ],
  edges: [
    { from: 'A', to: 'B', type: 'calls' },
    { from: 'B', to: 'C', type: 'depends_on' },
    { from: 'A', to: '/api/x', type: 'http' },
  ],
  apiIndex: [{ path: '/api/x', method: 'POST', nodeId: 'A' }],
  stats: { nodeCount: 3, edgeCount: 3, docCount: 3 },
};

describe('buildPipeline', () => {
  it('walks outbound edges up to depth', () => {
    const p = buildPipeline(catalog, { kind: 'file', id: 'A' }, 2);
    expect(p.steps.map((s) => s.nodeId)).toEqual(['A', 'B', 'C']);
  });

  it('starts from api index', () => {
    const p = buildPipeline(catalog, { kind: 'api', id: '/api/x' }, 1);
    expect(p.steps[0].nodeId).toBe('A');
  });

  it('exports markdown', () => {
    const p = buildPipeline(catalog, { kind: 'file', id: 'A' }, 2);
    const md = exportPipelineMarkdown(p);
    expect(md).toContain('# 数据流水线');
    expect(md).toContain('A');
    expect(md).toContain('calls');
  });
});
```

- [ ] **Step 2: Run tests (fail)**

```powershell
npx vitest run src/lib/pipeline.test.ts
```

- [ ] **Step 3: Implement pipeline.ts**

```typescript
import type { Catalog, EdgeType, GraphEdge } from './types';

export type PipelineStart =
  | { kind: 'file'; id: string }
  | { kind: 'api'; id: string };

export interface PipelineStep {
  nodeId: string;
  label: string;
  layer: string;
  summary: string;
  via?: { from: string; to: string; type: EdgeType };
  fixed?: boolean;
  bullets?: string[];
}

export interface Pipeline {
  start: PipelineStart;
  depth: number;
  steps: PipelineStep[];
  edges: GraphEdge[];
}

const WALK_TYPES = new Set(['calls', 'depends_on', 'http', 'implements']);

export function buildPipeline(catalog: Catalog, start: PipelineStart, depth: number): Pipeline {
  const d = Math.min(6, Math.max(1, depth));
  let rootId = start.kind === 'file' ? start.id : '';
  if (start.kind === 'api') {
    const hit = catalog.apiIndex.find((a) => a.path === start.id) ||
      catalog.edges.find((e) => e.type === 'http' && String(e.to) === start.id);
    rootId = (hit && 'nodeId' in hit ? hit.nodeId : (hit as GraphEdge | undefined)?.from) || '';
  }
  const nodeById = new Map(catalog.nodes.map((n) => [n.id, n]));
  if (!rootId || !nodeById.has(rootId)) {
    return { start, depth: d, steps: [], edges: [] };
  }

  const adj = new Map<string, GraphEdge[]>();
  for (const e of catalog.edges) {
    if (!WALK_TYPES.has(String(e.type)) && e.type !== 'http') continue;
    if (!adj.has(e.from)) adj.set(e.from, []);
    adj.get(e.from)!.push(e);
  }

  const steps: PipelineStep[] = [];
  const usedEdges: GraphEdge[] = [];
  const seen = new Set<string>();
  const queue: { id: string; dist: number; via?: GraphEdge }[] = [{ id: rootId, dist: 0 }];

  while (queue.length) {
    const cur = queue.shift()!;
    if (seen.has(cur.id)) continue;
    seen.add(cur.id);
    const n = nodeById.get(cur.id)!;
    steps.push({
      nodeId: n.id,
      label: n.label,
      layer: n.layer,
      summary: n.summary || '',
      via: cur.via ? { from: cur.via.from, to: cur.via.to, type: cur.via.type } : undefined,
      bullets: (n as any).functionBullets || [],
    });
    if (cur.via) usedEdges.push(cur.via);
    if (cur.dist >= d) continue;
    for (const e of adj.get(cur.id) || []) {
      // for http edges, skip non-file targets that are not nodes
      if (!nodeById.has(e.to)) continue;
      if (!seen.has(e.to)) queue.push({ id: e.to, dist: cur.dist + 1, via: e });
    }
  }

  // cap ~80 nodes
  const capped = steps.slice(0, 80);
  return { start, depth: d, steps: capped, edges: usedEdges };
}

export function exportPipelineMarkdown(p: Pipeline): string {
  const title = p.steps[0]?.label || (p.start.kind === 'api' ? p.start.id : p.start.id);
  const lines: string[] = [
    `# 数据流水线：${title}`,
    `- 生成时间：${new Date().toISOString()}`,
    `- 起点类型：${p.start.kind}`,
    `- 深度：${p.depth}`,
    '',
    '## 步骤',
  ];
  p.steps.forEach((s, i) => {
    lines.push(`### ${i + 1}. ${s.label}`);
    lines.push(`- 节点：\`${s.nodeId}\``);
    if (s.via) lines.push(`- 关系：\`${s.via.from}\` --${s.via.type}--> \`${s.via.to}\``);
    if (s.summary) lines.push(`- 职责：${s.summary}`);
    if (s.bullets?.length) {
      lines.push('- 伪代码要点：');
      s.bullets.forEach((b) => lines.push(`  1. ${b}`));
    }
    lines.push('');
  });
  lines.push('## 关系边清单');
  lines.push('| from | type | to |');
  lines.push('|------|------|-----|');
  for (const e of p.edges) {
    lines.push(`| ${e.from} | ${e.type} | ${e.to} |`);
  }
  return lines.join('\n');
}
```

- [ ] **Step 4: Pass tests**

```powershell
npx vitest run src/lib/pipeline.test.ts
```

Expected: PASS.

- [ ] **Step 5: PipelineCanvas UI**

```tsx
import { useMemo, useState } from 'react';
import type { Catalog } from '../lib/types';
import { buildPipeline, exportPipelineMarkdown, type PipelineStart } from '../lib/pipeline';

export function PipelineCanvas({
  catalog,
  initial,
  onClose,
  onOpenFile,
}: {
  catalog: Catalog;
  initial: PipelineStart;
  onClose: () => void;
  onOpenFile: (id: string) => void;
}) {
  const [start, setStart] = useState<PipelineStart>(initial);
  const [depth, setDepth] = useState(3);
  const [apiText, setApiText] = useState(initial.kind === 'api' ? initial.id : '');

  const pipeline = useMemo(() => buildPipeline(catalog, start, depth), [catalog, start, depth]);
  const md = useMemo(() => exportPipelineMarkdown(pipeline), [pipeline]);

  return (
    <div className="pipeline-overlay">
      <div className="pipeline-panel">
        <header>
          <strong>摘流水线</strong>
          <button onClick={onClose}>关闭</button>
        </header>
        <div className="pipeline-controls">
          <label>
            起点
            <select
              value={start.kind}
              onChange={(e) => {
                const kind = e.target.value as 'file' | 'api';
                if (kind === 'file') setStart({ kind: 'file', id: initial.kind === 'file' ? initial.id : catalog.nodes[0]?.id });
                else setStart({ kind: 'api', id: apiText || catalog.apiIndex[0]?.path || '/' });
              }}
            >
              <option value="file">类型/文件</option>
              <option value="api">API</option>
            </select>
          </label>
          {start.kind === 'api' && (
            <input value={apiText} onChange={(e) => { setApiText(e.target.value); setStart({ kind: 'api', id: e.target.value }); }} placeholder="/api/..." />
          )}
          <label>
            深度
            <input type="number" min={1} max={6} value={depth} onChange={(e) => setDepth(Number(e.target.value))} />
          </label>
          <button
            onClick={async () => {
              await navigator.clipboard.writeText(md);
            }}
          >
            复制 Markdown
          </button>
          <a
            href={`data:text/markdown;charset=utf-8,${encodeURIComponent(md)}`}
            download="pipeline.md"
          >
            下载 .md
          </a>
        </div>
        <div className="pipeline-steps">
          {pipeline.steps.map((s, i) => (
            <article key={s.nodeId + i} className="step-card">
              <header>
                <span className="step-idx">{i + 1}</span>
                <button className="linkish" onClick={() => onOpenFile(s.nodeId)}>{s.label}</button>
                <span className="muted">{s.layer}</span>
              </header>
              {s.via && <div className="muted">{s.via.type}</div>}
              <p>{s.summary}</p>
              <ul>
                {(s.bullets || []).slice(0, 5).map((b, j) => (
                  <li key={j}>{b}</li>
                ))}
              </ul>
            </article>
          ))}
          {!pipeline.steps.length && <p className="muted">无静态关系边，可调整深度或换起点</p>}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 6: Wire pipelineOpen in App** with `initial` from `selectedId` or first api.

- [ ] **Step 7: CSS overlay**

```css
.pipeline-overlay {
  position: fixed; inset: 0; background: rgba(28,25,23,.35);
  display: flex; align-items: center; justify-content: center; z-index: 40;
}
.pipeline-panel {
  width: min(1100px, 94vw); height: min(80vh, 860px);
  background: var(--panel); border: 1px solid var(--line); border-radius: 8px;
  display: flex; flex-direction: column; overflow: hidden;
}
.pipeline-steps {
  display: flex; gap: 10px; overflow: auto; padding: 12px; flex: 1;
}
.step-card {
  min-width: 220px; max-width: 260px; border: 1px solid var(--line);
  border-radius: 6px; padding: 10px; background: #fff;
}
```

- [ ] **Step 8: Commit**

```powershell
git add docs/pseudocode/viewer
git commit -m "feat: data pipeline extract and markdown export"
```

---

### Task 7: Polish, README, Entry Links

**Files:**
- Create: `docs/pseudocode/viewer/README.md`
- Modify: `docs/pseudocode/README.md`
- Modify: `docs/pseudocode/graphs/interactive/index.html`
- Create: `docs/pseudocode/viewer/.gitignore` (`node_modules`, maybe keep `public/docs-files` untracked if huge)

- [ ] **Step 1: viewer README**

```markdown
# PIM 伪代码可视化工作台

## 本地运行

```bash
cd docs/pseudocode/viewer
npm install
npm run catalog
npm run dev
```

浏览器打开终端提示的本地 URL。

## 构建

```bash
npm run build
npx serve dist
```

## 功能

- 阅读：三栏树 / 伪代码 / 关系边
- 关系图：WebGL 力导向
- 摘流水线：文件或 API 起点，导出 Markdown
```

- [ ] **Step 2: Link from docs/pseudocode/README.md**

Add:

```markdown
- 可视化工作台：`viewer/`（`npm run dev`）
```

- [ ] **Step 3: Old interactive index banner**

At top of `graphs/interactive/index.html` body, add a link:

```html
<p style="padding:8px;margin:0;background:#f7f3eb;border-bottom:1px solid #e7e0d5">
  新版工作台：<a href="../../viewer/">docs/pseudocode/viewer</a>（需先 npm run dev/build）
</p>
```

- [ ] **Step 4: .gitignore in viewer**

```
node_modules
dist
public/docs-files
```

Keep `public/catalog.json` committed for convenience **or** generate in CI/docs note only — prefer commit catalog.json for open-without-catalog if size OK (~after build measure). If > 5MB, gitignore catalog too and require `npm run catalog`.

- [ ] **Step 5: Full test pass**

```powershell
cd docs/pseudocode/viewer
npm test
npm run build
```

Expected: tests green; `dist/` produced.

- [ ] **Step 6: Commit**

```powershell
git add docs/pseudocode/viewer docs/pseudocode/README.md docs/pseudocode/graphs/interactive/index.html
git commit -m "docs: polish pseudocode viewer entrypoints and README"
```

---

### Task 8: PR And Verification Checklist

**Files:** none

- [ ] **Step 1: Manual checklist**

1. Open 5 files: core, api, module, client-web, test — dual section works  
2. Graph pan/zoom with tests hidden  
3. Pipeline from file + from api, copy markdown  
4. Search filters tree  

- [ ] **Step 2: Push and PR**

```powershell
git push -u origin HEAD
gh pr create --title "feat: pseudocode visual workbench" --body "Vite/React viewer: read three-pane, graph mode, pipeline extract. See docs/pseudocode/viewer/README.md"
```

- [ ] **Step 3: Note CI**

If only `docs/**` and no workflow matches, state no GA checks.

---

## Plan Self-Review

| Spec item | Task |
|-----------|------|
| 双模式阅读/图 | Task 4–5 |
| 三栏浅色纸质 | Task 1, 4 |
| 数据流 C 双起点+导出 | Task 6 |
| catalog 构建 | Task 2 |
| 懒加载 md | Task 3 |
| 性能（过滤 tests、深度限制） | Task 5–6 |
| 不改业务源码 | 全程只动 docs/pseudocode/viewer (+ 入口链接) |
| 单元测试 pipeline/mdSplit | Task 3, 6 |

Placeholder scan: none intentional.  
Types: `Catalog`, `PipelineStart`, `WorkbenchMode`, `DocSection` consistent across tasks.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-12-pseudocode-visual-workbench.md`.

**两种执行方式：**

1. **Subagent-Driven（推荐）** — 每任务新子代理 + 两阶段审查  
2. **Inline Execution** — 本会话按 executing-plans 连续实现  

选哪个？

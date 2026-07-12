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
    loadCatalog()
      .then(setCatalog)
      .catch((e) => setError(String(e.message || e)));
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
      {pipelineOpen ? null : null}
    </div>
  );
}

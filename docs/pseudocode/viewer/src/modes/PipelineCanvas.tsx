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
          <button type="button" onClick={onClose}>
            关闭
          </button>
        </header>
        <div className="pipeline-controls">
          <label>
            起点
            <select
              value={start.kind}
              onChange={(e) => {
                const kind = e.target.value as 'file' | 'api';
                if (kind === 'file') {
                  setStart({
                    kind: 'file',
                    id: initial.kind === 'file' ? initial.id : catalog.nodes[0]?.id,
                  });
                } else {
                  setStart({ kind: 'api', id: apiText || catalog.apiIndex[0]?.path || '/' });
                }
              }}
            >
              <option value="file">类型/文件</option>
              <option value="api">API</option>
            </select>
          </label>
          {start.kind === 'api' && (
            <input
              value={apiText}
              onChange={(e) => {
                setApiText(e.target.value);
                setStart({ kind: 'api', id: e.target.value });
              }}
              placeholder="/api/..."
            />
          )}
          <label>
            深度
            <input
              type="number"
              min={1}
              max={6}
              value={depth}
              onChange={(e) => setDepth(Number(e.target.value))}
            />
          </label>
          <button
            type="button"
            onClick={async () => {
              await navigator.clipboard.writeText(md);
            }}
          >
            复制 Markdown
          </button>
          <a href={`data:text/markdown;charset=utf-8,${encodeURIComponent(md)}`} download="pipeline.md">
            下载 .md
          </a>
        </div>
        <div className="pipeline-steps">
          {pipeline.steps.map((s, i) => (
            <article key={s.nodeId + i} className="step-card">
              <header>
                <span className="step-idx">{i + 1}</span>
                <button type="button" className="linkish" onClick={() => onOpenFile(s.nodeId)}>
                  {s.label}
                </button>
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

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
        <button
          type="button"
          className={mode === 'read' ? 'active' : ''}
          onClick={() => onMode('read')}
        >
          阅读
        </button>
        <button
          type="button"
          className={mode === 'graph' ? 'active' : ''}
          onClick={() => onMode('graph')}
        >
          关系图
        </button>
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
          <option key={l} value={l}>
            {l}
          </option>
        ))}
      </select>
      <button type="button" className="primary" onClick={onPipeline}>
        摘流水线
      </button>
      {stats && (
        <span className="muted">
          {stats.nodeCount} 节点 · {stats.edgeCount} 边
        </span>
      )}
    </header>
  );
}

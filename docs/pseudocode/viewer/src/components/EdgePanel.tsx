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
      <button type="button" className="primary" onClick={onPipeline}>
        从这里摘流水线
      </button>
      <h3>关系边 ({list.length})</h3>
      <ul>
        {list.map((e, i) => {
          const other = e.from === fileId ? e.to : e.from;
          const dir = e.from === fileId ? '→' : '←';
          const node = getNode(catalog, other);
          return (
            <li key={i}>
              <span className="edge-type">{e.type}</span> {dir}{' '}
              <button type="button" className="linkish" onClick={() => onOpen(other)}>
                {node?.label || other}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

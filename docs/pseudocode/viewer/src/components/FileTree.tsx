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
                  type="button"
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

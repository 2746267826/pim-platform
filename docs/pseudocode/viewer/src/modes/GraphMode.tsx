import { useEffect, useRef } from 'react';
import { Graph, NodeEvent } from '@antv/g6';
import type { Catalog } from '../lib/types';
import { filterGraphCatalog } from '../lib/graphData';

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
  onOpenInRead,
}: {
  catalog: Catalog;
  selectedId: string | null;
  hideTests: boolean;
  onSelect: (id: string) => void;
  onOpenInRead: (id: string) => void;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph | null>(null);
  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;

  useEffect(() => {
    if (!ref.current) return;
    const { nodes, edges } = filterGraphCatalog(catalog, hideTests);

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

    graph.on(NodeEvent.CLICK, (evt) => {
      const id = (evt as { target?: { id?: string } })?.target?.id;
      if (id) onSelectRef.current(String(id));
    });

    void graph.render();
    graphRef.current = graph;
    return () => {
      graph.destroy();
      graphRef.current = null;
    };
    // selectedId only used for initial size; avoid full rebuild on every click
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [catalog, hideTests]);

  return (
    <div className="graph-mode">
      <div className="graph-canvas" ref={ref} />
      <aside className="graph-side">
        <h3>选中</h3>
        <p className="mono">{selectedId || '点击节点'}</p>
        {selectedId && (
          <button type="button" className="primary" onClick={() => onOpenInRead(selectedId)}>
            在阅读中打开
          </button>
        )}
      </aside>
    </div>
  );
}

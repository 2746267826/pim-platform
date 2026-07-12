import { describe, it, expect } from 'vitest';
import { filterGraphCatalog } from './graphData';
import type { Catalog } from './types';

const catalog: Catalog = {
  generated: 'test',
  nodes: [
    { id: 'a', label: 'A', path: 'a', doc: 'a.md', layer: 'core', kind: 'file' },
    { id: 'b', label: 'B', path: 'b', doc: 'b.md', layer: 'tests', kind: 'file' },
    { id: 'c', label: 'C', path: 'c', doc: 'c.md', layer: 'api', kind: 'file' },
  ],
  edges: [
    { from: 'a', to: 'c', type: 'depends_on' },
    { from: 'b', to: 'a', type: 'tests' },
    { from: 'c', to: 'missing', type: 'calls' },
  ],
  apiIndex: [],
  stats: { nodeCount: 3, edgeCount: 3, docCount: 3 },
};

describe('filterGraphCatalog', () => {
  it('hides tests layer nodes and dangling edges when hideTests', () => {
    const { nodes, edges } = filterGraphCatalog(catalog, true);
    expect(nodes.map((n) => n.id)).toEqual(['a', 'c']);
    expect(edges).toEqual([{ from: 'a', to: 'c', type: 'depends_on' }]);
  });

  it('keeps tests nodes when hideTests is false', () => {
    const { nodes, edges } = filterGraphCatalog(catalog, false);
    expect(nodes.map((n) => n.id)).toEqual(['a', 'b', 'c']);
    expect(edges).toHaveLength(2);
  });
});

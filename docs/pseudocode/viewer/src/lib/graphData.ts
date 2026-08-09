import type { Catalog, GraphEdge, GraphNode } from './types';

export function filterGraphCatalog(
  catalog: Catalog,
  hideTests: boolean,
): { nodes: GraphNode[]; edges: GraphEdge[] } {
  const nodes = catalog.nodes.filter((n) => !(hideTests && n.layer === 'tests'));
  const ids = new Set(nodes.map((n) => n.id));
  const edges = catalog.edges.filter((e) => ids.has(e.from) && ids.has(e.to));
  return { nodes, edges };
}

/** Group graph nodes by domain key (uses `layer` as domain). */
export function groupNodesByDomain(nodes: GraphNode[]): Map<string, GraphNode[]> {
  const map = new Map<string, GraphNode[]>();
  for (const n of nodes) {
    const domain = n.layer || 'other';
    const list = map.get(domain);
    if (list) list.push(n);
    else map.set(domain, [n]);
  }
  return map;
}

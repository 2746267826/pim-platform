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

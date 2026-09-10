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

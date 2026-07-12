export type EdgeType = 'depends_on' | 'calls' | 'implements' | 'extends' | 'tests' | 'http' | string;

export interface GraphNode {
  id: string;
  label: string;
  path: string;
  doc: string;
  layer: string;
  kind: string;
  title?: string;
  summary?: string;
  functionBullets?: string[];
}

export interface GraphEdge {
  from: string;
  to: string;
  type: EdgeType;
}

export interface ApiIndexEntry {
  path: string;
  method: string;
  nodeId: string;
}

export interface Catalog {
  generated: string;
  nodes: GraphNode[];
  edges: GraphEdge[];
  apiIndex: ApiIndexEntry[];
  stats: { nodeCount: number; edgeCount: number; docCount: number };
}

export type WorkbenchMode = 'read' | 'graph';
export type DocSection = 'function' | 'line';

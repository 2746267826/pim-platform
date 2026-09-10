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

export interface WorkflowStep {
  nodeId: string;
  label: string;
  layer: string;
  summary?: string;
  order: number;
}

export interface WorkflowEdge {
  from: string;
  to: string;
  type: string;
}

export interface Workflow {
  id: string;
  title: string;
  domain: string; // e.g. api, mobile, calendar, auth, core, other
  steps: WorkflowStep[];
  edges: WorkflowEdge[];
}

export type WorkbenchMode = 'read' | 'graph' | 'workflow';
export type DocSection = 'function' | 'line';

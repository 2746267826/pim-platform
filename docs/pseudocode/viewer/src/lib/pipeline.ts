import type { Catalog, EdgeType, GraphEdge, GraphNode } from './types';

export type PipelineStart =
  | { kind: 'file'; id: string }
  | { kind: 'api'; id: string };

export interface PipelineStep {
  nodeId: string;
  label: string;
  layer: string;
  summary: string;
  via?: { from: string; to: string; type: EdgeType };
  fixed?: boolean;
  bullets?: string[];
}

export interface Pipeline {
  start: PipelineStart;
  depth: number;
  steps: PipelineStep[];
  edges: GraphEdge[];
}

const WALK_TYPES = new Set(['calls', 'depends_on', 'http', 'implements']);

export function buildPipeline(catalog: Catalog, start: PipelineStart, depth: number): Pipeline {
  const d = Math.min(6, Math.max(1, depth));
  let rootId = start.kind === 'file' ? start.id : '';
  if (start.kind === 'api') {
    const hit =
      catalog.apiIndex.find((a) => a.path === start.id) ||
      catalog.edges.find((e) => e.type === 'http' && String(e.to) === start.id);
    rootId = (hit && 'nodeId' in hit ? hit.nodeId : (hit as GraphEdge | undefined)?.from) || '';
  }
  const nodeById = new Map(catalog.nodes.map((n) => [n.id, n]));
  if (!rootId || !nodeById.has(rootId)) {
    return { start, depth: d, steps: [], edges: [] };
  }

  const adj = new Map<string, GraphEdge[]>();
  for (const e of catalog.edges) {
    if (!WALK_TYPES.has(String(e.type))) continue;
    if (!adj.has(e.from)) adj.set(e.from, []);
    adj.get(e.from)!.push(e);
  }

  const steps: PipelineStep[] = [];
  const usedEdges: GraphEdge[] = [];
  const seen = new Set<string>();
  const queue: { id: string; dist: number; via?: GraphEdge }[] = [{ id: rootId, dist: 0 }];

  while (queue.length) {
    const cur = queue.shift()!;
    if (seen.has(cur.id)) continue;
    seen.add(cur.id);
    const n = nodeById.get(cur.id)!;
    const bullets = (n as GraphNode).functionBullets || [];
    steps.push({
      nodeId: n.id,
      label: n.label,
      layer: n.layer,
      summary: n.summary || '',
      via: cur.via ? { from: cur.via.from, to: cur.via.to, type: cur.via.type } : undefined,
      bullets,
    });
    if (cur.via) usedEdges.push(cur.via);
    if (cur.dist >= d) continue;
    for (const e of adj.get(cur.id) || []) {
      if (!nodeById.has(e.to)) continue;
      if (!seen.has(e.to)) queue.push({ id: e.to, dist: cur.dist + 1, via: e });
    }
  }

  const capped = steps.slice(0, 80);
  return { start, depth: d, steps: capped, edges: usedEdges };
}

export function exportPipelineMarkdown(p: Pipeline): string {
  const title = p.steps[0]?.label || (p.start.kind === 'api' ? p.start.id : p.start.id);
  const lines: string[] = [
    `# 数据流水线：${title}`,
    `- 生成时间：${new Date().toISOString()}`,
    `- 起点类型：${p.start.kind}`,
    `- 深度：${p.depth}`,
    '',
    '## 步骤',
  ];
  p.steps.forEach((s, i) => {
    lines.push(`### ${i + 1}. ${s.label}`);
    lines.push(`- 节点：\`${s.nodeId}\``);
    if (s.via) lines.push(`- 关系：\`${s.via.from}\` --${s.via.type}--> \`${s.via.to}\``);
    if (s.summary) lines.push(`- 职责：${s.summary}`);
    if (s.bullets?.length) {
      lines.push('- 伪代码要点：');
      s.bullets.forEach((b) => lines.push(`  1. ${b}`));
    }
    lines.push('');
  });
  lines.push('## 关系边清单');
  lines.push('| from | type | to |');
  lines.push('|------|------|-----|');
  for (const e of p.edges) {
    lines.push(`| ${e.from} | ${e.type} | ${e.to} |`);
  }
  return lines.join('\n');
}

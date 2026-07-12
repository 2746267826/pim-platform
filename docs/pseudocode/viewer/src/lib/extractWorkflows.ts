import type { Catalog, Workflow, GraphNode, GraphEdge, WorkflowStep, WorkflowEdge } from './types';

const WALK = new Set(['calls', 'depends_on', 'http', 'implements']);

function domainOf(n: GraphNode): string {
  if (n.layer === 'api' || /Endpoints?\.cs$|Program\.cs$/.test(n.id)) return 'api';
  if (n.layer.includes('mobile') || n.id.includes('Mobile') || n.id.includes('client-android')) return 'mobile';
  if (n.layer.includes('calendar') || n.id.includes('Calendar')) return 'calendar';
  if (n.layer.includes('pctracker') || n.id.includes('PcTracker') || n.id.includes('pc-')) return 'pc-tracker';
  if (n.layer.includes('files') || n.id.includes('Files')) return 'files';
  if (n.layer.includes('quicknotes') || n.id.includes('QuickNote')) return 'quicknotes';
  if (n.layer.includes('stats')) return 'stats';
  if (n.id.includes('Auth') || n.id.includes('auth')) return 'auth';
  if (n.layer === 'core' || n.layer === 'infrastructure') return 'platform';
  if (n.layer === 'client-web') return 'client-web';
  if (n.layer === 'client-windows') return 'client-windows';
  if (n.layer === 'tests') return 'tests';
  return 'other';
}

function isSeed(n: GraphNode, outboundHttpFrom: Set<string>): boolean {
  if (n.kind === 'endpoint') return true;
  if (/Endpoints?\.cs$/.test(n.id)) return true;
  if (n.id.endsWith('Program.cs')) return true;
  if (n.id.endsWith('Module.cs')) return true;
  if (outboundHttpFrom.has(n.id)) return true;
  if (n.layer === 'api') return true;
  return false;
}

function bfsWorkflow(
  seed: GraphNode,
  nodeById: Map<string, GraphNode>,
  outEdges: Map<string, GraphEdge[]>,
  maxSteps: number,
): Workflow {
  const visited = new Set<string>();
  const steps: WorkflowStep[] = [];
  const usedEdges: WorkflowEdge[] = [];
  const queue: string[] = [seed.id];
  visited.add(seed.id);

  while (queue.length > 0 && steps.length < maxSteps) {
    const id = queue.shift()!;
    const node = nodeById.get(id);
    if (!node) continue;

    steps.push({
      nodeId: node.id,
      label: node.label,
      layer: node.layer,
      summary: node.summary,
      order: steps.length,
    });

    if (steps.length >= maxSteps) break;

    const edges = outEdges.get(id) ?? [];
    for (const e of edges) {
      if (visited.has(e.to)) continue;
      if (!nodeById.has(e.to)) continue;
      visited.add(e.to);
      usedEdges.push({ from: e.from, to: e.to, type: e.type });
      queue.push(e.to);
    }
  }

  return {
    id: `wf-${seed.id}`,
    title: seed.label || seed.title || seed.id,
    domain: domainOf(seed),
    steps,
    edges: usedEdges,
  };
}

function stepKey(wf: Workflow): string {
  return [...new Set(wf.steps.map((s) => s.nodeId))].sort().join('\0');
}

export function extractWorkflows(
  catalog: Catalog,
  opts?: { hideTests?: boolean; maxSteps?: number; maxWorkflows?: number },
): Workflow[] {
  const hideTests = opts?.hideTests !== false;
  const maxSteps = opts?.maxSteps ?? 24;
  const maxWorkflows = opts?.maxWorkflows ?? 80;

  const nodes = catalog.nodes.filter((n) => !(hideTests && n.layer === 'tests'));
  const nodeIds = new Set(nodes.map((n) => n.id));
  const nodeById = new Map(nodes.map((n) => [n.id, n]));

  const walkEdges = catalog.edges.filter(
    (e) => nodeIds.has(e.from) && nodeIds.has(e.to) && WALK.has(e.type),
  );

  const outEdges = new Map<string, GraphEdge[]>();
  const outboundHttpFrom = new Set<string>();
  for (const e of walkEdges) {
    const list = outEdges.get(e.from);
    if (list) list.push(e);
    else outEdges.set(e.from, [e]);
    if (e.type === 'http') outboundHttpFrom.add(e.from);
  }

  const seeds = nodes.filter((n) => isSeed(n, outboundHttpFrom));
  const limitedSeeds = seeds.slice(0, maxWorkflows);

  const raw: Workflow[] = [];
  for (const seed of limitedSeeds) {
    raw.push(bfsWorkflow(seed, nodeById, outEdges, maxSteps));
  }

  // Deduplicate: same set of step nodeIds → keep longer
  const byKey = new Map<string, Workflow>();
  for (const wf of raw) {
    const key = stepKey(wf);
    const existing = byKey.get(key);
    if (!existing || wf.steps.length > existing.steps.length) {
      byKey.set(key, wf);
    }
  }

  const workflows = [...byKey.values()];
  workflows.sort((a, b) => {
    const d = a.domain.localeCompare(b.domain);
    if (d !== 0) return d;
    return a.title.localeCompare(b.title);
  });

  return workflows.slice(0, maxWorkflows);
}

export function filterWorkflows(workflows: Workflow[], query: string, domain: string): Workflow[] {
  const q = query.trim().toLowerCase();
  return workflows.filter((wf) => {
    if (domain && domain !== 'all' && wf.domain !== domain) return false;
    if (!q) return true;
    if (wf.title.toLowerCase().includes(q)) return true;
    if (wf.domain.toLowerCase().includes(q)) return true;
    if (wf.id.toLowerCase().includes(q)) return true;
    return wf.steps.some(
      (s) =>
        s.label.toLowerCase().includes(q) ||
        s.nodeId.toLowerCase().includes(q) ||
        (s.summary?.toLowerCase().includes(q) ?? false),
    );
  });
}

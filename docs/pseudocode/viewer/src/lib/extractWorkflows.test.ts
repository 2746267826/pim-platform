import { describe, it, expect } from 'vitest';
import { extractWorkflows, filterWorkflows } from './extractWorkflows';
import type { Catalog } from './types';

const catalog: Catalog = {
  generated: 't',
  nodes: [
    { id: 'A', label: 'A', path: 'A', doc: '', layer: 'api', kind: 'endpoint', summary: '入口' },
    { id: 'B', label: 'B', path: 'B', doc: '', layer: 'module.mobile', kind: 'service', summary: '处理' },
    { id: 'C', label: 'C', path: 'C', doc: '', layer: 'infrastructure', kind: 'entity', summary: '存储' },
    { id: 'T', label: 'T', path: 'T', doc: '', layer: 'tests', kind: 'file', summary: '测试' },
  ],
  edges: [
    { from: 'A', to: 'B', type: 'calls' },
    { from: 'B', to: 'C', type: 'depends_on' },
    { from: 'T', to: 'A', type: 'tests' },
  ],
  apiIndex: [],
  stats: { nodeCount: 4, edgeCount: 3, docCount: 4 },
};

describe('extractWorkflows', () => {
  it('seed A produces workflow steps A,B,C in order', () => {
    const workflows = extractWorkflows(catalog, { hideTests: true });
    const wf = workflows.find((w) => w.steps[0]?.nodeId === 'A');
    expect(wf).toBeDefined();
    expect(wf!.steps.map((s) => s.nodeId)).toEqual(['A', 'B', 'C']);
  });

  it('hideTests filters test nodes', () => {
    const withTestSeed: Catalog = {
      ...catalog,
      nodes: [
        ...catalog.nodes,
        { id: 'TS', label: 'TestSeed', path: 'TS', doc: '', layer: 'tests', kind: 'endpoint' },
      ],
      edges: [...catalog.edges, { from: 'TS', to: 'B', type: 'calls' }],
    };

    const hidden = extractWorkflows(withTestSeed, { hideTests: true });
    expect(hidden.every((w) => w.steps.every((s) => s.layer !== 'tests'))).toBe(true);

    const shown = extractWorkflows(withTestSeed, { hideTests: false });
    expect(shown.some((w) => w.steps.some((s) => s.layer === 'tests'))).toBe(true);
  });
});

describe('filterWorkflows', () => {
  it('filters by query', () => {
    const workflows = extractWorkflows(catalog, { hideTests: true });
    const byQuery = filterWorkflows(workflows, 'A', 'all');
    expect(byQuery.length).toBeGreaterThan(0);
    expect(
      byQuery.every(
        (w) =>
          w.title.toLowerCase().includes('a') ||
          w.steps.some(
            (s) => s.label.toLowerCase().includes('a') || s.nodeId.toLowerCase().includes('a'),
          ),
      ),
    ).toBe(true);

    expect(filterWorkflows(workflows, 'zzz-no-match', 'all')).toEqual([]);
    expect(filterWorkflows(workflows, '', 'api').every((w) => w.domain === 'api')).toBe(true);
  });
});

import { describe, it, expect } from 'vitest';
import { buildPipeline, exportPipelineMarkdown } from './pipeline';
import type { Catalog } from './types';

const catalog: Catalog = {
  generated: 't',
  nodes: [
    { id: 'A', label: 'A', path: 'A', doc: '', layer: 'api', kind: 'endpoint', summary: '入口' },
    { id: 'B', label: 'B', path: 'B', doc: '', layer: 'module.mobile', kind: 'service', summary: '处理' },
    { id: 'C', label: 'C', path: 'C', doc: '', layer: 'infrastructure', kind: 'entity', summary: '存储' },
  ],
  edges: [
    { from: 'A', to: 'B', type: 'calls' },
    { from: 'B', to: 'C', type: 'depends_on' },
    { from: 'A', to: '/api/x', type: 'http' },
  ],
  apiIndex: [{ path: '/api/x', method: 'POST', nodeId: 'A' }],
  stats: { nodeCount: 3, edgeCount: 3, docCount: 3 },
};

describe('buildPipeline', () => {
  it('walks outbound edges up to depth', () => {
    const p = buildPipeline(catalog, { kind: 'file', id: 'A' }, 2);
    expect(p.steps.map((s) => s.nodeId)).toEqual(['A', 'B', 'C']);
  });

  it('starts from api index', () => {
    const p = buildPipeline(catalog, { kind: 'api', id: '/api/x' }, 1);
    expect(p.steps[0].nodeId).toBe('A');
  });

  it('exports markdown', () => {
    const p = buildPipeline(catalog, { kind: 'file', id: 'A' }, 2);
    const md = exportPipelineMarkdown(p);
    expect(md).toContain('# 数据流水线');
    expect(md).toContain('A');
    expect(md).toContain('calls');
  });
});

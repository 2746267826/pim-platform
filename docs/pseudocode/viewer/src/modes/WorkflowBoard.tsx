import { useMemo, useState } from 'react';
import type { Catalog } from '../lib/types';
import { extractWorkflows, filterWorkflows } from '../lib/extractWorkflows';
import { WorkflowLane } from '../components/WorkflowLane';

export function WorkflowBoard({
  catalog,
  selectedId,
  query,
  hideTests,
  onSelect,
  onOpenInRead,
  onPipelineFrom,
}: {
  catalog: Catalog;
  selectedId: string | null;
  query: string;
  hideTests: boolean;
  onSelect: (id: string) => void;
  onOpenInRead: (id: string) => void;
  onPipelineFrom: (id: string) => void;
}) {
  const [domain, setDomain] = useState('all');

  const workflows = useMemo(
    () => extractWorkflows(catalog, { hideTests }),
    [catalog, hideTests],
  );

  const domains = useMemo(() => {
    const set = new Set(workflows.map((w) => w.domain));
    return ['all', ...[...set].sort()];
  }, [workflows]);

  const visible = useMemo(
    () => filterWorkflows(workflows, query, domain),
    [workflows, query, domain],
  );

  const selectedNode = useMemo(
    () => (selectedId ? catalog.nodes.find((n) => n.id === selectedId) : undefined),
    [catalog.nodes, selectedId],
  );

  const selectedStep = useMemo(() => {
    if (!selectedId) return undefined;
    for (const wf of visible) {
      const step = wf.steps.find((s) => s.nodeId === selectedId);
      if (step) return { workflow: wf, step };
    }
    for (const wf of workflows) {
      const step = wf.steps.find((s) => s.nodeId === selectedId);
      if (step) return { workflow: wf, step };
    }
    return undefined;
  }, [selectedId, visible, workflows]);

  return (
    <div className="workflow-board">
      <div className="workflow-board-main">
        <div className="domain-chips">
          {domains.map((d) => (
            <button
              key={d}
              type="button"
              className={domain === d ? 'active' : undefined}
              onClick={() => setDomain(d)}
            >
              {d === 'all' ? '全部' : d}
            </button>
          ))}
        </div>
        {visible.length === 0 ? (
          <p className="muted">无匹配工作流，可调整域筛选或搜索关键词</p>
        ) : (
          visible.map((wf) => (
            <WorkflowLane
              key={wf.id}
              workflow={wf}
              selectedId={selectedId}
              onSelectStep={onSelect}
            />
          ))
        )}
      </div>
      <aside className="workflow-board-side">
        <h3>选中步骤</h3>
        {!selectedId ? (
          <p className="muted">点击步骤查看详情</p>
        ) : (
          <>
            <p className="mono">{selectedStep?.step.label || selectedNode?.label || selectedId}</p>
            <p className="muted">
              {selectedStep?.step.layer || selectedNode?.layer || '—'}
              {selectedStep?.workflow ? ` · ${selectedStep.workflow.domain}` : ''}
            </p>
            {(selectedStep?.step.summary || selectedNode?.summary) && (
              <p>{selectedStep?.step.summary || selectedNode?.summary}</p>
            )}
            <button type="button" className="primary" onClick={() => onOpenInRead(selectedId)}>
              在阅读中打开
            </button>
            <button type="button" onClick={() => onPipelineFrom(selectedId)}>
              从该步摘流水线
            </button>
          </>
        )}
      </aside>
    </div>
  );
}

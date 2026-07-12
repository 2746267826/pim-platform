import type { Workflow } from '../lib/types';

export function WorkflowLane({
  workflow,
  selectedId,
  onSelectStep,
}: {
  workflow: Workflow;
  selectedId: string | null;
  onSelectStep: (nodeId: string) => void;
}) {
  const steps = [...workflow.steps].sort((a, b) => a.order - b.order);

  return (
    <section className="workflow-lane">
      <header className="workflow-lane-header">
        <strong>{workflow.title}</strong>
        <span className="workflow-domain">{workflow.domain}</span>
      </header>
      <div className="workflow-steps-row">
        {steps.map((step, i) => (
          <span key={step.nodeId} style={{ display: 'contents' }}>
            {i > 0 && <span className="workflow-arrow">→</span>}
            <button
              type="button"
              className={selectedId === step.nodeId ? 'workflow-step active' : 'workflow-step'}
              onClick={() => onSelectStep(step.nodeId)}
              title={step.summary || step.label}
            >
              <div className="step-label">{step.label}</div>
              <div className="step-layer">{step.layer}</div>
            </button>
          </span>
        ))}
      </div>
    </section>
  );
}

import type { AgentStepVO, AgentRunVO } from '@/types/ai/agent';

export interface AgentPlanStep {
  stepId: number;
  name: string;
  tool: string;
  input: Record<string, unknown>;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'DONE';
  output?: unknown;
}

export function mapRunToPlanSteps(run: AgentRunVO | null): AgentPlanStep[] {
  if (!run?.steps?.length) return [];
  return run.steps.map((step, index) => ({
    stepId: step.index ?? index + 1,
    name: step.title,
    tool: step.tool || '',
    input: {},
    status: normalizeStepStatus(step.status),
    output: step.outputPreview
  }));
}

export function getPlanProgress(run: AgentRunVO | null): number {
  const steps = run?.steps ?? [];
  if (!steps.length) return 0;
  const done = steps.filter((s) => s.status === 'DONE' || s.status === 'SUCCEEDED').length;
  return Math.round((done / steps.length) * 100);
}

function normalizeStepStatus(status?: string): AgentPlanStep['status'] {
  if (status === 'DONE' || status === 'SUCCEEDED') return 'DONE';
  if (status === 'FAILED') return 'FAILED';
  if (status === 'RUNNING') return 'RUNNING';
  return 'PENDING';
}

export function extractPendingSteps(steps: AgentStepVO[]): AgentStepVO[] {
  return steps.filter((s) => s.status !== 'DONE' && s.status !== 'SUCCEEDED');
}

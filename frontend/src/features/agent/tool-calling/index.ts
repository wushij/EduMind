import type { AgentStepVO } from '@/types/ai/agent';

export interface AgentToolCallRecord {
  tool: string;
  status: string;
  output?: string;
  stepIndex: number;
}

export function extractToolCalls(steps: AgentStepVO[]): AgentToolCallRecord[] {
  return steps
    .filter((step) => Boolean(step.tool))
    .map((step) => ({
      tool: step.tool!,
      status: step.status,
      output: step.outputPreview,
      stepIndex: step.index
    }));
}

export function summarizeToolCalling(steps: AgentStepVO[]) {
  const calls = extractToolCalls(steps);
  const succeeded = calls.filter((c) => c.status === 'DONE' || c.status === 'SUCCEEDED').length;
  return {
    total: calls.length,
    succeeded,
    failed: calls.length - succeeded,
    calls
  };
}

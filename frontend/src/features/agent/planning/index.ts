export interface AgentPlanStep {
  stepId: number;
  name: string;
  tool: string;
  input: Record<string, any>;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  output?: any;
}

export class AgentPlanningFeature {
  public static createStep(id: number, name: string, tool: string, input: Record<string, any>): AgentPlanStep {
    return {
      stepId: id,
      name,
      tool,
      input,
      status: 'PENDING'
    };
  }
}

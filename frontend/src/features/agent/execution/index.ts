import { AgentPlanStep } from '../planning';

export class AgentExecutionFeature {
  public static executeStep(step: AgentPlanStep): Promise<AgentPlanStep> {
    step.status = 'RUNNING';
    return new Promise(resolve => {
      setTimeout(() => {
        step.status = 'COMPLETED';
        step.output = { message: `Step ${step.name} finished successfully` };
        resolve(step);
      }, 500);
    });
  }
}

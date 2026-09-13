import { useAgentRun } from '@/composables/ai/useAgentRun';

/** Agent 执行编排 — 封装 useAgentRun 供页面层调用 */
export function useAgentExecution() {
  const agentRun = useAgentRun();

  async function executeGoal(params: {
    agentCode: string;
    goal: string;
    courseId?: number;
  }) {
    return agentRun.runAgent({
      agentCode: params.agentCode,
      goal: params.goal,
      courseId: params.courseId
    });
  }

  return {
    ...agentRun,
    executeGoal
  };
}

export { useAgentRun };

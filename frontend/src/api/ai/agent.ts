import { get, post } from '@/core/http/request';
import type {
  AgentRunCreateRequest,
  AgentRunStartResponse,
  AgentRunVO,
  AgentVO
} from '@/types/ai/agent';

export const listAgents = () => get<AgentVO[]>('/ai/agents', undefined, { silent: true });

export const startAgentRun = (data: AgentRunCreateRequest) =>
  post<AgentRunStartResponse>('/ai/agent/runs', data);

export const getAgentRun = (runId: string) =>
  get<AgentRunVO>(`/ai/agent/runs/${runId}`);

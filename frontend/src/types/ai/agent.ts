export interface AgentVO {
  code: string;
  name: string;
  status: string;
  modelKey: string;
  toolCount: number;
  totalRuns: number;
  successRate: number;
}

export interface AgentRunCreateRequest {
  agentCode: string;
  goal: string;
  courseId?: number;
  context?: Record<string, unknown>;
}

export interface AgentRunStartResponse {
  runId: string;
  status: string;
}

export interface AgentStepVO {
  index: number;
  type: string;
  title: string;
  tool: string;
  status: string;
  outputPreview: string;
}

export interface AgentCitationVO {
  documentName?: string;
  pageNo?: number;
  chunkId?: number;
  score?: number;
  excerpt?: string;
  chunkIndex?: number;
}

export interface AgentRunResultVO {
  answer?: string;
  citations?: AgentCitationVO[];
  grading?: string;
  weakPoints?: unknown;
  advice?: string;
  count?: number;
  questionIds?: number[];
}

export interface AgentRunVO {
  runId: string;
  status: string;
  steps: AgentStepVO[];
  result?: AgentRunResultVO & Record<string, unknown>;
}

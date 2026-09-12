export interface PromptTemplate {
  id: number;
  code: string;
  name: string;
  category: 'teaching' | 'question' | 'grading' | 'rag' | 'agent' | 'common';
  description: string;
  systemPrompt: string;
  userPromptTemplate: string;
  variables: { name: string; label: string; defaultValue?: string; required?: boolean }[];
  version: string;
  status: 'PUBLISHED' | 'DRAFT' | 'ARCHIVED';
  boundModel: string;
  temperature: number;
  maxTokens: number;
  callCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface PromptVersionHistory {
  version: string;
  systemPrompt: string;
  userPromptTemplate: string;
  status: 'PUBLISHED' | 'ARCHIVED';
  updatedBy: string;
  updatedAt: string;
  changeLog?: string;
}

export interface PromptTestRequest {
  systemPrompt: string;
  userPromptTemplate: string;
  variables: Record<string, string>;
  model: string;
  temperature: number;
}

export interface PromptTestResponse {
  renderedUserPrompt: string;
  output: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  durationMs: number;
}

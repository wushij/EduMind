export interface GlobalAssistantMessage {
  role: 'user' | 'assistant';
  content: string;
  intent?: string;
  intentDesc?: string;
  targetCode?: string;
  streaming?: boolean;
}

export interface GlobalAssistantChatRequest {
  message: string;
  courseId?: number;
  conversationId?: string;
}

export interface GlobalAssistantAskResponse {
  conversationId: string;
  intent: string;
  intentDesc: string;
  targetCode?: string;
  content: string;
  citations?: unknown[];
}

export interface GlobalAssistantIntentEvent {
  route?: string;
  agentCode?: string;
  confidence?: number;
  slots?: Record<string, unknown>;
}

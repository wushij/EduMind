export interface Citation {
  documentName: string;
  page?: number;
  chunkSnippet: string;
}

export interface AIMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  isStreaming?: boolean;
  citations?: Citation[];
}

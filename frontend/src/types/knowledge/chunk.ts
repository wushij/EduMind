export interface DocumentChunk {
  id: string;
  documentId: number;
  content: string;
  page: number;
  score?: number;
}

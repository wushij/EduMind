import { DocumentChunk } from './chunk';

export interface RetrievalResult {
  query: string;
  chunks: DocumentChunk[];
  costMs: number;
}

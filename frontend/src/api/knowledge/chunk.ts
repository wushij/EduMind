import { get } from '@/core/http/request';
import { DocumentChunk } from '@/types/knowledge/chunk';

export const getChunks = (docId: number) => get<DocumentChunk[]>(`/documents/${docId}/chunks`);

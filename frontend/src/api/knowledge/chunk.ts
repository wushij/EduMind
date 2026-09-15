import { get, post } from '@/core/http/request';
import { DocumentChunk, ChunkQueryRequest, ChunkStatsVO, ChunkOperationResult } from '@/types/knowledge/chunk';

interface PageResult<T> {
  total?: number;
  page?: number;
  pageNum?: number;
  pageSize?: number;
  list?: T[];
}

interface ChunkTaskResponse {
  taskId?: string;
  status?: string;
  chunkCount?: number;
}

const EMPTY_CHUNK_STATS: ChunkStatsVO = {
  totalChunks: 0,
  indexedChunks: 0,
  pendingChunks: 0,
  failedChunks: 0,
  avgTokens: 0,
  totalTokens: 0
};

function mapChunk(raw: Record<string, unknown>): DocumentChunk {
  return {
    id: raw.id as number | string,
    documentId: raw.documentId as number,
    documentName: raw.documentName as string | undefined,
    chunkIndex: (raw.chunkIndex as number) ?? 0,
    content: (raw.content as string) || '',
    tokenCount: (raw.tokenEstimate as number) ?? (raw.tokenCount as number) ?? 0,
    heading: raw.heading as string | undefined,
    pageNo: raw.pageNo as number | undefined,
    status: (raw.status as DocumentChunk['status']) || 'PENDING',
    charCount: raw.charCount as number | undefined,
    createdAt: raw.createTime as string | undefined
  };
}

export const getChunks = async (docId?: number, params?: ChunkQueryRequest): Promise<DocumentChunk[]> => {
  if (!docId) {
    return [];
  }

  const res = await get<PageResult<Record<string, unknown>>>(`/documents/${docId}/chunks`, {
    page: params?.page || 1,
    pageSize: params?.pageSize || 50,
    keyword: params?.keyword
  });

  const list = res?.data?.list;
  if (list) {
    return list.map(mapChunk);
  }
  if (res?.data && Array.isArray(res.data)) {
    return (res.data as Record<string, unknown>[]).map(mapChunk);
  }
  return [];
};

export const triggerChunk = async (docId: number): Promise<ChunkOperationResult> => {
  const res = await post<ChunkTaskResponse>(`/documents/${docId}/chunk`);
  const data = res?.data;
  return {
    success: true,
    message: '文档切片完成',
    chunkCount: data?.chunkCount
  };
};

export const getChunkStats = async (kbId?: number): Promise<ChunkStatsVO> => {
  if (!kbId) {
    return { ...EMPTY_CHUNK_STATS };
  }
  const res = await get<ChunkStatsVO>(`/knowledge-bases/${kbId}/chunk-stats`);
  return res?.data ?? { ...EMPTY_CHUNK_STATS };
};

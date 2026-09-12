import { get, post } from '@/core/http/request';
import { DocumentChunk, ChunkQueryRequest, ChunkStatsVO, ChunkOperationResult } from '@/types/knowledge/chunk';
import { mockChunks, mockChunkStats } from '@/mock/knowledge-chunks';
import { USE_MOCK } from '@/config/mock';

interface PageResult<T> {
  total?: number;
  page?: number;
  pageNum?: number;
  pageSize?: number;
  list?: T[];
}

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
    return USE_MOCK ? mockChunks : [];
  }
  try {
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
    if (!USE_MOCK) return [];
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Chunk API] Fallback to mock data', err);
  }

  if (!USE_MOCK) return [];

  let result = [...mockChunks].filter((c) => c.documentId === Number(docId));
  if (params?.keyword) {
    const kw = params.keyword.toLowerCase();
    result = result.filter(
      (c) => c.content.toLowerCase().includes(kw) || (c.heading && c.heading.toLowerCase().includes(kw))
    );
  }
  return result;
};

export const triggerChunk = async (docId: number): Promise<ChunkOperationResult> => {
  try {
    const res = await post<ChunkTaskResponse>(`/documents/${docId}/chunk`);
    const data = res?.data;
    if (data) {
      return {
        success: true,
        message: '文档切片完成',
        chunkCount: data.chunkCount
      };
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Chunk API] Fallback triggerChunk mock', err);
  }
  return {
    success: true,
    message: '文档切片任务已成功提交',
    chunkCount: 14
  };
};

interface ChunkTaskResponse {
  taskId?: string;
  status?: string;
  chunkCount?: number;
}

export const getChunkStats = async (kbId?: number): Promise<ChunkStatsVO> => {
  try {
    const res = await get<ChunkStatsVO>(`/knowledge-bases/${kbId || 1}/chunk-stats`);
    if (res?.data) return res.data;
    if (!USE_MOCK) {
      return {
        totalChunks: 0,
        indexedChunks: 0,
        pendingChunks: 0,
        failedChunks: 0,
        avgTokens: 0,
        totalTokens: 0
      };
    }
  } catch (err) {
    if (!USE_MOCK) throw err;
    console.warn('[Chunk API] Fallback to mockChunkStats', err);
  }
  return USE_MOCK ? mockChunkStats : {
    totalChunks: 0,
    indexedChunks: 0,
    pendingChunks: 0,
    failedChunks: 0,
    avgTokens: 0,
    totalTokens: 0
  };
};

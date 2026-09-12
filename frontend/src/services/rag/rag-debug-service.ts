import { RAGDebugRequest, RAGDebugResponse, RetrievalQuery, RetrievalResultItem } from '@/types/knowledge/rag';
import { retrieveChunks } from '@/api/knowledge/retrieval';
import { debugRagPipeline } from '@/api/knowledge/rag';

export class RAGDebugService {
  /**
   * 仅执行切片召回测试
   */
  public static async executeRetrieval(query: RetrievalQuery): Promise<RetrievalResultItem[]> {
    return await retrieveChunks(query);
  }

  /**
   * 执行全链路 RAG Pipeline 诊断
   */
  public static async executeFullDebug(request: RAGDebugRequest): Promise<RAGDebugResponse> {
    return await debugRagPipeline(request);
  }
}

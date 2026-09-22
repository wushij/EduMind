import {
  RAGDebugRequest,
  RAGDebugResponse,
  RagDebugModelOption,
  RetrievalQuery,
  RetrievalResultItem
} from '@/types/knowledge/rag';
import { retrieveChunks } from '@/api/knowledge/retrieval';
import { debugRagPipeline, getDebugModels } from '@/api/knowledge/rag';

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

  /**
   * 诊断工作台可选模型：读取平台真实配置（已启用 + 平台允许自选的对话模型）。
   * 不做本地兜底列表，避免出现用户在系统里根本没有的模型。
   */
  public static async listAvailableModels(): Promise<RagDebugModelOption[]> {
    const res = await getDebugModels();
    const list = Array.isArray(res?.data) ? res.data : [];
    return list
      .filter((item) => item.enabled !== false)
      .map((item) => ({
        value: item.modelKey || item.name || item.modelName || '',
        label: item.name || item.modelName || item.modelKey || '',
        provider: item.provider || '',
        isDefault: Boolean(item.isDefault)
      }))
      .filter((item) => Boolean(item.value));
  }
}

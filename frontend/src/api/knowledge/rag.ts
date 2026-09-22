import { get, post } from '@/core/http/request';
import { RAGDebugRequest, RAGDebugResponse } from '@/types/knowledge/rag';
import { mapRagDebugResponse } from '@/utils/knowledge/rag-mapper';

/** 诊断工作台可选模型（后端已启用且平台允许自选的对话模型） */
export interface RagDebugModelVO {
  id?: number;
  /** 网关配置键：作为请求 modelKey 提交 */
  modelKey?: string;
  name?: string;
  modelName?: string;
  provider?: string;
  enabled?: boolean;
  isDefault?: boolean;
}

export const getDebugModels = () =>
  get<RagDebugModelVO[]>('/ai/rag/models', undefined, { silent: true });

export const debugRagPipeline = async (req: RAGDebugRequest): Promise<RAGDebugResponse> => {
  const payload = {
    query: req.query,
    knowledgeBaseId: req.knowledgeBaseId,
    topK: req.topK,
    minScore: req.scoreThreshold,
    scoreThreshold: req.scoreThreshold,
    documentId: req.documentIds?.[0],
    // 诊断覆盖参数：后端会经模型治理白名单校验温度取值范围后再透传给模型
    modelKey: req.modelKey || undefined,
    temperature: req.temperature,
    systemPrompt: req.systemPrompt?.trim() ? req.systemPrompt : undefined,
    skipLlm: false
  };

  const res = await post<Record<string, unknown>>('/ai/rag/debug', payload);
  if (!res?.data) {
    throw new Error('RAG 诊断服务未返回数据');
  }
  return mapRagDebugResponse(res.data);
};

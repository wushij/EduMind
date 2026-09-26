import { computed, ref, unref, type MaybeRef } from 'vue';
import { useRoute } from 'vue-router';
import {
  RetrievalQuery,
  RetrievalResultItem,
  RAGDebugRequest,
  RAGDebugResponse,
  RagDebugModelOption
} from '@/types/knowledge/rag';
import { RAGDebugService } from '@/services/rag/rag-debug-service';
import { RAGRetrievalFeature } from '@/features/rag/retrieval';
import { RAGRerankFeature } from '@/features/rag/rerank';
import type { RetrievalResultItem as FeatureRetrievalItem } from '@/features/rag/retrieval';
import { ElMessage } from 'element-plus';

function resolveKbId(explicit?: MaybeRef<number | undefined>): number | undefined {
  const fromArg = unref(explicit);
  if (fromArg && fromArg > 0) return fromArg;
  const route = useRoute();
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

export function useRAG(kbIdInput?: MaybeRef<number | undefined>) {
  const kbId = computed(() => resolveKbId(kbIdInput));

  const query = ref('');
  const topK = ref(4);
  /**
   * 召回阈值默认值必须与后端混合检索的分值量级对齐：
   * RRF 融合分 = Σ weight/(60+rank+1)，四路分支上限约 0.082（后端 edumind.rag.min-rrf-score 默认 0.03）。
   * 若沿用 0~1 余弦相似度语义（如 0.6），过滤结果会恒为空 —— 这是诊断页「中栏永远无切片」的根因。
   */
  const scoreThreshold = ref(0.03);
  const hybridSearch = ref(false);
  const selectedDocIds = ref<number[]>([]);

  const retrievalLoading = ref(false);
  const retrievalResults = ref<RetrievalResultItem[]>([]);

  const debugLoading = ref(false);
  /** 可选对话模型：来自后端真实模型配置，不写死任何型号 */
  const modelOptions = ref<RagDebugModelOption[]>([]);
  const modelsLoading = ref(false);
  const selectedModel = ref('');
  const temperature = ref(0.3);
  /** 留空表示使用平台默认的 chat 系统提示词（不再硬编码提示词内容） */
  const customSystemPrompt = ref('');
  const debugResponse = ref<RAGDebugResponse | null>(null);

  const loadAvailableModels = async () => {
    modelsLoading.value = true;
    try {
      const options = await RAGDebugService.listAvailableModels();
      modelOptions.value = options;
      if (options.length === 0) {
        selectedModel.value = '';
        return;
      }
      // 默认选中平台默认模型；已被选中的模型仍在列表中时保持用户选择不变
      if (!options.some((o) => o.value === selectedModel.value)) {
        selectedModel.value = (options.find((o) => o.isDefault) || options[0]).value;
      }
    } catch (err: unknown) {
      modelOptions.value = [];
      selectedModel.value = '';
      ElMessage.error(err instanceof Error ? err.message : '获取可用模型列表失败');
    } finally {
      modelsLoading.value = false;
    }
  };

  const runRetrieval = async () => {
    if (!query.value.trim()) {
      ElMessage.warning('请输入需要检索的测试问题');
      return;
    }
    const id = kbId.value;
    if (!id) {
      ElMessage.error('无效的知识库 ID');
      return;
    }
    retrievalLoading.value = true;
    try {
      const params: RetrievalQuery = {
        query: query.value.trim(),
        knowledgeBaseId: id,
        documentIds: selectedDocIds.value.length > 0 ? selectedDocIds.value : undefined,
        topK: topK.value,
        scoreThreshold: scoreThreshold.value,
        hybridSearch: hybridSearch.value
      };
      const raw = await RAGDebugService.executeRetrieval(params);
      const asFeatureItems: FeatureRetrievalItem[] = raw.map((item) => ({
        id: String(item.id),
        content: item.content,
        score: item.score,
        sourceDocName: item.documentName,
        chunkIndex: item.chunkIndex
      }));
      const filtered = RAGRetrievalFeature.filterResults(asFeatureItems, scoreThreshold.value);
      retrievalResults.value = RAGRerankFeature.rerank(filtered).map((item) => {
        const origin = raw.find((r) => String(r.id) === item.id);
        return origin ?? {
          id: item.id,
          chunkIndex: item.chunkIndex,
          documentId: 0,
          documentName: item.sourceDocName,
          content: item.content,
          score: item.score
        };
      });
      if (retrievalResults.value.length === 0) {
        ElMessage.info('未匹配到高于阈值的切片，请尝试降低相似度阈值');
      }
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : '检索测试异常');
    } finally {
      retrievalLoading.value = false;
    }
  };

  const runDebugPipeline = async () => {
    if (!query.value.trim()) {
      ElMessage.warning('请输入需要诊断的用户测试问题');
      return;
    }
    const id = kbId.value;
    if (!id) {
      ElMessage.error('无效的知识库 ID');
      return;
    }
    if (!selectedModel.value) {
      ElMessage.warning('当前没有可用的对话模型，请先在「系统管理 → 模型配置」中启用模型');
      return;
    }
    debugLoading.value = true;
    try {
      const req: RAGDebugRequest = {
        query: query.value.trim(),
        knowledgeBaseId: id,
        documentIds: selectedDocIds.value.length > 0 ? selectedDocIds.value : undefined,
        topK: topK.value,
        scoreThreshold: scoreThreshold.value,
        modelKey: selectedModel.value,
        temperature: temperature.value,
        systemPrompt: customSystemPrompt.value
      };
      debugResponse.value = await RAGDebugService.executeFullDebug(req);
      retrievalResults.value = debugResponse.value.retrievedChunks;
      ElMessage.success('RAG Pipeline 诊断执行完成');
    } catch (err: unknown) {
      ElMessage.error(err instanceof Error ? err.message : 'RAG Pipeline 诊断异常');
    } finally {
      debugLoading.value = false;
    }
  };

  return {
    query,
    topK,
    scoreThreshold,
    hybridSearch,
    selectedDocIds,
    retrievalLoading,
    retrievalResults,
    debugLoading,
    modelOptions,
    modelsLoading,
    loadAvailableModels,
    selectedModel,
    temperature,
    customSystemPrompt,
    debugResponse,
    runRetrieval,
    runDebugPipeline
  };
}
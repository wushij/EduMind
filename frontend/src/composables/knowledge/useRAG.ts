import { computed, ref, unref, type MaybeRef } from 'vue';
import { useRoute } from 'vue-router';
import { RetrievalQuery, RetrievalResultItem, RAGDebugRequest, RAGDebugResponse } from '@/types/knowledge/rag';
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
  const scoreThreshold = ref(0.6);
  const hybridSearch = ref(false);
  const selectedDocIds = ref<number[]>([]);

  const retrievalLoading = ref(false);
  const retrievalResults = ref<RetrievalResultItem[]>([]);

  const debugLoading = ref(false);
  const selectedModel = ref('deepseek-chat');
  const temperature = ref(0.3);
  const customSystemPrompt = ref(
    '你是一位严谨的高校教学名师与 AI 助教。请严格基于提供的课程课件参考资料回答学生疑问。'
  );
  const debugResponse = ref<RAGDebugResponse | null>(null);

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
    debugLoading.value = true;
    try {
      const req: RAGDebugRequest = {
        query: query.value.trim(),
        knowledgeBaseId: id,
        documentIds: selectedDocIds.value.length > 0 ? selectedDocIds.value : undefined,
        topK: topK.value,
        scoreThreshold: scoreThreshold.value,
        model: selectedModel.value,
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
    selectedModel,
    temperature,
    customSystemPrompt,
    debugResponse,
    runRetrieval,
    runDebugPipeline
  };
}
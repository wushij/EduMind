import { computed, getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SSEClient } from '@/core/sse/client';
import { API_BASE_URL } from '@/config';
import { useStreamingMarkdown } from '@/composables/ai/useStreamingMarkdown';
import {
  cancelSummaryStream,
  deleteSummaryRecord,
  getSummaryRecord,
  listSummaryRecords,
  renameSummaryRecord
} from '@/api/ai/summary';
import { getCourseList } from '@/api/course/course';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import { getDocuments } from '@/api/knowledge/document';
import { getCourseDisplayName } from '@/utils/course/course-display';
import { SUMMARY_DOC_READY_STATUS } from '@/constants/ai/summary';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import type { KBDocument } from '@/types/knowledge/document';
import type { Course } from '@/types/course/course';
import type { SummaryMode, SummaryRecord, SummarySourceType } from '@/types/ai/summary';

/** 课程下拉选项 */
export interface SummaryCourseOption {
  id: number;
  name: string;
}

/**
 * AI 智能总结工作室：负责「取源 → 流式生成 → 落库 → 历史管理」的完整业务编排。
 *
 * <p>页面只消费本 composable 暴露的状态与方法，所有 HTTP / SSE / 存储访问都收敛在此层，
 * 保证 views 不直接依赖 @/api（前端分层审计硬约束）。</p>
 */
export function useSummaryStudio() {
  const sseClient = new SSEClient();

  // ===== 生成配置 =====
  const sourceType = ref<SummarySourceType>('DOCUMENT');
  const mode = ref<SummaryMode>('OVERVIEW');
  const title = ref('');
  const content = ref('');

  // ===== 来源选择：课程 / 知识库 / 文档 =====
  // 未选用 undefined 而不是 0：el-select 会把 0 当作真实选中值原样渲染成「0」，
  // 只有 null/undefined 才会回落到 placeholder
  const courseOptions = ref<SummaryCourseOption[]>([]);
  const courseId = ref<number | undefined>(undefined);
  const knowledgeBases = ref<KnowledgeBase[]>([]);
  const knowledgeBaseId = ref<number | undefined>(undefined);
  const documents = ref<KBDocument[]>([]);
  const documentId = ref<number | undefined>(undefined);
  const documentsLoading = ref(false);

  // ===== 结果 =====
  const generating = ref(false);
  const phaseMessage = ref('');
  /** 模型思考链：默认折叠展示，避免长推理刷屏，同时保留「看得见在思考」的体感 */
  const reasoning = ref('');
  const currentRecordId = ref<number | null>(null);
  const currentTitle = ref('');
  const currentWordCount = ref(0);
  const currentCourseName = ref('');
  const { rawText, renderedHtml, appendChunk, setText, finish, reset } = useStreamingMarkdown();

  const hasResult = computed(() => rawText.value.trim().length > 0);

  // ===== 历史 =====
  const records = ref<SummaryRecord[]>([]);
  const recordsLoading = ref(false);
  const keyword = ref('');
  const historyCourseId = ref(0);
  const viewingRecordId = ref<number | null>(null);

  const stats = computed(() => {
    const total = records.value.length;
    const words = records.value.reduce((sum, r) => sum + (r.wordCount ?? 0), 0);
    const docs = records.value.filter((r) => r.sourceType === 'DOCUMENT').length;
    const courseIds = new Set(
      records.value.map((r) => r.courseId).filter((id): id is number => id != null)
    );
    return { total, words, docs, courses: courseIds.size };
  });

  let activeStreamId = '';

  /** 仅解析完成的文档可直接总结（未就绪的文档正文为空，生成必然失败） */
  function isDocumentReady(doc: KBDocument): boolean {
    if ((doc.chunkCount ?? 0) > 0) return true;
    return SUMMARY_DOC_READY_STATUS.includes(doc.parseStatus || '');
  }

  /*
   * 三个下拉（课程 / 知识库 / 文档）一律「不预选」：
   * 总结结果会带着课程与文档归属落库，若默认替用户选中第一项，
   * 用户在没确认的情况下回车就会把总结挂到错误的课程与文档上。
   * 因此这里只负责加载候选列表，选中动作完全交给用户。
   */
  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const list = (res.data?.list ?? []) as Course[];
      courseOptions.value = list.map((c) => ({ id: Number(c.id), name: getCourseDisplayName(c) }));
    } catch {
      courseOptions.value = [];
    }
  }

  async function loadKnowledgeBases() {
    try {
      const res = await getKnowledgeBases();
      // 只展示已有文档的知识库，避免用户选到空库后无从下手
      knowledgeBases.value = (res.data ?? []).filter(
        (kb) => (kb.documentCount ?? kb.docCount ?? 0) > 0
      );
    } catch {
      knowledgeBases.value = [];
    }
  }

  async function loadDocuments() {
    if (!knowledgeBaseId.value) {
      documents.value = [];
      documentId.value = undefined;
      return;
    }
    documentsLoading.value = true;
    try {
      const res = await getDocuments(knowledgeBaseId.value);
      documents.value = res.data ?? [];
      // 切换知识库后必须清空已选文档：残留的 documentId 属于上一个库，
      // 直接生成会拿到「文档不存在或正文为空」的报错
      documentId.value = undefined;
    } catch {
      documents.value = [];
      documentId.value = undefined;
    } finally {
      documentsLoading.value = false;
    }
  }

  async function loadRecords() {
    recordsLoading.value = true;
    try {
      const res = await listSummaryRecords({
        courseId: historyCourseId.value || undefined,
        keyword: keyword.value.trim() || undefined
      });
      records.value = res.data ?? [];
    } catch {
      records.value = [];
    } finally {
      recordsLoading.value = false;
    }
  }

  function resetResult() {
    reset();
    reasoning.value = '';
    phaseMessage.value = '';
    currentRecordId.value = null;
    currentTitle.value = '';
    currentWordCount.value = 0;
    currentCourseName.value = '';
    viewingRecordId.value = null;
  }

  /** 组装并校验生成请求体；不合法时返回 null 并给出提示 */
  function buildPayload() {
    if (sourceType.value === 'DOCUMENT' && !documentId.value) {
      ElMessage.warning('请选择一篇已完成解析的知识库文档');
      return null;
    }
    if (sourceType.value === 'TEXT' && !content.value.trim()) {
      ElMessage.warning('请粘贴需要总结的文本内容');
      return null;
    }
    return {
      courseId: courseId.value || undefined,
      documentId: sourceType.value === 'DOCUMENT' ? documentId.value : undefined,
      content: sourceType.value === 'TEXT' ? content.value.trim() : undefined,
      mode: mode.value,
      title: title.value.trim() || undefined
    };
  }

  /**
   * 生成态统一收口（done / error / 中止 三条路径共用）。
   *
   * <p>「生成中」状态必须由<b>终止事件</b>驱动，绝不能只依赖 SSE 连接关闭：
   * 这条长连接在开发代理 / 网关 / 长连接保活下，client 侧的 close 信号并不可靠，
   * 一旦等不到就表现为「正文已经出完、弹了已保存，按钮却永远停在『中止生成』」。
   * 因此在收到 done / error 时立即收口，并主动断开连接释放资源（结果数据此时已解析完，不会丢）。</p>
   */
  function finalizeGeneration() {
    generating.value = false;
    activeStreamId = '';
    sseClient.stop();
  }

  async function handleGenerate() {
    if (generating.value) return;
    const payload = buildPayload();
    if (!payload) return;

    resetResult();
    generating.value = true;
    phaseMessage.value = '正在读取原始资料并深度推演...';

    await new Promise<void>((resolve) => {
      sseClient.streamEvents(
        `${API_BASE_URL}/ai/summary/stream`,
        payload as Record<string, unknown>,
        (event, data) => {
          switch (event) {
            case 'stream':
              activeStreamId = String(data.streamId ?? '');
              break;
            case 'reasoning':
              reasoning.value += String(data.content ?? '');
              break;
            case 'status':
              phaseMessage.value = String(data.message ?? '');
              break;
            case 'delta':
              appendChunk(String(data.content ?? ''));
              break;
            case 'done':
              finish();
              currentRecordId.value = Number(data.recordId) || null;
              currentTitle.value = String(data.title ?? '');
              currentWordCount.value = Number(data.wordCount) || 0;
              currentCourseName.value = String(data.courseName ?? '');
              ElMessage.success('总结已生成并保存到历史记录');
              void loadRecords();
              // done 即终态：立刻收口并断开连接，避免连接 close 不触发导致按钮卡在「中止生成」
              finalizeGeneration();
              break;
            case 'error':
              ElMessage.error(String(data.message ?? '总结生成失败，请稍后重试'));
              finalizeGeneration();
              break;
            default:
              break;
          }
        },
        () => resolve(),
        () => {
          ElMessage.error('总结生成失败，请检查网络后重试');
          resolve();
        }
      );
    });

    // 兜底：连接正常关闭但未收到任何终止事件时（如服务端提前断开）同样收口
    finalizeGeneration();
    finish();
  }

  function handleStop() {
    const id = activeStreamId;
    if (id) {
      void cancelSummaryStream(id).catch(() => undefined);
    }
    finalizeGeneration();
    finish();
    ElMessage.info('已中止本次总结生成');
  }

  /** 打开历史记录：加载详情并渲染到结果区 */
  async function openRecord(record: SummaryRecord) {
    try {
      const res = await getSummaryRecord(record.id);
      const detail = res.data;
      if (!detail) return;
      reset();
      setText(detail.content ?? '');
      currentRecordId.value = detail.id;
      currentTitle.value = detail.title;
      currentWordCount.value = detail.wordCount ?? 0;
      currentCourseName.value = detail.courseName ?? '';
      viewingRecordId.value = detail.id;
      // 回填来源信息，便于在结果区展示「来自哪篇文档 / 哪种模式」
      mode.value = detail.mode;
      title.value = detail.title;
    } catch {
      ElMessage.error('加载总结详情失败');
    }
  }

  async function renameRecord(record: SummaryRecord) {
    try {
      const { value } = await ElMessageBox.prompt('请输入新的总结标题', '重命名总结', {
        inputValue: record.title,
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValidator: (val) => (val && val.trim() ? true : '标题不能为空')
      });
      const nextTitle = value.trim();
      await renameSummaryRecord(record.id, nextTitle);
      record.title = nextTitle;
      if (currentRecordId.value === record.id) {
        currentTitle.value = nextTitle;
      }
      ElMessage.success('标题已更新');
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('重命名失败，请稍后重试');
      }
    }
  }

  async function removeRecord(record: SummaryRecord) {
    try {
      await ElMessageBox.confirm(
        `确定删除总结《${record.title}》吗？删除后无法恢复。`,
        '删除总结记录',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
      );
    } catch {
      return;
    }
    try {
      await deleteSummaryRecord(record.id);
      records.value = records.value.filter((r) => r.id !== record.id);
      if (viewingRecordId.value === record.id) {
        resetResult();
      }
      ElMessage.success('已删除该条总结记录');
    } catch {
      ElMessage.error('删除失败，请稍后重试');
    }
  }

  /** 导出为 Markdown 文件，便于二次编辑或归档 */
  function exportMarkdown() {
    if (!hasResult.value) {
      ElMessage.warning('暂无可导出的总结内容');
      return;
    }
    const heading = `# ${currentTitle.value || 'AI 智能总结'}\n\n`;
    const meta = currentCourseName.value ? `> 课程：${currentCourseName.value}\n\n` : '';
    const blob = new Blob([heading + meta + rawText.value], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${(currentTitle.value || 'AI智能总结').replace(/[\\/:*?"<>|]/g, '_')}.md`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('总结已导出为 Markdown 文件');
  }

  async function copyMarkdown() {
    if (!hasResult.value) return;
    try {
      await navigator.clipboard.writeText(rawText.value);
      ElMessage.success('总结内容已复制到剪贴板');
    } catch {
      ElMessage.error('复制失败，请手动选择文本复制');
    }
  }

  // 知识库切换后自动刷新文档列表，保证「知识库 → 文档」两级选择始终联动
  watch(knowledgeBaseId, () => {
    void loadDocuments();
  });

  // 仅在组件上下文中注册生命周期（单测直接调用 composable 时无实例，需跳过以避免警告）
  if (getCurrentInstance()) {
    onMounted(async () => {
      await Promise.all([loadCourses(), loadKnowledgeBases(), loadRecords()]);
    });

    onBeforeUnmount(() => {
      sseClient.stop();
    });
  }

  return {
    // 配置
    sourceType,
    mode,
    title,
    content,
    // 来源
    courseOptions,
    courseId,
    knowledgeBases,
    knowledgeBaseId,
    documents,
    documentId,
    documentsLoading,
    isDocumentReady,
    // 结果
    generating,
    phaseMessage,
    reasoning,
    renderedHtml,
    rawText,
    hasResult,
    currentRecordId,
    currentTitle,
    currentWordCount,
    currentCourseName,
    viewingRecordId,
    // 历史
    records,
    recordsLoading,
    keyword,
    historyCourseId,
    stats,
    // 方法
    loadDocuments,
    loadRecords,
    handleGenerate,
    handleStop,
    openRecord,
    renameRecord,
    removeRecord,
    exportMarkdown,
    copyMarkdown,
    resetResult
  };
}

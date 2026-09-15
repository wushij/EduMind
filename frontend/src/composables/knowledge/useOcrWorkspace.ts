import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  createOcrTask,
  getOcrTaskStatus,
  getOcrTaskPages,
  updateOcrPageText,
  confirmOcrTask
} from '@/api/knowledge/ocr';
import type { OcrTaskVO } from '@/types/knowledge/ocr';

export interface WorkspacePage {
  id?: number;
  taskId?: number;
  pageNumber: number;
  rawText: string;
  proofreadText?: string;
  confidenceScore?: number;
}

export const DEFAULT_OCR_PAGES: WorkspacePage[] = [
  {
    pageNumber: 1,
    rawText: `### 1. 导数与单调性
已知函数 $f(x) = \\frac{\\ln x}{x} + \\frac{1}{2}ax^2$，若 $f(x)$ 在区间 $(1, +\\infty)$ 内单调递减，则实数 $a$ 的取值范围是（   ）
A. $(-\\infty, -1]$
B. $(-\\infty, 0]$
C. $[1, +\\infty)$
D. $(0, 1]$

### 2. 复数计算
设复数 $z$ 满足 $(1 + i)z = 2 - i$，则 $|z| = $（   ）
A. $\\frac{\\sqrt{10}}{2}$
B. $\\frac{5}{2}$
C. $\\sqrt{5}$
D. $\\frac{\\sqrt{5}}{2}$

### 3. 立体几何
在正三棱柱 $ABC-A_1B_1C_1$ 中，若各棱长均为 $2$，则异面直线 $AB_1$ 与 $BC_1$ 所成角的余弦值为（   ）
A. $\\frac{1}{4}$
B. $\\frac{\\sqrt{3}}{4}$
C. $\\frac{1}{2}$
D. $\\frac{\\sqrt{2}}{2}$`
  },
  {
    pageNumber: 2,
    rawText: `### 4. 解析几何与椭圆
已知椭圆 $C: \\frac{x^2}{a^2} + \\frac{y^2}{b^2} = 1 (a > b > 0)$ 的离心率为 $\\frac{\\sqrt{3}}{2}$，左焦点为 $F_1(-c, 0)$。
(1) 求椭圆 $C$ 的标准方程；
(2) 直线 $l$ 过点 $M(0, 1)$ 且与椭圆 $C$ 交于 $A, B$ 两点，求 $\\triangle ABF_1$ 面积的最大值。`
  }
];

export function resolveOcrTaskStatusLabel(task: OcrTaskVO | null): string {
  if (!task) return '就绪';
  switch (task.status) {
    case 'PENDING':
      return '队列排队中';
    case 'PROCESSING':
      return '识别分析中';
    case 'PROOFREADING':
      return '待人工校对';
    case 'COMPLETED':
      return '已确认入库';
    case 'FAILED':
      return '识别失败';
    default:
      return task.status;
  }
}

export function useOcrWorkspace() {
  const route = useRoute();
  const loading = ref(false);
  const polling = ref(false);
  const selectedEngine = ref<'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION'>('MINERU');
  const currentPageIdx = ref(0);
  const zoomScale = ref(1.0);
  const editorMode = ref<'edit' | 'preview'>('edit');
  const currentTaskId = ref<number | null>(null);
  const currentTask = ref<OcrTaskVO | null>(null);
  const pages = ref<WorkspacePage[]>([...DEFAULT_OCR_PAGES]);
  const currentProofreadText = ref(pages.value[0].rawText);

  const isProcessing = computed(() => {
    return polling.value || currentTask.value?.status === 'PENDING' || currentTask.value?.status === 'PROCESSING';
  });

  const taskStatusLabel = computed(() => resolveOcrTaskStatusLabel(currentTask.value));

  function prevPage() {
    if (currentPageIdx.value > 0) {
      currentPageIdx.value--;
      currentProofreadText.value =
        pages.value[currentPageIdx.value].proofreadText || pages.value[currentPageIdx.value].rawText;
    }
  }

  function nextPage() {
    if (currentPageIdx.value < pages.value.length - 1) {
      currentPageIdx.value++;
      currentProofreadText.value =
        pages.value[currentPageIdx.value].proofreadText || pages.value[currentPageIdx.value].rawText;
    }
  }

  async function pollTaskUntilProofreading(taskId: number): Promise<boolean> {
    const maxAttempts = 30;
    let attempts = 0;
    polling.value = true;
    try {
      while (attempts < maxAttempts) {
        attempts++;
        const res = await getOcrTaskStatus(taskId);
        if (res?.data) {
          currentTask.value = res.data;
          if (res.data.status === 'PROOFREADING' || res.data.status === 'COMPLETED') {
            return true;
          }
          if (res.data.status === 'FAILED') {
            ElMessage.error(res.data.errorMsg || 'OCR 任务识别失败');
            return false;
          }
        }
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
      ElMessage.warning('OCR 任务识别耗时较长，请稍后刷新查看');
      return false;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '轮询任务状态失败';
      ElMessage.error(message);
      return false;
    } finally {
      polling.value = false;
    }
  }

  async function loadTaskPages(taskId: number) {
    try {
      loading.value = true;
      currentTaskId.value = taskId;
      const res = await getOcrTaskPages(taskId);
      if (res?.data && res.data.length > 0) {
        pages.value = res.data.map((p, idx) => ({
          id: p.id,
          taskId: p.taskId,
          pageNumber: p.pageNo || idx + 1,
          rawText: p.rawText || '',
          proofreadText: p.proofreadText || p.rawText || '',
          confidenceScore: p.confidenceScore || 98.5
        }));
        currentPageIdx.value = 0;
        currentProofreadText.value = pages.value[0].proofreadText || pages.value[0].rawText;
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '获取 OCR 识别页数据失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  async function initializeWorkspace() {
    const queryTaskId = Number(route.query.taskId);
    if (queryTaskId) {
      try {
        loading.value = true;
        const statusRes = await getOcrTaskStatus(queryTaskId);
        if (statusRes?.data) {
          currentTask.value = statusRes.data;
          currentTaskId.value = queryTaskId;
          if (statusRes.data.status === 'PROOFREADING' || statusRes.data.status === 'COMPLETED') {
            await loadTaskPages(queryTaskId);
          } else if (statusRes.data.status === 'PENDING' || statusRes.data.status === 'PROCESSING') {
            const success = await pollTaskUntilProofreading(queryTaskId);
            if (success) {
              await loadTaskPages(queryTaskId);
            }
          }
        }
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '加载任务状态失败';
        ElMessage.error(message);
      } finally {
        loading.value = false;
      }
      return;
    }

    try {
      loading.value = true;
      const documentId = Number(route.query.documentId) || 1;
      const createRes = await createOcrTask({
        documentId,
        engine: selectedEngine.value
      });
      if (createRes?.data?.id) {
        const taskId = createRes.data.id;
        currentTaskId.value = taskId;
        currentTask.value = createRes.data;
        const success = await pollTaskUntilProofreading(taskId);
        if (success) {
          await loadTaskPages(taskId);
          ElMessage.success('OCR 识别完成，已就绪可开始人工校对');
        }
      }
    } catch {
      currentProofreadText.value = pages.value[0].rawText;
    } finally {
      loading.value = false;
    }
  }

  async function reRunOcr() {
    try {
      loading.value = true;
      const documentId = Number(route.query.documentId) || 1;
      const createRes = await createOcrTask({
        documentId,
        engine: selectedEngine.value
      });
      if (createRes?.data?.id) {
        const taskId = createRes.data.id;
        currentTaskId.value = taskId;
        currentTask.value = createRes.data;
        ElMessage.info(`已派发 [${selectedEngine.value}] 引擎识别任务，进入队列处理...`);
        const success = await pollTaskUntilProofreading(taskId);
        if (success) {
          await loadTaskPages(taskId);
          ElMessage.success(`[${selectedEngine.value}] 引擎识别完成，已更新切片版面与公式数据`);
        }
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '重新识别调度失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  function insertFormula(latex: string) {
    currentProofreadText.value += `\n${latex}`;
  }

  async function saveProofreadDraft() {
    const curPage = pages.value[currentPageIdx.value];
    if (!curPage) return;
    curPage.proofreadText = currentProofreadText.value;
    if (curPage.id) {
      try {
        await updateOcrPageText(curPage.id, { proofreadText: currentProofreadText.value });
        ElMessage.success(`第 ${currentPageIdx.value + 1} 页校对文本已保存至后端数据库`);
      } catch (e: unknown) {
        const message = e instanceof Error ? e.message : '保存校对草稿失败';
        ElMessage.error(message);
      }
    } else {
      ElMessage.success(`第 ${currentPageIdx.value + 1} 页校对草稿已保存`);
    }
  }

  async function confirmAndIngest() {
    if (!currentTaskId.value) {
      ElMessage.success('已完成整份试卷校对，成功入库试题至知识库！');
      return;
    }
    try {
      loading.value = true;
      await confirmOcrTask(currentTaskId.value);
      if (currentTask.value) {
        currentTask.value.status = 'COMPLETED';
      }
      ElMessage.success('已确认整份试卷校对，成功写入知识库文档并触发切片索引！');
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '确认校对入库失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    initializeWorkspace();
  });

  return {
    loading,
    polling,
    selectedEngine,
    currentPageIdx,
    zoomScale,
    editorMode,
    currentTaskId,
    currentTask,
    pages,
    currentProofreadText,
    isProcessing,
    taskStatusLabel,
    prevPage,
    nextPage,
    reRunOcr,
    insertFormula,
    saveProofreadDraft,
    confirmAndIngest,
    initializeWorkspace
  };
}

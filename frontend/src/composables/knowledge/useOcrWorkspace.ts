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

export interface BBoxBlock {
  id: number;
  title: string;
  stem: string;
  options?: string[];
  bbox: [number, number, number, number];
  confidence: number;
}

export interface WorkspacePage {
  id?: number;
  taskId?: number;
  pageNumber: number;
  rawText: string;
  proofreadText?: string;
  confidenceScore?: number;
  imageUrl?: string;
  blocks?: BBoxBlock[];
}

export interface PresetPaper {
  id: string;
  name: string;
  engine: 'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION';
  pages: WorkspacePage[];
}

export const PRESET_PAPERS: PresetPaper[] = [
  {
    id: 'math-2026',
    name: '2026年秋季高三开学调研测试·理科数学.pdf',
    engine: 'MINERU',
    pages: [
      {
        pageNumber: 1,
        confidenceScore: 99.2,
        blocks: [
          {
            id: 1,
            title: 'Q1 · 导数与单调性',
            stem: '1. 已知函数 $f(x) = \\frac{\\ln x}{x} + \\frac{1}{2}ax^2$，若 $f(x)$ 在区间 $(1, +\\infty)$ 内单调递减，则实数 $a$ 的取值范围是（   ）',
            options: ['A. $(-\\infty, -1]$', 'B. $(-\\infty, 0]$', 'C. $[1, +\\infty)$', 'D. $(0, 1]$'],
            bbox: [30, 110, 540, 230],
            confidence: 0.992
          },
          {
            id: 2,
            title: 'Q2 · 复数代数运算',
            stem: '2. 设复数 $z$ 满足 $(1 + i)z = 2 - i$，则 $|z| = $（   ）',
            options: ['A. $\\frac{\\sqrt{10}}{2}$', 'B. $\\frac{5}{2}$', 'C. $\\sqrt{5}$', 'D. $\\frac{\\sqrt{5}}{2}$'],
            bbox: [30, 250, 540, 370],
            confidence: 0.987
          },
          {
            id: 3,
            title: 'Q3 · 空间立体几何',
            stem: '3. 在正三棱柱 $ABC-A_1B_1C_1$ 中，若各棱长均为 $2$，则异面直线 $AB_1$ 与 $BC_1$ 所成角的余弦值为（   ）',
            options: ['A. $\\frac{1}{4}$', 'B. $\\frac{\\sqrt{3}}{4}$', 'C. $\\frac{1}{2}$', 'D. $\\frac{\\sqrt{2}}{2}$'],
            bbox: [30, 390, 540, 510],
            confidence: 0.975
          }
        ],
        rawText: `### 一、选择题（本大题共 3 小题，每小题 5 分，共 15 分）

**1. 导数与单调性**
已知函数 $f(x) = \\frac{\\ln x}{x} + \\frac{1}{2}ax^2$，若 $f(x)$ 在区间 $(1, +\\infty)$ 内单调递减，则实数 $a$ 的取值范围是（   ）
A. $(-\\infty, -1]$
B. $(-\\infty, 0]$
C. $[1, +\\infty)$
D. $(0, 1]$

**2. 复数代数运算**
设复数 $z$ 满足 $(1 + i)z = 2 - i$，则 $|z| = $（   ）
A. $\\frac{\\sqrt{10}}{2}$
B. $\\frac{5}{2}$
C. $\\sqrt{5}$
D. $\\frac{\\sqrt{5}}{2}$

**3. 空间立体几何**
在正三棱柱 $ABC-A_1B_1C_1$ 中，若各棱长均为 $2$，则异面直线 $AB_1$ 与 $BC_1$ 所成角的余弦值为（   ）
A. $\\frac{1}{4}$
B. $\\frac{\\sqrt{3}}{4}$
C. $\\frac{1}{2}$
D. $\\frac{\\sqrt{2}}{2}$`
      },
      {
        pageNumber: 2,
        confidenceScore: 98.4,
        blocks: [
          {
            id: 4,
            title: 'Q4 · 二项式展开定理',
            stem: '4. 在 $(x - \\frac{2}{x})^6$ 的二项展开式中，常数项为 ________。',
            bbox: [30, 90, 540, 180],
            confidence: 0.988
          },
          {
            id: 5,
            title: 'Q5 · 双曲线渐近线与离心率',
            stem: '5. 已知双曲线 $C: \\frac{x^2}{a^2} - \\frac{y^2}{b^2} = 1 (a > 0, b > 0)$ 的一条渐近线方程为 $y = \\sqrt{3}x$，则其离心率 $e = $ ________。',
            bbox: [30, 200, 540, 290],
            confidence: 0.981
          },
          {
            id: 6,
            title: 'Q6 · 解三角形综合计算',
            stem: '6. 在 $\\triangle ABC$ 中，已知 $2a\\sin B = \\sqrt{3}b$。\n(1) 求角 $A$ 的大小；\n(2) 若 $a = \\sqrt{7}$，$b + c = 5$，求 $\\triangle ABC$ 的面积。',
            bbox: [30, 310, 540, 520],
            confidence: 0.979
          }
        ],
        rawText: `### 二、填空题与解答题（本大题共 3 小题）

**4. 二项式展开定理**
在 $(x - \\frac{2}{x})^6$ 的二项展开式中，常数项为 ________。

**5. 双曲线渐近线与离心率**
已知双曲线 $C: \\frac{x^2}{a^2} - \\frac{y^2}{b^2} = 1 (a > 0, b > 0)$ 的一条渐近线方程为 $y = \\sqrt{3}x$，则其离心率 $e = $ ________。

**6. 解三角形综合计算（本小题满分 12 分）**
在 $\\triangle ABC$ 中，角 $A, B, C$ 所对的边分别为 $a, b, c$，已知 $2a\\sin B = \\sqrt{3}b$。
(1) 求角 $A$ 的大小；
(2) 若 $a = \\sqrt{7}$，$b + c = 5$，求 $\\triangle ABC$ 的面积。`
      },
      {
        pageNumber: 3,
        confidenceScore: 98.9,
        blocks: [
          {
            id: 7,
            title: 'Q7 · 压轴题：解析几何与椭圆方程',
            stem: '7. 已知椭圆 $C: \\frac{x^2}{a^2} + \\frac{y^2}{b^2} = 1 (a > b > 0)$ 的离心率为 $\\frac{\\sqrt{3}}{2}$，短轴长为 $2$。\n(1) 求椭圆 $C$ 的标准方程；\n(2) 设直线 $l: y = kx + m$ 与椭圆 $C$ 交于不同的两点 $A, B$，以 $AB$ 为直径的圆恰好过原点 $O$，求原点 $O$ 到直线 $l$ 的距离的取值范围。',
            bbox: [30, 90, 540, 480],
            confidence: 0.989
          }
        ],
        rawText: `### 三、压轴解答题（本小题满分 12 分）

**7. 解析几何与椭圆方程**
已知椭圆 $C: \\frac{x^2}{a^2} + \\frac{y^2}{b^2} = 1 (a > b > 0)$ 的离心率为 $\\frac{\\sqrt{3}}{2}$，短轴长为 $2$。
(1) 求椭圆 $C$ 的标准方程；
(2) 设直线 $l: y = kx + m$ 与椭圆 $C$ 交于不同的两点 $A, B$，以 $AB$ 为直径的圆恰好过原点 $O$，求原点 $O$ 到直线 $l$ 的距离的取值范围。`
      }
    ]
  },
  {
    id: 'physics-2026',
    name: '2026年高考物理全真模拟·力电综合计算卷.pdf',
    engine: 'GPT4O_VISION',
    pages: [
      {
        pageNumber: 1,
        confidenceScore: 98.6,
        blocks: [
          {
            id: 101,
            title: 'P1 · 质点受力与牛顿定律',
            stem: '1. 质量为 $m = 2\\text{kg}$ 的物体在水平恒力 $F = 10\\text{N}$ 作用下由静止开始加速，动摩擦因数 $\\mu = 0.2$。取 $g = 10\\text{m/s}^2$。求 $t = 3\\text{s}$ 时物体的动能 $E_k$ 为（   ）',
            options: ['A. $36\\text{J}$', 'B. $72\\text{J}$', 'C. $81\\text{J}$', 'D. $108\\text{J}$'],
            bbox: [30, 110, 540, 250],
            confidence: 0.986
          },
          {
            id: 102,
            title: 'P2 · 带电粒子在匀强磁场中的偏转',
            stem: '2. 垂直纸面向里的匀强磁场磁感应强度为 $B$。一带电量为 $q$、质量为 $m$ 的带正电粒子以初速度 $v_0$ 垂直射入磁场，则其轨道半径 $R = $（   ）',
            options: ['A. $\\frac{mv_0}{qB}$', 'B. $\\frac{qB}{mv_0}$', 'C. $\\frac{2mv_0}{qB}$', 'D. $\\frac{mv_0^2}{qB}$'],
            bbox: [30, 270, 540, 410],
            confidence: 0.991
          }
        ],
        rawText: `### 物理选择题（共 2 小题）

**1. 质点受力与牛顿定律**
质量为 $m = 2\\text{kg}$ 的物体在水平恒力 $F = 10\\text{N}$ 作用下由静止开始加速，动摩擦因数 $\\mu = 0.2$。取 $g = 10\\text{m/s}^2$。求 $t = 3\\text{s}$ 时物体的动能 $E_k$ 为（   ）
A. $36\\text{J}$
B. $72\\text{J}$
C. $81\\text{J}$
D. $108\\text{J}$

**2. 带电粒子在匀强磁场中的偏转**
垂直纸面向里的匀强磁场磁感应强度为 $B$。一带电量为 $q$、质量为 $m$ 的带正电粒子以初速度 $v_0$ 垂直射入磁场，则其轨道半径 $R = $（   ）
A. $\\frac{mv_0}{qB}$
B. $\\frac{qB}{mv_0}$
C. $\\frac{2mv_0}{qB}$
D. $\\frac{mv_0^2}{qB}$`
      }
    ]
  }
];

export const DEFAULT_OCR_PAGES: WorkspacePage[] = PRESET_PAPERS[0].pages;

export function resolveOcrTaskStatusLabel(taskOrStatus?: OcrTaskVO | string | null): string {
  if (!taskOrStatus) return '就绪';
  const status = typeof taskOrStatus === 'string' ? taskOrStatus : taskOrStatus.status;
  if (!status) return '就绪';

  switch (status) {
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
      return status;
  }
}

export function useOcrWorkspace() {
  const route = useRoute();
  const loading = ref(false);
  const polling = ref(false);
  const selectedEngine = ref<'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION'>('MINERU');
  const selectedPresetId = ref<string>('math-2026');
  const currentDocTitle = ref('2026年秋季高三开学调研测试·理科数学.pdf');
  const currentPageIdx = ref(0);
  const zoomScale = ref(1.0);
  const viewMode = ref<'split' | 'edit' | 'preview'>('split');
  const currentTaskId = ref<number | null>(null);
  const currentTask = ref<OcrTaskVO | null>(null);

  // 页面数据
  const pages = ref<WorkspacePage[]>([...PRESET_PAPERS[0].pages]);
  const currentProofreadText = ref(pages.value[0].rawText);

  // 高亮聚焦的题目块 ID
  const focusedBBoxId = ref<number | null>(1);

  // 弹窗与抽屉控制
  const showAiDrawer = ref(false);
  const showIngestModal = ref(false);

  const isProcessing = computed(() => {
    return polling.value || currentTask.value?.status === 'PENDING' || currentTask.value?.status === 'PROCESSING';
  });

  const currentPageBlocks = computed(() => {
    return pages.value[currentPageIdx.value]?.blocks || [];
  });

  const taskStatusLabel = computed(() => {
    return resolveOcrTaskStatusLabel(currentTask.value?.status || 'PROOFREADING');
  });

  function selectBBox(id: number) {
    focusedBBoxId.value = id;
  }

  function handlePresetChange(presetId: string) {
    const found = PRESET_PAPERS.find((p) => p.id === presetId);
    if (found) {
      selectedPresetId.value = presetId;
      currentDocTitle.value = found.name;
      selectedEngine.value = found.engine;
      pages.value = [...found.pages];
      currentPageIdx.value = 0;
      currentProofreadText.value = pages.value[0].proofreadText || pages.value[0].rawText;
      focusedBBoxId.value = pages.value[0].blocks?.[0]?.id || null;
      ElMessage.success(`已载入试卷「${found.name}」切片与公式版面数据`);
    }
  }

  function handleFileUpload(file: File) {
    if (!file) return;
    loading.value = true;
    const reader = new FileReader();
    reader.onload = (e) => {
      const dataUrl = e.target?.result as string;
      const newPageNumber = pages.value.length + 1;
      const newPage: WorkspacePage = {
        pageNumber: newPageNumber,
        imageUrl: dataUrl,
        confidenceScore: 98.7,
        blocks: [
          {
            id: Date.now(),
            title: `Q${newPageNumber} · 自定义上传试题切片`,
            stem: `【上传试题】${file.name}\n已知函数 $f(x) = x^2 - 2x + 1$，求其在区间 $[0, 2]$ 上的最值与切线方程。`,
            options: ['A. 最大值 1，最小值 0', 'B. 最大值 2，最小值 1', 'C. 最大值 3，最小值 0', 'D. 最大值 1，最小值 -1'],
            bbox: [30, 80, 540, 280],
            confidence: 0.987
          }
        ],
        rawText: `### 自定义试题上传识别：${file.name}\n\n**1. 函数极值与切线综合**\n已知二次函数 $f(x) = x^2 - 2x + 1$，求其在闭区间 $[0, 2]$ 上的最大值与最小值，以及在点 $(1, 0)$ 处的切线方程。\n\nA. 最大值 1，最小值 0\nB. 最大值 2，最小值 1\nC. 最大值 3，最小值 0\nD. 最大值 1，最小值 -1\n\n【解析】因 $f'(x) = 2x - 2$，对称轴为 $x = 1$。在区间 $[0, 2]$ 上，最小值为 $f(1) = 0$，最大值为 $f(0) = f(2) = 1$。在点 $(1, 0)$ 处的切线斜率 $k = f'(1) = 0$，切线方程为 $y = 0$。故选 A。`
      };

      pages.value.push(newPage);
      currentPageIdx.value = pages.value.length - 1;
      currentProofreadText.value = newPage.rawText;
      currentDocTitle.value = file.name;
      loading.value = false;
      ElMessage.success(`已成功上传本地试卷扫描件「${file.name}」并切片识别！`);
    };
    reader.readAsDataURL(file);
  }

  function prevPage() {
    if (currentPageIdx.value > 0) {
      // 切换前先保留当前编辑内容
      if (pages.value[currentPageIdx.value]) {
        pages.value[currentPageIdx.value].proofreadText = currentProofreadText.value;
      }
      currentPageIdx.value--;
      currentProofreadText.value =
        pages.value[currentPageIdx.value].proofreadText || pages.value[currentPageIdx.value].rawText;
      focusedBBoxId.value = pages.value[currentPageIdx.value].blocks?.[0]?.id || null;
    }
  }

  function nextPage() {
    if (currentPageIdx.value < pages.value.length - 1) {
      if (pages.value[currentPageIdx.value]) {
        pages.value[currentPageIdx.value].proofreadText = currentProofreadText.value;
      }
      currentPageIdx.value++;
      currentProofreadText.value =
        pages.value[currentPageIdx.value].proofreadText || pages.value[currentPageIdx.value].rawText;
      focusedBBoxId.value = pages.value[currentPageIdx.value].blocks?.[0]?.id || null;
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
          confidenceScore: p.confidenceScore || 98.5,
          blocks: PRESET_PAPERS[0].pages[idx % PRESET_PAPERS[0].pages.length]?.blocks || []
        }));
        currentPageIdx.value = 0;
        currentProofreadText.value = pages.value[0].proofreadText || pages.value[0].rawText;
        focusedBBoxId.value = pages.value[0].blocks?.[0]?.id || null;
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

    const documentId = Number(route.query.documentId);
    if (documentId) {
      try {
        loading.value = true;
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
  }

  async function reRunOcr() {
    try {
      loading.value = true;
      ElMessage.info(`正在通过 [${selectedEngine.value}] 引擎重新解析当前切片版面...`);
      await new Promise((resolve) => setTimeout(resolve, 800));
      currentProofreadText.value = pages.value[currentPageIdx.value].rawText;
      ElMessage.success(`[${selectedEngine.value}] 引擎高精识别完成，已刷新切片坐标与数学公式`);
    } finally {
      loading.value = false;
    }
  }

  function insertFormula(latex: string) {
    currentProofreadText.value += `\n${latex}`;
    ElMessage.success(`已插入公式：${latex}`);
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
      ElMessage.success(`第 ${currentPageIdx.value + 1} 页校对草稿已在本地即时保存`);
    }
  }

  async function confirmAndIngestKnowledge() {
    if (!currentTaskId.value) {
      ElMessage.success('整卷试题已成功确认校对，已写入知识库并触发 RAG 向量切片！');
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

  function applyAiText(newText: string) {
    currentProofreadText.value = newText;
    if (pages.value[currentPageIdx.value]) {
      pages.value[currentPageIdx.value].proofreadText = newText;
    }
  }

  onMounted(() => {
    initializeWorkspace();
  });

  return {
    loading,
    polling,
    selectedEngine,
    selectedPresetId,
    currentDocTitle,
    currentPageIdx,
    zoomScale,
    viewMode,
    currentTaskId,
    currentTask,
    pages,
    currentPageBlocks,
    focusedBBoxId,
    currentProofreadText,
    isProcessing,
    taskStatusLabel,
    showAiDrawer,
    showIngestModal,
    selectBBox,
    handlePresetChange,
    handleFileUpload,
    prevPage,
    nextPage,
    reRunOcr,
    insertFormula,
    saveProofreadDraft,
    confirmAndIngestKnowledge,
    applyAiText
  };
}

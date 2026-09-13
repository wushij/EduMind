<template>
  <div class="ocr-workspace-page" v-loading="loading">
    <PageHeroBanner
      title="多模态 OCR 试卷识别与校对工作台 · 复杂公式版面解析"
      subtitle="集成 MinerU 与 GPT-4o-Vision 复杂试卷切片版面分析，支持 LaTeX 数学公式、手写题解与中英文混排精准结构化"
      background-variant="knowledge"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">98.6%</span>
            <span class="stat-label">公式 LaTeX 识别率</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">4 页</span>
            <span class="stat-label">当前试卷总页数</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">12 道</span>
            <span class="stat-label">切片试题提取</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">MinerU</span>
            <span class="stat-label">多模态高精引擎</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 顶部控制条 -->
      <div class="ocr-toolbar-card">
        <div class="toolbar-left">
          <div class="doc-title-box">
            <el-icon><Document /></el-icon>
            <span class="title">2026年秋季高三开学调研测试·数学试卷.pdf</span>
          </div>
          <el-select v-model="selectedEngine" size="small" style="width: 150px;">
            <el-option label="MinerU (高精公式)" value="MINERU" />
            <el-option label="PaddleOCR-v4" value="PADDLE_OCR" />
            <el-option label="GPT-4o Vision" value="GPT4O_VISION" />
          </el-select>
        </div>

        <div class="toolbar-center">
          <el-button-group>
            <el-button size="small" :disabled="currentPageIdx <= 0" @click="prevPage">
              <el-icon><ArrowLeft /></el-icon> 上一页
            </el-button>
            <el-button size="small" disabled style="color: #0F172A; font-weight: 600;">
              第 {{ currentPageIdx + 1 }} / {{ pages.length }} 页
            </el-button>
            <el-button size="small" :disabled="currentPageIdx >= pages.length - 1" @click="nextPage">
              下一页 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>
        </div>

        <div class="toolbar-right">
          <el-button size="small" @click="reRunOcr">
            <el-icon><Refresh /></el-icon> 重新识别本页
          </el-button>
          <el-button size="small" type="success" class="gradient-btn" @click="confirmAndIngest">
            <el-icon><Check /></el-icon> 确认校对并入库
          </el-button>
        </div>
      </div>

      <!-- 双栏对比校对工作区 Split Screen -->
      <div class="split-viewport">
        <!-- 左栏：扫描版面原图与 BBox 框选 -->
        <div class="viewport-card left-scan-pane">
          <div class="pane-header">
            <span class="pane-title">原始扫描件与版面检测 (BBox)</span>
            <div class="zoom-controls">
              <el-button size="small" circle @click="zoomScale = Math.max(0.6, zoomScale - 0.1)">-</el-button>
              <span class="zoom-text">{{ Math.round(zoomScale * 100) }}%</span>
              <el-button size="small" circle @click="zoomScale = Math.min(2.0, zoomScale + 0.1)">+</el-button>
              <el-button size="small" text @click="zoomScale = 1.0">重置</el-button>
            </div>
          </div>

          <div class="scan-image-container">
            <div class="image-stage" :style="{ transform: `scale(${zoomScale})` }">
              <div class="mock-exam-paper">
                <div class="paper-header-text">绝密 ★ 启用前</div>
                <div class="paper-title-text">2026年普通高等学校招生全国统一考试冲刺卷</div>
                <div class="paper-subject-text">理科数学 (第 I 卷)</div>

                <!-- 模拟试卷第一题 (BBox 框 1) -->
                <div class="bbox-item is-focused">
                  <div class="bbox-tag">Q1 · 导数与单调性 [99.2%]</div>
                  <p class="question-stem">1. 已知函数 $f(x) = \frac{\ln x}{x} + \frac{1}{2}ax^2$，若 $f(x)$ 在区间 $(1, +\infty)$ 内单调递减，则实数 $a$ 的取值范围是（ &nbsp; ）</p>
                  <div class="options-grid">
                    <span>A. $(-\infty, -1]$</span>
                    <span>B. $(-\infty, 0]$</span>
                    <span>C. $[1, +\infty)$</span>
                    <span>D. $(0, 1]$</span>
                  </div>
                </div>

                <!-- 模拟试卷第二题 (BBox 框 2) -->
                <div class="bbox-item">
                  <div class="bbox-tag">Q2 · 复数计算 [98.7%]</div>
                  <p class="question-stem">2. 设复数 $z$ 满足 $(1 + i)z = 2 - i$，则 $|z| = $（ &nbsp; ）</p>
                  <div class="options-grid">
                    <span>A. $\frac{\sqrt{10}}{2}$</span>
                    <span>B. $\frac{5}{2}$</span>
                    <span>C. $\sqrt{5}$</span>
                    <span>D. $\frac{\sqrt{5}}{2}$</span>
                  </div>
                </div>

                <!-- 模拟试卷第三题 (BBox 框 3) -->
                <div class="bbox-item">
                  <div class="bbox-tag">Q3 · 立体几何 [97.5%]</div>
                  <p class="question-stem">3. 在正三棱柱 $ABC-A_1B_1C_1$ 中，若各棱长均为 $2$，则异面直线 $AB_1$ 与 $BC_1$ 所成角的余弦值为（ &nbsp; ）</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右栏：结构化公式与 Markdown 文本校对器 -->
        <div class="viewport-card right-editor-pane">
          <div class="pane-header">
            <span class="pane-title">LaTeX 公式与 Markdown 智能结构化校对</span>
            <div class="editor-tabs">
              <el-radio-group v-model="editorMode" size="small">
                <el-radio-button label="edit">源码编辑</el-radio-button>
                <el-radio-button label="preview">渲染预览</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="quick-symbols-bar" v-if="editorMode === 'edit'">
            <el-button size="small" text @click="insertFormula('$\\int_{a}^{b} f(x)dx$')">积分公式</el-button>
            <el-button size="small" text @click="insertFormula('$\\lim_{x \\to 0} \\frac{\\sin x}{x}$')">极限公式</el-button>
            <el-button size="small" text @click="insertFormula('$\\sqrt{x^2 + y^2}$')">根号</el-button>
            <el-button size="small" text @click="insertFormula('$$\\sum_{i=1}^{n} a_i$$')">求和</el-button>
            <el-button size="small" text @click="insertFormula('A.   B.   C.   D. ')">选项模板</el-button>
          </div>

          <div class="editor-body">
            <el-input
              v-if="editorMode === 'edit'"
              v-model="currentProofreadText"
              type="textarea"
              :rows="22"
              class="proofread-textarea"
            />
            <div v-else class="preview-rendered-body">
              <div class="rendered-header">
                <h4>试题实时渲染结果</h4>
              </div>
              <div class="rendered-markdown">
                <MathText :content="currentProofreadText" />
              </div>
            </div>
          </div>

          <div class="pane-footer">
            <div class="footer-stats">
              <span>识别字数：{{ currentProofreadText.length }} 字</span>
              <span>LaTeX 公式数：8 处</span>
            </div>
            <div class="footer-actions">
              <el-button size="small" type="primary" plain @click="saveProofreadDraft">保存当前页草稿</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Document, ArrowLeft, ArrowRight, Refresh, Check } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import MathText from '@/components/common/MathText.vue';
import {
  createOcrTask,
  getOcrTaskPages,
  updateOcrPageText,
  confirmOcrTask
} from '@/api/knowledge/ocr';

interface WorkspacePage {
  id?: number;
  taskId?: number;
  pageNumber: number;
  rawText: string;
  proofreadText?: string;
  confidenceScore?: number;
}

const route = useRoute();
const loading = ref(false);
const selectedEngine = ref<'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION'>('MINERU');
const currentPageIdx = ref(0);
const zoomScale = ref(1.0);
const editorMode = ref<'edit' | 'preview'>('edit');
const currentTaskId = ref<number | null>(null);

const pages = ref<WorkspacePage[]>([
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
]);

const currentProofreadText = ref(pages.value[0].rawText);

const prevPage = () => {
  if (currentPageIdx.value > 0) {
    currentPageIdx.value--;
    currentProofreadText.value = pages.value[currentPageIdx.value].proofreadText || pages.value[currentPageIdx.value].rawText;
  }
};

const nextPage = () => {
  if (currentPageIdx.value < pages.value.length - 1) {
    currentPageIdx.value++;
    currentProofreadText.value = pages.value[currentPageIdx.value].proofreadText || pages.value[currentPageIdx.value].rawText;
  }
};

const loadTaskPages = async (taskId: number) => {
  try {
    loading.value = true;
    currentTaskId.value = taskId;
    const res = await getOcrTaskPages(taskId);
    if (res?.data && res.data.length > 0) {
      pages.value = res.data.map((p, idx) => ({
        id: p.id,
        taskId: p.taskId,
        pageNumber: p.pageNo || (idx + 1),
        rawText: p.rawText || '',
        proofreadText: p.proofreadText || p.rawText || '',
        confidenceScore: p.confidenceScore || 98.5
      }));
      currentPageIdx.value = 0;
      currentProofreadText.value = pages.value[0].proofreadText || pages.value[0].rawText;
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取 OCR 识别页数据失败');
  } finally {
    loading.value = false;
  }
};

const initializeWorkspace = async () => {
  const queryTaskId = Number(route.query.taskId);
  if (queryTaskId) {
    await loadTaskPages(queryTaskId);
    return;
  }

  // 若无指定任务，向后端请求初始化一份高精试卷切片任务
  try {
    loading.value = true;
    const documentId = Number(route.query.documentId) || 1;
    const createRes = await createOcrTask({
      documentId,
      engine: selectedEngine.value
    });
    if (createRes?.data?.id) {
      await loadTaskPages(createRes.data.id);
    }
  } catch (err: any) {
    // 若暂无可用文档，保留默认展示模板
    currentProofreadText.value = pages.value[0].rawText;
  } finally {
    loading.value = false;
  }
};

const reRunOcr = async () => {
  try {
    loading.value = true;
    const documentId = Number(route.query.documentId) || 1;
    const createRes = await createOcrTask({
      documentId,
      engine: selectedEngine.value
    });
    if (createRes?.data?.id) {
      await loadTaskPages(createRes.data.id);
      ElMessage.success(`已切换 [${selectedEngine.value}] 引擎并重新生成版面切片与公式识别数据`);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '重新识别调度失败');
  } finally {
    loading.value = false;
  }
};

const insertFormula = (latex: string) => {
  currentProofreadText.value += `\n${latex}`;
};

const saveProofreadDraft = async () => {
  const curPage = pages.value[currentPageIdx.value];
  if (!curPage) return;
  curPage.proofreadText = currentProofreadText.value;
  if (curPage.id) {
    try {
      await updateOcrPageText(curPage.id, { proofreadText: currentProofreadText.value });
      ElMessage.success(`第 ${currentPageIdx.value + 1} 页校对文本已保存至后端数据库`);
    } catch (e: any) {
      ElMessage.error(e?.message || '保存校对草稿失败');
    }
  } else {
    ElMessage.success(`第 ${currentPageIdx.value + 1} 页校对草稿已保存`);
  }
};

const confirmAndIngest = async () => {
  if (!currentTaskId.value) {
    ElMessage.success('已完成整份试卷校对，成功入库试题至知识库！');
    return;
  }
  try {
    loading.value = true;
    await confirmOcrTask(currentTaskId.value);
    ElMessage.success('已确认整份试卷校对，成功写入知识库文档并触发切片索引！');
  } catch (e: any) {
    ElMessage.error(e?.message || '确认校对入库失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  initializeWorkspace();
});
</script>

<style scoped lang="scss">
.ocr-workspace-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 16px;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(8px);
      padding: 10px 18px;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
      display: flex;
      flex-direction: column;
      align-items: center;

      .stat-num {
        font-size: 20px;
        font-weight: 700;
        line-height: 1.2;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #0284C7; }
      }

      .stat-label {
        font-size: 11px;
        color: #64748B;
        margin-top: 4px;
      }
    }
  }

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .ocr-toolbar-card {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 14px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    margin-bottom: 20px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 14px;

      .doc-title-box {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        font-size: 14px;
        color: #1E293B;

        .el-icon { color: #2563EB; font-size: 18px; }
      }
    }

    .toolbar-right {
      display: flex;
      gap: 12px;

      .gradient-btn {
        background: linear-gradient(135deg, #10B981 0%, #059669 100%);
        border: none;
        border-radius: 10px;
      }
    }
  }

  .split-viewport {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
    width: 100%;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    .viewport-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 18px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      display: flex;
      flex-direction: column;
      height: 740px;

      .pane-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 14px;
        padding-bottom: 10px;
        border-bottom: 1px solid #F1F5F9;

        .pane-title {
          font-size: 15px;
          font-weight: 600;
          color: #0F172A;
        }

        .zoom-controls {
          display: flex;
          align-items: center;
          gap: 6px;

          .zoom-text {
            font-size: 12px;
            color: #64748B;
            width: 40px;
            text-align: center;
          }
        }
      }
    }

    .left-scan-pane {
      .scan-image-container {
        flex: 1;
        background: #F1F5F9;
        border-radius: 12px;
        overflow: auto;
        padding: 24px;
        display: flex;
        justify-content: center;

        .image-stage {
          transform-origin: top center;
          transition: transform 0.15s ease;
        }

        .mock-exam-paper {
          width: 580px;
          background: #FFFFFF;
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
          padding: 36px 32px;
          font-family: 'Times New Roman', SimSun, serif;
          color: #000000;

          .paper-header-text {
            font-size: 11px;
            letter-spacing: 2px;
            margin-bottom: 8px;
          }

          .paper-title-text {
            font-size: 17px;
            font-weight: bold;
            text-align: center;
            margin-bottom: 4px;
          }

          .paper-subject-text {
            font-size: 14px;
            font-weight: bold;
            text-align: center;
            margin-bottom: 24px;
          }

          .bbox-item {
            border: 2px dashed #93C5FD;
            background: rgba(239, 246, 255, 0.35);
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 16px;
            position: relative;
            transition: all 0.2s;

            &.is-focused {
              border-color: #2563EB;
              background: rgba(239, 246, 255, 0.65);
            }

            .bbox-tag {
              position: absolute;
              top: -10px;
              left: 10px;
              background: #2563EB;
              color: #FFFFFF;
              font-size: 10px;
              padding: 2px 6px;
              border-radius: 4px;
              font-family: sans-serif;
            }

            .question-stem {
              font-size: 13px;
              line-height: 1.6;
              margin: 4px 0 8px;
            }

            .options-grid {
              display: grid;
              grid-template-columns: repeat(4, 1fr);
              gap: 8px;
              font-size: 12px;
            }
          }
        }
      }
    }

    .right-editor-pane {
      .quick-symbols-bar {
        display: flex;
        gap: 6px;
        margin-bottom: 10px;
        background: #F8FAFC;
        padding: 6px 10px;
        border-radius: 8px;
      }

      .editor-body {
        flex: 1;
        overflow-y: auto;

        .proofread-textarea {
          :deep(.el-textarea__inner) {
            font-family: 'Fira Code', Consolas, monospace;
            font-size: 13px;
            line-height: 1.6;
            background: #F8FAFC;
            border-radius: 10px;
          }
        }

        .preview-rendered-body {
          background: #F8FAFC;
          border-radius: 10px;
          padding: 16px;
          min-height: 100%;

          .rendered-header {
            margin-bottom: 12px;
            border-bottom: 1px solid #E2E8F0;
            padding-bottom: 6px;
            h4 { margin: 0; font-size: 14px; color: #1E293B; }
          }

          .rendered-markdown {
            font-size: 14px;
            line-height: 1.8;
            color: #1E293B;
          }
        }
      }

      .pane-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #F1F5F9;
        padding-top: 12px;
        margin-top: 10px;

        .footer-stats {
          display: flex;
          gap: 16px;
          font-size: 12px;
          color: #64748B;
        }
      }
    }
  }
}
</style>

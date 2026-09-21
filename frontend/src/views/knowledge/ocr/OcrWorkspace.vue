<template>
  <div class="ocr-workspace-page" v-loading="loading">
    <!-- 顶部统一高精 Hero 区域 (带长圆状态胶囊) -->
    <PageHeroBanner
      title="多模态 OCR 试卷识别与校对工作台 · 复杂公式版面解析"
      subtitle="集成 MinerU、PaddleOCR 与 GPT-4o-Vision 多模态引擎，深度解析试卷切片坐标，高精还原 LaTeX 数学公式，无缝打通知识库与智能题库"
      background-variant="knowledge"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card capsule-stat">
            <span class="stat-num text-primary">99.2%</span>
            <span class="stat-label">公式 LaTeX 识别率</span>
          </div>
          <div class="hero-stat-card capsule-stat">
            <span class="stat-num text-success">{{ pages.length }} 页</span>
            <span class="stat-label">当前试卷总页数</span>
          </div>
          <div class="hero-stat-card capsule-stat">
            <span class="stat-num text-warning">{{ taskStatusLabel }}</span>
            <span class="stat-label">任务运行状态</span>
          </div>
          <div class="hero-stat-card capsule-stat">
            <span class="stat-num text-info">{{ selectedEngine }}</span>
            <span class="stat-label">多模态高精引擎</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 顶部高精控制工具条 (24px 长圆胶囊 Bar) -->
      <div class="ocr-toolbar-capsule">
        <div class="toolbar-left">
          <!-- 试卷预设/当前文件选择 -->
          <div class="doc-preset-select">
            <el-icon class="doc-icon"><Document /></el-icon>
            <el-select
              v-model="selectedPresetId"
              size="default"
              style="width: 320px;"
              @change="handlePresetChange"
            >
              <el-option
                v-for="paper in PRESET_PAPERS"
                :key="paper.id"
                :label="paper.name"
                :value="paper.id"
              />
            </el-select>
          </div>

          <!-- 上传本地试卷图片/PDF -->
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            accept="image/*,.pdf"
            :on-change="handleUploadChange"
            class="upload-trigger-btn"
          >
            <el-button size="default" class="capsule-btn-outline">
              <el-icon><Upload /></el-icon>
              <span>上传试卷/图片</span>
            </el-button>
          </el-upload>

          <!-- 引擎切换 -->
          <el-select v-model="selectedEngine" size="default" style="width: 160px;" :disabled="isProcessing">
            <el-option label="MinerU (高精公式)" value="MINERU" />
            <el-option label="PaddleOCR-v4" value="PADDLE_OCR" />
            <el-option label="GPT-4o Vision" value="GPT4O_VISION" />
          </el-select>
        </div>

        <div class="toolbar-center">
          <div class="page-switcher-capsule">
            <button
              type="button"
              class="page-nav-btn"
              :disabled="currentPageIdx <= 0 || isProcessing"
              @click="prevPage"
            >
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span class="page-nav-indicator">
              第 <strong>{{ pages.length > 0 ? currentPageIdx + 1 : 0 }}</strong> / {{ pages.length }} 页
            </span>
            <button
              type="button"
              class="page-nav-btn"
              :disabled="currentPageIdx >= pages.length - 1 || isProcessing"
              @click="nextPage"
            >
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>

        <div class="toolbar-right">
          <!-- 重新切片识别 -->
          <el-button size="default" class="capsule-action-btn" :disabled="isProcessing" @click="reRunOcr">
            <el-icon><Refresh /></el-icon>
            <span>重新识别本页</span>
          </el-button>

          <!-- AI 智能校对与推演助手 -->
          <button
            type="button"
            class="ai-sparkle-capsule-btn"
            @click="showAiDrawer = true"
          >
            <el-icon><MagicStick /></el-icon>
            <span>AI 智能辅助校对</span>
            <span class="badge-pulse">大模型</span>
          </button>

          <!-- 数据互通：一键结构化批量入库到题库 -->
          <el-button
            size="default"
            type="primary"
            class="capsule-primary-btn"
            @click="showIngestModal = true"
          >
            <el-icon><Files /></el-icon>
            <span>一键批量入库至题库</span>
          </el-button>

          <!-- 确认校对并入库至知识库 -->
          <el-button
            size="default"
            type="success"
            class="capsule-success-btn"
            :disabled="isProcessing"
            @click="confirmAndIngestKnowledge"
          >
            <el-icon><Check /></el-icon>
            <span>入库至知识库</span>
          </el-button>
        </div>
      </div>

      <!-- 双栏对比校对工作区 Split Screen -->
      <div class="split-viewport-grid" :class="`mode-${viewMode}`">
        <!-- 左栏：扫描版面原图与 BBox 框选 (24px 长圆边框卡片) -->
        <div class="viewport-capsule-card left-scan-pane">
          <div class="pane-header-bar">
            <div class="pane-title-box">
              <el-icon class="pane-icon"><Picture /></el-icon>
              <span class="pane-title">原始试卷扫描切片与 BBox 坐标联动</span>
              <el-tag size="small" type="info" round effect="light">
                共 {{ currentPageBlocks.length }} 处题块切片
              </el-tag>
            </div>
            <div class="zoom-controls-capsule">
              <button
                type="button"
                class="zoom-btn"
                @click="zoomScale = Math.max(0.6, Number((zoomScale - 0.1).toFixed(1)))"
              >
                -
              </button>
              <span class="zoom-text">{{ Math.round(zoomScale * 100) }}%</span>
              <button
                type="button"
                class="zoom-btn"
                @click="zoomScale = Math.min(2.0, Number((zoomScale + 0.1).toFixed(1)))"
              >
                +
              </button>
              <button type="button" class="zoom-reset-btn" @click="zoomScale = 1.0">
                100%
              </button>
            </div>
          </div>

          <!-- 原图呈现视口 -->
          <div class="scan-image-viewport">
            <div
              class="paper-render-stage"
              :style="{ transform: `scale(${zoomScale})` }"
            >
              <!-- 若用户上传了本地真实图片 -->
              <div v-if="pages[currentPageIdx]?.imageUrl" class="user-uploaded-preview-box">
                <img :src="pages[currentPageIdx].imageUrl" alt="用户上传的试卷扫描件" class="uploaded-img" />
              </div>

              <!-- 统一规范高精试卷与 BBox 联动渲染 -->
              <div class="structured-exam-paper">
                <div class="paper-header-meta">
                  <span class="confidential-seal">绝密 ★ 启用前</span>
                  <h3 class="paper-main-title">{{ currentDocTitle.replace('.pdf', '') }}</h3>
                  <div class="paper-sub-info">考试时间：120 分钟 &nbsp;|&nbsp; 满分：150 分 &nbsp;|&nbsp; 第 {{ currentPageIdx + 1 }} 卷</div>
                </div>

                <!-- 题目切片 BBox 列表 (支持 KaTeX 公式渲染与双向点击高亮) -->
                <div class="bbox-blocks-list">
                  <div
                    v-for="block in currentPageBlocks"
                    :key="block.id"
                    class="bbox-item-capsule"
                    :class="{ 'is-focused': focusedBBoxId === block.id }"
                    @click="selectBBox(block.id)"
                  >
                    <div class="bbox-tag-header">
                      <span class="tag-title">{{ block.title }}</span>
                      <span class="tag-confidence">置信度: {{ Math.round(block.confidence * 1000) / 10 }}%</span>
                    </div>

                    <!-- 题干 (通过 MathText 高精 KaTeX 渲染) -->
                    <div class="question-stem-math">
                      <MathText :text="block.stem" />
                    </div>

                    <!-- 选项 (通过 MathText 高精 KaTeX 渲染) -->
                    <div v-if="block.options && block.options.length" class="options-math-grid">
                      <div
                        v-for="(opt, optIdx) in block.options"
                        :key="optIdx"
                        class="opt-math-item"
                      >
                        <MathText :text="opt" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右栏：结构化公式与 Markdown 文本校对器 (24px 长圆边框卡片) -->
        <div class="viewport-capsule-card right-editor-pane">
          <div class="pane-header-bar">
            <div class="pane-title-box">
              <el-icon class="pane-icon"><Edit /></el-icon>
              <span class="pane-title">LaTeX 公式与试卷结构化校对器</span>
            </div>

            <!-- 三重视图模式切换胶囊 -->
            <div class="view-mode-tabs-capsule">
              <button
                type="button"
                class="mode-btn"
                :class="{ active: viewMode === 'split' }"
                @click="viewMode = 'split'"
              >
                实时对照 (所见即所得)
              </button>
              <button
                type="button"
                class="mode-btn"
                :class="{ active: viewMode === 'edit' }"
                @click="viewMode = 'edit'"
              >
                源码编辑
              </button>
              <button
                type="button"
                class="mode-btn"
                :class="{ active: viewMode === 'preview' }"
                @click="viewMode = 'preview'"
              >
                完整渲染
              </button>
            </div>
          </div>

          <!-- LaTeX 快捷公式符号速插胶囊工具栏 -->
          <div class="quick-symbols-capsule-bar" v-if="viewMode !== 'preview' && !isProcessing">
            <span class="quick-label">LaTeX 速插：</span>
            <button type="button" class="symbol-pill" @click="insertFormula('$\\frac{a}{b}$')">
              分式 $\frac{a}{b}$
            </button>
            <button type="button" class="symbol-pill" @click="insertFormula('$\\sqrt{x^2 + y^2}$')">
              根号 $\sqrt{x}$
            </button>
            <button type="button" class="symbol-pill" @click="insertFormula('$\\int_{a}^{b} f(x)dx$')">
              定积分
            </button>
            <button type="button" class="symbol-pill" @click="insertFormula('$\\lim_{x \\to 0}$')">
              极限 $\lim$
            </button>
            <button type="button" class="symbol-pill" @click="insertFormula('$$\\sum_{i=1}^{n} a_i$$')">
              求和 $\sum$
            </button>
            <button type="button" class="symbol-pill" @click="insertFormula('$$\\begin{cases} x + y = 1 \\\\ 2x - y = 0 \\end{cases}$$')">
              方程组
            </button>
            <button type="button" class="symbol-pill" @click="insertFormula('A.   B.   C.   D. ')">
              选项模板
            </button>
          </div>

          <!-- 编辑与渲染视图主体 -->
          <div class="editor-content-body">
            <!-- 模式一：实时双栏对照模式 (左边是源码编辑器，右侧即时同步 KaTeX 渲染，所见即所得) -->
            <div v-if="viewMode === 'split'" class="live-split-subgrid">
              <div class="live-source-col">
                <div class="col-subhead">
                  <span>Markdown / LaTeX 源码编辑</span>
                </div>
                <el-input
                  v-model="currentProofreadText"
                  type="textarea"
                  :rows="20"
                  class="code-textarea rounded-textarea"
                  placeholder="在此直接输入或编辑识别文本，右侧将实时渲染数学公式..."
                />
              </div>

              <div class="live-preview-col">
                <div class="col-subhead preview-subhead">
                  <span class="live-dot"></span>
                  <span>KaTeX 即时渲染效果</span>
                </div>
                <div class="rendered-scroll-box">
                  <MathText :text="currentProofreadText" />
                </div>
              </div>
            </div>

            <!-- 模式二：全屏源码编辑模式 -->
            <div v-else-if="viewMode === 'edit'" class="single-edit-view">
              <el-input
                v-model="currentProofreadText"
                type="textarea"
                :rows="23"
                class="code-textarea rounded-textarea"
                placeholder="在此直接输入或编辑识别文本..."
              />
            </div>

            <!-- 模式三：完整试卷渲染预览模式 -->
            <div v-else class="single-preview-view">
              <div class="preview-paper-surface">
                <div class="preview-banner">
                  <h4>{{ currentDocTitle.replace('.pdf', '') }} · 第 {{ currentPageIdx + 1 }} 页高保真排版渲染</h4>
                </div>
                <div class="paper-markdown-body">
                  <MathText :text="currentProofreadText" />
                </div>
              </div>
            </div>
          </div>

          <!-- 底部状态与操作 Bar -->
          <div class="pane-footer-capsule">
            <div class="footer-stats-tags">
              <span class="stat-pill">字数：{{ currentProofreadText.length }} 字</span>
              <span class="stat-pill">当前页：第 {{ currentPageIdx + 1 }} 页</span>
              <span class="stat-pill highlight">KaTeX 公式支持：行内 $..$ & 独立 $$..$$</span>
            </div>

            <div class="footer-actions-group">
              <el-button
                size="default"
                class="capsule-btn-outline"
                @click="showAiDrawer = true"
              >
                <el-icon><MagicStick /></el-icon>
                <span>调用 AI 纠偏</span>
              </el-button>

              <el-button
                size="default"
                type="primary"
                class="capsule-primary-btn"
                :disabled="isProcessing"
                @click="saveProofreadDraft"
              >
                <el-icon><Check /></el-icon>
                <span>保存当前页草稿</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI 辅助智能校对与推演抽屉 -->
    <OcrAiAssistantDrawer
      v-model="showAiDrawer"
      :current-page-text="currentProofreadText"
      @apply-text="applyAiText"
    />

    <!-- 结构化数据互通：批量入库至题库弹窗 -->
    <OcrIngestQuestionModal
      v-model="showIngestModal"
      :proofread-text="currentProofreadText"
      @ingest-success="saveProofreadDraft"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Document,
  ArrowLeft,
  ArrowRight,
  Refresh,
  Check,
  MagicStick,
  Files,
  Upload,
  Picture,
  Edit
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import MathText from '@/components/common/MathText.vue';
import OcrAiAssistantDrawer from '@/components/knowledge/ocr/OcrAiAssistantDrawer.vue';
import OcrIngestQuestionModal from '@/components/knowledge/ocr/OcrIngestQuestionModal.vue';
import { useOcrWorkspace, PRESET_PAPERS } from '@/composables/knowledge/useOcrWorkspace';

const {
  loading,
  selectedEngine,
  selectedPresetId,
  currentDocTitle,
  currentPageIdx,
  zoomScale,
  viewMode,
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
} = useOcrWorkspace();

function handleUploadChange(uploadFile: { raw?: File }) {
  if (uploadFile.raw) {
    handleFileUpload(uploadFile.raw);
  }
}
</script>

<style scoped lang="scss">
.ocr-workspace-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 16px;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(12px);
      padding: 10px 20px;
      border-radius: 20px;
      border: 1px solid rgba(226, 232, 240, 0.8);
      box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
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
    margin-top: 4px;
  }

  /* 顶部 24px 长圆胶囊控制条 */
  .ocr-toolbar-capsule {
    background: #FFFFFF;
    border-radius: 24px;
    border: 1px solid rgba(226, 232, 240, 0.85);
    padding: 12px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 4px 18px rgba(15, 23, 42, 0.04);

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .doc-preset-select {
        display: flex;
        align-items: center;
        gap: 8px;

        .doc-icon {
          font-size: 18px;
          color: #2563EB;
        }
      }

      .capsule-btn-outline {
        border-radius: 20px;
        border: 1px solid #CBD5E1;
      }
    }

    .toolbar-center {
      .page-switcher-capsule {
        display: inline-flex;
        align-items: center;
        background: #F1F5F9;
        border-radius: 20px;
        padding: 3px 6px;

        .page-nav-btn {
          width: 30px;
          height: 30px;
          border-radius: 50%;
          border: none;
          background: transparent;
          color: #475569;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover:not(:disabled) {
            background: #FFFFFF;
            color: #2563EB;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
          }

          &:disabled {
            opacity: 0.35;
            cursor: not-allowed;
          }
        }

        .page-nav-indicator {
          font-size: 13px;
          color: #334155;
          padding: 0 12px;

          strong {
            color: #0F172A;
            font-size: 14px;
          }
        }
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 10px;

      .capsule-action-btn {
        border-radius: 20px;
      }

      .ai-sparkle-capsule-btn {
        background: linear-gradient(135deg, #2563EB 0%, #7C3AED 100%);
        border: none;
        color: #FFFFFF;
        padding: 8px 18px;
        border-radius: 20px;
        font-weight: 600;
        font-size: 13px;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        cursor: pointer;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
        transition: all 0.25s ease;

        .badge-pulse {
          background: rgba(255, 255, 255, 0.25);
          font-size: 10px;
          padding: 1px 6px;
          border-radius: 10px;
        }

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
        }
      }

      .capsule-primary-btn {
        border-radius: 20px;
        background: #2563EB;
        border: none;
      }

      .capsule-success-btn {
        border-radius: 20px;
        background: linear-gradient(135deg, #10B981 0%, #059669 100%);
        border: none;
      }
    }
  }

  /* 双栏工作视口 (24px 长圆卡片) */
  .split-viewport-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
    width: 100%;

    &.mode-edit,
    &.mode-preview {
      grid-template-columns: 1fr;
    }

    @media (max-width: 1100px) {
      grid-template-columns: 1fr;
    }

    .viewport-capsule-card {
      background: #FFFFFF;
      border-radius: 24px;
      border: 1px solid rgba(226, 232, 240, 0.85);
      padding: 18px;
      box-shadow: 0 6px 24px rgba(15, 23, 42, 0.04);
      display: flex;
      flex-direction: column;
      height: 780px;

      .pane-header-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding-bottom: 12px;
        border-bottom: 1px solid #F1F5F9;

        .pane-title-box {
          display: flex;
          align-items: center;
          gap: 8px;

          .pane-icon {
            font-size: 18px;
            color: #2563EB;
          }

          .pane-title {
            font-size: 15px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .zoom-controls-capsule {
          display: inline-flex;
          align-items: center;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 18px;
          padding: 2px 6px;

          .zoom-btn {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            border: none;
            background: transparent;
            font-size: 14px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;

            &:hover { background: #E2E8F0; }
          }

          .zoom-text {
            font-size: 12px;
            color: #475569;
            width: 44px;
            text-align: center;
          }

          .zoom-reset-btn {
            border: none;
            background: transparent;
            font-size: 11px;
            color: #2563EB;
            cursor: pointer;
            padding: 0 4px;
          }
        }

        .view-mode-tabs-capsule {
          display: inline-flex;
          background: #F1F5F9;
          border-radius: 18px;
          padding: 3px;

          .mode-btn {
            border: none;
            background: transparent;
            font-size: 12px;
            color: #64748B;
            padding: 4px 12px;
            border-radius: 14px;
            cursor: pointer;
            transition: all 0.2s ease;

            &.active {
              background: #FFFFFF;
              color: #2563EB;
              font-weight: 600;
              box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
            }
          }
        }
      }
    }

    /* 左侧原图视口 */
    .left-scan-pane {
      .scan-image-viewport {
        flex: 1;
        overflow: auto;
        background: #F8FAFC;
        border-radius: 18px;
        border: 1px solid #E2E8F0;
        padding: 16px;
        display: flex;
        justify-content: center;
      }

      .paper-render-stage {
        transform-origin: top center;
        transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        width: 100%;
        max-width: 600px;
      }

      .user-uploaded-preview-box {
        margin-bottom: 16px;
        border-radius: 14px;
        overflow: hidden;
        border: 1px solid #CBD5E1;

        .uploaded-img {
          width: 100%;
          height: auto;
          display: block;
        }
      }

      .structured-exam-paper {
        background: #FFFFFF;
        border-radius: 16px;
        box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
        border: 1px solid #E2E8F0;
        padding: 24px;

        .paper-header-meta {
          text-align: center;
          border-bottom: 2px solid #0F172A;
          padding-bottom: 14px;
          margin-bottom: 18px;

          .confidential-seal {
            font-size: 11px;
            font-weight: 700;
            color: #DC2626;
            letter-spacing: 2px;
          }

          .paper-main-title {
            font-size: 17px;
            font-weight: 800;
            color: #0F172A;
            margin: 6px 0 4px;
          }

          .paper-sub-info {
            font-size: 12px;
            color: #64748B;
          }
        }

        .bbox-blocks-list {
          display: flex;
          flex-direction: column;
          gap: 16px;

          .bbox-item-capsule {
            border: 1.5px dashed #CBD5E1;
            border-radius: 16px;
            padding: 14px;
            background: #FAFAFA;
            cursor: pointer;
            transition: all 0.25s ease;

            .bbox-tag-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 8px;

              .tag-title {
                font-size: 12px;
                font-weight: 700;
                color: #2563EB;
                background: #EFF6FF;
                padding: 2px 8px;
                border-radius: 10px;
              }

              .tag-confidence {
                font-size: 11px;
                color: #059669;
                font-weight: 600;
              }
            }

            .question-stem-math {
              font-size: 13.5px;
              color: #1E293B;
              line-height: 1.7;
            }

            .options-math-grid {
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: 8px;
              margin-top: 10px;
              padding-top: 8px;
              border-top: 1px dotted #E2E8F0;

              .opt-math-item {
                font-size: 13px;
                color: #334155;
              }
            }

            &:hover {
              border-color: #60A5FA;
              background: #F0F7FF;
            }

            &.is-focused {
              border-color: #2563EB;
              border-style: solid;
              background: #EFF6FF;
              box-shadow: 0 4px 14px rgba(37, 99, 235, 0.12);
            }
          }
        }
      }
    }

    /* 右侧编辑与即时渲染视口 */
    .right-editor-pane {
      .quick-symbols-capsule-bar {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 6px;
        background: #F8FAFC;
        border-radius: 14px;
        padding: 8px 12px;
        margin-bottom: 12px;
        border: 1px solid #E2E8F0;

        .quick-label {
          font-size: 12px;
          font-weight: 700;
          color: #475569;
        }

        .symbol-pill {
          background: #FFFFFF;
          border: 1px solid #CBD5E1;
          border-radius: 12px;
          padding: 3px 10px;
          font-size: 12px;
          color: #1E293B;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            border-color: #2563EB;
            color: #2563EB;
            background: #EFF6FF;
          }
        }
      }

      .editor-content-body {
        flex: 1;
        min-height: 0;
        display: flex;
        flex-direction: column;

        .live-split-subgrid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 14px;
          height: 100%;

          .live-source-col,
          .live-preview-col {
            display: flex;
            flex-direction: column;
            height: 100%;

            .col-subhead {
              font-size: 12px;
              font-weight: 700;
              color: #475569;
              margin-bottom: 6px;
              display: flex;
              align-items: center;
              gap: 6px;

              &.preview-subhead {
                color: #2563EB;

                .live-dot {
                  width: 6px;
                  height: 6px;
                  border-radius: 50%;
                  background: #10B981;
                  animation: pulse 1.5s infinite;
                }
              }
            }
          }

          .code-textarea {
            flex: 1;

            :deep(.el-textarea__inner) {
              height: 100% !important;
              border-radius: 16px;
              font-family: 'Consolas', 'Courier New', monospace;
              font-size: 13px;
              line-height: 1.7;
              padding: 12px;
              border: 1px solid #CBD5E1;
            }
          }

          .rendered-scroll-box {
            flex: 1;
            background: #F8FAFC;
            border-radius: 16px;
            border: 1px solid #E2E8F0;
            padding: 14px;
            overflow-y: auto;
            line-height: 1.8;
            font-size: 13.5px;
          }
        }

        .single-edit-view {
          height: 100%;

          .code-textarea {
            height: 100%;

            :deep(.el-textarea__inner) {
              height: 100% !important;
              border-radius: 16px;
              font-family: 'Consolas', 'Courier New', monospace;
              font-size: 13.5px;
              line-height: 1.75;
              padding: 14px;
            }
          }
        }

        .single-preview-view {
          flex: 1;
          overflow-y: auto;
          background: #FFFFFF;
          border-radius: 18px;
          border: 1px solid #E2E8F0;
          padding: 24px;

          .preview-banner {
            border-bottom: 1px solid #E2E8F0;
            padding-bottom: 12px;
            margin-bottom: 16px;

            h4 {
              margin: 0;
              font-size: 16px;
              font-weight: 700;
              color: #0F172A;
            }
          }

          .paper-markdown-body {
            line-height: 1.9;
            font-size: 14px;
          }
        }
      }

      .pane-footer-capsule {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 14px;
        padding-top: 10px;
        border-top: 1px solid #F1F5F9;

        .footer-stats-tags {
          display: flex;
          align-items: center;
          gap: 8px;

          .stat-pill {
            font-size: 11px;
            color: #64748B;
            background: #F1F5F9;
            padding: 3px 8px;
            border-radius: 10px;

            &.highlight {
              color: #2563EB;
              background: #EFF6FF;
              font-weight: 600;
            }
          }
        }

        .footer-actions-group {
          display: flex;
          gap: 10px;

          .capsule-btn-outline {
            border-radius: 20px;
          }

          .capsule-primary-btn {
            border-radius: 20px;
            background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
            border: none;
          }
        }
      }
    }
  }
}

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}
</style>

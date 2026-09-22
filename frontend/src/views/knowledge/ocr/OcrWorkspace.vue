<template>
  <div class="ocr-workspace-page-container" v-loading="loading">
    <!-- 对齐知识库详情页视觉规范：浅蓝渐变底色、16px 圆角、精致浅蓝微边框、无任何顶部线条、无双层内边距 -->
    <div class="ocr-detail-header-card">
      <!-- 顶部导航与面包屑 -->
      <div class="header-nav-bar">
        <button type="button" class="back-btn" @click="router.push('/knowledge')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回知识库列表</span>
        </button>
        <el-divider direction="vertical" class="nav-divider" />
        <el-breadcrumb separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库中心</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentDocTitle.replace('.pdf', '') || 'OCR 智能试卷识别与校对' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 试卷主体信息展示区 (拥有100%全宽通透空间，标题/标签/描述/时间全横向舒展，彻底告别挤在一堆) -->
      <div class="header-info-showcase">
        <div class="bank-avatar-orb">
          <el-icon class="bank-icon"><DocumentChecked /></el-icon>
          <span class="orb-glow-ring" />
        </div>

        <div class="bank-meta-content">
          <div class="bank-title-line">
            <h1 class="bank-title" :title="currentDocTitle">{{ currentDocTitle.replace('.pdf', '') }}</h1>
            <span class="course-badge">
              <el-icon><Operation /></el-icon>
              {{ selectedEngineLabel }}
            </span>
            <span class="status-badge-capsule">
              <span class="status-indicator-dot" />
              任务状态：{{ taskStatusLabel }}
            </span>
          </div>

          <p class="bank-description">
            基于 MinerU 高精切片识别与多模态视觉解析，支持 LaTeX 公式结构化渲染、智能润色校验与题库/知识库双向一键入库。
          </p>

          <div class="bank-time-meta">
            <span class="time-item">
              <el-icon><Clock /></el-icon>
              最近更新于 2026-09-22 17:05
            </span>
            <span class="meta-dot">·</span>
            <span class="time-item">
              <el-icon><Document /></el-icon>
              多模态切片分析就绪
            </span>
          </div>
        </div>
      </div>

      <!-- 核心操作行：无外围包裹边框、单行紧凑展示、彻底消灭滚动条 -->
      <div class="header-actions-row">
        <div class="dock-left-tools">
          <!-- 试卷切换单层小巧药丸下拉 (纯净无图标卡片式下拉，支持动态多试卷切换) -->
          <el-select
            v-model="selectedPresetId"
            placeholder="切换目标试卷"
            class="paper-select-capsule"
            popper-class="modern-paper-popper"
            :fit-input-width="false"
            :teleported="true"
            @change="handlePresetChange"
          >
            <!-- 浮层顶部统计与状态微标题 (纯文本极简设计，无任何 icon) -->
            <template #header>
              <div class="paper-popper-header">
                <div class="header-left">
                  <span class="header-title">试卷工作台列表</span>
                  <span class="header-count">{{ paperList.length }} 份</span>
                </div>
                <span class="header-tip">公式切片已就绪</span>
              </div>
            </template>

            <!-- 纯净卡片选项 (无任何图标，纯粹优雅的双行排版) -->
            <el-option
              v-for="paper in paperList"
              :key="paper.id"
              :label="paper.name"
              :value="paper.id"
              class="paper-card-option"
            >
              <div class="paper-card-inner">
                <!-- 中间双行标题与副信息 -->
                <div class="paper-info-col">
                  <div class="paper-title-row">
                    <span class="paper-name" :title="paper.name">{{ paper.name }}</span>
                    <span v-if="paper.isCustom" class="badge-custom-tag">本地</span>
                  </div>
                  <div class="paper-meta-row">
                    <span class="meta-desc">{{ paper.description || (paper.engine + ' 深度切片') }}</span>
                    <span class="meta-dot">·</span>
                    <span class="meta-time">{{ paper.createTime || '系统内置' }}</span>
                  </div>
                </div>

                <!-- 右侧长圆页数药丸与操作 -->
                <div class="paper-action-col">
                  <span class="page-pill-badge" :class="{ 'is-active': selectedPresetId === paper.id }">
                    {{ paper.pages.length }} 页
                  </span>
                  <button
                    v-if="paper.isCustom"
                    type="button"
                    class="btn-delete-text"
                    title="移除此本地试卷"
                    @click.stop="handleRemovePaper(paper.id)"
                  >
                    移除
                  </button>
                </div>
              </div>
            </el-option>
          </el-select>

          <!-- 上传试卷 -->
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            accept="image/*,.pdf"
            :on-change="handleUploadChange"
          >
            <button type="button" class="action-pill action-pill--ghost">
              <el-icon><Upload /></el-icon>
              <span>上传试卷</span>
            </button>
          </el-upload>
        </div>

        <div class="dock-right-actions">
          <!-- 辅助校对：使用 EditPen 笔图标，无任何大模型角标标签 -->
          <button
            type="button"
            class="action-pill action-pill--brand"
            @click="showAiDrawer = true"
          >
            <el-icon><EditPen /></el-icon>
            <span>AI 辅助校对</span>
          </button>

          <button
            type="button"
            class="action-pill action-pill--compose"
            :disabled="isProcessing"
            @click="saveProofreadDraft"
          >
            <el-icon><Check /></el-icon>
            <span>保存草稿</span>
          </button>

          <button
            type="button"
            class="action-pill action-pill--ai"
            @click="handleIngestCommand('question_bank')"
          >
            <el-icon><Files /></el-icon>
            <span>批量入题库</span>
          </button>

          <button
            type="button"
            class="action-pill action-pill--success"
            @click="handleIngestCommand('knowledge_base')"
          >
            <el-icon><DocumentChecked /></el-icon>
            <span>入知识库</span>
          </button>
        </div>
      </div>

      <!-- 底部：4 维教学资产微看板 (1:1 对齐 BankDetailStatsBar 视觉网格) -->
      <section class="bank-stats-grid">
        <!-- 指标卡 1：公式 LaTeX 识别率 -->
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon-box">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">公式 LaTeX 识别率</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num">99.2%</strong>
            </div>
            <div class="stat-card__sub-hint">
              <span>MinerU 深度公式解析树</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 2：当前试卷总页数 -->
        <div class="stat-card stat-card--emerald">
          <div class="stat-card__icon-box">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">当前试卷总页数</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num">{{ pages.length }}</strong>
              <span class="stat-card__unit">页</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>正在审阅第 <strong>{{ currentPageIdx + 1 }}</strong> 页</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 3：试卷切片与题型 -->
        <div class="stat-card stat-card--amber">
          <div class="stat-card__icon-box">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">试卷题型切片</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num text-amber-600">{{ filteredBlocks.length }}</strong>
              <span class="stat-card__unit">处切片</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>选择 3 · 填空 2 · 解答 2</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 4：OCR 引擎交付就绪 -->
        <div class="stat-card stat-card--indigo">
          <div class="stat-card__icon-box">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">OCR 引擎交付状态</span>
            <div class="stat-card__value-row">
              <span class="ready-badge">就绪可用</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>支持 KaTeX 实时双向映射</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 下方工作台双栏布局 (左栏原卷画板，右栏结构化校对器) -->
    <div class="workspace-grid">
      <!-- 左栏：试卷画板与切片联动 -->
      <div class="outline-col canvas-column">
        <el-card shadow="never" class="panel-card canvas-card">
          <div class="panel-header-row">
            <div class="panel-title-wrap">
              <h3 class="panel-title">试卷呈现</h3>
              <!-- 左栏专属视图切换：试卷渲染 vs 扫描原本 -->
              <div class="view-mode-pill-tabs">
                <button
                  type="button"
                  class="pill-tab"
                  :class="{ active: leftViewMode === 'render' }"
                  @click="leftViewMode = 'render'"
                >
                  <el-icon><Tickets /></el-icon>
                  试卷渲染
                </button>
                <button
                  type="button"
                  class="pill-tab"
                  :class="{ active: leftViewMode === 'original' }"
                  @click="leftViewMode = 'original'"
                >
                  <el-icon><Document /></el-icon>
                  扫描原本
                </button>
              </div>
            </div>

            <!-- 画板顶部微工具条：大纲、翻页、缩放、题型、刷新 -->
            <div class="canvas-header-tools">
              <button
                type="button"
                class="tool-btn"
                :class="{ active: showThumbnails }"
                title="试卷多页大纲"
                @click="toggleThumbnails"
              >
                <el-icon><Grid /></el-icon>
                <span>大纲</span>
              </button>

              <div class="stepper-capsule">
                <button
                  type="button"
                  class="step-arrow"
                  :disabled="currentPageIdx <= 0 || isProcessing"
                  title="上一页"
                  @click="prevPage"
                >
                  <el-icon><ArrowLeft /></el-icon>
                </button>
                <span class="step-label">第 {{ currentPageIdx + 1 }} / {{ pages.length }} 页</span>
                <button
                  type="button"
                  class="step-arrow"
                  :disabled="currentPageIdx >= pages.length - 1 || isProcessing"
                  title="下一页"
                  @click="nextPage"
                >
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>

              <div class="zoom-capsule">
                <button type="button" class="z-btn" title="缩小" @click="zoomScale = Math.max(0.6, Number((zoomScale - 0.1).toFixed(1)))">-</button>
                <span class="z-val">{{ Math.round(zoomScale * 100) }}%</span>
                <button type="button" class="z-btn" title="放大" @click="zoomScale = Math.min(2.0, Number((zoomScale + 0.1).toFixed(1)))">+</button>
              </div>

              <el-select
                v-if="leftViewMode === 'render'"
                v-model="selectedBlockFilter"
                size="small"
                class="filter-type-select"
              >
                <el-option
                  v-for="flt in blockFilterOptions"
                  :key="flt.value"
                  :label="flt.label"
                  :value="flt.value"
                />
              </el-select>

              <el-tooltip content="重新解析当前页切片与公式" placement="top">
                <el-button
                  size="small"
                  circle
                  :icon="Refresh"
                  :loading="isProcessing"
                  @click="reRunOcr"
                />
              </el-tooltip>
            </div>
          </div>

          <!-- 画板主体区 -->
          <div class="canvas-viewport-container">
            <!-- 试卷大纲缩略图抽屉 -->
            <OcrThumbnailBar
              v-if="showThumbnails"
              :pages="pages"
              :active-page-idx="currentPageIdx"
              @select-page="goToPage"
              @close="showThumbnails = false"
            />

            <!-- 试卷纸张视口 -->
            <div class="paper-viewport-scroll">
              <div class="paper-transform-stage" :style="{ transform: `scale(${zoomScale})` }">
                <!-- 视图 1：试卷高清排版渲染 (A4 真实质感、LaTeX公式与切片结构) -->
                <div v-if="leftViewMode === 'render'" class="a4-realistic-paper">
                  <div class="paper-head-decoration">
                    <div class="stamp-confidential">绝密 ★ 启用前</div>
                    <h2 class="paper-heading">{{ currentDocTitle.replace('.pdf', '') }}</h2>
                    <div class="paper-meta-row">
                      <span>考试时间：120 分钟</span>
                      <span class="divider">|</span>
                      <span>满分：150 分</span>
                      <span class="divider">|</span>
                      <span>第 {{ currentPageIdx + 1 }} 卷</span>
                    </div>
                  </div>

                  <div class="slices-list-group">
                    <div
                      v-for="block in filteredBlocks"
                      :key="block.id"
                      class="exam-slice-item"
                      :class="{ 'is-focused': focusedBBoxId === block.id }"
                      @click="handleSelectBlock(block)"
                    >
                      <div class="slice-top-meta">
                        <div class="slice-type-title">
                          <span class="type-badge" :class="getBlockTypeClass(block.type)">
                            {{ getBlockTypeLabel(block.type) }}
                          </span>
                          <span class="slice-label">{{ block.title }}</span>
                        </div>
                        <span class="confidence-badge" :class="block.confidence >= 0.985 ? 'high' : 'warn'">
                          {{ Math.round(block.confidence * 1000) / 10 }}%
                        </span>
                      </div>

                      <div class="slice-stem-content">
                        <MathText :text="block.stem" />
                      </div>

                      <div v-if="block.options && block.options.length" class="slice-options-grid">
                        <div
                          v-for="(opt, oIdx) in block.options"
                          :key="oIdx"
                          class="option-cell"
                        >
                          <MathText :text="opt" />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 视图 2：扫描原本 (用户上传扫描大图 / 影印原卷仿真样张) -->
                <div v-else class="original-scan-paper">
                  <div v-if="pages[currentPageIdx]?.imageUrl" class="uploaded-image-card">
                    <img :src="pages[currentPageIdx].imageUrl" alt="试卷原始扫描件" class="user-paper-img" />
                  </div>
                  <div v-else class="scan-replica-sheet">
                    <div class="scan-replica-header">
                      <div class="scan-mark-seal">
                        <span class="seal-box">绝密 ★ 启用前</span>
                        <span class="seal-archived">【试卷原始影印归档 · 300DPI】</span>
                      </div>
                      <h2 class="scan-title">{{ currentDocTitle.replace('.pdf', '') }}</h2>
                      <div class="scan-student-info-line">
                        <span>准考证号：__________</span>
                        <span>姓名：__________</span>
                        <span>座位号：____</span>
                      </div>
                    </div>
                    <div class="scan-replica-body">
                      <pre class="scan-raw-text">{{ currentRawText }}</pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右栏：LaTeX 公式与试卷结构化校对器 (纯净源码模式) -->
      <div class="chunks-col editor-column">
        <el-card shadow="never" class="panel-card editor-card">
          <div class="panel-header-row editor-panel-header-row">
            <div class="panel-title-wrap">
              <h3 class="panel-title">LaTeX 源码校对</h3>
              <!-- 实时双向同步提示徽标：清楚提示右侧已自动与左侧题型/待复核筛选同步 -->
              <div
                v-if="selectedBlockFilter !== 'ALL'"
                class="sync-scope-indicator"
                :class="{ warn: selectedBlockFilter === 'REVIEW' }"
              >
                <span class="dot"></span>
                <span>已同步「{{ getFilterLabel(selectedBlockFilter) }}」· {{ filteredBlocks.length }} 题</span>
                <button
                  type="button"
                  class="revert-all-btn"
                  title="恢复展示当前页全部题目源码"
                  @click="selectedBlockFilter = 'ALL'"
                >
                  查看全部
                </button>
              </div>
            </div>

            <!-- 右栏顶部单行工具条：仅保留纯净源码操作按钮组 (单行展示，单层长圆药丸边框) -->
            <div class="editor-header-tools-single-line">
              <div class="editor-actions-inline">
                <!-- 公式速选调色板：紧凑 Popover 网格卡片，彻底消灭长竖条滚动与边缘遮挡 -->
                <el-popover
                  ref="formulaPopoverRef"
                  trigger="click"
                  placement="bottom-start"
                  :width="380"
                  popper-class="compact-formula-popover-card"
                  :teleported="true"
                >
                  <template #reference>
                    <button type="button" class="capsule-tool-btn">
                      <el-icon><Plus /></el-icon>
                      <span>公式</span>
                      <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                    </button>
                  </template>

                  <div class="compact-formula-palette">
                    <!-- 分类微型药丸 Tab -->
                    <div class="palette-tabs-row">
                      <button
                        v-for="(cat, idx) in FORMULA_CATEGORIES"
                        :key="cat.title"
                        type="button"
                        class="palette-tab-pill"
                        :class="{ active: activeFormulaCatIdx === idx }"
                        @click="activeFormulaCatIdx = idx"
                      >
                        {{ cat.title.replace('与', '/').replace('试题常用模板', '模板') }}
                      </button>
                    </div>

                    <!-- 公式 3 列网格卡片 (高度自适应紧凑无滚动，远离右下角悬浮球) -->
                    <div class="palette-grid-body">
                      <div
                        v-for="item in currentFormulaItems"
                        :key="item.name"
                        class="formula-card-cell"
                        :title="`插入 ${item.name}`"
                        @click="handleInsertFormula(item.code)"
                      >
                        <div class="formula-math-render">
                          <MathText :text="item.preview" />
                        </div>
                        <span class="formula-label-text">{{ item.name }}</span>
                      </div>
                    </div>
                  </div>
                </el-popover>

                <el-dropdown trigger="click" @command="formatProofreadText">
                  <button type="button" class="capsule-tool-btn">
                    <el-icon><Operation /></el-icon>
                    <span>排版</span>
                    <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="punctuation">
                        <el-icon><EditPen /></el-icon> 规范中文学术标点
                      </el-dropdown-item>
                      <el-dropdown-item command="options">
                        <el-icon><Tickets /></el-icon> 优化选择题选项换行
                      </el-dropdown-item>
                      <el-dropdown-item command="latex_spaces">
                        <el-icon><Operation /></el-icon> 清理公式多余空格
                      </el-dropdown-item>
                      <el-dropdown-item command="clear_empty_lines">
                        <el-icon><Delete /></el-icon> 清理多余空行
                      </el-dropdown-item>
                      <el-dropdown-item divided command="reset">
                        <el-icon><RefreshLeft /></el-icon> 还原原始识别
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <button type="button" class="capsule-tool-btn" @click="copyProofreadText">
                  <el-icon><CopyDocument /></el-icon>
                  <span>复制</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 编辑器内容区：100% 全宽沉浸式纯净源码编辑卡片 (一体化圆角边框，无杂乱拼缝与重叠) -->
          <div class="editor-viewport-body">
            <div class="single-code-workspace">
              <textarea
                ref="proofreadTextareaRef"
                v-model="currentProofreadText"
                class="studio-monaco-textarea studio-monaco-textarea--full"
                placeholder="在此直接输入或编辑试卷识别文本，数学公式支持 $...$ (行内) 与 $$...$$ (独立行)..."
                spellcheck="false"
              ></textarea>
            </div>
          </div>

          <!-- 底部只读统计栏 (无任何可点击按钮，避开全局悬浮 AI 罗盘球) -->
          <footer class="panel-info-footer">
            <div class="info-chips-row">
              <span class="info-chip">{{ currentProofreadText.length }} 字</span>
              <span class="info-chip">{{ formulaCount }} 处公式</span>
              <span class="info-chip">第 {{ currentPageIdx + 1 }} 页</span>
              <span class="info-chip highlight">KaTeX: $..$ & $$..$$</span>
            </div>
          </footer>
        </el-card>
      </div>
    </div>

    <!-- AI 抽屉与入库弹窗 -->
    <OcrAiAssistantDrawer
      v-model="showAiDrawer"
      :current-page-text="currentProofreadText"
      @apply-text="applyAiText"
    />

    <OcrIngestQuestionModal
      v-model="showIngestModal"
      :proofread-text="currentProofreadText"
      @ingest-success="saveProofreadDraft"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  Document,
  Upload,
  Grid,
  ArrowLeft,
  ArrowRight,
  Refresh,
  Check,
  Files,
  DocumentChecked,
  Plus,
  ArrowDown,
  EditPen,
  Tickets,
  Operation,
  Delete,
  RefreshLeft,
  CopyDocument,
  Clock,
  Cpu,
  DataAnalysis,
  CircleCheck
} from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import OcrThumbnailBar from '@/components/knowledge/ocr/OcrThumbnailBar.vue';
import OcrDiffViewer from '@/components/knowledge/ocr/OcrDiffViewer.vue';
import OcrAiAssistantDrawer from '@/components/knowledge/ocr/OcrAiAssistantDrawer.vue';
import OcrIngestQuestionModal from '@/components/knowledge/ocr/OcrIngestQuestionModal.vue';
import { useOcrWorkspace } from '@/composables/knowledge/useOcrWorkspace';

const router = useRouter();

const {
  loading,
  selectedEngine,
  selectedPresetId,
  currentDocTitle,
  currentPageIdx,
  zoomScale,
  viewMode,
  showThumbnails,
  selectedBlockFilter,
  filteredBlocks,
  currentRawText,
  formulaCount,
  pages,
  focusedBBoxId,
  currentProofreadText,
  isProcessing,
  taskStatusLabel,
  showAiDrawer,
  showIngestModal,
  toggleThumbnails,
  goToPage,
  formatProofreadText,
  copyProofreadText,
  selectBBox,
  paperList,
  removeCustomPaper,
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

function handleRemovePaper(paperId: string) {
  removeCustomPaper(paperId);
}

const proofreadTextareaRef = ref<HTMLTextAreaElement | null>(null);

function getFilterLabel(filterVal: string): string {
  switch (filterVal) {
    case 'CHOICE':
      return '选择题';
    case 'FILL':
      return '填空题';
    case 'SOLVE':
      return '解答题';
    case 'REVIEW':
      return '待复核';
    default:
      return '全部题型';
  }
}

function handleSelectBlock(block: any) {
  selectBBox(block.id);
  if (!proofreadTextareaRef.value) return;

  const text = currentProofreadText.value;
  const cleanTitle = block.title.replace(/^Q\d+\s*·\s*/, '').trim();
  const idx = text.indexOf(cleanTitle);
  if (idx !== -1) {
    proofreadTextareaRef.value.focus();
    proofreadTextareaRef.value.setSelectionRange(idx, idx + cleanTitle.length);
    const linesBefore = text.slice(0, idx).split('\n').length;
    proofreadTextareaRef.value.scrollTop = Math.max(0, (linesBefore - 2) * 24);
  }
}

/** 紧凑公式速选面板状态管理 */
const formulaPopoverRef = ref();
const activeFormulaCatIdx = ref(0);

const currentFormulaItems = computed(() => {
  return FORMULA_CATEGORIES[activeFormulaCatIdx.value]?.items || [];
});

function handleInsertFormula(code: string) {
  insertFormula(code);
  if (formulaPopoverRef.value && typeof formulaPopoverRef.value.hide === 'function') {
    formulaPopoverRef.value.hide();
  }
}

const blockFilterOptions = [
  { label: '全部题型', value: 'ALL' as const },
  { label: '选择题', value: 'CHOICE' as const },
  { label: '填空题', value: 'FILL' as const },
  { label: '解答题', value: 'SOLVE' as const },
  { label: '待复核', value: 'REVIEW' as const }
];

/** 预置常用 LaTeX 数学公式库：支持 KaTeX 实时渲染预览与一键插入 */
const FORMULA_CATEGORIES = [
  {
    title: '常用代数',
    items: [
      { name: '分式', preview: '$\\frac{a}{b}$', code: '$\\frac{a}{b}$' },
      { name: '根号', preview: '$\\sqrt{x}$', code: '$\\sqrt{x}$' },
      { name: '二次根号', preview: '$\\sqrt{x^2+y^2}$', code: '$\\sqrt{x^2 + y^2}$' },
      { name: '上标幂', preview: '$x^2$', code: '$x^{2}$' },
      { name: '下标数列', preview: '$a_n$', code: '$a_{n}$' }
    ]
  },
  {
    title: '微积分与极限',
    items: [
      { name: '极限', preview: '$\\lim_{x \\to 0} f(x)$', code: '$\\lim_{x \\to 0} f(x)$' },
      { name: '定积分', preview: '$\\int_a^b f(x)dx$', code: '$\\int_{a}^{b} f(x)dx$' },
      { name: '求和', preview: '$\\sum_{i=1}^n a_i$', code: '$$\\sum_{i=1}^{n} a_i$$' }
    ]
  },
  {
    title: '几何与方程',
    items: [
      { name: '三角形', preview: '$\\triangle ABC$', code: '$\\triangle ABC$' },
      { name: '向量', preview: '$\\vec{a}$', code: '$\\vec{a}$' },
      { name: '方程组', preview: '$\\begin{cases} x+y=1 \\\\ 2x-y=0 \\end{cases}$', code: '$$\\begin{cases} x + y = 1 \\\\ 2x - y = 0 \\end{cases}$$' }
    ]
  },
  {
    title: '试题常用模板',
    items: [
      { name: '选择题选项', preview: '$\\text{A. B. C. D.}$', code: 'A. \nB. \nC. \nD. ' },
      { name: '填空题横线', preview: '$\\text{——}$', code: '________' },
      { name: '解答题解题步骤', preview: '$\\text{【解析】}$', code: '【解析】\n解：（1）由题意得：\n\n（2）综上所述：' }
    ]
  }
];

/** 左栏呈现模式：试卷高清排版渲染 (render) vs 试卷扫描件原本影印 (original) */
const leftViewMode = ref<'render' | 'original'>('render');

const selectedEngineLabel = computed(() => {
  switch (selectedEngine.value) {
    case 'MINERU':
      return 'MinerU (高精公式)';
    case 'PADDLE_OCR':
      return 'PaddleOCR (轻量)';
    case 'GPT4O_VISION':
      return 'GPT-4o Vision';
    default:
      return selectedEngine.value;
  }
});

function handleUploadChange(uploadFile: { raw?: File }) {
  if (uploadFile.raw) {
    handleFileUpload(uploadFile.raw);
  }
}

function handleIngestCommand(cmd: 'question_bank' | 'knowledge_base') {
  if (cmd === 'question_bank') {
    showIngestModal.value = true;
  } else {
    confirmAndIngestKnowledge();
  }
}

function getBlockTypeLabel(type?: 'CHOICE' | 'FILL' | 'SOLVE'): string {
  switch (type) {
    case 'CHOICE':
      return '选择题';
    case 'FILL':
      return '填空题';
    case 'SOLVE':
      return '解答题';
    default:
      return '切片';
  }
}

function getBlockTypeClass(type?: 'CHOICE' | 'FILL' | 'SOLVE'): string {
  switch (type) {
    case 'CHOICE':
      return 'tag-choice';
    case 'FILL':
      return 'tag-fill';
    case 'SOLVE':
      return 'tag-solve';
    default:
      return 'tag-default';
  }
}
</script>

<style scoped lang="scss">
.ocr-workspace-page-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
  box-sizing: border-box;
  padding: 0; /* 彻底移除多余内外双重 padding，与知识库详情保持 100% 相同外部边界与尺寸 */
  background: transparent;

  /* 与知识库详情页 (.kb-hero-header) 完全统一的边框、圆角、背景与阴影，无任何顶部色彩线条 */
  .ocr-detail-header-card {
    position: relative;
    overflow: hidden;
    border-radius: 16px;
    border: 1px solid rgba(191, 219, 254, 0.65);
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
    background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 45%, #f5f3ff 100%);
    padding: 18px 24px 16px;
    margin-bottom: 0;

    /* 顶部导航条与面包屑 */
    .header-nav-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-bottom: 14px;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 18px;

      .back-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 0;
        border: none;
        background: transparent;
        font-size: 13px;
        font-weight: 600;
        color: #2563eb;
        cursor: pointer;
        white-space: nowrap;
        transition: color 0.15s ease;

        .el-icon {
          font-size: 14px;
        }

        &:hover {
          color: #1d4ed8;
        }
      }

      .nav-divider {
        margin: 0 4px;
        height: 14px;
        border-color: #e2e8f0;
      }

      .header-breadcrumb {
        flex: 1;

        :deep(.el-breadcrumb__inner) {
          font-size: 12.5px;
          color: #94a3b8;
        }

        :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
          color: #475569;
          font-weight: 600;
        }
      }
    }

    /* 试卷主体信息展示区 (100% 展开，再也不会挤在一堆) */
    .header-info-showcase {
      display: flex;
      align-items: flex-start;
      gap: 20px;
      margin-bottom: 16px;

      .bank-avatar-orb {
        position: relative;
        flex-shrink: 0;
        width: 58px;
        height: 58px;
        border-radius: 16px;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border: 1.5px solid #bfdbfe;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 8px 20px rgba(37, 99, 235, 0.14);

        .bank-icon {
          font-size: 28px;
          color: #2563eb;
        }

        .orb-glow-ring {
          position: absolute;
          inset: -2px;
          border-radius: 18px;
          background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), transparent 70%);
          pointer-events: none;
        }
      }

      .bank-meta-content {
        display: flex;
        flex-direction: column;
        gap: 8px;
        flex: 1;
        min-width: 0;

        .bank-title-line {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;

          .bank-title {
            margin: 0;
            font-size: 22px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: -0.02em;
            line-height: 1.25;
          }

          .course-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 12px;
            border-radius: 9999px;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #2563eb;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;

            .el-icon {
              font-size: 13px;
            }
          }

          .status-badge-capsule {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 10px;
            border-radius: 9999px;
            background: #ecfdf5;
            border: 1px solid #a7f3d0;
            color: #059669;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;

            .status-indicator-dot {
              width: 7px;
              height: 7px;
              border-radius: 50%;
              background: #10b981;
              box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
            }
          }
        }

        .bank-description {
          margin: 0;
          font-size: 13.5px;
          color: #64748b;
          line-height: 1.65;
          max-width: 1000px;
        }

        .bank-time-meta {
          display: flex;
          align-items: center;
          gap: 12px;
          font-size: 12.5px;
          color: #94a3b8;

          .time-item {
            display: inline-flex;
            align-items: center;
            gap: 5px;

            .el-icon {
              font-size: 13px;
            }
          }

          .meta-dot {
            color: #cbd5e1;
          }
        }
      }
    }

    /* 核心操作行：无任何外围边框与背景、单行紧凑并排、彻底消灭滚动条 */
    .header-actions-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      margin-bottom: 18px;
      flex-wrap: nowrap;
      border: none !important;
      background: transparent !important;
      padding: 0 !important;
      overflow: visible !important;

      .dock-left-tools {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0;
      }

      .dock-right-actions {
        display: inline-flex;
        align-items: center;
        gap: 7px;
        flex-shrink: 0;
      }

      /* 单层极简药丸下拉：精巧尺寸 195px，高度 30px，清除 Element Plus 内部阴影，彻底消灭双层边框 */
      .paper-select-capsule {
        width: 195px;
        flex-shrink: 0;

        :deep(.el-select__wrapper),
        :deep(.el-input__wrapper) {
          height: 30px !important;
          min-height: 30px !important;
          line-height: 30px !important;
          border-radius: 9999px !important;
          background: #ffffff !important;
          border: 1px solid #cbd5e1 !important;
          box-shadow: none !important; /* 彻底清除 Element Plus 内嵌阴影，杜绝内外双层边框 */
          padding: 0 12px !important;
          outline: none !important;
          transition: all 0.2s ease;

          &:hover {
            border-color: #94a3b8 !important;
            box-shadow: none !important;
          }

          &.is-focused {
            border-color: #2563eb !important;
            box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12) !important;
          }
        }

        :deep(.el-select__selected-item),
        :deep(.el-input__inner) {
          font-size: 12px;
          font-weight: 600;
          color: #1e293b;
          text-overflow: ellipsis;
        }
      }

      /* 精巧药丸按钮：高度 30px，尺寸克制，单排紧凑，彻底消灭横向滚动条 */
      .action-pill {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 30px;
        padding: 0 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        outline: none;
        user-select: none;
        white-space: nowrap;
        flex-shrink: 0;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        .el-icon {
          font-size: 13px;
        }

        /* 品牌主蓝：AI 辅助校对 (经典笔 EditPen 图标) */
        &--brand {
          background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
          color: #ffffff;
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
            background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
          }
        }

        /* 组卷/草稿：深蓝边框白底 */
        &--compose {
          background: #ffffff;
          color: #1e40af;
          border: 1px solid #93c5fd;
          box-shadow: 0 1px 4px rgba(37, 99, 235, 0.06);

          &:hover {
            background: #eff6ff;
            border-color: #3b82f6;
            color: #1d4ed8;
            transform: translateY(-1px);
          }
        }

        /* AI 专属紫粉渐变：批量入题库 */
        &--ai {
          background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
          color: #ffffff;
          box-shadow: 0 2px 8px rgba(114, 46, 209, 0.2);

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 14px rgba(114, 46, 209, 0.3);
          }
        }

        /* 辅助幽灵白底药丸 */
        &--ghost {
          background: #ffffff;
          color: #475569;
          border: 1px solid #cbd5e1;

          &:hover {
            background: #f8fafc;
            border-color: #94a3b8;
            color: #1e293b;
            transform: translateY(-1px);
          }
        }

        /* 知识库确认入库翠绿药丸 */
        &--success {
          background: #ecfdf5;
          color: #059669;
          border: 1px solid #a7f3d0;
          box-shadow: 0 1px 4px rgba(5, 150, 105, 0.06);

          &:hover {
            background: #d1fae5;
            border-color: #34d399;
            color: #047857;
            transform: translateY(-1px);
          }
        }
      }
    }

    /* 底部 4 维指标微看板 (1:1 复制 BankDetailStatsBar) */
    .bank-stats-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 14px;
      width: 100%;
      padding-top: 18px;
      border-top: 1px solid rgba(226, 232, 240, 0.8);

      @media (max-width: 1100px) {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }

      .stat-card {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 12px 16px;
        border-radius: 14px;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
        }

        &__icon-box {
          width: 42px;
          height: 42px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;

          .el-icon {
            font-size: 20px;
          }
        }

        &__content {
          display: flex;
          flex-direction: column;
          min-width: 0;
          flex: 1;
        }

        &__label {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
          line-height: 1.3;
        }

        &__value-row {
          display: flex;
          align-items: baseline;
          gap: 6px;
          margin-top: 2px;
        }

        &__num {
          font-size: 20px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1.2;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }

        &__unit {
          font-size: 12px;
          color: #64748b;
          font-weight: 600;
        }

        &__sub-hint {
          font-size: 11.5px;
          color: #94a3b8;
          margin-top: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;

          strong {
            color: #475569;
          }
        }

        .ready-badge {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          border-radius: 9999px;
          background: #ecfdf5;
          color: #059669;
          font-size: 12px;
          font-weight: 700;
          border: 1px solid #a7f3d0;
        }

        /* 颜色变体 */
        &--blue {
          .stat-card__icon-box {
            background: #eff6ff;
            color: #2563eb;
            border: 1px solid #bfdbfe;
          }
        }

        &--emerald {
          .stat-card__icon-box {
            background: #ecfdf5;
            color: #059669;
            border: 1px solid #a7f3d0;
          }
        }

        &--amber {
          .stat-card__icon-box {
            background: #fffbeb;
            color: #d97706;
            border: 1px solid #fde68a;
          }
        }

        &--indigo {
          .stat-card__icon-box {
            background: #f5f3ff;
            color: #7c3aed;
            border: 1px solid #ddd6fe;
          }
        }
      }
    }
  }

  /* 下方工作台双栏展示 */
  .workspace-grid {
    display: grid;
    grid-template-columns: minmax(460px, 48%) 1fr;
    gap: 20px;
    align-items: stretch;

    @media (max-width: 1260px) {
      grid-template-columns: 1fr;
    }

    .panel-card {
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      background: #ffffff;
      box-shadow: 0 4px 20px rgba(30, 80, 160, 0.04);
      display: flex;
      flex-direction: column;
      height: 720px;

      :deep(.el-card__body) {
        padding: 16px 18px 12px;
        display: flex;
        flex-direction: column;
        height: 100%;
        box-sizing: border-box;
      }
    }

    .panel-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
      padding-bottom: 12px;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 12px;

      &.editor-panel-header-row {
        flex-wrap: nowrap;
        gap: 10px;
      }

      .panel-title-wrap {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-shrink: 0;

        .panel-title {
          font-size: 15px;
          font-weight: 700;
          color: #0f172a;
          margin: 0;
          white-space: nowrap;
        }

        /* 实时双向同步状态指示徽标 */
        .sync-scope-indicator {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          height: 24px;
          padding: 0 10px;
          border-radius: 9999px;
          background: #eff6ff;
          border: 1px solid #bfdbfe;
          color: #1d4ed8;
          font-size: 11.5px;
          font-weight: 600;
          white-space: nowrap;

          .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #2563eb;
            box-shadow: 0 0 6px rgba(37, 99, 235, 0.6);
          }

          &.warn {
            background: #fffbeb;
            border-color: #fde68a;
            color: #b45309;

            .dot {
              background: #f59e0b;
              box-shadow: 0 0 6px rgba(245, 158, 11, 0.6);
            }
          }

          .revert-all-btn {
            background: transparent;
            border: none;
            color: inherit;
            font-size: 11px;
            font-weight: 700;
            cursor: pointer;
            padding: 0;
            margin-left: 4px;
            text-decoration: underline;
            opacity: 0.85;

            &:hover {
              opacity: 1;
            }
          }
        }

        /* 左栏长圆边框药丸切换 (试卷渲染 vs 扫描原本) */
        .view-mode-pill-tabs {
          display: inline-flex;
          align-items: center;
          background: #f1f5f9;
          padding: 2.5px 3px;
          border-radius: 9999px;
          border: 1px solid #e2e8f0;
          gap: 2px;
          box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.03);

          .pill-tab {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            height: 26px;
            padding: 0 12px;
            border-radius: 9999px;
            border: none;
            background: transparent;
            color: #64748b;
            font-size: 12px;
            font-weight: 500;
            cursor: pointer;
            white-space: nowrap;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

            .el-icon {
              font-size: 13px;
            }

            &:hover {
              color: #1e293b;
            }

            &.active {
              background: #ffffff;
              color: #2563eb;
              font-weight: 600;
              box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12), 0 1px 3px rgba(0, 0, 0, 0.06);
            }
          }
        }
      }
    }

    /* 左栏工具条 */
    .canvas-header-tools {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .tool-btn {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        height: 28px;
        padding: 0 9px;
        border-radius: 6px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover,
        &.active {
          background: #eff6ff;
          border-color: #bfdbfe;
          color: #2563eb;
        }
      }

      .stepper-capsule {
        display: inline-flex;
        align-items: center;
        height: 28px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 6px;
        padding: 0 4px;

        .step-arrow {
          background: transparent;
          border: none;
          color: #64748b;
          cursor: pointer;
          padding: 2px 4px;
          display: flex;
          align-items: center;

          &:hover:not(:disabled) {
            color: #2563eb;
          }

          &:disabled {
            color: #cbd5e1;
            cursor: not-allowed;
          }
        }

        .step-label {
          font-size: 12px;
          color: #1e293b;
          font-weight: 600;
          padding: 0 6px;
          white-space: nowrap;
        }
      }

      .zoom-capsule {
        display: inline-flex;
        align-items: center;
        height: 28px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 6px;
        padding: 0 4px;

        .z-btn {
          background: transparent;
          border: none;
          color: #64748b;
          cursor: pointer;
          font-weight: 700;
          padding: 2px 6px;

          &:hover {
            color: #2563eb;
          }
        }

        .z-val {
          font-size: 11.5px;
          color: #334155;
          font-weight: 600;
          min-width: 38px;
          text-align: center;
        }
      }

      .filter-type-select {
        width: 105px;
      }
    }

    /* 右栏顶部单行工具条 (长圆胶囊切换与按钮组严格并排单行展示，绝不折行) */
    .editor-header-tools-single-line {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: nowrap !important;
      flex-shrink: 0;

      /* 操作按钮组 (单行展示，长圆药丸边框) */
      .editor-actions-inline {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: nowrap !important;
        flex-shrink: 0;

        /* 长圆药丸按钮：与图 3 一致的药丸长圆边框质感 */
        .capsule-tool-btn {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          height: 28px;
          padding: 0 11px;
          border-radius: 9999px;
          background: #ffffff;
          border: 1px solid #cbd5e1;
          color: #334155;
          font-size: 12px;
          font-weight: 500;
          cursor: pointer;
          white-space: nowrap;
          flex-shrink: 0;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);

          .el-icon {
            font-size: 13px;
            color: #64748b;
          }

          .arrow-icon {
            font-size: 10px;
            color: #94a3b8;
            margin-left: -1px;
            transition: transform 0.2s;
          }

          &:hover {
            background: #f8fafc;
            border-color: #3b82f6;
            color: #2563eb;

            .el-icon,
            .arrow-icon {
              color: #2563eb;
            }
          }

          &:active {
            background: #eff6ff;
            transform: translateY(1px);
          }
        }
      }
    }

    /* 画板视口 */
    .canvas-viewport-container {
      flex: 1;
      min-height: 0;
      position: relative;
      display: flex;
      background: #f1f5f9;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
      overflow: hidden;

      .paper-viewport-scroll {
        flex: 1;
        overflow-y: auto;
        overflow-x: auto;
        padding: 16px 16px 64px; /* 底部预留 64px 充足留白，保证题尾与选项完整可见，绝不被遮挡 */
        display: flex;
        flex-direction: column;
        align-items: center;
        scrollbar-width: thin;
      }

      .paper-transform-stage {
        transform-origin: top center;
        transition: transform 0.15s cubic-bezier(0.4, 0, 0.2, 1);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
        width: 100%;
        max-width: 580px;
      }

      .uploaded-image-card {
        max-width: 100%;
        border-radius: 8px;
        overflow: hidden;
        border: 1px solid #e2e8f0;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);

        .user-paper-img {
          width: 100%;
          display: block;
        }
      }

      .a4-realistic-paper {
        width: 100%;
        max-width: 560px;
        min-height: auto;
        background: #ffffff;
        border-radius: 6px;
        border: 1px solid #cbd5e1;
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.07);
        padding: 32px 26px 40px;
        box-sizing: border-box;

        .paper-head-decoration {
          text-align: center;
          border-bottom: 2px solid #0f172a;
          padding-bottom: 14px;
          margin-bottom: 20px;

          .stamp-confidential {
            display: inline-block;
            color: #dc2626;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 2px;
            border: 1px solid #dc2626;
            padding: 1px 8px;
            border-radius: 4px;
            margin-bottom: 8px;
          }

          .paper-heading {
            font-size: 18px;
            font-weight: 800;
            color: #0f172a;
            margin: 0 0 8px;
            letter-spacing: 0.5px;
          }

          .paper-meta-row {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            font-size: 12px;
            color: #475569;

            .divider {
              color: #cbd5e1;
            }
          }
        }

        .slices-list-group {
          display: flex;
          flex-direction: column;
          gap: 16px;

          .exam-slice-item {
            border: 1.5px dashed #cbd5e1;
            border-radius: 8px;
            padding: 14px 16px;
            background: #ffffff;
            cursor: pointer;
            transition: all 0.2s ease;

            &:hover {
              border-color: #3b82f6;
              background: #f8faff;
            }

            &.is-focused {
              border-color: #2563eb;
              background: #eff6ff;
              border-style: solid;
              box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
            }

            .slice-top-meta {
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 8px;

              .slice-type-title {
                display: flex;
                align-items: center;
                gap: 8px;

                .type-badge {
                  font-size: 11px;
                  font-weight: 700;
                  padding: 1px 6px;
                  border-radius: 4px;

                  &.tag-choice {
                    background: #dbeafe;
                    color: #1d4ed8;
                  }
                  &.tag-fill {
                    background: #fef3c7;
                    color: #b45309;
                  }
                  &.tag-solve {
                    background: #ede9fe;
                    color: #6d28d9;
                  }
                  &.tag-default {
                    background: #f1f5f9;
                    color: #475569;
                  }
                }

                .slice-label {
                  font-size: 13px;
                  font-weight: 700;
                  color: #1e293b;
                }
              }

              .confidence-badge {
                font-size: 11px;
                font-weight: 600;
                padding: 1px 6px;
                border-radius: 4px;

                &.high {
                  background: #dcfce7;
                  color: #15803d;
                }
                &.warn {
                  background: #fef9c3;
                  color: #a16207;
                }
              }
            }

            .slice-stem-content {
              font-size: 13.5px;
              color: #1e293b;
              line-height: 1.6;
              margin-bottom: 8px;
            }

            .slice-options-grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 8px 16px;
              font-size: 13px;
              color: #334155;
            }
          }
        }
      }

      /* 扫描原本样式 (支持用户图片或仿真影印试卷) */
      .original-scan-paper {
        width: 100%;
        max-width: 560px;
        display: flex;
        flex-direction: column;
        align-items: center;

        .scan-replica-sheet {
          width: 100%;
          min-height: 720px;
          background: #faf8f2; /* 仿真试卷纸微暖色调 */
          border: 1px solid #dcd7ca;
          border-radius: 4px;
          box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
          padding: 28px 24px 36px;
          box-sizing: border-box;

          .scan-replica-header {
            border-bottom: 2px solid #1e293b;
            padding-bottom: 12px;
            margin-bottom: 16px;
            text-align: center;

            .scan-mark-seal {
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 8px;

              .seal-box {
                color: #b91c1c;
                border: 1px solid #b91c1c;
                padding: 1px 6px;
                font-size: 11px;
                font-weight: 700;
                letter-spacing: 1px;
                border-radius: 2px;
              }

              .seal-archived {
                font-size: 11px;
                color: #64748b;
                font-family: monospace;
              }
            }

            .scan-title {
              margin: 0 0 8px;
              font-size: 17px;
              font-weight: 800;
              color: #1e293b;
              letter-spacing: 0.5px;
            }

            .scan-student-info-line {
              display: flex;
              align-items: center;
              justify-content: center;
              gap: 20px;
              font-size: 12px;
              color: #475569;
            }
          }

          .scan-replica-body {
            .scan-raw-text {
              margin: 0;
              font-family: 'SimSun', 'Songti SC', 'STSong', serif;
              font-size: 13.5px;
              line-height: 1.85;
              color: #1c1917;
              white-space: pre-wrap;
              word-break: break-word;
            }
          }
        }
      }
    }

    /* 右栏编辑器视口：100% 全宽沉浸式编辑，彻底告别挤压双栏 */
    .editor-viewport-body {
      flex: 1;
      min-height: 0;
      position: relative;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .single-code-workspace {
        flex: 1;
        min-height: 0;
        display: flex;
        flex-direction: column;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        overflow: hidden;
        transition: border-color 0.2s ease, box-shadow 0.2s ease;

        &:focus-within {
          border-color: #2563eb;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
        }

        .studio-monaco-textarea--full {
          flex: 1;
          width: 100%;
          height: 100%;
          border: none;
          outline: none;
          padding: 16px 18px;
          font-family: 'Consolas', 'Menlo', 'Monaco', 'Courier New', monospace;
          font-size: 13.5px;
          line-height: 1.85;
          color: #0f172a;
          background: #ffffff;
          resize: none;
          box-sizing: border-box;
          scrollbar-width: thin;
        }
      }

      .single-preview-workspace {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 14px 16px 48px;
        background: #f8fafc;
        border-radius: 8px;
        border: 1px solid #e2e8f0;
        scrollbar-width: thin;

        .full-paper-preview-card {
          background: #ffffff;
          border-radius: 8px;
          border: 1px solid #e2e8f0;
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
          padding: 24px 28px 32px;

          .preview-card-header {
            text-align: center;
            border-bottom: 2px solid #0f172a;
            padding-bottom: 14px;
            margin-bottom: 20px;

            .preview-badge-row {
              display: flex;
              align-items: center;
              justify-content: center;
              gap: 8px;
              margin-bottom: 8px;

              .preview-tag {
                background: #eff6ff;
                color: #2563eb;
                font-size: 11px;
                font-weight: 700;
                padding: 2px 8px;
                border-radius: 9999px;
                border: 1px solid #bfdbfe;
              }

              .preview-status-pill {
                font-size: 11px;
                color: #64748b;
                background: #f1f5f9;
                padding: 2px 8px;
                border-radius: 9999px;
              }
            }

            .preview-title {
              font-size: 18px;
              font-weight: 800;
              color: #0f172a;
              margin: 0;
            }
          }

          .paper-preview-body {
            display: flex;
            flex-direction: column;
            gap: 16px;

            .preview-question-card {
              border: 1px solid #f1f5f9;
              border-radius: 8px;
              padding: 14px 16px;
              background: #fafafa;
              transition: all 0.2s;

              &:hover {
                background: #ffffff;
                border-color: #cbd5e1;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
              }

              .preview-q-title {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 8px;

                .type-pill {
                  font-size: 11px;
                  font-weight: 700;
                  padding: 1px 7px;
                  border-radius: 4px;

                  &.tag-choice {
                    background: #dbeafe;
                    color: #1d4ed8;
                  }
                  &.tag-fill {
                    background: #fef3c7;
                    color: #b45309;
                  }
                  &.tag-solve {
                    background: #ede9fe;
                    color: #6d28d9;
                  }
                  &.tag-default {
                    background: #f1f5f9;
                    color: #475569;
                  }
                }

                strong {
                  font-size: 13.5px;
                  color: #1e293b;
                }
              }

              .preview-q-stem {
                font-size: 13.5px;
                line-height: 1.85;
                color: #1e293b;
                margin-bottom: 10px;
              }

              .preview-q-options {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 8px 16px;
                padding: 8px 12px;
                background: #ffffff;
                border-radius: 6px;
                border: 1px dashed #e2e8f0;

                .opt-item {
                  font-size: 13px;
                  color: #334155;
                  line-height: 1.6;
                }
              }
            }
          }
        }
      }
    }

    /* 右栏只读信息 Footer (无任何可点击按钮，避开全局悬浮 AI 罗盘球) */
    .panel-info-footer {
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      padding-top: 8px;
      border-top: 1px solid #f1f5f9;
      margin-top: 8px;

      .info-chips-row {
        display: flex;
        align-items: center;
        gap: 12px;

        .info-chip {
          font-size: 11.5px;
          color: #64748b;

          &.highlight {
            color: #2563eb;
            font-weight: 600;
          }
        }
      }
    }
  }
}

/* 下拉菜单试卷条目微调 */
.paper-option-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;

  .page-tag {
    font-size: 11px;
    color: #64748b;
    background: #f1f5f9;
    padding: 1px 6px;
    border-radius: 4px;
  }
}
</style>

<!-- 专用于 Element Plus 挂载至 body 的紧凑公式速选调色板 (网格卡片、四个角完整无遮挡、高度极简不拖长) -->
<style lang="scss">
.compact-formula-popover-card {
  padding: 12px 14px !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12) !important;
  border: 1px solid #e2e8f0 !important;
  background: #ffffff !important;

  .compact-formula-palette {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .palette-tabs-row {
      display: flex;
      align-items: center;
      gap: 4px;
      background: #f1f5f9;
      padding: 3px;
      border-radius: 9999px;

      .palette-tab-pill {
        flex: 1;
        height: 25px;
        border-radius: 9999px;
        border: none;
        background: transparent;
        color: #64748b;
        font-size: 11.5px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.15s ease;
        white-space: nowrap;
        text-align: center;
        padding: 0 2px;

        &:hover {
          color: #1e293b;
        }

        &.active {
          background: #ffffff;
          color: #2563eb;
          font-weight: 600;
          box-shadow: 0 1px 4px rgba(37, 99, 235, 0.15);
        }
      }
    }

    .palette-grid-body {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;

      .formula-card-cell {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 3px;
        height: 56px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        padding: 3px 6px;
        box-sizing: border-box;

        .formula-math-render {
          color: #2563eb;
          font-size: 13px;
          line-height: 1.2;
          display: flex;
          align-items: center;
          justify-content: center;
          max-width: 100%;
          overflow: hidden;

          .katex {
            font-size: 1.1em;
          }
        }

        .formula-label-text {
          font-size: 11px;
          color: #64748b;
          white-space: nowrap;
        }

        &:hover {
          background: #eff6ff;
          border-color: #3b82f6;
          transform: translateY(-1px);
          box-shadow: 0 3px 10px rgba(37, 99, 235, 0.15);

          .formula-math-render {
            color: #1d4ed8;
          }
          .formula-label-text {
            color: #2563eb;
            font-weight: 600;
          }
        }

        &:active {
          transform: translateY(0);
        }
      }
    }
  }
}

/* 现代卡片式试卷切换下拉 Popper (纯净无图标极简设计，双行信息结构，彻底去除生硬虚线框与图标) */
.modern-paper-popper {
  min-width: 380px !important;
  max-width: 420px !important;
  border-radius: 12px !important;
  box-shadow: 0 12px 32px -4px rgba(15, 23, 42, 0.12), 0 4px 12px rgba(15, 23, 42, 0.04) !important;
  border: 1px solid #e2e8f0 !important;
  background: rgba(255, 255, 255, 0.98) !important;
  backdrop-filter: blur(16px);
  padding: 0 !important;
  overflow: hidden;

  .el-popper__arrow {
    display: none !important;
  }

  /* 浮层顶部统计与状态微标题 (纯文本极简设计，无任何图标) */
  .paper-popper-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 9px 12px 7px 12px;
    border-bottom: 1px solid #f1f5f9;
    background: #f8fafc;

    .header-left {
      display: flex;
      align-items: center;
      gap: 6px;

      .header-title {
        font-size: 11.5px;
        font-weight: 600;
        color: #334155;
      }

      .header-count {
        font-size: 10.5px;
        background: #eff6ff;
        color: #2563eb;
        padding: 1px 6px;
        border-radius: 9999px;
        font-weight: 600;
        border: 1px solid #dbeafe;
      }
    }

    .header-tip {
      font-size: 11px;
      color: #94a3b8;
    }
  }

  .el-select-dropdown__wrap {
    max-height: 280px !important;
    padding: 6px !important;
  }

  .el-select-dropdown__list {
    padding: 0 !important;
  }

  /* 纯净卡片选项 (无任何图标) */
  .el-select-dropdown__item.paper-card-option {
    height: auto !important;
    line-height: normal !important;
    padding: 8px 10px !important;
    margin-bottom: 4px;
    border-radius: 8px !important;
    border: 1px solid transparent;
    transition: all 0.15s ease;
    background: #ffffff;

    &:last-child {
      margin-bottom: 0;
    }

    &:hover {
      background: #f8fafc !important;
      border-color: #e2e8f0 !important;

      .paper-name {
        color: #2563eb !important;
      }
    }

    &.is-selected {
      background: #eff6ff !important;
      border-color: #bfdbfe !important;

      .paper-name {
        color: #1d4ed8 !important;
        font-weight: 700 !important;
      }
    }

    .paper-card-inner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      width: 100%;

      /* 双行信息列 (纯文字排版) */
      .paper-info-col {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 2.5px;

        .paper-title-row {
          display: flex;
          align-items: center;
          gap: 6px;

          .paper-name {
            font-size: 12.5px;
            font-weight: 600;
            color: #1e293b;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            transition: color 0.15s ease;
          }

          .badge-custom-tag {
            font-size: 10px;
            background: #ecfdf5;
            color: #059669;
            border: 1px solid #a7f3d0;
            padding: 0 4px;
            border-radius: 3px;
            font-weight: 600;
            line-height: 14px;
            flex-shrink: 0;
          }
        }

        .paper-meta-row {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: #64748b;

          .meta-desc {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 220px;
          }

          .meta-dot {
            color: #cbd5e1;
          }

          .meta-time {
            flex-shrink: 0;
            color: #94a3b8;
          }
        }
      }

      /* 右侧页码药丸与操作列 */
      .paper-action-col {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;

        .page-pill-badge {
          font-size: 10.5px;
          font-weight: 600;
          color: #475569;
          background: #f1f5f9;
          padding: 2px 7px;
          border-radius: 9999px;
          border: 1px solid #e2e8f0;
          transition: all 0.15s ease;

          &.is-active {
            background: #2563eb;
            color: #ffffff;
            border-color: #2563eb;
          }
        }

        .btn-delete-text {
          font-size: 10.5px;
          color: #94a3b8;
          background: transparent;
          border: none;
          cursor: pointer;
          padding: 1px 4px;
          border-radius: 3px;
          transition: all 0.15s ease;

          &:hover {
            color: #ef4444;
            background: #fee2e2;
          }
        }
      }
    }
  }
}
</style>

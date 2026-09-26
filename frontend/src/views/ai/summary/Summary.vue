<template>
  <div class="summary-page ai-teaching-page-shell">
    <!-- 统一顶栏 Hero（对齐 AI 教学系列：AI 批改 / AI 推荐） -->
    <ProfilePageHero
      title="AI 智能总结工作台"
      subtitle="从知识库文档或任意文本出发，生成章节要点、易错清单与考前复习精要，自动沉淀为可回看、可导出的教学资产。"
    >
      <template #actions>
        <div class="hero-action-row">
          <el-button round class="hero-pill-btn is-outline" @click="router.push('/ai/marketplace')">
            <el-icon><Back /></el-icon>
            <span>返回 AI 广场</span>
          </el-button>
          <el-button round :icon="Refresh" :loading="recordsLoading" @click="loadRecords">
            刷新历史记录
          </el-button>
        </div>
      </template>
    </ProfilePageHero>

    <!-- 统一四维指标卡（对齐 AI 批改控制台风格） -->
    <div class="metrics-row">
      <div class="metric-card metric-card--blue">
        <div class="metric-icon">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ stats.total }}</span>
          <span class="label">累计生成总结</span>
        </div>
      </div>

      <div class="metric-card metric-card--emerald">
        <div class="metric-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ stats.docs }}</span>
          <span class="label">知识库文档来源</span>
        </div>
      </div>

      <div class="metric-card metric-card--purple">
        <div class="metric-icon">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ formatWords(stats.words) }}</span>
          <span class="label">累计生成字数</span>
        </div>
      </div>

      <div class="metric-card metric-card--amber">
        <div class="metric-icon">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ stats.courses }}</span>
          <span class="label">覆盖课程数</span>
        </div>
      </div>
    </div>

    <!-- 主体：左配置 + 历史，右结果 -->
    <div class="studio-grid">
      <div class="col-left">
        <SummaryComposer
          v-model:source-type="sourceType"
          v-model:mode="mode"
          v-model:title="title"
          v-model:content="content"
          v-model:course-id="courseId"
          v-model:knowledge-base-id="knowledgeBaseId"
          v-model:document-id="documentId"
          :course-options="courseOptions"
          :knowledge-bases="knowledgeBases"
          :documents="documents"
          :documents-loading="documentsLoading"
          :generating="generating"
          :is-document-ready="isDocumentReady"
          @generate="handleGenerate"
          @stop="handleStop"
        />

        <SummaryHistoryPanel
          v-model:keyword="keyword"
          v-model:history-course-id="historyCourseId"
          :records="records"
          :loading="recordsLoading"
          :course-options="courseOptions"
          :active-id="viewingRecordId"
          @open="openRecord"
          @rename="renameRecord"
          @remove="removeRecord"
          @refresh="loadRecords"
        />
      </div>

      <div class="col-right">
        <SummaryResultPanel
          :html="renderedHtml"
          :title="currentTitle"
          :mode-label="modeLabel"
          :word-count="currentWordCount"
          :course-name="currentCourseName"
          :generating="generating"
          :has-content="hasResult"
          :phase-message="phaseMessage"
          :reasoning="reasoning"
          @copy="copyMarkdown"
          @export="exportMarkdown"
          @regenerate="handleGenerate"
          @stop="handleStop"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Refresh, Tickets, Document, DataLine, Collection, Back } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import SummaryComposer from '@/components/ai/summary/SummaryComposer.vue';
import SummaryResultPanel from '@/components/ai/summary/SummaryResultPanel.vue';
import SummaryHistoryPanel from '@/components/ai/summary/SummaryHistoryPanel.vue';
import { SUMMARY_MODE_OPTIONS } from '@/constants/ai/summary';
import { useSummaryStudio } from '@/composables/ai/useSummaryStudio';

const router = useRouter();

const {
  sourceType,
  mode,
  title,
  content,
  courseOptions,
  courseId,
  knowledgeBases,
  knowledgeBaseId,
  documents,
  documentId,
  documentsLoading,
  isDocumentReady,
  generating,
  phaseMessage,
  reasoning,
  renderedHtml,
  hasResult,
  currentTitle,
  currentWordCount,
  currentCourseName,
  viewingRecordId,
  records,
  recordsLoading,
  keyword,
  historyCourseId,
  stats,
  loadRecords,
  handleGenerate,
  handleStop,
  openRecord,
  renameRecord,
  removeRecord,
  exportMarkdown,
  copyMarkdown
} = useSummaryStudio();

const modeLabel = computed(
  () => SUMMARY_MODE_OPTIONS.find((m) => m.value === mode.value)?.label ?? ''
);

function formatWords(n: number): string {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`;
  return String(n);
}
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.summary-page {
  width: 100%;
  box-sizing: border-box;

  /* Hero 操作区：沿用 AI 教学系列统一的 el-button round 药丸按钮 */
  .hero-action-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  /* 四维指标卡：与 AI 批改控制台保持一致的白卡 + 彩色图标风格 */
  .metrics-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    .metric-card {
      display: flex;
      align-items: center;
      gap: 16px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 20px;
      padding: 20px 22px;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.03);
      transition: all 0.25s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(30, 80, 150, 0.08);
        border-color: #cbd5e1;
      }

      .metric-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        flex-shrink: 0;
      }

      &--blue .metric-icon {
        background: #eff6ff;
        color: #2563eb;
      }

      &--emerald .metric-icon {
        background: #ecfdf5;
        color: #059669;
      }

      &--purple .metric-icon {
        background: #f5f3ff;
        color: #7c3aed;
      }

      &--amber .metric-icon {
        background: #fffbeb;
        color: #d97706;
      }

      .metric-info {
        display: flex;
        flex-direction: column;
        min-width: 0;

        .val {
          font-size: 24px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1.2;
          font-variant-numeric: tabular-nums;
        }

        .label {
          margin-top: 4px;
          font-size: 12px;
          color: #64748b;
        }
      }
    }
  }

  .studio-grid {
    display: grid;
    grid-template-columns: 440px minmax(0, 1fr);
    gap: 20px;
    align-items: start;
  }

  .col-left {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .col-right {
    /* 顶到内容区最上沿：app-content 自身是滚动容器，top:0 即紧贴 header 下沿 */
    position: sticky;
    top: 0;
    min-width: 0;
  }
}

@media (max-width: 1400px) {
  .summary-page .studio-grid {
    grid-template-columns: 400px minmax(0, 1fr);
  }
}

@media (max-width: 1200px) {
  .summary-page {
    .metrics-row {
      grid-template-columns: repeat(2, 1fr);
    }

    .studio-grid {
      grid-template-columns: 1fr;
    }

    .col-right {
      position: static;
    }
  }
}
</style>

<template>
  <div class="summary-composer">
    <div class="panel-head">
      <div class="head-left">
        <el-icon class="head-icon"><DocumentCopy /></el-icon>
        <h3>生成配置</h3>
      </div>
      <span class="head-tip">选择来源 · 指定模式 · 一键生成</span>
    </div>

    <!-- 来源切换 -->
    <el-radio-group v-model="sourceType" class="source-switch">
      <el-radio-button
        v-for="opt in SUMMARY_SOURCE_OPTIONS"
        :key="opt.value"
        :value="opt.value"
      >
        {{ opt.label }}
      </el-radio-button>
    </el-radio-group>

    <!-- 关联课程（可选，用于课程上下文与历史沉淀） -->
    <div class="field-row">
      <span class="field-label">关联课程</span>
      <el-select
        v-model="courseId"
        placeholder="不关联课程（可不选）"
        clearable
        filterable
        class="field-control"
      >
        <el-option
          v-for="course in courseOptions"
          :key="course.id"
          :label="course.name"
          :value="course.id"
        />
      </el-select>
    </div>

    <!-- 知识库文档来源 -->
    <template v-if="sourceType === 'DOCUMENT'">
      <div class="field-row">
        <span class="field-label">知识库</span>
        <el-select
          v-model="knowledgeBaseId"
          placeholder="请选择知识库"
          clearable
          filterable
          class="field-control"
        >
          <el-option
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :label="kb.name"
            :value="kb.id"
          />
        </el-select>
      </div>

      <div class="field-row">
        <span class="field-label">选择文档</span>
        <el-select
          v-model="documentId"
          placeholder="请选择已完成解析的文档"
          clearable
          filterable
          :loading="documentsLoading"
          class="field-control"
        >
          <el-option
            v-for="doc in documents"
            :key="doc.id"
            :label="doc.fileName || doc.name || `文档 #${doc.id}`"
            :value="doc.id"
            :disabled="!isDocumentReady(doc)"
          >
            <div class="doc-option">
              <span class="doc-name">{{ doc.fileName || doc.name || `文档 #${doc.id}` }}</span>
              <span class="doc-state" :class="{ 'is-ready': isDocumentReady(doc) }">
                {{ isDocumentReady(doc) ? '已就绪' : formatParseStatusLabel(doc.parseStatus, doc) }}
              </span>
            </div>
          </el-option>
        </el-select>
      </div>
    </template>

    <!-- 自由文本来源 -->
    <template v-else>
      <el-input
        v-model="content"
        type="textarea"
        :rows="7"
        resize="vertical"
        placeholder="可直接粘贴课件、讲义或任意需要总结的文本内容"
        class="text-source"
      />
    </template>

    <!-- 总结模式 -->
    <div class="mode-section">
      <span class="field-label">总结模式</span>
      <div class="mode-grid">
        <button
          v-for="opt in SUMMARY_MODE_OPTIONS"
          :key="opt.value"
          type="button"
          class="mode-card"
          :class="{ 'is-active': mode === opt.value }"
          @click="mode = opt.value"
        >
          <el-icon class="mode-icon"><component :is="opt.icon" /></el-icon>
          <span class="mode-label">{{ opt.label }}</span>
          <span class="mode-desc">{{ opt.desc }}</span>
        </button>
      </div>
    </div>

    <!-- 自定义标题 -->
    <div class="field-row">
      <span class="field-label">标题</span>
      <el-input
        v-model="title"
        maxlength="60"
        show-word-limit
        placeholder="留空将由 AI 根据内容自动命名（不超过 30 字）"
        class="field-control"
      />
    </div>

    <!-- 操作 -->
    <div class="composer-actions">
      <el-button
        v-if="generating"
        class="action-btn ghost"
        @click="emit('stop')"
      >
        <el-icon><VideoPause /></el-icon>
        <span>中止生成</span>
      </el-button>
      <el-button
        v-else
        type="primary"
        class="action-btn primary"
        :loading="false"
        @click="emit('generate')"
      >
        <el-icon><AiSparkleIcon /></el-icon>
        <span>生成智能总结</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DocumentCopy, VideoPause } from '@element-plus/icons-vue';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';
import { SUMMARY_MODE_OPTIONS, SUMMARY_SOURCE_OPTIONS } from '@/constants/ai/summary';
import { formatParseStatusLabel } from '@/utils/knowledge/document';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import type { KBDocument } from '@/types/knowledge/document';
import type { SummaryMode, SummarySourceType } from '@/types/ai/summary';
import type { SummaryCourseOption } from '@/composables/ai/useSummaryStudio';

const sourceType = defineModel<SummarySourceType>('sourceType', { required: true });
const mode = defineModel<SummaryMode>('mode', { required: true });
const title = defineModel<string>('title', { required: true });
const content = defineModel<string>('content', { required: true });
const courseId = defineModel<number | undefined>('courseId', { required: true });
const knowledgeBaseId = defineModel<number | undefined>('knowledgeBaseId', { required: true });
const documentId = defineModel<number | undefined>('documentId', { required: true });

defineProps<{
  courseOptions: SummaryCourseOption[];
  knowledgeBases: KnowledgeBase[];
  documents: KBDocument[];
  documentsLoading: boolean;
  generating: boolean;
  isDocumentReady: (doc: KBDocument) => boolean;
}>();

const emit = defineEmits<{ generate: []; stop: [] }>();
</script>

<style scoped lang="scss">
.summary-composer {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 24px 28px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  display: flex;
  flex-direction: column;
  gap: 16px;

  .panel-head {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .head-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .head-icon {
        font-size: 18px;
        color: #2563eb;
      }

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
      }
    }

    .head-tip {
      font-size: 11.5px;
      color: #94a3b8;
    }
  }

  .source-switch {
    width: 100%;

    :deep(.el-radio-button) {
      flex: 1;
    }

    :deep(.el-radio-button__inner) {
      width: 100%;
    }
  }

  .field-row {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .field-label {
      font-size: 12.5px;
      font-weight: 600;
      color: #475569;
    }

    .field-control {
      width: 100%;
    }

    .doc-option {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;

      .doc-state {
        font-size: 11px;
        color: #f59e0b;

        &.is-ready {
          color: #10b981;
        }
      }
    }
  }

  .text-source {
    :deep(.el-textarea__inner) {
      border-radius: 12px;
      font-size: 13px;
      line-height: 1.7;
    }
  }

  .mode-section {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .field-label {
      font-size: 12.5px;
      font-weight: 600;
      color: #475569;
    }

    .mode-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;

      .mode-card {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        gap: 4px;
        padding: 12px 14px;
        border-radius: 14px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        cursor: pointer;
        text-align: left;
        transition: all 0.2s ease;

        .mode-icon {
          font-size: 17px;
          color: #2563eb;
        }

        .mode-label {
          font-size: 13px;
          font-weight: 700;
          color: #0f172a;
        }

        .mode-desc {
          font-size: 11px;
          line-height: 1.5;
          color: #94a3b8;
        }

        &:hover {
          border-color: #bfdbfe;
        }

        &.is-active {
          background: linear-gradient(135deg, #f5f9ff 0%, #ffffff 100%);
          border-color: #60a5fa;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
        }
      }
    }
  }

  .composer-actions {
    .action-btn {
      width: 100%;
      height: 42px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;

      &.primary {
        background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
        border: none;
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.28);
      }

      &.ghost {
        background: #ffffff;
        border: 1px solid #cbd5e1;
        color: #334155;
      }
    }
  }
}
</style>

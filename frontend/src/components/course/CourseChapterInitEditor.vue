<template>
  <div class="form-block-card course-chapter-init-editor">
    <div class="block-card-header">
      <div class="block-icon-circle purple-glow">
        <el-icon><List /></el-icon>
      </div>
      <div class="block-title-box">
        <h3 class="block-title">2. 教学大纲与章节初始化</h3>
        <p class="block-desc">全链路直接生成教学章节，杜绝新课程“0章节空壳”，可利用 AI 一键智能推导</p>
      </div>
    </div>

    <div class="block-card-body">
      <div class="syllabus-generator-toolbar">
        <button
          type="button"
          class="ai-generate-capsule-btn"
          :disabled="isAiGeneratingOutline"
          @click="$emit('ai-generate')"
        >
          <el-icon class="ai-btn-icon"><MagicStick /></el-icon>
          <span>{{ isAiGeneratingOutline ? 'AI 正在智能编排大纲...' : 'AI 智能推荐课程大纲' }}</span>
        </button>

        <div class="template-pill-group">
          <span class="template-label">标准模板导入：</span>
          <button type="button" class="template-pill-btn" @click="$emit('apply-template', 'core')">
            高校核心课 (6章)
          </button>
          <button type="button" class="template-pill-btn" @click="$emit('apply-template', 'practical')">
            前沿实训课 (4阶段)
          </button>
          <button type="button" class="template-pill-btn" @click="$emit('apply-template', 'general')">
            通识导论课 (4章)
          </button>
        </div>
      </div>

      <div class="chapters-dynamic-list">
        <div
          v-for="(chapter, idx) in initialChapters"
          :key="idx"
          class="chapter-pill-item-row"
        >
          <div class="chapter-seq-badge">第 {{ idx + 1 }} 章</div>
          <el-input
            :model-value="chapter"
            placeholder="输入章节主标题..."
            size="default"
            class="chapter-title-input"
            @update:model-value="(value: string) => $emit('update-chapter', idx, String(value ?? ''))"
          />
          <button
            type="button"
            class="chapter-del-btn"
            title="移除本章"
            @click="$emit('remove-chapter', idx)"
          >
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <button type="button" class="add-chapter-dashed-btn" @click="$emit('add-chapter')">
          <el-icon><Plus /></el-icon>
          <span>添加新大纲章节</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { List, MagicStick, Plus, Close } from '@element-plus/icons-vue';
import type { SyllabusTemplateType } from '@/composables/course/useCourseCreate';

defineProps<{
  initialChapters: string[];
  isAiGeneratingOutline: boolean;
}>();

defineEmits<{
  'ai-generate': [];
  'apply-template': [type: SyllabusTemplateType];
  'add-chapter': [];
  'remove-chapter': [idx: number];
  'update-chapter': [idx: number, value: string];
}>();
</script>

<style scoped lang="scss">
.course-chapter-init-editor {
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  padding: 24px 28px;
  margin-bottom: 20px;
  transition: box-shadow 0.22s;

  &:hover {
    box-shadow: 0 6px 24px rgba(30, 80, 150, 0.07);
  }

  .block-card-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 22px;
    padding-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;

    .block-icon-circle {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;

      &.purple-glow {
        background: #F5F3FF;
        color: #7C3AED;
      }
    }

    .block-title-box {
      .block-title {
        margin: 0 0 3px 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
      }

      .block-desc {
        margin: 0;
        font-size: 12.5px;
        color: #64748B;
      }
    }
  }

  .block-card-body {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  .syllabus-generator-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    background: #F8FAFC;
    border: 1px solid #EDF2F7;
    border-radius: 14px;
    padding: 12px 16px;

    .ai-generate-capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      height: 36px;
      padding: 0 18px;
      border-radius: 9999px;
      background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
      border: none;
      color: #FFFFFF;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 3px 10px rgba(124, 58, 237, 0.25);
      transition: all 0.22s;

      .ai-btn-icon {
        font-size: 15px;
      }

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(124, 58, 237, 0.35);
      }

      &:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }
    }

    .template-pill-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .template-label {
        font-size: 12px;
        color: #64748B;
      }

      .template-pill-btn {
        height: 28px;
        padding: 0 12px;
        border-radius: 9999px;
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        color: #475569;
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          color: #1677FF;
          border-color: #93C5FD;
          background: #F0F7FF;
        }
      }
    }
  }

  .chapters-dynamic-list {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .chapter-pill-item-row {
      display: flex;
      align-items: center;
      gap: 10px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 9999px;
      padding: 4px 6px 4px 14px;
      transition: all 0.2s;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.05);
      }

      .chapter-seq-badge {
        font-size: 12px;
        font-weight: 700;
        color: #1677FF;
        background: #EFF6FF;
        padding: 2px 10px;
        border-radius: 9999px;
        white-space: nowrap;
      }

      .chapter-title-input {
        flex: 1;

        :deep(.el-input__wrapper) {
          box-shadow: none !important;
          background: transparent !important;
          padding: 0;
          height: 32px;
        }

        :deep(.el-input__inner) {
          font-size: 13.5px;
          color: #1E293B;
          font-weight: 500;
        }
      }

      .chapter-del-btn {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: none;
        background: #F1F5F9;
        color: #94A3B8;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-size: 12px;
        transition: all 0.2s;

        &:hover {
          background: #FEE2E2;
          color: #EF4444;
        }
      }
    }

    .add-chapter-dashed-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      height: 38px;
      border-radius: 9999px;
      border: 1.5px dashed #CBD5E1;
      background: transparent;
      color: #64748B;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #1677FF;
        color: #1677FF;
        background: #F0F7FF;
      }
    }
  }
}
</style>

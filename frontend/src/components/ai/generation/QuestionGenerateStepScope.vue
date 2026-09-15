<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 2 步：划定考察章节与核心考点范围</h3>
    <p class="pane-desc">多选需要考察的章节范围，AI 将对重点知识点进行题目覆盖：</p>

    <div class="chapters-selection-list">
      <label
        v-for="ch in currentCourseChapters"
        :key="ch.id"
        class="chapter-check-row"
        :class="{ checked: formState.chapterIds.includes(ch.id) }"
      >
        <input
          type="checkbox"
          :value="ch.id"
          :checked="formState.chapterIds.includes(ch.id)"
          class="hidden-checkbox"
          @change="$emit('toggle-chapter', ch.id)"
        />
        <div class="check-box-circle">
          <el-icon v-if="formState.chapterIds.includes(ch.id)"><Check /></el-icon>
        </div>
        <div class="chapter-label-col">
          <strong class="ch-title">{{ ch.title }}</strong>
          <span class="ch-desc">{{ ch.description }}</span>
        </div>
      </label>
    </div>

    <div class="kp-chips-section">
      <span class="kp-tips-title">
        <el-icon class="mr-1 text-amber-500"><Opportunity /></el-icon>
        重点命题知识点倾向标签（点击切换高优考察）：
      </span>
      <div class="kp-pills-row">
        <span
          v-for="kp in availableKnowledgePoints"
          :key="kp"
          class="pill-selectable-kp"
          :class="{ selected: formState.knowledgePointNames.includes(kp) }"
          @click="$emit('toggle-kp', kp)"
        >
          <span>{{ kp }}</span>
          <el-icon v-if="formState.knowledgePointNames.includes(kp)"><Check /></el-icon>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check, Opportunity } from '@element-plus/icons-vue';
import type { QuestionGenerateFormState } from './question-generate-types';

defineProps<{
  formState: QuestionGenerateFormState;
  currentCourseChapters: any[];
  availableKnowledgePoints: string[];
}>();

defineEmits<{
  'toggle-chapter': [id: number];
  'toggle-kp': [kp: string];
}>();
</script>

<style scoped lang="scss">
.step-content-pane {
  .pane-title {
    margin: 0 0 6px 0;
    font-size: 18px;
    font-weight: 700;
    color: #0F172A;
  }

  .pane-desc {
    margin: 0 0 24px 0;
    font-size: 13.5px;
    color: #64748B;
  }
}

.chapters-selection-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;

  .chapter-check-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 18px;
    border-radius: 14px;
    border: 1px solid #E2E8F0;
    cursor: pointer;
    transition: all 0.2s;

    .hidden-checkbox {
      display: none;
    }

    .check-box-circle {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 2px solid #CBD5E1;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #FFFFFF;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .chapter-label-col {
      .ch-title {
        font-size: 14px;
        color: #0F172A;
        display: block;
      }
      .ch-desc {
        font-size: 12px;
        color: #64748B;
        margin-top: 2px;
        display: block;
      }
    }

    &.checked {
      border-color: #1677FF;
      background: #F0F7FF;

      .check-box-circle {
        background: #1677FF;
        border-color: #1677FF;
      }
    }
  }
}

.kp-chips-section {
  padding-top: 14px;
  border-top: 1px solid #F1F5F9;

  .kp-tips-title {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: #334155;
    margin-bottom: 10px;
  }

  .kp-pills-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .pill-selectable-kp {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 9999px; // 长圆药丸
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      color: #475569;
      font-size: 12.5px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #93C5FD;
        color: #1677FF;
      }

      &.selected {
        border-color: #1677FF;
        background: #EAF3FF;
        color: #1677FF;
        font-weight: 600;
      }
    }
  }
}
</style>

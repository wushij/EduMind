<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 3 步：设定目标题型与难度系数</h3>
    <p class="pane-desc">可选择混合多种题型生成，或单一题型批量命题：</p>

    <div class="setting-group-box">
      <label class="setting-label">目标生成题型 (支持多选)</label>
      <div class="type-pills-selector">
        <div
          v-for="t in typeOptions"
          :key="t.type"
          class="type-pill-card"
          :class="{ active: formState.questionTypes.includes(t.type) }"
          @click="$emit('toggle-type', t.type)"
        >
          <el-icon class="type-icon" :style="{ color: t.color }">
            <component :is="t.icon" />
          </el-icon>
          <span class="type-label">{{ t.label }}</span>
          <el-icon v-if="formState.questionTypes.includes(t.type)" class="type-check">
            <Check />
          </el-icon>
        </div>
      </div>
    </div>

    <div class="setting-group-box">
      <label class="setting-label">整体考卷预期难度</label>
      <div class="diff-radios-row">
        <div
          v-for="d in difficultyOptions"
          :key="d.val"
          class="diff-pill-radio"
          :class="[d.colorClass, { active: formState.difficulty === d.val }]"
          @click="formState.difficulty = d.val"
        >
          <span class="diff-dot"></span>
          <span>{{ d.label }}</span>
          <span class="diff-desc">({{ d.desc }})</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue';
import type { QuestionType } from '@/mock/questions';
import type { DifficultyOption, QuestionGenerateFormState, QuestionTypeOption } from './question-generate-types';

defineProps<{
  formState: QuestionGenerateFormState;
  typeOptions: QuestionTypeOption[];
  difficultyOptions: DifficultyOption[];
}>();

defineEmits<{
  'toggle-type': [type: QuestionType];
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

.setting-group-box {
  margin-bottom: 24px;

  .setting-label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: #1E293B;
    margin-bottom: 12px;
  }

  .type-pills-selector {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;

    .type-pill-card {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 18px;
      border-radius: 9999px; // 长圆跑道
      border: 1.5px solid #E2E8F0;
      cursor: pointer;
      transition: all 0.2s;

      .type-icon {
        font-size: 16px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }

      .type-label {
        font-size: 13.5px;
        font-weight: 500;
        color: #334155;
        flex: 1;
      }

      .type-check {
        color: #1677FF;
        font-weight: 700;
      }

      &.active {
        border-color: #1677FF;
        background: #F0F7FF;
        .type-label {
          color: #1677FF;
          font-weight: 600;
        }
      }
    }
  }

  .diff-radios-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 14px;

    .diff-pill-radio {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      border-radius: 9999px; // 长圆单选
      border: 1.5px solid #E2E8F0;
      cursor: pointer;
      transition: all 0.2s;
      font-size: 13.5px;
      color: #334155;

      .diff-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: #CBD5E1;
      }

      .diff-desc {
        font-size: 11px;
        color: #94A3B8;
      }

      &.active {
        border-color: #1677FF;
        background: #F0F7FF;
        color: #1677FF;
        font-weight: 600;

        .diff-dot {
          background: #1677FF;
        }
      }
    }
  }
}
</style>

<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 4 步：题量规模、单题分值与卷面预算</h3>
    <p class="pane-desc">精准设定出题规模，系统将实时核算卷面参考总分与学生作答负荷：</p>

    <!-- 预设规模一键配置 -->
    <div class="preset-scale-row">
      <span class="scale-label">快速测验预设：</span>
      <div class="scale-buttons">
        <button
          v-for="p in scalePresets"
          :key="p.name"
          type="button"
          class="scale-preset-btn"
          :class="{ active: formState.count === p.count && formState.scorePerQuestion === p.score }"
          @click="applyPreset(p.count, p.score)"
        >
          <span class="p-name">{{ p.name }}</span>
          <span class="p-meta">{{ p.count }} 题 / {{ p.count * p.score }} 分</span>
        </button>
      </div>
    </div>

    <!-- 步进器网格 -->
    <div class="stepper-config-grid">
      <div class="config-pill-card">
        <span class="config-title">生成题目总数量</span>
        <div class="stepper-controls">
          <button
            type="button"
            class="stepper-btn"
            :disabled="formState.count <= 1"
            @click="formState.count--"
          >
            -
          </button>
          <span class="stepper-val">{{ formState.count }} <small>道试题</small></span>
          <button
            type="button"
            class="stepper-btn"
            :disabled="formState.count >= 20"
            @click="formState.count++"
          >
            +
          </button>
        </div>
        <span class="config-tip">单次向导建议生成 3 ~ 10 道题目以保证大模型高思维链质量</span>
      </div>

      <div class="config-pill-card">
        <span class="config-title">每题基准赋分</span>
        <div class="stepper-controls">
          <button
            type="button"
            class="stepper-btn"
            :disabled="formState.scorePerQuestion <= 1"
            @click="formState.scorePerQuestion--"
          >
            -
          </button>
          <span class="stepper-val">{{ formState.scorePerQuestion }} <small>分 / 题</small></span>
          <button
            type="button"
            class="stepper-btn"
            :disabled="formState.scorePerQuestion >= 50"
            @click="formState.scorePerQuestion++"
          >
            +
          </button>
        </div>
        <span class="config-tip">保存入库后依然可在试卷组装与发布作业时逐题微调分值</span>
      </div>
    </div>

    <!-- 卷面总览核算面板 -->
    <div class="score-audit-bar">
      <div class="audit-item">
        <span class="a-label">参考试卷总分</span>
        <strong class="a-val text-blue-600">{{ formState.count * formState.scorePerQuestion }} 分</strong>
      </div>
      <div class="audit-divider"></div>
      <div class="audit-item">
        <span class="a-label">预估作答耗时</span>
        <strong class="a-val text-slate-800">约 {{ estimatedMinutes }} 分钟</strong>
      </div>
      <div class="audit-divider"></div>
      <div class="audit-item">
        <span class="a-label">平均认知负荷</span>
        <strong class="a-val text-emerald-600">适中（梯度递进）</strong>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { QuestionGenerateFormState } from './question-generate-types';

const props = defineProps<{
  formState: QuestionGenerateFormState;
}>();

const scalePresets = [
  { name: '随堂 5 题速测', count: 5, score: 5 },
  { name: '单元 10 题巩固', count: 10, score: 5 },
  { name: '阶段 15 题精炼', count: 15, score: 5 },
  { name: '期末 20 题模拟', count: 20, score: 5 }
];

function applyPreset(count: number, score: number) {
  props.formState.count = count;
  props.formState.scorePerQuestion = score;
}

const estimatedMinutes = computed(() => {
  return Math.round(props.formState.count * 2.2);
});
</script>

<style scoped lang="scss">
.step-content-pane {
  .pane-title {
    margin: 0 0 6px 0;
    font-size: 18px;
    font-weight: 800;
    color: #0F172A;
  }

  .pane-desc {
    margin: 0 0 20px 0;
    font-size: 13.5px;
    color: #64748B;
  }
}

.preset-scale-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;

  .scale-label {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
  }

  .scale-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .scale-preset-btn {
      padding: 6px 14px;
      border-radius: 9999px;
      background: #FFFFFF;
      border: 1.5px solid #E2E8F0;
      color: #334155;
      font-size: 12.5px;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .p-name {
        font-weight: 600;
      }

      .p-meta {
        font-size: 11px;
        color: #94A3B8;
      }

      &:hover {
        border-color: #1677FF;
        color: #1677FF;
      }

      &.active {
        border-color: #1677FF;
        background: #EFF6FF;
        color: #1677FF;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);

        .p-meta {
          color: #60A5FA;
        }
      }
    }
  }
}

.stepper-config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;

  .config-pill-card {
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    box-shadow: 0 4px 12px rgba(30, 80, 160, 0.03);

    .config-title {
      font-size: 14.5px;
      font-weight: 700;
      color: #0F172A;
      margin-bottom: 16px;
    }

    .stepper-controls {
      display: flex;
      align-items: center;
      gap: 18px;
      margin-bottom: 14px;

      .stepper-btn {
        width: 42px;
        height: 42px;
        border-radius: 50%;
        border: 1px solid #CBD5E1;
        background: #F8FAFC;
        font-size: 20px;
        font-weight: 600;
        color: #334155;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;

        &:hover:not(:disabled) {
          border-color: #1677FF;
          color: #1677FF;
          background: #EFF6FF;
          transform: scale(1.05);
        }

        &:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }
      }

      .stepper-val {
        font-size: 26px;
        font-weight: 800;
        color: #1677FF;
        min-width: 100px;
        text-align: center;

        small {
          font-size: 13px;
          color: #64748B;
          font-weight: 500;
        }
      }
    }

    .config-tip {
      font-size: 12px;
      color: #94A3B8;
      text-align: center;
    }
  }
}

.score-audit-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 16px 20px;
  border-radius: 16px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;

  .audit-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .a-label {
      font-size: 12px;
      color: #64748B;
    }

    .a-val {
      font-size: 18px;
      font-weight: 700;
    }
  }

  .audit-divider {
    width: 1px;
    height: 32px;
    background: #E2E8F0;
  }
}
</style>

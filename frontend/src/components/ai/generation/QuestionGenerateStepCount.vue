<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 4 步：题量规模与单题分值</h3>
    <p class="pane-desc">配置本次生成题目的总数以及默认赋分标准：</p>

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
          <span class="stepper-val">{{ formState.count }} <small>道题目</small></span>
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
        <span class="config-title">每题参考建议分值</span>
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
        <span class="config-tip">保存入库后依然可在试卷组装时微调单题分值</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { QuestionGenerateFormState } from './question-generate-types';

defineProps<{
  formState: QuestionGenerateFormState;
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

.stepper-config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  .config-pill-card {
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    background: #F8FAFC;
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;

    .config-title {
      font-size: 14.5px;
      font-weight: 600;
      color: #0F172A;
      margin-bottom: 16px;
    }

    .stepper-controls {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 14px;

      .stepper-btn {
        width: 38px;
        height: 38px;
        border-radius: 50%;
        border: 1px solid #CBD5E1;
        background: #FFFFFF;
        font-size: 18px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;

        &:hover:not(:disabled) {
          border-color: #1677FF;
          color: #1677FF;
        }

        &:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }
      }

      .stepper-val {
        font-size: 24px;
        font-weight: 700;
        color: #1677FF;
        min-width: 90px;
        text-align: center;

        small {
          font-size: 13px;
          color: #64748B;
          font-weight: 400;
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
</style>

<template>
  <div class="config-section-card">
    <div class="section-header-flex">
      <div>
        <h3 class="section-title">3. 题型配比与分值精细规划</h3>
        <p class="section-desc">调节各题型题量与每题单价，确保右侧实时计算的总分严格对齐目标总分：</p>
      </div>

      <!-- 实时总分校验胶囊指示器 -->
      <div
        class="total-score-pill-indicator"
        :class="{ 'is-matched': isScoreMatched, 'is-mismatch': !isScoreMatched }"
      >
        <el-icon class="indicator-icon">
          <Check v-if="isScoreMatched" />
          <Warning v-else />
        </el-icon>
        <span class="indicator-text">
          当前规划总分：<strong>{{ calculatedTotalScore }}</strong> / {{ examForm.totalScore }} 分
        </span>
      </div>
    </div>

    <!-- 题型规则卡片网格 -->
    <div class="rules-table-list">
      <div
        v-for="(rule, idx) in examForm.rules"
        :key="rule.type"
        class="rule-row-card"
      >
        <div class="rule-type-col">
          <span class="rule-index">0{{ idx + 1 }}</span>
          <strong class="rule-name">{{ rule.label }}</strong>
        </div>

        <div class="rule-stepper-col">
          <span class="col-label">题量：</span>
          <div class="mini-stepper">
            <button
              type="button"
              class="step-btn"
              :disabled="rule.count <= 0"
              @click="rule.count--"
            >
              -
            </button>
            <span class="step-num">{{ rule.count }}</span>
            <button
              type="button"
              class="step-btn"
              :disabled="rule.count >= 50"
              @click="rule.count++"
            >
              +
            </button>
          </div>
          <span class="unit-text">题</span>
        </div>

        <div class="rule-stepper-col">
          <span class="col-label">每题分值：</span>
          <div class="mini-stepper">
            <button
              type="button"
              class="step-btn"
              :disabled="rule.scoreEach <= 1"
              @click="rule.scoreEach--"
            >
              -
            </button>
            <span class="step-num">{{ rule.scoreEach }}</span>
            <button
              type="button"
              class="step-btn"
              :disabled="rule.scoreEach >= 50"
              @click="rule.scoreEach++"
            >
              +
            </button>
          </div>
          <span class="unit-text">分</span>
        </div>

        <div class="rule-subtotal-col">
          <span class="subtotal-label">该大题小计：</span>
          <strong class="subtotal-val">{{ rule.count * rule.scoreEach }} 分</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check, Warning } from '@element-plus/icons-vue';
import type { ExamFormState } from './exam-generate-types';

defineProps<{
  examForm: ExamFormState;
  calculatedTotalScore: number;
  isScoreMatched: boolean;
}>();
</script>

<style scoped lang="scss">
.config-section-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 28px 32px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
  margin-bottom: 22px;

  .section-title {
    margin: 0 0 16px 0;
    font-size: 16.5px;
    font-weight: 700;
    color: #0F172A;
  }

  .section-header-flex {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;

    .section-desc {
      margin: 0;
      font-size: 13px;
      color: #64748B;
    }

    .total-score-pill-indicator {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px; // 长圆指示条
      font-size: 13px;
      font-weight: 500;
      white-space: nowrap;

      &.is-matched {
        background: #ECFDF5;
        border: 1px solid #A7F3D0;
        color: #059669;

        strong {
          color: #059669;
          font-size: 15px;
        }
      }

      &.is-mismatch {
        background: #FFFBEB;
        border: 1px solid #FDE68A;
        color: #D97706;

        strong {
          color: #DC2626;
          font-size: 15px;
        }
      }
    }
  }

  .rules-table-list {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .rule-row-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 20px;
      background: #F8FAFC;
      border-radius: 16px;
      border: 1px solid #EDF2F7;
      transition: all 0.2s;

      &:hover {
        background: #FFFFFF;
        border-color: #DBEAFE;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
      }

      .rule-type-col {
        display: flex;
        align-items: center;
        gap: 12px;
        width: 180px;

        .rule-index {
          font-size: 12px;
          font-weight: 700;
          color: #94A3B8;
        }

        .rule-name {
          font-size: 14px;
          color: #0F172A;
        }
      }

      .rule-stepper-col {
        display: flex;
        align-items: center;
        gap: 8px;

        .col-label {
          font-size: 12.5px;
          color: #64748B;
        }

        .mini-stepper {
          display: inline-flex;
          align-items: center;
          background: #FFFFFF;
          border: 1px solid #CBD5E1;
          border-radius: 9999px; // 长圆步进器
          padding: 2px;

          .step-btn {
            width: 26px;
            height: 26px;
            border-radius: 50%;
            border: none;
            background: transparent;
            color: #475569;
            font-size: 14px;
            cursor: pointer;

            &:hover:not(:disabled) {
              background: #EAF3FF;
              color: #1677FF;
            }

            &:disabled {
              opacity: 0.3;
              cursor: not-allowed;
            }
          }

          .step-num {
            min-width: 32px;
            text-align: center;
            font-size: 13.5px;
            font-weight: 700;
            color: #1E293B;
          }
        }

        .unit-text {
          font-size: 12px;
          color: #94A3B8;
        }
      }

      .rule-subtotal-col {
        width: 140px;
        text-align: right;

        .subtotal-label {
          font-size: 11.5px;
          color: #94A3B8;
        }

        .subtotal-val {
          display: block;
          font-size: 15px;
          font-weight: 700;
          color: #1677FF;
        }
      }
    }
  }
}
</style>

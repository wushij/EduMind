<template>
  <div class="config-section-card">
    <div class="section-header-flex">
      <div class="header-text-box">
        <h3 class="section-title">3. 题型规划与真实题库配比</h3>
        <p class="section-desc">
          已联动当前课程真实题库资产。根据各题型库存科学规划抽取量与单题分值，不足部分将由 AI 智能补全：
        </p>
      </div>

      <div class="header-action-group">
        <!-- 一键智能均衡配比胶囊按钮 -->
        <button
          type="button"
          class="capsule-tool-btn is-magic"
          @click="$emit('auto-balance')"
        >
          <el-icon><MagicStick /></el-icon>
          <span>一键均衡 100 分</span>
        </button>

        <!-- 实时总分校验长圆指示器 -->
        <div
          class="total-score-pill-indicator"
          :class="{ 'is-matched': isScoreMatched, 'is-mismatch': !isScoreMatched }"
        >
          <el-icon class="indicator-icon">
            <Check v-if="isScoreMatched" />
            <Warning v-else />
          </el-icon>
          <span class="indicator-text">
            当前规划：<strong>{{ calculatedTotalScore }}</strong> / {{ examForm.totalScore }} 分
          </span>
        </div>
      </div>
    </div>

    <!-- 真实题库库存全景速览胶囊条 -->
    <div class="bank-stats-bar">
      <span class="stats-bar-title">本课真实题库存量：</span>
      <div class="stats-pills-row">
        <span class="bank-pill">
          单选题 <strong>{{ bankTypeStats?.SINGLE_CHOICE || 0 }}</strong> 题
        </span>
        <span class="bank-pill">
          多选题 <strong>{{ bankTypeStats?.MULTIPLE_CHOICE || 0 }}</strong> 题
        </span>
        <span class="bank-pill">
          判断题 <strong>{{ bankTypeStats?.JUDGE || 0 }}</strong> 题
        </span>
        <span class="bank-pill">
          填空题 <strong>{{ bankTypeStats?.COMPLETION || 0 }}</strong> 题
        </span>
        <span class="bank-pill">
          综合题 <strong>{{ bankTypeStats?.SHORT_ANSWER || 0 }}</strong> 题
        </span>
      </div>
    </div>

    <!-- 题型规则卡片列表 -->
    <div class="rules-table-list">
      <div
        v-for="(rule, idx) in examForm.rules"
        :key="rule.type"
        class="rule-row-card"
      >
        <div class="rule-type-col">
          <span class="rule-index-pill">0{{ idx + 1 }}</span>
          <div class="type-name-box">
            <strong class="rule-name">{{ rule.label }}</strong>
            <span class="stock-info">
              库中存量: <strong>{{ bankTypeStats?.[rule.type] || 0 }}</strong> 题
            </span>
          </div>
        </div>

        <!-- 题量步进器（长圆边框） -->
        <div class="rule-stepper-col">
          <span class="col-label">抽取题量：</span>
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

          <!-- 智能补齐标签提示 -->
          <span
            v-if="rule.count > (bankTypeStats?.[rule.type] || 0)"
            class="ai-fill-badge"
          >
            AI 补齐 {{ rule.count - (bankTypeStats?.[rule.type] || 0) }} 题
          </span>
        </div>

        <!-- 分值步进器（长圆边框） -->
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

        <!-- 小计与操作 -->
        <div class="rule-subtotal-col">
          <div class="subtotal-box">
            <span class="subtotal-label">题型小计</span>
            <strong class="subtotal-val">{{ rule.count * rule.scoreEach }} 分</strong>
          </div>
          <button
            type="button"
            class="delete-rule-btn"
            title="移除本题型"
            @click="$emit('remove-rule', rule.type)"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部添加题型长圆栏 -->
    <div class="add-rule-footer-bar">
      <span class="add-label">快捷增添考核大题：</span>
      <div class="add-capsules">
        <button
          v-for="cand in candidateTypes"
          :key="cand.type"
          type="button"
          class="add-type-chip"
          :disabled="isTypeAdded(cand.type)"
          @click="$emit('add-rule', cand.type, cand.label)"
        >
          + {{ cand.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check, Warning, MagicStick, Delete } from '@element-plus/icons-vue';
import type { ExamFormState } from './exam-generate-types';

const props = defineProps<{
  examForm: ExamFormState;
  calculatedTotalScore: number;
  isScoreMatched: boolean;
  bankTypeStats?: Record<string, number>;
}>();

defineEmits<{
  (e: 'auto-balance'): void;
  (e: 'add-rule', type: string, label: string): void;
  (e: 'remove-rule', type: string): void;
}>();

const candidateTypes = [
  { type: 'SINGLE_CHOICE', label: '单项选择题' },
  { type: 'MULTIPLE_CHOICE', label: '多项选择题' },
  { type: 'JUDGE', label: '判断题' },
  { type: 'COMPLETION', label: '填空题' },
  { type: 'SHORT_ANSWER', label: '综合解答题' }
];

function isTypeAdded(type: string) {
  return props.examForm.rules.some((r) => r.type === type);
}
</script>

<style scoped lang="scss">
.config-section-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 28px 32px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  margin-bottom: 22px;

  .section-title {
    margin: 0 0 8px 0;
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
  }
}

.section-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;

  .header-text-box {
    max-width: 620px;

    .section-desc {
      margin: 0;
      font-size: 13px;
      color: #64748b;
      line-height: 1.5;
    }
  }

  .header-action-group {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }

  .capsule-tool-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 36px;
    padding: 0 16px;
    border-radius: 9999px; // 长圆边框
    border: 1px solid #bfdbfe;
    background: #eff6ff;
    color: #2563eb;
    font-size: 12.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #dbeafe;
      border-color: #93c5fd;
      transform: translateY(-1px);
    }
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
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      color: #059669;

      strong {
        color: #059669;
        font-size: 15px;
      }
    }

    &.is-mismatch {
      background: #fffbeb;
      border: 1px solid #fde68a;
      color: #d97706;

      strong {
        color: #dc2626;
        font-size: 15px;
      }
    }
  }
}

/* 真实题库库存速览栏 */
.bank-stats-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8fafc;
  border-radius: 9999px; // 长圆边框
  padding: 8px 18px;
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
  flex-wrap: wrap;

  .stats-bar-title {
    font-size: 12.5px;
    font-weight: 600;
    color: #475569;
  }

  .stats-pills-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .bank-pill {
      font-size: 12px;
      color: #64748b;
      background: #ffffff;
      padding: 3px 12px;
      border-radius: 9999px; // 长圆胶囊
      border: 1px solid #e2e8f0;

      strong {
        color: #2563eb;
        font-weight: 700;
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
    background: #ffffff;
    border-radius: 18px;
    border: 1px solid #e2e8f0;
    transition: all 0.2s;

    &:hover {
      border-color: #93c5fd;
      box-shadow: 0 4px 16px rgba(37, 99, 235, 0.06);
    }

    .rule-type-col {
      display: flex;
      align-items: center;
      gap: 14px;
      width: 220px;

      .rule-index-pill {
        width: 26px;
        height: 26px;
        border-radius: 50%;
        background: #f1f5f9;
        color: #64748b;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: 700;
      }

      .type-name-box {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .rule-name {
          font-size: 14.5px;
          color: #0f172a;
        }

        .stock-info {
          font-size: 11.5px;
          color: #94a3b8;

          strong {
            color: #475569;
          }
        }
      }
    }

    .rule-stepper-col {
      display: flex;
      align-items: center;
      gap: 8px;

      .col-label {
        font-size: 12.5px;
        color: #64748b;
      }

      .mini-stepper {
        display: inline-flex;
        align-items: center;
        background: #f8fafc;
        border: 1px solid #cbd5e1;
        border-radius: 9999px; // 长圆步进器
        padding: 2px;

        .step-btn {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          border: none;
          background: #ffffff;
          color: #475569;
          font-size: 14px;
          font-weight: 700;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

          &:hover:not(:disabled) {
            background: #eff6ff;
            color: #2563eb;
          }

          &:disabled {
            opacity: 0.35;
            cursor: not-allowed;
          }
        }

        .step-num {
          min-width: 36px;
          text-align: center;
          font-size: 14px;
          font-weight: 700;
          color: #0f172a;
        }
      }

      .unit-text {
        font-size: 12.5px;
        color: #94a3b8;
      }

      .ai-fill-badge {
        font-size: 11px;
        font-weight: 600;
        background: #fdf4ff;
        color: #c026d3;
        border: 1px solid #f0abfc;
        padding: 2px 8px;
        border-radius: 9999px; // 长圆小徽章
      }
    }

    .rule-subtotal-col {
      display: flex;
      align-items: center;
      gap: 16px;

      .subtotal-box {
        text-align: right;

        .subtotal-label {
          font-size: 11px;
          color: #94a3b8;
        }

        .subtotal-val {
          display: block;
          font-size: 16px;
          font-weight: 700;
          color: #2563eb;
        }
      }

      .delete-rule-btn {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        border: 1px solid #fecaca;
        background: #fef2f2;
        color: #ef4444;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #fee2e2;
          transform: scale(1.05);
        }
      }
    }
  }
}

/* 底部添加题型栏 */
.add-rule-footer-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
  flex-wrap: wrap;

  .add-label {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
  }

  .add-capsules {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .add-type-chip {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      color: #334155;
      font-size: 12px;
      font-weight: 600;
      padding: 5px 14px;
      border-radius: 9999px; // 长圆边框
      cursor: pointer;
      transition: all 0.2s;

      &:hover:not(:disabled) {
        background: #eff6ff;
        color: #2563eb;
        border-color: #bfdbfe;
        transform: translateY(-1px);
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }
  }
}
</style>

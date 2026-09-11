<template>
  <div class="question-card" :class="{ 'is-editing': isEditing }">
    <!-- 顶部卡片元信息条 -->
    <div class="card-top-bar">
      <div class="left-meta">
        <span class="index-badge">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="pill-badge pill-badge--type">{{ typeLabel }}</span>
        <span class="pill-badge pill-badge--diff" :class="difficultyClass">
          {{ difficultyLabel }}
        </span>
        <span class="pill-badge pill-badge--score">{{ question.score }} 分</span>
      </div>

      <div class="right-actions">
        <button
          type="button"
          class="capsule-icon-btn"
          title="切换解析展开"
          @click="showAnalysis = !showAnalysis"
        >
          <span>{{ showAnalysis ? '收起解析' : '查看解析' }}</span>
        </button>

        <button
          v-if="allowEdit"
          type="button"
          class="capsule-icon-btn capsule-icon-btn--primary"
          @click="toggleEdit"
        >
          <span>{{ isEditing ? '完成' : '编辑' }}</span>
        </button>

        <button
          v-if="allowDelete"
          type="button"
          class="capsule-icon-btn capsule-icon-btn--danger"
          title="剔除本题"
          @click="$emit('delete', question.id)"
        >
          <span>剔除</span>
        </button>
      </div>
    </div>

    <!-- 题干编辑或展示 -->
    <div class="stem-section">
      <div v-if="!isEditing" class="stem-content">
        {{ question.stem }}
      </div>
      <el-input
        v-else
        v-model="question.stem"
        type="textarea"
        :rows="3"
        class="edit-textarea"
      />
    </div>

    <!-- 选项列表 (选择题) -->
    <div v-if="question.options && question.options.length > 0" class="options-container">
      <div
        v-for="opt in question.options"
        :key="opt.key"
        class="option-pill-row"
        :class="{ 'is-correct': opt.isCorrect }"
      >
        <span class="opt-key-circle">{{ opt.key }}</span>
        <span class="opt-content">{{ opt.content }}</span>
        <span v-if="opt.isCorrect" class="correct-tag">正确答案</span>
      </div>
    </div>

    <!-- 简答题/填空题答案 -->
    <div v-else-if="question.correctAnswer" class="answer-display-box">
      <span class="ans-label">参考答案：</span>
      <span class="ans-text">{{ question.correctAnswer }}</span>
    </div>

    <!-- 展开的解析与知识点卡片 -->
    <div v-show="showAnalysis" class="analysis-expanded-card">
      <div class="analysis-row">
        <strong class="analysis-title">💡 权威解析与考点点拨：</strong>
        <p class="analysis-body">{{ question.analysis }}</p>
      </div>

      <div class="kp-row">
        <span class="kp-label">关联知识点：</span>
        <div class="kp-tags">
          <span
            v-for="kp in question.knowledgePointNames"
            :key="kp"
            class="pill-kp-badge"
          >
            🧠 {{ kp }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Question } from '@/types/question/question';

const props = withDefaults(
  defineProps<{
    question: Question;
    index?: number;
    allowEdit?: boolean;
    allowDelete?: boolean;
  }>(),
  {
    index: 0,
    allowEdit: true,
    allowDelete: true
  }
);

const emit = defineEmits<{
  (e: 'delete', id: number): void;
  (e: 'update', q: Question): void;
}>();

const showAnalysis = ref(true);
const isEditing = ref(false);

const typeLabel = computed(() => {
  if (props.question.typeLabel) return props.question.typeLabel;
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[props.question.type] || '题目';
});

const difficultyLabel = computed(() => {
  if (props.question.difficultyLabel) return props.question.difficultyLabel;
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  };
  return map[props.question.difficulty] || '中等';
});

const difficultyClass = computed(() => {
  if (props.question.difficulty === 'EASY') return 'diff-easy';
  if (props.question.difficulty === 'HARD') return 'diff-hard';
  return 'diff-medium';
});

function toggleEdit() {
  if (isEditing.value) {
    emit('update', { ...props.question });
  }
  isEditing.value = !isEditing.value;
}
</script>

<style scoped lang="scss">
.question-card {
  background: #FFFFFF;
  border-radius: 18px;
  padding: 22px 24px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  transition: all 0.22s ease;
  margin-bottom: 18px;

  &:hover {
    box-shadow: 0 8px 24px rgba(30, 80, 150, 0.08);
    border-color: #DBEAFE;
  }

  .card-top-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    .left-meta {
      display: flex;
      align-items: center;
      gap: 8px;

      .index-badge {
        width: 26px;
        height: 26px;
        border-radius: 8px;
        background: #0F172A;
        color: #FFFFFF;
        font-size: 12px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .pill-badge {
        padding: 2px 10px;
        border-radius: 9999px; // 长圆跑道胶囊
        font-size: 11.5px;
        font-weight: 600;

        &--type {
          background: #EFF6FF;
          color: #1677FF;
        }

        &--score {
          background: #FEF3C7;
          color: #D97706;
        }

        &--diff {
          &.diff-easy { background: #ECFDF5; color: #059669; }
          &.diff-medium { background: #EEF2FF; color: #4F46E5; }
          &.diff-hard { background: #FEE2E2; color: #DC2626; }
        }
      }
    }

    .right-actions {
      display: flex;
      align-items: center;
      gap: 8px;

      .capsule-icon-btn {
        height: 30px;
        padding: 0 14px;
        border-radius: 9999px; // 长圆小按钮
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: #CBD5E1;
          color: #1677FF;
        }

        &--primary {
          background: #EFF6FF;
          border-color: #BFDBFE;
          color: #1677FF;
        }

        &--danger {
          &:hover {
            background: #FEF2F2;
            border-color: #FECACA;
            color: #EF4444;
          }
        }
      }
    }
  }

  .stem-section {
    margin-bottom: 16px;

    .stem-content {
      font-size: 15px;
      font-weight: 600;
      color: #0F172A;
      line-height: 1.6;
    }
  }

  .options-container {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 16px;

    .option-pill-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 16px;
      border-radius: 9999px; // 纯正长圆选项条
      background: #F8FAFC;
      border: 1px solid #EDF2F7;
      font-size: 13.5px;
      color: #334155;
      transition: all 0.2s;

      .opt-key-circle {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #E2E8F0;
        color: #475569;
        font-weight: 700;
        font-size: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .opt-content {
        flex: 1;
      }

      &.is-correct {
        background: #F0FDF4;
        border-color: #86EFAC;
        color: #166534;

        .opt-key-circle {
          background: #22C55E;
          color: #FFFFFF;
        }

        .correct-tag {
          font-size: 11px;
          font-weight: 600;
          color: #15803D;
          background: #DCFCE7;
          padding: 2px 8px;
          border-radius: 9999px;
        }
      }
    }
  }

  .answer-display-box {
    padding: 12px 16px;
    border-radius: 12px;
    background: #F8FAFC;
    border: 1px solid #EDF2F7;
    margin-bottom: 14px;
    font-size: 13.5px;

    .ans-label {
      font-weight: 600;
      color: #0F172A;
    }

    .ans-text {
      color: #1677FF;
      font-weight: 600;
    }
  }

  .analysis-expanded-card {
    background: #F8FAFC;
    border-radius: 14px;
    padding: 16px 18px;
    border: 1px solid #EDF2F7;
    margin-top: 14px;

    .analysis-row {
      margin-bottom: 12px;

      .analysis-title {
        font-size: 13px;
        color: #0F172A;
        display: block;
        margin-bottom: 4px;
      }

      .analysis-body {
        margin: 0;
        font-size: 13px;
        color: #475569;
        line-height: 1.6;
      }
    }

    .kp-row {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .kp-label {
        font-size: 12px;
        color: #64748B;
      }

      .kp-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .pill-kp-badge {
          padding: 2px 10px;
          border-radius: 9999px; // 长圆药丸
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          color: #1D4ED8;
          font-size: 11.5px;
          font-weight: 500;
        }
      }
    }
  }
}
</style>

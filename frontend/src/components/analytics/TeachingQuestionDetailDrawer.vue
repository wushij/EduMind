<template>
  <el-drawer
    v-model="visible"
    :title="`考点原题详情 · ${question?.name || '题目详情'}`"
    size="520px"
    destroy-on-close
    class="question-detail-drawer"
  >
    <div v-if="question" class="drawer-inner-body">
      <!-- 考点与题目基本信息徽章行 -->
      <div class="meta-tag-line">
        <span class="type-badge">{{ question.errorTypeName || '待归因' }}</span>
        <span class="course-badge">{{ question.course }}</span>
        <span class="mastery-badge" :class="`badge-${question.status}`">
          <template v-if="question.rate === null">掌握度: 暂无数据 ({{ question.statusLabel }})</template>
          <template v-else>掌握度: {{ question.rate }}% ({{ question.statusLabel }})</template>
        </span>
      </div>

      <!-- 题目题干区域 -->
      <div class="content-block">
        <h4 class="block-title">
          题目题干
          <span v-if="questionTypeLabel" class="question-type-tag">{{ questionTypeLabel }}</span>
        </h4>
        <div class="stem-box math-rendered-box">
          <div v-if="question.questionStem" v-html="renderMath(question.questionStem)" />
          <div v-else class="empty-stem-hint">
            <span>原题干：关于「{{ question.name }}」的变式练习与概念辨析题（题目 #{{ question.questionId }}）</span>
          </div>
        </div>

        <!-- 原题选项：选择题才渲染，主观题（填空/简答）没有选项属正常 -->
        <ul v-if="optionList.length" class="option-list">
          <li
            v-for="opt in optionList"
            :key="opt.key"
            class="option-item"
            :class="{ 'is-correct': opt.isCorrect }"
          >
            <span class="option-key">{{ opt.key }}</span>
            <span class="option-content math-rendered-box" v-html="renderMath(opt.content)" />
            <span v-if="opt.isCorrect" class="correct-tag">正确项</span>
          </li>
        </ul>
        <div v-else-if="isObjectiveWithoutOptions" class="option-empty-hint">
          该原题未录入选项数据，建议在题库中补全后再用于讲评。
        </div>
      </div>

      <!-- AI 深度错因分析 -->
      <div class="content-block">
        <h4 class="block-title">AI 学情深度诊断归因</h4>
        <div class="diagnosis-box" :class="`diag-${question.status}`">
          <div class="diag-reason math-rendered-box" v-html="renderMath(question.errorReason || '学生对该考点的概念内涵与外延理解模糊。')" />
        </div>
      </div>

      <!-- 教学建议 -->
      <div class="content-block">
        <h4 class="block-title">针对性教研与干预建议</h4>
        <div class="advice-box">
          <el-icon class="bulb-icon"><AiSparkleIcon /></el-icon>
          <div class="advice-text">
            {{ question.suggestion || '该考点尚未完成 AI 认知归因，暂无可执行建议；可在「错题分析」中对原题发起 AI 深度诊断后查看。' }}
          </div>
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div class="drawer-action-footer">
        <el-button
          type="primary"
          class="action-btn"
          :icon="AiSparkleIcon"
          @click="handleJumpQuiz"
        >
          一键生成针对性巩固测验
        </el-button>
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { KnowledgeMasteryItem } from '@/composables/analytics/useTeachingReport';
import { renderMathText } from '@/utils/format/render-math';
import { parseQuestionOptions } from '@/utils/question/normalize-question';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

/** 客观题题型 → 中文标签；主观题（填空/简答）没有选项，不需要渲染选项区 */
const OBJECTIVE_TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题'
};

const props = defineProps<{
  modelValue: boolean;
  question: KnowledgeMasteryItem | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'quick-quiz', q: KnowledgeMasteryItem): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

function renderMath(text?: string): string {
  if (!text) return '';
  return renderMathText(text);
}

const questionTypeLabel = computed(() => OBJECTIVE_TYPE_LABELS[String(props.question?.questionType || '')] || '');

/**
 * 原题选项：后端透出的是题目表里的 options JSON 字符串，
 * 可能出现数组、键值对象、带 "A.xxx" 前缀的字符串等多种历史格式，统一交给公共解析器，
 * 避免这里再写一套「只认数组」的解析导致选项整段消失。
 */
const optionList = computed(() => {
  const raw = props.question?.questionOptions;
  if (!raw) return [];
  return parseQuestionOptions(raw, props.question?.questionAnswer ?? undefined).filter(
    (opt) => String(opt.content ?? '').trim().length > 0
  );
});

/** 客观题却没有解析出选项：属于题库数据缺失，需要提示教师，而不是静默留白 */
const isObjectiveWithoutOptions = computed(
  () => Boolean(questionTypeLabel.value) && optionList.value.length === 0
);

function handleJumpQuiz() {
  if (props.question) {
    emit('quick-quiz', props.question);
    visible.value = false;
  }
}
</script>

<style scoped lang="scss">
.drawer-inner-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;

  .meta-tag-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .type-badge {
      padding: 3px 10px;
      border-radius: 6px;
      background: #eff6ff;
      color: #1d4ed8;
      font-size: 12px;
      font-weight: 700;
    }

    .course-badge {
      padding: 3px 10px;
      border-radius: 6px;
      background: #f1f5f9;
      color: #475569;
      font-size: 12px;
    }

    .mastery-badge {
      padding: 3px 10px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 700;

      &.badge-danger { background: #fee2e2; color: #dc2626; }
      &.badge-warning { background: #fef3c7; color: #d97706; }
      &.badge-normal { background: #e0f2fe; color: #0284c7; }
      &.badge-good { background: #d1fae5; color: #059669; }
    }
  }

  .content-block {
    .block-title {
      margin: 0 0 10px 0;
      font-size: 14px;
      font-weight: 700;
      color: #0f172a;
      display: flex;
      align-items: center;
      gap: 8px;

      .question-type-tag {
        padding: 2px 8px;
        border-radius: 6px;
        background: #f1f5f9;
        color: #64748b;
        font-size: 11px;
        font-weight: 600;
      }
    }

    .option-list {
      list-style: none;
      margin: 10px 0 0 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;

      .option-item {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        padding: 10px 12px;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        background: #ffffff;
        font-size: 13.5px;
        line-height: 1.6;
        color: #1e293b;

        &.is-correct {
          border-color: #86efac;
          background: #f0fdf4;
        }

        .option-key {
          flex: none;
          width: 22px;
          height: 22px;
          border-radius: 50%;
          background: #f1f5f9;
          color: #475569;
          font-size: 12px;
          font-weight: 700;
          display: inline-flex;
          align-items: center;
          justify-content: center;
        }

        &.is-correct .option-key {
          background: #dcfce7;
          color: #15803d;
        }

        .option-content {
          flex: 1;
          min-width: 0;
          word-break: break-word;
        }

        .correct-tag {
          flex: none;
          padding: 1px 8px;
          border-radius: 9999px;
          background: #dcfce7;
          color: #15803d;
          font-size: 11px;
          font-weight: 700;
        }
      }
    }

    .option-empty-hint {
      margin-top: 10px;
      padding: 10px 12px;
      border: 1px dashed #fde68a;
      border-radius: 10px;
      background: #fffbeb;
      color: #92400e;
      font-size: 12.5px;
      line-height: 1.6;
    }

    .stem-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px;
      font-size: 14px;
      line-height: 1.7;
      color: #1e293b;

      .empty-stem-hint {
        color: #64748b;
        font-style: italic;
      }
    }

    .diagnosis-box {
      border-radius: 12px;
      padding: 16px;
      font-size: 13.5px;
      line-height: 1.65;

      &.diag-danger { background: #fff5f5; border: 1px solid #fecaca; color: #991b1b; }
      &.diag-warning { background: #fffbeb; border: 1px solid #fde68a; color: #92400e; }
      &.diag-normal { background: #f0f7ff; border: 1px solid #bae6fd; color: #075985; }
      &.diag-good { background: #f0fdf4; border: 1px solid #bbf7d0; color: #166534; }
    }

    .advice-box {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      border-radius: 12px;
      padding: 14px 16px;
      display: flex;
      align-items: flex-start;
      gap: 10px;

      .bulb-icon {
        font-size: 18px;
        color: #2563eb;
        margin-top: 2px;
      }

      .advice-text {
        font-size: 13.5px;
        line-height: 1.6;
        color: #1e40af;
      }
    }
  }

  .drawer-action-footer {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 10px;
    padding-top: 16px;
    border-top: 1px solid #e2e8f0;

    .action-btn {
      flex: 1;
      border-radius: 9999px;
      height: 40px;
      font-weight: 600;
      background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);
    }
  }
}

:deep(.katex) {
  font-size: 1.05em;
}
</style>

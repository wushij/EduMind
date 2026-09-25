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
        <span class="type-badge">{{ question.errorTypeName || '概念理解' }}</span>
        <span class="course-badge">{{ question.course }}</span>
        <span class="mastery-badge" :class="`badge-${question.status}`">
          掌握度: {{ question.rate }}% ({{ question.statusLabel }})
        </span>
      </div>

      <!-- 题目题干区域 -->
      <div class="content-block">
        <h4 class="block-title">题目题干</h4>
        <div class="stem-box math-rendered-box">
          <div v-if="question.questionStem" v-html="renderMath(question.questionStem)" />
          <div v-else class="empty-stem-hint">
            <span>原题干：关于「{{ question.name }}」的变式练习与概念辨析题（题目 #{{ question.questionId }}）</span>
          </div>
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
          <div class="advice-text">{{ question.suggestion }}</div>
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
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

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

<template>
  <el-card shadow="never" class="quiz-card">
    <div class="stem-box">
      <span class="stem-no">Q{{ currentIndex + 1 }}.</span>
      <div class="stem-text">
        <MathText :text="currentQuestion.stem" />
      </div>
    </div>

    <div v-if="currentQuestion.options && currentQuestion.options.length" class="options-group">
      <div
        v-for="opt in currentQuestion.options"
        :key="opt.key"
        class="option-item"
        :class="{
          selected: userAnswers[currentIndex] === opt.key,
          'is-right': submittedCurrent && opt.key === displayReferenceAnswer(currentIndex),
          'is-error':
            submittedCurrent &&
            userAnswers[currentIndex] === opt.key &&
            opt.key !== displayReferenceAnswer(currentIndex)
        }"
        @click="!submittedCurrent && onSelectOption(opt.key)"
      >
        <div class="option-key-circle">{{ opt.key }}</div>
        <div class="option-content">
          <MathText :text="opt.val" />
        </div>
      </div>
    </div>

    <div v-else class="text-answer-group">
      <el-input
        :model-value="userAnswers[currentIndex]"
        type="textarea"
        :rows="4"
        placeholder="请输入你的解答步骤与最终推导结果..."
        :disabled="submittedCurrent"
        @update:model-value="(v: string) => onTextAnswerChange(currentIndex, v)"
      />
    </div>

    <!-- AI 批改推演中：统一罗盘雷达 + 秒级计时 + 可中止，避免"没思考就展开答案"的观感 -->
    <AiCognitiveThinkingPanel
      v-if="grading"
      :active="grading"
      v-bind="AI_COGNITIVE_THINKING_PRESETS.practiceGrading"
      show-footer-actions
      abort-label="中止 AI 批改"
      @abort="emit('abort-grading')"
    />

    <!--
      反馈面板：这里必须用普通 v-if。
      v-else-if 写在 <transition> 的子组件上时，与上方 <AiCognitiveThinkingPanel v-if> 之间隔了 transition 一层，
      Vue 编译器无法把两者连成条件链，会直接报 “v-else/v-else-if has no adjacent v-if or v-else-if” 导致页面白屏。
      因此改为独立 v-if，并显式排除批改中（!grading），保持与推演面板互斥的原有语义。
    -->
    <transition name="el-zoom-in-top">
      <AIPracticeFeedbackPanel
        v-if="!grading && submittedCurrent && showFeedback"
        :correct="answersState[currentIndex] === 'CORRECT'"
        :kp-title="currentQuestion.kpTitle"
        :reference-answer="displayReferenceAnswer(currentIndex)"
        :analysis="displayAnalysis(currentIndex)"
      />
    </transition>

    <div class="quiz-controls-bar">
      <div class="left-action">
        <el-button v-if="currentIndex > 0" plain @click="onJumpToQuestion(currentIndex - 1)">
          上一题
        </el-button>
      </div>

      <div class="right-action">
        <el-button
          v-if="!submittedCurrent && instantFeedback"
          type="primary"
          plain
          :disabled="userAnswers[currentIndex] === undefined || userAnswers[currentIndex] === ''"
          :loading="grading"
          @click="onSubmitCurrentQuestion"
        >
          确认作答并查看 AI 解析
        </el-button>

        <el-button
          v-if="!submittedCurrent && !instantFeedback"
          type="primary"
          plain
          :disabled="userAnswers[currentIndex] === undefined"
          @click="onSubmitCurrentQuestion"
        >
          标记本题已作答
        </el-button>

        <el-button
          v-if="currentIndex < questions.length - 1"
          type="primary"
          @click="onJumpToQuestion(currentIndex + 1)"
        >
          下一题
        </el-button>

        <el-button v-else type="success" :loading="submitting" @click="onFinishPractice">
          完成本次练习并生成报告
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import MathText from '@/components/common/MathText.vue';
import AIPracticeFeedbackPanel from '@/components/learning/AIPracticeFeedbackPanel.vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import type { PracticeQuestion } from '@/composables/learning/useAIPractice';

const emit = defineEmits<{
  'abort-grading': [];
}>();

defineProps<{
  currentIndex: number;
  questions: PracticeQuestion[];
  currentQuestion: PracticeQuestion;
  userAnswers: Record<number, string>;
  answersState: Record<number, 'CORRECT' | 'WRONG' | 'PENDING'>;
  submittedCurrent: boolean;
  instantFeedback: boolean;
  grading: boolean;
  submitting: boolean;
  showFeedback: boolean;
  displayAnalysis: (index: number) => string;
  displayReferenceAnswer: (index: number) => string;
  onSelectOption: (key: string) => void;
  onTextAnswerChange: (index: number, value: string) => void;
  onSubmitCurrentQuestion: () => void;
  onJumpToQuestion: (idx: number) => void;
  onFinishPractice: () => void;
}>();
</script>

<style scoped lang="scss">
.quiz-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 10px;

  .stem-box {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 16px;
    font-weight: 600;
    line-height: 1.7;
    color: #0f172a;
    margin-bottom: 24px;
    padding: 14px 18px;
    background: #f8fafc;
    border-radius: 12px;
    border-left: 4px solid #1677ff;

    .stem-no {
      color: #1677ff;
    }
  }

  .options-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 24px;

    .option-item {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px 18px;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
      background: #fff;
      cursor: pointer;
      transition: all 0.2s;

      .option-key-circle {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: #f1f5f9;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        color: #475569;
        font-size: 13px;
      }

      &:hover {
        border-color: #93c5fd;
        background: #f8fafc;
      }

      &.selected {
        border-color: #1677ff;
        background: #eff6ff;

        .option-key-circle {
          background: #1677ff;
          color: #fff;
        }
      }

      &.is-right {
        border-color: #52c41a;
        background: #f6ffed;

        .option-key-circle {
          background: #52c41a;
          color: #fff;
        }
      }

      &.is-error {
        border-color: #f5222d;
        background: #fff1f0;

        .option-key-circle {
          background: #f5222d;
          color: #fff;
        }
      }
    }
  }

  .quiz-controls-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #f1f5f9;
    flex-wrap: wrap;
    gap: 12px;

    .right-action {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }
}
</style>

<template>
  <div class="practice-header-hud">
    <div class="hud-left">
      <el-tag effect="dark" type="primary" class="q-progress-badge">
        第 {{ currentIndex + 1 }} / {{ questions.length }} 题
      </el-tag>
      <span class="q-type-label">{{ currentQuestion.typeText }}</span>
      <el-tag size="small" :type="getDifficultyTag(currentQuestion.difficulty)" effect="plain">
        {{ currentQuestion.difficulty }}
      </el-tag>
      <span v-if="usedSeconds > 0" class="timer-label">
        <el-icon><Timer /></el-icon>
        {{ formatTime(usedSeconds) }}
      </span>
    </div>

    <div class="hud-center">
      <div class="nav-dots">
        <span
          v-for="(q, idx) in questions"
          :key="q.id"
          class="nav-dot"
          :class="{
            active: idx === currentIndex,
            answered: userAnswers[idx] !== undefined,
            correct: answersState[idx] === 'CORRECT',
            wrong: answersState[idx] === 'WRONG'
          }"
          @click="onJumpToQuestion(idx)"
        >
          {{ idx + 1 }}
        </span>
      </div>
    </div>

    <div class="hud-right">
      <el-button type="danger" text @click="onConfirmExit">退出练习</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Timer } from '@element-plus/icons-vue';
import type { PracticeQuestion } from '@/composables/learning/useAIPractice';

defineProps<{
  currentIndex: number;
  questions: PracticeQuestion[];
  userAnswers: Record<number, string>;
  answersState: Record<number, 'CORRECT' | 'WRONG' | 'PENDING'>;
  usedSeconds: number;
  currentQuestion: PracticeQuestion;
  getDifficultyTag: (difficulty: string) => string;
  onJumpToQuestion: (idx: number) => void;
  onConfirmExit: () => void;
}>();

function formatTime(sec: number) {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}
</script>

<style scoped lang="scss">
.practice-header-hud {
  background: #fff;
  border-radius: 14px;
  padding: 12px 20px;
  border: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  .hud-left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    .q-progress-badge {
      font-weight: 700;
      border-radius: 9999px;
    }

    .q-type-label {
      font-weight: 600;
      font-size: 13px;
      color: #475569;
    }

    .timer-label {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #64748b;
    }
  }

  .hud-center {
    .nav-dots {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      .nav-dot {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: 1px solid #cbd5e1;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: 700;
        color: #64748b;
        cursor: pointer;
        transition: all 0.2s;

        &.active {
          border-color: #1677ff;
          color: #1677ff;
          transform: scale(1.15);
        }

        &.answered {
          background: #f1f5f9;
        }

        &.correct {
          background: #52c41a;
          border-color: #52c41a;
          color: #fff;
        }

        &.wrong {
          background: #f5222d;
          border-color: #f5222d;
          color: #fff;
        }
      }
    }
  }
}
</style>

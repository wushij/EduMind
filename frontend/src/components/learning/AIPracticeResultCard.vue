<template>
  <el-card shadow="never" class="result-card">
    <div class="result-header">
      <div class="score-circle">
        <span class="score-value">{{ finalScore }}</span>
        <span class="score-unit">分</span>
      </div>
      <div class="result-meta">
        <h3 class="result-title">本次自适应专项练习报告</h3>
        <p class="result-subtitle">
          共完成 {{ questions.length }} 道练习题，正确率 {{ accuracyRate }}%，用时约
          {{ Math.max(1, Math.round(usedSeconds / 60)) }} 分钟。
        </p>
        <div class="stat-pills">
          <el-tag type="success">正确 {{ correctCount }} 题</el-tag>
          <el-tag type="danger">错误 {{ questions.length - correctCount }} 题</el-tag>
          <el-tag v-if="wrongCount > 0" type="warning">已收录错题 {{ wrongCount }} 道</el-tag>
        </div>
      </div>
    </div>

    <el-divider />

    <div class="result-advice">
      <h4>
        <el-icon><ChatLineSquare /></el-icon>
        AI 学习导师建议
      </h4>
      <p>{{ aiSummary || '正在生成学习建议…' }}</p>
    </div>

    <div class="result-actions">
      <el-button size="large" @click="onResetToSetup">再练一组</el-button>
      <el-button size="large" @click="onGoToReport">查看学情报告</el-button>
      <el-button size="large" type="primary" @click="onGoToWrongQuestions">
        查看错题本与深度归因
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ChatLineSquare } from '@element-plus/icons-vue';
import type { PracticeQuestion } from '@/composables/learning/useAIPractice';

const props = defineProps<{
  questions: PracticeQuestion[];
  correctCount: number;
  finalScore: number;
  accuracyRate: number;
  usedSeconds: number;
  aiSummary: string;
  wrongQuestionIds?: number[];
  onResetToSetup: () => void;
  onGoToWrongQuestions: () => void;
  onGoToReport: () => void;
}>();

const wrongCount = computed(() => props.wrongQuestionIds?.length ?? props.questions.length - props.correctCount);
</script>

<style scoped lang="scss">
.result-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 24px;

  .result-header {
    display: flex;
    align-items: center;
    gap: 28px;
    flex-wrap: wrap;

    .score-circle {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      box-shadow: 0 8px 24px rgba(22, 119, 255, 0.25);

      .score-value {
        font-size: 38px;
        font-weight: 800;
      }

      .score-unit {
        font-size: 14px;
        margin-top: 12px;
      }
    }

    .result-meta {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .result-title {
        margin: 0;
        font-size: 22px;
        font-weight: 700;
        color: #0f172a;
      }

      .result-subtitle {
        margin: 0;
        font-size: 14px;
        color: #64748b;
      }

      .stat-pills {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
        margin-top: 4px;
      }
    }
  }

  .result-advice {
    background: #f8fafc;
    border-radius: 12px;
    padding: 16px 20px;
    margin: 20px 0;

    h4 {
      margin: 0 0 8px;
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.7;
      color: #475569;
    }
  }

  .result-actions {
    display: flex;
    justify-content: center;
    gap: 16px;
    flex-wrap: wrap;
    margin-top: 24px;
  }
}
</style>

<template>
  <div class="question-preview-page">
    <!-- 顶部状态与批处理动作栏 -->
    <div class="preview-top-bar">
      <div class="summary-col">
        <div class="title-row">
          <h1 class="page-title">AI 生成题目卡片预览</h1>
          <span class="pill-count-badge">已就绪 {{ generatedQuestions.length }} 道题目</span>
          <span class="pill-score-badge">卷面参考总分：{{ totalScore }} 分</span>
        </div>
        <p class="page-subtitle">
          题目已由大模型按照知识点大纲结构化组织，支持在此就地微调修改，满意后可一键批量导入题库。
        </p>
      </div>

      <div class="action-buttons-row">
        <button
          type="button"
          class="capsule-action-btn capsule-action-btn--default"
          @click="router.push('/ai/question/generate')"
        >
          <span>← 返回调整出题参数</span>
        </button>

        <button
          type="button"
          class="capsule-action-btn capsule-action-btn--primary"
          :disabled="generatedQuestions.length === 0 || saving"
          @click="handleBatchSave"
        >
          <span v-if="!saving">批量保存并入库 ({{ generatedQuestions.length }} 题)</span>
          <span v-else>正在保存入库...</span>
        </button>
      </div>
    </div>

    <!-- 题目卡片流式网格 -->
    <div v-if="generatedQuestions.length > 0" class="questions-stream-container">
      <QuestionCard
        v-for="(q, index) in generatedQuestions"
        :key="q.id"
        :question="q"
        :index="index"
        @delete="handleDeleteQuestion"
      />
    </div>

    <!-- 若被全部剔除时的空状态 -->
    <div v-else class="empty-questions-box">
      <span class="empty-emoji">📝</span>
      <h3>当前暂无生成的题目</h3>
      <p>所有题目已被剔除，可点击下方返回重新配置生成</p>
      <button
        type="button"
        class="capsule-action-btn capsule-action-btn--primary"
        @click="router.push('/ai/question/generate')"
      >
        重新生成题目
      </button>
    </div>

    <!-- 底部悬浮长圆快捷操作栏 -->
    <div v-if="generatedQuestions.length > 0" class="floating-dock-bar">
      <div class="dock-inner">
        <span class="dock-summary">
          共 <strong>{{ generatedQuestions.length }}</strong> 道高契合度试题，包含单选、多选与简答题
        </span>
        <button
          type="button"
          class="dock-save-btn"
          :disabled="saving"
          @click="handleBatchSave"
        >
          <span>一键批量入库</span>
          <span>✓</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import QuestionCard from '@/components/question/QuestionCard.vue';
import { useQuestionGenerate } from '@/composables/ai/useQuestionGenerate';

const router = useRouter();
const { generatedQuestions, deleteQuestion, batchSave } = useQuestionGenerate();
const saving = ref(false);

const totalScore = computed(() => {
  return generatedQuestions.value.reduce((acc, cur) => acc + (cur.score || 0), 0);
});

function handleDeleteQuestion(id: number) {
  deleteQuestion(id);
}

async function handleBatchSave() {
  saving.value = true;
  try {
    await batchSave();
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="scss">
.question-preview-page {
  max-width: 920px;
  margin: 0 auto;
  padding: 10px 0 80px;

  .preview-top-bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 24px;
    background: #FFFFFF;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

    .summary-col {
      .title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .page-title {
          margin: 0;
          font-size: 22px;
          font-weight: 700;
          color: #0F172A;
        }

        .pill-count-badge {
          padding: 2px 10px;
          border-radius: 9999px; // 长圆跑道
          background: #EAF3FF;
          color: #1677FF;
          font-size: 12px;
          font-weight: 600;
        }

        .pill-score-badge {
          padding: 2px 10px;
          border-radius: 9999px;
          background: #FEF3C7;
          color: #D97706;
          font-size: 12px;
          font-weight: 600;
        }
      }

      .page-subtitle {
        margin: 6px 0 0 0;
        font-size: 13.5px;
        color: #64748B;
        line-height: 1.5;
      }
    }

    .action-buttons-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;
    }
  }

  // 长圆按钮
  .capsule-action-btn {
    height: 40px;
    padding: 0 20px;
    border-radius: 9999px; // 长圆跑道胶囊
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.22s ease;
    white-space: nowrap;

    &--default {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      color: #475569;

      &:hover {
        border-color: #CBD5E1;
        color: #1677FF;
        background: #F8FAFC;
      }
    }

    &--primary {
      background: #1677FF;
      border: none;
      color: #FFFFFF;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.28);

      &:hover {
        background: #4096FF;
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }

  .questions-stream-container {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .empty-questions-box {
    padding: 80px 0;
    text-align: center;
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #EBF1F7;

    .empty-emoji {
      font-size: 40px;
      display: block;
      margin-bottom: 12px;
    }

    h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      color: #1E293B;
    }

    p {
      margin: 0 0 20px 0;
      font-size: 13.5px;
      color: #94A3B8;
    }
  }

  // 底部长圆快捷 Dock
  .floating-dock-bar {
    position: fixed;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 100;

    .dock-inner {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 8px 10px 8px 24px;
      border-radius: 9999px; // 长圆跑道胶囊
      background: rgba(15, 23, 42, 0.9);
      backdrop-filter: blur(12px);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
      border: 1px solid rgba(255, 255, 255, 0.15);

      .dock-summary {
        font-size: 13.5px;
        color: #F8FAFC;

        strong {
          color: #38BDF8;
        }
      }

      .dock-save-btn {
        height: 38px;
        padding: 0 20px;
        border-radius: 9999px;
        background: #1677FF;
        color: #FFFFFF;
        font-size: 13.5px;
        font-weight: 600;
        border: none;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        transition: all 0.2s;

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
        }
      }
    }
  }
}
</style>

<template>
  <el-card v-loading="loading" shadow="never" class="ai-teaching-advice">
    <template #header>
      <div class="advice-header">
        <el-icon class="advice-icon"><Opportunity /></el-icon>
        <span class="advice-title">AI 教学策略建议</span>
        <el-button
          v-if="showGenerate"
          type="primary"
          size="small"
          :loading="loading"
          @click="emit('generate')"
        >
          生成建议
        </el-button>
      </div>
    </template>

    <div v-if="advice" class="advice-body">
      <div class="summary-box">
        <div class="bot-avatar">
          <el-icon><Cpu /></el-icon>
        </div>
        <p class="summary-text">{{ advice.summary }}</p>
      </div>

      <ul v-if="advice.actions.length" class="action-list">
        <li v-for="(action, index) in advice.actions" :key="index">
          <span class="action-index">{{ index + 1 }}</span>
          <span>{{ action }}</span>
        </li>
      </ul>
    </div>

    <el-empty v-else description="暂无教学建议，点击生成获取 AI 分析" :image-size="80" />
  </el-card>
</template>

<script setup lang="ts">
import { Opportunity, Cpu } from '@element-plus/icons-vue';
import type { TeachingAdviceVO } from '@/types/analytics/mastery';

withDefaults(
  defineProps<{
    advice?: TeachingAdviceVO | null;
    loading?: boolean;
    showGenerate?: boolean;
  }>(),
  {
    advice: null,
    loading: false,
    showGenerate: true
  }
);

const emit = defineEmits<{
  generate: [];
}>();
</script>

<style scoped lang="scss">
.ai-teaching-advice {
  border-radius: 14px;
  background: linear-gradient(135deg, #FAF5FF 0%, #FFFFFF 100%);
  border-color: #E9D5FF;

  .advice-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .advice-icon {
      font-size: 18px;
      color: #722ED1;
    }

    .advice-title {
      flex: 1;
      font-weight: 700;
      color: #0F172A;
    }
  }

  .advice-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .summary-box {
    display: flex;
    gap: 12px;

    .bot-avatar {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      background: linear-gradient(135deg, #722ED1 0%, #9333EA 100%);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
      box-shadow: 0 3px 10px rgba(114, 46, 209, 0.25);
    }

    .summary-text {
      margin: 0;
      color: #334155;
      line-height: 1.7;
      font-size: 14px;
    }
  }

  .action-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;

    li {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 12px;
      background: #F8FAFC;
      border-radius: 10px;
      font-size: 13px;
      color: #475569;
    }

    .action-index {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #722ED1;
      color: #fff;
      font-size: 12px;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
  }
}
</style>

<template>
  <el-card v-loading="loading" shadow="never" class="ai-teaching-advice">
    <template #header>
      <div class="advice-header">
        <div class="header-left">
          <el-icon class="advice-icon"><Opportunity /></el-icon>
          <span class="advice-title">AI 教学策略建议</span>
          <span v-if="advice" class="ai-source-badge">DeepSeek 推演已生成</span>
        </div>

        <div class="header-right-actions">
          <button
            v-if="loading"
            type="button"
            class="capsule-btn capsule-btn--warning"
            @click="emit('stop')"
          >
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>停止推演</span>
          </button>

          <template v-else>
            <button
              v-if="advice"
              type="button"
              class="capsule-btn capsule-btn--danger"
              @click="handleClear"
            >
              <span>删除建议</span>
            </button>

            <button
              v-if="showGenerate"
              type="button"
              class="capsule-btn capsule-btn--primary"
              @click="emit('generate')"
            >
              <span>{{ advice ? '重新推演' : '生成建议' }}</span>
            </button>
          </template>
        </div>
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

    <el-empty v-else description="暂无教学建议，点击右上角「生成建议」获取 AI 深度策略推演" :image-size="80" />
  </el-card>
</template>

<script setup lang="ts">
import { Opportunity, Cpu, Loading } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
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
  (e: 'generate'): void;
  (e: 'clear'): void;
  (e: 'stop'): void;
}>();

function handleClear() {
  ElMessageBox.confirm('确定清空当前已生成的 AI 教学策略建议吗？', '清空建议确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('clear');
  }).catch(() => {});
}
</script>

<style scoped lang="scss">
.ai-teaching-advice {
  border-radius: 14px;
  background: linear-gradient(135deg, #FAF5FF 0%, #FFFFFF 100%);
  border-color: #E9D5FF;

  .advice-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .header-right-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .advice-icon {
      font-size: 18px;
      color: #722ED1;
    }

    .advice-title {
      font-weight: 700;
      color: #0F172A;
      font-size: 15px;
    }

    .ai-source-badge {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 9999px;
      background: #F3E8FF;
      color: #7E22CE;
      font-weight: 600;
    }

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 16px;
      height: 32px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      outline: none;
      box-sizing: border-box;

      &--warning {
        background: #FFFBEB;
        color: #D97706;
        border: 1px solid #FCD34D;
        gap: 6px;

        &:hover {
          background: #FEF3C7;
          color: #B45309;
          border-color: #F59E0B;
          transform: translateY(-1px);
        }
      }

      &--danger {
        background: #FEF2F2;
        color: #EF4444;
        border: 1px solid #FECACA;

        &:hover:not(:disabled) {
          background: #FEE2E2;
          color: #DC2626;
          border-color: #F87171;
          transform: translateY(-1px);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }

      &--primary {
        background: #1677FF;
        color: #FFFFFF;
        border: 1px solid transparent;
        box-shadow: 0 2px 6px rgba(22, 119, 255, 0.25);

        &:hover:not(:disabled) {
          background: #3B82F6;
          transform: translateY(-1px);
          box-shadow: 0 4px 10px rgba(22, 119, 255, 0.35);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }
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

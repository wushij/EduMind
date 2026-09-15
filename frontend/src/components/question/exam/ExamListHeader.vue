<template>
  <div class="exam-header-dock">
    <div class="header-left">
      <div class="title-with-icon">
        <el-icon class="header-icon text-blue-600"><Tickets /></el-icon>
        <h1 class="main-title">试卷与考试管理中心</h1>
        <span class="capsule-count-tag">已收录 {{ pageReady ? total : '--' }} 套标准化期末试卷</span>
      </div>
      <p class="sub-desc">
        集中管理期中/期末统一测试试卷、随堂测验与单元测试，支持双向细目表校验、AI 一键调优换题与格式化导出。
      </p>
    </div>

    <div class="header-right-actions">
      <button
        type="button"
        class="capsule-btn capsule-btn--ai"
        @click="routerInstance.push('/ai/exam/generate')"
      >
        <el-icon class="sparkle-icon"><MagicStick /></el-icon>
        <span>AI 智能组卷</span>
        <span class="pill-bubble">双向细目表</span>
      </button>

      <button
        type="button"
        class="capsule-btn capsule-btn--secondary"
        @click="routerInstance.push('/ai/question/generate')"
      >
        <el-icon><EditPen /></el-icon>
        <span>AI 智能出题</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Tickets, MagicStick, EditPen } from '@element-plus/icons-vue';

defineProps<{
  pageReady: boolean;
  total: number;
}>();

const routerInstance = useRouter();
</script>

<style scoped lang="scss">
.exam-header-dock {
  background: #FFFFFF;
  border-radius: 18px;
  padding: 24px 28px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;

  .header-left {
    max-width: 680px;

    .title-with-icon {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
      flex-wrap: wrap;

      .header-icon {
        font-size: 22px;
      }

      .main-title {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        letter-spacing: -0.3px;
      }

      .capsule-count-tag {
        padding: 2px 12px;
        border-radius: 9999px;
        background: #EFF6FF;
        border: 1px solid #BFDBFE;
        color: #1677FF;
        font-size: 11.5px;
        font-weight: 600;
      }
    }

    .sub-desc {
      margin: 0;
      font-size: 13.5px;
      color: #64748B;
      line-height: 1.6;
    }
  }

  .header-right-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 42px;
      padding: 0 20px;
      border-radius: 9999px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.22s ease;

      &--ai {
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        box-shadow: 0 4px 14px rgba(114, 46, 209, 0.28);

        .pill-bubble {
          font-size: 11px;
          padding: 1px 8px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.22);
        }

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(114, 46, 209, 0.38);
        }
      }

      &--secondary {
        background: #FFFFFF;
        color: #334155;
        border: 1.5px solid #CBD5E1;

        &:hover {
          background: #F8FAFC;
          color: #1677FF;
          border-color: #93C5FD;
        }
      }
    }
  }
}
</style>

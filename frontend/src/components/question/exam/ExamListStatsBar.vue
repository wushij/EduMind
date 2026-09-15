<template>
  <section class="exam-stats-bar">
    <div class="stat-pill-item">
      <el-icon class="pill-icon text-blue-600"><Document /></el-icon>
      <span class="pill-label">试卷总数：</span>
      <strong class="pill-val">{{ pageReady ? `${total} 套` : '--' }}</strong>
    </div>
    <div class="stat-pill-item">
      <el-icon class="pill-icon text-emerald-600"><CircleCheck /></el-icon>
      <span class="pill-label">已排版就绪：</span>
      <strong class="pill-val">{{ pageReady ? `${examCount} 套 (当前页)` : '--' }}</strong>
    </div>
    <div class="stat-pill-item">
      <el-icon class="pill-icon text-indigo-600"><User /></el-icon>
      <span class="pill-label">命题教师：</span>
      <strong class="pill-val">教研组统一命题</strong>
    </div>
    <div class="stat-pill-item stat-pill-item--highlight">
      <el-icon class="pill-icon text-amber-500"><Lightning /></el-icon>
      <span class="pill-label">AI 组卷提效：</span>
      <strong class="pill-val">平均节省备课 4.5 小时/套</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Document, CircleCheck, User, Lightning } from '@element-plus/icons-vue';

defineProps<{
  pageReady: boolean;
  total: number;
  examCount: number;
}>();
</script>

<style scoped lang="scss">
.exam-stats-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  min-height: 36px;

  .stat-pill-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 36px;
    padding: 0 16px;
    border-radius: 9999px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 10px rgba(30, 80, 150, 0.03);
    font-size: 12.5px;
    color: #64748B;

    .pill-icon {
      font-size: 14px;
    }

    .pill-val {
      color: #1E293B;
      font-weight: 700;
    }

    &--highlight {
      margin-left: auto;
      background: #EFF6FF;
      border-color: #BFDBFE;
      color: #1677FF;

      .pill-val {
        color: #1677FF;
      }
    }
  }
}
</style>

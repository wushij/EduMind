<template>
  <div class="content-card">
    <div class="card-header-row">
      <div class="card-header-left">
        <el-icon class="card-icon card-icon--emerald"><Aim /></el-icon>
        <h3 class="card-title">教学目标与能力达成度</h3>
      </div>
      <div v-if="editable" class="header-actions">
        <button type="button" class="table-action-pill" @click="emit('ai-lesson')">
          AI 教案
        </button>
        <button type="button" class="table-action-pill table-action-pill--primary" @click="emit('manage')">
          管理目标
        </button>
      </div>
    </div>

    <div v-if="objectives.length > 0" class="objectives-list">
      <div v-for="(obj, index) in objectives" :key="obj.id ?? index" class="objective-item">
        <div class="obj-index">{{ String(index + 1).padStart(2, '0') }}</div>
        <div class="obj-text">
          <strong>{{ obj.title }}</strong>
          <p v-if="obj.description">{{ obj.description }}</p>
        </div>
      </div>
    </div>
    <div v-else class="empty-block">
      <el-icon class="empty-icon"><Aim /></el-icon>
      <p class="empty-text">{{ editable ? '添加教学目标，帮助学生理解本课能力要求。' : '教师尚未配置教学目标。' }}</p>
      <button v-if="editable" type="button" class="capsule-mini-btn" @click="emit('manage')">添加第一条</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Aim } from '@element-plus/icons-vue';
import type { CourseObjectiveVO } from '@/types/course/overview';

defineProps<{
  objectives: CourseObjectiveVO[];
  editable?: boolean;
}>();

const emit = defineEmits<{ manage: []; 'ai-lesson': [] }>();
</script>

<style scoped lang="scss">
@import './course-overview-shared.scss';

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.objectives-list {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .objective-item {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 14px 18px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid #f1f5f9;

    .obj-index {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #1677ff;
      color: #fff;
      font-size: 13px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .obj-text {
      strong { font-size: 14px; color: #0f172a; }
      p {
        margin: 4px 0 0;
        font-size: 13px;
        color: #64748b;
        line-height: 1.5;
      }
    }
  }
}
</style>

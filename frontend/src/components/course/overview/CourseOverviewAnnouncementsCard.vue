<template>
  <div class="content-card">
    <div class="card-header-row">
      <div class="card-header-left">
        <el-icon class="card-icon card-icon--amber"><Bell /></el-icon>
        <h3 class="card-title">课程通知与近期安排</h3>
        <span v-if="total > 0" class="count-pill">共 {{ total }} 条</span>
      </div>
      <div class="header-actions">
        <button
          v-if="editable"
          type="button"
          class="table-action-pill table-action-pill--primary"
          @click="emit('publish')"
        >
          发布公告
        </button>
        <button
          v-if="total > 0"
          type="button"
          class="table-action-pill table-action-pill--primary"
          @click="emit('view-all')"
        >
          查看全部
        </button>
      </div>
    </div>

    <div v-if="announcements.length > 0" class="notice-list">
      <div v-for="item in announcements" :key="item.id" class="notice-item">
        <div class="notice-date">
          <span class="day">{{ dayPart(item.publishTime) }}</span>
          <span class="month">{{ monthPart(item.publishTime) }}</span>
        </div>
        <div class="notice-body">
          <div class="notice-title">
            <span v-if="item.pinned" class="pin-tag">置顶</span>
            {{ item.title }}
          </div>
          <div class="notice-desc">{{ excerpt(item.content) }}</div>
        </div>
      </div>
    </div>
    <div v-else class="empty-block">
      <el-icon class="empty-icon"><Bell /></el-icon>
      <p class="empty-text">{{ editable ? '发布课程通知，同步提醒选课成员。' : '暂无课程通知。' }}</p>
      <button v-if="editable" type="button" class="capsule-mini-btn" @click="emit('publish')">发布第一条</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue';
import type { CourseAnnouncementVO } from '@/types/course/overview';

defineProps<{
  announcements: CourseAnnouncementVO[];
  total: number;
  editable?: boolean;
}>();

const emit = defineEmits<{ publish: []; 'view-all': [] }>();

function dayPart(time?: string) {
  if (!time) return '--';
  const d = new Date(time);
  return Number.isNaN(d.getTime()) ? '--' : String(d.getDate()).padStart(2, '0');
}

function monthPart(time?: string) {
  if (!time) return '';
  const d = new Date(time);
  return Number.isNaN(d.getTime()) ? '' : `${d.getMonth() + 1}月`;
}

function excerpt(text?: string) {
  if (!text) return '';
  return text.length > 80 ? `${text.slice(0, 80)}…` : text;
}
</script>

<style scoped lang="scss">
@use './course-overview-shared.scss' as *;

.count-pill {
  height: 22px;
  padding: 0 8px;
  border-radius: 9999px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #ea580c;
  font-size: 11px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .notice-item {
    display: flex;
    align-items: center;
    gap: 18px;
    padding: 12px 0;
    border-bottom: 1px solid #f1f5f9;

    &:last-child { border-bottom: none; }

    .notice-date {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: #eff6ff;
      color: #1677ff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .day { font-size: 16px; font-weight: 700; line-height: 1; }
      .month { font-size: 10.5px; margin-top: 2px; }
    }

    .notice-title {
      font-size: 14px;
      font-weight: 600;
      color: #1e293b;

      .pin-tag {
        display: inline-block;
        margin-right: 6px;
        padding: 0 6px;
        border-radius: 9999px;
        background: #fef3c7;
        color: #d97706;
        font-size: 10px;
        font-weight: 700;
      }
    }

    .notice-desc {
      font-size: 12.5px;
      color: #64748b;
      margin-top: 4px;
    }
  }
}
</style>

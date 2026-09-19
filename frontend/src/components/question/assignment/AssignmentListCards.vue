<template>
  <div v-loading="loading" class="assignments-grid-wrapper">
    <div v-if="assignments.length > 0" class="assignments-stack">
      <div v-for="a in assignments" :key="a.id" class="assignment-row-card">
        <div class="row-left-info">
          <div class="tags-and-title">
            <el-tag :type="statusTag(a.status)" size="small" effect="light" round>
              {{ statusLabel(a.status) }}
            </el-tag>
            <h3 class="assignment-title" @click="router.push(`/question/assignments/${a.id}`)">
              {{ a.title }}
            </h3>
          </div>
          <div class="meta-sub-row">
            <span class="meta-item">
              <el-icon class="meta-icon"><Reading /></el-icon>
              {{ a.courseName || getCourseName(a.courseId) }}
            </span>
            <span class="meta-item">
              <el-icon class="meta-icon"><Clock /></el-icon>
              截止时间：{{ a.deadline || '—' }}
            </span>
            <span class="meta-item">
              <el-icon class="meta-icon"><User /></el-icon>
              已提交：<strong>{{ a.submissionCount || 0 }}</strong> 份
            </span>
          </div>
        </div>
        <div class="row-center-progress">
          <div class="prog-label">
            <span>班级提交进度</span>
            <span>{{ progressLabel(a) }}</span>
          </div>
          <el-progress :percentage="progressPercent(a)" :color="progressColor(a)" />
        </div>
        <div class="row-right-actions">
          <button
            v-if="a.status === 'DRAFT'"
            v-permission="'assignment:delete'"
            type="button"
            class="table-action-pill table-action-pill--danger"
            @click.stop="emit('delete', a.id, a.title)"
          >
            删除
          </button>
          <el-button type="primary" size="small" @click="router.push(`/question/assignments/${a.id}`)">
            批改与答卷管理
          </el-button>
          <el-button type="success" plain size="small" :icon="Cpu" @click="emit('ai-grade', a.id)">
            一键 AI 批改
          </el-button>
        </div>
      </div>
    </div>
    <div v-else class="empty-state-panel">
      <el-icon class="empty-icon"><FolderOpened /></el-icon>
      <h3>暂无匹配的作业任务</h3>
      <p>您可以点击右上角「发布新作业」，为学生选拔试题并设定考核时间。</p>
      <el-button type="primary" @click="router.push('/question/assignments/create')">
        立即发布新作业
      </el-button>
    </div>
    <AppPagination
      v-model:page-num="pageNumProxy"
      v-model:page-size="pageSizeProxy"
      :total="total"
      @change="emit('page-change')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Reading, Clock, User, Cpu, FolderOpened } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { Assignment } from '@/types/question/assignment';
import { ASSIGNMENT_STATUS_LABEL, ASSIGNMENT_STATUS_TAG } from '@/constants/question/assignment';

const props = defineProps<{
  assignments: Assignment[];
  loading: boolean;
  total: number;
  pageNum: number;
  pageSize: number;
  getCourseName: (courseId?: number) => string;
  progressPercent: (a: Assignment) => number;
  progressLabel: (a: Assignment) => string;
}>();

const emit = defineEmits<{
  'update:pageNum': [value: number];
  'update:pageSize': [value: number];
  'page-change': [];
  'ai-grade': [id: number];
  delete: [id: number, title: string];
}>();

const router = useRouter();

const pageNumProxy = computed({
  get: () => props.pageNum,
  set: (v: number) => emit('update:pageNum', v)
});

const pageSizeProxy = computed({
  get: () => props.pageSize,
  set: (v: number) => emit('update:pageSize', v)
});

function statusLabel(status: string) {
  return ASSIGNMENT_STATUS_LABEL[status] || status;
}

function statusTag(status: string) {
  return ASSIGNMENT_STATUS_TAG[status] || 'info';
}

function progressColor(a: Assignment) {
  const ratio = (a.submissionCount || 0) / Math.max(a.studentCount || 1, 1);
  if (ratio >= 0.9) return '#10b981';
  if (ratio >= 0.6) return '#3b82f6';
  return '#f59e0b';
}
</script>

<style scoped lang="scss">
.assignments-grid-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assignments-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.assignment-row-card {
  display: grid;
  grid-template-columns: 1fr minmax(180px, 220px) auto;
  gap: 20px;
  align-items: center;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

.tags-and-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.assignment-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  cursor: pointer;

  &:hover {
    color: #2563eb;
  }
}

.meta-sub-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #64748b;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.prog-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.row-right-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.empty-state-panel {
  text-align: center;
  padding: 48px 24px;
  background: #fff;
  border-radius: 16px;
  border: 1px dashed #cbd5e1;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }

  h3 {
    margin: 0 0 8px;
    color: #334155;
  }

  p {
    color: #64748b;
    margin-bottom: 16px;
  }
}

.table-action-pill {
  border: 1px solid #fecaca;
  background: #fff;
  color: #dc2626;
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 13px;
  cursor: pointer;
}
</style>

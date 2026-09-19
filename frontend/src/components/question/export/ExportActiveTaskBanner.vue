<template>
  <div v-if="task" class="export-active-task-banner" role="status" aria-live="polite">
    <div class="banner-main">
      <el-icon v-if="isProcessing" class="banner-icon is-spin"><Loading /></el-icon>
      <el-icon v-else-if="task.status === 'SUCCESS'" class="banner-icon is-success"><CircleCheck /></el-icon>
      <el-icon v-else-if="task.status === 'FAILED'" class="banner-icon is-error"><CircleClose /></el-icon>
      <el-icon v-else class="banner-icon"><Clock /></el-icon>

      <div class="banner-text">
        <p class="banner-title">{{ bannerTitle }}</p>
        <p class="banner-desc">{{ bannerDesc }}</p>
        <el-progress
          v-if="isProcessing"
          :percentage="progressPercent"
          :stroke-width="6"
          :show-text="false"
          class="banner-progress"
        />
      </div>
    </div>

    <div class="banner-actions">
      <button
        v-if="task.status === 'SUCCESS'"
        type="button"
        class="module-capsule-btn module-capsule-btn--primary module-capsule-btn--sm"
        @click="emit('download')"
      >
        再次下载
      </button>
      <button
        v-if="isProcessing"
        type="button"
        class="module-capsule-btn module-capsule-btn--secondary module-capsule-btn--sm"
        @click="emit('viewJobs')"
      >
        查看任务列表
      </button>
      <button
        v-if="!isProcessing"
        type="button"
        class="banner-dismiss"
        aria-label="关闭提示"
        @click="emit('dismiss')"
      >
        关闭
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Loading, CircleCheck, CircleClose, Clock } from '@element-plus/icons-vue';
import type { ActiveExportTask } from '@/composables/question/useExport';

const props = defineProps<{
  task: ActiveExportTask | null;
}>();

const emit = defineEmits<{
  download: [];
  viewJobs: [];
  dismiss: [];
}>();

const isProcessing = computed(
  () => props.task?.status === 'PENDING' || props.task?.status === 'PROCESSING'
);

const progressPercent = computed(() => {
  const p = props.task?.progress;
  if (typeof p === 'number' && p > 0) return Math.min(100, p);
  if (props.task?.status === 'PENDING') return 8;
  if (props.task?.status === 'PROCESSING') return 15;
  return 100;
});

const bannerTitle = computed(() => {
  const t = props.task;
  if (!t) return '';
  if (t.status === 'PENDING') return 'PDF 已加入导出队列';
  if (t.status === 'PROCESSING') return '正在服务端排版生成 PDF';
  if (t.status === 'SUCCESS') return 'PDF 已生成，可下载';
  if (t.status === 'FAILED') return 'PDF 生成失败';
  return '导出任务';
});

const bannerDesc = computed(() => {
  const t = props.task;
  if (!t) return '';
  const name = t.title ? `「${t.title}」` : '';
  if (t.status === 'PENDING') {
    return `${name} 后台 Worker 即将开始排版，请稍候（非浏览器直接下载）。`;
  }
  if (t.status === 'PROCESSING') {
    return `${name} 正在渲染卷面、公式与分页，完成后将自动尝试下载。`;
  }
  if (t.status === 'SUCCESS') {
    return `${name} 文件已写入云端归档，若未自动下载可点击右侧按钮。`;
  }
  if (t.status === 'FAILED') {
    return t.errorMsg || '请检查试卷是否有题目、MinIO 是否可用，或稍后重试。';
  }
  return '';
});
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.export-active-task-banner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid #bfdbfe;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);

  .banner-main {
    display: flex;
    gap: 12px;
    flex: 1;
    min-width: 0;
  }

  .banner-icon {
    font-size: 22px;
    color: #2563eb;
    margin-top: 2px;
    flex-shrink: 0;

    &.is-success {
      color: #16a34a;
    }

    &.is-error {
      color: #dc2626;
    }

    &.is-spin {
      animation: export-spin 1s linear infinite;
    }
  }

  .banner-text {
    flex: 1;
    min-width: 0;
  }

  .banner-title {
    margin: 0 0 4px;
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
  }

  .banner-desc {
    margin: 0;
    font-size: 12px;
    line-height: 1.55;
    color: #475569;
  }

  .banner-progress {
    margin-top: 10px;
    max-width: 420px;
  }

  .banner-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    flex-shrink: 0;
  }

  .module-capsule-btn--sm {
    padding: 6px 14px;
    font-size: 12px;
  }

  .banner-dismiss {
    border: none;
    background: transparent;
    color: #64748b;
    font-size: 12px;
    cursor: pointer;
    padding: 6px 8px;

    &:hover {
      color: #334155;
    }
  }
}

@keyframes export-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

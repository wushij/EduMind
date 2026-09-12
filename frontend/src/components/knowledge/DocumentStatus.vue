<template>
  <div class="doc-pipeline-status-badge">
    <el-tag
      :type="statusTagType"
      size="small"
      effect="light"
      class="status-tag"
    >
      <span class="status-dot" :class="statusDotClass"></span>
      <span class="status-text">{{ statusLabel }}</span>
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  status?: string;
  stage?: 'parse' | 'chunk' | 'index';
}>();

const statusLabel = computed(() => {
  switch (props.status) {
    case 'PARSED':
    case 'CHUNKED':
    case 'INDEXED':
      return '已就绪';
    case 'PARSING':
      return '正在解析';
    case 'CHUNKING':
      return '正在切片';
    case 'INDEXING':
      return '正在索引';
    case 'FAILED':
    case 'CHUNK_FAILED':
    case 'INDEX_FAILED':
      return '异常失败';
    case 'PENDING':
      return '排队等待';
    default:
      return props.status || '就绪';
  }
});

const statusTagType = computed(() => {
  switch (props.status) {
    case 'PARSED':
    case 'CHUNKED':
    case 'INDEXED':
      return 'success';
    case 'PARSING':
    case 'CHUNKING':
    case 'INDEXING':
    case 'PENDING':
      return 'warning';
    case 'FAILED':
    case 'CHUNK_FAILED':
    case 'INDEX_FAILED':
      return 'danger';
    default:
      return 'info';
  }
});

const statusDotClass = computed(() => {
  switch (props.status) {
    case 'PARSED':
    case 'CHUNKED':
    case 'INDEXED':
      return 'dot-success';
    case 'PARSING':
    case 'CHUNKING':
    case 'INDEXING':
    case 'PENDING':
      return 'dot-warning';
    case 'FAILED':
    case 'CHUNK_FAILED':
    case 'INDEX_FAILED':
      return 'dot-danger';
    default:
      return 'dot-info';
  }
});
</script>

<style scoped lang="scss">
.doc-pipeline-status-badge {
  display: inline-flex;
  align-items: center;

  .status-tag {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 11.5px;
    padding: 2px 8px;
    border-radius: 6px;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;

      &.dot-success { background: #10B981; }
      &.dot-warning { background: #F59E0B; }
      &.dot-danger { background: #EF4444; }
      &.dot-info { background: #94A3B8; }
    }
  }
}
</style>

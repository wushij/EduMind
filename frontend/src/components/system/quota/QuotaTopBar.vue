<template>
  <div class="quota-top-bar">
    <div class="context-info">
      <el-icon><School /></el-icon>
      <span class="tenant-title">当前租户大盘：{{ tenantName }} ({{ campusName }})</span>
      <el-tag size="small" type="success" effect="plain">企业教育旗舰版</el-tag>
    </div>
    <div class="action-buttons">
      <el-button round :icon="Refresh" class="btn-refresh" :loading="refreshing" @click="emit('refresh')">
        刷新算力大盘
      </el-button>
      <el-button type="primary" class="gradient-btn" @click="emit('open-config')">
        <el-icon><Setting /></el-icon>
        <span>租户全局配额策略</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh, School, Setting } from '@element-plus/icons-vue';

defineProps<{
  tenantName: string;
  campusName: string;
  refreshing: boolean;
}>();

const emit = defineEmits<{
  refresh: [];
  'open-config': [];
}>();
</script>

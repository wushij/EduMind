<template>
  <div class="compute-quota-management-card">
    <!-- 顶部 Tab 导航：严格围绕“算力与配额”进行划分 -->
    <div class="quota-tabs-header">
      <div class="tabs-nav-group">
        <button
          type="button"
          class="tab-nav-btn"
          :class="{ active: activeTab === 'allocation' }"
          @click="emit('update:activeTab', 'allocation')"
        >
          <el-icon><DataAnalysis /></el-icon>
          <span>校区与院系配额切分大盘</span>
          <span class="tab-badge">{{ deptQuotaListLength }}</span>
        </button>
        <button
          type="button"
          class="tab-nav-btn"
          :class="{ active: activeTab === 'ledger' }"
          @click="emit('update:activeTab', 'ledger')"
        >
          <el-icon><Files /></el-icon>
          <span>算力消耗与配额抵扣流水</span>
          <span class="tab-badge">{{ total }}</span>
        </button>
      </div>

      <div class="quota-meta-tag">
        <span class="dot-online"></span>
        <span>算力池实时计费状态：正常入账中</span>
      </div>
    </div>

    <slot />
  </div>
</template>

<script setup lang="ts">
import { DataAnalysis, Files } from '@element-plus/icons-vue';

defineProps<{
  activeTab: 'allocation' | 'ledger';
  deptQuotaListLength: number;
  total: number;
}>();

const emit = defineEmits<{
  'update:activeTab': [value: 'allocation' | 'ledger'];
}>();
</script>

<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <span class="stat-num text-primary">{{ tokenPercentage }}%</span>
      <span class="stat-label">总算力 Token 水位</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-success">{{ (quotas.storageUsed / 1024).toFixed(2) }} GB</span>
      <span class="stat-label">向量检索库容量</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-warning">{{ quotas.qpsPeak }} QPS</span>
      <span class="stat-label">今日并发峰值</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num" :class="tokenPercentage >= quotas.tokenWarningThreshold ? (tokenPercentage >= 100 ? 'text-danger' : 'text-warning') : 'text-info'">
        {{ tokenPercentage >= 100 ? '超额阻断' : (tokenPercentage >= quotas.tokenWarningThreshold ? '水位预警' : '配额正常') }}
      </span>
      <span class="stat-label">算力配额健康状态</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TenantQuotasState } from '@/composables/system/useTenantQuota';

defineProps<{
  tokenPercentage: number;
  quotas: TenantQuotasState;
}>();
</script>

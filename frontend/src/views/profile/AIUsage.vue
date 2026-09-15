<template>
  <div v-loading="loading" class="user-ai-usage-page">
    <AIUsageHeroCard
      :loading="loading"
      :usage="usage"
      :quota-pill-class="quotaPillClass"
      :today-usage-percent="todayUsagePercent"
      :last-updated-text="lastUpdatedText"
      :format-number="formatNumber"
      @refresh="loadUsage(true)"
    />

    <AIUsageMetricsGrid :usage="usage" :format-number="formatNumber" />

    <AIUsageLogPanel
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :loading="loading"
      :log-loading="logLoading"
      :usage="usage"
      :log-total="logTotal"
      :table-header-style="tableHeaderStyle"
      :format-number="formatNumber"
      :format-date-time="formatDateTime"
      @page-change="loadUsage(false, { tableOnly: true })"
    />
  </div>
</template>

<script setup lang="ts">
import AIUsageHeroCard from '@/components/profile/AIUsageHeroCard.vue';
import AIUsageMetricsGrid from '@/components/profile/AIUsageMetricsGrid.vue';
import AIUsageLogPanel from '@/components/profile/AIUsageLogPanel.vue';
import { useAIUsage } from '@/composables/profile/useAIUsage';

const {
  loading,
  logLoading,
  usage,
  pageNum,
  pageSize,
  logTotal,
  todayUsagePercent,
  quotaPillClass,
  lastUpdatedText,
  tableHeaderStyle,
  formatNumber,
  formatDateTime,
  loadUsage
} = useAIUsage();
</script>

<style scoped lang="scss">
.user-ai-usage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 8px;
}
</style>

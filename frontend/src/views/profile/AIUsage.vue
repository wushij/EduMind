<template>
  <div v-loading="loading" class="user-ai-usage-page profile-page-shell">
    <AIUsageHeroCard
      v-model:selected-period="selectedPeriod"
      :loading="loading"
      :usage="usage"
      :active-period-info="activePeriodInfo"
      :quota-pill-class="quotaPillClass"
      :today-usage-percent="todayUsagePercent"
      :last-updated-text="lastUpdatedText"
      :format-number="formatNumber"
      @refresh="loadUsage(true)"
    />

    <AIUsageMetricsGrid
      :usage="usage"
      :format-number="formatNumber"
      :week-tokens-used="weekTokensUsed"
      :month-tokens-used="monthTokensUsed"
      :total-tokens-used="totalTokensUsed"
      :today-calls="todayCalls"
      :week-calls="weekCalls"
      :month-calls="monthCalls"
      :total-calls="totalCalls"
      :month-cost-r-m-b="monthCostRMB"
      :total-cost-r-m-b="totalCostRMB"
    />

    <AIUsageLogPanel
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      v-model:selected-days="selectedLogDays"
      :loading="loading"
      :log-loading="logLoading"
      :usage="usage"
      :log-total="logTotal"
      :table-header-style="tableHeaderStyle"
      :format-number="formatNumber"
      :format-date-time="formatDateTime"
      @page-change="loadUsage(false, { tableOnly: true })"
      @days-change="changeLogDays"
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
  selectedPeriod,
  selectedLogDays,
  todayUsagePercent,
  quotaPillClass,
  weekTokensUsed,
  monthTokensUsed,
  totalTokensUsed,
  todayCalls,
  weekCalls,
  monthCalls,
  totalCalls,
  monthCostRMB,
  totalCostRMB,
  activePeriodInfo,
  lastUpdatedText,
  tableHeaderStyle,
  formatNumber,
  formatDateTime,
  loadUsage,
  changeLogDays
} = useAIUsage();
</script>

<style scoped lang="scss">
@use '@/styles/profile-page-shell.scss';

.user-ai-usage-page {
  /* shell gap handled by profile-page-shell */
}
</style>

<template>
  <div class="gateway-dashboard-page">
    <PageHeroBanner
      title="AI 模型网关与算力调度监控 · 全链路治理大盘"
      subtitle="全链路监控大模型网关请求吞吐、Token 算力消耗水位、调用延迟 SLA、多服务商路由健康度与熔断防御"
      background-variant="system"
      size="large"
    >
      <template #extra>
        <GatewayHeroStats :metrics="metrics" :format-tokens="formatTokens" />
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <GatewayControlBar
        :loading="loading"
        :range="range"
        :range-options="rangeOptions"
        v-model:auto-refresh="autoRefresh"
        :last-sync-time="lastSyncTime"
        @select-range="selectRange"
        @toggle-auto-refresh="toggleAutoRefresh"
        @refresh="loadMetrics"
        @open-routes="router.push('/system/gateway/routes')"
        @open-trace="openTraceDrawer"
      />

      <div class="dashboard-metrics-body" v-loading="loading">
        <el-alert
          v-if="usedMockFallback"
          type="info"
          :closable="false"
          show-icon
          title="当前处于 Mock 预览模式，呈现样例数据"
          class="mock-alert"
        />

        <GatewayMetricsGrid
          :metrics="metrics"
          :get-prompt-ratio="getPromptRatio"
          :format-tokens="formatTokens"
          @reset-circuit="handleResetCircuit"
        />

        <GatewayChartsRow :metrics="metrics" />

        <GatewayProviderTable
          v-model:model-search-key="modelSearchKey"
          :filtered-providers="filteredProviders"
          :format-tokens="formatTokens"
          :get-calls-percentage="getCallsPercentage"
          :get-model-provider-brand="getModelProviderBrand"
          :get-circuit-state-class="getCircuitStateClass"
          :get-circuit-state-text="getCircuitStateText"
          @open-trace-by-model="openTraceByModel"
          @reset-model-circuit="handleResetModelCircuit"
        />
      </div>
    </div>

    <GatewayCircuitPanel
      v-model:visible="traceDrawerVisible"
      v-model:trace-model-filter="traceModelFilter"
      v-model:trace-scene-filter="traceSceneFilter"
      v-model:trace-page="tracePage"
      v-model:trace-page-size="tracePageSize"
      :trace-loading="traceLoading"
      :trace-logs="traceLogs"
      :trace-total="traceTotal"
      :translate-scene-name="translateSceneName"
      @load-trace-logs="loadTraceLogs"
    />
  </div>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import GatewayHeroStats from '@/components/system/gateway/GatewayHeroStats.vue';
import GatewayControlBar from '@/components/system/gateway/GatewayControlBar.vue';
import GatewayMetricsGrid from '@/components/system/gateway/GatewayMetricsGrid.vue';
import GatewayChartsRow from '@/components/system/gateway/GatewayChartsRow.vue';
import GatewayProviderTable from '@/components/system/gateway/GatewayProviderTable.vue';
import GatewayCircuitPanel from '@/components/system/gateway/GatewayCircuitPanel.vue';
import { useGateway } from '@/composables/system/useGateway';

const {
  router,
  loading,
  usedMockFallback,
  range,
  rangeOptions,
  metrics,
  lastSyncTime,
  modelSearchKey,
  autoRefresh,
  traceDrawerVisible,
  traceLoading,
  traceLogs,
  traceModelFilter,
  traceSceneFilter,
  tracePage,
  tracePageSize,
  traceTotal,
  getPromptRatio,
  filteredProviders,
  formatTokens,
  getCallsPercentage,
  getModelProviderBrand,
  getCircuitStateClass,
  getCircuitStateText,
  translateSceneName,
  selectRange,
  loadMetrics,
  toggleAutoRefresh,
  handleResetCircuit,
  handleResetModelCircuit,
  openTraceDrawer,
  openTraceByModel,
  loadTraceLogs
} = useGateway();
</script>

<style scoped lang="scss">
.gateway-dashboard-page {
  padding-bottom: 40px;

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .dashboard-metrics-body {
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-height: 200px;
  }
}
</style>

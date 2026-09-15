<template>
  <div class="dashboard-control-bar">
    <div class="filter-tools">
      <div class="range-segmented-control">
        <button
          v-for="opt in rangeOptions"
          :key="opt.value"
          type="button"
          class="range-tab-item"
          :class="{ 'is-active': range === opt.value }"
          @click="$emit('select-range', opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>

      <div class="auto-refresh-switch" :class="{ 'is-active': autoRefresh }">
        <el-switch
          :model-value="autoRefresh"
          size="small"
          @update:model-value="$emit('update:autoRefresh', $event)"
          @change="$emit('toggle-auto-refresh', $event)"
        />
        <span class="switch-tip">30s 自动刷新</span>
      </div>

      <span class="sync-time">数据最后同步：{{ lastSyncTime || '--:--:--' }}</span>
    </div>

    <div class="action-buttons">
      <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="$emit('refresh')">
        刷新
      </el-button>

      <el-button type="primary" round class="gradient-btn" @click="$emit('open-routes')">
        <el-icon><Connection /></el-icon>
        <span>路由规则配置</span>
      </el-button>

      <el-button plain round @click="$emit('open-trace')">
        <el-icon><Document /></el-icon>
        <span>Trace 明细</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh, Connection, Document } from '@element-plus/icons-vue';
import { GATEWAY_RANGE_OPTIONS } from '@/composables/system/useGateway';

defineProps<{
  loading: boolean;
  range: string;
  autoRefresh: boolean;
  lastSyncTime: string;
  rangeOptions?: typeof GATEWAY_RANGE_OPTIONS;
}>();

defineEmits<{
  'select-range': [value: string];
  'update:autoRefresh': [value: boolean];
  'toggle-auto-refresh': [value: boolean];
  refresh: [];
  'open-routes': [];
  'open-trace': [];
}>();
</script>

<style scoped lang="scss">
.dashboard-control-bar {
  background: #FFFFFF;
  border-radius: 14px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  border: 1px solid #E2E8F0;

  .filter-tools {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    min-width: 0;
  }

  .sync-time {
    font-size: 12px;
    color: #94A3B8;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
    flex-shrink: 0;
    padding: 5px 12px;
    border-radius: 999px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
  }

  .action-buttons {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-left: auto;
    flex-shrink: 0;
  }

  .range-segmented-control {
    display: inline-flex;
    align-items: center;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
    border-radius: 999px;
    padding: 3px;
    gap: 2px;
    flex-shrink: 0;

    .range-tab-item {
      border: none;
      background: transparent;
      padding: 6px 14px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      color: #64748B;
      cursor: pointer;
      white-space: nowrap;
      transition: color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        color: #1E293B;
      }

      &.is-active {
        background: #FFFFFF;
        color: #1677FF;
        box-shadow: inset 0 0 0 1px rgba(22, 119, 255, 0.12), 0 2px 8px rgba(15, 23, 42, 0.08);
      }
    }
  }

  .refresh-btn {
    min-width: 84px;
  }

  .auto-refresh-switch {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: #F8FAFC;
    padding: 5px 12px 5px 10px;
    border-radius: 999px;
    border: 1px solid #E2E8F0;
    transition: background-color 0.2s ease, border-color 0.2s ease;
    flex-shrink: 0;

    &.is-active {
      background: #EFF6FF;
      border-color: #BFDBFE;

      .switch-tip {
        color: #2563EB;
      }
    }

    .switch-tip {
      font-size: 12px;
      font-weight: 500;
      color: #64748B;
      white-space: nowrap;
    }
  }

  .gradient-btn {
    background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
    border: none;
    color: #FFFFFF;
  }
}
</style>

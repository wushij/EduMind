<template>
  <div class="filter-capsule-card">
    <div class="filter-row">
      <div class="filter-inputs">
        <el-input
          v-model="queryParams.title"
          placeholder="搜索操作模块/标题..."
          clearable
          class="pill-input"
          :prefix-icon="Search"
          @keyup.enter="$emit('query')"
          @clear="$emit('query')"
        />
        <el-input
          v-model="queryParams.operName"
          placeholder="搜索操作人员姓名/账号..."
          clearable
          class="pill-input"
          :prefix-icon="Search"
          @keyup.enter="$emit('query')"
          @clear="$emit('query')"
        />
        <el-select
          v-model="queryParams.businessType"
          placeholder="业务类型"
          clearable
          class="pill-select"
          @change="$emit('query')"
        >
          <el-option label="全部类型" :value="null" />
          <el-option label="新增 (INSERT)" :value="1" />
          <el-option label="修改 (UPDATE)" :value="2" />
          <el-option label="删除 (DELETE)" :value="3" />
          <el-option label="授权/变更 (GRANT)" :value="7" />
          <el-option label="导出 (EXPORT)" :value="5" />
          <el-option label="导入 (IMPORT)" :value="6" />
          <el-option label="清空 (CLEAN)" :value="8" />
          <el-option label="其它 (OTHER)" :value="0" />
        </el-select>
        <el-select
          v-model="queryParams.status"
          placeholder="执行状态"
          clearable
          class="pill-select"
          @change="$emit('query')"
        >
          <el-option label="全部状态" :value="null" />
          <el-option label="正常成功 (0)" :value="0" />
          <el-option label="异常拦截 (1)" :value="1" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :shortcuts="dateRangeShortcuts"
          class="pill-date-picker"
          @change="onDateRangeChange"
        />
      </div>

      <div class="filter-actions">
        <button
          type="button"
          class="pill-btn pill-btn--primary"
          :disabled="loading"
          @click="$emit('query')"
        >
          <el-icon class="mr-1"><Search /></el-icon>
          <span>检索</span>
        </button>
        <button
          type="button"
          class="pill-btn pill-btn--default"
          :disabled="loading"
          @click="$emit('reset')"
        >
          <el-icon class="mr-1" :class="{ 'is-loading': loading }"><Refresh /></el-icon>
          <span>重置</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue';
import type { OperLogPageQuery } from '@/types/system/oper-log';
import { OPER_LOG_DATE_SHORTCUTS } from '@/composables/system/useAudit';

const dateRange = defineModel<[string, string] | null>('dateRange', { required: true });

const queryParams = defineModel<OperLogPageQuery>('queryParams', { required: true });

defineProps<{
  loading: boolean;
  dateRangeShortcuts?: typeof OPER_LOG_DATE_SHORTCUTS;
}>();

const emit = defineEmits<{
  query: [];
  reset: [];
  'date-range-change': [range: [string, string] | null];
}>();

function onDateRangeChange(range: [string, string] | null) {
  emit('date-range-change', range);
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.filter-capsule-card {
  background: #ffffff;
  border-radius: $border-radius-xl;
  padding: 16px 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: $shadow-sm;

  .filter-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;

    .filter-inputs {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      flex: 1;

      .pill-input {
        width: 200px;
        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
          &:hover {
            box-shadow: 0 0 0 1px #cbd5e1 inset;
          }
          &.is-focus {
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.25) inset;
          }
        }
      }

      .pill-select {
        width: 140px;
        :deep(.el-select__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
        }
      }

      .pill-date-picker {
        width: 250px;
        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          padding-right: 14px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
        }
      }
    }

    .filter-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

/* 跑道药丸胶囊按钮通用样式 */
.pill-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &--primary {
    background: #1677ff;
    color: #ffffff;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
    &:hover {
      background: #4096ff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
    }
  }

  &--default {
    background: #f1f5f9;
    color: #475569;
    border-color: #e2e8f0;
    &:hover {
      background: #e2e8f0;
      color: #1e293b;
    }
  }

  &:disabled {
    opacity: 0.65;
    cursor: not-allowed;
    transform: none !important;
  }
}
</style>

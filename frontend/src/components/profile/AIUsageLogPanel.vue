<template>
  <div class="log-panel-card">
    <div class="panel-header-line">
      <div class="panel-header-left">
        <div class="panel-icon-badge">
          <el-icon><Clock /></el-icon>
        </div>
        <div>
          <h3 class="panel-title">近期个人 AI 交互记录</h3>
          <span class="panel-sub">近 7 天调用明细</span>
        </div>
      </div>
      <span v-if="logTotal > 0" class="record-count-pill">
        近 7 天共 {{ logTotal }} 条
      </span>
    </div>

    <div v-loading="logLoading" class="log-table-wrap">
      <el-empty
        v-if="!loading && !logLoading && usage.recentLogs.length === 0"
        class="empty-state"
        description="暂无 AI 调用记录，使用智能助教或出题功能后将在此展示"
      />

      <template v-else>
        <el-table
          :data="usage.recentLogs"
          style="width: 100%"
          :header-cell-style="tableHeaderStyle"
          :row-class-name="() => 'log-table-row'"
        >
          <el-table-column prop="sceneLabel" label="使用功能 / 场景" min-width="200">
            <template #default="{ row }">
              <span class="tool-title">{{ row.sceneLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="model" label="基座模型" width="220">
            <template #default="{ row }">
              <span class="model-pill">{{ row.model || 'unknown' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalTokens" label="消耗 Token" width="150" align="center">
            <template #default="{ row }">
              <span class="tokens-pill">{{ formatNumber(row.totalTokens) }} toks</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="交互时间" width="190">
            <template #default="{ row }">
              <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <AppPagination
          v-model:page-num="pageNum"
          v-model:page-size="pageSize"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          @change="emit('page-change')"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Clock } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });

defineProps<{
  loading: boolean;
  logLoading: boolean;
  usage: PersonalAiUsageVO;
  logTotal: number;
  tableHeaderStyle: Record<string, string | number>;
  formatNumber: (value: number) => string;
  formatDateTime: (value: string) => string;
}>();

const emit = defineEmits<{
  'page-change': [];
}>();
</script>

<style scoped lang="scss">
.log-panel-card {
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid #E2E8F0;
  padding: 24px 28px 8px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);

  .panel-header-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;
    gap: 12px;
    flex-wrap: wrap;

    .panel-header-left {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .panel-icon-badge {
      width: 44px;
      height: 44px;
      border-radius: 16px;
      background: linear-gradient(135deg, #EFF6FF 0%, #F5F3FF 100%);
      border: 1px solid #E2E8F0;
      color: #1677FF;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .panel-title {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
    }

    .panel-sub {
      display: block;
      font-size: 12px;
      color: #94A3B8;
      margin-top: 2px;
    }

    .record-count-pill {
      padding: 6px 16px;
      border-radius: 9999px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      font-size: 12px;
      font-weight: 600;
      color: #64748B;
    }
  }

  .log-table-wrap {
    :deep(.empty-state) {
      padding: 48px 0;

      .el-empty__description {
        color: #94A3B8;
        font-size: 13px;
      }
    }

    :deep(.el-table) {
      --el-table-border-color: transparent;
      --el-table-row-hover-bg-color: #F8FAFC;
      background: transparent;

      &::before { display: none; }

      .el-table__header-wrapper th.el-table__cell {
        border-bottom: 1px solid #EEF2F7;
      }

      .log-table-row td.el-table__cell {
        padding: 14px 0;
        border-bottom: 1px solid #F8FAFC;
      }

      .log-table-row:last-child td.el-table__cell {
        border-bottom: none;
      }
    }
  }

  .tool-title {
    font-size: 13.5px;
    font-weight: 600;
    color: #1E293B;
  }

  .model-pill {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 500;
    color: #475569;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
  }

  .tokens-pill {
    display: inline-block;
    font-family: ui-monospace, 'Cascadia Code', monospace;
    font-size: 12px;
    font-weight: 600;
    color: #2563EB;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    padding: 4px 12px;
    border-radius: 9999px;
  }

  .time-text {
    font-size: 12.5px;
    color: #64748B;
    font-variant-numeric: tabular-nums;
  }
}
</style>

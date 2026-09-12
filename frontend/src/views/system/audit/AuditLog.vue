<template>
  <div class="audit-log-page">
    <!-- 1. 顶部 Token 审计与成本核心卡片 -->
    <div class="metrics-grid">
      <div class="stat-card">
        <div class="card-icon icon-blue">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="card-details">
          <span class="label">累计 AI 调用总次数</span>
          <span class="val">{{ summary.totalCalls.toLocaleString() }} <span class="unit">次</span></span>
          <span class="hint text-green">调用成功率 {{ summary.successRate }}%</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-icon icon-purple">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="card-details">
          <span class="label">Token 消耗总规模</span>
          <span class="val text-purple">{{ (summary.totalTokens / 10000).toFixed(1) }} <span class="unit">万 toks</span></span>
          <span class="hint">约 {{ (summary.totalTokens / 1000000).toFixed(2) }} M Tokens</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-icon icon-amber">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="card-details">
          <span class="label">预估模型消耗总成本</span>
          <span class="val text-amber">¥ {{ summary.totalCostRMB.toFixed(2) }}</span>
          <span class="hint">按各厂商 1K Tokens 阶梯计费</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-icon icon-emerald">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="card-details">
          <span class="label">平均响应耗时 (Avg Latency)</span>
          <span class="val text-emerald">{{ summary.avgLatencyMs }} <span class="unit">ms</span></span>
          <span class="hint">流式首包平均 180ms</span>
        </div>
      </div>
    </div>

    <!-- 2. 近 7 天趋势可视化看板 -->
    <div class="trend-visual-card">
      <div class="visual-header">
        <span class="title">近 7 天 Token 消耗与调用频次趋势分析</span>
        <span class="sub">每日自动归档统计</span>
      </div>

      <div class="chart-bars-mock">
        <div
          v-for="d in summary.dailyTrend"
          :key="d.date"
          class="day-col"
        >
          <div class="bar-visual-wrapper">
            <div
              class="bar-fill"
              :style="{ height: `${Math.min(100, Math.round((d.calls / 35000) * 100))}%` }"
              :title="`${d.date}: ${d.calls} 次调用, ${d.cost} 元`"
            ></div>
          </div>
          <span class="day-label">{{ d.date }}</span>
          <span class="day-calls">{{ (d.calls / 1000).toFixed(1) }}k次</span>
        </div>
      </div>
    </div>

    <!-- 3. 调用审计明细表格 -->
    <div class="log-table-section">
      <div class="table-header-bar">
        <div class="left">
          <span class="sec-title">AI 调用全链路审计日志</span>
        </div>
        <div class="filter-controls">
          <el-select v-model="statusFilter" size="small" style="width: 130px" @change="loadLogs">
            <el-option value="" label="全部状态" />
            <el-option value="SUCCESS" label="成功 (SUCCESS)" />
            <el-option value="TIMEOUT" label="超时 (TIMEOUT)" />
            <el-option value="FAILED" label="异常 (FAILED)" />
          </el-select>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户或工具名称..."
            size="small"
            style="width: 220px"
            clearable
            :prefix-icon="Search"
            @keyup.enter="loadLogs"
            @clear="loadLogs"
          />
        </div>
      </div>

      <el-card shadow="never" class="log-card">
        <el-table
          v-loading="tableLoading"
          :data="logs"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600' }"
        >
          <el-table-column prop="traceId" label="链路 Trace ID" width="160">
            <template #default="{ row }">
              <span class="trace-badge">{{ row.traceId }}</span>
            </template>
          </el-table-column>

          <el-table-column label="调用用户" width="140">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="username">{{ row.username }}</span>
                <span class="role-badge">{{ row.userRole }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="toolName" label="业务场景 / 工具" min-width="160">
            <template #default="{ row }">
              <span class="tool-name">{{ row.toolName || '通用对话' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="model" label="生成模型" width="140">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.model }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Token 规模" width="180">
            <template #default="{ row }">
              <div class="tokens-cell">
                <span class="tot">总计: <strong>{{ row.totalTokens }}</strong></span>
                <span class="det">(入:{{ row.promptTokens }} / 出:{{ row.completionTokens }})</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="耗时" width="100">
            <template #default="{ row }">
              <span class="duration-text">{{ row.durationMs }}ms</span>
            </template>
          </el-table-column>

          <el-table-column label="调用状态" width="110">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'SUCCESS' ? 'success' : row.status === 'TIMEOUT' ? 'warning' : 'danger'"
                size="small"
              >
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="请求时间" width="160" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getAuditSummary, getAuditLogs } from '@/api/system/audit';
import { AIAuditLog, AuditSummaryVO } from '@/types/system/audit';
import {
  DataLine,
  Cpu,
  Coin,
  Timer,
  Search
} from '@element-plus/icons-vue';

const summary = ref<AuditSummaryVO>({
  totalCalls: 0,
  totalTokens: 0,
  totalCostRMB: 0,
  avgLatencyMs: 0,
  successRate: 100,
  dailyTrend: [],
  modelDistribution: []
});

const logs = ref<AIAuditLog[]>([]);
const tableLoading = ref(false);
const searchKeyword = ref('');
const statusFilter = ref('');

const loadSummary = async () => {
  summary.value = await getAuditSummary();
};

const loadLogs = async () => {
  tableLoading.value = true;
  try {
    const res = await getAuditLogs({
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined
    });
    logs.value = res.list;
  } finally {
    tableLoading.value = false;
  }
};

onMounted(() => {
  loadSummary();
  loadLogs();
});
</script>

<style scoped lang="scss">
.audit-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;

    @media (max-width: 1024px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .stat-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 16px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

      .card-icon {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;

        &.icon-blue { background: #EFF6FF; color: #2563EB; }
        &.icon-purple { background: #FAF5FF; color: #9333EA; }
        &.icon-amber { background: #FFFBEB; color: #D97706; }
        &.icon-emerald { background: #ECFDF5; color: #059669; }
      }

      .card-details {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .label { font-size: 12px; color: #64748B; font-weight: 500; }
        .val {
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;

          &.text-purple { color: #9333EA; }
          &.text-amber { color: #D97706; }
          &.text-emerald { color: #059669; }

          .unit { font-size: 12px; font-weight: normal; color: #94A3B8; }
        }
        .hint { font-size: 11px; color: #94A3B8; &.text-green { color: #16A34A; } }
      }
    }
  }

  .trend-visual-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 18px 24px;
    box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

    .visual-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .title { font-size: 14px; font-weight: 700; color: #1E293B; }
      .sub { font-size: 12px; color: #94A3B8; }
    }

    .chart-bars-mock {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      height: 140px;
      padding-top: 10px;

      .day-col {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        flex: 1;

        .bar-visual-wrapper {
          width: 32px;
          height: 90px;
          background: #F1F5F9;
          border-radius: 6px;
          display: flex;
          align-items: flex-end;
          overflow: hidden;

          .bar-fill {
            width: 100%;
            background: linear-gradient(180deg, #3B82F6 0%, #1D4ED8 100%);
            border-radius: 6px 6px 0 0;
            transition: height 0.4s ease;
          }
        }

        .day-label { font-size: 12px; color: #64748B; }
        .day-calls { font-size: 11px; color: #94A3B8; font-family: ui-monospace, monospace; }
      }
    }
  }

  .log-table-section {
    .table-header-bar {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-bottom: none;
      border-radius: 12px 12px 0 0;
      padding: 14px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .sec-title { font-size: 14px; font-weight: 700; color: #1E293B; }
      .filter-controls { display: flex; align-items: center; gap: 10px; }
    }

    .log-card {
      border-radius: 0 0 12px 12px;

      .trace-badge {
        font-family: ui-monospace, monospace;
        font-size: 11.5px;
        color: #475569;
        background: #F1F5F9;
        padding: 2px 6px;
        border-radius: 4px;
      }

      .user-cell {
        display: flex;
        align-items: center;
        gap: 6px;

        .username { font-size: 13px; font-weight: 600; color: #1E293B; }
        .role-badge { font-size: 10px; color: #64748B; background: #F8FAFC; border: 1px solid #E2E8F0; padding: 1px 4px; border-radius: 3px; }
      }

      .tool-name { font-size: 13px; color: #334155; }

      .tokens-cell {
        display: flex;
        flex-direction: column;
        gap: 2px;
        font-size: 12px;

        .tot { color: #1E293B; }
        .det { font-size: 11px; color: #94A3B8; }
      }

      .duration-text {
        font-family: ui-monospace, monospace;
        font-size: 12px;
        color: #059669;
        font-weight: 600;
      }
    }
  }
}
</style>

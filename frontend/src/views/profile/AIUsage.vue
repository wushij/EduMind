<template>
  <div v-loading="loading" class="user-ai-usage-page">
    <!-- 顶部 Hero -->
    <div class="usage-hero-card">
      <div class="hero-bg-glow hero-bg-glow--blue" />
      <div class="hero-bg-glow hero-bg-glow--purple" />
      <div class="hero-content">
        <div class="hero-left">
          <span class="hero-eyebrow">Personal AI Dashboard</span>
          <h2>个人 AI 使用量与配额</h2>
          <p>实时监控大模型调用频次、Token 消耗明细及今日可用额度</p>
        </div>
        <div class="hero-right">
          <div class="hero-actions">
            <el-button
              round
              class="btn-refresh"
              :icon="Refresh"
              :loading="loading"
              @click="loadUsage(true)"
            >
              刷新
            </el-button>
            <span v-if="lastUpdatedText" class="updated-at-pill">{{ lastUpdatedText }}</span>
          </div>
          <div class="quota-pill" :class="quotaPillClass">
            <span class="quota-dot" />
            {{ usage.quotaStatus }}
          </div>
          <div class="quota-percent-pill">
            剩余 <strong>{{ usage.remainingPercent }}%</strong>
          </div>
        </div>
      </div>

      <!-- 今日额度进度条 -->
      <div class="hero-progress-block">
        <div class="progress-meta">
          <span class="progress-label">今日 Token 额度</span>
          <span class="progress-val">
            {{ formatNumber(usage.todayTokensUsed) }}
            <span class="sep">/</span>
            {{ formatNumber(usage.dailyTokenLimit) }}
          </span>
        </div>
        <div class="capsule-progress-track">
          <div
            class="capsule-progress-fill"
            :class="quotaPillClass"
            :style="{ width: `${todayUsagePercent}%` }"
          />
        </div>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="metric-cards-grid">
      <div class="metric-card metric-card--blue">
        <div class="metric-card-inner">
          <div class="card-icon icon-blue">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="card-info">
            <span class="label">今日已消耗 Token</span>
            <span class="val text-blue">
              {{ formatNumber(usage.todayTokensUsed) }}
              <span class="unit">toks</span>
            </span>
            <span class="sub">限额 {{ formatNumber(usage.dailyTokenLimit) }} toks / 日</span>
          </div>
        </div>
      </div>

      <div class="metric-card metric-card--purple">
        <div class="metric-card-inner">
          <div class="card-icon icon-purple">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="card-info">
            <span class="label">累计智能问答与出题</span>
            <span class="val text-purple">
              {{ formatNumber(usage.totalQaAndGenerateCalls) }}
              <span class="unit">次</span>
            </span>
            <span class="sub">平均耗时 {{ usage.avgLatencyMs }}ms</span>
          </div>
        </div>
      </div>

      <div class="metric-card metric-card--amber">
        <div class="metric-card-inner">
          <div class="card-icon icon-amber">
            <el-icon><Coin /></el-icon>
          </div>
          <div class="card-info">
            <span class="label">本学期预估成本</span>
            <span class="val text-amber">¥ {{ usage.semesterEstimatedCostRMB.toFixed(2) }}</span>
            <span class="sub sub-pill">由教学平台全额资助</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 交互记录 -->
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
            @change="loadUsage(false, { tableOnly: true })"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ChatDotRound, Clock, Coin, DataAnalysis, Refresh } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { getMyAiUsage } from '@/api/profile/ai-usage';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

const AI_USAGE_CHANGED_EVENT = 'edumind:ai-usage-changed';
const REFRESH_INTERVAL_MS = 15000;

const loading = ref(false);
const logLoading = ref(false);
const silentLoading = ref(false);
const lastUpdatedAt = ref<Date | null>(null);
const pageNum = ref(1);
const pageSize = ref(10);
const logTotal = ref(0);
let refreshTimer: ReturnType<typeof setInterval> | null = null;

const usage = reactive<PersonalAiUsageVO>({
  todayTokensUsed: 0,
  dailyTokenLimit: 100000,
  remainingPercent: 100,
  quotaStatus: '今日额度充足',
  totalQaAndGenerateCalls: 0,
  avgLatencyMs: 0,
  semesterEstimatedCostRMB: 0,
  recentLogs: [],
  totalLogCount: 0,
  logPageNum: 1,
  logPageSize: 10
});

const todayUsagePercent = computed(() => {
  if (!usage.dailyTokenLimit) return 0;
  return Math.min(100, Math.round((usage.todayTokensUsed / usage.dailyTokenLimit) * 100));
});

const quotaPillClass = computed(() => {
  if (usage.remainingPercent >= 50) return 'is-success';
  if (usage.remainingPercent >= 20) return 'is-warning';
  return 'is-danger';
});

const lastUpdatedText = computed(() => {
  if (!lastUpdatedAt.value) return '';
  return `更新于 ${lastUpdatedAt.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`;
});

const tableHeaderStyle = {
  background: '#F8FAFC',
  color: '#64748B',
  fontWeight: '600',
  fontSize: '12px',
  borderBottom: '1px solid #EEF2F7'
};

const formatNumber = (value: number) => value.toLocaleString('zh-CN');

const formatDateTime = (value: string) => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
};

const loadUsage = async (manual = false, options?: { tableOnly?: boolean }) => {
  if (loading.value || silentLoading.value || logLoading.value) return;
  if (manual) {
    loading.value = true;
  } else if (options?.tableOnly) {
    logLoading.value = true;
  } else if (usage.recentLogs.length > 0) {
    silentLoading.value = true;
  } else {
    logLoading.value = true;
  }
  try {
    const res = await getMyAiUsage({
      logDays: 7,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    if (res?.data) {
      Object.assign(usage, res.data);
      logTotal.value = res.data.totalLogCount ?? 0;
      pageNum.value = res.data.logPageNum ?? pageNum.value;
      pageSize.value = res.data.logPageSize ?? pageSize.value;
      lastUpdatedAt.value = new Date();
    }
  } catch (err) {
    if (manual) {
      ElMessage.error('加载 AI 使用统计失败');
    }
    console.error('[AIUsage] load failed', err);
  } finally {
    loading.value = false;
    silentLoading.value = false;
    logLoading.value = false;
  }
};

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    loadUsage();
  }
};

const handleAiUsageChanged = () => {
  pageNum.value = 1;
  loadUsage();
};

const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      loadUsage();
    }
  }, REFRESH_INTERVAL_MS);
};

onMounted(() => {
  loadUsage(true);
  startAutoRefresh();
  document.addEventListener('visibilitychange', handleVisibilityChange);
  window.addEventListener(AI_USAGE_CHANGED_EVENT, handleAiUsageChanged);
});

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  window.removeEventListener(AI_USAGE_CHANGED_EVENT, handleAiUsageChanged);
});
</script>

<style scoped lang="scss">
.user-ai-usage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 8px;

  // ── Hero ──
  .usage-hero-card {
    position: relative;
    overflow: hidden;
    background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFF 55%, #F5F3FF 100%);
    border: 1px solid #E2E8F0;
    border-radius: 24px;
    padding: 28px 32px 24px;
    box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);

    .hero-bg-glow {
      position: absolute;
      border-radius: 50%;
      pointer-events: none;
      filter: blur(60px);
      opacity: 0.35;

      &--blue {
        width: 220px;
        height: 220px;
        background: #BFDBFE;
        top: -80px;
        right: 120px;
      }

      &--purple {
        width: 160px;
        height: 160px;
        background: #DDD6FE;
        bottom: -60px;
        left: 40px;
      }
    }

    .hero-content {
      position: relative;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      margin-bottom: 22px;

      @media (max-width: 768px) {
        flex-direction: column;
      }
    }

    .hero-left {
      .hero-eyebrow {
        display: inline-block;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: #1677FF;
        background: rgba(22, 119, 255, 0.08);
        border: 1px solid rgba(22, 119, 255, 0.15);
        padding: 4px 14px;
        border-radius: 9999px;
        margin-bottom: 10px;
      }

      h2 {
        margin: 0;
        font-size: 22px;
        font-weight: 800;
        color: #0F172A;
        letter-spacing: -0.02em;
      }

      p {
        margin: 6px 0 0;
        font-size: 13.5px;
        color: #64748B;
        line-height: 1.5;
      }
    }

    .hero-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 10px;
      flex-shrink: 0;
      flex-wrap: wrap;
    }

    .hero-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      justify-content: flex-end;
    }

    .refresh-pill-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 7px 16px;
      border-radius: 9999px;
      border: 1px solid #BFDBFE;
      background: #FFFFFF;
      color: #2563EB;
      font-size: 12.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);

      &:hover:not(:disabled) {
        background: #EFF6FF;
        border-color: #93C5FD;
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }

    .updated-at-pill {
      padding: 6px 12px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.85);
      border: 1px solid #E2E8F0;
      font-size: 11.5px;
      color: #94A3B8;
    }

    .quota-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 8px 18px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      border: 1px solid transparent;

      .quota-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex-shrink: 0;
      }

      &.is-success {
        background: #ECFDF5;
        color: #059669;
        border-color: #A7F3D0;

        .quota-dot { background: #10B981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25); }
      }

      &.is-warning {
        background: #FFFBEB;
        color: #D97706;
        border-color: #FDE68A;

        .quota-dot { background: #F59E0B; box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.25); }
      }

      &.is-danger {
        background: #FEF2F2;
        color: #DC2626;
        border-color: #FECACA;

        .quota-dot { background: #EF4444; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.25); }
      }
    }

    .quota-percent-pill {
      padding: 8px 16px;
      border-radius: 9999px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      font-size: 13px;
      color: #64748B;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

      strong {
        color: #0F172A;
        font-weight: 700;
      }
    }

    .hero-progress-block {
      position: relative;

      .progress-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
        font-size: 12.5px;

        .progress-label {
          color: #64748B;
          font-weight: 500;
        }

        .progress-val {
          color: #1E293B;
          font-weight: 600;
          font-variant-numeric: tabular-nums;

          .sep {
            color: #CBD5E1;
            margin: 0 4px;
            font-weight: 400;
          }
        }
      }

      .capsule-progress-track {
        width: 100%;
        height: 10px;
        background: rgba(226, 232, 240, 0.8);
        border-radius: 9999px;
        overflow: hidden;

        .capsule-progress-fill {
          height: 100%;
          border-radius: 9999px;
          transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);

          &.is-success {
            background: linear-gradient(90deg, #34D399 0%, #10B981 100%);
          }

          &.is-warning {
            background: linear-gradient(90deg, #FBBF24 0%, #F59E0B 100%);
          }

          &.is-danger {
            background: linear-gradient(90deg, #F87171 0%, #EF4444 100%);
          }
        }
      }
    }
  }

  // ── 指标卡片 ──
  .metric-cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }

    .metric-card {
      border-radius: 22px;
      border: 1px solid #E2E8F0;
      overflow: hidden;
      transition: transform 0.2s, box-shadow 0.2s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 12px 28px rgba(15, 23, 42, 0.07);
      }

      &--blue { background: linear-gradient(160deg, #FFFFFF 0%, #EFF6FF 100%); }
      &--purple { background: linear-gradient(160deg, #FFFFFF 0%, #FAF5FF 100%); }
      &--amber { background: linear-gradient(160deg, #FFFFFF 0%, #FFFBEB 100%); }

      .metric-card-inner {
        padding: 22px 24px;
        display: flex;
        align-items: center;
        gap: 18px;
      }

      .card-icon {
        width: 52px;
        height: 52px;
        border-radius: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        flex-shrink: 0;

        &.icon-blue {
          background: #FFFFFF;
          color: #2563EB;
          border: 1px solid #BFDBFE;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
        }

        &.icon-purple {
          background: #FFFFFF;
          color: #9333EA;
          border: 1px solid #E9D5FF;
          box-shadow: 0 4px 12px rgba(147, 51, 234, 0.12);
        }

        &.icon-amber {
          background: #FFFFFF;
          color: #D97706;
          border: 1px solid #FDE68A;
          box-shadow: 0 4px 12px rgba(217, 119, 6, 0.12);
        }
      }

      .card-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;

        .label {
          font-size: 12px;
          color: #64748B;
          font-weight: 500;
        }

        .val {
          font-size: 22px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.02em;
          font-variant-numeric: tabular-nums;

          &.text-blue { color: #2563EB; }
          &.text-purple { color: #9333EA; }
          &.text-amber { color: #D97706; }

          .unit {
            font-size: 12px;
            font-weight: 500;
            color: #94A3B8;
            margin-left: 2px;
          }
        }

        .sub {
          font-size: 11.5px;
          color: #94A3B8;
        }

        .sub-pill {
          display: inline-block;
          width: fit-content;
          padding: 2px 10px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.7);
          border: 1px solid #FDE68A;
          color: #B45309;
          font-weight: 500;
        }
      }
    }
  }

  // ── 记录面板 ──
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
}
</style>

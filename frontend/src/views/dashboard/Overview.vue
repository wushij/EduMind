<template>
  <div class="dashboard-overview-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">全景教学与 AI 运营看板</h2>
        <span class="page-subtitle">宏观掌握平台教学资产、大模型推理交互频次与学情测评总览</span>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Refresh" @click="handleRefresh" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 核心资产指标看板 -->
    <div class="overview-metrics-grid">
      <div class="metric-card" v-for="kpi in kpiCards" :key="kpi.id" :class="`metric-card--${kpi.theme}`">
        <div class="card-icon-wrap">
          <el-icon><component :is="kpi.icon" /></el-icon>
        </div>
        <div class="card-data-wrap">
          <div class="metric-value-row">
            <span class="metric-value">{{ kpi.value }}</span>
            <span class="metric-unit">{{ kpi.unit }}</span>
          </div>
          <span class="metric-label">{{ kpi.label }}</span>
          <span class="metric-sub">{{ kpi.sublabel }}</span>
        </div>
      </div>
    </div>

    <!-- 趋势图与知识资产 -->
    <div class="overview-charts-grid">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-header-between">
            <span class="header-title">AI 助教交互与教学趋势</span>
            <el-radio-group v-model="selectedDataPeriod" size="small">
              <el-radio-button label="WEEK">近7天</el-radio-button>
              <el-radio-button label="MONTH">近30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div v-loading="trendLoading" ref="teachingLineChartRef" class="big-chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-header-between">
            <span class="header-title">学生学情综合层级分布</span>
            <el-button text size="small" type="primary" @click="router.push('/analytics/learning')">
              进入学情看板 &gt;
            </el-button>
          </div>
        </template>
        <div class="pie-chart-flex">
          <div ref="abilityPieChartRef" class="pie-chart-container"></div>
          <div class="pie-legend-col">
            <div v-for="item in abilityLegend" :key="item.label" class="pie-legend-row">
              <div class="legend-dot-label">
                <span class="legend-color-dot" :style="{ backgroundColor: item.color }"></span>
                <span class="legend-name">{{ item.label }}</span>
              </div>
              <span class="legend-val">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 深度运营与分析直达 -->
    <div class="deep-analytics-links">
      <div class="link-card" @click="router.push('/analytics/learning')">
        <div class="link-icon link-icon--blue"><el-icon><TrendCharts /></el-icon></div>
        <div class="link-text">
          <span class="link-title">学情综合诊断中心</span>
          <span class="link-desc">多维认知素养雷达图、薄弱知识点智能探查</span>
        </div>
      </div>

      <div class="link-card" @click="router.push('/knowledge')">
        <div class="link-icon link-icon--purple"><el-icon><FolderOpened /></el-icon></div>
        <div class="link-text">
          <span class="link-title">RAG 知识库工程大屏</span>
          <span class="link-desc">查看文档切片向量化、召回测试与知识图谱</span>
        </div>
      </div>

      <div class="link-card" @click="router.push('/ai/marketplace')">
        <div class="link-icon link-icon--green"><el-icon><Cpu /></el-icon></div>
        <div class="link-text">
          <span class="link-title">AI 工具应用中心</span>
          <span class="link-desc">教案备课、智能组卷、作业批改全栈工具集</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh, TrendCharts, FolderOpened, Cpu } from '@element-plus/icons-vue';
import { useDashboard } from '@/composables/dashboard/useDashboard';

const {
  router,
  loading,
  trendLoading,
  kpiCards,
  selectedDataPeriod,
  abilityLegend,
  teachingLineChartRef,
  abilityPieChartRef,
  fetchSummary,
  fetchTrendData
} = useDashboard({ enableCharts: true });

function handleRefresh() {
  fetchSummary();
  fetchTrendData();
}
</script>

<style scoped lang="scss">
.dashboard-overview-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-left {
      .page-title {
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 4px 0;
      }

      .page-subtitle {
        font-size: 13px;
        color: #64748B;
      }
    }
  }

  .overview-metrics-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }

    .metric-card {
      background: #FFFFFF;
      border-radius: 16px;
      padding: 20px;
      border: 1px solid #E2E8F0;
      display: flex;
      align-items: center;
      gap: 16px;

      .card-icon-wrap {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        flex-shrink: 0;
      }

      &--blue {
        .card-icon-wrap { background: #EFF6FF; color: #2563EB; }
      }
      &--emerald {
        .card-icon-wrap { background: #ECFDF5; color: #059669; }
      }
      &--purple {
        .card-icon-wrap { background: #F5F3FF; color: #7C3AED; }
      }
      &--amber {
        .card-icon-wrap { background: #FFFBEB; color: #D97706; }
      }

      .card-data-wrap {
        display: flex;
        flex-direction: column;
        gap: 3px;
        min-width: 0;

        .metric-value-row {
          display: flex;
          align-items: baseline;
          gap: 4px;

          .metric-value {
            font-size: 24px;
            font-weight: 800;
            color: #0F172A;
          }

          .metric-unit {
            font-size: 12px;
            color: #64748B;
          }
        }

        .metric-label {
          font-size: 13px;
          font-weight: 600;
          color: #334155;
        }

        .metric-sub {
          font-size: 11px;
          color: #94A3B8;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }

  .overview-charts-grid {
    display: grid;
    grid-template-columns: 6fr 4fr;
    gap: 20px;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    .chart-card {
      border-radius: 16px;
      border: 1px solid #E2E8F0;

      .card-header-between {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .header-title {
          font-size: 15px;
          font-weight: 700;
          color: #1E293B;
        }
      }

      .big-chart-container {
        width: 100%;
        height: 260px;
      }

      .pie-chart-flex {
        display: flex;
        align-items: center;
        justify-content: space-around;
        height: 260px;

        .pie-chart-container {
          width: 180px;
          height: 240px;
        }

        .pie-legend-col {
          display: flex;
          flex-direction: column;
          gap: 12px;
          flex: 1;
          padding-left: 20px;

          .pie-legend-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 13px;

            .legend-dot-label {
              display: flex;
              align-items: center;
              gap: 8px;

              .legend-color-dot {
                width: 8px;
                height: 8px;
                border-radius: 50%;
              }

              .legend-name {
                color: #475569;
              }
            }

            .legend-val {
              font-weight: 700;
              color: #1E293B;
            }
          }
        }
      }
    }
  }

  .deep-analytics-links {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }

    .link-card {
      background: #FFFFFF;
      border-radius: 14px;
      padding: 16px 18px;
      border: 1px solid #E2E8F0;
      display: flex;
      align-items: center;
      gap: 14px;
      cursor: pointer;
      transition: all 0.2s ease;

      .link-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;

        &--blue { background: #EFF6FF; color: #2563EB; }
        &--purple { background: #F5F3FF; color: #7C3AED; }
        &--green { background: #ECFDF5; color: #059669; }
      }

      .link-text {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .link-title {
          font-size: 14px;
          font-weight: 700;
          color: #1E293B;
        }

        .link-desc {
          font-size: 11px;
          color: #94A3B8;
        }
      }

      &:hover {
        background: #F8FAFC;
        border-color: #BFDBFE;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
      }
    }
  }
}
</style>

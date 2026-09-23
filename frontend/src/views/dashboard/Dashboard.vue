<template>
  <div class="dashboard-page-container">
    <!-- 1. 顶部 Hero Banner（保留原产品首页主视觉设计背景图，精准对齐核心交互） -->
    <DashboardHero
      @primary="router.push('/ai/marketplace')"
      @video="handleWatchVideo"
    />

    <!-- 2. 四大核心 KPI 数据看板（直连后端统计真实数据） -->
    <div class="kpi-cards-grid">
      <div
        v-for="kpi in kpiCards"
        :key="kpi.id"
        class="kpi-card"
        :class="`kpi-card--${kpi.theme}`"
        @click="router.push(kpi.route)"
      >
        <div class="kpi-card-content">
          <div class="kpi-header-row">
            <span class="kpi-label">{{ kpi.label }}</span>
            <div class="kpi-icon-badge">
              <el-icon><component :is="kpi.icon" /></el-icon>
            </div>
          </div>
          <div class="kpi-value-row">
            <span class="kpi-value">{{ kpi.value }}</span>
            <span v-if="kpi.unit" class="kpi-unit">{{ kpi.unit }}</span>
          </div>
          <div class="kpi-footer-row">
            <span class="kpi-sublabel" :title="kpi.sublabel">{{ kpi.sublabel }}</span>
            <span class="kpi-trend-tag" :class="{ 'kpi-trend-tag--alert': !kpi.trendUp }">
              {{ kpi.trend }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 中部：常用功能与待办协同 -->
    <div class="dashboard-middle-section">
      <!-- 常用教学生产力功能 -->
      <div class="common-functions-card">
        <div class="section-title-row">
          <div class="title-left">
            <h2 class="section-title">智能教学生产力中心</h2>
            <span class="section-subtitle">基于大模型与知识图谱的开箱即用教学工具</span>
          </div>
          <el-button text size="small" class="explore-link" @click="router.push('/ai/marketplace')">
            进入 AI 工具广场 &gt;
          </el-button>
        </div>

        <div class="functions-grid">
          <div
            v-for="fn in commonFunctions"
            :key="fn.title"
            class="function-item-card"
            @click="router.push(fn.route)"
          >
            <ColorIcon
              :icon="fn.icon"
              :theme="fn.theme"
              size="lg"
              rounded="xl"
            />
            <span class="function-title">{{ fn.title }}</span>
            <span v-if="fn.desc" class="function-desc" :title="fn.desc">{{ fn.desc }}</span>
          </div>
        </div>
      </div>

      <!-- 智能待办与任务中枢 -->
      <div class="todo-card">
        <div class="section-title-row">
          <div class="title-left">
            <h2 class="section-title">我的待办事项</h2>
            <span v-if="unreadNoticeCount > 0" class="todo-badge">{{ unreadNoticeCount }} 未读</span>
          </div>
          <div class="title-actions">
            <el-button text size="small" class="view-all-link" @click="fetchSummary">
              刷新
            </el-button>
            <span class="view-all-link" @click="router.push('/dashboard/todo')">
              全部 &gt;
            </span>
          </div>
        </div>

        <div class="todo-list" v-loading="loading">
          <template v-if="todoItems.length > 0">
            <div
              v-for="item in todoItems"
              :key="item.id"
              class="todo-item"
              :class="{ 'todo-item--urgent': item.urgent }"
              @click="handleTodoClick(item)"
            >
              <div class="todo-left">
                <span class="status-dot" :style="{ backgroundColor: item.dotColor }"></span>
                <span class="todo-title">{{ item.title }}</span>
              </div>
              <span class="todo-count" :class="{ 'todo-count--urgent': item.urgent }">
                {{ item.count }}
              </span>
            </div>
          </template>

          <div v-else class="todo-empty">
            <el-icon class="empty-icon"><CircleCheck /></el-icon>
            <span class="empty-text">待办事项均已处理完毕</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 底部三列：数据走势、学情掌握度画像、最新动态 -->
    <div class="dashboard-bottom-grid">
      <!-- 教学与 AI 赋能互动趋势 -->
      <div class="grid-card">
        <div class="card-header-row">
          <div class="header-left">
            <h3 class="card-title">教学与 AI 赋能趋势</h3>
            <span class="card-tip">交互频次与活跃走向</span>
          </div>
          <el-radio-group v-model="selectedDataPeriod" size="small" class="period-toggle">
            <el-radio-button label="WEEK">本周</el-radio-button>
            <el-radio-button label="MONTH">近30天</el-radio-button>
          </el-radio-group>
        </div>

        <div class="metric-highlight-row">
          <div class="metric-block">
            <span class="metric-label">教学助教交互调用</span>
            <span class="metric-val">{{ summary?.aiConversationCount ?? 0 }} 次</span>
          </div>
          <div class="metric-block">
            <span class="metric-label">活跃走势</span>
            <span class="metric-tag">平稳运行中</span>
          </div>
        </div>

        <div v-loading="trendLoading" ref="teachingLineChartRef" class="chart-container"></div>
      </div>

      <!-- 学生学情与能力分布 -->
      <div class="grid-card">
        <div class="card-header-row">
          <div class="header-left">
            <h3 class="card-title">学生学情综合分布</h3>
            <span class="card-tip">知识点掌握与素养层级</span>
          </div>
          <span class="view-all-link" @click="router.push('/analytics/learning')">
            详情 &gt;
          </span>
        </div>

        <div class="donut-chart-wrapper">
          <div ref="abilityPieChartRef" class="chart-container-donut"></div>
          <div class="legend-list">
            <div
              v-for="item in abilityLegend"
              :key="item.label"
              class="legend-item"
            >
              <span class="legend-dot" :style="{ backgroundColor: item.color }"></span>
              <span class="legend-name">{{ item.label }}</span>
              <span class="legend-percent">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时业务动态与通知 -->
      <div class="grid-card">
        <div class="card-header-row">
          <div class="header-left">
            <h3 class="card-title">实时动态与通知</h3>
            <span class="card-tip">系统与教学活动日志</span>
          </div>
          <span class="view-all-link" @click="router.push('/dashboard/recent')">
            更多 &gt;
          </span>
        </div>

        <div class="news-list" v-loading="loading">
          <div
            v-for="news in latestNews"
            :key="news.id"
            class="news-item"
            @click="router.push(news.route || '/dashboard/recent')"
          >
            <div class="news-left">
              <span class="news-category-badge" :style="{ backgroundColor: news.tagColor + '18', color: news.tagColor }">
                {{ news.category || '动态' }}
              </span>
              <span class="news-title" :title="news.title">{{ news.title }}</span>
            </div>
            <span class="news-time">{{ news.time }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DashboardHero from '@/components/dashboard/DashboardHero.vue';
import ColorIcon from '@/components/common/ColorIcon.vue';
import { CircleCheck } from '@element-plus/icons-vue';
import { useDashboard } from '@/composables/dashboard/useDashboard';

const {
  router,
  summary,
  loading,
  trendLoading,
  kpiCards,
  commonFunctions,
  todoItems,
  selectedDataPeriod,
  abilityLegend,
  latestNews,
  unreadNoticeCount,
  teachingLineChartRef,
  abilityPieChartRef,
  fetchSummary,
  handleTodoClick,
  handleWatchVideo
} = useDashboard({ enableCharts: true });
</script>

<style scoped lang="scss">
.dashboard-page-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 2. 四大 KPI 指标卡 */
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .kpi-card {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 18px 20px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      opacity: 0;
      transition: opacity 0.25s ease;
    }

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 24px rgba(30, 80, 150, 0.1);

      &::before {
        opacity: 1;
      }
    }

    &--blue {
      &::before { background: linear-gradient(90deg, #2563EB, #60A5FA); }
      .kpi-icon-badge { background: #EFF6FF; color: #2563EB; }
    }

    &--emerald {
      &::before { background: linear-gradient(90deg, #059669, #34D399); }
      .kpi-icon-badge { background: #ECFDF5; color: #059669; }
    }

    &--purple {
      &::before { background: linear-gradient(90deg, #7C3AED, #A78BFA); }
      .kpi-icon-badge { background: #F5F3FF; color: #7C3AED; }
    }

    &--amber {
      &::before { background: linear-gradient(90deg, #D97706, #FBBF24); }
      .kpi-icon-badge { background: #FFFBEB; color: #D97706; }
    }

    .kpi-card-content {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .kpi-header-row {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .kpi-label {
          font-size: 13px;
          font-weight: 600;
          color: #64748B;
        }

        .kpi-icon-badge {
          width: 32px;
          height: 32px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;
        }
      }

      .kpi-value-row {
        display: flex;
        align-items: baseline;
        gap: 6px;

        .kpi-value {
          font-size: 26px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.5px;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }

        .kpi-unit {
          font-size: 13px;
          color: #64748B;
          font-weight: 500;
        }
      }

      .kpi-footer-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        font-size: 12px;

        .kpi-sublabel {
          color: #94A3B8;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          flex: 1;
        }

        .kpi-trend-tag {
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 6px;
          background: #F1F5F9;
          color: #475569;
          font-weight: 500;
          white-space: nowrap;

          &--alert {
            background: #FEF2F2;
            color: #DC2626;
            font-weight: 600;
          }
        }
      }
    }
  }
}

/* 3. 中部功能与待办协同 */
.dashboard-middle-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: stretch;

  @media (max-width: 1200px) {
    grid-template-columns: minmax(0, 1fr) 320px;
  }

  @media (max-width: 992px) {
    grid-template-columns: 1fr;
  }
}

.common-functions-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
  border: 1px solid #EBF1F7;
  min-width: 0;
  overflow: hidden;

  .section-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .title-left {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .section-title {
        font-size: 16px;
        font-weight: 700;
        color: #1E293B;
        margin: 0;
      }

      .section-subtitle {
        font-size: 12px;
        color: #94A3B8;
      }
    }

    .explore-link {
      font-size: 12px;
      color: #2563EB;
      padding: 0;

      &:hover {
        color: #1D4ED8;
      }
    }
  }

  .functions-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .function-item-card {
      background: #F8FAFC;
      border: 1px solid #EDF2F7;
      border-radius: 14px;
      padding: 18px 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      text-align: center;
      cursor: pointer;
      min-width: 0;
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

      .function-title {
        font-size: 13px;
        font-weight: 700;
        color: #334155;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 100%;
        transition: color 0.2s ease;
      }

      .function-desc {
        font-size: 11px;
        color: #94A3B8;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 100%;
      }

      &:hover {
        background: #EFF6FF;
        border-color: #BFDBFE;
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.1);

        .function-title {
          color: #1D4ED8;
        }
      }
    }
  }
}

.todo-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
  border: 1px solid #EBF1F7;
  display: flex;
  flex-direction: column;
  min-width: 0;
  width: 100%;

  .section-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    flex-shrink: 0;
    white-space: nowrap;

    .title-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .section-title {
        font-size: 16px;
        font-weight: 700;
        color: #1E293B;
        margin: 0;
      }

      .todo-badge {
        font-size: 11px;
        background: #FEE2E2;
        color: #DC2626;
        padding: 2px 7px;
        border-radius: 999px;
        font-weight: 600;
      }
    }

    .title-actions {
      display: flex;
      align-items: center;
      gap: 10px;

      .view-all-link {
        font-size: 12px;
        color: #64748B;
        cursor: pointer;
        transition: color 0.2s ease;

        &:hover {
          color: #1D4ED8;
        }
      }
    }
  }

  .todo-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;

    .todo-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 11px 12px;
      border-radius: 10px;
      background: #F8FAFC;
      border: 1px solid transparent;
      cursor: pointer;
      transition: all 0.2s ease;
      min-width: 0;

      &:hover {
        background-color: #F1F5F9;
        border-color: #E2E8F0;
      }

      &--urgent {
        background-color: #FEF2F2;
        border-color: #FEE2E2;

        &:hover {
          background-color: #FEE2E2;
        }
      }

      .todo-left {
        display: flex;
        align-items: center;
        gap: 10px;
        min-width: 0;
        flex: 1;

        .status-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          flex-shrink: 0;
        }

        .todo-title {
          font-size: 13px;
          font-weight: 600;
          color: #334155;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .todo-count {
        font-size: 12px;
        font-weight: 600;
        color: #64748B;
        white-space: nowrap;
        flex-shrink: 0;
        margin-left: 8px;

        &--urgent {
          color: #DC2626;
        }
      }
    }

    .todo-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      padding: 36px 0;
      color: #10B981;

      .empty-icon {
        font-size: 32px;
      }

      .empty-text {
        font-size: 13px;
        color: #64748B;
      }
    }
  }
}

/* 4. 底部三列看板 */
.dashboard-bottom-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;

  @media (max-width: 1100px) {
    grid-template-columns: 1fr;
  }

  .grid-card {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 22px 24px;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
    border: 1px solid #EBF1F7;
    display: flex;
    flex-direction: column;
    min-height: 290px;

    .card-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .header-left {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .card-title {
          font-size: 15px;
          font-weight: 700;
          color: #1E293B;
          margin: 0;
        }

        .card-tip {
          font-size: 11px;
          color: #94A3B8;
        }
      }

      .view-all-link {
        font-size: 12px;
        color: #64748B;
        cursor: pointer;
        transition: color 0.2s ease;

        &:hover {
          color: #1D4ED8;
        }
      }
    }

    .metric-highlight-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 12px;
      background: #F8FAFC;
      border-radius: 8px;
      margin-bottom: 6px;

      .metric-block {
        display: flex;
        align-items: baseline;
        gap: 6px;

        .metric-label {
          font-size: 12px;
          color: #64748B;
        }

        .metric-val {
          font-size: 14px;
          font-weight: 700;
          color: #2563EB;
        }

        .metric-tag {
          font-size: 11px;
          color: #059669;
          font-weight: 600;
        }
      }
    }

    .chart-container {
      width: 100%;
      height: 180px;
      flex: 1;
    }

    .donut-chart-wrapper {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex: 1;

      .chart-container-donut {
        width: 150px;
        height: 180px;
      }

      .legend-list {
        display: flex;
        flex-direction: column;
        gap: 10px;
        flex: 1;
        padding-left: 8px;

        .legend-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 12px;

          .legend-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            margin-right: 6px;
          }

          .legend-name {
            color: #475569;
            flex: 1;
          }

          .legend-percent {
            font-weight: 700;
            color: #1E293B;
          }
        }
      }
    }

    .news-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      flex: 1;
      margin-top: 6px;

      .news-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 12px;
        padding: 8px 10px;
        border-radius: 8px;
        cursor: pointer;
        transition: background 0.2s ease;

        &:hover {
          background: #F8FAFC;
        }

        .news-left {
          display: flex;
          align-items: center;
          gap: 8px;
          overflow: hidden;
          flex: 1;

          .news-category-badge {
            font-size: 10px;
            font-weight: 600;
            padding: 2px 6px;
            border-radius: 4px;
            white-space: nowrap;
          }

          .news-title {
            color: #334155;
            font-weight: 500;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }

        .news-time {
          color: #94A3B8;
          font-size: 11px;
          flex-shrink: 0;
          margin-left: 8px;
        }
      }
    }
  }
}
</style>

<template>
  <div class="dashboard-page-container">
    <DashboardHero
      @primary="router.push('/ai/marketplace')"
      @video="handleWatchVideo"
    />

    <div class="dashboard-middle-section">
      <div class="common-functions-card">
        <div class="section-title-row">
          <h2 class="section-title">常用功能</h2>
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
          </div>
        </div>
      </div>

      <div class="todo-card">
        <div class="section-title-row">
          <h2 class="section-title">我的待办</h2>
          <span class="view-all-link" @click="router.push('/question/submissions')">
            查看全部 &gt;
          </span>
        </div>

        <div class="todo-list">
          <div
            v-for="item in todoItems"
            :key="item.id"
            class="todo-item"
            @click="handleTodoClick(item)"
          >
            <div class="todo-left">
              <span class="status-dot" :style="{ backgroundColor: item.dotColor }"></span>
              <span class="todo-title">{{ item.title }}</span>
            </div>
            <span class="todo-count">{{ item.count }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-bottom-grid">
      <div class="grid-card">
        <div class="card-header-row">
          <h3 class="card-title">教学数据概览</h3>
          <el-select v-model="selectedDataPeriod" size="small" class="period-select" style="width: 86px">
            <el-option label="本周" value="WEEK" />
            <el-option label="本月" value="MONTH" />
            <el-option label="本学期" value="TERM" />
          </el-select>
        </div>

        <div class="metric-highlight-row">
          <span class="metric-label">课堂活跃度</span>
          <span class="metric-val">78%</span>
        </div>

        <div ref="teachingLineChartRef" class="chart-container"></div>
      </div>

      <div class="grid-card">
        <div class="card-header-row">
          <h3 class="card-title">学生能力分布</h3>
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

      <div class="grid-card">
        <div class="card-header-row">
          <h3 class="card-title">最新动态</h3>
          <span class="view-all-link" @click="router.push('/system/audit')">
            更多 &gt;
          </span>
        </div>

        <div class="news-list">
          <div
            v-for="news in latestNews"
            :key="news.id"
            class="news-item"
          >
            <div class="news-left">
              <span class="news-bullet-icon" :style="{ backgroundColor: news.tagColor }"></span>
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
import { useDashboard } from '@/composables/dashboard/useDashboard';

const {
  router,
  commonFunctions,
  todoItems,
  selectedDataPeriod,
  abilityLegend,
  latestNews,
  teachingLineChartRef,
  abilityPieChartRef,
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

.dashboard-middle-section {
  display: grid;
  grid-template-columns: 7fr 3fr;
  gap: 20px;
  margin-top: -6px;

  @media (max-width: 1100px) {
    grid-template-columns: 1fr;
  }
}

.common-functions-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
  border: 1px solid #EBF1F7;

  .section-title-row {
    margin-bottom: 20px;

    .section-title {
      font-size: 16px;
      font-weight: 700;
      color: #1E293B;
      margin: 0;
    }
  }

  .functions-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .function-item-card {
      background: #F8FAFC;
      border: 1px solid #F1F5F9;
      border-radius: 12px;
      padding: 18px 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      cursor: pointer;
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

      .function-title {
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        transition: color 0.2s ease;
      }

      &:hover {
        background: #EFF6FF;
        border-color: #BFDBFE;
        transform: translateY(-3px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12);

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

  .section-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .section-title {
      font-size: 16px;
      font-weight: 700;
      color: #1E293B;
      margin: 0;
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

  .todo-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;

    .todo-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: #F8FAFC;
      }

      .todo-left {
        display: flex;
        align-items: center;
        gap: 10px;

        .status-dot {
          width: 7px;
          height: 7px;
          border-radius: 50%;
          flex-shrink: 0;
        }

        .todo-title {
          font-size: 13px;
          font-weight: 500;
          color: #334155;
        }
      }

      .todo-count {
        font-size: 12px;
        font-weight: 600;
        color: #64748B;
      }
    }
  }
}

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
    min-height: 270px;

    .card-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .card-title {
        font-size: 15px;
        font-weight: 700;
        color: #1E293B;
        margin: 0;
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
      align-items: baseline;
      gap: 8px;
      margin-bottom: 4px;

      .metric-label {
        font-size: 12px;
        color: #64748B;
      }

      .metric-val {
        font-size: 15px;
        font-weight: 700;
        color: #10B981;
      }
    }

    .chart-container {
      width: 100%;
      height: 170px;
      flex-shrink: 0;
    }

    .donut-chart-wrapper {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex: 1;

      .chart-container-donut {
        width: 150px;
        height: 170px;
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
            font-weight: 600;
            color: #1E293B;
          }
        }
      }
    }

    .news-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      flex: 1;
      margin-top: 6px;

      .news-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 12px;

        .news-left {
          display: flex;
          align-items: center;
          gap: 8px;
          overflow: hidden;

          .news-bullet-icon {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            flex-shrink: 0;
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

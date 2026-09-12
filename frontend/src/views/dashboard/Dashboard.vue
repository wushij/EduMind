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
import { ref, onMounted, onUnmounted, nextTick, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import DashboardHero from '@/components/dashboard/DashboardHero.vue';
import ColorIcon, { type IconTheme } from '@/components/common/ColorIcon.vue';
import {
  EditPen,
  Tickets,
  CircleCheck,
  TrendCharts,
  FolderOpened,
  ChatDotRound,
  Histogram,
  Connection
} from '@element-plus/icons-vue';
import * as echarts from 'echarts';

const router = useRouter();

interface CommonFunctionItem {
  title: string;
  icon: any;
  theme: IconTheme;
  route: string;
}

const commonFunctions: CommonFunctionItem[] = [
  { title: 'AI 备课', icon: EditPen, theme: 'blue', route: '/ai/lesson' },
  { title: '智能出题', icon: Tickets, theme: 'cyan', route: '/ai/question/generate' },
  { title: '作业批改', icon: CircleCheck, theme: 'emerald', route: '/ai/grading' },
  { title: '学情分析', icon: TrendCharts, theme: 'amber', route: '/analytics/learning' },
  { title: '课程资源', icon: FolderOpened, theme: 'rose', route: '/course' },
  { title: '课堂互动', icon: ChatDotRound, theme: 'purple', route: '/course/ai-assistant' },
  { title: '教学评价', icon: Histogram, theme: 'indigo', route: '/analytics' },
  { title: '家校沟通', icon: Connection, theme: 'teal', route: '/dashboard' }
];

interface TodoItem {
  id: number;
  title: string;
  count: string;
  dotColor: string;
  route: string;
}

const todoItems: TodoItem[] = [
  { id: 1, title: '待批改作业', count: '12 份', dotColor: '#3B82F6', route: '/question/submissions' },
  { id: 2, title: '待审核课程资源', count: '3 个', dotColor: '#10B981', route: '/course' },
  { id: 3, title: '学生问题咨询', count: '8 条', dotColor: '#EF4444', route: '/course/ai-assistant' },
  { id: 4, title: '课堂反馈待处理', count: '5 条', dotColor: '#F59E0B', route: '/analytics' },
  { id: 5, title: '系统通知', count: '2 条', dotColor: '#3B82F6', route: '/dashboard' }
];

function handleTodoClick(item: TodoItem) {
  router.push(item.route);
}

function handleWatchVideo() {
  // 预留：产品介绍视频
}

const selectedDataPeriod = ref('WEEK');

const abilityLegend = [
  { label: '优秀', percent: 24, color: '#2563EB' },
  { label: '良好', percent: 36, color: '#06B6D4' },
  { label: '中等', percent: 28, color: '#F59E0B' },
  { label: '待提升', percent: 12, color: '#F97316' }
];

const latestNews = [
  { id: 1, title: '三年级数学单元测试', time: '10:24', tagColor: '#3B82F6' },
  { id: 2, title: 'AI 生成的教案已完成', time: '09:18', tagColor: '#8B5CF6' },
  { id: 3, title: '学生提交了作业', time: '昨天', tagColor: '#10B981' },
  { id: 4, title: '新课程资源已上线', time: '昨天', tagColor: '#F59E0B' },
  { id: 5, title: '系统版本更新', time: '05-20', tagColor: '#64748B' }
];

const teachingLineChartRef = ref<HTMLElement | null>(null);
const abilityPieChartRef = ref<HTMLElement | null>(null);

const lineChartInstance = shallowRef<echarts.ECharts | null>(null);
const pieChartInstance = shallowRef<echarts.ECharts | null>(null);

function initLineChart() {
  if (!teachingLineChartRef.value) return;
  if (lineChartInstance.value) {
    lineChartInstance.value.dispose();
  }

  const chart = echarts.init(teachingLineChartRef.value);
  lineChartInstance.value = chart;

  const option: echarts.EChartsOption = {
    grid: {
      left: '2%',
      right: '4%',
      top: '12%',
      bottom: '8%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B',
        fontSize: 12
      },
      formatter: '{b}: 活跃度 {c}%'
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: {
        lineStyle: { color: '#CBD5E1' }
      },
      axisTick: { show: false },
      axisLabel: {
        color: '#64748B',
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      interval: 25,
      splitLine: {
        lineStyle: {
          color: '#F1F5F9',
          type: 'dashed'
        }
      },
      axisLabel: {
        show: false
      }
    },
    series: [
      {
        name: '课堂活跃度',
        type: 'line',
        data: [45, 52, 60, 56, 70, 75, 78],
        smooth: true,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: {
          color: '#2563EB',
          borderColor: '#FFFFFF',
          borderWidth: 2
        },
        lineStyle: {
          width: 3,
          color: '#2563EB'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.35)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      }
    ]
  };

  chart.setOption(option);
}

function initPieChart() {
  if (!abilityPieChartRef.value) return;
  if (pieChartInstance.value) {
    pieChartInstance.value.dispose();
  }

  const chart = echarts.init(abilityPieChartRef.value);
  pieChartInstance.value = chart;

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B',
        fontSize: 12
      },
      formatter: '{b}: {c}%'
    },
    series: [
      {
        name: '学生能力分布',
        type: 'pie',
        radius: ['52%', '78%'],
        center: ['48%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 6
        },
        data: [
          { value: 24, name: '优秀', itemStyle: { color: '#2563EB' } },
          { value: 36, name: '良好', itemStyle: { color: '#06B6D4' } },
          { value: 28, name: '中等', itemStyle: { color: '#F59E0B' } },
          { value: 12, name: '待提升', itemStyle: { color: '#F97316' } }
        ]
      }
    ]
  };

  chart.setOption(option);
}

function handleResize() {
  lineChartInstance.value?.resize();
  pieChartInstance.value?.resize();
}

onMounted(() => {
  nextTick(() => {
    initLineChart();
    initPieChart();
    window.addEventListener('resize', handleResize);
  });
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  lineChartInstance.value?.dispose();
  pieChartInstance.value?.dispose();
});
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

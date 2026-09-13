<template>
  <div class="learning-report-page" v-loading="loading">
    <PageHeroBanner
      title="个人学情深度分析与诊断报告"
      subtitle="多维评估个人知识掌握曲线、作业作答质量与认知提升轨迹，智能输出个性化学期突破建议"
      background-variant="learning"
    >
      <template #extra>
        <div class="report-filters">
          <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="loadReport">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-radio-group v-model="timeRange" @change="loadReport">
            <el-radio-button label="7d">近 7 天</el-radio-button>
            <el-radio-button label="30d">近 30 天</el-radio-button>
            <el-radio-button label="term">本学期</el-radio-button>
          </el-radio-group>
        </div>
      </template>
    </PageHeroBanner>

    <div class="report-main-grid">
      <!-- 4 个核心学情指标卡片 -->
      <div class="metrics-row">
        <div class="metric-card">
          <div class="metric-icon metric-icon-blue">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-value">{{ reportData.studyMinutes }} <small>分钟</small></span>
            <span class="metric-label">累计专注学时</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon metric-icon-green">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-value">{{ reportData.masteryRate }}%</span>
            <span class="metric-label">考点综合掌握率</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon metric-icon-purple">
            <el-icon><DocumentChecked /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-value">{{ reportData.assignmentAccuracy }}%</span>
            <span class="metric-label">作业平均正确率</span>
          </div>
        </div>

        <div class="metric-card">
          <div class="metric-icon metric-icon-orange">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="metric-info">
            <span class="metric-value">{{ reportData.aiInteractions }} <small>次</small></span>
            <span class="metric-label">AI 提问与答疑</span>
          </div>
        </div>
      </div>

      <!-- 认知雷达与薄弱考点剖析 -->
      <div class="two-col-grid">
        <el-card shadow="never" class="radar-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">五维认知能力雷达</span>
              <el-tag size="small" type="success" effect="plain">优于班级 78% 同学</el-tag>
            </div>
          </template>
          <div ref="radarChartRef" class="chart-container" />
        </el-card>

        <el-card shadow="never" class="trend-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">学习时长与成绩演进趋势</span>
              <el-tag size="small" type="primary" effect="plain">持续上升曲线</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </div>

      <!-- AI 导师个性化诊断与攻坚建议 -->
      <el-card shadow="never" class="advice-card">
        <template #header>
          <div class="card-header-row">
            <div class="advice-title">
              <el-icon class="ai-sparkle"><Cpu /></el-icon>
              <span>AI 教学导师学期诊断建议书</span>
            </div>
            <span class="report-code">报告编号：EM-RPT-2026-{{ courseId }}</span>
          </div>
        </template>
        <div class="advice-content">
          <p class="summary-paragraph">
            <strong>总体评价：</strong>{{ reportData.adviceSummary }}
          </p>
          <div class="action-suggestions">
            <div
              v-for="(item, idx) in reportData.actionItems"
              :key="idx"
              class="suggestion-item"
            >
              <div class="item-badge">{{ idx + 1 }}</div>
              <div class="item-text">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-desc">{{ item.desc }}</span>
              </div>
              <el-button size="small" type="primary" plain @click="handleAction(item.action)">
                {{ item.btnText }}
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import {
  Timer,
  DataLine,
  DocumentChecked,
  ChatDotRound,
  Cpu
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

const router = useRouter();
const { courseOptions, courseId } = useTeacherCourses(102);

const loading = ref(false);
const timeRange = ref('30d');

const radarChartRef = ref<HTMLDivElement | null>(null);
const trendChartRef = ref<HTMLDivElement | null>(null);
let radarChart: echarts.ECharts | null = null;
let trendChart: echarts.ECharts | null = null;

const reportData = ref({
  studyMinutes: 480,
  masteryRate: 84.5,
  assignmentAccuracy: 89.2,
  aiInteractions: 36,
  adviceSummary: '近期在核心概念定理的识记与常规题型演算上表现优异，各章节作答保持高度平稳。在涉及多定理交汇的逆向推导与极端边界条件判定上稍有疏漏，建议加强高阶变式练习。',
  actionItems: [
    {
      title: '巩固“洛必达法则与未定式极限”薄弱点',
      desc: '历史错误频次最高考点，建议复习前驱函数连续性定义，完成 3 道靶向变式题。',
      btnText: '去错题攻坚',
      action: 'WRONG'
    },
    {
      title: '开启“积分综合变换”认知冲刺',
      desc: '当前该章节处于良好掌握区间，建议挑战高阶自适应题库冲刺完全精熟。',
      btnText: '开始练习',
      action: 'PRACTICE'
    }
  ]
});

const initCharts = () => {
  if (radarChartRef.value) {
    if (radarChart) radarChart.dispose();
    radarChart = echarts.init(radarChartRef.value);
    radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: [
          { name: '概念识记', max: 100 },
          { name: '定理推导', max: 100 },
          { name: '综合计算', max: 100 },
          { name: '逆向建模', max: 100 },
          { name: '答题规范', max: 100 }
        ],
        radius: '65%',
        axisName: { color: '#475569', fontSize: 12 }
      },
      series: [
        {
          type: 'radar',
          data: [
            {
              value: [92, 78, 88, 70, 95],
              name: '个人掌握水平',
              areaStyle: { color: 'rgba(22, 119, 255, 0.25)' },
              itemStyle: { color: '#1677FF' }
            },
            {
              value: [82, 72, 75, 62, 80],
              name: '班级平均基准',
              areaStyle: { color: 'rgba(82, 196, 26, 0.15)' },
              itemStyle: { color: '#52C41A' }
            }
          ]
        }
      ]
    });
  }

  if (trendChartRef.value) {
    if (trendChart) trendChart.dispose();
    trendChart = echarts.init(trendChartRef.value);
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['学情掌握度(%)', '专注学时(分钟)'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周']
      },
      yAxis: [
        { type: 'value', min: 50, max: 100, name: '掌握度' },
        { type: 'value', name: '分钟', min: 0, max: 150 }
      ],
      series: [
        {
          name: '学情掌握度(%)',
          type: 'line',
          smooth: true,
          data: [68, 72, 75, 80, 82, 86],
          itemStyle: { color: '#1677FF' },
          lineStyle: { width: 3 }
        },
        {
          name: '专注学时(分钟)',
          type: 'bar',
          yAxisIndex: 1,
          data: [60, 75, 80, 110, 90, 125],
          itemStyle: { color: '#93C5FD', borderRadius: [4, 4, 0, 0] },
          barWidth: 16
        }
      ]
    });
  }
};

const loadReport = async () => {
  loading.value = true;
  setTimeout(() => {
    loading.value = false;
    nextTick(() => {
      initCharts();
    });
  }, 300);
};

const handleAction = (action: string) => {
  if (action === 'WRONG') {
    router.push({ path: '/learning/wrong-questions', query: { courseId: courseId.value } });
  } else if (action === 'PRACTICE') {
    router.push({ path: '/learning/ai-practice', query: { courseId: courseId.value } });
  }
};

onMounted(() => {
  loadReport();
  window.addEventListener('resize', () => {
    radarChart?.resize();
    trendChart?.resize();
  });
});
</script>

<style scoped lang="scss">
.learning-report-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;

  .report-filters {
    display: flex;
    gap: 16px;
    align-items: center;
    margin-top: 12px;
    flex-wrap: wrap;
  }

  .report-main-grid {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .metrics-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;

      @media (max-width: 992px) {
        grid-template-columns: repeat(2, 1fr);
      }
      @media (max-width: 576px) {
        grid-template-columns: 1fr;
      }

      .metric-card {
        background: #FFFFFF;
        border-radius: 14px;
        padding: 16px 20px;
        border: 1px solid #E2E8F0;
        display: flex;
        align-items: center;
        gap: 16px;
        box-shadow: 0 2px 10px rgba(30, 80, 150, 0.04);
        transition: all 0.25s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 18px rgba(30, 80, 150, 0.08);
        }

        .metric-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;

          &.metric-icon-blue {
            background: #EFF6FF;
            color: #1677FF;
          }
          &.metric-icon-green {
            background: #F0FDF4;
            color: #52C41A;
          }
          &.metric-icon-purple {
            background: #FAF5FF;
            color: #722ED1;
          }
          &.metric-icon-orange {
            background: #FFF7ED;
            color: #FA8C16;
          }
        }

        .metric-info {
          display: flex;
          flex-direction: column;

          .metric-value {
            font-size: 24px;
            font-weight: 800;
            color: #0F172A;

            small {
              font-size: 13px;
              font-weight: 500;
              color: #64748B;
            }
          }

          .metric-label {
            font-size: 12px;
            color: #64748B;
            margin-top: 2px;
          }
        }
      }
    }

    .two-col-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;

      @media (max-width: 992px) {
        grid-template-columns: 1fr;
      }

      .radar-card,
      .trend-card {
        border-radius: 16px;
        border: 1px solid #E2E8F0;

        .card-header-row {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .card-title {
            font-size: 15px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .chart-container {
          height: 320px;
          width: 100%;
        }
      }
    }

    .advice-card {
      border-radius: 16px;
      border: 1px solid #E2E8F0;

      .card-header-row {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .advice-title {
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
          display: flex;
          align-items: center;
          gap: 8px;

          .ai-sparkle {
            color: #1677FF;
          }
        }

        .report-code {
          font-size: 12px;
          color: #94A3B8;
        }
      }

      .advice-content {
        .summary-paragraph {
          font-size: 14px;
          line-height: 1.7;
          color: #334155;
          margin: 0 0 16px 0;
          padding: 12px 16px;
          background: #F8FAFC;
          border-radius: 10px;
          border-left: 4px solid #1677FF;
        }

        .action-suggestions {
          display: flex;
          flex-direction: column;
          gap: 12px;

          .suggestion-item {
            display: flex;
            align-items: center;
            gap: 16px;
            padding: 12px 16px;
            border-radius: 10px;
            border: 1px solid #E2E8F0;
            background: #FFFFFF;
            transition: all 0.2s ease;

            &:hover {
              border-color: #93C5FD;
              background: #F8FAFC;
            }

            .item-badge {
              width: 26px;
              height: 26px;
              border-radius: 50%;
              background: #EFF6FF;
              color: #1677FF;
              display: flex;
              align-items: center;
              justify-content: center;
              font-weight: 700;
              font-size: 13px;
            }

            .item-text {
              flex: 1;
              display: flex;
              flex-direction: column;

              .item-title {
                font-size: 14px;
                font-weight: 600;
                color: #1E293B;
              }

              .item-desc {
                font-size: 12px;
                color: #64748B;
                margin-top: 2px;
              }
            }
          }
        }
      }
    }
  }
}
</style>

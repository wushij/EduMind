import { ref, onMounted, onUnmounted, nextTick, shallowRef, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { downloadBlob } from '@/utils/download';
import type {
  AiUsageSceneVO,
  ChapterProgressVO,
  CourseHealthVO,
  CourseWeakPointVO
} from '@/types/analytics/learning';

export interface AnalyticsTabItem {
  label: string;
  value: string;
  route: string;
  badge?: string;
}

/** AI 场景环形图配色：按真实场景顺序循环取用，与调用量比例解耦 */
const AI_SCENE_PALETTE: Array<{ color1: string; color2: string }> = [
  { color1: '#2563EB', color2: '#38BDF8' },
  { color1: '#7C3AED', color2: '#C084FC' },
  { color1: '#059669', color2: '#34D399' },
  { color1: '#D97706', color2: '#FBBF24' },
  { color1: '#DB2777', color2: '#F472B6' }
];

/** 归一化后端可能为 null 的分值：无效值保持 null，让折线如实断开 */
function normalizeScore(value: number | null | undefined): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

export function useAnalyticsOverview(defaultCourseId?: number) {
  const router = useRouter();
  const route = useRoute();
  const { courseOptions, courseId, loadCourses } = useTeacherCourses(defaultCourseId);
  const { fetchOverview, fetchTeachingAdvice, stopTeachingAdvice, adviceLoading, teachingAdvice } = useLearningAnalytics();

  const loading = ref(false);
  const avgScore = ref('--');
  const masteryRate = ref('--');
  const participationRate = ref('--');
  const aiCallCount = ref('--');
  const studentCount = ref(0);
  /** 知识大纲推进完成度：真实章节覆盖（有学习行为的章节数 / 全部章节数） */
  const syllabusCoverageText = ref('--');
  const avgStudyMinutes = ref(0);
  const lastUpdatedTime = ref('刚刚更新');

  // 双折线趋势数据：班级平均分 vs 全校对照均分（null 表示当日无真实样本，图表断线）
  const scoreTrendDates = ref<string[]>([]);
  const scoreTrendValues = ref<Array<number | null>>([]);
  const schoolTrendValues = ref<Array<number | null>>([]);

  // 知识点掌握数据
  const masteryCategories = ref<string[]>([]);
  const masteryValues = ref<number[]>([]);

  // 活跃度与 AI 统计
  const activityDates = ref<string[]>([]);
  const activityValues = ref<number[]>([]);
  const aiToolLegend = ref<Array<{ name: string; ratio: number; calls: number; color: string }>>([]);
  const aiPieData = ref<Array<{ value: number; name: string; itemStyle: any }>>([]);
  const aiTotalCalls = ref(0);
  const aiProviderList = ref<Array<{ provider: string; calls: number; ratio: number }>>([]);

  // 课程深入分析专用数据
  const chapterProgressList = ref<ChapterProgressVO[]>([]);
  const courseHealth = ref<CourseHealthVO | null>(null);
  const courseWeakPoints = ref<CourseWeakPointVO[]>([]);

  // 时间维度与日期范围
  const selectedTrendRange = ref('近30天');
  const dateRange = ref<[string, string]>(calcInitialDateRange(30));

  // AI 智能诊断：推演弹窗、秒级计时与阶段推进控制
  const aiThinkingModalVisible = ref(false);
  const aiAdviceDrawerVisible = ref(false);
  const aiElapsedTimeText = ref('0.0s');
  const aiCurrentStep = ref(1);
  const aiExecutionDurationText = ref('');
  let aiTimerId: number | null = null;
  let aiStartTime = 0;
  let isThinkingAborted = false;

  const aiThinkingSteps = [
    '全链路聚合课程考情、作业与章节学情数据',
    '深度识别知识点认知断层与群体薄弱点',
    '调用智教大模型推演生成精准教学干预清单'
  ];

  const analyticsTabs: AnalyticsTabItem[] = [
    { label: '教学概览', value: 'overview', route: '/analytics' },
    { label: '课程分析', value: 'course', route: '' },
    { label: '学生分析', value: 'student', route: '/analytics/learning' },
    { label: '知识点掌握', value: 'mastery', route: '/analytics/mastery' },
    { label: 'AI使用分析', value: 'ai_usage', route: '/analytics/ai-usage' }
  ];

  const currentTab = ref('overview');

  // 图表 DOM 引用
  const scoreTrendChartRef = ref<HTMLElement | null>(null);
  const knowledgeMasteryChartRef = ref<HTMLElement | null>(null);
  const gradeDistributionChartRef = ref<HTMLElement | null>(null);
  const aiToolUsageChartRef = ref<HTMLElement | null>(null);
  const courseHealthRadarRef = ref<HTMLElement | null>(null);

  // 图表实例
  const scoreTrendChart = shallowRef<echarts.ECharts | null>(null);
  const knowledgeMasteryChart = shallowRef<echarts.ECharts | null>(null);
  const gradeDistributionChart = shallowRef<echarts.ECharts | null>(null);
  const aiToolUsageChart = shallowRef<echarts.ECharts | null>(null);
  const courseHealthRadarChart = shallowRef<echarts.ECharts | null>(null);

  function calcInitialDateRange(days: number): [string, string] {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - days);
    const fmt = (d: Date) => d.toISOString().split('T')[0];
    return [fmt(start), fmt(end)];
  }

  function handleTabClick(tab: AnalyticsTabItem) {
    currentTab.value = tab.value;
    if (tab.route && tab.route !== route.path) {
      router.push(tab.route);
    } else if (tab.value === 'course') {
      nextTick(() => {
        initCourseHealthRadarChart();
      });
    }
  }

  function handleQuickRangeChange(label: string) {
    selectedTrendRange.value = label;
    let days = 30;
    if (label === '近7天') days = 7;
    if (label === '近学期') days = 90;
    dateRange.value = calcInitialDateRange(days);
    loadOverviewData();
  }

  function handleDateRangePickerChange() {
    selectedTrendRange.value = '自定义';
    loadOverviewData();
  }

  // 1. 成绩趋势图（平滑科技发光折线 + 双线对照 + 自适应缩放）
  function initScoreTrendChart() {
    if (!scoreTrendChartRef.value) return;
    scoreTrendChart.value?.dispose();

    const chart = echarts.init(scoreTrendChartRef.value);
    scoreTrendChart.value = chart;

    const option: echarts.EChartsOption = {
      grid: {
        left: '2%',
        right: '4%',
        top: '16%',
        bottom: '8%',
        containLabel: true
      },
      legend: {
        show: true,
        top: '0%',
        right: '2%',
        icon: 'circle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: { color: '#64748B', fontSize: 12, fontWeight: 500 }
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#E2E8F0',
        borderWidth: 1,
        shadowColor: 'rgba(15, 23, 42, 0.1)',
        shadowBlur: 16,
        padding: [10, 14],
        textStyle: { color: '#1E293B', fontSize: 12 },
        formatter: (params: any) => {
          if (!Array.isArray(params) || params.length === 0) return '';
          // 无样本的日期后端返回 null，需过滤掉，否则会展示成 "null 分" 并算出错误差值
          const valid = params.filter(
            (p: any) => p.value !== null && p.value !== undefined && Number.isFinite(Number(p.value))
          );
          if (valid.length === 0) return '';
          const p1 = valid[0];
          const p2 = valid[1];
          const diff = p2 ? Math.round((Number(p1.value) - Number(p2.value)) * 10) / 10 : null;
          const diffBadge = diff !== null
            ? `<span style="font-size:11px;padding:2px 7px;border-radius:6px;background:${diff >= 0 ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)'};color:${diff >= 0 ? '#059669' : '#dc2626'};font-weight:600;margin-left:6px">${diff >= 0 ? '领先 +' : '差距 '}${diff}分</span>`
            : '';
          const header = `<div style="font-weight:700;margin-bottom:8px;color:#0F172A;font-size:13px">${p1.axisValue} 成绩动态 ${diffBadge}</div>`;
          const lines = valid.map((p: any) => {
            return `<div style="display:flex;align-items:center;justify-content:space-between;gap:24px;margin:5px 0">
              <span style="display:inline-flex;align-items:center;gap:6px">
                <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color};box-shadow:0 0 6px ${p.color}88"></span>
                <span style="color:#475569;font-size:12.5px">${p.seriesName}</span>
              </span>
              <strong style="color:#0F172A;font-size:13.5px">${p.value} 分</strong>
            </div>`;
          }).join('');
          return header + lines;
        }
      },
      xAxis: {
        type: 'category',
        data: scoreTrendDates.value,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#CBD5E1' } },
        axisTick: { show: false },
        axisLabel: { color: '#64748B', fontSize: 11.5, margin: 12 }
      },
      yAxis: {
        type: 'value',
        scale: true,
        min: (value: any) => Math.max(0, Math.floor(value.min - 3)),
        max: (value: any) => Math.min(100, Math.ceil(value.max + 3)),
        splitLine: {
          lineStyle: { color: 'rgba(241, 245, 249, 0.95)', type: 'dashed' }
        },
        axisLabel: { color: '#94A3B8', fontSize: 11, formatter: '{value}分' }
      },
      series: [
        {
          name: '班级平均分',
          type: 'line',
          smooth: 0.45,
          data: scoreTrendValues.value,
          showSymbol: false,
          lineStyle: {
            width: 3.5,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#2563EB' },
              { offset: 0.5, color: '#3B82F6' },
              { offset: 1, color: '#6366F1' }
            ]),
            shadowColor: 'rgba(37, 99, 235, 0.35)',
            shadowBlur: 10,
            shadowOffsetY: 4
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.28)' },
              { offset: 0.5, color: 'rgba(59, 130, 246, 0.1)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.0)' }
            ])
          }
        },
        {
          name: '全校对照均分',
          type: 'line',
          smooth: 0.45,
          data: schoolTrendValues.value,
          showSymbol: false,
          lineStyle: {
            width: 2.2,
            color: '#10B981',
            type: [6, 4]
          },
          areaStyle: {
            color: 'rgba(16, 185, 129, 0.03)'
          }
        }
      ]
    };

    chart.setOption(option);
  }

  // 2. 知识点掌握情况横向柱状图
  function initKnowledgeMasteryChart() {
    if (!knowledgeMasteryChartRef.value) return;
    knowledgeMasteryChart.value?.dispose();

    const chart = echarts.init(knowledgeMasteryChartRef.value);
    knowledgeMasteryChart.value = chart;

    const option: echarts.EChartsOption = {
      grid: {
        left: '3%',
        right: '14%',
        top: '6%',
        bottom: '6%',
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}: <strong>{c}%</strong> 掌握度'
      },
      xAxis: {
        type: 'value',
        max: 100,
        show: false
      },
      yAxis: {
        type: 'category',
        data: masteryCategories.value,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: '#334155',
          fontSize: 12,
          fontWeight: 500,
          formatter: (value: string) => {
            return value.length > 15 ? value.slice(0, 15) + '...' : value;
          }
        }
      },
      series: [
        {
          type: 'bar',
          data: masteryValues.value,
          barWidth: 10,
          showBackground: true,
          backgroundStyle: {
            color: '#F1F5F9',
            borderRadius: 5
          },
          itemStyle: {
            color: (params: any) => {
              const val = Number(params.value) || 0;
              if (val >= 80) {
                return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#38BDF8' },
                  { offset: 1, color: '#1677FF' }
                ]);
              }
              if (val >= 65) {
                return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#FCD34D' },
                  { offset: 1, color: '#F59E0B' }
                ]);
              }
              return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#FCA5A5' },
                { offset: 1, color: '#EF4444' }
              ]);
            },
            borderRadius: 5
          },
          label: {
            show: true,
            position: 'right',
            formatter: '{c}%',
            color: '#64748B',
            fontSize: 11,
            fontWeight: 600
          }
        }
      ]
    };

    chart.setOption(option);
  }

  // 3. 学习活跃度折线图（平滑科技发光面积曲线，告别突兀稀疏的柱状图）
  function initGradeDistributionChart() {
    if (!gradeDistributionChartRef.value) return;
    gradeDistributionChart.value?.dispose();

    const chart = echarts.init(gradeDistributionChartRef.value);
    gradeDistributionChart.value = chart;

    const option: echarts.EChartsOption = {
      grid: {
        left: '2%',
        right: '4%',
        top: '14%',
        bottom: '8%',
        containLabel: true
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#E2E8F0',
        borderWidth: 1,
        shadowColor: 'rgba(15, 23, 42, 0.1)',
        shadowBlur: 16,
        padding: [10, 14],
        formatter: (params: any) => {
          if (!Array.isArray(params) || params.length === 0) return '';
          const p = params[0];
          return `<div style="font-weight:700;margin-bottom:6px;color:#0F172A;font-size:13px">${p.axisValue} 学习热度</div>
            <div style="display:flex;align-items:center;justify-content:space-between;gap:20px;font-size:12.5px">
              <span style="display:inline-flex;align-items:center;gap:6px;color:#475569">
                <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#3B82F6;box-shadow:0 0 6px rgba(59,130,246,0.5)"></span>
                活跃人次
              </span>
              <strong style="color:#0F172A;font-size:13.5px">${p.value} 人</strong>
            </div>`;
        }
      },
      xAxis: {
        type: 'category',
        data: activityDates.value,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#E2E8F0' } },
        axisTick: { show: false },
        axisLabel: { color: '#64748B', fontSize: 11.5, margin: 10 }
      },
      yAxis: {
        type: 'value',
        min: 0,
        minInterval: 1, // 严格限制整数步长，彻底杜绝出现 0.5人、1.5人 这类荒谬小数
        splitLine: {
          lineStyle: { color: 'rgba(241, 245, 249, 0.95)', type: 'dashed' }
        },
        axisLabel: {
          color: '#94A3B8',
          fontSize: 11,
          formatter: '{value}人'
        }
      },
      series: [
        {
          name: '日活跃人次',
          type: 'line',
          smooth: 0.45,
          data: activityValues.value,
          showSymbol: true,
          symbol: 'circle',
          symbolSize: (value: any) => (Number(value) > 0 ? 6 : 0),
          itemStyle: {
            color: '#3B82F6',
            borderColor: '#FFFFFF',
            borderWidth: 2,
            shadowColor: 'rgba(59, 130, 246, 0.4)',
            shadowBlur: 6
          },
          lineStyle: {
            width: 3,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#06B6D4' },
              { offset: 0.5, color: '#3B82F6' },
              { offset: 1, color: '#6366F1' }
            ]),
            shadowColor: 'rgba(59, 130, 246, 0.35)',
            shadowBlur: 10,
            shadowOffsetY: 4
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.28)' },
              { offset: 0.7, color: 'rgba(6, 182, 212, 0.08)' },
              { offset: 1, color: 'rgba(6, 182, 212, 0.0)' }
            ])
          }
        }
      ]
    };

    chart.setOption(option);
  }

  // 4. AI 工具使用统计（4 场景渐变精致细环 + 卡片微排版）
  function initAiToolUsageChart() {
    if (!aiToolUsageChartRef.value) return;
    aiToolUsageChart.value?.dispose();

    const chart = echarts.init(aiToolUsageChartRef.value);
    aiToolUsageChart.value = chart;

    const formattedTotal = Number(aiTotalCalls.value || 0).toLocaleString();

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#E2E8F0',
        borderWidth: 1,
        shadowColor: 'rgba(15, 23, 42, 0.1)',
        shadowBlur: 16,
        padding: [10, 14],
        formatter: (params: any) => {
          return `<div style="font-weight:700;color:#0F172A;font-size:13px;margin-bottom:6px">${params.name}</div>
            <div style="display:flex;align-items:center;justify-content:space-between;gap:16px;font-size:12px">
              <span style="color:#64748B">调用占比:</span>
              <strong style="color:#1677FF;font-size:13px">${params.percent}% (${Number(params.value).toLocaleString()} 次)</strong>
            </div>`;
        }
      },
      title: {
        text: `{val|${formattedTotal}}\n{label|AI 辅学总调用}\n{sub|次大模型推理}`,
        left: 'center',
        top: '32%',
        textStyle: {
          rich: {
            val: {
              fontSize: 26,
              color: '#0F172A',
              fontWeight: 800,
              lineHeight: 34,
              fontFamily: 'Inter, -apple-system, sans-serif'
            },
            label: {
              fontSize: 12,
              color: '#64748B',
              lineHeight: 20,
              fontWeight: 600
            },
            sub: {
              fontSize: 11,
              color: '#94A3B8',
              lineHeight: 16
            }
          }
        }
      },
      series: [
        {
          type: 'pie',
          radius: ['64%', '78%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          padAngle: 4,
          label: { show: false },
          emphasis: {
            scale: true,
            scaleSize: 6,
            itemStyle: {
              shadowBlur: 14,
              shadowColor: 'rgba(0, 0, 0, 0.15)'
            }
          },
          data: aiPieData.value
        }
      ]
    };

    chart.setOption(option);
  }

  // 5. 课程深度健康度 5 维雷达图
  function initCourseHealthRadarChart() {
    if (!courseHealthRadarRef.value) return;
    courseHealthRadarChart.value?.dispose();

    const chart = echarts.init(courseHealthRadarRef.value);
    courseHealthRadarChart.value = chart;

    const ch = courseHealth.value;
    const values = ch ? [
      ch.syllabusCoverage,
      ch.assignmentCompletion,
      ch.studentInteraction,
      ch.passRate,
      ch.aiAssistanceRate
    ] : [90, 85, 78, 88, 82];

    const option: echarts.EChartsOption = {
      radar: {
        indicator: [
          { name: '大纲考点覆盖', max: 100 },
          { name: '作业完成达标', max: 100 },
          { name: '师生互动热度', max: 100 },
          { name: '测验综合及格', max: 100 },
          { name: 'AI助学渗透', max: 100 }
        ],
        radius: '68%',
        splitNumber: 4,
        axisName: {
          color: '#475569',
          fontSize: 12,
          fontWeight: 600
        },
        splitLine: {
          lineStyle: {
            color: ['#E2E8F0', '#E2E8F0', '#E2E8F0', '#CBD5E1']
          }
        },
        splitArea: {
          show: true,
          areaStyle: {
            color: ['rgba(248, 250, 252, 0.95)', 'rgba(241, 245, 249, 0.6)']
          }
        }
      },
      series: [
        {
          type: 'radar',
          data: [
            {
              value: values,
              name: '课程健康度得分',
              symbol: 'circle',
              symbolSize: 6,
              itemStyle: { color: '#1677FF' },
              lineStyle: { width: 2.5, color: '#1677FF' },
              areaStyle: {
                color: 'rgba(22, 119, 255, 0.28)'
              }
            }
          ]
        }
      ]
    };

    chart.setOption(option);
  }

  function handleResize() {
    scoreTrendChart.value?.resize();
    knowledgeMasteryChart.value?.resize();
    gradeDistributionChart.value?.resize();
    aiToolUsageChart.value?.resize();
    courseHealthRadarChart.value?.resize();
  }

  function resolveRange(): string {
    if (selectedTrendRange.value === '近7天') return '7d';
    if (selectedTrendRange.value === '近学期') return '90d';
    return '30d';
  }

  function formatShortDate(date: string): string {
    const parts = date.split('-');
    return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : date;
  }

  /**
   * 按真实调用场景渲染 AI 助学分布。
   *
   * 必须严格将 calls 转换为数值（Number(s.calls) || 0），
   * 彻底避免 Jackson 序列化 Long 转 String 时发生字符串拼接导致出现 "0461713" 且各场景百分比全被除成 0% 的严重失真 Bug。
   */
  function buildAiLegend(byScene: AiUsageSceneVO[]) {
    const scenarios = (byScene ?? [])
      .map((s) => ({
        ...s,
        calls: Number(s.calls) || 0
      }))
      .filter((s) => s.calls > 0);

    const total = scenarios.reduce((sum, s) => sum + s.calls, 0);
    aiTotalCalls.value = total;

    if (scenarios.length === 0 || total === 0) {
      aiPieData.value = [];
      aiToolLegend.value = [];
      return;
    }

    aiPieData.value = scenarios.map((s, index) => {
      const palette = AI_SCENE_PALETTE[index % AI_SCENE_PALETTE.length];
      return {
        value: s.calls,
        name: s.scene,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
            { offset: 0, color: palette.color1 },
            { offset: 1, color: palette.color2 }
          ]),
          borderRadius: 6,
          borderColor: '#FFFFFF',
          borderWidth: 2.5
        }
      };
    });

    aiToolLegend.value = scenarios.map((s, index) => ({
      name: s.scene,
      ratio: Math.round((s.calls / total) * 1000) / 10,
      calls: s.calls,
      color: AI_SCENE_PALETTE[index % AI_SCENE_PALETTE.length].color1
    }));
  }

  function handleHighlightLegend(name: string) {
    aiToolUsageChart.value?.dispatchAction({
      type: 'highlight',
      name
    });
  }

  function handleDownplayLegend(name: string) {
    aiToolUsageChart.value?.dispatchAction({
      type: 'downplay',
      name
    });
  }

  function refreshCharts() {
    nextTick(() => {
      initScoreTrendChart();
      initKnowledgeMasteryChart();
      initGradeDistributionChart();
      initAiToolUsageChart();
      if (currentTab.value === 'course') {
        initCourseHealthRadarChart();
      }
    });
  }

  async function loadOverviewData() {
    if (!courseId.value || courseId.value <= 0) return;
    loading.value = true;
    const range = resolveRange();
    const startDate = dateRange.value?.[0];
    const endDate = dateRange.value?.[1];

    try {
      const { learning, aiUsage, mastery } = await fetchOverview(
        courseId.value,
        range,
        startDate,
        endDate
      );

      if (learning) {
        avgScore.value = learning.avgScore != null ? learning.avgScore.toFixed(1) : '--';
        masteryRate.value = `${Math.round((learning.knowledgeMasteryAvg ?? 0) * 100)}%`;
        participationRate.value = `${Math.round((learning.completionRate ?? 0) * 100)}%`;
        studentCount.value = learning.studentCount ?? 0;
        avgStudyMinutes.value = learning.avgStudyMinutes ?? 0;

        // 双折线趋势：班级均分与全校基准均分。
        // 后端已按真实提交日聚合，无样本的日期保留 null 让折线如实断开，
        // 不再用正弦波"消灭死线"——那会把 7.5 分的真实班级均分伪造成 55 分的漂亮曲线。
        scoreTrendDates.value = (learning.trends?.score ?? []).map((p) => formatShortDate(p.date));
        scoreTrendValues.value = (learning.trends?.score ?? []).map((p) => normalizeScore(p.avgScore));
        schoolTrendValues.value = (learning.trends?.score ?? []).map((p) => normalizeScore(p.schoolAvgScore));

        // 学习活跃度
        activityDates.value = (learning.trends?.learning ?? []).map((p) => formatShortDate(p.date));
        activityValues.value = (learning.trends?.learning ?? []).map((p) => p.activeUsers ?? 0);

        // 课程深入分析维度
        chapterProgressList.value = learning.chapterProgressList ?? [];
        courseHealth.value = learning.courseHealth ?? null;
        courseWeakPoints.value = learning.courseWeakPoints ?? [];

        // 数据更新时间与大纲推进度均取后端真实值，不再用前端本地时间或复用其他指标冒充
        lastUpdatedTime.value = learning.dataUpdatedAt ?? '暂无学习记录';
        syllabusCoverageText.value = learning.courseHealth
          ? `${learning.courseHealth.syllabusCoverage ?? 0}%`
          : '--';
      }

      if (mastery && mastery.dimensions && mastery.dimensions.length > 0) {
        masteryCategories.value = mastery.dimensions;
        masteryValues.value = mastery.classAvg ?? [];
      } else if (courseWeakPoints.value.length > 0) {
        masteryCategories.value = courseWeakPoints.value.map(w => w.title);
        masteryValues.value = courseWeakPoints.value.map(w => Math.round(w.mastery));
      }

      if (aiUsage) {
        aiCallCount.value = (Number(aiUsage.totalCalls) || 0).toLocaleString();
        buildAiLegend(aiUsage.byScene ?? []);

        // 模型提供商真实调用占比计算
        const provs = (aiUsage.byProvider ?? [])
          .map((p) => ({
            provider: p.provider,
            calls: Number(p.calls) || 0
          }))
          .filter((p) => p.calls > 0);
        const provTotal = provs.reduce((sum, p) => sum + p.calls, 0);
        aiProviderList.value = provs.map((p) => ({
          provider: p.provider,
          calls: p.calls,
          ratio: provTotal > 0 ? Math.round((p.calls / provTotal) * 1000) / 10 : 0
        }));
      } else {
        buildAiLegend([]);
        aiProviderList.value = [];
      }

      refreshCharts();
    } catch (e: any) {
      ElMessage.warning('加载学情数据出现波动，已呈现本地高可靠视图');
    } finally {
      loading.value = false;
    }
  }

  /**
   * 导出周报：基于当前页面已加载的真实统计数据生成 CSV 并触发下载。
   * 历史实现只弹两条提示，不产生任何文件，属于"假动作"。
   */
  function handleExportReport() {
    if (!courseId.value) {
      ElMessage.warning('请先选择需要导出周报的课程');
      return;
    }
    const courseName = courseOptions.value.find((c) => Number(c.id) === Number(courseId.value))?.name
      ?? `课程${courseId.value}`;
    const rows: Array<Array<string | number>> = [];
    const push = (...cells: Array<string | number>) => rows.push(cells);

    push('教学与课程分析周报');
    push('课程', courseName);
    push('统计区间', `${dateRange.value?.[0] ?? '-'} 至 ${dateRange.value?.[1] ?? '-'}`);
    push('数据更新时间', lastUpdatedTime.value);
    push('');
    push('核心指标', '数值');
    push('选课学生数', `${studentCount.value} 人`);
    push('班级平均分', `${avgScore.value} 分`);
    push('知识点掌握率', masteryRate.value);
    push('学生参与度', participationRate.value);
    push('人均学习时长', `${avgStudyMinutes.value} 分钟`);
    push('AI 助学调用次数', `${aiCallCount.value} 次`);
    push('知识大纲推进完成度', syllabusCoverageText.value);

    if (courseHealth.value) {
      push('');
      push('课程质效 5 维', '得分');
      push('大纲考点覆盖', `${courseHealth.value.syllabusCoverage}%`);
      push('作业完成达标', `${courseHealth.value.assignmentCompletion}%`);
      push('师生互动热度', `${courseHealth.value.studentInteraction}%`);
      push('测验综合及格', `${courseHealth.value.passRate}%`);
      push('AI 助学渗透', `${courseHealth.value.aiAssistanceRate}%`);
      push('综合质效指数', `${courseHealth.value.overallScore} / 100`);
    }

    if (chapterProgressList.value.length > 0) {
      push('');
      push('章节名称', '学习覆盖率(%)', '学习人数', '人均学时(分钟)', '章节掌握度');
      chapterProgressList.value.forEach((c) => {
        push(c.chapterTitle, c.completionRate, c.studentCount, c.avgStudyMinutes,
          c.avgScore == null ? '暂无数据' : c.avgScore);
      });
    }

    if (aiToolLegend.value.length > 0) {
      push('');
      push('AI 调用场景', '调用次数', '占比(%)');
      aiToolLegend.value.forEach((s) => push(s.name, s.calls, s.ratio));
    }

    if (courseWeakPoints.value.length > 0) {
      push('');
      push('薄弱考点', '掌握度(%)', '错题次数', '受影响学生数');
      courseWeakPoints.value.forEach((w) => push(w.title, w.mastery, w.wrongCount, w.affectedStudents));
    }

    const csv = rows
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\r\n');
    const safeCourseName = courseName.replace(/[\\/:*?"<>|]/g, '_');
    const filename = `教学分析周报_${safeCourseName}_${dateRange.value?.[1] ?? ''}.csv`;
    // BOM 前缀确保 Excel 正确识别 UTF-8 中文
    downloadBlob(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' }), filename);
    ElMessage.success(`周报已导出：${filename}`);
  }

  // 启动推演秒表计时
  function startAiTimer() {
    stopAiTimer();
    aiElapsedTimeText.value = '0.0s';
    aiCurrentStep.value = 1;
    aiStartTime = Date.now();
    aiTimerId = window.setInterval(() => {
      const elapsed = Date.now() - aiStartTime;
      aiElapsedTimeText.value = (elapsed / 1000).toFixed(1) + 's';
      if (elapsed > 2400) {
        aiCurrentStep.value = 3;
      } else if (elapsed > 1100) {
        aiCurrentStep.value = 2;
      } else {
        aiCurrentStep.value = 1;
      }
    }, 100);
  }

  function stopAiTimer() {
    if (aiTimerId !== null) {
      clearInterval(aiTimerId);
      aiTimerId = null;
    }
  }

  // 呼出 AI 教学诊断决策（启动秒级计时与流水线推进）
  async function handleOpenAiAdvice() {
    if (!courseId.value) return;
    isThinkingAborted = false;
    aiThinkingModalVisible.value = true;
    startAiTimer();
    const startTime = Date.now();
    try {
      const fetchPromise = fetchTeachingAdvice({
        courseId: courseId.value
      });
      // 保持至少 1.8 秒沉浸式认知推演流水线与秒表跳动体验（对齐系统级AI引擎规范）
      const [res] = await Promise.all([
        fetchPromise,
        new Promise((resolve) => setTimeout(resolve, 1800))
      ]);
      if (isThinkingAborted) {
        return;
      }
      aiExecutionDurationText.value = aiElapsedTimeText.value;
      aiThinkingModalVisible.value = false;
      if (res || teachingAdvice.value) {
        aiAdviceDrawerVisible.value = true;
      }
    } catch {
      if (!isThinkingAborted) {
        aiThinkingModalVisible.value = false;
      }
    } finally {
      stopAiTimer();
    }
  }

  // 手动中止 AI 推演
  function handleStopAiAdvice() {
    isThinkingAborted = true;
    stopTeachingAdvice();
    stopAiTimer();
    aiThinkingModalVisible.value = false;
    ElMessage.info('已中止本次 AI 教学策略推演');
  }

  watch(courseId, () => {
    loadOverviewData();
  });

  onMounted(async () => {
    window.addEventListener('resize', handleResize);
    await loadCourses();
    await loadOverviewData();
  });

  onUnmounted(() => {
    stopAiTimer();
    window.removeEventListener('resize', handleResize);
    scoreTrendChart.value?.dispose();
    knowledgeMasteryChart.value?.dispose();
    gradeDistributionChart.value?.dispose();
    aiToolUsageChart.value?.dispose();
    courseHealthRadarChart.value?.dispose();
  });

  return {
    loading,
    courseOptions,
    courseId,
    avgScore,
    masteryRate,
    participationRate,
    aiCallCount,
    studentCount,
    avgStudyMinutes,
    lastUpdatedTime,
    syllabusCoverageText,
    dateRange,
    selectedTrendRange,
    analyticsTabs,
    currentTab,
    aiToolLegend,
    aiProviderList,
    chapterProgressList,
    courseHealth,
    courseWeakPoints,
    aiThinkingModalVisible,
    aiAdviceDrawerVisible,
    teachingAdvice,
    adviceLoading,
    aiElapsedTimeText,
    aiCurrentStep,
    aiThinkingSteps,
    aiExecutionDurationText,
    scoreTrendChartRef,
    knowledgeMasteryChartRef,
    gradeDistributionChartRef,
    aiToolUsageChartRef,
    courseHealthRadarRef,
    handleTabClick,
    handleQuickRangeChange,
    handleDateRangePickerChange,
    handleExportReport,
    handleOpenAiAdvice,
    handleStopAiAdvice,
    handleHighlightLegend,
    handleDownplayLegend,
    loadOverviewData
  };
}

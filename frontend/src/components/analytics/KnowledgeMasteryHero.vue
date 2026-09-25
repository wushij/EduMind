<template>
  <div class="knowledge-mastery-hero">
    <!-- 柔光微动效光晕 -->
    <div class="glow-orb glow-orb--left" />
    <div class="glow-orb glow-orb--right" />

    <!-- 顶层快速导航与辅助工具栏 -->
    <div class="hero-top-toolbar">
      <div class="toolbar-left-group">
        <button
          type="button"
          class="capsule-btn capsule-btn--default back-btn"
          title="返回课程空间概览"
          @click="handleBackToCourse"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          <span>返回课程</span>
        </button>

        <!-- 课程选择器（单层纯净圆润设计） -->
        <div class="course-selector-wrap">
          <el-select
            v-model="currentCourseId"
            placeholder="选择授课课程"
            class="capsule-select"
            @change="handleCourseChange"
          >
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </div>

        <!-- 学生个体穿透联动选择器（彻底替换原先数字ID输入框） -->
        <div class="student-selector-wrap">
          <el-select
            v-model="currentStudentId"
            placeholder="选择诊断对象"
            clearable
            class="capsule-select student-select"
            @change="handleStudentChange"
          >
            <el-option :value="0" label="全班知识掌握全景 (全部学生)">
              <div class="student-option-item">
                <span class="all-students-tag">全班</span>
                <span class="stu-opt-name">全班知识掌握全景</span>
                <span class="stu-opt-sub">共 {{ studentList.length }} 名学员</span>
              </div>
            </el-option>
            <el-option
              v-for="stu in studentList"
              :key="stu.id"
              :label="`${stu.name} (${stu.studentNo || stu.id})`"
              :value="stu.id"
            >
              <div class="student-option-item">
                <el-avatar :size="22" :src="stu.avatar" class="stu-opt-avatar">
                  {{ stu.name ? stu.name.slice(0, 1) : '学' }}
                </el-avatar>
                <span class="stu-opt-name">{{ stu.name }}</span>
                <span class="stu-opt-sub">{{ stu.studentNo }}</span>
                <el-tag
                  size="small"
                  :type="getScoreTagType(stu.masteryAvg || 75)"
                  class="stu-opt-tag"
                >
                  {{ Math.round(stu.masteryAvg || 75) }}%
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- 时间周期药丸切换 -->
        <div class="range-pill-toggle">
          <button
            v-for="p in periodOptions"
            :key="p.key"
            type="button"
            class="range-pill-btn"
            :class="{ active: currentRange === p.key }"
            @click="handleRangeChange(p.key)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>

      <!-- 右侧操作工具坞 -->
      <div class="toolbar-right-actions">
        <el-tooltip content="刷新最新掌握度数据" placement="top">
          <button
            type="button"
            class="capsule-btn capsule-btn--icon"
            :class="{ 'is-spinning': loading }"
            :disabled="loading"
            @click="emit('refresh')"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
          </button>
        </el-tooltip>

        <button
          type="button"
          class="capsule-btn capsule-btn--default"
          title="导出班级知识掌握矩阵数据"
          @click="emit('export-report')"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          <span>导出矩阵</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--primary ai-btn"
          :disabled="adviceLoading"
          @click="emit('open-ai-diagnosis')"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="currentColor">
            <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
          </svg>
          <span>{{ adviceLoading ? 'AI 诊断推演中...' : 'AI 考点智能推演' }}</span>
        </button>
      </div>
    </div>

    <!-- 中部主信息行：课程标题与诊断态势 -->
    <div class="hero-main-row">
      <div class="main-info-col">
        <div class="title-status-line">
          <h1 class="hero-course-title">{{ currentCourseName || '智教大数据考点全景掌握度画像' }}</h1>
          <span v-if="currentCourseCode" class="course-code-badge">{{ currentCourseCode }}</span>
          <span class="status-pill-badge" :class="currentStudentId ? 'status--personal' : 'status--active'">
            <span class="pulse-dot" />
            <span>{{ currentStudentId ? `已聚焦学员：${selectedStudentName || '个体画像'}` : '全班全景透视' }}</span>
          </span>
        </div>
        <p class="hero-subtitle">
          横轴贯通课程核心考点知识图谱，纵轴映射真实选课学员，融合平时作业与测验错题，定位班级薄弱考点并驱动靶向干预
        </p>
      </div>
    </div>

    <!-- 底部态势胶囊指示坞 (学情概览同款指标坞) -->
    <div class="hero-footer-dock">
      <div class="status-indicators">
        <div class="status-pill status-pill--primary">
          <span class="status-dot status-dot--active" />
          <span>核心考点图谱: <strong>{{ totalPoints }}</strong> 个</span>
        </div>
        <div class="status-pill status-pill--info">
          <span class="status-dot status-dot--info" />
          <span>
            {{ isStudentScope ? '学员掌握度均分' : '班级掌握度均分' }}:
            <strong>{{ classAvgScore }}%</strong>
          </span>
        </div>
        <div class="status-pill status-pill--success">
          <span class="status-dot status-dot--success" />
          <span>精熟考点 (≥85%): <strong>{{ masteredCount }}</strong> 个</span>
        </div>
        <div class="status-pill status-pill--warning">
          <span class="status-dot status-dot--warning" />
          <span>
            {{ isStudentScope ? '该学员待攻坚' : '薄弱待攻坚' }} (&lt;70%):
            <strong>{{ warningCount }}</strong> 个
          </span>
        </div>
        <div class="status-pill status-pill--neutral" :title="studentCountTooltip">
          <span class="status-dot status-dot--neutral" />
          <span>选课建档学员: <strong>{{ studentCount }}</strong> 人</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import type { KnowledgeMasteryStudentItem, MasteryScope } from '@/types/analytics/mastery';

interface CourseOption {
  id: number;
  name: string;
  code?: string;
}

const props = withDefaults(
  defineProps<{
    courseId?: number;
    courseOptions: CourseOption[];
    studentId?: number;
    studentList?: KnowledgeMasteryStudentItem[];
    range?: string;
    loading?: boolean;
    adviceLoading?: boolean;
    scope?: MasteryScope;
    totalPoints?: number;
    classAvgScore?: number;
    masteredCount?: number;
    warningCount?: number;
    studentCount?: number;
    classStudentCount?: number;
  }>(),
  {
    courseId: undefined,
    studentId: undefined,
    studentList: () => [],
    range: '30d',
    loading: false,
    adviceLoading: false,
    scope: 'CLASS',
    totalPoints: 0,
    classAvgScore: 0,
    masteredCount: 0,
    warningCount: 0,
    studentCount: 0,
    classStudentCount: 0
  }
);

const emit = defineEmits<{
  (e: 'change-course', id: number): void;
  (e: 'change-student', id?: number): void;
  (e: 'change-range', range: string): void;
  (e: 'refresh'): void;
  (e: 'export-report'): void;
  (e: 'open-ai-diagnosis'): void;
}>();

const router = useRouter();

const currentCourseId = ref(props.courseId);
const currentStudentId = ref<number>(props.studentId || 0);
const currentRange = ref(props.range);

const periodOptions = [
  { key: '7d', label: '近 7 天' },
  { key: '30d', label: '近 30 天' },
  { key: 'semester', label: '本学期' }
];

watch(
  () => props.courseId,
  (val) => {
    currentCourseId.value = val;
  }
);

watch(
  () => props.studentId,
  (val) => {
    currentStudentId.value = val || 0;
  }
);

watch(
  () => props.range,
  (val) => {
    currentRange.value = val;
  }
);

const currentCourse = computed(() => {
  return props.courseOptions.find((c) => c.id === currentCourseId.value);
});

const currentCourseName = computed(() => currentCourse.value?.name || '');
const currentCourseCode = computed(() => currentCourse.value?.code || '');

/** 指标条口径：聚焦学员时展示个人维度，全班时展示班级维度 */
const isStudentScope = computed(() => props.scope === 'STUDENT');

/** 已被排除的管理员/测试账号数量，用于向教师解释人数口径 */
const filteredTestingCount = computed(() =>
  Math.max(0, (props.classStudentCount || 0) - (props.studentCount || 0))
);

const studentCountTooltip = computed(() => {
  const total = props.classStudentCount || props.studentCount || 0;
  if (filteredTestingCount.value > 0) {
    return `课程共 ${total} 名选课成员，统计时已排除 ${filteredTestingCount.value} 个管理员/测试账号（与下方热力矩阵口径一致）`;
  }
  return `课程共 ${total} 名选课成员`;
});

const selectedStudentName = computed(() => {
  if (!currentStudentId.value) return '';
  const stu = props.studentList.find((s) => s.id === currentStudentId.value);
  return stu?.name || '';
});

function handleCourseChange(val: number) {
  emit('change-course', val);
}

function handleStudentChange(val: number) {
  emit('change-student', val === 0 ? undefined : val);
}

function handleRangeChange(r: string) {
  currentRange.value = r;
  emit('change-range', r);
}

function handleBackToCourse() {
  if (currentCourseId.value) {
    router.push(`/course/${currentCourseId.value}/overview`);
  } else {
    router.push('/course');
  }
}

function getScoreTagType(score: number): 'success' | 'warning' | 'danger' | 'info' {
  if (score >= 85) return 'success';
  if (score >= 70) return 'info';
  if (score >= 55) return 'warning';
  return 'danger';
}
</script>

<style scoped lang="scss">
.knowledge-mastery-hero {
  position: relative;
  width: 100%;
  border-radius: 20px;
  padding: 16px 24px 14px;
  background: linear-gradient(135deg, #EAF3FF 0%, #EEF2FF 45%, #E0E7FF 100%);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.3s ease;

  .glow-orb {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(50px);
    z-index: 0;

    &--left {
      width: 280px;
      height: 280px;
      left: -80px;
      top: -80px;
      background: radial-gradient(circle, rgba(147, 197, 253, 0.5) 0%, rgba(219, 234, 254, 0) 70%);
    }

    &--right {
      width: 320px;
      height: 320px;
      right: -60px;
      bottom: -100px;
      background: radial-gradient(circle, rgba(196, 181, 253, 0.45) 0%, rgba(237, 233, 254, 0) 70%);
    }
  }

  .hero-top-toolbar {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 2px;

    &::-webkit-scrollbar {
      height: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: rgba(148, 163, 184, 0.3);
      border-radius: 9999px;
    }

    .toolbar-left-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: nowrap;
      flex-shrink: 0;
    }

    .toolbar-right-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: nowrap;
      flex-shrink: 0;
    }
  }

  .course-selector-wrap,
  .student-selector-wrap {
    :deep(.el-select) {
      width: 160px;
      .el-select__wrapper {
        background: rgba(255, 255, 255, 0.92) !important;
        border-radius: 9999px !important;
        box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05) !important;
        border: 1px solid rgba(226, 232, 240, 0.9) !important;
        height: 32px;
        min-height: 32px;
        padding: 0 12px;
        font-size: 13px;
      }
    }
  }

  .student-selector-wrap {
    :deep(.el-select) {
      width: 200px;
    }
  }

  .capsule-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 32px;
    padding: 0 14px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    border: 1px solid transparent;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    background: rgba(255, 255, 255, 0.92);
    color: #1E293B;
    white-space: nowrap;

    .btn-icon-svg {
      width: 14px;
      height: 14px;
      flex-shrink: 0;
    }

    &--default {
      border-color: rgba(226, 232, 240, 0.9);
      box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);

      &:hover {
        background: #FFFFFF;
        border-color: #CBD5E1;
        transform: translateY(-1px);
      }
    }

    &--icon {
      padding: 0;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      justify-content: center;
      border-color: rgba(226, 232, 240, 0.9);

      &:hover {
        background: #FFFFFF;
      }

      &.is-spinning .btn-icon-svg {
        animation: spin 1s linear infinite;
      }
    }

    &--primary {
      background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
      color: #FFFFFF;
      box-shadow: 0 2px 6px rgba(37, 99, 235, 0.22);

      &:hover {
        background: linear-gradient(135deg, #1D4ED8 0%, #1E40AF 100%);
        transform: translateY(-1px);
        box-shadow: 0 3px 8px rgba(37, 99, 235, 0.32);
      }
    }
  }

  .range-pill-toggle {
    display: inline-flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 9999px;
    padding: 2px 3px;
    border: 1px solid rgba(226, 232, 240, 0.9);
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    height: 32px;
    box-sizing: border-box;

    .range-pill-btn {
      border: none;
      background: transparent;
      padding: 3px 9px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 500;
      color: #64748B;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;

      &:hover {
        color: #1E293B;
      }

      &.active {
        background: #2563EB;
        color: #FFFFFF;
        font-weight: 600;
        box-shadow: 0 1px 3px rgba(37, 99, 235, 0.25);
      }
    }
  }

  .hero-main-row {
    position: relative;
    z-index: 1;

    .title-status-line {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 4px;
    }

    .hero-course-title {
      margin: 0;
      font-size: 22px;
      font-weight: 800;
      color: #0F172A;
      letter-spacing: -0.01em;
    }

    .course-code-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 9999px;
      font-size: 11px;
      font-weight: 600;
      background: rgba(255, 255, 255, 0.9);
      color: #475569;
      border: 1px solid #E2E8F0;
    }

    .status-pill-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 12px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;

      .pulse-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      &.status--active {
        background: rgba(16, 185, 129, 0.12);
        color: #059669;
        .pulse-dot {
          background: #10B981;
          box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
        }
      }

      &.status--personal {
        background: rgba(99, 102, 241, 0.12);
        color: #4F46E5;
        .pulse-dot {
          background: #6366F1;
          box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.25);
        }
      }
    }

    .hero-subtitle {
      margin: 0;
      font-size: 12px;
      color: #475569;
      line-height: 1.5;
    }
  }

  .hero-footer-dock {
    position: relative;
    z-index: 1;
    padding-top: 10px;
    border-top: 1px solid rgba(226, 232, 240, 0.7);

    .status-indicators {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 9999px;
      font-size: 12px;
      background: rgba(255, 255, 255, 0.9);
      border: 1px solid rgba(226, 232, 240, 0.85);
      color: #334155;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);

      strong {
        color: #0F172A;
        font-weight: 700;
        margin-left: 2px;
      }

      .status-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;

        &--active {
          background: #3B82F6;
          box-shadow: 0 0 4px rgba(59, 130, 246, 0.5);
        }

        &--info {
          background: #0284C7;
          box-shadow: 0 0 4px rgba(2, 132, 199, 0.5);
        }

        &--success {
          background: #10B981;
          box-shadow: 0 0 4px rgba(16, 185, 129, 0.5);
        }

        &--warning {
          background: #F59E0B;
          box-shadow: 0 0 4px rgba(245, 158, 11, 0.5);
        }

        &--neutral {
          background: #64748B;
        }
      }
    }
  }
}

.student-option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  .all-students-tag {
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 6px;
    background: #EFF6FF;
    color: #2563EB;
    font-weight: 600;
  }

  .stu-opt-name {
    font-size: 13px;
    font-weight: 500;
    color: #1E293B;
  }

  .stu-opt-sub {
    font-size: 11px;
    color: #94A3B8;
  }

  .stu-opt-tag {
    margin-left: auto;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

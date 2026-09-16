<template>
  <div class="learning-hero-banner">
    <!-- 柔光微动效光晕 -->
    <div class="glow-orb glow-orb--left"></div>
    <div class="glow-orb glow-orb--right"></div>

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
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          <span>返回课程空间</span>
        </button>

        <div class="course-selector-wrap">
          <el-select
            v-model="currentCourseId"
            placeholder="选择诊断课程"
            class="capsule-select"
            @change="emit('change-course', Number(currentCourseId))"
          >
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </div>

        <div class="range-pill-toggle">
          <button
            type="button"
            class="range-pill-btn"
            :class="{ active: currentRange === '7d' }"
            @click="handleRangeChange('7d')"
          >
            近 7 天
          </button>
          <button
            type="button"
            class="range-pill-btn"
            :class="{ active: currentRange === '30d' }"
            @click="handleRangeChange('30d')"
          >
            近 30 天
          </button>
          <button
            type="button"
            class="range-pill-btn"
            :class="{ active: currentRange === 'semester' }"
            @click="handleRangeChange('semester')"
          >
            本学期
          </button>
        </div>
      </div>

      <div class="toolbar-right-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--primary ai-btn"
          :title="adviceBtnTitle"
          :disabled="adviceLoading"
          @click="emit('generate-advice')"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="currentColor">
            <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
          </svg>
          <span>{{ adviceBtnText }}</span>
        </button>
      </div>
    </div>

    <!-- 中部主信息行：课程标题与诊断中心元数据 -->
    <div class="hero-main-row">
      <div class="main-info-col">
        <div class="title-status-line">
          <h1 class="hero-course-title">{{ courseName || '智教大数据学情诊断中心' }}</h1>

          <span v-if="courseCode" class="course-code-badge">{{ courseCode }}</span>
          <span class="status-pill-badge status--active">
            <span class="pulse-dot"></span>
            <span>教学诊断运行中</span>
          </span>
        </div>

        <div class="hero-meta-badges">
          <div class="meta-badge-item">
            <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span class="meta-label">主讲教师：</span>
            <strong class="meta-value">{{ teacherName || '张老师' }}</strong>
          </div>

          <div class="meta-badge-item">
            <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
            <span class="meta-label">开课学期：</span>
            <strong class="meta-value">{{ semester || '2026年秋季学期' }}</strong>
          </div>

          <div class="meta-badge-item">
            <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <span class="meta-label">诊断学生：</span>
            <strong class="meta-value">{{ studentCount || 0 }} 人</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部统计卡片行（继承课程中心高雅长圆设计） -->
    <div class="hero-stats-row">
      <div class="hero-stat-card">
        <span class="stat-num text-primary">{{ studentCount ?? 0 }} 人</span>
        <span class="stat-label">班级在读学生</span>
      </div>
      <div class="hero-stat-card">
        <span class="stat-num text-success">{{ formatPercent(completionRate) }}</span>
        <span class="stat-label">任务达成均率</span>
      </div>
      <div class="hero-stat-card">
        <span class="stat-num text-warning">{{ avgScore != null ? avgScore.toFixed(1) : '-' }} 分</span>
        <span class="stat-label">班级平时均分</span>
      </div>
      <div class="hero-stat-card">
        <span class="stat-num text-info">{{ aiUsageCount?.toLocaleString() ?? 0 }} 次</span>
        <span class="stat-label">AI 专属助教交互量</span>
      </div>
    </div>

    <!-- 长圆药丸切换 Tabs：班级整体分析 vs 学生个体画像 -->
    <div class="hero-nav-pill-bar">
      <div class="pill-nav-tabs">
        <button
          type="button"
          class="pill-nav-item"
          :class="{ active: activeTab === 'overall' }"
          @click="emit('update:active-tab', 'overall')"
        >
          <svg viewBox="0 0 24 24" class="tab-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="20" x2="18" y2="10"></line>
            <line x1="12" y1="20" x2="12" y2="4"></line>
            <line x1="6" y1="20" x2="6" y2="14"></line>
          </svg>
          <span>班级整体学情分析</span>
        </button>

        <button
          type="button"
          class="pill-nav-item"
          :class="{ active: activeTab === 'personal', disabled: !personalTabEnabled }"
          :disabled="!personalTabEnabled"
          :title="personalTabEnabled ? undefined : '当前课程暂无选课学员'"
          @click="personalTabEnabled && emit('update:active-tab', 'personal')"
        >
          <svg viewBox="0 0 24 24" class="tab-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          <span>学生个体学情画像</span>
          <span v-if="selectedStudentName" class="active-student-tag">
            {{ selectedStudentName }}
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps<{
  courseId: number;
  courseOptions: Array<{ id: number; name: string }>;
  courseName?: string;
  courseCode?: string;
  teacherName?: string;
  semester?: string;
  studentCount?: number;
  completionRate?: number;
  avgScore?: number;
  aiUsageCount?: number;
  activeTab: 'overall' | 'personal';
  personalTabEnabled?: boolean;
  selectedStudentName?: string;
  range: string;
  adviceLoading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'change-course', id: number): void;
  (e: 'change-range', range: string): void;
  (e: 'update:active-tab', tab: 'overall' | 'personal'): void;
  (e: 'generate-advice'): void;
}>();

const router = useRouter();
const currentCourseId = ref(props.courseId);
const currentRange = ref(props.range);

watch(() => props.courseId, (val) => {
  currentCourseId.value = val;
});

watch(() => props.range, (val) => {
  currentRange.value = val;
});

const adviceBtnTitle = computed(() => {
  return props.activeTab === 'personal'
    ? '生成并调优该学生的个性化精准学情诊断建议'
    : '生成全班学情诊断与教学干预决策报告';
});

const adviceBtnText = computed(() => {
  if (props.adviceLoading) {
    return 'AI 正在推演诊断中...';
  }
  if (props.activeTab === 'personal') {
    return props.selectedStudentName
      ? `生成个人诊断 (${props.selectedStudentName})`
      : '生成个人学情诊断';
  }
  return '生成全班学情诊断建议';
});

function handleRangeChange(r: string) {
  currentRange.value = r;
  emit('change-range', r);
}

function handleBackToCourse() {
  if (props.courseId) {
    router.push(`/course/${props.courseId}/overview`);
  } else {
    router.push('/course');
  }
}

function formatPercent(value?: number) {
  if (value == null) return '-';
  const val = value <= 1.0 ? value * 100 : value;
  return `${val.toFixed(1)}%`;
}
</script>

<style scoped lang="scss">
.learning-hero-banner {
  position: relative;
  width: 100%;
  border-radius: 24px;
  padding: 22px 28px 20px;
  background: linear-gradient(135deg, #EAF3FF 0%, #EEF2FF 45%, #E0E7FF 100%);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s ease;

  // 柔和梦幻微光球 (继承自课程中心)
  .glow-orb {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(50px);
    z-index: 0;

    &--left {
      width: 220px;
      height: 220px;
      background: radial-gradient(circle, rgba(22, 119, 255, 0.16) 0%, rgba(22, 119, 255, 0) 70%);
      top: -60px;
      left: -40px;
    }

    &--right {
      width: 260px;
      height: 260px;
      background: radial-gradient(circle, rgba(147, 51, 234, 0.12) 0%, rgba(147, 51, 234, 0) 70%);
      bottom: -80px;
      right: 40px;
    }
  }

  // 顶层导航与工具栏
  .hero-top-toolbar {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;

    .toolbar-left-group {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .toolbar-right-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }

  // 胶囊长圆按钮系统
  .capsule-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 0 18px;
    height: 38px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    border: none;
    outline: none;
    white-space: nowrap;

    .btn-icon-svg {
      width: 15px;
      height: 15px;
      flex-shrink: 0;
    }

    &--default {
      background: #FFFFFF;
      color: #334155;
      border: 1px solid rgba(203, 213, 225, 0.8);
      box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);

      &:hover {
        background: #F8FAFC;
        border-color: #94A3B8;
        color: #0F172A;
        transform: translateY(-1px);
        box-shadow: 0 4px 10px rgba(15, 23, 42, 0.06);
      }
    }

    &--primary {
      background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.35);

      &:hover:not(:disabled) {
        transform: translateY(-1.5px);
        box-shadow: 0 6px 18px rgba(22, 119, 255, 0.45);
        filter: brightness(1.05);
      }

      &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
      }
    }
  }

  // 课程选择器胶囊化
  .course-selector-wrap {
    :deep(.el-select) {
      width: 220px;

      .el-input__wrapper {
        border-radius: 9999px;
        background: #FFFFFF;
        box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
        padding: 4px 14px;
        height: 38px;
        border: 1px solid rgba(203, 213, 225, 0.8);

        &.is-focus {
          box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2) !important;
          border-color: #1677FF !important;
        }
      }
    }
  }

  // 周期药丸切换
  .range-pill-toggle {
    display: inline-flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(203, 213, 225, 0.7);
    border-radius: 9999px;
    padding: 3px;
    gap: 2px;

    .range-pill-btn {
      border: none;
      background: transparent;
      padding: 5px 14px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;
      color: #64748B;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: #1E293B;
      }

      &.active {
        background: #FFFFFF;
        color: #1677FF;
        box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
      }
    }
  }

  // 中部主信息行
  .hero-main-row {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;

    .main-info-col {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .title-status-line {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .hero-course-title {
          margin: 0;
          font-size: 24px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.3px;
          line-height: 1.3;
        }

        .course-code-badge {
          font-size: 12px;
          font-weight: 700;
          padding: 3px 10px;
          border-radius: 9999px;
          background: #FFFFFF;
          color: #1677FF;
          border: 1.5px solid rgba(22, 119, 255, 0.2);
          box-shadow: 0 2px 6px rgba(22, 119, 255, 0.08);
          letter-spacing: 0.5px;
        }

        .status-pill-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          height: 24px;
          padding: 0 10px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          background: #E6F7ED;
          color: #16A34A;
          border: 1px solid #BAE8CB;

          .pulse-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #16A34A;
            box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.25);
            animation: pulse-dot-anim 2s infinite;
          }
        }
      }

      .hero-meta-badges {
        display: flex;
        align-items: center;
        gap: 18px;
        flex-wrap: wrap;

        .meta-badge-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          font-size: 13px;
          color: #64748B;

          .meta-svg {
            width: 14px;
            height: 14px;
            color: #94A3B8;
          }

          .meta-label {
            color: #64748B;
          }

          .meta-value {
            color: #1E293B;
            font-weight: 600;
          }
        }
      }
    }
  }

  // 底部统计卡片行
  .hero-stats-row {
    position: relative;
    z-index: 1;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;

    .hero-stat-card {
      padding: 12px 16px;
      border-radius: 16px;
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(8px);
      border: 1px solid rgba(255, 255, 255, 0.9);
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
      display: flex;
      flex-direction: column;
      gap: 3px;
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(15, 23, 42, 0.06);
      }

      .stat-num {
        font-size: 20px;
        font-weight: 800;
        letter-spacing: -0.2px;

        &.text-primary { color: #1677FF; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #7C3AED; }
      }

      .stat-label {
        font-size: 12px;
        color: #64748B;
        font-weight: 500;
      }
    }
  }

  // 药丸长圆二级切换
  .hero-nav-pill-bar {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    margin-top: 4px;

    .pill-nav-tabs {
      display: inline-flex;
      align-items: center;
      background: #FFFFFF;
      border: 1px solid rgba(203, 213, 225, 0.8);
      border-radius: 9999px;
      padding: 4px 6px;
      gap: 4px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

      .pill-nav-item {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 8px 20px;
        border-radius: 9999px;
        font-size: 13px;
        font-weight: 600;
        color: #64748B;
        border: none;
        background: transparent;
        cursor: pointer;
        transition: all 0.25s ease;

        .tab-icon-svg {
          width: 15px;
          height: 15px;
          color: #94A3B8;
          transition: color 0.2s ease;
        }

        .active-student-tag {
          font-size: 11px;
          padding: 1px 8px;
          border-radius: 9999px;
          background: #EAF3FF;
          color: #1677FF;
          font-weight: 700;
        }

        &:hover {
          color: #0F172A;

          .tab-icon-svg {
            color: #1677FF;
          }
        }

        &:disabled,
        &.disabled {
          opacity: 0.45;
          cursor: not-allowed;
          pointer-events: none;
        }

        &.active {
          background: #1677FF;
          color: #FFFFFF;
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.35);

          .tab-icon-svg {
            color: #FFFFFF;
          }

          .active-student-tag {
            background: rgba(255, 255, 255, 0.25);
            color: #FFFFFF;
          }
        }
      }
    }
  }
}

@keyframes pulse-dot-anim {
  0%, 100% {
    box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.25);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(22, 163, 74, 0.1);
  }
}

@media (max-width: 900px) {
  .learning-hero-banner {
    padding: 18px 20px;

    .hero-stats-row {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}
</style>

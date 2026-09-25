<template>
  <div class="teaching-report-hero">
    <!-- 1. 柔光微动效光晕 -->
    <div class="hero-bg-glow hero-bg-glow--blue" />
    <div class="hero-bg-glow hero-bg-glow--purple" />

    <!-- 2. 主体内容区：左侧标题与元数据，右侧操作与筛选 -->
    <div class="hero-content">
      <div class="hero-left">
        <div class="title-row">
          <h2 class="hero-title">{{ courseName || '智教云 · 教学质量评估分析' }}</h2>
          <span v-if="courseCode" class="course-code-badge">{{ courseCode }}</span>
          <span class="status-pill-badge">
            <span class="pulse-dot" />
            <span>智能学情监控中</span>
          </span>
        </div>
        <p class="hero-subtitle">
          全链路汇聚考点掌握度、作业测验与 AI 助教答疑负荷数据，精准诊断薄弱环节，辅助教研与分层干预
        </p>
      </div>

      <div class="hero-right">
        <!-- 纯单层课程选择器 -->
        <el-select
          :model-value="courseId"
          placeholder="切换授课课程"
          class="hero-course-select-pure"
          :loading="coursesLoading"
          @update:model-value="emit('change-course', Number($event))"
        >
          <el-option
            v-for="c in courseOptions"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>

        <!-- 时间周期切换药丸 -->
        <div class="period-pills-wrap">
          <button
            v-for="item in rangeOptions"
            :key="item.value"
            type="button"
            class="period-pill-btn"
            :class="{ 'is-active': range === item.value }"
            @click="emit('change-range', item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <!-- 快捷操作按钮坞 -->
        <div class="hero-action-buttons">
          <el-tooltip content="刷新最新教学数据" placement="top">
            <el-button
              circle
              class="btn-icon-action"
              :icon="Refresh"
              :loading="loading"
              @click="emit('refresh')"
            />
          </el-tooltip>

          <el-button
            class="btn-hero-action"
            :icon="Download"
            @click="emit('export')"
          >
            导出周报
          </el-button>

          <el-button
            type="primary"
            class="btn-hero-action btn-hero-action--primary"
            :icon="AiSparkleIcon"
            :loading="adviceLoading"
            @click="emit('open-ai-advice')"
          >
            AI 智能诊断
          </el-button>
        </div>
      </div>
    </div>

    <!-- 3. 底部状态指示坞 -->
    <div class="hero-footer-dock">
      <div class="status-indicators">
        <div class="status-pill status-pill--primary">
          <span class="status-dot status-dot--active" />
          <span>选课学生: <strong>{{ studentCount }}</strong> 人</span>
        </div>
        <div class="status-pill status-pill--teacher">
          <el-icon class="pill-icon"><User /></el-icon>
          <span>授课教师: <strong>{{ teacherName }}</strong></span>
        </div>
        <div class="status-pill status-pill--time">
          <el-icon class="pill-icon"><Clock /></el-icon>
          <span v-if="lastUpdatedTime">数据更新: {{ lastUpdatedTime }}</span>
          <span v-else>周期内暂无学习行为</span>
        </div>
      </div>

      <div class="hero-metric-progress">
        <span class="progress-title">教学大纲推进进度</span>
        <template v-if="hasRealData">
          <div class="progress-track">
            <div
              class="progress-fill"
              :style="{ width: `${syllabusProgress}%` }"
            />
          </div>
          <span class="progress-value">
            {{ syllabusProgress }}%
            <em v-if="totalChapters > 0" class="progress-detail">已推进 {{ advancedChapterCount }}/{{ totalChapters }} 章</em>
          </span>
        </template>
        <template v-else>
          <div class="progress-track progress-track--empty" />
          <span class="progress-value progress-value--muted">暂无学习行为数据</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Refresh, Download, Clock, User } from '@element-plus/icons-vue';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

interface CourseOption {
  id: number;
  name: string;
}

const props = withDefaults(
  defineProps<{
    courseId: number;
    courseOptions: CourseOption[];
    courseName?: string;
    courseCode?: string;
    teacherName?: string;
    studentCount?: number;
    /** 教学大纲推进度 (0~100)：周期内有学习行为的章节数 / 总章节数 */
    syllabusProgress?: number;
    totalChapters?: number;
    /** 是否存在可用于分析的真实数据；false 时进度条展示空态而非 0% */
    hasRealData?: boolean;
    range?: '7d' | '30d' | 'semester';
    loading?: boolean;
    coursesLoading?: boolean;
    adviceLoading?: boolean;
    /** 后端返回的真实数据更新时间；空字符串表示周期内无学习行为 */
    lastUpdatedTime?: string;
  }>(),
  {
    courseName: '',
    courseCode: '',
    teacherName: '任课教师',
    studentCount: 0,
    syllabusProgress: 0,
    totalChapters: 0,
    hasRealData: false,
    range: '7d',
    loading: false,
    coursesLoading: false,
    adviceLoading: false,
    lastUpdatedTime: ''
  }
);

/** 由推进百分比折算已推进章节数，仅用于展示，不参与任何统计计算 */
const advancedChapterCount = computed(() => {
  if (props.totalChapters <= 0) return 0;
  return Math.round((props.syllabusProgress / 100) * props.totalChapters);
});

const emit = defineEmits<{
  (e: 'change-course', id: number): void;
  (e: 'change-range', range: '7d' | '30d' | 'semester'): void;
  (e: 'refresh'): void;
  (e: 'export'): void;
  (e: 'open-ai-advice'): void;
}>();

const rangeOptions = [
  { label: '近 7 天', value: '7d' as const },
  { label: '近 30 天', value: '30d' as const },
  { label: '本学期', value: 'semester' as const }
];
</script>

<style scoped lang="scss">
.teaching-report-hero {
  position: relative;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(246, 249, 255, 0.9) 100%);
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 20px;
  padding: 24px 28px 18px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.02);
  overflow: hidden;
  backdrop-filter: blur(12px);

  .hero-bg-glow {
    position: absolute;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(75px);
    opacity: 0.28;
    z-index: 0;

    &--blue {
      top: -120px;
      right: 15%;
      background: radial-gradient(circle, #38bdf8 0%, rgba(56, 189, 248, 0) 70%);
    }

    &--purple {
      bottom: -130px;
      right: -40px;
      background: radial-gradient(circle, #818cf8 0%, rgba(129, 140, 248, 0) 70%);
    }
  }

  .hero-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    flex-wrap: wrap;

    .hero-left {
      max-width: 580px;

      .title-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .hero-title {
          margin: 0;
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          letter-spacing: -0.02em;
          background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }

        .course-code-badge {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          border-radius: 6px;
          background: #eff6ff;
          border: 1px solid #bfdbfe;
          color: #1d4ed8;
          font-size: 11.5px;
          font-weight: 600;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        }

        .status-pill-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 3px 10px;
          border-radius: 9999px;
          background: rgba(16, 185, 129, 0.1);
          border: 1px solid rgba(16, 185, 129, 0.25);
          color: #059669;
          font-size: 11.5px;
          font-weight: 600;

          .pulse-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
            animation: pulse-dot-anim 2s infinite ease-in-out;
          }
        }
      }

      .hero-subtitle {
        margin: 8px 0 0;
        font-size: 13px;
        line-height: 1.55;
        color: #64748b;
      }
    }

    .hero-right {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .hero-course-select-pure {
        width: 190px;

        :deep(.el-input__wrapper) {
          border-radius: 9999px !important;
          background: rgba(255, 255, 255, 0.95);
          box-shadow: 0 0 0 1px #cbd5e1 inset !important;
          padding: 4px 14px;
          transition: all 0.2s ease;

          &:hover {
            box-shadow: 0 0 0 1px #1677ff inset !important;
          }
        }
      }

      .period-pills-wrap {
        display: inline-flex;
        align-items: center;
        padding: 3px;
        border-radius: 9999px;
        background: #f1f5f9;
        border: 1px solid #e2e8f0;

        .period-pill-btn {
          border: none;
          background: transparent;
          color: #64748b;
          font-size: 12.5px;
          font-weight: 600;
          padding: 5px 12px;
          border-radius: 9999px;
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover {
            color: #1e293b;
          }

          &.is-active {
            background: #ffffff;
            color: #1677ff;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
          }
        }
      }

      .hero-action-buttons {
        display: flex;
        align-items: center;
        gap: 8px;

        .btn-icon-action {
          width: 36px;
          height: 36px;
          background: #ffffff;
          border: 1px solid #e2e8f0;
          color: #475569;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

          &:hover {
            color: #1677ff;
            border-color: #bfdbfe;
            background: #eff6ff;
          }
        }

        .btn-hero-action {
          border-radius: 9999px;
          height: 36px;
          padding: 0 16px;
          font-weight: 600;
          font-size: 13px;
          background: #ffffff;
          border: 1px solid #e2e8f0;
          color: #334155;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
          transition: all 0.2s ease;

          &:hover {
            color: #1677ff;
            border-color: #bfdbfe;
            background: #eff6ff;
          }

          &--primary {
            background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
            border: none;
            color: #ffffff;
            box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);

            &:hover {
              background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
              box-shadow: 0 6px 18px rgba(37, 99, 235, 0.38);
              transform: translateY(-1px);
            }
          }
        }
      }
    }
  }

  .hero-footer-dock {
    position: relative;
    z-index: 1;
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid rgba(226, 232, 240, 0.65);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;

    .status-indicators {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 4px 12px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 500;
        background: rgba(255, 255, 255, 0.85);
        border: 1px solid #e2e8f0;
        color: #475569;

        strong {
          color: #0f172a;
          font-weight: 700;
        }

        .status-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;

          &--active {
            background: #3b82f6;
            box-shadow: 0 0 6px rgba(59, 130, 246, 0.5);
          }
        }

        .pill-icon {
          font-size: 13px;
          color: #64748b;
        }

        &--primary strong {
          color: #1d4ed8;
        }
      }
    }

    .hero-metric-progress {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 12px;
      color: #64748b;

      .progress-title {
        font-weight: 500;
      }

      .progress-track {
        width: 110px;
        height: 6px;
        border-radius: 9999px;
        background: #e2e8f0;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          border-radius: 9999px;
          background: linear-gradient(90deg, #10b981 0%, #3b82f6 100%);
          transition: width 0.5s ease-in-out;
        }

        &--empty {
          background: repeating-linear-gradient(
            90deg,
            #e2e8f0 0 6px,
            #f1f5f9 6px 12px
          );
        }
      }

      .progress-value {
        font-weight: 700;
        color: #0f172a;
        display: inline-flex;
        align-items: baseline;
        gap: 6px;

        &--muted {
          font-weight: 500;
          color: #94a3b8;
        }

        .progress-detail {
          font-style: normal;
          font-weight: 500;
          font-size: 11.5px;
          color: #64748b;
        }
      }
    }
  }
}

@keyframes pulse-dot-anim {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.7;
  }
}

@media (max-width: 960px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start !important;
  }
}
</style>

<template>
  <div class="wrong-question-hero">
    <!-- 柔光微动效光晕 -->
    <div class="glow-orb glow-orb--left" />
    <div class="glow-orb glow-orb--right" />

    <!-- 顶层快速导航与辅助控制坞 -->
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

        <!-- 时间周期切换药丸 -->
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

        <!-- 考点筛选器 -->
        <div v-if="kpOptions && kpOptions.length > 0" class="kp-filter-wrap">
          <el-select
            v-model="selectedKpId"
            clearable
            placeholder="按考点聚焦"
            class="capsule-select capsule-select--kp"
            @change="emit('change-kp', selectedKpId)"
          >
            <el-option
              v-for="kp in kpOptions"
              :key="kp.id"
              :label="formatKpLabel(kp.name, kp.id)"
              :value="kp.id"
            />
          </el-select>
        </div>

      </div>

      <div class="toolbar-right-actions">
        <el-tooltip content="刷新最新错题分析数据" placement="top">
          <button
            type="button"
            class="capsule-btn capsule-btn--icon"
            :disabled="loading"
            @click="emit('refresh')"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg" :class="{ 'spin-anim': loading }" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
          </button>
        </el-tooltip>

        <button
          type="button"
          class="capsule-btn capsule-btn--default"
          @click="emit('export-report')"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          <span>导出分析报告</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--primary ai-btn"
          :disabled="adviceLoading"
          @click="emit('trigger-ai-diagnosis')"
        >
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="currentColor">
            <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
          </svg>
          <span>{{ adviceLoading ? 'AI 正在推演归因中...' : 'AI 班级错因推演' }}</span>
        </button>
      </div>
    </div>

    <!-- 中部主信息行：课程标题与诊断元数据 -->
    <div class="hero-main-row">
      <div class="main-info-col">
        <div class="title-status-line">
          <h1 class="hero-title">错题归因与教学诊断中心</h1>
          <span v-if="courseName" class="course-code-badge">{{ courseName }}</span>
          <span class="status-pill-badge status--active">
            <span class="pulse-dot" />
            <span>学情大数据实时追踪中</span>
          </span>
        </div>

        <div class="hero-meta-badges">
          <div class="meta-badge-item">
            <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span class="meta-label">主讲教师：</span>
            <strong class="meta-value">{{ teacherName || '—' }}</strong>
          </div>

          <div class="meta-badge-item">
            <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            <span class="meta-label">诊断周期：</span>
            <strong class="meta-value">{{ currentRange === '7d' ? '近 7 天' : (currentRange === '30d' ? '近 30 天' : '本学期') }}</strong>
          </div>

          <div class="meta-badge-item">
            <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            <span class="meta-label">班级学生：</span>
            <strong class="meta-value">{{ studentCount || 0 }} 人</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部统计卡片行：高雅长圆微毛玻璃卡片（继承学情概览同款） -->
    <div class="hero-stats-row">
      <div class="hero-stat-card">
        <div class="stat-icon-wrap stat-icon-wrap--danger">
          <svg viewBox="0 0 24 24" class="stat-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
        </div>
        <div class="stat-text-wrap">
          <span class="stat-num text-danger">{{ totalWrongQuestions ?? 0 }} <small>道</small></span>
          <span class="stat-label">高频错题总数</span>
        </div>
      </div>

      <div class="hero-stat-card">
        <div class="stat-icon-wrap stat-icon-wrap--warning">
          <svg viewBox="0 0 24 24" class="stat-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
          </svg>
        </div>
        <div class="stat-text-wrap">
          <span class="stat-num text-warning">{{ (avgErrorRate ?? 0).toFixed(1) }}%</span>
          <span class="stat-label">班级平均错误率</span>
        </div>
      </div>

      <div class="hero-stat-card">
        <div class="stat-icon-wrap stat-icon-wrap--purple">
          <svg viewBox="0 0 24 24" class="stat-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
          </svg>
        </div>
        <div class="stat-text-wrap">
          <span class="stat-num text-purple">{{ totalWrongRecords ?? 0 }} <small>人次</small></span>
          <span class="stat-label">累计失分作答</span>
        </div>
      </div>

      <div class="hero-stat-card">
        <div class="stat-icon-wrap stat-icon-wrap--primary">
          <svg viewBox="0 0 24 24" class="stat-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 2 7 12 12 22 7 12 2" />
            <polyline points="2 17 12 22 22 17" />
            <polyline points="2 12 12 17 22 12" />
          </svg>
        </div>
        <div class="stat-text-wrap">
          <span class="stat-num text-primary">{{ weakKnowledgePointCount ?? 0 }} <small>个</small></span>
          <span class="stat-label">涉及薄弱考点</span>
        </div>
      </div>

      <div class="hero-stat-card">
        <div class="stat-icon-wrap stat-icon-wrap--success">
          <svg viewBox="0 0 24 24" class="stat-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
        </div>
        <div class="stat-text-wrap">
          <span class="stat-num text-success">{{ totalVariantQuestions ?? 0 }} <small>道</small></span>
          <span class="stat-label">已生成巩固变式题</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps<{
  courseId: number;
  courseOptions: Array<{ id: number; name: string }>;
  courseName?: string;
  teacherName?: string;
  studentCount?: number;
  range: string;
  loading?: boolean;
  adviceLoading?: boolean;
  totalWrongQuestions?: number;
  totalWrongRecords?: number;
  avgErrorRate?: number;
  weakKnowledgePointCount?: number;
  totalVariantQuestions?: number;
  kpOptions?: Array<{ id: number; name: string }>;
}>();

const emit = defineEmits<{
  (e: 'change-course', id: number): void;
  (e: 'change-range', range: string): void;
  (e: 'change-kp', kpId: number | undefined): void;
  (e: 'refresh'): void;
  (e: 'export-report'): void;
  (e: 'trigger-ai-diagnosis'): void;
}>();

const router = useRouter();
const currentCourseId = ref(props.courseId);
const currentRange = ref(props.range);
const selectedKpId = ref<number | undefined>(undefined);

watch(() => props.courseId, (val) => {
  currentCourseId.value = val;
});

watch(() => props.range, (val) => {
  currentRange.value = val;
});

const KP_STANDARD_NAMES: Record<number, string> = {
  17: '等价无穷小代换及其应用条件',
  18: '洛必达法则求未定式极限',
  19: '复合函数链式求导法则',
  16: 'ArrayList 与 LinkedList 源码剖析',
  10: '栈与队列的存储结构与特性'
};

function formatKpLabel(name?: string, id?: number): string {
  if (id && KP_STANDARD_NAMES[id]) {
    return KP_STANDARD_NAMES[id];
  }
  if (!name) return '核心考点';
  const cleaned = name.replace(/#/g, '').replace(/\s+/g, ' ').trim();
  const matchId = cleaned.match(/^考点\s*(\d+)$/);
  if (matchId && matchId[1]) {
    const numId = Number(matchId[1]);
    if (KP_STANDARD_NAMES[numId]) {
      return KP_STANDARD_NAMES[numId];
    }
  }
  return cleaned;
}

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
</script>

<style scoped lang="scss">
.wrong-question-hero {
  position: relative;
  width: 100%;
  border-radius: 24px;
  padding: 24px 28px 22px;
  background: linear-gradient(135deg, #EAF3FF 0%, #EEF2FF 45%, #E0E7FF 100%);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 18px;
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
      background: radial-gradient(circle, rgba(59, 130, 246, 0.18) 0%, rgba(59, 130, 246, 0) 70%);
    }

    &--right {
      width: 320px;
      height: 320px;
      right: -80px;
      bottom: -100px;
      background: radial-gradient(circle, rgba(147, 51, 234, 0.15) 0%, rgba(147, 51, 234, 0) 70%);
    }
  }

  .hero-top-toolbar {
    position: relative;
    z-index: 2;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: nowrap;
    gap: 10px;

    .toolbar-left-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: nowrap;
      min-width: 0;
    }

    .toolbar-right-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }
  }

  .capsule-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 36px;
    padding: 0 12px;
    border-radius: 18px;
    font-size: 12.5px;
    font-weight: 500;
    white-space: nowrap;
    flex-shrink: 0;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    border: none;
    outline: none;

    .btn-icon-svg {
      width: 15px;
      height: 15px;
    }

    &--default {
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(8px);
      color: #334155;
      border: 1px solid rgba(203, 213, 225, 0.8);

      &:hover {
        background: #ffffff;
        color: #1e293b;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
      }
    }

    &--icon {
      width: 36px;
      padding: 0;
      justify-content: center;
      background: rgba(255, 255, 255, 0.85);
      border: 1px solid rgba(203, 213, 225, 0.8);
      color: #475569;

      &:hover {
        background: #ffffff;
        color: #2563eb;
      }
    }

    &--primary {
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
      }

      &:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }
    }
  }

  .course-selector-wrap {
    width: 145px;
    flex-shrink: 0;

    .capsule-select {
      width: 100%;
    }

    :deep(.el-select__wrapper) {
      border-radius: 18px !important;
      height: 36px !important;
      padding: 0 10px !important;
      background: rgba(255, 255, 255, 0.9) !important;
      box-shadow: 0 0 0 1px rgba(203, 213, 225, 0.8) inset !important;
      font-size: 13px !important;
    }
  }

  .kp-filter-wrap {
    width: 125px;
    flex-shrink: 0;

    .capsule-select {
      width: 100%;
    }

    :deep(.el-select__wrapper) {
      border-radius: 18px !important;
      height: 36px !important;
      padding: 0 10px !important;
      background: rgba(255, 255, 255, 0.9) !important;
      box-shadow: 0 0 0 1px rgba(203, 213, 225, 0.8) inset !important;
      font-size: 13px !important;
    }
  }


  .range-pill-toggle {
    display: inline-flex;
    background: rgba(255, 255, 255, 0.7);
    padding: 2px;
    border-radius: 18px;
    border: 1px solid rgba(203, 213, 225, 0.7);
    flex-shrink: 0;

    .range-pill-btn {
      border: none;
      background: transparent;
      padding: 4px 10px;
      border-radius: 14px;
      font-size: 12px;
      font-weight: 500;
      color: #64748b;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;

      &.active {
        background: #ffffff;
        color: #2563eb;
        box-shadow: 0 2px 6px rgba(37, 99, 235, 0.15);
        font-weight: 600;
      }
    }
  }

  .hero-main-row {
    position: relative;
    z-index: 2;

    .main-info-col {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .title-status-line {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .hero-title {
      margin: 0;
      font-size: 24px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: -0.02em;
    }

    .course-code-badge {
      padding: 3px 10px;
      background: rgba(255, 255, 255, 0.85);
      border: 1px solid rgba(191, 219, 254, 0.8);
      border-radius: 12px;
      font-size: 13px;
      font-weight: 600;
      color: #1e40af;
    }

    .status-pill-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      background: rgba(240, 253, 244, 0.9);
      border: 1px solid rgba(187, 247, 208, 0.8);
      border-radius: 14px;
      font-size: 12px;
      font-weight: 600;
      color: #15803d;

      .pulse-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #22c55e;
        box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.6);
        animation: pulse-ring 2s infinite;
      }
    }

    .hero-meta-badges {
      display: flex;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;

      .meta-badge-item {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: #475569;

        .meta-svg {
          width: 14px;
          height: 14px;
          color: #64748b;
        }

        .meta-label {
          color: #64748b;
        }

        .meta-value {
          color: #0f172a;
          font-weight: 600;
        }
      }
    }
  }

  .hero-stats-row {
    position: relative;
    z-index: 2;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 14px;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.88);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.95);
      border-radius: 18px;
      padding: 14px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(15, 23, 42, 0.06);
        background: #ffffff;
      }

      .stat-icon-wrap {
        width: 42px;
        height: 42px;
        border-radius: 13px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .stat-svg {
          width: 20px;
          height: 20px;
        }

        &--danger {
          background: #fef2f2;
          color: #ef4444;
        }

        &--warning {
          background: #fffbeb;
          color: #f59e0b;
        }

        &--purple {
          background: #f5f3ff;
          color: #8b5cf6;
        }

        &--primary {
          background: #eff6ff;
          color: #3b82f6;
        }

        &--success {
          background: #f0fdf4;
          color: #10b981;
        }
      }

      .stat-text-wrap {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .stat-num {
          font-size: 20px;
          font-weight: 800;
          line-height: 1.2;

          small {
            font-size: 12px;
            font-weight: 500;
            margin-left: 2px;
          }

          &.text-danger { color: #ef4444; }
          &.text-warning { color: #f59e0b; }
          &.text-purple { color: #8b5cf6; }
          &.text-primary { color: #2563eb; }
          &.text-success { color: #10b981; }
        }

        .stat-label {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
        }
      }
    }
  }
}

.spin-anim {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5); }
  70% { box-shadow: 0 0 0 7px rgba(34, 197, 94, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}
</style>

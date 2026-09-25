<template>
  <div class="ai-usage-hero">
    <!-- 1. 柔光微动效光晕 -->
    <div class="hero-bg-glow hero-bg-glow--blue" />
    <div class="hero-bg-glow hero-bg-glow--purple" />

    <!-- 2. 主体内容区：左侧标题与元数据，右侧操作与筛选 -->
    <div class="hero-content">
      <div class="hero-left">
        <div class="title-row">
          <h2 class="hero-title">
            {{ courseName ? `${courseName} · AI 消耗与调用分析` : 'AI 算力消耗与智能调用分析' }}
          </h2>
          <span v-if="courseCode" class="course-code-badge">{{ courseCode }}</span>
          <span class="status-pill-badge">
            <span class="pulse-dot" />
            <span>实时算力审计中</span>
          </span>
        </div>
        <p class="hero-subtitle">
          全链路真实审计 AI 助学答疑、智能批改、题目解析与备课助手之 Token 消耗、交互延迟与教育场景分布
        </p>
      </div>

      <div class="hero-right">
        <!-- 纯单层课程选择器 -->
        <el-select
          :model-value="courseId"
          placeholder="全部课程"
          clearable
          class="hero-course-select-pure"
          :loading="coursesLoading"
          @update:model-value="emit('change-course', $event ? Number($event) : undefined)"
        >
          <el-option :value="undefined" label="全部课程 (全局视图)" />
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
          <el-tooltip content="刷新最新算力消耗数据" placement="top">
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
            导出审计报表
          </el-button>
        </div>
      </div>
    </div>

    <!-- 3. 底部状态指示坞 -->
    <div class="hero-footer-dock">
      <div class="status-indicators">
        <div class="status-pill status-pill--scope">
          <span class="status-dot status-dot--active" />
          <span>审计范围: <strong>{{ courseName || '全校全部课程' }}</strong></span>
        </div>
        <div class="status-pill status-pill--success">
          <span class="status-dot status-dot--success" />
          <span>模型可用率: <strong>{{ successRate }}%</strong></span>
        </div>
        <div class="status-pill status-pill--latency">
          <el-icon class="pill-icon"><Timer /></el-icon>
          <span>平均交互耗时: <strong>{{ avgLatencyMs }} ms</strong></span>
        </div>
        <div class="status-pill status-pill--saving">
          <el-icon class="pill-icon"><Trophy /></el-icon>
          <span>估算节约教研工时: <strong>{{ totalSavedHours }} 小时</strong></span>
        </div>
        <div class="status-pill status-pill--time">
          <el-icon class="pill-icon"><Clock /></el-icon>
          <span>更新时间: {{ lastUpdatedTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Refresh, Download, Timer, Trophy, Clock } from '@element-plus/icons-vue';

interface CourseOption {
  id: number;
  name: string;
}

const props = withDefaults(
  defineProps<{
    courseId?: number;
    courseName?: string;
    courseCode?: string;
    courseOptions?: CourseOption[];
    coursesLoading?: boolean;
    range?: string;
    loading?: boolean;
    avgLatencyMs?: number;
    successRate?: number;
    totalSavedHours?: number;
  }>(),
  {
    courseId: undefined,
    courseName: '',
    courseCode: '',
    courseOptions: () => [],
    coursesLoading: false,
    range: '7d',
    loading: false,
    avgLatencyMs: 280,
    successRate: 99.8,
    totalSavedHours: 0
  }
);

const emit = defineEmits<{
  (e: 'change-course', val?: number): void;
  (e: 'change-range', val: string): void;
  (e: 'refresh'): void;
  (e: 'export'): void;
}>();

const rangeOptions = [
  { label: '近 24 小时', value: '24h' },
  { label: '近 7 天', value: '7d' },
  { label: '近 30 天', value: '30d' },
  { label: '近学期', value: 'semester' }
];

const lastUpdatedTime = computed(() => {
  const d = new Date();
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`;
});
</script>

<style scoped lang="scss">
.ai-usage-hero {
  position: relative;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.88) 100%);
  border-radius: 20px;
  padding: 24px 28px 18px;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(226, 232, 240, 0.8);
  overflow: hidden;
  backdrop-filter: blur(20px);
  margin-bottom: 20px;

  /* 科技柔光球 */
  .hero-bg-glow {
    position: absolute;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    filter: blur(80px);
    pointer-events: none;
    opacity: 0.12;
    z-index: 0;

    &--blue {
      top: -120px;
      left: -80px;
      background: radial-gradient(circle, #3b82f6 0%, rgba(59, 130, 246, 0) 70%);
    }

    &--purple {
      bottom: -140px;
      right: 10%;
      background: radial-gradient(circle, #8b5cf6 0%, rgba(139, 92, 246, 0) 70%);
    }
  }

  .hero-content {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;
  }

  .hero-left {
    flex: 1;
    min-width: 320px;

    .title-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      margin-bottom: 8px;
    }

    .hero-title {
      font-size: 22px;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: -0.02em;
      margin: 0;
    }

    .course-code-badge {
      display: inline-block;
      padding: 3px 10px;
      font-size: 12px;
      font-family: monospace;
      font-weight: 600;
      color: #2563eb;
      background: #eff6ff;
      border: 1px solid #dbeafe;
      border-radius: 6px;
    }

    .status-pill-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      font-size: 12px;
      font-weight: 600;
      color: #059669;
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.2);
      border-radius: 9999px;

      .pulse-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: #10b981;
        box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
        animation: pulse-green 2s infinite;
      }
    }

    .hero-subtitle {
      margin: 0;
      font-size: 13.5px;
      color: #64748b;
      line-height: 1.5;
      max-width: 680px;
    }
  }

  .hero-right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  /* 纯单层长圆胶囊课程选择器 */
  .hero-course-select-pure {
    width: 220px;

    :deep(.el-input__wrapper) {
      background: #ffffff;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
      border: 1px solid #e2e8f0;
      border-radius: 9999px;
      padding: 4px 14px;
      transition: all 0.2s ease;

      &:hover,
      &.is-focus {
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }
    }
  }

  /* 时间周期长圆药丸按钮组 */
  .period-pills-wrap {
    display: flex;
    align-items: center;
    background: #f1f5f9;
    padding: 4px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;

    .period-pill-btn {
      border: none;
      background: transparent;
      padding: 6px 14px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 500;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: #0f172a;
      }

      &.is-active {
        background: #ffffff;
        color: #2563eb;
        font-weight: 600;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
      }
    }
  }

  .hero-action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;

    .btn-icon-action {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      color: #64748b;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
      width: 36px;
      height: 36px;
      border-radius: 50%;

      &:hover {
        color: #2563eb;
        border-color: #3b82f6;
        background: #f8fafc;
      }
    }

    .btn-hero-action {
      border-radius: 9999px;
      font-weight: 500;
      padding: 8px 18px;
      height: 36px;
      border: 1px solid #e2e8f0;
      color: #334155;
      background: #ffffff;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

      &:hover {
        border-color: #cbd5e1;
        background: #f8fafc;
        color: #0f172a;
      }
    }
  }

  /* 底部状态指示坞 */
  .hero-footer-dock {
    position: relative;
    z-index: 1;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid rgba(226, 232, 240, 0.6);

    .status-indicators {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
    }

    .status-pill {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12.5px;
      color: #64748b;

      strong {
        color: #1e293b;
        font-weight: 600;
      }

      .status-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;

        &--active {
          background-color: #3b82f6;
        }

        &--success {
          background-color: #10b981;
        }
      }

      .pill-icon {
        font-size: 14px;
        color: #64748b;
      }

      &--latency .pill-icon {
        color: #0284c7;
      }

      &--saving .pill-icon {
        color: #8b5cf6;
      }
    }
  }
}

@keyframes pulse-green {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.5);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

@media (max-width: 960px) {
  .ai-usage-hero {
    padding: 20px;

    .hero-content {
      flex-direction: column;
      align-items: flex-start;
    }

    .hero-right {
      width: 100%;
      justify-content: flex-start;
    }
  }
}
</style>

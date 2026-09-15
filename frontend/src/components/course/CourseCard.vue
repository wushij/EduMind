<template>
  <div class="course-card" @click="handleCardClick">
    <!-- 16:9 封面区域 (CARD_COVER) -->
    <div class="cover-wrapper">
      <img
        v-if="(course.coverUrl || course.cover) && !imageError"
        :src="course.coverUrl || course.cover"
        :alt="course.title"
        class="cover-image"
        loading="lazy"
        @error="handleImageError"
      />
      <!-- 无封面时的专属学科渐变占位 -->
      <div v-else class="fallback-gradient" :class="gradientClass">
        <div class="fallback-icon">
          <svg viewBox="0 0 24 24" class="course-svg" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <span class="fallback-title">{{ course.code || 'EduMind 课程' }}</span>
      </div>

      <!-- 顶部浮层标签：状态与课程代号 (均为纯正胶囊药丸) -->
      <div class="cover-badges">
        <span class="pill-badge pill-badge--status" :class="statusClass">
          <span class="status-dot"></span>
          {{ statusText }}
        </span>
        <span v-if="course.code" class="pill-badge pill-badge--code">
          {{ course.code }}
        </span>
      </div>

      <!-- 悬浮微遮罩与快速跳转提示 -->
      <div class="cover-overlay">
        <span class="overlay-action">查看课程空间 →</span>
      </div>
    </div>

    <!-- 卡片主体内容区 -->
    <div class="card-body">
      <!-- 课程名称 -->
      <h3 class="course-title" :title="course.title">
        {{ course.title }}
      </h3>

      <!-- 简介 (两行截断) -->
      <p v-if="course.description" class="course-desc">
        {{ course.description }}
      </p>

      <!-- 学习进度条 (长圆胶囊进度条) -->
      <div v-if="course.progress !== undefined" class="progress-section">
        <div class="progress-label-row">
          <span class="progress-title">学习进度</span>
          <span class="progress-percent">{{ course.progress }}%</span>
        </div>
        <div class="capsule-progress-track">
          <div
            class="capsule-progress-bar"
            :style="{ width: `${Math.min(100, Math.max(0, course.progress))}%` }"
          ></div>
        </div>
      </div>

      <!-- 底部元信息与长圆操作按钮 -->
      <div class="card-footer">
        <div class="teacher-info">
          <div class="teacher-avatar">
            {{ course.teacherName.slice(0, 1) }}
          </div>
          <div class="meta-texts">
            <span class="teacher-name">{{ course.teacherName }}</span>
            <span class="meta-sub">
              {{ course.studentCount }} 人已加入 · {{ course.chapterCount }} 章节
            </span>
          </div>
        </div>

        <button
          type="button"
          class="capsule-action-btn"
          @click.stop="handleEnterCourse"
        >
          <span>进入课程</span>
          <svg viewBox="0 0 24 24" class="btn-arrow-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Course } from '@/types/course/course';
import { useCourse } from '@/composables/course/useCourse';

const props = defineProps<{
  course: Course;
}>();

const router = useRouter();
const { setCurrentCourse } = useCourse();
const imageError = ref(false);

function handleImageError() {
  imageError.value = true;
}

const statusText = computed(() => {
  if (props.course.status === 'ARCHIVED' || props.course.status === 2) {
    return '已结课';
  }
  return '进行中';
});

const statusClass = computed(() => {
  if (props.course.status === 'ARCHIVED' || props.course.status === 2) {
    return 'status-archived';
  }
  return 'status-active';
});

// 根据课程 ID 模数分配专属质感学科渐变
const gradientClass = computed(() => {
  const id = Number(props.course.id || 0);
  const mod = id % 4;
  if (mod === 0) return 'grad-blue';
  if (mod === 1) return 'grad-purple';
  if (mod === 2) return 'grad-cyan';
  return 'grad-indigo';
});

function handleCardClick() {
  setCurrentCourse(props.course);
  router.push(`/course/${props.course.id}/overview`);
}

function handleEnterCourse() {
  setCurrentCourse(props.course);
  router.push(`/course/${props.course.id}/overview`);
}
</script>

<style scoped lang="scss">
.course-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #EBF1F7;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(22, 119, 255, 0.12);
    border-color: #DBEAFE;

    .cover-image {
      transform: scale(1.04);
    }

    .cover-overlay {
      opacity: 1;
    }

    .capsule-action-btn {
      background: #1677FF;
      color: #FFFFFF;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
    }
  }

  // 1. 16:9 封面
  .cover-wrapper {
    position: relative;
    width: 100%;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    background: #F1F5F9;

    .cover-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.35s ease;
    }

    // 渐变学科底图兜底
    .fallback-gradient {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #FFFFFF;

      &.grad-blue {
        background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
      }
      &.grad-purple {
        background: linear-gradient(135deg, #7C3AED 0%, #4F46E5 100%);
      }
      &.grad-cyan {
        background: linear-gradient(135deg, #0284C7 0%, #0D9488 100%);
      }
      &.grad-indigo {
        background: linear-gradient(135deg, #4338CA 0%, #312E81 100%);
      }

      .fallback-icon {
        width: 42px;
        height: 42px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;

        .course-svg {
          width: 22px;
          height: 22px;
        }
      }

      .fallback-title {
        font-size: 14px;
        font-weight: 600;
        letter-spacing: 0.5px;
      }
    }

    // 顶部胶囊药丸徽标
    .cover-badges {
      position: absolute;
      top: 12px;
      left: 12px;
      right: 12px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      z-index: 2;

      .pill-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 24px;
        padding: 0 10px;
        border-radius: 9999px; // 纯正长圆胶囊
        font-size: 11.5px;
        font-weight: 600;
        backdrop-filter: blur(8px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);

        &--status {
          &.status-active {
            background: rgba(16, 185, 129, 0.9);
            color: #FFFFFF;
            .status-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #FFFFFF;
              animation: pulseDot 2s infinite;
            }
          }

          &.status-archived {
            background: rgba(100, 116, 139, 0.85);
            color: #FFFFFF;
            .status-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #E2E8F0;
            }
          }
        }

        &--code {
          background: rgba(15, 23, 42, 0.7);
          color: #F8FAFC;
          letter-spacing: 0.3px;
        }
      }
    }

    // 悬浮快速蒙层
    .cover-overlay {
      position: absolute;
      inset: 0;
      background: rgba(15, 23, 42, 0.35);
      backdrop-filter: blur(2px);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.25s ease;
      z-index: 1;

      .overlay-action {
        color: #FFFFFF;
        font-size: 13.5px;
        font-weight: 600;
        padding: 6px 16px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.25);
        border: 1px solid rgba(255, 255, 255, 0.4);
      }
    }
  }

  // 2. 卡片主体
  .card-body {
    padding: 18px 20px 20px;
    display: flex;
    flex-direction: column;
    flex: 1;

    .course-title {
      margin: 0 0 8px 0;
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .course-desc {
      margin: 0 0 14px 0;
      font-size: 13px;
      color: #64748B;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
      min-height: 38px;
    }

    // 3. 长圆胶囊进度条
    .progress-section {
      margin-bottom: 14px;

      .progress-label-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 11.5px;
        color: #64748B;
        margin-bottom: 5px;

        .progress-percent {
          font-weight: 600;
          color: #1677FF;
        }
      }

      .capsule-progress-track {
        width: 100%;
        height: 6px;
        background: #F1F5F9;
        border-radius: 9999px; // 长圆槽
        overflow: hidden;

        .capsule-progress-bar {
          height: 100%;
          border-radius: 9999px; // 长圆进度
          background: linear-gradient(90deg, #3B82F6 0%, #1677FF 100%);
          transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
        }
      }
    }

    // 4. 底部教师信息与胶囊按钮
    .card-footer {
      margin-top: auto;
      padding-top: 14px;
      border-top: 1px solid #F1F5F9;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;

      .teacher-info {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;

        .teacher-avatar {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background: #E0E7FF;
          color: #4F46E5;
          font-size: 12px;
          font-weight: 600;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .meta-texts {
          display: flex;
          flex-direction: column;
          min-width: 0;

          .teacher-name {
            font-size: 12.5px;
            font-weight: 600;
            color: #1E293B;
            line-height: 1.2;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .meta-sub {
            font-size: 11px;
            color: #94A3B8;
            margin-top: 2px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }

      // 纯正长圆交互按钮
      .capsule-action-btn {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        height: 32px;
        padding: 0 12px;
        border-radius: 9999px; // 长圆胶囊
        background: #F1F5F9;
        color: #334155;
        border: none;
        font-size: 12.5px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.22s ease;
        flex-shrink: 0;

        .btn-arrow-svg {
          width: 14px;
          height: 14px;
          transition: transform 0.2s;
        }

        &:hover {
          background: #1677FF;
          color: #FFFFFF;
          .btn-arrow-svg {
            transform: translateX(2px);
          }
        }
      }
    }
  }
}

@keyframes pulseDot {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}
</style>

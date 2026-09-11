<template>
  <div class="course-detail-container" :class="{ 'course-detail-container--ai': isAiRoute }">
    <!-- 顶部课程上下文信息条 (对齐页面Banner规范，非普通标题) -->
    <div class="course-header-bar">
      <div class="header-left">
        <button type="button" class="back-link" @click="router.push('/course')">
          <svg viewBox="0 0 24 24" class="back-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          <span>返回课程列表</span>
        </button>

        <div class="course-title-row">
          <h1 class="course-title">{{ currentCourse?.title || '正在加载课程...' }}</h1>
          <span class="pill-tag pill-tag--code">{{ currentCourse?.code || 'MATH1001' }}</span>
          <span class="pill-tag pill-tag--status">
            <span class="pulse-circle"></span>
            {{ statusText }}
          </span>
        </div>

        <div class="meta-info-row">
          <span class="meta-item">
            <span class="meta-label">主讲教师：</span>
            <strong class="meta-val">{{ currentCourse?.teacherName || '任课教师' }}</strong>
          </span>
          <span class="divider">/</span>
          <span class="meta-item">
            <span class="meta-label">开课学期：</span>
            <strong class="meta-val">{{ currentCourse?.semester || '2026秋季学期' }}</strong>
          </span>
          <span class="divider">/</span>
          <span class="meta-item">
            <span class="meta-label">修读人数：</span>
            <strong class="meta-val">{{ currentCourse?.studentCount || 0 }} 人</strong>
          </span>
        </div>
      </div>

      <div class="header-right">
        <!-- 重点功能：一键进入课程专属 AI 助教 -->
        <button
          type="button"
          class="ai-assistant-btn"
          @click="navigateToTab('ai')"
        >
          <el-icon class="ai-sparkle-icon"><MagicStick /></el-icon>
          <span>课程专属 AI 助教</span>
          <span class="ai-pill-bubble">SSE 实时解惑</span>
        </button>
      </div>
    </div>

    <!-- 课程空间二级导航 Tab (长圆药丸指示栏) -->
    <div class="course-nav-bar">
      <div class="pill-nav-tabs">
        <router-link
          v-for="item in subTabs"
          :key="item.path"
          :to="item.path"
          class="nav-tab-item"
          :class="{ active: route.path.startsWith(item.path) }"
        >
          <el-icon v-if="item.icon" class="tab-icon">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </div>
    </div>

    <!-- 子路由内容展示区 -->
    <div class="course-subview-content">
      <div class="course-subview-router">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <div v-if="Component" :key="route.path" class="course-subview-inner">
              <component :is="Component" :course="currentCourse" />
            </div>
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useCourse } from '@/composables/course/useCourse';
import {
  Reading,
  Document,
  Connection,
  FolderOpened,
  Service,
  User,
  MagicStick
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const { currentCourse, fetchCourseDetail } = useCourse();

const courseId = computed(() => route.params.id || '101');
const isAiRoute = computed(() => route.path.endsWith('/ai'));

const statusText = computed(() => {
  if (currentCourse.value?.status === 'ARCHIVED' || currentCourse.value?.status === 2) {
    return '已结课';
  }
  return '进行中';
});

const subTabs = computed(() => [
  { label: '课程概览', path: `/course/${courseId.value}/overview`, icon: Reading },
  { label: '大纲与章节', path: `/course/${courseId.value}/chapters`, icon: Document },
  { label: '知识点图谱', path: `/course/${courseId.value}/knowledge-points`, icon: Connection },
  { label: '课件与教学资料', path: `/course/${courseId.value}/resources`, icon: FolderOpened },
  { label: '课程 AI 助教', path: `/course/${courseId.value}/ai`, icon: Service },
  { label: '选课班级成员', path: `/course/${courseId.value}/members`, icon: User }
]);

function navigateToTab(tab: string) {
  router.push(`/course/${courseId.value}/${tab}`);
}

onMounted(() => {
  fetchCourseDetail(courseId.value as string);
});
</script>

<style scoped lang="scss">
.course-detail-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;

  // 1. 顶部课程上下文信息条
  .course-header-bar {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 22px 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);

    .header-left {
      .back-link {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: transparent;
        border: none;
        padding: 0;
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
        cursor: pointer;
        margin-bottom: 8px;
        transition: color 0.2s;

        .back-svg {
          width: 14px;
          height: 14px;
        }

        &:hover {
          color: #1677FF;
        }
      }

      .course-title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .course-title {
          margin: 0;
          font-size: 22px;
          font-weight: 700;
          color: #0F172A;
          letter-spacing: -0.2px;
        }

        .pill-tag {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          height: 24px;
          padding: 0 12px;
          border-radius: 9999px; // 长圆胶囊
          font-size: 12px;
          font-weight: 600;

          &--code {
            background: #F1F5F9;
            color: #475569;
          }

          &--status {
            background: #E8F8F0;
            color: #10B981;

            .pulse-circle {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10B981;
            }
          }
        }
      }

      .meta-info-row {
        margin-top: 10px;
        font-size: 13px;
        color: #64748B;
        display: flex;
        align-items: center;
        gap: 10px;

        .meta-label {
          color: #94A3B8;
        }

        .meta-val {
          color: #334155;
          font-weight: 500;
        }

        .divider {
          color: #CBD5E1;
        }
      }
    }

    .header-right {
      .ai-assistant-btn {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        height: 46px;
        padding: 0 20px;
        border-radius: 9999px; // 胶囊主按钮
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        border: none;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 4px 16px rgba(22, 119, 255, 0.35);
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

        .ai-sparkle-icon {
          font-size: 16px;
        }

        .ai-pill-bubble {
          font-size: 11px;
          font-weight: 600;
          padding: 2px 8px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.22);
          backdrop-filter: blur(4px);
        }

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 24px rgba(114, 46, 209, 0.45);
        }

        &:active {
          transform: translateY(0);
        }
      }
    }
  }

  // 2. 药丸二级导航
  .course-nav-bar {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 6px 10px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 2px 10px rgba(30, 80, 150, 0.03);

    .pill-nav-tabs {
      display: flex;
      align-items: center;
      gap: 6px;
      overflow-x: auto;

      .nav-tab-item {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 16px;
        border-radius: 9999px; // 纯正长圆药丸 Tab
        font-size: 13.5px;
        font-weight: 500;
        color: #64748B;
        text-decoration: none;
        transition: all 0.2s ease;
        white-space: nowrap;

        .tab-icon {
          font-size: 14px;
        }

        &:hover {
          color: #1677FF;
          background: #F8FAFC;
        }

        &.active {
          color: #1677FF;
          background: #EAF3FF;
          font-weight: 600;
        }
      }
    }
  }

  // 3. 子路由内容区
  .course-subview-content {
    min-height: 480px;
    width: 100%;
    min-width: 0;
    max-width: 100%;
    box-sizing: border-box;
  }

  // AI 助教页：精简顶栏并保留充足底部呼吸空间，允许平滑向下滚动
  &.course-detail-container--ai {
    gap: 12px;
    padding-bottom: 32px;

    .course-header-bar {
      display: none;
    }

    .course-nav-bar {
      flex-shrink: 0;
    }

    .course-subview-content {
      width: 100%;
      min-width: 0;
    }
  }
}

// 页面切换微动画
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

<template>
  <div
    class="course-detail-container"
    :class="{
      'course-detail-container--ai': isAiRoute,
      'course-detail-container--immersive': isImmersiveLessonStudio
    }"
  >
    <!-- 顶部课程上下文 Hero Banner (模仿课程中心 PageHeroBanner 视觉设计，AI 工作台下隐藏) -->
    <div v-if="!isImmersiveLessonStudio && !isAiRoute" class="course-hero-header">
      <!-- 柔光微动效光晕 -->
      <div class="glow-orb glow-orb--left"></div>
      <div class="glow-orb glow-orb--right"></div>

      <!-- 顶层快速导航与辅助工具栏 -->
      <div class="hero-top-toolbar">
        <button type="button" class="capsule-btn capsule-btn--default" @click="router.push('/course')">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          <span>返回课程列表</span>
        </button>

        <div class="toolbar-right-actions">
          <button
            v-if="courseEditable"
            type="button"
            class="capsule-btn capsule-btn--default edit-btn"
            title="编辑当前课程档案与AI人设"
            @click="showEditDrawer = true"
          >
            <el-icon class="btn-icon-svg text-primary"><EditPen /></el-icon>
            <span>编辑课程信息</span>
          </button>

          <button
            v-if="courseEditable"
            type="button"
            class="table-action-pill table-action-pill--danger"
            title="归档当前课程"
            @click="handleArchiveCourse"
          >
            归档课程
          </button>

          <button
            v-if="currentCourse?.code"
            type="button"
            class="capsule-btn capsule-btn--default code-btn"
            title="点击复制课程专属代号"
            @click="copyCourseCode"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg text-primary" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
            </svg>
            <span>课程代号：<strong class="code-highlight">{{ currentCourse.code }}</strong></span>
            <span class="copy-tag">复制</span>
          </button>
        </div>
      </div>

      <!-- 中部主信息行：课程标题与 AI 助教操作按钮 -->
      <div class="hero-main-row">
        <div class="main-info-col">
          <div class="title-status-line">
            <h1 v-if="currentCourse?.title" class="hero-course-title">{{ currentCourse.title }}</h1>
            <div v-else class="hero-title-skeleton"></div>

            <span v-if="currentCourse?.code" class="course-code-badge">{{ currentCourse.code }}</span>
            <span class="status-pill-badge" :class="statusClass">
              <span class="pulse-dot"></span>
              <span>{{ statusText }}</span>
            </span>
          </div>

          <div class="hero-meta-badges">
            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <span class="meta-label">主讲教师：</span>
              <strong class="meta-value">{{ currentCourse?.teacherName || '任课教师' }}</strong>
            </div>

            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="16" y1="2" x2="16" y2="6"></line>
                <line x1="8" y1="2" x2="8" y2="6"></line>
                <line x1="3" y1="10" x2="21" y2="10"></line>
              </svg>
              <span class="meta-label">开课学期：</span>
              <strong class="meta-value">{{ currentCourse?.semester || '2026年秋季学期' }}</strong>
            </div>

            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
              <span class="meta-label">修读人数：</span>
              <strong class="meta-value">{{ currentCourse?.studentCount || 0 }} 人</strong>
            </div>
          </div>
        </div>

        <!-- 右侧：长圆专属 AI 助教操作按钮（1:1 继承课程中心 capsule-btn--primary 设计） -->
        <div class="main-action-col">
          <button
            type="button"
            class="capsule-btn capsule-btn--primary"
            @click="navigateToTab('ai')"
          >
            <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="currentColor">
              <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
            </svg>
            <span>课程专属 AI 助教</span>
            <svg viewBox="0 0 24 24" class="btn-arrow-svg" fill="none" stroke="currentColor" stroke-width="2.2">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </button>
        </div>
      </div>

      <!-- 底部统计卡片行（完全模仿课程中心，高雅平滑） -->
      <div class="hero-stats-row">
        <div class="hero-stat-card">
          <span class="stat-num text-primary">{{ currentCourse?.chapterCount || 0 }}</span>
          <span class="stat-label">大纲章节课时</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-success">{{ currentCourse?.knowledgePointCount || 0 }}</span>
          <span class="stat-label">知识点图谱节点</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-warning">{{ currentCourse?.studentCount || 0 }} 人</span>
          <span class="stat-label">已选修读学生</span>
        </div>
        <div v-if="currentCourse?.aiUsageCount != null && currentCourse.aiUsageCount > 0" class="hero-stat-card">
          <span class="stat-num text-info">{{ currentCourse.aiUsageCount }}</span>
          <span class="stat-label">AI 助教累计问答</span>
        </div>
        <div v-else-if="currentCourse?.knowledgeBaseId" class="hero-stat-card">
          <span class="stat-num text-info">{{ kbMountDocCount }}</span>
          <span class="stat-label">知识库文档 · {{ kbMountChunkCount }} 向量切片</span>
        </div>
      </div>
    </div>

    <!-- 课程空间二级导航 Tab (长圆药丸指示栏，在全屏 AI 助教工作台下隐藏，留足垂直空间) -->
    <div v-if="!isImmersiveLessonStudio && !isAiRoute" class="course-nav-bar">
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
    <div class="course-subview-content" :class="{ 'course-subview-content--immersive': isImmersiveLessonStudio }">
      <div class="course-subview-router">
        <router-view v-slot="{ Component, route: childRoute }">
          <transition name="fade-slide" mode="out-in">
            <keep-alive :include="['CourseAI']">
              <component
                :is="Component"
                :key="childRoute.meta?.keepAlive ? childRoute.path : childRoute.fullPath"
                :course="currentCourse"
                class="course-subview-inner"
              />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
    <!-- 课程快速编辑抽屉 -->
    <CourseEditDrawer
      v-model="showEditDrawer"
      :course="currentCourse"
      @saved="handleCourseSaved"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'CourseDetail'
});

import { ref, computed, onMounted, watch, provide } from 'vue';
import { useCourseEditable, courseDetailInjectionKey } from '@/composables/course/useCourseEditable';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useCourse } from '@/composables/course/useCourse';
import { getKnowledgeBaseDetail } from '@/api/knowledge/knowledge-base';
import CourseEditDrawer from '@/components/course/CourseEditDrawer.vue';
import {
  Reading,
  Document,
  Connection,
  FolderOpened,
  Service,
  User,
  EditPen,
  Notebook
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const { currentCourse, fetchCourseDetail, confirmArchiveCourse } = useCourse();
provide(courseDetailInjectionKey, currentCourse);
const courseEditable = useCourseEditable(currentCourse);

async function handleArchiveCourse() {
  if (!currentCourse.value?.id) return;
  await confirmArchiveCourse(
    { id: currentCourse.value.id, title: currentCourse.value.title },
    () => router.push('/course')
  );
}
const showEditDrawer = ref(false);
const kbMountDocCount = ref(0);
const kbMountChunkCount = ref(0);

async function refreshKnowledgeMountSummary() {
  const kbId = currentCourse.value?.knowledgeBaseId;
  if (!kbId) {
    kbMountDocCount.value = 0;
    kbMountChunkCount.value = 0;
    return;
  }
  try {
    const res = await getKnowledgeBaseDetail(kbId);
    const kb = res.data;
    kbMountDocCount.value = kb?.docCount ?? kb?.documentCount ?? 0;
    kbMountChunkCount.value = kb?.chunkCount ?? 0;
  } catch {
    kbMountDocCount.value = 0;
    kbMountChunkCount.value = 0;
  }
}

function handleCourseSaved(updated: any) {
  if (currentCourse.value) {
    Object.assign(currentCourse.value, updated);
  }
  void refreshKnowledgeMountSummary();
}

const courseId = computed(() => (route.params.id ? String(route.params.id) : ''));
const isAiRoute = computed(() => route.path.endsWith('/ai'));
const isImmersiveLessonStudio = computed(() => Boolean(route.meta.immersiveLessonStudio));

const statusText = computed(() => {
  if (
    currentCourse.value?.status === 'ARCHIVED'
    || currentCourse.value?.status === 'INACTIVE'
    || currentCourse.value?.status === 0
    || currentCourse.value?.status === 2
  ) {
    return '已结课';
  }
  return '进行中';
});

const statusClass = computed(() => {
  if (
    currentCourse.value?.status === 'ARCHIVED'
    || currentCourse.value?.status === 'INACTIVE'
    || currentCourse.value?.status === 0
    || currentCourse.value?.status === 2
  ) {
    return 'status--archived';
  }
  return 'status--active';
});

const subTabs = computed(() => [
  { label: '课程概览', path: `/course/${courseId.value}/overview`, icon: Reading },
  { label: '大纲与章节', path: `/course/${courseId.value}/chapters`, icon: Document },
  { label: '知识点图谱', path: `/course/${courseId.value}/knowledge-points`, icon: Connection },
  { label: '课件与教学资料', path: `/course/${courseId.value}/resources`, icon: FolderOpened },
  { label: '课程作业', path: `/course/${courseId.value}/assignments`, icon: Notebook },
  { label: '课程 AI 助教', path: `/course/${courseId.value}/ai`, icon: Service },
  { label: '选课班级成员', path: `/course/${courseId.value}/members`, icon: User }
]);

function navigateToTab(tab: string) {
  router.push(`/course/${courseId.value}/${tab}`);
}

function copyCourseCode() {
  if (!currentCourse.value?.code) return;
  navigator.clipboard.writeText(currentCourse.value.code);
  ElMessage.success(`已复制课程专属代号：${currentCourse.value.code}`);
}

async function loadCourse(id: string) {
  if (!id) return;
  try {
    // 立即基于当前课程知识库ID拉取挂载统计（避免因单例缓存命中直接跳过统计拉取）
    const summaryPromise = refreshKnowledgeMountSummary();
    const course = await fetchCourseDetail(id);
    if (course?.id) {
      localStorage.setItem('edumind_last_course_id', String(course.id));
    }
    await summaryPromise;
    // 课程详情拉取后若更新了知识库绑定，再次确保统计数据为最新
    await refreshKnowledgeMountSummary();
  } catch {
    // 错误处理已在全局拦截器捕获
  }
}

// 仅当 params.id 真实跨课程变更时触发，子 Tab 切换（overview -> chapters 等）时绝不重新触发请求
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId && (!currentCourse.value || String(currentCourse.value.id) !== String(newId))) {
      void loadCourse(String(newId));
    }
  }
);

watch(
  () => currentCourse.value?.knowledgeBaseId,
  (kbId) => {
    if (kbId) {
      void refreshKnowledgeMountSummary();
    } else {
      kbMountDocCount.value = 0;
      kbMountChunkCount.value = 0;
    }
  },
  { immediate: true }
);

onMounted(() => {
  if (courseId.value) {
    void loadCourse(courseId.value);
  }
});
provide('refreshCourseKbSummary', refreshKnowledgeMountSummary);
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

  // 1. 顶部课程上下文 Hero Banner (完全模仿课程中心 PageHeroBanner 视觉设计)
  .course-hero-header {
    position: relative;
    width: 100%;
    border-radius: 24px;
    padding: 20px 28px 18px;
    background: linear-gradient(135deg, #EAF3FF 0%, #EEF2FF 45%, #E0E7FF 100%);
    border: 1px solid rgba(255, 255, 255, 0.85);
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
    overflow: hidden;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 16px;
    transition: all 0.3s ease;

    // 柔和科技梦幻微光球 (继承自 PageHeroBanner)
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

      .toolbar-right-actions {
        display: flex;
        align-items: center;
        gap: 10px;
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

          // 平滑无感骨架条，消除文字硬跳变
          .hero-title-skeleton {
            width: 260px;
            height: 28px;
            border-radius: 8px;
            background: linear-gradient(90deg, rgba(226, 232, 240, 0.6) 25%, rgba(241, 245, 249, 0.9) 50%, rgba(226, 232, 240, 0.6) 75%);
            background-size: 200% 100%;
            animation: shimmer-skeleton 1.5s infinite;
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

            &.status--active {
              background: #E6F7ED;
              color: #16A34A;
              border: 1px solid #BAE8CB;

              .pulse-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #16A34A;
                box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.25);
              }
            }

            &.status--archived {
              background: #F1F5F9;
              color: #64748B;
              border: 1px solid #CBD5E1;

              .pulse-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #94A3B8;
              }
            }
          }
        }

        .hero-meta-badges {
          display: flex;
          align-items: center;
          gap: 16px;
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

      // 右侧操作按钮列
      .main-action-col {
        display: flex;
        align-items: center;
      }
    }

    // 长圆按钮统一样式 (1:1 继承课程中心 PageHeroBanner 按钮规范)
    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      height: 40px;
      padding: 0 22px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
      white-space: nowrap;

      .btn-icon-svg {
        width: 16px;
        height: 16px;
        flex-shrink: 0;

        &.text-primary {
          color: #1677FF;
        }
      }

      .btn-arrow-svg {
        width: 14px;
        height: 14px;
        transition: transform 0.2s ease;
      }

      &--primary {
        background: #1677FF;
        color: #FFFFFF;
        border: none;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
          box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);

          .btn-arrow-svg {
            transform: translateX(3px);
          }
        }

        &:active {
          background: #0958D9;
          transform: translateY(0);
        }
      }

      &--default {
        height: 34px;
        padding: 0 16px;
        font-size: 13px;
        background: rgba(255, 255, 255, 0.88);
        backdrop-filter: blur(8px);
        color: #334155;
        border: 1px solid rgba(226, 232, 240, 0.9);
        box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);

        &:hover {
          border-color: #BFDBFE;
          color: #1677FF;
          background: #FFFFFF;
          transform: translateY(-1px);
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.1);
        }

        &.code-btn {
          .code-highlight {
            color: #1677FF;
            letter-spacing: 0.5px;
            margin-left: 2px;
          }

          .copy-tag {
            font-size: 11px;
            padding: 1px 6px;
            border-radius: 9999px;
            background: #EAF3FF;
            color: #1677FF;
            font-weight: 600;
            margin-left: 4px;
          }
        }
      }
    }

    // 底部统计卡片行 (1:1 继承课程中心 hero-stats-row 规范)
    .hero-stats-row {
      position: relative;
      z-index: 1;
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      margin-top: 2px;

      .hero-stat-card {
        background: rgba(255, 255, 255, 0.92);
        backdrop-filter: blur(8px);
        padding: 6px 20px;
        border-radius: 9999px;
        border: 1.5px solid rgba(22, 119, 255, 0.12);
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
        display: flex;
        align-items: center;
        gap: 10px;
        transition: all 0.25s ease;

        &:hover {
          background: #FFFFFF;
          border-color: #1677FF;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
        }

        .stat-num {
          font-size: 18px;
          font-weight: 800;
          line-height: 1;

          &.text-primary { color: #2563EB; }
          &.text-success { color: #16A34A; }
          &.text-warning { color: #D97706; }
          &.text-info { color: #0284C7; }
        }

        .stat-label {
          font-size: 12.5px;
          font-weight: 500;
          color: #475569;
          white-space: nowrap;
        }
      }
    }
  }

  // 2. 药丸二级导航
  .course-nav-bar {
    background: #FFFFFF;
    border-radius: 16px;
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
        border-radius: 9999px;
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

  // AI 助教页：精简顶栏并自适应撑满高度，消除顶部与底部多余空白
  &.course-detail-container--ai {
    gap: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    height: 100%;

    .course-hero-header,
    .course-nav-bar {
      display: none !important;
    }

    .course-subview-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
      height: 100%;
      width: 100%;
      min-width: 0;

      .course-subview-router,
      .course-subview-inner {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-height: 0;
        height: 100%;
      }
    }
  }

  &.course-detail-container--immersive {
    gap: 0;

    .course-subview-content--immersive {
      min-height: calc(100vh - 72px);
      padding: 0;
      max-width: none;
    }

    .course-subview-inner {
      max-width: none;
      padding: 0;
    }
  }
}

// 骨架屏发光微动画
@keyframes shimmer-skeleton {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

// 星芒微呼吸旋转
@keyframes pulse-spin {
  0% { transform: scale(1) rotate(0deg); opacity: 0.9; }
  50% { transform: scale(1.1) rotate(20deg); opacity: 1; }
  100% { transform: scale(1) rotate(-10deg); opacity: 0.9; }
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

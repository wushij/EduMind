<template>
  <div class="learning-tasks-page" v-loading="loading">
    <!-- 顶部现代沉浸式 HUD 仪表盘 -->
    <div class="tasks-hero-panel">
      <div class="hero-left">
        <div class="hero-badge">
          <el-icon class="mr-1 text-primary"><Calendar /></el-icon>
          学期任务看板
        </div>
        <h1 class="hero-title">学习任务中心 · 课程作业</h1>
        <p class="hero-subtitle">
          高效规划每一份作业与阶段测验，提交后由 EduMind AI 引擎实时生成诊断报告与解题思路解析。
        </p>
      </div>

      <!-- 4 大多维指标卡 -->
      <div class="hero-stats-grid">
        <!-- 综合完成进度环 -->
        <div class="hud-card hud-progress-card">
          <div class="hud-circle-wrapper">
            <el-progress
              type="circle"
              :percentage="completionPercentage"
              :width="68"
              :stroke-width="7"
              color="#2563eb"
            />
          </div>
          <div class="hud-text">
            <span class="hud-label">总完成度</span>
            <span class="hud-val">{{ completionPercentage }}%</span>
            <span class="hud-sub">已达成 {{ completedCount }}/{{ tasks.length }} 项</span>
          </div>
        </div>

        <!-- 待完成任务 -->
        <div class="hud-card">
          <div class="stat-icon-wrap pending-bg">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="hud-text">
            <span class="hud-label">待完成作业</span>
            <span class="hud-val text-warning">{{ pendingCount }}</span>
            <span class="hud-sub">
              <span v-if="urgentCount > 0" class="urgent-tip">包含 {{ urgentCount }} 项近期截止</span>
              <span v-else>暂无急迫任务</span>
            </span>
          </div>
        </div>

        <!-- 已完成评阅 -->
        <div class="hud-card">
          <div class="stat-icon-wrap completed-bg">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="hud-text">
            <span class="hud-label">已提交评阅</span>
            <span class="hud-val text-success">{{ completedCount }}</span>
            <span class="hud-sub">可随时复盘成绩</span>
          </div>
        </div>

        <!-- 关联课程数 -->
        <div class="hud-card">
          <div class="stat-icon-wrap course-bg">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="hud-text">
            <span class="hud-label">关联课程</span>
            <span class="hud-val text-info">{{ activeCoursesCount }}</span>
            <span class="hud-sub">覆盖当前在读学期</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选、搜索与工具控制栏 -->
    <div class="tasks-control-bar">
      <!-- 状态标签页切换 -->
      <div class="filter-tabs">
        <button
          type="button"
          class="tab-btn"
          :class="{ 'is-active': filterStatus === 'ALL' }"
          @click="filterStatus = 'ALL'"
        >
          全部任务 <span class="badge">{{ tasks.length }}</span>
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ 'is-active': filterStatus === 'PENDING' }"
          @click="filterStatus = 'PENDING'"
        >
          待完成 <span class="badge badge-pending">{{ pendingCount }}</span>
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ 'is-active': filterStatus === 'URGENT' }"
          @click="filterStatus = 'URGENT'"
        >
          即将截止 <span class="badge badge-urgent">{{ urgentCount }}</span>
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ 'is-active': filterStatus === 'COMPLETED' }"
          @click="filterStatus = 'COMPLETED'"
        >
          已达成 <span class="badge badge-completed">{{ completedCount }}</span>
        </button>
      </div>

      <!-- 右侧筛选与搜索 -->
      <div class="tools-group">
        <!-- 关键字搜索 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索作业或试卷名称..."
          clearable
          class="search-input"
          :prefix-icon="Search"
        />

        <!-- 课程过滤 -->
        <el-select
          v-model="courseFilter"
          placeholder="全部课程"
          class="course-select"
          clearable
          @change="onCourseFilterChange"
        >
          <el-option label="全部课程" :value="undefined" />
          <el-option
            v-for="c in courseOptions"
            :key="c.id"
            :label="c.name || c.title || ('课程 #' + c.id)"
            :value="c.id"
          />
        </el-select>

        <!-- 排序方式 -->
        <el-select v-model="sortOrder" placeholder="排序方式" class="sort-select">
          <el-option label="截止时间临近" value="DUE_ASC" />
          <el-option label="最新发布优先" value="DATE_DESC" />
          <el-option label="最高分值优先" value="SCORE_DESC" />
        </el-select>
      </div>
    </div>

    <!-- 任务卡片流 -->
    <div v-if="filteredTasks.length > 0" class="tasks-grid">
      <div
        v-for="t in filteredTasks"
        :key="t.id"
        class="task-card-card"
        :class="{
          'is-completed': t.status === 'COMPLETED',
          'is-urgent': isTaskUrgent(t)
        }"
      >
        <!-- 左侧色彩条 -->
        <div class="color-stripe" :style="{ background: getCourseColor(t.courseId) }"></div>

        <div class="card-main-content">
          <!-- 头部：课程标签与截止状态胶囊 -->
          <div class="card-meta-top">
            <div class="course-pill">
              <span class="course-dot" :style="{ background: getCourseColor(t.courseId) }"></span>
              <span class="course-title">{{ t.courseName }}</span>
            </div>

            <!-- 动态截止倒计时状态胶囊 -->
            <div class="deadline-badge" :class="getDeadLineBadgeClass(t)">
              <el-icon class="mr-1"><Timer /></el-icon>
              <span>{{ getDeadlineDisplayText(t) }}</span>
            </div>
          </div>

          <!-- 任务标题 -->
          <div class="card-title-row">
            <h3 class="task-title" :title="t.title">
              {{ t.title }}
            </h3>
          </div>

          <!-- 任务属性标签 -->
          <div class="task-specs-row">
            <span class="spec-item">
              <el-icon><Trophy /></el-icon>
              满分 {{ t.totalScore || 100 }} 分
            </span>
            <span class="spec-item">
              <el-icon><Clock /></el-icon>
              预计用时 {{ t.estimatedMinutes }} 分钟
            </span>
            <span class="spec-item ai-grade-chip">
              <el-icon><Cpu /></el-icon>
              AI 智能快评
            </span>
          </div>

          <el-divider class="my-3" />

          <!-- 底部动作条 -->
          <div class="card-bottom-row">
            <div class="submission-status-hint">
              <template v-if="t.status === 'COMPLETED'">
                <span class="text-success"><el-icon><CircleCheckFilled /></el-icon> 作业已提交完成</span>
              </template>
              <template v-else>
                <span v-if="isTaskUrgent(t)" class="text-urgent">
                  <el-icon><WarningFilled /></el-icon> 即将截止，请及时作答
                </span>
                <span v-else class="text-muted">
                  截止时间：{{ t.dueDate }}
                </span>
              </template>
            </div>

            <div class="card-actions">
              <el-button
                v-if="t.status === 'PENDING'"
                type="primary"
                class="btn-start"
                @click="handleEnterTask(t)"
              >
                <el-icon class="mr-1"><Edit /></el-icon>
                立即开始
              </el-button>
              <el-button
                v-else
                type="info"
                plain
                class="btn-result"
                @click="handleReviewTask(t)"
              >
                <el-icon class="mr-1"><Document /></el-icon>
                查看评阅报告
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state-card">
      <el-empty description="当前无符合条件的学习任务">
        <template #image>
          <div class="empty-icon-wrap">
            <el-icon><DocumentChecked /></el-icon>
          </div>
        </template>
        <p class="empty-tip-text">
          {{ searchKeyword ? '未找到相关名称的作业，可尝试清除筛选关键词。' : '全部已发布的课程作业已顺利搞定，去课程中心探索更多知识吧！' }}
        </p>
        <el-button v-if="searchKeyword || courseFilter" plain @click="resetFilters">重置筛选</el-button>
        <el-button v-else type="primary" plain @click="router.push('/course')">前往课程中心</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Calendar,
  Clock,
  CircleCheck,
  CircleCheckFilled,
  Reading,
  Timer,
  Trophy,
  Cpu,
  WarningFilled,
  Edit,
  Document,
  DocumentChecked,
  Search
} from '@element-plus/icons-vue';
import { useLearningTasks, type LearningTaskItem } from '@/composables/learning/useLearningTasks';

const router = useRouter();
const {
  loading,
  tasks,
  courseOptions,
  pendingCount,
  completedCount,
  completionPercentage,
  loadCourseOptions,
  fetchTasks
} = useLearningTasks();

const filterStatus = ref<'ALL' | 'PENDING' | 'URGENT' | 'COMPLETED'>('ALL');
const courseFilter = ref<number | undefined>(undefined);
const searchKeyword = ref('');
const sortOrder = ref<'DUE_ASC' | 'DATE_DESC' | 'SCORE_DESC'>('DUE_ASC');

// 紧急任务数量 (截止时间在 3 天内且待完成)
const urgentCount = computed(() => {
  return tasks.value.filter((t) => t.status === 'PENDING' && isTaskUrgent(t)).length;
});

// 关联不同课程数
const activeCoursesCount = computed(() => {
  const cids = new Set(tasks.value.map((t) => t.courseId).filter(Boolean));
  return cids.size || (courseOptions.value?.length || 1);
});

// 判断某个任务是否紧急
function isTaskUrgent(t: LearningTaskItem): boolean {
  if (t.status === 'COMPLETED') return false;
  if (!t.rawDeadline && !t.dueDate) return false;
  const targetStr = t.rawDeadline || t.dueDate;
  const deadlineTime = new Date(targetStr).getTime();
  if (isNaN(deadlineTime)) return false;
  const now = Date.now();
  const diffHours = (deadlineTime - now) / (1000 * 3600);
  return diffHours >= 0 && diffHours <= 72; // 72小时以内
}

// 截止时间显示文案
function getDeadlineDisplayText(t: LearningTaskItem): string {
  if (t.status === 'COMPLETED') return '已完成作答';
  if (!t.rawDeadline && !t.dueDate) return '无截止限制';
  const targetStr = t.rawDeadline || t.dueDate;
  const deadlineTime = new Date(targetStr).getTime();
  if (isNaN(deadlineTime)) return `截止：${t.dueDate}`;
  const now = Date.now();
  const diffHours = (deadlineTime - now) / (1000 * 3600);

  if (diffHours < 0) return '已逾期截止';
  if (diffHours < 24) {
    const hours = Math.max(1, Math.floor(diffHours));
    return `今日截止 · 剩 ${hours} 小时`;
  }
  const days = Math.ceil(diffHours / 24);
  if (days <= 3) {
    return `仅剩 ${days} 天截止`;
  }
  return `截止：${t.dueDate.slice(5)}`;
}

// 截止标签样式类型
function getDeadLineBadgeClass(t: LearningTaskItem): string {
  if (t.status === 'COMPLETED') return 'is-done';
  if (!t.rawDeadline && !t.dueDate) return 'is-normal';
  const targetStr = t.rawDeadline || t.dueDate;
  const deadlineTime = new Date(targetStr).getTime();
  if (isNaN(deadlineTime)) return 'is-normal';
  const now = Date.now();
  const diffHours = (deadlineTime - now) / (1000 * 3600);

  if (diffHours < 0) return 'is-overdue';
  if (diffHours <= 24) return 'is-critical';
  if (diffHours <= 72) return 'is-warning';
  return 'is-normal';
}

// 课程主题色彩映射
const COURSE_COLORS = [
  '#2563eb', // 蓝
  '#7c3aed', // 紫
  '#059669', // 绿
  '#d97706', // 琥珀
  '#0284c7', // 浅蓝
  '#db2777'  // 玫红
];

function getCourseColor(courseId?: number): string {
  if (!courseId) return COURSE_COLORS[0];
  return COURSE_COLORS[Math.abs(courseId) % COURSE_COLORS.length];
}

// 过滤与排序
const filteredTasks = computed(() => {
  let list = tasks.value.filter((t) => {
    // 状态过滤
    if (filterStatus.value === 'PENDING' && t.status !== 'PENDING') return false;
    if (filterStatus.value === 'COMPLETED' && t.status !== 'COMPLETED') return false;
    if (filterStatus.value === 'URGENT' && (!isTaskUrgent(t) || t.status === 'COMPLETED')) return false;

    // 关键词搜索
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const matchTitle = t.title.toLowerCase().includes(kw);
      const matchCourse = t.courseName.toLowerCase().includes(kw);
      if (!matchTitle && !matchCourse) return false;
    }

    return true;
  });

  // 排序
  list = [...list].sort((a, b) => {
    if (sortOrder.value === 'DUE_ASC') {
      const timeA = new Date(a.rawDeadline || a.dueDate).getTime() || 9999999999999;
      const timeB = new Date(b.rawDeadline || b.dueDate).getTime() || 9999999999999;
      return timeA - timeB;
    }
    if (sortOrder.value === 'SCORE_DESC') {
      return (b.totalScore || 0) - (a.totalScore || 0);
    }
    return b.id - a.id;
  });

  return list;
});

function resetFilters() {
  searchKeyword.value = '';
  courseFilter.value = undefined;
  filterStatus.value = 'ALL';
  fetchTasks();
}

function handleEnterTask(t: LearningTaskItem) {
  router.push(`/learning/assignments/${t.assignmentId}/take`);
}

function handleReviewTask(t: LearningTaskItem) {
  router.push(`/learning/assignments/${t.assignmentId}/result`);
}

async function onCourseFilterChange() {
  await fetchTasks(courseFilter.value);
}

onMounted(async () => {
  await loadCourseOptions();
  await fetchTasks();
});
</script>

<style scoped lang="scss">
.learning-tasks-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 60px;
  max-width: 1440px;
  margin: 0 auto;
}

// 顶部现代沉浸式 HUD 仪表盘
.tasks-hero-panel {
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f2fe 50%, #f5f9ff 100%);
  border: 1px solid #dbeafe;
  border-radius: 18px;
  padding: 28px 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  box-shadow: 0 4px 20px rgba(37, 99, 235, 0.05);

  .hero-left {
    .hero-badge {
      display: inline-flex;
      align-items: center;
      background: rgba(37, 99, 235, 0.1);
      color: #1d4ed8;
      font-size: 13px;
      font-weight: 600;
      padding: 4px 12px;
      border-radius: 20px;
      margin-bottom: 10px;
    }

    .hero-title {
      margin: 0 0 8px;
      font-size: 24px;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: -0.02em;
    }

    .hero-subtitle {
      margin: 0;
      font-size: 14px;
      color: #475569;
      max-width: 760px;
      line-height: 1.6;
    }
  }

  // 4 大多维指标卡
  .hero-stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    .hud-card {
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.08);
        border-color: #cbd5e1;
      }

      &.hud-progress-card {
        background: #ffffff;
      }

      .stat-icon-wrap {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        flex-shrink: 0;

        &.pending-bg {
          background: #fff7ed;
          color: #ea580c;
        }

        &.completed-bg {
          background: #f0fdf4;
          color: #16a34a;
        }

        &.course-bg {
          background: #f0f9ff;
          color: #0284c7;
        }
      }

      .hud-text {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;

        .hud-label {
          font-size: 13px;
          color: #64748b;
          font-weight: 500;
        }

        .hud-val {
          font-size: 22px;
          font-weight: 700;
          color: #0f172a;
          line-height: 1.2;

          &.text-warning { color: #ea580c; }
          &.text-success { color: #16a34a; }
          &.text-info { color: #0284c7; }
        }

        .hud-sub {
          font-size: 12px;
          color: #94a3b8;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;

          .urgent-tip {
            color: #dc2626;
            font-weight: 600;
          }
        }
      }
    }
  }
}

// 筛选与搜索控制栏
.tasks-control-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px 18px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);

  .filter-tabs {
    display: flex;
    align-items: center;
    gap: 8px;

    .tab-btn {
      background: none;
      border: none;
      font-size: 14px;
      font-weight: 500;
      color: #64748b;
      padding: 8px 14px;
      border-radius: 8px;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s ease;

      &:hover {
        color: #2563eb;
        background: #f1f5f9;
      }

      &.is-active {
        color: #2563eb;
        background: #eff6ff;
        font-weight: 600;

        .badge {
          background: #2563eb;
          color: #ffffff;
        }
      }

      .badge {
        font-size: 12px;
        background: #f1f5f9;
        color: #64748b;
        padding: 1px 7px;
        border-radius: 10px;
        font-weight: 600;

        &.badge-pending {
          background: #ffedd5;
          color: #c2410c;
        }

        &.badge-urgent {
          background: #fee2e2;
          color: #dc2626;
        }

        &.badge-completed {
          background: #dcfce7;
          color: #15803d;
        }
      }
    }
  }

  .tools-group {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .search-input {
      width: 220px;
      --el-input-border-radius: 8px;
    }

    .course-select {
      width: 170px;
      --el-border-radius-base: 8px;
    }

    .sort-select {
      width: 150px;
      --el-border-radius-base: 8px;
    }
  }
}

// 任务卡片网格流
.tasks-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;

  .task-card-card {
    position: relative;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
    overflow: hidden;
    display: flex;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: #cbd5e1;
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.07);
    }

    &.is-urgent {
      border-color: #fecaca;
      background: linear-gradient(to right, #ffffff, #fffdfd);
    }

    &.is-completed {
      opacity: 0.92;
      background: #fafbfc;
    }

    .color-stripe {
      width: 5px;
      flex-shrink: 0;
    }

    .card-main-content {
      flex: 1;
      padding: 20px 22px;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .card-meta-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 12px;

      .course-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        padding: 3px 10px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;
        color: #334155;

        .course-dot {
          width: 7px;
          height: 7px;
          border-radius: 50%;
        }

        .course-title {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 220px;
        }
      }

      .deadline-badge {
        display: inline-flex;
        align-items: center;
        font-size: 12px;
        font-weight: 600;
        padding: 3px 10px;
        border-radius: 6px;

        &.is-critical {
          background: #fef2f2;
          color: #dc2626;
          border: 1px solid #fecaca;
        }

        &.is-warning {
          background: #fffbeb;
          color: #d97706;
          border: 1px solid #fde68a;
        }

        &.is-normal {
          background: #f1f5f9;
          color: #475569;
        }

        &.is-done {
          background: #f0fdf4;
          color: #16a34a;
        }

        &.is-overdue {
          background: #fef2f2;
          color: #991b1b;
        }
      }
    }

    .card-title-row {
      margin-bottom: 12px;

      .task-title {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        line-height: 1.45;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        transition: color 0.2s;

        &:hover {
          color: #2563eb;
        }
      }
    }

    .task-specs-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
      font-size: 13px;
      color: #64748b;

      .spec-item {
        display: inline-flex;
        align-items: center;
        gap: 5px;
      }

      .ai-grade-chip {
        color: #7c3aed;
        background: #f5f3ff;
        padding: 1px 8px;
        border-radius: 4px;
        font-weight: 500;
        font-size: 12px;
      }
    }

    .card-bottom-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;

      .submission-status-hint {
        font-size: 13px;

        .text-success {
          color: #16a34a;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-weight: 500;
        }

        .text-urgent {
          color: #dc2626;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-weight: 600;
        }

        .text-muted {
          color: #64748b;
        }
      }

      .card-actions {
        .btn-start {
          font-weight: 600;
          border-radius: 8px;
          padding: 0 18px;
          background: linear-gradient(135deg, #2563eb, #1d4ed8);
          border: none;
          box-shadow: 0 3px 10px rgba(37, 99, 235, 0.25);
          transition: all 0.2s;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 5px 14px rgba(37, 99, 235, 0.35);
          }
        }

        .btn-result {
          font-weight: 500;
          border-radius: 8px;
        }
      }
    }
  }
}

// 空状态卡片
.empty-state-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 60px 20px;
  text-align: center;

  .empty-icon-wrap {
    font-size: 48px;
    color: #94a3b8;
    margin-bottom: 12px;
  }

  .empty-tip-text {
    font-size: 14px;
    color: #64748b;
    margin-bottom: 16px;
  }
}

@media (max-width: 1024px) {
  .tasks-hero-panel .hero-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .tasks-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .tasks-hero-panel .hero-stats-grid {
    grid-template-columns: 1fr;
  }

  .tasks-control-bar {
    flex-direction: column;
    align-items: stretch;

    .tools-group {
      flex-direction: column;
      .search-input, .course-select, .sort-select {
        width: 100%;
      }
    }
  }
}
</style>


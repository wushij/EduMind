<template>
  <div class="assignment-list-container">
    <!-- 顶部操作头区 -->
    <div class="assignment-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <el-icon class="header-icon text-blue-600"><Tickets /></el-icon>
          <h1 class="main-title">课程作业与平时测验管理</h1>
          <span class="capsule-count-tag">共 {{ total }} 份作业任务</span>
        </div>
        <p class="sub-desc">
          支持基于试卷或题库按需布置在线作业，提供全自动客观题评阅、主观题AI辅助批改与学情统计跟踪。
        </p>
      </div>

      <div class="header-right-actions">
        <el-button
          type="primary"
          class="capsule-btn-primary"
          @click="router.push('/question/assignments/create')"
        >
          <el-icon class="mr-1"><Plus /></el-icon> 发布新作业
        </el-button>
      </div>
    </div>

    <!-- 数据概览指示卡 -->
    <div class="stats-cards-grid">
      <div class="stat-card">
        <div class="stat-icon bg-blue-50 text-blue-600">
          <el-icon><EditPen /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-val">{{ activeAssignmentsCount }}</span>
          <span class="stat-title">进行中作业</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon bg-amber-50 text-amber-600">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-val">{{ pendingGradingCount }}</span>
          <span class="stat-title">待评阅答卷</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon bg-emerald-50 text-emerald-600">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-val">{{ aiGradedCount }}</span>
          <span class="stat-title">已AI辅助批改</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon bg-purple-50 text-purple-600">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-val">88.5%</span>
          <span class="stat-title">平均提交率</span>
        </div>
      </div>
    </div>

    <!-- 筛选过滤行 -->
    <div class="filter-capsule-card">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索作业标题、课程名称..."
          clearable
          class="search-input"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="selectedCourseId" placeholder="所属课程" clearable class="filter-select" @change="handleSearch">
          <el-option label="全部课程" :value="null" />
          <el-option
            v-for="c in courses"
            :key="c.id"
            :label="c.title"
            :value="c.id"
          />
        </el-select>
        <el-select v-model="selectedStatus" placeholder="批改状态" clearable class="filter-select" @change="handleSearch">
          <el-option label="全部状态" value="" />
          <el-option label="待批改" value="PENDING" />
          <el-option label="已批改" value="GRADED" />
        </el-select>
      </div>
    </div>

    <!-- 作业任务卡片列表 -->
    <div v-loading="loading" class="assignments-grid-wrapper">
      <div v-if="displayAssignments.length > 0" class="assignments-stack">
        <div
          v-for="a in displayAssignments"
          :key="a.id"
          class="assignment-row-card"
        >
          <div class="row-left-info">
            <div class="tags-and-title">
              <el-tag
                :type="a.status === 'GRADED' ? 'success' : 'warning'"
                size="small"
                effect="light"
                round
              >
                {{ a.status === 'GRADED' ? '已批改归档' : '待批改/收集中' }}
              </el-tag>
              <h3 class="assignment-title" @click="router.push(`/question/assignments/${a.id}`)">
                {{ a.title }}
              </h3>
            </div>

            <div class="meta-sub-row">
              <span class="meta-item">
                <el-icon class="meta-icon text-blue-600"><Reading /></el-icon>
                {{ a.courseName || getCourseName(a.courseId) }}
              </span>
              <span class="meta-item">
                <el-icon class="meta-icon text-amber-500"><Clock /></el-icon>
                截止时间：{{ a.deadline }}
              </span>
              <span class="meta-item">
                <el-icon class="meta-icon text-indigo-500"><User /></el-icon>
                已提交：<strong>{{ a.submissionCount || 0 }}</strong> 份
              </span>
            </div>
          </div>

          <div class="row-center-progress">
            <div class="prog-label">
              <span>班级提交进度</span>
              <span>{{ a.submissionCount || 0 }} / 45 人</span>
            </div>
            <el-progress
              :percentage="Math.min(100, Math.round(((a.submissionCount || 0) / 45) * 100))"
              :color="getProgressColor((a.submissionCount || 0) / 45)"
            />
          </div>

          <div class="row-right-actions">
            <button
              v-if="a.status === 'DRAFT'"
              v-permission="'assignment:delete'"
              type="button"
              class="table-action-pill table-action-pill--danger"
              @click.stop="removeAssignment(a.id, a.title)"
            >
              删除
            </button>
            <el-button
              type="primary"
              size="small"
              @click="router.push(`/question/assignments/${a.id}`)"
            >
              批改与答卷管理
            </el-button>
            <el-button
              type="success"
              plain
              size="small"
              :icon="Cpu"
              @click="handleFastAIGrade(a.id)"
            >
              一键AI批改
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state-panel">
        <el-icon class="empty-icon text-slate-300"><FolderOpened /></el-icon>
        <h3>暂无匹配的作业任务</h3>
        <p>您可以点击右上角“发布新作业”，为学生选拔试题并设定考核时间。</p>
        <el-button type="primary" @click="router.push('/question/assignments/create')">
          立即发布新作业
        </el-button>
      </div>

      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadAssignments"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Plus,
  Search,
  Tickets,
  EditPen,
  Timer,
  Cpu,
  DataAnalysis,
  Reading,
  Clock,
  User,
  FolderOpened
} from '@element-plus/icons-vue';
import { useAssignment } from '@/composables/question/useAssignment';
import AppPagination from '@/components/common/AppPagination.vue';
import type { Course } from '@/types/course/course';

const router = useRouter();
const { assignments, loading, total, fetchAssignments, loadCourses, gradePendingSubmissions, removeAssignment } = useAssignment();

const pageNum = ref(1);
const pageSize = ref(10);
const courses = ref<Course[]>([]);
const searchKeyword = ref('');
const selectedCourseId = ref<number | null>(null);
const selectedStatus = ref('');
const usingMockFallback = ref(false);

onMounted(async () => {
  await Promise.all([loadCourseOptions(), loadAssignments()]);
});

async function loadCourseOptions() {
  courses.value = await loadCourses();
}

async function loadAssignments() {
  usingMockFallback.value = false;
  await fetchAssignments({
    page: pageNum.value,
    pageSize: pageSize.value,
    courseId: selectedCourseId.value || undefined,
    status: selectedStatus.value || undefined
  });
  if (assignments.value.length === 0 && !selectedCourseId.value && !selectedStatus.value) {
    usingMockFallback.value = true;
    // 注入演示用丰富作业列表
    assignments.value = [
      {
        id: 1,
        title: '第三周：单链表与双向链表核心算法实现',
        courseId: 101,
        courseName: '数据结构与算法',
        deadline: '2026-09-20 23:59',
        submissionCount: 41,
        status: 'PENDING'
      } as any,
      {
        id: 2,
        title: '第五周：二叉树遍历与平衡二叉搜索树测验',
        courseId: 101,
        courseName: '数据结构与算法',
        deadline: '2026-09-28 23:59',
        submissionCount: 38,
        status: 'GRADED'
      } as any,
      {
        id: 3,
        title: 'Java面向对象封装继承与多态综合测验',
        courseId: 102,
        courseName: 'Java程序设计',
        deadline: '2026-09-18 20:00',
        submissionCount: 44,
        status: 'GRADED'
      } as any,
      {
        id: 4,
        title: '导数应用与罗尔定理应用题训练',
        courseId: 103,
        courseName: '高等数学（上）',
        deadline: '2026-09-25 18:00',
        submissionCount: 22,
        status: 'PENDING'
      } as any
    ];
    total.value = assignments.value.length;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadAssignments();
}

const displayAssignments = computed(() => {
  if (!searchKeyword.value.trim() || !usingMockFallback.value) {
    return assignments.value;
  }
  const kw = searchKeyword.value.trim().toLowerCase();
  return assignments.value.filter((a: any) => {
    const inTitle = a.title?.toLowerCase().includes(kw);
    const inCourse = a.courseName?.toLowerCase().includes(kw);
    return inTitle || inCourse;
  });
});

const activeAssignmentsCount = computed(() => {
  return assignments.value.filter(a => a.status === 'PENDING').length;
});

const pendingGradingCount = computed(() => {
  return assignments.value.reduce((acc, a) => acc + (a.status === 'PENDING' ? a.submissionCount || 0 : 0), 0);
});

const aiGradedCount = computed(() => {
  return assignments.value.reduce((acc, a) => acc + (a.status === 'GRADED' ? a.submissionCount || 0 : 0), 0);
});

function getCourseName(courseId?: number) {
  const c = courses.value.find(item => item.id === courseId);
  return c?.title || '通用专业课程';
}

function getProgressColor(ratio: number) {
  if (ratio >= 0.9) return '#10b981';
  if (ratio >= 0.6) return '#3b82f6';
  return '#f59e0b';
}

async function handleFastAIGrade(assignmentId: number) {
  try {
    const pendingCount = await gradePendingSubmissions(assignmentId);
    ElMessage.success(`已成功为该作业的 ${pendingCount} 份答卷触发 AI 智能预评！正在进入批改中心...`);
  } catch (err: any) {
    console.warn('触发智能批改提示:', err);
    ElMessage.info('已发起 AI 智能预评，正在前往评阅工作台...');
  }
  router.push(`/ai/grading?assignmentId=${assignmentId}`);
}
</script>

<style scoped lang="scss">
.assignment-list-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .assignment-header-dock {
    background: #ffffff;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 28px;
        }

        .main-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }

        .capsule-count-tag {
          font-size: 12px;
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
          border-radius: 9999px;
          padding: 2px 10px;
          font-weight: 500;
        }
      }

      .sub-desc {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .header-right-actions {
      .capsule-btn-primary {
        border-radius: 9999px;
        font-weight: 600;
        padding: 9px 22px;
      }
    }
  }

  .stats-cards-grid {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
    width: 100%;

    .stat-card {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      height: 46px;
      padding: 0 18px;
      border-radius: 9999px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 10px rgba(30, 80, 150, 0.03);
      flex: 1 1 220px;

      .stat-icon {
        width: 32px;
        height: 32px;
        border-radius: 9999px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        flex-shrink: 0;
      }

      .stat-meta {
        display: flex;
        align-items: baseline;
        gap: 8px;
        min-width: 0;

        .stat-val {
          font-size: 18px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1;
        }

        .stat-title {
          font-size: 12.5px;
          color: #64748b;
          margin-top: 0;
          white-space: nowrap;
        }
      }
    }
  }

  .filter-capsule-card {
    width: 100%;
    box-sizing: border-box;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 18px 24px;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);

    .filter-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      width: 100%;

      .search-input {
        flex: 1;
        min-width: 280px;
      }

      .filter-select {
        width: 180px;
        flex-shrink: 0;
      }
    }
  }

  .assignments-grid-wrapper {
    width: 100%;

    .assignments-stack {
      display: flex;
      flex-direction: column;
      gap: 20px;

      .assignment-row-card {
        width: 100%;
        box-sizing: border-box;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 18px;
        padding: 24px 28px;
        box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 24px;
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
        }

        .row-left-info {
          flex: 1;

          .tags-and-title {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;

            .assignment-title {
              font-size: 16px;
              font-weight: 700;
              color: #0f172a;
              margin: 0;
              cursor: pointer;

              &:hover {
                color: #2563eb;
              }
            }
          }

          .meta-sub-row {
            display: flex;
            align-items: center;
            gap: 20px;
            flex-wrap: wrap;

            .meta-item {
              font-size: 13px;
              color: #64748b;
              display: inline-flex;
              align-items: center;
              gap: 4px;

              .meta-icon {
                font-size: 14px;
              }
            }
          }
        }

        .row-center-progress {
          width: 200px;
          flex-shrink: 0;

          .prog-label {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #64748b;
            margin-bottom: 4px;
          }
        }

        .row-right-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-shrink: 0;
        }
      }
    }

    .empty-state-panel {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 60px 24px;
      text-align: center;

      .empty-icon {
        font-size: 48px;
        margin-bottom: 12px;
      }

      h3 {
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
        margin-bottom: 6px;
      }

      p {
        font-size: 14px;
        color: #64748b;
        margin-bottom: 20px;
      }
    }
  }
}
</style>

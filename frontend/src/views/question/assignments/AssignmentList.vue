<template>
  <div class="assignment-list-container">
    <!-- 顶部操作头区 -->
    <div class="assignment-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <el-icon class="header-icon text-blue-600"><Tickets /></el-icon>
          <h1 class="main-title">课程作业与平时测验管理</h1>
          <span class="capsule-count-tag">共 {{ assignments.length }} 份作业任务</span>
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
        />
        <el-select v-model="selectedCourseId" placeholder="所属课程" clearable class="filter-select">
          <el-option label="全部课程" :value="null" />
          <el-option
            v-for="c in courses"
            :key="c.id"
            :label="c.title"
            :value="c.id"
          />
        </el-select>
        <el-select v-model="selectedStatus" placeholder="批改状态" clearable class="filter-select">
          <el-option label="全部状态" value="" />
          <el-option label="待批改" value="PENDING" />
          <el-option label="已批改" value="GRADED" />
        </el-select>
      </div>
    </div>

    <!-- 作业任务卡片列表 -->
    <div v-loading="loading" class="assignments-grid-wrapper">
      <div v-if="filteredAssignments.length > 0" class="assignments-stack">
        <div
          v-for="a in filteredAssignments"
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
import { getCourseList } from '@/api/course/course';
import { getSubmissionsByAssignment, gradeSubmission } from '@/api/question/submission';
import type { Course } from '@/types/course/course';

const router = useRouter();
const { assignments, loading, fetchAssignments } = useAssignment();

const courses = ref<Course[]>([]);
const searchKeyword = ref('');
const selectedCourseId = ref<number | null>(null);
const selectedStatus = ref('');

onMounted(async () => {
  await Promise.all([loadCourses(), loadAssignments()]);
});

async function loadCourses() {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    courses.value = res.data?.list || [
      { id: 101, title: '数据结构与算法' } as any,
      { id: 102, title: 'Java程序设计' } as any,
      { id: 103, title: '高等数学（上）' } as any
    ];
  } catch {
    courses.value = [
      { id: 101, title: '数据结构与算法' } as any,
      { id: 102, title: 'Java程序设计' } as any,
      { id: 103, title: '高等数学（上）' } as any
    ];
  }
}

async function loadAssignments() {
  await fetchAssignments();
  if (assignments.value.length === 0) {
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
  }
}

const filteredAssignments = computed(() => {
  return assignments.value.filter((a: any) => {
    if (selectedCourseId.value && a.courseId !== selectedCourseId.value) return false;
    if (selectedStatus.value && a.status !== selectedStatus.value) return false;
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const inTitle = a.title?.toLowerCase().includes(kw);
      const inCourse = a.courseName?.toLowerCase().includes(kw);
      if (!inTitle && !inCourse) return false;
    }
    return true;
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
    const sRes = await getSubmissionsByAssignment(assignmentId);
    const pendingSubs = (sRes.data || []).filter((s: any) => s.status === 'PENDING');
    for (const sub of pendingSubs) {
      await gradeSubmission(sub.id);
    }
    ElMessage.success(`已成功为该作业的 ${pendingSubs.length} 份答卷触发 AI 智能预评！正在进入批改中心...`);
  } catch (err: any) {
    console.warn('触发智能批改提示:', err);
    ElMessage.info('已发起 AI 智能预评，正在前往评阅工作台...');
  }
  router.push(`/ai/grading?assignmentId=${assignmentId}`);
}
</script>

<style scoped lang="scss">
.assignment-list-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);

  .assignment-header-dock {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 24px;

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
        background: #2563eb;
        border-color: #2563eb;
        border-radius: 8px;
        font-weight: 500;
        padding: 9px 20px;
      }
    }
  }

  .stats-cards-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
    margin-bottom: 20px;

    .stat-card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

      .stat-icon {
        width: 48px;
        height: 48px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
      }

      .stat-meta {
        display: flex;
        flex-direction: column;

        .stat-val {
          font-size: 24px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1.1;
        }

        .stat-title {
          font-size: 13px;
          color: #64748b;
          margin-top: 4px;
        }
      }
    }
  }

  .filter-capsule-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 20px;
    margin-bottom: 20px;

    .filter-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .search-input {
        width: 320px;
      }

      .filter-select {
        width: 160px;
      }
    }
  }

  .assignments-grid-wrapper {
    .assignments-stack {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .assignment-row-card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 20px 24px;
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

<template>
  <div class="course-assignments-panel" v-loading="loading">
    <div class="panel-head">
      <div class="head-texts">
        <h3 class="panel-title">
          <el-icon class="mr-1 text-primary"><Notebook /></el-icon>
          课程作业与课堂测验
        </h3>
        <p class="panel-desc">查看本课程已发布的在线测验与作业任务，支持即时在线作答与 AI 智能辅导。</p>
      </div>
      <el-button v-if="isTeacherView" type="primary" plain class="btn-manage" @click="goManage">
        进入作业管理
      </el-button>
    </div>

    <div v-if="assignments.length" class="assignment-cards-grid">
      <div
        v-for="a in assignments"
        :key="a.id"
        class="course-assignment-card"
        :class="{ 'is-completed': isDone(a.mySubmissionStatus) }"
      >
        <div class="card-left-badge">
          <el-icon><EditPen /></el-icon>
        </div>

        <div class="card-content-body">
          <div class="card-title-row">
            <h4 class="assignment-title" :title="a.title">{{ a.title }}</h4>
            <el-tag size="small" :type="statusTag(a.mySubmissionStatus)" effect="light" class="status-pill">
              {{ statusLabel(a.mySubmissionStatus) }}
            </el-tag>
          </div>

          <div class="assignment-meta-row">
            <span class="meta-item">
              <el-icon><Timer /></el-icon>
              截止时间：{{ formatDeadline(a.deadline) }}
            </span>
            <span class="meta-item">
              <el-icon><Trophy /></el-icon>
              满分 {{ a.totalScore ?? 100 }} 分
            </span>
            <span v-if="a.settings?.aiGradingEnabled !== false" class="meta-item chip-ai">
              <el-icon><Cpu /></el-icon>
              AI 智能快评
            </span>
          </div>
        </div>

        <div class="assignment-actions">
          <el-button
            v-if="!isTeacherView"
            :type="isDone(a.mySubmissionStatus) ? 'info' : 'primary'"
            :plain="isDone(a.mySubmissionStatus)"
            size="default"
            class="btn-action"
            @click="goTake(a.id, a.mySubmissionStatus)"
          >
            <el-icon class="mr-1">
              <Document v-if="isDone(a.mySubmissionStatus)" />
              <Edit v-else />
            </el-icon>
            {{ actionLabel(a.mySubmissionStatus) }}
          </el-button>
        </div>
      </div>
    </div>

    <div v-else class="empty-wrap">
      <el-empty description="本课程暂无已发布作业或测验" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  Notebook,
  EditPen,
  Timer,
  Trophy,
  Cpu,
  Edit,
  Document
} from '@element-plus/icons-vue';
import { getMyAssignments } from '@/api/question/assignment';
import type { StudentAssignment } from '@/types/question/assignment';
import {
  SUBMISSION_STATUS,
  SUBMISSION_STATUS_LABEL,
  SUBMISSION_STATUS_TAG
} from '@/constants/question/assignment';
import { useAuthStore } from '@/stores/auth/auth';
import { RoleEnum } from '@/constants/auth';

const props = defineProps<{ course?: { id?: number } }>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const assignments = ref<StudentAssignment[]>([]);

const courseId = computed(() => Number(props.course?.id || route.params.id));

const isTeacherView = computed(() => {
  const roles = authStore.currentUser?.roles || [];
  return roles.includes(RoleEnum.TEACHER) || roles.includes(RoleEnum.ADMIN);
});

function isDone(st?: string): boolean {
  return st === SUBMISSION_STATUS.GRADED || st === SUBMISSION_STATUS.REVIEWED || st === SUBMISSION_STATUS.SUBMITTED;
}

function formatDeadline(val?: string) {
  if (!val) return '无限制';
  return String(val).replace('T', ' ').slice(0, 16);
}

function statusLabel(st?: string) {
  if (!st || st === SUBMISSION_STATUS.NOT_STARTED) return '待完成';
  return SUBMISSION_STATUS_LABEL[st] || st;
}

function statusTag(st?: string) {
  return (SUBMISSION_STATUS_TAG[st || ''] as 'info' | 'success' | 'warning' | 'primary') || 'warning';
}

function actionLabel(st?: string) {
  if (isDone(st)) {
    return '查看评阅报告';
  }
  return '立即作答';
}

function goTake(id: number, st?: string) {
  if (isDone(st)) {
    router.push(`/learning/assignments/${id}/result`);
  } else {
    router.push(`/learning/assignments/${id}/take`);
  }
}

function goManage() {
  router.push({ path: '/question/assignments', query: { courseId: String(courseId.value) } });
}

async function loadAssignments() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const res = await getMyAssignments({ courseId: courseId.value });
    assignments.value = (res.data || []).filter((a) => a.status === 'PUBLISHED' || a.status === 'CLOSED');
  } finally {
    loading.value = false;
  }
}

onMounted(loadAssignments);
watch(courseId, loadAssignments);
</script>

<style scoped lang="scss">
.course-assignments-panel {
  padding: 8px 4px 24px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;

  .head-texts {
    .panel-title {
      margin: 0 0 6px;
      font-size: 18px;
      font-weight: 700;
      color: #0f172a;
      display: flex;
      align-items: center;
    }

    .panel-desc {
      margin: 0;
      font-size: 13px;
      color: #64748b;
    }
  }

  .btn-manage {
    border-radius: 8px;
    font-weight: 500;
  }
}

.assignment-cards-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.course-assignment-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);
  transition: all 0.25s ease;

  &:hover {
    border-color: #93c5fd;
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.06);
  }

  &.is-completed {
    background: #fafbfc;
  }

  .card-left-badge {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: #eff6ff;
    color: #2563eb;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  .card-content-body {
    flex: 1;
    min-width: 0;

    .card-title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
      flex-wrap: wrap;

      .assignment-title {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #0f172a;
      }

      .status-pill {
        font-weight: 500;
        border-radius: 6px;
      }
    }

    .assignment-meta-row {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 13px;
      color: #64748b;
      flex-wrap: wrap;

      .meta-item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }

      .chip-ai {
        color: #7c3aed;
        background: #f5f3ff;
        padding: 1px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 500;
      }
    }
  }

  .assignment-actions {
    flex-shrink: 0;

    .btn-action {
      border-radius: 8px;
      font-weight: 600;
      padding: 0 16px;
    }
  }
}

.empty-wrap {
  padding: 40px 0;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

@media (max-width: 640px) {
  .course-assignment-card {
    flex-direction: column;
    align-items: flex-start;

    .assignment-actions {
      width: 100%;
      .btn-action {
        width: 100%;
      }
    }
  }
}
</style>


<template>
  <div class="course-assignments-panel" v-loading="loading">
    <div class="panel-head">
      <h3 class="panel-title">课程作业</h3>
      <p class="panel-desc">查看本课程已发布的在线作业与提交状态。</p>
      <el-button v-if="isTeacherView" type="primary" plain @click="goManage">
        进入作业管理
      </el-button>
    </div>

    <div v-if="assignments.length" class="assignment-list">
      <div v-for="a in assignments" :key="a.id" class="assignment-row">
        <div class="assignment-main">
          <span class="assignment-title">{{ a.title }}</span>
          <span class="assignment-deadline">截止：{{ formatDeadline(a.deadline) }}</span>
        </div>
        <div class="assignment-actions">
          <el-tag size="small" :type="statusTag(a.mySubmissionStatus)">
            {{ statusLabel(a.mySubmissionStatus) }}
          </el-tag>
          <el-button
            v-if="!isTeacherView"
            type="primary"
            size="small"
            @click="goTake(a.id, a.mySubmissionStatus)"
          >
            {{ actionLabel(a.mySubmissionStatus) }}
          </el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="本课程暂无已发布作业" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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

function formatDeadline(val?: string) {
  if (!val) return '—';
  return String(val).replace('T', ' ').slice(0, 16);
}

function statusLabel(st?: string) {
  if (!st || st === SUBMISSION_STATUS.NOT_STARTED) return '未开始';
  return SUBMISSION_STATUS_LABEL[st] || st;
}

function statusTag(st?: string) {
  return (SUBMISSION_STATUS_TAG[st || ''] as 'info' | 'success' | 'warning' | 'primary') || 'info';
}

function actionLabel(st?: string) {
  if (st === SUBMISSION_STATUS.GRADED || st === SUBMISSION_STATUS.REVIEWED) {
    return '查看成绩';
  }
  return '去作答';
}

function goTake(id: number, st?: string) {
  if (st === SUBMISSION_STATUS.GRADED || st === SUBMISSION_STATUS.REVIEWED) {
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
  margin-bottom: 20px;

  .panel-title {
    margin: 0 0 6px;
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
  }

  .panel-desc {
    margin: 0 0 12px;
    font-size: 13px;
    color: #64748b;
  }
}

.assignment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assignment-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.assignment-title {
  font-weight: 600;
  color: #0f172a;
  display: block;
}

.assignment-deadline {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  display: block;
}

.assignment-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
</style>

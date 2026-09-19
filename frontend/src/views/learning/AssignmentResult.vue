<template>
  <div class="assignment-result-page" v-loading="loading">
    <PageHeroBanner
      title="作业提交结果"
      subtitle="查看本次作业的提交状态与评阅得分。"
      background-variant="learning"
    />

    <el-card v-if="paper" class="result-card" shadow="never">
      <h3>{{ paper.title }}</h3>
      <p class="status-line">
        状态：
        <el-tag :type="statusTag">{{ statusLabel }}</el-tag>
      </p>
      <p v-if="submission?.totalScore != null" class="score-line">
        得分：<strong>{{ submission.totalScore }}</strong> / {{ submission.maxScore ?? paper.totalScore ?? '—' }}
      </p>
      <p v-else class="score-line muted">评阅进行中，请稍后查看最终得分。</p>
      <div class="actions">
        <el-button @click="router.push('/learning/tasks')">返回任务中心</el-button>
        <el-button type="primary" plain @click="router.push(`/question/assignments/${assignmentId}`)">
          查看作业详情
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useStudentAssignments } from '@/composables/learning/useStudentAssignments';
import { SUBMISSION_STATUS_LABEL, SUBMISSION_STATUS_TAG } from '@/constants/question/assignment';

const route = useRoute();
const router = useRouter();
const assignmentId = Number(route.params.id);

const { loading, paper, loadPaper, loadSubmissionResult } = useStudentAssignments();
const submission = ref<Record<string, unknown> | null>(null);

const statusLabel = computed(() => {
  const st = (submission.value?.status as string) || paper.value?.mySubmissionStatus || 'SUBMITTED';
  return SUBMISSION_STATUS_LABEL[st] || st;
});

const statusTag = computed(() => {
  const st = (submission.value?.status as string) || paper.value?.mySubmissionStatus || 'SUBMITTED';
  return SUBMISSION_STATUS_TAG[st] || 'info';
});

onMounted(async () => {
  const p = await loadPaper(assignmentId);
  if (p?.mySubmissionId) {
    submission.value = (await loadSubmissionResult(p.mySubmissionId)) as Record<string, unknown>;
  }
});
</script>

<style scoped lang="scss">
.result-card {
  border-radius: 16px;

  h3 {
    margin: 0 0 12px;
  }
}

.status-line,
.score-line {
  margin: 8px 0;
}

.muted {
  color: #64748b;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>

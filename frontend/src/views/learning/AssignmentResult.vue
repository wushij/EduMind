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
      <p v-if="submission?.submitTime" class="submit-line muted">提交时间：{{ formatTime(submission.submitTime) }}</p>

      <div class="actions">
        <el-button @click="router.push('/learning/tasks')">返回任务中心</el-button>
        <el-button type="primary" plain :disabled="!canViewDetail" @click="toggleDetail">
          {{ showDetail ? '收起答题详情' : '查看答题详情' }}
        </el-button>
      </div>

      <!-- 学生端只读答题详情：题干 / 我的作答 / 参考答案 / 解析 / AI 与教师评语 -->
      <AssignmentResultDetail
        v-if="showDetail"
        :grading-items="submission?.gradingItems"
        :answers="submission?.answers"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import AssignmentResultDetail from '@/components/learning/AssignmentResultDetail.vue';
import { useStudentAssignments } from '@/composables/learning/useStudentAssignments';
import { SUBMISSION_STATUS_LABEL, SUBMISSION_STATUS_TAG } from '@/constants/question/assignment';
import type { SubmissionItem } from '@/types/question/submission';

const route = useRoute();
const router = useRouter();
const assignmentId = Number(route.params.id);

const { loading, paper, loadPaper, loadSubmissionResult } = useStudentAssignments();
const submission = ref<SubmissionItem | null>(null);
const showDetail = ref(false);

const statusLabel = computed(() => {
  const st = submission.value?.status || paper.value?.mySubmissionStatus || 'SUBMITTED';
  return SUBMISSION_STATUS_LABEL[st] || st;
});

const statusTag = computed(() => {
  const st = submission.value?.status || paper.value?.mySubmissionStatus || 'SUBMITTED';
  return SUBMISSION_STATUS_TAG[st] || 'info';
});

/** 有逐题评阅结果或作答明细才允许展开详情 */
const canViewDetail = computed(() => {
  const gradingCount = submission.value?.gradingItems?.length ?? 0;
  const answerCount = submission.value?.answers?.length ?? 0;
  return gradingCount > 0 || answerCount > 0;
});

function toggleDetail() {
  showDetail.value = !showDetail.value;
}

function formatTime(value?: string) {
  if (!value) {
    return '—';
  }
  return value.replace('T', ' ').slice(0, 16);
}

onMounted(async () => {
  const p = await loadPaper(assignmentId);
  if (p?.mySubmissionId) {
    submission.value = await loadSubmissionResult(p.mySubmissionId);
  }
});
</script>

<style scoped lang="scss">
.result-card {
  border-radius: 22px;

  h3 {
    margin: 0 0 12px;
  }
}

.status-line,
.score-line,
.submit-line {
  margin: 8px 0;
}

.submit-line {
  font-size: 13px;
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

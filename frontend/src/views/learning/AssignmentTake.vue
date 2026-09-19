<template>
  <div class="assignment-take-page" v-loading="loading">
    <PageHeroBanner
      :title="paper?.title || '在线作业作答'"
      subtitle="请在截止时间前完成作答并提交，提交后将进入智能评阅流程。"
      background-variant="learning"
    />

    <div v-if="paper" class="take-content">
      <div class="take-meta">
        <span>满分 {{ paper.totalScore ?? '—' }} 分</span>
        <span>截止：{{ paper.deadline || '—' }}</span>
      </div>

      <el-card v-for="(item, idx) in paper.questions || []" :key="item.questionId" class="question-card" shadow="never">
        <div class="q-head">
          <span class="q-index">第 {{ idx + 1 }} 题</span>
          <el-tag size="small">{{ item.question?.type || '题目' }}</el-tag>
          <span class="q-score">{{ item.score || 5 }} 分</span>
        </div>
        <p class="q-stem">{{ item.question?.stem }}</p>
        <el-input
          v-if="isShortAnswer(item.question?.type)"
          v-model="answers[item.questionId]"
          type="textarea"
          :rows="4"
          placeholder="请输入作答内容"
        />
        <el-radio-group
          v-else-if="item.question?.type === 'SINGLE_CHOICE' || item.question?.type === 'TRUE_FALSE'"
          v-model="answers[item.questionId]"
        >
          <el-radio v-for="opt in parseOptions(item.question?.options)" :key="opt.key" :label="opt.key">
            {{ opt.key }}. {{ opt.content }}
          </el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="item.question?.type === 'MULTIPLE_CHOICE'" v-model="multiAnswers[item.questionId]">
          <el-checkbox v-for="opt in parseOptions(item.question?.options)" :key="opt.key" :label="opt.key">
            {{ opt.key }}. {{ opt.content }}
          </el-checkbox>
        </el-checkbox-group>
        <el-input v-else v-model="answers[item.questionId]" placeholder="请输入答案" />
      </el-card>

      <div class="take-actions">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交答卷</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useStudentAssignments } from '@/composables/learning/useStudentAssignments';

const route = useRoute();
const router = useRouter();
const assignmentId = Number(route.params.id);

const { loading, paper, loadPaper, submitPaper } = useStudentAssignments();
const answers = reactive<Record<number, string>>({});
const multiAnswers = reactive<Record<number, string[]>>({});
const submitting = ref(false);

function parseOptions(raw?: string) {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.map((o: { key?: string; content?: string }) => ({
        key: o.key || '',
        content: o.content || ''
      }));
    }
  } catch {
    return [];
  }
  return [];
}

function isShortAnswer(type?: string) {
  return type === 'SHORT_ANSWER' || type === 'FILL_BLANK';
}

function buildPayload() {
  const list: Array<{ questionId: number; answer: string }> = [];
  for (const item of paper.value?.questions || []) {
    const qid = item.questionId;
    let answer = answers[qid] || '';
    if (item.question?.type === 'MULTIPLE_CHOICE') {
      answer = (multiAnswers[qid] || []).sort().join(',');
    }
    list.push({ questionId: qid, answer });
  }
  return list;
}

async function handleSubmit() {
  await ElMessageBox.confirm('确认提交当前答卷？提交后将进入评阅流程。', '提交确认', {
    type: 'warning'
  });
  submitting.value = true;
  try {
    await submitPaper(assignmentId, buildPayload());
    ElMessage.success('答卷已提交');
    router.replace(`/learning/assignments/${assignmentId}/result`);
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '提交失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  const p = await loadPaper(assignmentId);
  if (p?.mySubmissionStatus === 'GRADED' || p?.mySubmissionStatus === 'SUBMITTED') {
    router.replace(`/learning/assignments/${assignmentId}/result`);
  }
});
</script>

<style scoped lang="scss">
.assignment-take-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.take-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.take-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #64748b;
}

.question-card {
  border-radius: 14px;
}

.q-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.q-index {
  font-weight: 600;
}

.q-stem {
  margin: 0 0 12px;
  line-height: 1.6;
}

.take-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-bottom: 24px;
}
</style>

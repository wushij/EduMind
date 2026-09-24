<template>
  <div class="wrong-question-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>错题归因分析</h2>
        <p>聚合高频错题、错因类型与 AI 诊断建议</p>
      </div>
      <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="reload">
        <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据"
      class="mock-alert"
    />

    <el-card shadow="never" class="table-card">
      <el-table :data="wrongQuestions?.list ?? []" stripe>
        <el-table-column prop="questionId" label="题目 ID" width="100" />
        <el-table-column prop="wrongCount" label="错误次数" width="100" />
        <el-table-column label="错因类型" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="type in row.errorTypes"
              :key="type"
              size="small"
              effect="plain"
              class="error-tag"
            >
              {{ type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnosis" label="AI 诊断" min-width="260" show-overflow-tooltip />
        <el-table-column label="变式题" width="120">
          <template #default="{ row }">
            {{ row.variantQuestionIds?.length ?? 0 }} 道
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :loading="diagnosingId === row.id"
              @click="handleDiagnose(row)"
            >
              AI 诊断
            </el-button>
            <el-button link type="primary" @click="goGenerate(row)">生成巩固题</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AppPagination
        v-model:page-num="page"
        v-model:page-size="pageSize"
        :total="wrongQuestions?.total ?? 0"
        @change="reload"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import type { WrongQuestionItemVO } from '@/types/analytics/mastery';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import AppPagination from '@/components/common/AppPagination.vue';

const router = useRouter();

const { courseOptions, courseId, loadCourses } = useTeacherCourses();
const page = ref(1);
const pageSize = ref(10);
const diagnosingId = ref<number | null>(null);

const { loading, usedMockFallback, wrongQuestions, fetchWrongQuestions, diagnoseWrong } = useLearningAnalytics();

async function reload() {
  if (!courseId.value || courseId.value <= 0) return;
  await fetchWrongQuestions(courseId.value, page.value, pageSize.value);
}

watch(courseId, () => {
  page.value = 1;
  reload();
});

async function handleDiagnose(row: WrongQuestionItemVO) {
  if (!row.id) {
    ElMessage.warning('错题记录 ID 缺失，无法诊断');
    return;
  }
  diagnosingId.value = row.id;
  try {
    const result = await diagnoseWrong(row.id);
    if (!result) {
      ElMessage.error('错题诊断失败');
      return;
    }
    row.diagnosis = result.diagnosis ?? row.diagnosis;
    row.variantQuestionIds = result.variantQuestionIds;
    ElMessage.success(`诊断完成，已生成 ${result.variantQuestionIds.length} 道变式题`);
  } catch {
    ElMessage.error('错题诊断失败');
  } finally {
    diagnosingId.value = null;
  }
}

function goGenerate(row: WrongQuestionItemVO) {
  router.push({
    path: '/ai/question/generate',
    query: { questionId: String(row.questionId) }
  });
}

onMounted(reload);
</script>

<style scoped lang="scss">
.wrong-question-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;

    h2 {
      margin: 0 0 6px;
      font-size: 22px;
      font-weight: 700;
      color: #0F172A;
    }

    p {
      margin: 0;
      color: #64748B;
      font-size: 14px;
    }
  }

  .table-card {
    border-radius: 14px;
  }

  .error-tag {
    margin-right: 6px;
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>

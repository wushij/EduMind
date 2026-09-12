<template>
  <div class="learning-analysis-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>学生学情诊断</h2>
        <p>追踪课程学习活跃度、成绩趋势与 AI 助教使用情况</p>
      </div>
      <div class="header-filters">
        <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="reload">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="range" style="width: 120px" @change="reload">
          <el-option label="近 7 天" value="7d" />
          <el-option label="近 30 天" value="30d" />
          <el-option label="本学期" value="semester" />
        </el-select>
      </div>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据（后端不可用或 VITE_USE_MOCK=true）"
      class="mock-alert"
    />

    <div class="kpi-grid">
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">在读学生</div>
        <div class="kpi-value">{{ learningData?.studentCount ?? 0 }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">任务完成率</div>
        <div class="kpi-value">{{ formatPercent(learningData?.completionRate) }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">班级平均分</div>
        <div class="kpi-value">{{ learningData?.avgScore?.toFixed(1) ?? '-' }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">平均学习时长</div>
        <div class="kpi-value">{{ learningData?.avgStudyMinutes ?? 0 }} 分钟</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">知识点掌握均值</div>
        <div class="kpi-value">{{ formatPercent(learningData?.knowledgeMasteryAvg) }}</div>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">AI 助教调用</div>
        <div class="kpi-value">{{ learningData?.aiUsageCount?.toLocaleString() ?? 0 }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <span class="card-title">学习活跃与成绩趋势</span>
      </template>
      <LearningChart :trends="learningData?.trends ?? null" />
    </el-card>

    <AiTeachingAdvice
      :advice="teachingAdvice"
      :loading="loading"
      @generate="handleGenerateAdvice"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import LearningChart from '@/components/analytics/LearningChart.vue';
import AiTeachingAdvice from '@/components/analytics/AiTeachingAdvice.vue';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

const { courseOptions, courseId } = useTeacherCourses(102);
const range = ref('7d');

const {
  loading,
  usedMockFallback,
  learningData,
  teachingAdvice,
  fetchLearning,
  fetchTeachingAdvice
} = useLearningAnalytics();

function formatPercent(value?: number) {
  if (value == null) return '-';
  return `${value.toFixed(1)}%`;
}

async function reload() {
  await fetchLearning(courseId.value, range.value);
}

async function handleGenerateAdvice() {
  await fetchTeachingAdvice({ courseId: courseId.value });
}

onMounted(reload);
</script>

<style scoped lang="scss">
.learning-analysis-page {
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

  .header-filters {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .mock-alert {
    margin-bottom: 0;
  }

  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }

  .kpi-card {
    border-radius: 12px;

    .kpi-label {
      font-size: 13px;
      color: #64748B;
      margin-bottom: 8px;
    }

    .kpi-value {
      font-size: 24px;
      font-weight: 700;
      color: #0F172A;
    }
  }

  .chart-card {
    border-radius: 14px;

    .card-title {
      font-weight: 700;
      color: #0F172A;
    }
  }
}

@media (max-width: 960px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}
</style>

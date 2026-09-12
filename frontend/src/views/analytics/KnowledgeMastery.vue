<template>
  <div class="knowledge-mastery-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>知识点掌握度画像</h2>
        <p>对比个人与班级在各知识维度的掌握情况，定位薄弱考点</p>
      </div>
      <div class="header-filters">
        <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="reload">
          <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-input-number
          v-model="studentId"
          :min="1"
          placeholder="学生 ID"
          controls-position="right"
          @change="reload"
        />
      </div>
    </div>

    <el-alert
      v-if="usedMockFallback"
      type="info"
      :closable="false"
      show-icon
      title="当前展示 Mock 数据"
      class="mock-alert"
    />

    <!-- V1.1 班级知识点全景掌握度热力矩阵 -->
    <KnowledgeHeatmap :course-id="courseId" />

    <div class="content-grid">
      <el-card shadow="never" class="radar-card">
        <template #header>
          <span class="card-title">掌握度雷达图</span>
        </template>
        <KnowledgeRadar :data="masteryData" />
      </el-card>

      <el-card shadow="never" class="weak-card">
        <template #header>
          <span class="card-title">薄弱知识点</span>
        </template>
        <div v-if="masteryData?.weakPoints?.length" class="weak-list">
          <div v-for="item in masteryData.weakPoints" :key="item.knowledgePointId" class="weak-item">
            <div class="weak-top">
              <span class="weak-title">{{ item.title }}</span>
              <el-tag :type="item.mastery < 50 ? 'danger' : 'warning'" size="small">
                {{ item.mastery }}%
              </el-tag>
            </div>
            <p class="weak-suggestion">{{ item.suggestion }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无薄弱知识点" :image-size="80" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import KnowledgeRadar from '@/components/analytics/KnowledgeRadar.vue';
import KnowledgeHeatmap from '@/components/analytics/KnowledgeHeatmap.vue';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';

const { courseOptions, courseId } = useTeacherCourses(102);
const studentId = ref<number | undefined>(undefined);

const { loading, usedMockFallback, masteryData, fetchMastery } = useLearningAnalytics();

async function reload() {
  await fetchMastery(courseId.value, studentId.value);
}

onMounted(reload);
</script>

<style scoped lang="scss">
.knowledge-mastery-page {
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

  .content-grid {
    display: grid;
    grid-template-columns: 1.2fr 0.8fr;
    gap: 20px;
  }

  .card-title {
    font-weight: 700;
    color: #0F172A;
  }

  .radar-card,
  .weak-card {
    border-radius: 14px;
  }

  .weak-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .weak-item {
    padding: 14px;
    border-radius: 12px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;

    .weak-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }

    .weak-title {
      font-weight: 600;
      color: #1E293B;
      font-size: 14px;
    }

    .weak-suggestion {
      margin: 0;
      font-size: 13px;
      color: #64748B;
      line-height: 1.6;
    }
  }
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>

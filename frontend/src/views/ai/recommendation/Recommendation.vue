<template>
  <div class="recommendation-page ai-teaching-page-shell">
    <ProfilePageHero
      title="AI 学习推荐"
      subtitle="结合学情与课程上下文，推荐适配的题目与学习资源。"
    >
      <template #actions>
        <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="loadData">
          刷新推荐
        </el-button>
      </template>
    </ProfilePageHero>

    <div class="toolbar ai-teaching-surface-card toolbar-surface">
      <span class="toolbar-label">课程</span>
      <el-select
        v-model="courseId"
        placeholder="请选择课程"
        style="width: 260px"
        :loading="courseLoading"
        @change="loadData"
      >
        <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <div class="grid">
      <div class="ai-teaching-surface-card grid-card">
        <h3>推荐题目</h3>
        <el-empty v-if="questions.length === 0" description="暂无推荐题目" />
        <ul v-else>
          <li v-for="item in questions" :key="item.id">{{ item.stem }}</li>
        </ul>
      </div>
      <div class="ai-teaching-surface-card grid-card">
        <h3>推荐资源</h3>
        <el-empty v-if="resources.length === 0" description="暂无推荐资源" />
        <ul v-else>
          <li v-for="item in resources" :key="item.id">{{ item.title }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import { useAIRecommendation } from '@/composables/ai/useAIRecommendation';

const { courseId, courseOptions, courseLoading, loading, questions, resources, loadData } =
  useAIRecommendation();
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.toolbar-surface {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;

  .toolbar-label {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
  }
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }

  .grid-card {
    h3 {
      margin: 0 0 12px;
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }

    ul {
      margin: 0;
      padding-left: 18px;
      color: #334155;
      line-height: 1.6;
    }
  }
}
</style>

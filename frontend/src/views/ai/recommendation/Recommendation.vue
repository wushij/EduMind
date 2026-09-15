<template>
  <div class="recommendation-page">
    <h2>AI 学习推荐</h2>
    <div class="toolbar">
      <el-input-number v-model="courseId" :min="1" />
      <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="loadData">
        刷新推荐
      </el-button>
    </div>

    <div class="grid">
      <el-card shadow="never">
        <h3>推荐题目</h3>
        <el-empty v-if="questions.length === 0" description="暂无推荐题目" />
        <ul v-else>
          <li v-for="item in questions" :key="item.id">{{ item.stem }}</li>
        </ul>
      </el-card>
      <el-card shadow="never">
        <h3>推荐资源</h3>
        <el-empty v-if="resources.length === 0" description="暂无推荐资源" />
        <ul v-else>
          <li v-for="item in resources" :key="item.id">{{ item.title }}</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { useAIRecommendation } from '@/composables/ai/useAIRecommendation';

const { courseId, loading, questions, resources, loadData } = useAIRecommendation();
</script>

<style scoped lang="scss">
.recommendation-page {
  padding: 24px;

  .toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  ul {
    padding-left: 18px;
    line-height: 1.8;
  }
}
</style>

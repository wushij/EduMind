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
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { getRecommendations } from '@/api/ai/recommendation';

const courseId = ref(1);
const loading = ref(false);
const questions = ref<Array<{ id: number; stem: string }>>([]);
const resources = ref<Array<{ id: number; title: string }>>([]);

async function loadData() {
  loading.value = true;
  try {
    const res = await getRecommendations(courseId.value);
    questions.value = res?.data?.questions || [];
    resources.value = res?.data?.resources || [];
  } catch {
    questions.value = [];
    resources.value = [];
    ElMessage.error('加载推荐失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
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

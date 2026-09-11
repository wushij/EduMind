<template>
  <div class="kb-detail-container">
    <div class="kb-header-card">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="router.push('/knowledge')" class="back-btn">返回知识库列表</el-button>
        <div class="kb-title-row">
          <h2>{{ kbInfo.title }}</h2>
          <el-tag type="success" size="small">{{ kbInfo.status }}</el-tag>
        </div>
        <p class="kb-desc">{{ kbInfo.description }}</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Upload">上传课件文档</el-button>
      </div>
    </div>

    <!-- 知识库二级 Tab 导航 -->
    <div class="kb-nav-tabs">
      <el-menu :default-active="route.path" mode="horizontal" router :ellipsis="false">
        <el-menu-item :index="`/knowledge/${kbId}/documents`">文档管理</el-menu-item>
        <el-menu-item :index="`/knowledge/${kbId}/chunks`">切片与分块</el-menu-item>
        <el-menu-item :index="`/knowledge/${kbId}/embeddings`">向量状态</el-menu-item>
        <el-menu-item :index="`/knowledge/${kbId}/retrieval`">检索测试</el-menu-item>
        <el-menu-item :index="`/knowledge/${kbId}/rag-debug`">RAG 诊断</el-menu-item>
        <el-menu-item :index="`/knowledge/${kbId}/graph`">知识图谱</el-menu-item>
      </el-menu>
    </div>

    <div class="kb-subview-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Upload } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();

const kbId = computed(() => route.params.id || '1');
const kbInfo = computed(() => ({
  title: '高等数学官方教学课件知识库',
  status: '已就绪 (128 个文档)',
  description: '汇聚高数教案、教学大纲、典型习题解答与期末复习要点，面向 RAG 检索生成提供权威语料支撑。'
}));
</script>

<style scoped lang="scss">
.kb-detail-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .kb-header-card {
    background: #FFFFFF;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border: 1px solid #E2E8F0;

    .header-left {
      .back-btn { margin-bottom: 8px; font-size: 13px; color: #64748B; padding: 0; }
      .kb-title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        h2 { margin: 0; font-size: 22px; font-weight: 700; color: #1E293B; }
      }
      .kb-desc { margin: 8px 0 0 0; font-size: 13px; color: #64748B; }
    }
  }

  .kb-nav-tabs {
    background: #FFFFFF;
    border-radius: 10px;
    padding: 0 16px;
    border: 1px solid #E2E8F0;
    :deep(.el-menu) { border-bottom: none; }
  }

  .kb-subview-content {
    min-height: 400px;
  }
}
</style>

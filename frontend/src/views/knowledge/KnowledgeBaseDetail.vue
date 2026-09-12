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

    <div class="kb-subview-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft, Upload } from '@element-plus/icons-vue';

const router = useRouter();

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

  .kb-subview-content {
    min-height: 400px;
  }
}
</style>

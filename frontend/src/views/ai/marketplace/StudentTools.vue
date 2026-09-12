<template>
  <MarketplaceToolsPage
    active-category="STUDENT"
    empty-title="暂无学生类 AI 工具"
    empty-description="可尝试切换分类或搜索其他工具"
    :empty-icon="Reading"
  />
</template>

<script setup lang="ts">
import { Reading } from '@element-plus/icons-vue';
import MarketplaceToolsPage from '@/components/ai/MarketplaceToolsPage.vue';
</script>

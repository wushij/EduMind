<template>
  <MarketplaceToolsPage
    active-category="TEACHER"
    empty-title="暂无教师类 AI 工具"
    empty-description="可尝试切换分类或搜索其他工具"
    :empty-icon="School"
  />
</template>

<script setup lang="ts">
import { School } from '@element-plus/icons-vue';
import MarketplaceToolsPage from '@/components/ai/MarketplaceToolsPage.vue';
</script>

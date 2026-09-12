<template>
  <MarketplaceToolsPage
    active-category="RECOMMENDED"
    empty-title="暂无推荐类 AI 工具"
    empty-description="可尝试切换分类或搜索其他工具"
    :empty-icon="Star"
  />
</template>

<script setup lang="ts">
import { Star } from '@element-plus/icons-vue';
import MarketplaceToolsPage from '@/components/ai/MarketplaceToolsPage.vue';
</script>

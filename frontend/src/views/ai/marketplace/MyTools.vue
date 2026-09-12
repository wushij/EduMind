<template>
  <MarketplaceToolsPage
    active-category="MY_TOOLS"
    empty-title="暂无收藏的常用 AI 工具"
    empty-description="点击工具卡片右上角的星标图标即可加入常用列表"
    :empty-icon="Star"
  />
</template>

<script setup lang="ts">
import { Star } from '@element-plus/icons-vue';
import MarketplaceToolsPage from '@/components/ai/MarketplaceToolsPage.vue';
</script>

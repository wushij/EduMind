<template>
  <div class="v05-tool-notice-page">
    <V05FeatureNotice
      :title="notice.title"
      :description="notice.description"
      :alternatives="notice.alternatives"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import V05FeatureNotice from '@/components/common/V05FeatureNotice.vue';
import { getV05ToolNotice } from '@/constants/ai/v05-tool-notices';

const route = useRoute();

const notice = computed(() => {
  const toolId = String(route.params.toolId || '');
  return getV05ToolNotice(toolId);
});
</script>

<style scoped lang="scss">
.v05-tool-notice-page {
  padding: 8px 0 24px;
}
</style>

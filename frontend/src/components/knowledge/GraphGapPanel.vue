<template>
  <el-card shadow="never" class="gap-panel">
    <template #header>前置漏洞定位</template>
    <el-empty v-if="!gaps.length" description="暂无漏洞项" />
    <el-timeline v-else>
      <el-timeline-item v-for="gap in gaps" :key="gap.knowledgePointId" type="warning">
        <p class="gap-title">{{ gap.title }}</p>
        <p v-for="pre in gap.missingPrerequisites" :key="pre.id" class="gap-pre">
          未掌握前置：{{ pre.title }}
        </p>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script setup lang="ts">
import type { GraphGapVO } from '@/types/knowledge/graph';

defineProps<{
  gaps: GraphGapVO[];
}>();
</script>

<style scoped lang="scss">
.gap-panel {
  border-radius: 12px;
}
.gap-title {
  font-weight: 600;
  margin: 0 0 4px;
}
.gap-pre {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
</style>

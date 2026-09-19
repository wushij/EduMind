<template>
  <div class="learning-path-panel">
    <LearningPathWeekGrid :weeks="weeks" @execute-task="onExecute" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import LearningPathWeekGrid from '@/components/learning/path/LearningPathWeekGrid.vue';
import type { LearningPathTask, LearningPathVO } from '@/types/learning/learning-path';

const props = defineProps<{
  path: LearningPathVO | null;
}>();

const router = useRouter();
const weeks = computed(() => props.path?.weeks ?? []);

function onExecute(task: LearningPathTask) {
  if (task.targetUrl) {
    router.push(task.targetUrl);
  }
}
</script>

<style scoped lang="scss">
.learning-path-panel {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 20px 24px;
}
</style>

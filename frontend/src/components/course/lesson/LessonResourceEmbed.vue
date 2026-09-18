<template>
  <div v-if="resource" class="resource-embed-card">
    <div class="resource-meta">
      <el-icon><Document /></el-icon>
      <div>
        <p class="resource-title">{{ resource.title }}</p>
        <span class="resource-type">{{ resource.resourceType || '资料' }}</span>
      </div>
    </div>
    <video
      v-if="isVideo && display === 'embed' && resource.downloadUrl"
      class="resource-video"
      controls
      :src="resource.downloadUrl"
    />
    <a
      v-else-if="resource.downloadUrl"
      class="capsule-download-btn"
      :href="resource.downloadUrl"
      target="_blank"
      rel="noopener noreferrer"
    >
      打开资料
    </a>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Document } from '@element-plus/icons-vue';
import type { LessonResourceSummary } from '@/api/course/lesson';

const props = defineProps<{
  resourceId: number;
  display: 'embed' | 'link';
  resources?: LessonResourceSummary[];
}>();

const resource = computed(() =>
  (props.resources || []).find(r => Number(r.resourceId) === Number(props.resourceId) || Number(r.id) === Number(props.resourceId))
);

const isVideo = computed(() => {
  const type = (resource.value?.resourceType || '').toUpperCase();
  return type.includes('VIDEO') || type.includes('MP4');
});
</script>

<style scoped lang="scss">
.resource-embed-card {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.resource-meta {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 10px;
}

.resource-title {
  margin: 0;
  font-weight: 600;
  color: #0f172a;
}

.resource-type {
  font-size: 0.75rem;
  color: #64748b;
}

.resource-video {
  width: 100%;
  border-radius: 12px;
  max-height: 360px;
  background: #000;
}

.capsule-download-btn {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  text-decoration: none;
  font-size: 0.85rem;
}
</style>

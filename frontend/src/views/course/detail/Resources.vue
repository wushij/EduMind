<template>
  <div class="page-container">
    <div class="page-header">
      <h2>课程资源</h2>
    </div>
    <div class="page-content">
      <el-card v-loading="loading" shadow="never">
        <el-table :data="resources" stripe>
          <el-table-column prop="title" label="资源名称" />
          <el-table-column prop="resourceType" label="类型" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useCourseMember } from '@/composables/course/useCourseMember';

const route = useRoute();
const courseId = Number(route.params.id);
const { resources, loading, fetchResources } = useCourseMember(courseId);

onMounted(() => fetchResources());
</script>

<style scoped lang="scss">
.page-container .page-header {
  margin-bottom: 16px;
  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
  }
}
</style>

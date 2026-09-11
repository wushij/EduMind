<template>
  <div class="page-container">
    <div class="page-header">
      <h2>课程成员</h2>
    </div>
    <div class="page-content">
      <el-card v-loading="loading" shadow="never">
        <el-table :data="members" stripe>
          <el-table-column prop="userId" label="用户ID" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="memberRole" label="角色" />
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
const { members, loading, fetchMembers } = useCourseMember(courseId);

onMounted(() => fetchMembers());
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

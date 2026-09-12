<template>
  <div class="learning-path-page">
    <h2>学习路径</h2>
    <div class="toolbar">
      <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="loadPath">
        <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadPath">生成路径</el-button>
    </div>

    <el-empty v-if="!path?.weeks?.length && !loading" description="暂无学习路径" />
    <el-timeline v-else>
      <el-timeline-item
        v-for="week in path?.weeks || []"
        :key="week.weekNo"
        :timestamp="`第 ${week.weekNo} 周`"
        placement="top"
      >
        <el-card shadow="never">
          <h3>{{ week.theme }}</h3>
          <ul>
            <li v-for="(task, idx) in week.tasks" :key="idx">
              [{{ task.type }}] {{ task.title }}
            </li>
          </ul>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getLearningPath } from '@/api/learning/learning-path';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { useAuthStore } from '@/stores/auth/auth';
import type { LearningPathVO } from '@/types/learning/learning-path';

const { courseOptions, courseId } = useTeacherCourses(102);
const authStore = useAuthStore();
const loading = ref(false);
const path = ref<LearningPathVO | null>(null);

async function loadPath() {
  loading.value = true;
  try {
    const studentId = authStore.currentUser?.id;
    const res = await getLearningPath(courseId.value, studentId);
    path.value = res?.data || null;
  } catch {
    path.value = null;
    ElMessage.error('加载学习路径失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadPath);
</script>

<style scoped lang="scss">
.learning-path-page {
  padding: 24px;

  .toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  h3 {
    margin: 0 0 8px;
  }
}
</style>

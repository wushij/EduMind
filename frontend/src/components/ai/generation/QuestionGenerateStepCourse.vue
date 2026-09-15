<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 1 步：选择关联教学课程空间</h3>
    <p class="pane-desc">AI 将以所选课程的大纲知识树与已沉淀的课件知识库作为出题依据：</p>

    <div v-if="displayCourses.length > 0" class="courses-picker-grid">
      <div
        v-for="course in displayCourses"
        :key="course.id"
        class="course-picker-item"
        :class="{ active: formState.courseId === course.id }"
        @click="formState.courseId = course.id"
      >
        <div class="picker-header">
          <span class="course-code">{{ course.code || ('CS' + course.id) }}</span>
          <span v-if="formState.courseId === course.id" class="active-badge">
            <el-icon><Check /></el-icon> 已选
          </span>
        </div>
        <h4 class="course-name">{{ course.title }}</h4>
        <p class="course-meta">主讲：{{ course.teacherName || '任课教师' }} · {{ course.chapterCount || 6 }} 章节</p>
      </div>
    </div>
    <el-empty v-else description="暂无课程数据，请先创建课程" />
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue';
import type { QuestionGenerateFormState } from './question-generate-types';

defineProps<{
  displayCourses: any[];
  formState: QuestionGenerateFormState;
}>();
</script>

<style scoped lang="scss">
.step-content-pane {
  .pane-title {
    margin: 0 0 6px 0;
    font-size: 18px;
    font-weight: 700;
    color: #0F172A;
  }

  .pane-desc {
    margin: 0 0 24px 0;
    font-size: 13.5px;
    color: #64748B;
  }
}

.courses-picker-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  .course-picker-item {
    border-radius: 16px;
    border: 2px solid #E2E8F0;
    padding: 16px 18px;
    cursor: pointer;
    transition: all 0.22s ease;

    .picker-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;

      .course-code {
        font-size: 12px;
        font-weight: 700;
        color: #64748B;
        background: #F1F5F9;
        padding: 2px 8px;
        border-radius: 9999px;
      }

      .active-badge {
        font-size: 11px;
        font-weight: 600;
        color: #1677FF;
      }
    }

    .course-name {
      margin: 0 0 6px 0;
      font-size: 14.5px;
      font-weight: 700;
      color: #1E293B;
    }

    .course-meta {
      margin: 0;
      font-size: 12px;
      color: #94A3B8;
    }

    &:hover {
      border-color: #93C5FD;
      transform: translateY(-2px);
    }

    &.active {
      border-color: #1677FF;
      background: #F0F7FF;
    }
  }
}
</style>

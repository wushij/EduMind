<template>
  <div class="step-content-pane">
    <div class="step-header-flex">
      <div>
        <h3 class="pane-title">第 1 步：选择关联教学课程空间</h3>
        <p class="pane-desc">AI 将以所选课程的教学大纲知识树与已沉淀的课件知识库作为出题核心依据：</p>
      </div>

      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索课程名称或课程编码..."
          clearable
          class="course-search-input"
          :prefix-icon="Search"
        />
      </div>
    </div>

    <div v-if="filteredCourses.length > 0" class="courses-picker-grid">
      <div
        v-for="course in filteredCourses"
        :key="course.id"
        class="course-picker-item"
        :class="{ active: formState.courseId === course.id }"
        @click="formState.courseId = course.id"
      >
        <div class="picker-header">
          <div class="code-and-cat">
            <span class="course-code">{{ course.code || ('CS' + course.id) }}</span>
            <span class="course-cat">{{ course.category || inferCategory(course.title) }}</span>
          </div>
          <span v-if="formState.courseId === course.id" class="active-badge">
            <el-icon><Check /></el-icon> 已选定
          </span>
        </div>

        <h4 class="course-name">{{ course.title || course.name }}</h4>
        <p class="course-desc">{{ course.description || '国家精品教学大纲课程，覆盖完整教学设计与考点' }}</p>

        <div class="course-footer-stats">
          <span class="stat-pill">
            <el-icon class="mr-0.5"><User /></el-icon>
            {{ course.teacherName || '任课教师' }}
          </span>
          <span class="stat-pill">
            <el-icon class="mr-0.5"><Collection /></el-icon>
            {{ course.chapterCount || 4 }} 个章节
          </span>
          <span class="stat-pill stat-pill--highlight">
            <el-icon class="mr-0.5"><Reading /></el-icon>
            考点直连
          </span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state-box">
      <el-empty description="未搜索到匹配的课程，请尝试更换关键词" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Check, Search, User, Collection, Reading } from '@element-plus/icons-vue';
import type { QuestionGenerateFormState } from './question-generate-types';

const props = defineProps<{
  displayCourses: any[];
  formState: QuestionGenerateFormState;
}>();

const searchQuery = ref('');

const filteredCourses = computed(() => {
  if (!searchQuery.value.trim()) return props.displayCourses;
  const q = searchQuery.value.toLowerCase().trim();
  return props.displayCourses.filter(c => {
    const title = (c.title || c.name || '').toLowerCase();
    const code = (c.code || '').toLowerCase();
    return title.includes(q) || code.includes(q);
  });
});

function inferCategory(title: string = ''): string {
  const t = title.toLowerCase();
  if (t.includes('ai') || t.includes('大模型') || t.includes('智能')) return '人工智能';
  if (t.includes('数据结构') || t.includes('算法')) return '专业核心';
  if (t.includes('操作系统') || t.includes('体系结构')) return '系统工程';
  if (t.includes('数学') || t.includes('微积分')) return '数理基础';
  return '专业课程';
}
</script>

<style scoped lang="scss">
.step-content-pane {
  .step-header-flex {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;
    flex-wrap: wrap;

    .pane-title {
      margin: 0 0 6px 0;
      font-size: 18px;
      font-weight: 800;
      color: #0F172A;
    }

    .pane-desc {
      margin: 0;
      font-size: 13.5px;
      color: #64748B;
    }

    .search-box {
      width: 280px;

      :deep(.el-input__wrapper) {
        border-radius: 9999px;
        padding-left: 14px;
        box-shadow: 0 0 0 1px #E2E8F0 inset;
        &:hover, &.is-focus {
          box-shadow: 0 0 0 1.5px #1677FF inset;
        }
      }
    }
  }
}

.courses-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;

  .course-picker-item {
    border-radius: 18px;
    border: 2px solid #E2E8F0;
    background: #FFFFFF;
    padding: 18px 20px;
    cursor: pointer;
    transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;

    .picker-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;

      .code-and-cat {
        display: flex;
        align-items: center;
        gap: 6px;

        .course-code {
          font-size: 12px;
          font-weight: 700;
          color: #1E293B;
          background: #F1F5F9;
          padding: 3px 8px;
          border-radius: 6px;
          letter-spacing: 0.2px;
        }

        .course-cat {
          font-size: 11px;
          color: #64748B;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          padding: 2px 7px;
          border-radius: 9999px;
        }
      }

      .active-badge {
        font-size: 12px;
        font-weight: 700;
        color: #1677FF;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        background: #EFF6FF;
        padding: 3px 10px;
        border-radius: 9999px;
      }
    }

    .course-name {
      margin: 0 0 6px 0;
      font-size: 16px;
      font-weight: 700;
      color: #1E293B;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .course-desc {
      margin: 0 0 14px 0;
      font-size: 12.5px;
      color: #64748B;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      flex: 1;
    }

    .course-footer-stats {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      padding-top: 10px;
      border-top: 1px solid #F1F5F9;

      .stat-pill {
        display: inline-flex;
        align-items: center;
        font-size: 11.5px;
        color: #64748B;
        background: #F8FAFC;
        padding: 2px 8px;
        border-radius: 9999px;

        &--highlight {
          color: #0284C7;
          background: #F0F9FF;
          font-weight: 600;
        }
      }
    }

    &:hover {
      border-color: #93C5FD;
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(30, 80, 160, 0.08);
    }

    &.active {
      border-color: #1677FF;
      background: linear-gradient(180deg, #F0F7FF 0%, #FFFFFF 100%);
      box-shadow: 0 8px 24px rgba(22, 119, 255, 0.12);

      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 4px;
        background: linear-gradient(90deg, #1677FF 0%, #722ED1 100%);
      }
    }
  }
}

.empty-state-box {
  padding: 40px 0;
}
</style>

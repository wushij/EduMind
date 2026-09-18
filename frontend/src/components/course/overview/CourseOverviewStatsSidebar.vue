<template>
  <div class="content-card stats-summary-card">
    <h4 class="card-sub-header">课程空间数据概览</h4>
    <div class="stat-items-grid">
      <div class="stat-pill-box">
        <span class="stat-val">{{ course?.chapterCount ?? 0 }}</span>
        <span class="stat-label">教学大纲章节</span>
      </div>
      <div class="stat-pill-box">
        <span class="stat-val">{{ course?.knowledgePointCount ?? 0 }}</span>
        <span class="stat-label">核心知识点</span>
      </div>
      <div class="stat-pill-box">
        <span class="stat-val">{{ course?.studentCount ?? 0 }}</span>
        <span class="stat-label">在读选课学生</span>
      </div>
      <div class="stat-pill-box">
        <span class="stat-val">{{ course?.resourceCount ?? 0 }}</span>
        <span class="stat-label">课件与教学资料</span>
      </div>
    </div>

    <div class="quick-cta-actions">
      <button
        type="button"
        class="capsule-block-btn capsule-block-btn--primary"
        :disabled="!courseId"
        @click="go(`/course/${courseId}/chapters`)"
      >
        <span>开始章节学习</span>
        <el-icon><ArrowRight /></el-icon>
      </button>
      <button
        type="button"
        class="capsule-block-btn capsule-block-btn--ai"
        :disabled="!courseId"
        @click="go(`/course/${courseId}/ai`)"
      >
        <span>向课程 AI 助教提问</span>
        <el-icon><Service /></el-icon>
      </button>
      <button
        type="button"
        class="capsule-block-btn capsule-block-btn--outline"
        :disabled="!courseId"
        @click="go(`/ai/question/generate?courseId=${courseId}`)"
      >
        <span>针对本课 AI 智能出题</span>
        <el-icon><EditPen /></el-icon>
      </button>
      <button
        type="button"
        class="capsule-block-btn capsule-block-btn--outline"
        @click="emit('knowledge-base')"
      >
        <span>{{ course?.knowledgeBaseId ? '进入关联知识库' : '关联学科知识库' }}</span>
        <el-icon><FolderOpened /></el-icon>
      </button>
      <button
        v-if="editable"
        type="button"
        class="capsule-block-btn capsule-block-btn--outline"
        @click="emit('edit-profile')"
      >
        <span>编辑修改课程档案</span>
        <el-icon><EditPen /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight, EditPen, FolderOpened, Service } from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';

const props = defineProps<{
  course?: Course | null;
  editable?: boolean;
}>();

const emit = defineEmits<{ 'edit-profile': []; 'knowledge-base': [] }>();

const router = useRouter();
const courseId = computed(() => props.course?.id);

function go(path: string) {
  if (!courseId.value) return;
  router.push(path);
}
</script>

<style scoped lang="scss">
@use './course-overview-shared.scss' as *;

.stats-summary-card {
  .card-sub-header {
    margin: 0 0 16px;
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
  }

  .stat-items-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    margin-bottom: 20px;

    .stat-pill-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 12px;
      text-align: center;

      .stat-val {
        font-size: 20px;
        font-weight: 700;
        color: #1677ff;
      }

      .stat-label {
        font-size: 11.5px;
        color: #64748b;
        margin-top: 2px;
        display: block;
      }
    }
  }

  .quick-cta-actions {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .capsule-block-btn {
      width: 100%;
      height: 42px;
      border-radius: 9999px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 20px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.22s ease;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &--primary {
        background: #1677ff;
        color: #fff;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);

        &:not(:disabled):hover {
          background: #0958d9;
          box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);
          transform: translateY(-1px);
        }

        &:not(:disabled):active {
          transform: translateY(0);
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);
        }
      }

      &--ai {
        background: linear-gradient(135deg, #eef2ff 0%, #faf5ff 100%);
        border: 1px solid #c7d2fe;
        color: #4f46e5;

        &:not(:disabled):hover {
          background: linear-gradient(135deg, #e0e7ff 0%, #f3e8ff 100%);
          border-color: #a5b4fc;
          color: #4338ca;
          box-shadow: 0 4px 12px rgba(79, 70, 229, 0.12);
          transform: translateY(-1px);
        }

        &:not(:disabled):active {
          transform: translateY(0);
        }
      }

      &--outline {
        background: #fff;
        border: 1px solid #e2e8f0;
        color: #334155;

        &:not(:disabled):hover {
          background: #f8fafc;
          border-color: #cbd5e1;
          color: #0f172a;
          box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
          transform: translateY(-1px);
        }

        &:not(:disabled):active {
          transform: translateY(0);
          background: #f1f5f9;
        }
      }
    }
  }
}
</style>

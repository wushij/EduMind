<template>
  <el-dialog
    :model-value="visible"
    width="540px"
    align-center
    :close-on-click-modal="false"
    class="capsule-success-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <template #header>
      <div class="dialog-header-custom">
        <el-icon class="header-success-icon"><CircleCheckFilled /></el-icon>
        <span>课程空间全链路创建成功</span>
      </div>
    </template>

    <div class="success-dialog-body">
      <div class="success-trophy-circle">
        <el-icon class="trophy-svg-icon"><Trophy /></el-icon>
      </div>
      <h3 class="success-course-title">《{{ createdCourseName }}》</h3>
      <p class="success-course-sub">
        课程档案、{{ createdCourseChaptersCount }} 个大纲章节、主讲教师权限与 AI 助教均已成功建立！
      </p>

      <div class="invitation-code-box">
        <span class="invitation-label">课程选课邀请码：</span>
        <span class="invitation-code">{{ courseCode }}</span>
        <button type="button" class="copy-code-btn" @click="$emit('copy-code')">复制</button>
      </div>

      <div class="next-step-actions">
        <button
          type="button"
          class="capsule-next-btn capsule-next-btn--primary"
          @click="$emit('go-to-course', 'overview')"
        >
          <el-icon class="btn-inner-icon"><Right /></el-icon>
          <span>立即进入课程教学空间概览</span>
        </button>
        <button
          type="button"
          class="capsule-next-btn capsule-next-btn--secondary"
          @click="$emit('go-to-course', 'chapters')"
        >
          <el-icon class="btn-inner-icon"><Reading /></el-icon>
          <span>继续完善大纲微课时与测验</span>
        </button>
        <button type="button" class="capsule-next-btn capsule-next-btn--neutral" @click="$emit('go-to-list')">
          返回课程中心列表
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { CircleCheckFilled, Trophy, Right, Reading } from '@element-plus/icons-vue';

defineProps<{
  visible: boolean;
  createdCourseName: string;
  courseCode: string;
  createdCourseChaptersCount: number;
}>();

defineEmits<{
  'update:visible': [value: boolean];
  'copy-code': [];
  'go-to-course': [tab: 'overview' | 'chapters'];
  'go-to-list': [];
}>();
</script>

<style scoped lang="scss">
:deep(.capsule-success-dialog) {
  border-radius: 24px;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding-bottom: 0;
  }
}

.dialog-header-custom {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;

  .header-success-icon {
    font-size: 20px;
    color: #10B981;
  }
}

.success-dialog-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 10px 10px 20px;

  .success-trophy-circle {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, #EFF6FF 0%, #FAF5FF 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;
    box-shadow: 0 4px 16px rgba(22, 119, 255, 0.15);

    .trophy-svg-icon {
      font-size: 32px;
      color: #1677FF;
    }
  }

  .success-course-title {
    margin: 0 0 6px 0;
    font-size: 18px;
    font-weight: 700;
    color: #0F172A;
  }

  .success-course-sub {
    margin: 0 0 18px 0;
    font-size: 13px;
    color: #64748B;
    line-height: 1.5;
  }

  .invitation-code-box {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: #F8FAFC;
    border: 1px dashed #CBD5E1;
    border-radius: 9999px;
    padding: 6px 16px;
    margin-bottom: 24px;

    .invitation-label {
      font-size: 12.5px;
      color: #64748B;
    }

    .invitation-code {
      font-size: 15px;
      font-weight: 700;
      color: #1677FF;
      font-family: monospace;
    }

    .copy-code-btn {
      background: transparent;
      border: none;
      color: #1677FF;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      padding: 0;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .next-step-actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;

    .capsule-next-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      height: 42px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      border: none;

      .btn-inner-icon {
        font-size: 16px;
      }

      &--primary {
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
        }
      }

      &--secondary {
        background: #EFF6FF;
        color: #1677FF;
        border: 1px solid #BFDBFE;

        &:hover {
          background: #DBEAFE;
        }
      }

      &--neutral {
        background: #FFFFFF;
        color: #64748B;
        border: 1px solid #E2E8F0;

        &:hover {
          background: #F8FAFC;
          color: #1E293B;
        }
      }
    }
  }
}
</style>

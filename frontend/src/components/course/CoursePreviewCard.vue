<template>
  <div class="course-preview-card">
    <div class="preview-header-bar">
      <div class="preview-title-wrap">
        <el-icon class="preview-title-icon"><Monitor /></el-icon>
        <span class="preview-title-text">【我的课程】实时卡片预览</span>
      </div>
      <span class="live-tag">LIVE PREVIEW</span>
    </div>

    <div class="live-course-card">
      <div
        class="live-card-cover"
        :class="currentCoverClass"
        :style="form.coverUrl ? { backgroundImage: `url(${form.coverUrl})` } : {}"
      >
        <div class="cover-glass-overlay"></div>
        <div class="cover-top-meta">
          <span class="capsule-badge capsule-badge--semester">{{ form.semester }}</span>
          <span class="capsule-badge capsule-badge--active">开课中</span>
        </div>
        <div class="cover-bottom-meta">
          <span class="capsule-badge capsule-badge--category">{{ form.category }}</span>
          <span class="capsule-badge capsule-badge--code">代码: {{ form.code || 'CS-NEW' }}</span>
        </div>
      </div>

      <div class="live-card-body">
        <h4 class="live-course-title">{{ form.name || '（待输入新课程全称）' }}</h4>
        <p class="live-course-desc">
          {{ form.description || '课程简介将展示在此处，为学生提供教学背景与目标引导...' }}
        </p>

        <div class="live-teacher-row">
          <div class="teacher-info">
            <div class="teacher-avatar">师</div>
            <span class="teacher-name">主讲教师 (当前账号)</span>
          </div>
          <div class="credit-pill">{{ form.credits }} 学分 · {{ form.plannedHours }} 学时</div>
        </div>

        <div class="live-stats-pill-group">
          <div class="stat-pill-item">
            <el-icon><List /></el-icon>
            <span>{{ form.initialChapters.length }} 教学大纲章节</span>
          </div>
          <div class="stat-pill-item">
            <el-icon><Connection /></el-icon>
            <span>{{ kbLabel }}</span>
          </div>
        </div>

        <div class="ai-assistant-live-pill">
          <el-icon class="ai-pill-icon"><Service /></el-icon>
          <span class="ai-text">
            专属 AI 助教：<strong>{{ currentPersonaName }}</strong> 已准备就绪
          </span>
        </div>

        <div class="live-card-footer">
          <button type="button" class="live-card-enter-btn" disabled>
            进入课程教学空间 (创建后点亮)
          </button>
        </div>
      </div>
    </div>

    <div class="initialization-checklist-card">
      <h5 class="checklist-title">
        <el-icon class="checklist-title-icon"><Lightning /></el-icon>
        <span>全链路创建时系统将自动完成：</span>
      </h5>
      <ul class="checklist-items">
        <li class="check-item">
          <el-icon class="check-icon"><Check /></el-icon>
          <span>在 <code>course</code> 持久化课程档案与邀请码</span>
        </li>
        <li class="check-item">
          <el-icon class="check-icon"><Check /></el-icon>
          <span>将 {{ form.initialChapters.length }} 个初始章节写入 <code>course_chapter</code></span>
        </li>
        <li class="check-item">
          <el-icon class="check-icon"><Check /></el-icon>
          <span>在 <code>course_member</code> 中自动将您设为主讲教师</span>
        </li>
        <li class="check-item">
          <el-icon class="check-icon"><Check /></el-icon>
          <span>挂载 {{ currentPersonaName }} 角色并生成专属问候语</span>
        </li>
        <li class="check-item">
          <el-icon class="check-icon"><Check /></el-icon>
          <span>开辟课程资料库 RAG 向量切片专属存储路径</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { List, Connection, Monitor, Service, Lightning, Check } from '@element-plus/icons-vue';
import type { CourseCreateFormState } from '@/composables/course/useCourseCreate';

defineProps<{
  form: CourseCreateFormState;
  currentCoverClass: string;
  kbLabel: string;
  currentPersonaName: string;
}>();
</script>

<style scoped lang="scss">
.course-preview-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 360px;

  @media (max-width: 1100px) {
    max-width: 100%;
  }

  .preview-header-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .preview-title-wrap {
      display: flex;
      align-items: center;
      gap: 6px;

      .preview-title-icon {
        font-size: 15px;
        color: #475569;
      }

      .preview-title-text {
        font-size: 13.5px;
        font-weight: 700;
        color: #334155;
      }
    }

    .live-tag {
      font-size: 10.5px;
      font-weight: 800;
      letter-spacing: 0.05em;
      padding: 2px 8px;
      border-radius: 9999px;
      background: #DEF7EC;
      color: #03543F;
    }
  }

  .live-course-card {
    background: #FFFFFF;
    border-radius: 20px;
    overflow: hidden;
    border: 1px solid #EBF1F7;
    box-shadow: 0 8px 24px rgba(30, 80, 150, 0.07);
    transition: all 0.22s;

    .live-card-cover {
      position: relative;
      height: 140px;
      background-size: cover;
      background-position: center;
      padding: 12px 14px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      &.grad-blue { background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 50%, #60A5FA 100%); }
      &.grad-purple { background: linear-gradient(135deg, #4C1D95 0%, #7C3AED 50%, #A78BFA 100%); }
      &.grad-cyan { background: linear-gradient(135deg, #064E3B 0%, #0D9488 50%, #2DD4BF 100%); }
      &.grad-indigo { background: linear-gradient(135deg, #1E1B4B 0%, #3730A3 50%, #6366F1 100%); }
      &.grad-amber { background: linear-gradient(135deg, #78350F 0%, #D97706 50%, #FBBF24 100%); }

      .cover-glass-overlay {
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, rgba(0, 0, 0, 0.1) 0%, rgba(0, 0, 0, 0.4) 100%);
      }

      .cover-top-meta,
      .cover-bottom-meta {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .capsule-badge {
        display: inline-block;
        padding: 2px 10px;
        border-radius: 9999px;
        font-size: 11px;
        font-weight: 600;

        &--semester {
          background: rgba(255, 255, 255, 0.9);
          color: #1E293B;
          backdrop-filter: blur(4px);
        }

        &--active {
          background: #10B981;
          color: #FFFFFF;
        }

        &--category {
          background: rgba(15, 23, 42, 0.7);
          color: #F8FAFC;
          backdrop-filter: blur(4px);
        }

        &--code {
          background: rgba(255, 255, 255, 0.9);
          color: #1677FF;
          font-family: monospace;
        }
      }
    }

    .live-card-body {
      padding: 16px 18px 18px;
      display: flex;
      flex-direction: column;
      gap: 12px;

      .live-course-title {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.4;
      }

      .live-course-desc {
        margin: 0;
        font-size: 12.5px;
        color: #64748B;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .live-teacher-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-top: 4px;

        .teacher-info {
          display: flex;
          align-items: center;
          gap: 8px;

          .teacher-avatar {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            background: #EFF6FF;
            color: #1677FF;
            font-size: 11px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
          }

          .teacher-name {
            font-size: 12px;
            color: #475569;
            font-weight: 500;
          }
        }

        .credit-pill {
          font-size: 11.5px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 8px;
          border-radius: 9999px;
        }
      }

      .live-stats-pill-group {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .stat-pill-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 3px 10px;
          background: #F8FAFC;
          border: 1px solid #EDF2F7;
          border-radius: 9999px;
          font-size: 11.5px;
          color: #475569;
          font-weight: 500;
        }
      }

      .ai-assistant-live-pill {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        background: linear-gradient(135deg, #F0FDF4 0%, #F5F3FF 100%);
        border: 1px solid #D1FAE5;
        border-radius: 12px;
        font-size: 12px;
        color: #065F46;

        .ai-pill-icon {
          font-size: 15px;
          color: #4F46E5;
        }

        strong {
          color: #4F46E5;
        }
      }

      .live-card-footer {
        margin-top: 4px;

        .live-card-enter-btn {
          width: 100%;
          height: 36px;
          border-radius: 9999px;
          border: 1px dashed #CBD5E1;
          background: #F8FAFC;
          color: #94A3B8;
          font-size: 12.5px;
          font-weight: 600;
          cursor: not-allowed;
        }
      }
    }
  }

  .initialization-checklist-card {
    background: #F8FAFC;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 16px;

    .checklist-title {
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 0 0 10px 0;
      font-size: 13px;
      font-weight: 700;
      color: #334155;

      .checklist-title-icon {
        font-size: 14px;
        color: #D97706;
      }
    }

    .checklist-items {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 7px;

      .check-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        color: #475569;

        .check-icon {
          color: #10B981;
          font-size: 13px;
          flex-shrink: 0;
        }

        code {
          background: #EDF2F7;
          padding: 1px 4px;
          border-radius: 4px;
          font-family: monospace;
          font-size: 11px;
        }
      }
    }
  }
}
</style>

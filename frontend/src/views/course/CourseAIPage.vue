<template>
  <div class="course-ai-page-root">
    <!-- 1. 加载中状态 -->
    <div v-if="loading" class="page-loading-card">
      <div class="loading-content">
        <el-icon class="is-loading spin-icon"><Loading /></el-icon>
        <span class="loading-text">正在检索当前学校租户学科课程空间...</span>
      </div>
    </div>

    <!-- 2. 无课程空状态：当前租户下没有课程时友好呈现，零报错，不带任何错误 ID -->
    <div v-else-if="courses.length === 0" class="course-ai-empty-container">
      <!-- 顶部大视觉 Banner -->
      <div class="empty-hero-banner">
        <div class="banner-overlay-content">
          <div class="hero-brand-row">
            <div class="hero-brand-squircle">
              <img class="hero-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
            </div>
            <div class="hero-brand-titles">
              <h1 class="hero-main-title">课程专属 AI 助教空间</h1>
              <p class="hero-sub-title">
                基于学科教学大纲与向量知识库，提供全天候深度答疑、重难点剖析与个性化伴学
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 中部空状态交互卡片 -->
      <div class="empty-card-wrapper">
        <div class="empty-illustration-box">
          <div class="glow-backdrop"></div>
          <div class="illustration-circle">
            <el-icon class="illustration-icon"><ChatDotRound /></el-icon>
          </div>
        </div>

        <div class="empty-info-texts">
          <h2 class="empty-title">当前学校 / 租户下暂无可用课程</h2>
          <p class="empty-desc">
            课程专属 AI 助教深度绑定具体学科的大纲体系、知识图谱与教学课件资料。<br />
            当前所选租户上下文中尚未创建或加入任何学科课程，无法开启专属 AI 对话。
          </p>
        </div>

        <div class="empty-actions-row">
          <template v-if="canCreateCourse">
            <button
              type="button"
              class="capsule-btn capsule-btn--primary"
              @click="router.push('/course/create')"
            >
              <el-icon><Plus /></el-icon>
              <span>创建首门课程</span>
            </button>
            <button
              type="button"
              class="capsule-btn capsule-btn--default"
              @click="router.push('/course')"
            >
              <el-icon><Collection /></el-icon>
              <span>进入课程中心</span>
            </button>
          </template>

          <template v-else>
            <button
              type="button"
              class="capsule-btn capsule-btn--primary"
              @click="router.push('/course')"
            >
              <el-icon><Reading /></el-icon>
              <span>加入班级课程</span>
            </button>
            <button
              type="button"
              class="capsule-btn capsule-btn--default"
              @click="router.push('/dashboard')"
            >
              <el-icon><HomeFilled /></el-icon>
              <span>返回系统工作台</span>
            </button>
          </template>
        </div>

        <div class="empty-hint-tag">
          <el-icon><InfoFilled /></el-icon>
          <span>多租户提示：若您所属的其他学校租户已有课程，可在右上角切换租户</span>
        </div>
      </div>
    </div>

    <!-- 3. 有课程状态：直接加载完整的 CourseAI 工作台，无任何跳转卡顿，立即可用 -->
    <CourseAIPanel
      v-else-if="activeCourse"
      :key="activeCourse.id"
      :course="activeCourse"
      @switch-course="handleSwitchActiveCourse"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'CourseAIPage'
});

import { useRouter } from 'vue-router';
import {
  Loading,
  ChatDotRound,
  Plus,
  Collection,
  Reading,
  HomeFilled,
  InfoFilled
} from '@element-plus/icons-vue';
import { useCourseAIPage } from '@/composables/course/useCourseAIWorkspace';
import CourseAIPanel from '@/components/course/CourseAIPanel.vue';

const router = useRouter();
const { loading, courses, activeCourse, canCreateCourse, handleSwitchActiveCourse } = useCourseAIPage();
</script>

<style scoped lang="scss">
.course-ai-page-root {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-sizing: border-box;

  .page-loading-card {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 440px;
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);

    .loading-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 14px;
      color: #64748B;
      font-size: 14px;

      .spin-icon {
        font-size: 32px;
        color: #1677FF;
      }
    }
  }

  .course-ai-empty-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
    width: 100%;

    .empty-hero-banner {
      width: 100%;
      border-radius: 20px;
      background: linear-gradient(135deg, #1E3A8A 0%, #2563EB 50%, #4F46E5 100%);
      padding: 32px 36px;
      box-sizing: border-box;
      box-shadow: 0 10px 30px rgba(37, 99, 235, 0.18);
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        right: -80px;
        top: -80px;
        width: 320px;
        height: 320px;
        border-radius: 50%;
        background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0) 70%);
        pointer-events: none;
      }

      .banner-overlay-content {
        position: relative;
        z-index: 2;

        .hero-brand-row {
          display: flex;
          align-items: center;
          gap: 20px;

          .hero-brand-squircle {
            width: 60px;
            height: 60px;
            border-radius: 18px;
            background: rgba(255, 255, 255, 0.95);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
            flex-shrink: 0;

            .hero-brand-logo {
              width: 38px;
              height: 38px;
              object-fit: contain;
            }
          }

          .hero-brand-titles {
            .hero-main-title {
              margin: 0;
              font-size: 24px;
              font-weight: 700;
              color: #FFFFFF;
              letter-spacing: -0.3px;
            }

            .hero-sub-title {
              margin: 6px 0 0;
              font-size: 13.5px;
              color: rgba(255, 255, 255, 0.85);
              max-width: 720px;
              line-height: 1.5;
            }
          }
        }
      }
    }

    .empty-card-wrapper {
      background: #FFFFFF;
      border-radius: 20px;
      padding: 60px 32px 50px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 4px 24px rgba(15, 23, 42, 0.04);
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;

      .empty-illustration-box {
        position: relative;
        margin-bottom: 24px;

        .glow-backdrop {
          position: absolute;
          left: 50%;
          top: 50%;
          transform: translate(-50%, -50%);
          width: 140px;
          height: 140px;
          border-radius: 50%;
          background: radial-gradient(circle, rgba(22, 119, 255, 0.12) 0%, rgba(22, 119, 255, 0) 70%);
        }

        .illustration-circle {
          position: relative;
          z-index: 2;
          width: 88px;
          height: 88px;
          border-radius: 50%;
          background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
          border: 2px solid #BFDBFE;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #1677FF;
          font-size: 40px;
          box-shadow: 0 8px 24px rgba(22, 119, 255, 0.15);
        }
      }

      .empty-info-texts {
        max-width: 580px;

        .empty-title {
          margin: 0 0 12px;
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
        }

        .empty-desc {
          margin: 0 0 32px;
          font-size: 14px;
          color: #64748B;
          line-height: 1.65;
        }
      }

      .empty-actions-row {
        display: flex;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
        justify-content: center;

        .capsule-btn {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          height: 44px;
          padding: 0 26px;
          border-radius: 9999px;
          font-size: 14px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
          border: 1px solid transparent;

          &--primary {
            background: linear-gradient(135deg, #1677FF 0%, #2563EB 100%);
            color: #FFFFFF;
            box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 6px 20px rgba(22, 119, 255, 0.4);
            }
          }

          &--default {
            background: #F8FAFC;
            border-color: #CBD5E1;
            color: #334155;

            &:hover {
              background: #F1F5F9;
              border-color: #94A3B8;
              color: #0F172A;
            }
          }
        }
      }

      .empty-hint-tag {
        margin-top: 36px;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 12.5px;
        color: #94A3B8;
        padding: 6px 14px;
        background: #F8FAFC;
        border-radius: 9999px;
        border: 1px solid #E2E8F0;
      }
    }
  }
}
</style>

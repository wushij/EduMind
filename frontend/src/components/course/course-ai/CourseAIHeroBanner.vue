<template>
<!-- 1. 顶部专属视觉大 Banner -->
    <div class="course-ai-hero-banner">
      <div class="banner-overlay-content">
        <div class="hero-left-section">
          <div class="hero-header-row">
            <!-- 渐变立体图标盒 -->
            <div class="hero-icon-squircle">
              <img class="hero-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
            </div>
            <div class="hero-titles">
              <div class="hero-title-row">
                <h2 class="hero-course-title">
                  {{ displayCourseTitle }} · AI 助手
                </h2>
                <!-- 切换当前学校租户名下其他课程 -->
                <el-dropdown
                  v-if="tenantCourseOptions.length > 1"
                  trigger="click"
                  @command="handleSwitchCourse"
                >
                  <button type="button" class="course-switch-pill" title="切换当前所选课程">
                    <span>切换课程</span>
                    <el-icon class="arrow"><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu class="course-dropdown-menu">
                      <el-dropdown-item
                        v-for="c in tenantCourseOptions"
                        :key="c.id"
                        :command="c.id"
                        :class="{ 'is-selected': Number(c.id) === currentCourseIdNum }"
                      >
                        {{ c.title || c.name }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <p class="hero-course-desc">
                基于学科知识库与教学大纲，为你提供专业、准确、个性化的学习支持
              </p>
            </div>
          </div>

          <!-- 5 个核心功能长圆跑道胶囊按钮 (原型同款) -->
          <div class="hero-capsules-row">
            <button
              v-for="pill in quickActionPills"
              :key="pill.title"
              type="button"
              class="hero-action-pill"
              @click="handlePillClick(pill)"
            >
              <el-icon class="pill-icon" :style="{ color: pill.color }">
                <component :is="pill.icon" />
              </el-icon>
              <span class="pill-text">{{ pill.title }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { ArrowDown } from '@element-plus/icons-vue';
import { courseAiUiKey } from '@/components/course/course-ai/course-ai-ui-key';

const {
  displayCourseTitle,
  currentCourseIdNum,
  tenantCourseOptions,
  handleSwitchCourse,
  quickActionPills,
  handlePillClick
} = inject(courseAiUiKey)!;
</script>

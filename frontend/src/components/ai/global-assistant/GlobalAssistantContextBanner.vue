<template>
<!-- 课程研读上下文横条 (动态感知，杜绝写死) -->
            <div class="context-banner">
              <div class="context-pill">
                <el-icon class="context-icon"><Reading /></el-icon>
                <span class="context-label">
                  {{ isLessonStudioContext ? '课节备课上下文：' : '当前教学研读空间：' }}
                </span>
                <span class="context-title" :title="activeCourseLabel">
                  {{ activeCourseLabel }}
                </span>
              </div>
              <button
                type="button"
                class="context-switch-btn"
                :title="manualGlobalScope ? '同步当前页面研读空间' : '切换为全域通用研读模式'"
                @click="toggleScopeMode"
              >
                <el-icon :size="12"><Switch /></el-icon>
                <span>{{ manualGlobalScope ? '同步页面' : '全域模式' }}</span>
              </button>
            </div>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { Reading, Switch } from '@element-plus/icons-vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const { activeCourseLabel, manualGlobalScope, toggleScopeMode, isLessonStudioContext } =
  inject(globalAssistantUiKey)!;
</script>

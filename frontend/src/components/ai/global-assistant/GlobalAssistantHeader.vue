<template>
<!-- 1. 顶部 Header -->
            <header class="copilot-header">
              <div class="header-left">
                <div class="copilot-avatar">
                  <img :src="logoImg" alt="智教云 AI" class="copilot-header-logo" />
                </div>
                <div class="header-info">
                  <div class="title-row">
                    <h3 class="copilot-title">智教云 · AI 教学副驾驶</h3>
                    <span class="badge-rag">RAG 知识图谱增强</span>
                  </div>
                  <p class="copilot-subtitle">基于课程知识库与教学大模型深度检索，精准解答教学与学情难点</p>
                </div>
              </div>

              <div class="header-actions">
                <div class="header-toolbar" role="toolbar" aria-label="操作">
                  <!-- 不再提供模型自选入口：统一使用后台「AI 模型配置」中标为默认(is_default)的对话模型 -->
                  <button
                    class="header-btn"
                    :class="{ 'is-active': isWideMode }"
                    :title="isWideMode ? '恢复标准侧边栏 (640px)' : '展开宽屏研读工作台 (920px)'"
                    aria-label="切换侧边栏宽度"
                    @click="isWideMode = !isWideMode"
                  >
                    <el-icon :size="15"><FullScreen v-if="!isWideMode" /><Aim v-else /></el-icon>
                  </button>

                  <span class="header-toolbar-sep" aria-hidden="true" />

                  <!-- 历史对话记录按钮 (对标 Code Compass 原生交互) -->
                  <button
                    class="header-btn"
                    :class="{ 'is-active': isHistoryPanelOpen }"
                    :title="isHistoryPanelOpen ? '收起历史会话' : '历史对话记录'"
                    aria-label="历史对话记录"
                    @click="toggleHistoryPanel"
                  >
                    <el-icon :size="15"><Clock /></el-icon>
                  </button>

                  <span class="header-toolbar-sep" aria-hidden="true" />

                  <button
                    class="header-btn"
                    title="开启新会话"
                    aria-label="新对话"
                    @click="startNewSession"
                  >
                    <el-icon :size="15"><Plus /></el-icon>
                  </button>

                  <span class="header-toolbar-sep" aria-hidden="true" />

                  <button
                    class="header-btn"
                    title="清空当前对话记录"
                    aria-label="清空对话"
                    @click="clearMessages"
                  >
                    <el-icon :size="15"><Delete /></el-icon>
                  </button>

                  <span class="header-toolbar-sep" aria-hidden="true" />

                  <button
                    class="header-btn header-btn--close"
                    title="关闭 (Esc / Alt+C)"
                    aria-label="关闭"
                    @click="drawerVisible = false"
                  >
                    <el-icon :size="15"><Close /></el-icon>
                  </button>
                </div>
              </div>
            </header>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { Plus, Close, Delete, FullScreen, Aim, Clock } from '@element-plus/icons-vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const {
  logoImg,
  isWideMode,
  isHistoryPanelOpen,
  toggleHistoryPanel,
  startNewSession,
  clearMessages,
  drawerVisible
} = inject(globalAssistantUiKey)!;
</script>

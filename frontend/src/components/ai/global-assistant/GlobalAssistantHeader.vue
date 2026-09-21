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
                  <!-- 大模型选择器下拉框 (与课程中心 1:1 对齐规范) -->
                  <el-dropdown trigger="click" @command="handleModelSelect">
                    <button type="button" class="header-model-btn" title="切换当前 AI 推理引擎">
                      <span class="model-sparkle">✦</span>
                      <span class="model-name">{{ currentModel }}</span>
                      <el-icon class="model-arrow" :size="12"><ArrowDown /></el-icon>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu class="model-dropdown-menu ga-model-menu">
                        <el-dropdown-item
                          v-for="m in modelOptions"
                          :key="m.key"
                          :command="m.key"
                          :class="{ 'is-selected': currentModelKey === m.key }"
                        >
                          <div class="model-item-row">
                            <span class="item-name">{{ m.name }}</span>
                            <span class="item-badge">{{ m.desc }}</span>
                          </div>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>

                  <span class="header-toolbar-sep" aria-hidden="true" />

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
import { Plus, Close, Delete, FullScreen, Aim, Clock, ArrowDown } from '@element-plus/icons-vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const {
  logoImg,
  isWideMode,
  isHistoryPanelOpen,
  toggleHistoryPanel,
  startNewSession,
  clearMessages,
  drawerVisible,
  modelOptions,
  currentModel,
  currentModelKey,
  handleModelSelect
} = inject(globalAssistantUiKey)!;
</script>

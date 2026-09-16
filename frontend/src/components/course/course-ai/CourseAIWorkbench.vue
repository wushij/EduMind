<template>
<!-- 2. 原型同款三栏式教学工作台 (左: 课程章节 | 中: AI 问答主工作台 | 右: 辅助学习面板) -->
    <div class="course-ai-grid-workbench">
      <!-- 左栏：课程章节 (240px) -->
      <aside class="workbench-col workbench-col--chapters">
        <div class="col-card chapters-card">
          <div class="col-card-header">
            <span class="header-title">课程章节</span>
            <span class="count-badge">{{ chaptersData.length }}章</span>
          </div>

          <!-- 章节搜索框 -->
          <div class="chapters-search-box">
            <el-input
              v-model="chapterKeyword"
              placeholder="搜索章节内容..."
              size="small"
              clearable
              class="chapter-search-input"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 章节可折叠树列表 -->
          <div class="chapters-scroll-area">
            <div
              v-for="chapter in filteredChapters"
              :key="chapter.id"
              class="chapter-group-item"
            >
              <div
                class="chapter-header-row"
                @click="chapter.expanded = !chapter.expanded"
              >
                <el-icon class="arrow-icon">
                  <ArrowDown v-if="chapter.expanded" />
                  <ArrowRight v-else />
                </el-icon>
                <span class="chapter-label" :title="chapter.title">{{ chapter.title }}</span>
              </div>

              <!-- 二级节列表 -->
              <div v-show="chapter.expanded" class="sections-sub-list">
                <div
                  v-for="sec in chapter.sections"
                  :key="sec.id"
                  class="section-leaf-row"
                  :class="{ active: activeSectionId === sec.id }"
                  @click="selectSection(sec)"
                >
                  <span class="section-dot"></span>
                  <span class="section-title" :title="sec.title">{{ sec.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中栏：AI 问答主工作台 (Flex 1) -->
      <main class="workbench-col workbench-col--chat">
        <div class="col-card chat-workbench-card">
          <!-- 工作台顶栏：模式切换、模型选择、历史记录 -->
          <div class="chat-top-toolbar">
            <div class="mode-capsule-tabs">
              <button
                v-for="tab in modeTabs"
                :key="tab.key"
                type="button"
                class="mode-pill-tab"
                :class="{ active: currentModeTab === tab.key }"
                @click="switchModeTab(tab.key)"
              >
                {{ tab.label }}
              </button>
            </div>

            <div class="toolbar-right-actions">
              <!-- 大模型选择器下拉框 -->
              <el-dropdown trigger="click" @command="handleModelSelect">
                <button type="button" class="toolbar-pill-btn model-selector-btn">
                  <span class="model-name">{{ currentModel }}</span>
                  <el-icon class="arrow"><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu class="model-dropdown-menu">
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

              <!-- 开启新对话 -->
              <button
                type="button"
                class="toolbar-pill-btn new-chat-btn"
                title="开启新问答会话"
                @click="handleCreateNewSession"
              >
                <el-icon><Plus /></el-icon>
                <span>新对话</span>
              </button>

              <!-- 历史记录抽屉触发按钮 -->
              <button
                type="button"
                class="toolbar-pill-btn history-trigger-btn"
                @click="historyDrawerVisible = true"
              >
                <el-icon><Clock /></el-icon>
                <span>历史记录</span>
              </button>
            </div>
          </div>

          <!-- 消息流展示滚动区 -->
          <div
            ref="messagesScrollRef"
            class="messages-flow-scroll"
            data-chat-scroll="true"
            @scroll="handleViewportScroll"
          >
            <!-- 章节锚定上下文小浮条 -->
            <div v-if="activeSectionTitle" class="context-anchor-chip">
              <el-icon class="pin-icon"><Connection /></el-icon>
              <span>当前知识锚定章节：<strong>{{ activeSectionTitle }}</strong></span>
              <button type="button" class="clear-anchor-btn" @click="activeSectionId = null; activeChapterId = undefined; activeSectionTitle = ''">
                <el-icon><Close /></el-icon>
              </button>
            </div>

            <!-- 历史已结算消息列表 -->
            <ChatMessage
              v-for="(msg, idx) in allDisplayMessages"
              :key="msg.id || idx"
              :message="msg"
              :is-last="idx === allDisplayMessages.length - 1 && !streaming"
              :follow-up-prompts="followUpPrompts"
              @send-prompt="handleSendPrompt"
              @regenerate="handleRegenerate(idx)"
              @delete="confirmDeleteMessage(idx)"
              @reasoning-collapse="pauseAutoScrollFollow"
            />

            <!-- 正在流式生成的进行时消息卡片 (对标侧边栏真流式) -->
            <div v-if="streaming" class="streaming-active-row">
              <div class="streaming-avatar-box">
                <img class="assistant-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
              </div>
              <div class="streaming-body-box">
                <div class="msg-meta-header">
                  <span class="sender-name">EduMind 课程 AI 助教</span>
                  <span class="streaming-badge-tag">正在生成研读解析...</span>
                </div>

                <!-- 深度思考状态卡片 (动态脉冲，正文输出时自动折叠) -->
                <AIThinking
                  v-if="showThinkingPanel && (!streamingAnswerBody || streamingThinkingDisplay)"
                  :content="streamingThinkingDisplay"
                  :folded="isReasoningFolded"
                  :active="!streamingAnswerBody && (isReasoningActive || !streamingThinkingDisplay)"
                  :has-answer-body="!!streamingAnswerBody"
                  :phase-message="streamPhaseMessage || '正在深度研读本门课程知识大纲与切片...'"
                  @update:folded="isReasoningFolded = $event"
                  @user-collapse="pauseAutoScrollFollow"
                />

                <!-- 正文流式渲染 (useStreamingMarkdown 增量输出) -->
                <div v-if="streamingAnswerBody" class="msg-bubble is-streaming">
                  <div class="markdown-body chat-md-content" v-html="streamingRenderedHtml" />
                  <span class="stream-cursor">▋</span>
                </div>
              </div>
            </div>

            <!-- 不可见物理锚点，用于 requestAnimationFrame 顺畅跟随贴底向上滚动 -->
            <div ref="streamAnchorRef" class="stream-bottom-anchor" />
          </div>

          <transition name="fade">
            <button
              v-if="showScrollToBottom"
              class="scroll-bottom-btn"
              type="button"
              aria-label="回到底部"
              title="查看最新回复"
              @click="scrollToBottomSmooth"
            >
              <svg class="scroll-bottom-svg" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="12" r="10" class="scroll-bottom-svg__bg" />
                <path d="M12 7.5v6.5" class="scroll-bottom-svg__shaft" />
                <path d="M8.8 11.8 12 15 15.2 11.8" class="scroll-bottom-svg__head" />
              </svg>
            </button>
          </transition>

          <!-- 底部多功能自适应输入框组件 -->
          <ChatInput
            ref="chatInputRef"
            class="chat-input-dock"
            :streaming="streaming"
            @send="handleSend"
            @stop="stopStream"
          />
        </div>
      </main>

      <!-- 右栏：辅助学习面板 (280px) -->
      <aside class="workbench-col workbench-col--assist">
        <!-- 卡片 1：课程相关资源 -->
        <div class="col-card assist-card resources-assist-card">
          <div class="col-card-header">
            <span class="header-title">课程相关资源</span>
            <button type="button" class="more-link" @click="navigateToResources">
              <span>更多</span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <!-- 资源分类筛选小胶囊 -->
          <div class="resource-filter-pills">
            <button
              v-for="cat in resourceCategories"
              :key="cat"
              type="button"
              class="filter-pill"
              :class="{ active: activeResourceCat === cat }"
              @click="activeResourceCat = cat"
            >
              {{ cat }}
            </button>
          </div>

          <!-- 资源文件列表 -->
          <div class="resource-items-list" v-loading="resourcesLoading">
            <div
              v-for="res in filteredResources"
              :key="res.title"
              class="resource-file-row"
              @click="downloadResource(res)"
            >
              <div class="file-badge-box" :class="`file-badge-box--${res.type}`">
                <span class="badge-text">{{ res.ext }}</span>
              </div>
              <span class="file-name" :title="res.title">{{ res.title }}</span>
              <button type="button" class="file-download-btn" title="下载资料">
                <el-icon><Download /></el-icon>
              </button>
            </div>
            <div v-if="filteredResources.length === 0 && !resourcesLoading" class="resource-empty-box">
              <el-icon><Document /></el-icon>
              <span>当前分类暂无教学资料</span>
            </div>
          </div>
        </div>

        <!-- 卡片 2：推荐问题 (精选最多 5 个高频点击直问) -->
        <div class="col-card assist-card prompts-assist-card">
          <div class="col-card-header">
            <span class="header-title">推荐问题</span>
          </div>

          <div class="recommended-prompts-list">
            <div
              v-for="(question, qIdx) in recommendedQuestions.slice(0, 5)"
              :key="qIdx"
              class="prompt-item-row"
              @click="handleSendRecommended(question)"
            >
              <el-icon class="prompt-arrow-icon"><ArrowRight /></el-icon>
              <span class="prompt-text" :title="question">{{ question }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import {
  Search,
  ArrowDown,
  ArrowRight,
  Clock,
  Connection,
  Download,
  Plus,
  Document,
  Close
} from '@element-plus/icons-vue';
import ChatMessage from '@/components/ai/ChatMessage.vue';
import ChatInput from '@/components/ai/ChatInput.vue';
import AIThinking from '@/components/ai/AIChat/AIThinking.vue';
import { courseAiUiKey } from '@/components/course/course-ai/course-ai-ui-key';

const {
  chapterKeyword,
  chaptersData,
  filteredChapters,
  activeChapterId,
  activeSectionId,
  selectSection,
  modeTabs,
  currentModeTab,
  switchModeTab,
  modelOptions,
  currentModel,
  currentModelKey,
  handleModelSelect,
  historyDrawerVisible,
  handleCreateNewSession,
  chatInputRef,
  streaming,
  messagesScrollRef,
  streamAnchorRef,
  streamingRenderedHtml,
  streamingAnswerBody,
  streamingThinkingDisplay,
  isReasoningFolded,
  isReasoningActive,
  streamPhaseMessage,
  followUpPrompts,
  showThinkingPanel,
  allDisplayMessages,
  handleSend,
  handleSendPrompt,
  handleRegenerate,
  confirmDeleteMessage,
  stopStream,
  scrollToBottomSmooth,
  showScrollToBottom,
  pauseAutoScrollFollow,
  handleViewportScroll,
  resourceCategories,
  activeResourceCat,
  resourcesLoading,
  filteredResources,
  downloadResource,
  navigateToResources,
  recommendedQuestions,
  handleSendRecommended,
  activeSectionTitle
} = inject(courseAiUiKey)!;
</script>

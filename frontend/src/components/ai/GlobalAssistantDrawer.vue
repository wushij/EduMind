<template>
  <div class="global-assistant-root">
    <!-- 1. 悬浮罗盘触发球 -->
    <GlobalAssistantTrigger
      :active="drawerVisible"
      :is-streaming="isStreaming"
      @toggle="toggleDrawer"
    />

    <!-- 2. 全局侧边抽屉面板 (Teleport 至 body，避免受父级 stacking context 约束) -->
    <teleport to="body">
      <transition name="copilot-drawer">
        <div
          v-if="drawerVisible"
          class="copilot-drawer-overlay"
          @click.self="drawerVisible = false"
          @wheel.self.prevent
          @touchmove.self.prevent
        >
          <div class="copilot-drawer-panel" :class="{ 'is-wide-panel': isWideMode }" @wheel.stop>
            <!-- 侧边环境极光发光边框 -->
            <div class="panel-glow-border" />

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

            <!-- 课程研读上下文横条 (动态感知，杜绝写死) -->
            <div class="context-banner">
              <div class="context-pill">
                <el-icon class="context-icon"><Reading /></el-icon>
                <span class="context-label">当前教学研读空间：</span>
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

            <!-- 历史会话下拉层 (对标 Code Compass 滑动面板) -->
            <transition name="slide-history">
              <div v-if="isHistoryPanelOpen" class="history-panel">
                <div class="history-panel-header">
                  <span class="history-title">历史问答会话</span>
                  <div class="history-header-actions">
                    <button
                      v-if="sessions.length > 0"
                      type="button"
                      class="history-clear-btn"
                      @click="confirmClearAllSessions"
                    >
                      清空全部
                    </button>
                    <button type="button" class="history-new-btn" @click="startNewSession">
                      + 新会话
                    </button>
                  </div>
                </div>

                <div v-if="sessions.length > 0" class="history-list-scroll">
                  <div
                    v-for="item in sessions"
                    :key="item.id"
                    class="history-item"
                    :class="{ 'is-current': conversationId === item.id }"
                    @click="selectSession(item)"
                  >
                    <div class="history-item-content">
                      <span class="history-item-title" :title="item.title">{{ item.title }}</span>
                      <span class="history-item-time">{{ formatSessionTime(item.updatedAt) }}</span>
                    </div>
                    <button
                      type="button"
                      class="history-delete-btn"
                      title="删除会话"
                      @click.stop="confirmDeleteSession(item.id)"
                    >
                      <el-icon :size="13"><Delete /></el-icon>
                    </button>
                  </div>
                </div>
                <div v-else class="history-empty">
                  <p>暂无历史会话记录</p>
                </div>
              </div>
            </transition>

            <!-- 2. 消息流式主列表区域 -->
            <div
              ref="messagesScrollRef"
              class="chat-viewport"
              @scroll="handleViewportScroll"
            >
              <div v-if="isSessionLoading" class="session-loading">
                <span class="thinking-spinner" />
                <span>正在加载会话记录...</span>
              </div>

              <!-- 空状态：对标 Code Compass 居中呼吸感同心轨道设计 -->
              <div v-else-if="messages.length === 0 && !isStreaming" class="empty-state">
                <div class="empty-illustration">
                  <div class="empty-orbit empty-orbit--outer" />
                  <div class="empty-orbit empty-orbit--inner" />
                  <div class="empty-core">
                    <img :src="logoImg" alt="EduMind Logo" class="empty-core-logo" />
                  </div>
                </div>
                <h3 class="empty-title">今天想探讨什么教学课题？</h3>
                <p class="empty-subtitle">
                  已关联当前课程知识库与题库切片，为你提供知识溯源、智能组卷与学情推理的深度伴学
                </p>

                <!-- 快捷探索推荐长条胶囊 (告别单调方块按钮) -->
                <div class="empty-prompts-wrap">
                  <div class="empty-prompts-header">
                    <span class="prompts-tag">✦ 快捷探索推荐</span>
                    <span class="prompts-hint">点击即问</span>
                  </div>
                  <div class="empty-prompts-list">
                    <button
                      v-for="(chip, idx) in presetChips"
                      :key="idx"
                      type="button"
                      class="empty-prompt-pill"
                      :title="chip.prompt"
                      @click="handleSendPrompt(chip.prompt)"
                    >
                      <span class="pill-sparkle">✦</span>
                      <span class="pill-text">{{ chip.prompt }}</span>
                      <span class="pill-arrow">↗</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 历史消息列表 -->
              <div v-else class="message-stream">
                <div
                  v-for="(msg, idx) in messages"
                  :key="msg.id || idx"
                  class="message-row"
                  :class="msg.role === 'user' ? 'message-row-user' : 'message-row-assistant'"
                >
                  <!-- 助手头像：使用官方 Logo 并与回答卡片顶对齐 -->
                  <div v-if="msg.role === 'assistant'" class="msg-avatar assistant-avatar">
                    <img :src="logoImg" alt="智教云 AI" class="assistant-avatar-img" />
                  </div>

                  <!-- 消息主内容列 -->
                  <div class="message-content-col">
                    <!-- 气泡主体 (意图识别置于气泡内顶部，使头像与气泡自然平齐) -->
                    <div class="message-bubble" :class="`bubble-${msg.role}`">
                      <!-- 仅在特殊意图（组卷、跳转、Agent任务等）且非普通chat才展示轻量意图标注 -->
                      <div
                        v-if="msg.intent && msg.intent !== 'chat' && msg.intent !== 'CHAT' && msg.role === 'assistant'"
                        class="bubble-intent-row"
                      >
                        <el-tag size="small" :type="getIntentTagType(msg.intent)" effect="dark" round>
                          {{ msg.intentDesc || msg.intent }}
                        </el-tag>
                        <el-button
                          v-if="msg.targetCode && msg.targetCode.startsWith('/')"
                          size="small"
                          type="primary"
                          link
                          @click="handleNavigate(msg.targetCode)"
                        >
                          点击直达功能模块 →
                        </el-button>
                      </div>

                      <!-- 深度思考卡片 (默认折叠) -->
                      <AIThinking
                        v-if="msg.role === 'assistant' && msg.reasoningContent"
                        :content="msg.reasoningContent"
                        :folded="msg.reasoningFolded ?? true"
                        :active="false"
                        :has-answer-body="true"
                        @update:folded="msg.reasoningFolded = $event"
                      />

                      <!-- Markdown 渲染正文 -->
                      <div
                        v-if="msg.content?.trim()"
                        class="markdown-body chat-md-content"
                        v-html="renderChatMarkdown(msg.content)"
                      />

                      <!-- 引用切片溯源卡片 (若存在 citations) -->
                      <div
                        v-if="msg.citations && msg.citations.length > 0"
                        class="citations-tray"
                      >
                        <div class="citations-header">
                          <span class="citations-badge"><el-icon><Reading /></el-icon> 参考课程知识库切片</span>
                          <span class="citations-sub">点击直达知识库详情</span>
                        </div>
                        <div class="citations-cards">
                          <div
                            v-for="(item, cIdx) in msg.citations"
                            :key="cIdx"
                            class="citation-card"
                            @click="jumpToCitation(item)"
                          >
                            <div class="citation-top">
                              <span class="citation-idx">[{{ cIdx + 1 }}]</span>
                              <span class="citation-doc">《{{ item.documentName || '课程核心资料' }}》</span>
                              <span class="citation-match">匹配度 {{ formatMatchScore(item.score) }}</span>
                            </div>
                            <div v-if="item.excerpt" class="citation-excerpt">
                              {{ item.excerpt }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 消息快捷操作栏：复制、重新生成、删除 (删除带二次确认) -->
                    <div class="message-actions" :class="{ 'is-user-actions': msg.role === 'user' }">
                      <button
                        type="button"
                        class="action-chip"
                        title="复制内容"
                        @click="copyMessage(msg.content)"
                      >
                        <el-icon :size="12"><CopyDocument /></el-icon>
                        <span class="action-chip-text">复制</span>
                      </button>

                      <button
                        v-if="msg.role === 'assistant'"
                        type="button"
                        class="action-chip"
                        title="重新生成回答"
                        :disabled="isStreaming"
                        @click="handleRegenerate(idx)"
                      >
                        <el-icon :size="12"><RefreshRight /></el-icon>
                        <span class="action-chip-text">重新生成</span>
                      </button>

                      <button
                        type="button"
                        class="action-chip action-chip--danger"
                        title="删除本条对话记录"
                        @click="confirmDeleteMessage(idx)"
                      >
                        <el-icon :size="12"><Delete /></el-icon>
                        <span class="action-chip-text">删除</span>
                      </button>
                    </div>

                    <!-- 推荐追问胶囊 (仅在最后一条助手回答完毕后展示) -->
                    <div
                      v-if="!isStreaming && msg.role === 'assistant' && idx === messages.length - 1 && followUpPrompts.length > 0"
                      class="message-followup-tray"
                    >
                      <div class="followup-tray-title">
                        <span class="followup-sparkle">✦</span>
                        <span>推荐继续探索：</span>
                      </div>
                      <div class="followup-pills-list">
                        <button
                          v-for="(fp, fIdx) in followUpPrompts"
                          :key="fIdx"
                          type="button"
                          class="followup-pill-btn"
                          :title="fp"
                          @click="handleSendPrompt(fp)"
                        >
                          <span class="followup-pill-text">{{ fp }}</span>
                          <span class="followup-pill-arrow">↗</span>
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- 用户头像 -->
                  <div v-if="msg.role === 'user'" class="msg-avatar user-avatar">
                    <img
                      v-if="userAvatarSrc && !userAvatarBroken"
                      :src="userAvatarSrc"
                      class="avatar-img"
                      alt="avatar"
                      @error="userAvatarBroken = true"
                    />
                    <span v-else class="avatar-fallback">{{ userAvatarFallback }}</span>
                  </div>
                </div>

                <!-- 3. 正在流式生成的进行时消息 -->
                <div v-if="isStreaming" class="message-row message-row-assistant">
                  <div class="msg-avatar assistant-avatar">
                    <img :src="logoImg" alt="智教云 AI" class="assistant-avatar-img" />
                  </div>

                  <div class="message-content-col">
                    <div class="message-bubble bubble-assistant is-streaming-bubble">
                      <!-- 意图识别胶囊 (常规问答不展示，非chat特殊功能才展示) -->
                      <div
                        v-if="streamingIntent.intent && streamingIntent.intent !== 'chat' && streamingIntent.intent !== 'CHAT'"
                        class="bubble-intent-row"
                      >
                        <el-tag size="small" :type="getIntentTagType(streamingIntent.intent)" effect="dark" round>
                          {{ streamingIntent.intentDesc || streamingIntent.intent }}
                        </el-tag>
                      </div>

                      <!-- 思考卡片：流式生成中动态自转，默认折叠 -->
                      <AIThinking
                        v-if="!streamingAnswerBody || streamingThinkingDisplay"
                        :content="streamingThinkingDisplay"
                        :folded="isReasoningFolded"
                        :active="!streamingAnswerBody && (isReasoningActive || !streamingThinkingDisplay)"
                        :has-answer-body="!!streamingAnswerBody"
                        :phase-message="streamPhaseMessage"
                        @update:folded="isReasoningFolded = $event"
                      />

                      <!-- 正文流式渲染：rAF + 80ms 节流 -->
                      <div
                        v-if="streamingAnswerBody"
                        class="markdown-body chat-md-content"
                        v-html="streamingRenderedHtml"
                      />

                      <!-- 流式打字机闪烁光标 -->
                      <span v-if="isStreaming && streamingAnswerBody" class="streaming-cursor" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 流式输出平滑跟随锚点与底部留白 -->
              <div ref="streamAnchorRef" class="stream-follow-anchor" aria-hidden="true" />
              <div class="chat-bottom-spacer" aria-hidden="true" />
            </div>

            <!-- 悬浮微圆钮：轻巧触底按钮 (不遮挡正文) -->
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

            <!-- 4. 底部输入栏 -->
            <footer class="copilot-footer">
              <!-- 自适应卡片输入框 -->
              <div class="composer" :class="{ 'is-expanded': isMultiline }">
                <div class="composer-gem-indicator">
                  <span class="composer-gem-sparkle">✦</span>
                </div>

                <div class="composer-body">
                  <textarea
                    ref="textareaRef"
                    v-model="inputContent"
                    class="chat-textarea"
                    :rows="isMultiline ? 2 : 1"
                    :placeholder="isStreaming ? 'AI 正在推理作答中...' : (isMultiline ? '输入教学问题或备课需求... (Enter 发送, Shift+Enter 换行)' : '输入教学问题、备课需求或考点探讨... (Enter 发送)')"
                    :disabled="isStreaming"
                    @keydown="handleTextareaKeydown"
                    @input="handleInput"
                  />
                </div>

                <div class="composer-toolbar" :class="{ 'is-inline': !isMultiline, 'is-bottom': isMultiline }">
                  <div v-if="isMultiline" class="toolbar-left">
                    <span class="input-shortcut-hint">Enter 发送 · Shift+Enter 换行</span>
                  </div>

                  <div class="toolbar-right">
                    <!-- 清空当前输入 -->
                    <button
                      v-if="inputContent.trim() && !isStreaming"
                      type="button"
                      class="composer-btn--clear"
                      title="清空当前输入"
                      @click="inputContent = ''"
                    >
                      <el-icon><CircleClose /></el-icon>
                    </button>

                    <!-- 正在生成时的【停止生成】红色光晕脉冲按钮 -->
                    <button
                      v-if="isStreaming"
                      type="button"
                      class="action-btn action-btn--stop"
                      title="停止当前生成"
                      @click="stopStreaming"
                    >
                      <el-icon class="stop-icon"><VideoPause /></el-icon>
                      <span>停止生成</span>
                    </button>

                    <!-- 发送按钮 -->
                    <button
                      v-else
                      type="button"
                      class="action-btn action-btn--send"
                      :class="{ 'is-send-pill': isMultiline }"
                      :disabled="!inputContent.trim()"
                      :title="inputContent.trim() ? '发送提问 (Enter)' : '请输入问题后发送'"
                      @click="handleSubmit"
                    >
                      <el-icon class="send-icon"><Promotion /></el-icon>
                      <span v-if="isMultiline" class="send-text">发送</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 底部研读免责声明 -->
              <p class="copilot-disclaimer">
                内容由智教云知识图谱与教学大模型实时检索生成 · 教学结论仅供备课与研读参考
              </p>
            </footer>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue';
import {
  Plus,
  Close,
  Delete,
  Reading,
  CopyDocument,
  RefreshRight,
  VideoPause,
  Promotion,
  CircleClose,
  FullScreen,
  Aim,
  Clock,
  Switch
} from '@element-plus/icons-vue';
import logoImg from '@/assets/images/logo.png';
import GlobalAssistantTrigger from '@/components/ai/GlobalAssistantTrigger.vue';
import AIThinking from '@/components/ai/AIChat/AIThinking.vue';
import { useGlobalAssistant } from '@/composables/ai/useGlobalAssistant';
import { renderChatMarkdown } from '@/utils/ai/chat-markdown';
import { useAuthStore } from '@/stores/auth/auth';
import { DEFAULT_AVATAR } from '@/constants/auth';
import { normalizeAvatarUrl } from '@/utils/format/file';

const authStore = useAuthStore();
const userAvatarBroken = ref(false);
const userAvatarSrc = computed(() => {
  const avatar = authStore.currentUser?.avatar;
  return normalizeAvatarUrl(avatar) || DEFAULT_AVATAR;
});
const userAvatarFallback = computed(() => {
  const name = authStore.currentUser?.realName || authStore.currentUser?.username || '我';
  return name.trim().slice(0, 1) || '我';
});

watch(
  () => authStore.currentUser?.avatar,
  () => {
    userAvatarBroken.value = false;
  }
);

const isWideMode = ref(false);

const {
  drawerVisible,
  inputContent,
  isStreaming,
  messages,
  conversationId,
  messagesScrollRef,
  streamAnchorRef,
  showScrollToBottom,
  activeCourseLabel,
  manualGlobalScope,
  toggleScopeMode,
  presetChips,
  followUpPrompts,
  // 历史会话管理
  isHistoryPanelOpen,
  isSessionLoading,
  sessions,
  toggleHistoryPanel,
  selectSession,
  confirmDeleteSession,
  confirmClearAllSessions,
  formatSessionTime,
  // 流式状态
  streamingIntent,
  streamingRenderedHtml,
  streamingAnswerBody,
  streamingThinkingDisplay,
  isReasoningFolded,
  isReasoningActive,
  streamPhaseMessage,
  // 方法
  toggleDrawer,
  clearMessages,
  startNewSession,
  handleSubmit,
  handleSendPrompt,
  stopStreaming,
  handleRegenerate,
  confirmDeleteMessage,
  copyMessage,
  jumpToCitation,
  handleNavigate,
  scrollToBottomSmooth,
  scrollToBottomInstant,
  scheduleFollowStreamOutput,
  handleViewportScroll,
  getIntentTagType,
  formatMatchScore
} = useGlobalAssistant();

const textareaRef = ref<HTMLTextAreaElement | null>(null);


watch(drawerVisible, (visible) => {
  if (visible) {
    scrollToBottomInstant();
    nextTick(() => {
      textareaRef.value?.focus();
    });
  }
});

const isMultiline = computed(() => {
  const text = inputContent.value || '';
  return text.includes('\n') || text.length > 36;
});

function handleInput() {
  nextTick(() => {
    if (!textareaRef.value) return;
    textareaRef.value.style.height = 'auto';
    const scrollH = textareaRef.value.scrollHeight;
    textareaRef.value.style.height = `${Math.min(Math.max(scrollH, 24), 120)}px`;
  });
}

function handleTextareaKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    if (e.shiftKey) {
      return;
    }
    e.preventDefault();
    handleSubmit();
  }
}
</script>

<style scoped lang="scss">
/* 遮罩层 */
.copilot-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  justify-content: flex-end;
  overscroll-behavior: contain;
}

/* 主面板 */
.copilot-drawer-panel {
  position: relative;
  width: min(640px, 100vw);
  max-width: 100vw;
  height: 100vh;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border-left: 1px solid rgba(22, 119, 255, 0.25);
  box-shadow: -12px 0 40px rgba(15, 23, 42, 0.12), -2px 0 16px rgba(22, 119, 255, 0.1);
  display: flex;
  flex-direction: column;
  color: #0f172a;
  overflow: hidden;
  overscroll-behavior: contain;
  transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &.is-wide-panel {
    width: min(920px, 96vw);
  }
}

/* 左侧流光发光边框 */
.panel-glow-border {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, transparent, rgba(22, 119, 255, 0.7), transparent);
  pointer-events: none;
}

/* 1. Header */
.copilot-header {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.92);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.copilot-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(22, 119, 255, 0.06);
  padding: 3px;
  border: 1px solid rgba(22, 119, 255, 0.2);

  .copilot-header-logo {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.copilot-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.01em;
}

.badge-rag {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.08);
  border: 1px solid rgba(22, 119, 255, 0.25);
  color: #1677ff;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.copilot-subtitle {
  margin: 2px 0 0;
  font-size: 11px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.header-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px 5px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.header-toolbar-sep {
  width: 1px;
  height: 14px;
  margin: 0 2px;
  background: rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.header-btn {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: transparent;
  border: none;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.06);
    color: #1677ff;
  }

  &.is-active {
    background: rgba(22, 119, 255, 0.12);
    color: #1677ff;
  }

  &--close:hover {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }
}

/* 研读上下文横条 */
.context-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 18px;
  background: rgba(22, 119, 255, 0.06);
  border-bottom: 1px solid rgba(22, 119, 255, 0.15);
  font-size: 11.5px;
}

.context-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;

  .context-icon {
    color: #1677ff;
    font-size: 13px;
    flex-shrink: 0;
  }

  .context-label {
    color: #64748b;
    flex-shrink: 0;
  }

  .context-title {
    color: #1677ff;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.context-switch-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.08);
  border: 1px solid rgba(22, 119, 255, 0.2);
  color: #1677ff;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
  margin-left: 8px;

  &:hover {
    background: rgba(22, 119, 255, 0.16);
    border-color: #1677ff;
  }
}

/* 历史会话滑动层 (对标 Code Compass 面板设计) */
.history-panel {
  background: #ffffff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.12);
  z-index: 20;
  display: flex;
  flex-direction: column;
  max-height: 420px;
}

.history-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  background: #f8fafc;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .history-title {
    font-size: 14px;
    font-weight: 600;
    color: #1e293b;
  }

  .history-header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .history-clear-btn {
    font-size: 12px;
    padding: 4px 10px;
    border-radius: 999px;
    background: rgba(239, 68, 68, 0.08);
    border: 1px solid rgba(239, 68, 68, 0.25);
    color: #ef4444;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background: rgba(239, 68, 68, 0.14);
    }
  }

  .history-new-btn {
    font-size: 12px;
    padding: 4px 10px;
    border-radius: 999px;
    background: rgba(22, 119, 255, 0.1);
    border: 1px solid rgba(22, 119, 255, 0.25);
    color: #1677ff;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background: rgba(22, 119, 255, 0.16);
    }
  }
}

.history-list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px;
  max-height: 350px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  scrollbar-width: thin;
}

.history-empty {
  text-align: center;
  padding: 32px 16px;
  color: #94a3b8;
  font-size: 13px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    background: #eff6ff;
    border-color: rgba(22, 119, 255, 0.2);

    .history-delete-btn {
      opacity: 1;
    }
  }

  &.is-current {
    background: rgba(22, 119, 255, 0.08);
    border-color: rgba(22, 119, 255, 0.35);

    .history-item-title {
      color: #1677ff;
      font-weight: 600;
    }
  }

  .history-item-content {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .history-item-title {
    font-size: 13px;
    font-weight: 500;
    color: #334155;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .history-item-time {
    font-size: 11px;
    color: #94a3b8;
  }

  .history-delete-btn {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: none;
    background: transparent;
    color: #94a3b8;
    cursor: pointer;
    opacity: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s ease;

    &:hover {
      background: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }
}

.session-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 16px;
  color: #64748b;
  font-size: 13px;

  .thinking-spinner {
    width: 22px;
    height: 22px;
    border: 2px solid rgba(22, 119, 255, 0.15);
    border-top-color: #1677ff;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 历史面板滑动动画 */
.slide-history-enter-active,
.slide-history-leave-active {
  transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: top center;
}

.slide-history-enter-from,
.slide-history-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-8px);
}


/* 2. 聊天视口 */
.chat-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scrollbar-width: thin;
  scrollbar-color: rgba(22, 119, 255, 0.25) transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(22, 119, 255, 0.2);
    border-radius: 3px;
    &:hover { background: rgba(22, 119, 255, 0.4); }
  }
}

/* 空状态 */
.empty-state {
  margin: auto 0;
  text-align: center;
  padding: 24px 10px;

  .empty-illustration {
    position: relative;
    width: 68px;
    height: 68px;
    margin: 0 auto 16px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .empty-orbit {
    position: absolute;
    border-radius: 50%;
    border: 1px dashed rgba(22, 119, 255, 0.35);

    &--outer {
      inset: 0;
      animation: dialSlowSpin 24s linear infinite;
    }
    &--inner {
      inset: 10px;
      border-color: rgba(114, 46, 209, 0.35);
      animation: dialSlowSpin 16s linear infinite reverse;
    }
  }

  .empty-core {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    background: #ffffff;
    border: 1.5px solid rgba(22, 119, 255, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);
    overflow: hidden;
    padding: 3px;

    .empty-core-logo {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  .empty-title {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 8px;
  }

  .empty-subtitle {
    font-size: 12.5px;
    color: #64748b;
    line-height: 1.6;
    max-width: 380px;
    margin: 0 auto 24px;
  }
}

/* 快捷推荐长条胶囊 */
.empty-prompts-wrap {
  text-align: left;
  background: rgba(248, 250, 252, 0.85);
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px 14px;

  .empty-prompts-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .prompts-tag {
      font-size: 12px;
      font-weight: 600;
      color: #1677ff;
    }

    .prompts-hint {
      font-size: 11px;
      color: #94a3b8;
    }
  }

  .empty-prompts-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .empty-prompt-pill {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 10px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    color: #334155;
    font-size: 12.5px;
    cursor: pointer;
    text-align: left;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    .pill-sparkle {
      color: #1677ff;
      font-size: 12px;
    }

    .pill-text {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .pill-arrow {
      color: #94a3b8;
      font-size: 13px;
      transition: transform 0.2s ease;
    }

    &:hover {
      background: #f0f7ff;
      border-color: #91caff;
      color: #1677ff;
      transform: translateX(3px);

      .pill-arrow {
        transform: translate(2px, -2px);
        color: #1677ff;
      }
    }
  }
}

/* 消息流布局 */
.message-stream {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  width: 100%;

  &-user {
    justify-content: flex-end; /* 标准靠右对齐：气泡在左，头像在最右侧 */

    .msg-avatar {
      background: #1677ff;
      color: #ffffff;
      font-size: 12px;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
      flex-shrink: 0;
      overflow: hidden;
      padding: 0;

      .avatar-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
        display: block;
      }

      .avatar-fallback {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
      }
    }

    .message-content-col {
      align-items: flex-end;
      max-width: 86%;
    }

    .message-bubble {
      background: #1677ff;
      color: #ffffff;
      border-radius: 14px 2px 14px 14px;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.2);
    }
  }

  &-assistant {
    justify-content: flex-start;

    .msg-avatar {
      background: #ffffff;
      border: 1px solid rgba(22, 119, 255, 0.2);
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.1);
      overflow: hidden;
      padding: 2px;
      flex-shrink: 0;
    }

    .message-content-col {
      align-items: flex-start;
      flex: 1;
      max-width: calc(100% - 42px);
      min-width: 0;
    }

    .message-bubble {
      background: #ffffff;
      color: #0f172a;
      border: 1px solid #e2e8f0;
      border-radius: 2px 14px 14px 14px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
      width: 100%;
      box-sizing: border-box;
    }
  }
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px; /* 与气泡顶部完美对齐 */

  .assistant-avatar-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    border-radius: 50%;
  }
}

.message-content-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.message-bubble {
  padding: 12px 16px;
  font-size: 13.5px;
  line-height: 1.7;
  word-break: break-word;
  overflow: hidden;

  &.is-streaming-bubble {
    border-color: rgba(22, 119, 255, 0.4);
    box-shadow: 0 0 14px rgba(22, 119, 255, 0.08);
  }

  .bubble-intent-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    padding-bottom: 8px;
    border-bottom: 1px dashed rgba(22, 119, 255, 0.15);
  }
}

/* 深度 Markdown 排版 */
.chat-md-content {
  :deep(p) {
    margin: 8px 0;
    &:first-child { margin-top: 0; }
    &:last-child { margin-bottom: 0; }
  }

  :deep(strong) {
    font-weight: 700;
    color: #0958d9;
  }

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 14px 0 8px;
    color: #0f172a;
    font-weight: 700;
  }

  :deep(h1) { font-size: 1.25em; }
  :deep(h2) { font-size: 1.15em; border-bottom: 1px solid #f1f5f9; padding-bottom: 4px; }
  :deep(h3) { font-size: 1.05em; }

  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 1.5em;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(blockquote) {
    margin: 10px 0;
    padding: 8px 12px;
    background: #f8fafc;
    border-left: 3px solid #1677ff;
    color: #64748b;
    border-radius: 4px;
    font-size: 12.5px;
  }

  :deep(hr) {
    margin: 14px 0;
    border: none;
    border-top: 1px solid #e2e8f0;
  }

  :deep(.katex) {
    word-break: normal;
    overflow-wrap: normal;
  }

  :deep(.table-wrap) {
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin: 10px 0 12px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
    background: #fafbfc;
    box-sizing: border-box;
  }

  :deep(.table-wrap::-webkit-scrollbar) {
    height: 4px;
  }

  :deep(.table-wrap::-webkit-scrollbar-button) {
    display: none;
    width: 0;
    height: 0;
  }

  :deep(table) {
    width: 100%;
    min-width: 100%;
    border-collapse: collapse;
    font-size: 12px;
    line-height: 1.55;
    table-layout: auto;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #e2e8f0;
    padding: 6px 10px;
    text-align: left;
    vertical-align: top;
    word-break: break-word;
    min-width: 72px;
  }

  :deep(th) {
    background: #f8fafc;
    font-weight: 600;
    color: #1677ff;
    white-space: nowrap;
  }

  :deep(.code-block-wrapper) {
    margin: 12px 0;
    border-radius: 8px;
    background: #1e1e1e;
    overflow: hidden;
    border: 1px solid #333333;

    .code-header {
      background: #252526;
      padding: 6px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .code-lang {
        font-size: 11px;
        color: #9cdcfe;
        font-weight: 700;
        text-transform: uppercase;
      }

      .code-copy-btn {
        background: #3c3c3c;
        color: #cccccc;
        border: none;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 10.5px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #505050;
          color: #ffffff;
        }

        &.is-copied {
          background: #52c41a;
          color: #ffffff;
        }
      }
    }

    pre {
      margin: 0;
      padding: 10px 12px;
      overflow-x: auto;
      font-size: 12px;
      line-height: 1.55;
    }
  }

  /* Mermaid 图谱容器排版 */
  :deep(.mermaid-diagram-wrapper) {
    margin: 14px 0;
    border-radius: 10px;
    border: 1px solid rgba(22, 119, 255, 0.22);
    background: #fdfdfd;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

    .mermaid-header {
      background: #f0f7ff;
      padding: 6px 12px;
      font-size: 11px;
      font-weight: 600;
      color: #1677ff;
      border-bottom: 1px solid rgba(22, 119, 255, 0.15);
    }

    .mermaid-diagram {
      padding: 14px 10px;
      display: flex;
      justify-content: center;
      overflow-x: auto;

      svg {
        max-width: 100%;
        height: auto;
      }

      .mermaid-loading {
        color: #8c8c8c;
        font-size: 12px;
      }
    }
  }

  :deep(.copilot-citation-sup) {
    display: inline-block;
    color: #1677ff;
    background: rgba(22, 119, 255, 0.1);
    border: 1px solid rgba(22, 119, 255, 0.25);
    padding: 0 4px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 600;
    cursor: pointer;
    vertical-align: super;
    margin: 0 2px;
  }
}

/* 打字机闪烁光标 */
.streaming-cursor {
  display: inline-block;
  width: 6px;
  height: 14px;
  background: #1677ff;
  margin-left: 3px;
  vertical-align: middle;
  animation: blink 1s infinite;
}

/* 引用切片托盘 */
.citations-tray {
  margin-top: 10px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;

  .citations-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;

    .citations-badge {
      font-size: 11.5px;
      color: #1677ff;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .citations-sub {
      font-size: 10.5px;
      color: #94a3b8;
    }
  }

  .citations-cards {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .citation-card {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 8px 10px;
    font-size: 11.5px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #f0f7ff;
      border-color: #91caff;
    }

    .citation-top {
      display: flex;
      align-items: center;
      gap: 6px;

      .citation-idx {
        color: #1677ff;
        font-weight: 700;
      }

      .citation-doc {
        color: #334155;
        font-weight: 600;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .citation-match {
        color: #52c41a;
        font-size: 10.5px;
      }
    }

    .citation-excerpt {
      margin-top: 4px;
      color: #64748b;
      font-size: 11px;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }
}

/* 消息操作栏 */
.message-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.85;

  &.is-user-actions {
    justify-content: flex-end;
  }

  .action-chip {
    background: transparent;
    border: 1px solid transparent;
    color: #94a3b8;
    border-radius: 6px;
    padding: 3px 6px;
    font-size: 11px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    transition: all 0.18s;

    &:hover {
      background: rgba(0, 0, 0, 0.05);
      color: #1677ff;
    }

    &--danger:hover {
      background: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }
}

/* 推荐追问 */
.message-followup-tray {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .followup-tray-title {
    font-size: 11.5px;
    color: #1677ff;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .followup-pills-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .followup-pill-btn {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 6px 12px;
    border-radius: 8px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    color: #334155;
    font-size: 12px;
    cursor: pointer;
    text-align: left;
    transition: all 0.2s;

    &:hover {
      background: #f0f7ff;
      border-color: #91caff;
      color: #1677ff;
      transform: translateX(2px);
    }
  }
}

/* 视口底部呼吸间距 */
.chat-bottom-spacer {
  height: 52px;
  flex-shrink: 0;
}

/* 悬浮微圆钮触底按钮 (靠右下定位，绝不遮挡正文) */
.scroll-bottom-btn {
  position: absolute;
  bottom: 96px;
  right: 24px;
  z-index: 20;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(22, 119, 255, 0.35);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-2px);
    background: #1677ff;
    box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);

    .scroll-bottom-svg__bg { fill: #1677ff; }
    .scroll-bottom-svg__shaft, .scroll-bottom-svg__head { stroke: #ffffff; }
  }

  .scroll-bottom-svg {
    width: 20px;
    height: 20px;
    &__bg { fill: #ffffff; }
    &__shaft, &__head { stroke: #1677ff; stroke-width: 2.2; fill: none; }
  }
}

/* 4. 底部输入框与工具栏 */
.copilot-footer {
  background: #ffffff;
  padding: 10px 16px 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.composer {
  border-radius: 12px;
  border: 1.5px solid #d9d9d9;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  padding: 6px 10px;
  gap: 8px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:focus-within {
    border-color: #1677ff;
    box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15);
  }

  &.is-expanded {
    flex-direction: column;
    align-items: stretch;
    padding: 10px 12px;
  }

  .composer-gem-indicator {
    color: #1677ff;
    font-size: 14px;
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  .composer-body {
    flex: 1;
    display: flex;
  }

  .chat-textarea {
    width: 100%;
    border: none;
    outline: none;
    resize: none;
    font-size: 13.5px;
    line-height: 1.5;
    color: #0f172a;
    background: transparent;
    font-family: inherit;

    &::placeholder {
      color: #94a3b8;
    }
  }

  .composer-toolbar {
    display: flex;
    align-items: center;

    &.is-inline {
      gap: 6px;
    }

    &.is-bottom {
      justify-content: space-between;
      margin-top: 8px;
      padding-top: 6px;
      border-top: 1px solid #f1f5f9;
    }

    .input-shortcut-hint {
      font-size: 11px;
      color: #94a3b8;
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }

  .composer-btn--clear {
    background: transparent;
    border: none;
    color: #bfbfbf;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    padding: 2px;
    transition: color 0.18s;

    &:hover { color: #8c8c8c; }
  }

  .action-btn {
    border: none;
    border-radius: 999px;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12.5px;
    font-weight: 600;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &--send {
      width: 32px;
      height: 32px;
      justify-content: center;
      background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);

      &.is-send-pill {
        width: auto;
        padding: 5px 14px;
      }

      &:hover:not(:disabled) {
        transform: scale(1.05);
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.45);
      }

      &:disabled {
        background: #d9d9d9;
        box-shadow: none;
        cursor: not-allowed;
      }
    }

    &--stop {
      padding: 5px 12px;
      background: #ff4d4f;
      color: #ffffff;
      box-shadow: 0 0 10px rgba(255, 77, 79, 0.45);
      animation: stopPulse 2s infinite;

      &:hover {
        background: #cf1322;
      }
    }
  }
}

.copilot-disclaimer {
  margin: 8px 0 0;
  text-align: center;
  font-size: 10.5px;
  color: #94a3b8;
}

/* 动效 */
.copilot-drawer-enter-active,
.copilot-drawer-leave-active {
  transition: all 0.32s cubic-bezier(0.16, 1, 0.3, 1);

  .copilot-drawer-panel {
    transition: transform 0.32s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

.copilot-drawer-enter-from,
.copilot-drawer-leave-to {
  opacity: 0;

  .copilot-drawer-panel {
    transform: translateX(100%);
  }
}

@keyframes dialSlowSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes stopPulse {
  0% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.6); }
  70% { box-shadow: 0 0 0 8px rgba(255, 77, 79, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0); }
}
</style>

<template>
<!-- 2. 消息流式主列表区域 -->
            <div
              ref="messagesScrollRef"
              class="chat-viewport ga-messages-scroll"
              data-chat-scroll="true"
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
                    <span class="prompts-tag"><el-icon class="prompts-tag-icon"><StarFilled /></el-icon> 快捷探索推荐</span>
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
                      <el-icon class="pill-sparkle"><StarFilled /></el-icon>
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

                      <!-- 深度思考卡片（遵循个人偏好 → 默认呈现策略） -->
                      <AIThinking
                        v-if="msg.role === 'assistant' && showThinkingPanel && msg.reasoningContent"
                        :content="msg.reasoningContent"
                        :folded="resolveReasoningFolded(msg.reasoningFolded)"
                        :active="false"
                        :has-answer-body="true"
                        @update:folded="msg.reasoningFolded = $event"
                        @user-collapse="pauseAutoScrollFollow"
                      />

                      <!-- Markdown 渲染正文 -->
                      <div
                        v-if="msg.content?.trim()"
                        class="markdown-body chat-md-content"
                        v-html="renderChatMarkdown(msg.content)"
                      />

                      <!-- 引用切片溯源（默认折叠，需时展开） -->
                      <div
                        v-if="msg.citations && msg.citations.length > 0"
                        class="citations-tray"
                      >
                        <button
                          type="button"
                          class="citations-header citations-header--toggle"
                          @click="toggleCitationTray(citationTrayKey(msg, idx))"
                        >
                          <span class="citations-badge">
                            <el-icon><Reading /></el-icon>
                            参考课程知识库切片 ({{ msg.citations.length }})
                          </span>
                          <span class="citations-header-right">
                            <span class="citations-sub">
                              {{ isCitationTrayExpanded(citationTrayKey(msg, idx)) ? '收起' : '展开查看' }}
                            </span>
                            <el-icon
                              class="citations-chevron"
                              :class="{ 'is-expanded': isCitationTrayExpanded(citationTrayKey(msg, idx)) }"
                            >
                              <ArrowDown />
                            </el-icon>
                          </span>
                        </button>
                        <div
                          v-show="isCitationTrayExpanded(citationTrayKey(msg, idx))"
                          class="citations-cards"
                        >
                          <div
                            v-for="(item, cIdx) in msg.citations"
                            :key="cIdx"
                            class="citation-card"
                            @click="jumpToCitation(item)"
                          >
                            <div class="citation-top">
                              <span class="citation-idx">[{{ cIdx + 1 }}]</span>
                              <span class="citation-doc">《{{ item.documentName || '课程核心资料' }}》</span>
                              <span class="citation-match">
                                匹配度 {{ formatMatchScore(item.score, citationScores(msg.citations)) }}
                              </span>
                            </div>
                            <div v-if="citationExcerptSource(item)" class="citation-excerpt">
                              {{ formatCitationCardPreview(citationExcerptSource(item)) }}
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

                      <LessonStudioInsertActions
                        v-if="msg.role === 'assistant' && isLessonStudioContext && msg.content?.trim()"
                        :msg="msg"
                        :msg-index="idx"
                        :messages="messages"
                      />
                    </div>

                    <!-- 推荐追问胶囊 (仅在最后一条助手回答完毕后展示) -->
                    <div
                      v-if="!isStreaming && msg.role === 'assistant' && idx === messages.length - 1 && followUpPrompts.length > 0"
                      class="message-followup-tray"
                    >
                      <div class="followup-tray-title">
                        <el-icon class="followup-sparkle"><StarFilled /></el-icon>
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
                      <!-- 正在生成状态头 -->
                      <div class="msg-meta-header">
                        <span class="sender-name">EduMind 智能教学助教</span>
                        <span class="streaming-badge-tag">正在深度研读与生成解答...</span>
                      </div>

                      <!-- 意图识别胶囊 (常规问答不展示，非chat特殊功能才展示) -->
                      <div
                        v-if="streamingIntent.intent && streamingIntent.intent !== 'chat' && streamingIntent.intent !== 'CHAT'"
                        class="bubble-intent-row"
                      >
                        <el-tag size="small" :type="getIntentTagType(streamingIntent.intent)" effect="dark" round>
                          {{ streamingIntent.intentDesc || streamingIntent.intent }}
                        </el-tag>
                      </div>

                      <!-- 思考卡片：流式生成中遵循个人偏好默认呈现策略，侧边栏保持极简不展示多余控制 -->
                      <AIThinking
                        v-if="showThinkingPanel && (!streamingAnswerBody || streamingThinkingDisplay)"
                        :content="streamingThinkingDisplay"
                        :folded="isReasoningFolded"
                        :active="!streamingAnswerBody && (isReasoningActive || !streamingThinkingDisplay)"
                        :has-answer-body="!!streamingAnswerBody"
                        :phase-message="streamPhaseMessage"
                        :show-timer="false"
                        @update:folded="isReasoningFolded = $event"
                        @user-collapse="pauseAutoScrollFollow"
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
</template>

<script setup lang="ts">
import { inject, ref } from 'vue';
import { Reading, CopyDocument, RefreshRight, Delete, StarFilled, ArrowDown } from '@element-plus/icons-vue';
import type { CitationItem, GlobalAssistantMessage } from '@/types/ai/assistant';
import { collectCitationScores } from '@/utils/ai/citation-score';
import {
  citationExcerptSource,
  formatCitationCardPreview
} from '@/utils/ai/citation-excerpt';
import AIThinking from '@/components/ai/AIChat/AIThinking.vue';
import LessonStudioInsertActions from '@/components/ai/global-assistant/LessonStudioInsertActions.vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const {
  messagesScrollRef,
  streamAnchorRef,
  handleViewportScroll,
  isSessionLoading,
  messages,
  isStreaming,
  logoImg,
  presetChips,
  handleSendPrompt,
  showThinkingPanel,
  renderChatMarkdown,
  resolveReasoningFolded,
  getIntentTagType,
  handleNavigate,
  formatMatchScore,
  jumpToCitation,
  copyMessage,
  handleRegenerate,
  confirmDeleteMessage,
  followUpPrompts,
  userAvatarSrc,
  userAvatarBroken,
  userAvatarFallback,
  streamingIntent,
  streamingRenderedHtml,
  streamingAnswerBody,
  streamingThinkingDisplay,
  isReasoningFolded,
  isReasoningActive,
  streamPhaseMessage,
  pauseAutoScrollFollow,
  isLessonStudioContext,
  stopStreaming
} = inject(globalAssistantUiKey)!;

/** 参考切片托盘默认折叠 */
const expandedCitationTrays = ref<Set<string>>(new Set());

function citationTrayKey(msg: GlobalAssistantMessage, idx: number): string {
  return String(msg.id ?? `idx-${idx}`);
}

function isCitationTrayExpanded(key: string): boolean {
  return expandedCitationTrays.value.has(key);
}

function citationScores(citations?: CitationItem[]) {
  return collectCitationScores(citations);
}

function toggleCitationTray(key: string) {
  const next = new Set(expandedCitationTrays.value);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  expandedCitationTrays.value = next;
}
</script>

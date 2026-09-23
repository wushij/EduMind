<template>
  <div class="chat-message-row" :class="`chat-message-row--${message.role}`">
    <!-- 头像 -->
    <div class="msg-avatar-box">
      <div v-if="message.role === 'assistant'" class="assistant-avatar-circle">
        <img class="assistant-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
      </div>
      <div v-else class="user-avatar-circle">
        <img
          v-if="userAvatarSrc && !userAvatarBroken"
          :src="userAvatarSrc"
          class="user-avatar-img"
          alt="avatar"
          @error="userAvatarBroken = true"
        />
        <svg v-else viewBox="0 0 24 24" class="user-svg" fill="currentColor">
          <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
        </svg>
      </div>
    </div>

    <!-- 消息主体 -->
    <div class="msg-content-wrapper">
      <div class="msg-meta-header">
        <span class="sender-name">{{ message.role === 'assistant' ? 'EduMind 课程 AI 助教' : '我' }}</span>
        <span class="msg-time">{{ message.createdAt }}</span>
      </div>

      <!-- 长期记忆激活胶囊指示条 -->
      <div
        v-if="message.role === 'assistant' && message.recalledMemories && message.recalledMemories.length > 0"
        class="recalled-memory-chip-row"
      >
        <div class="mem-pill-badge">
          <el-icon class="mem-icon"><Cpu /></el-icon>
          <span class="mem-text">已唤醒 {{ message.recalledMemories.length }} 条长效记忆：</span>
          <span
            v-for="mem in message.recalledMemories"
            :key="mem.id"
            class="mem-tag"
            :title="mem.summary"
          >
            {{ mem.summary.length > 22 ? mem.summary.substring(0, 20) + '...' : mem.summary }}
          </span>
        </div>
      </div>

      <AIThinking
        v-if="message.role === 'assistant' && showThinkingPanel && (thinkingDisplay || (message.isStreaming && !answerContent))"
        :content="thinkingDisplay"
        :folded="resolvedReasoningFolded"
        :active="!!message.isStreaming && !!message.isReasoningActive"
        :has-answer-body="!!answerContent"
        :phase-message="message.streamPhaseMessage"
        @update:folded="message.reasoningFolded = $event"
        @user-collapse="$emit('reasoning-collapse')"
      />

      <div ref="bubbleRef" class="msg-bubble" :class="{ 'is-streaming': message.isStreaming }">
        <!-- 统一使用标准高保真 Markdown / KaTeX / Mermaid / 代码高亮解析 -->
        <div class="markdown-body chat-md-content" v-html="renderedHtml" />

        <!-- 打字机闪烁光标 -->
        <span v-if="message.isStreaming && answerContent" class="stream-cursor">▋</span>

        <!-- 气泡底端内联时间 (对齐原型设计) -->
        <div class="bubble-timestamp-corner">
          <span>{{ message.createdAt }}</span>
        </div>
      </div>

      <!-- 知识库精准出处溯源列表 -->
      <CitationList
        v-if="message.role === 'assistant' && message.citations && message.citations.length > 0"
        :citations="message.citations"
      />

      <!-- 底部辅助长圆小工具条 (用户与AI均支持复制与删除，AI额外支持重新生成) -->
      <div v-if="!message.isStreaming" class="msg-actions-bar" :class="{ 'is-user-actions': message.role === 'user' }">
        <button
          type="button"
          class="pill-action-btn"
          :title="message.role === 'user' ? '复制提问内容' : '复制回答全文'"
          @click="handleCopy"
        >
          <el-icon><DocumentCopy /></el-icon>
          <span>{{ isCopied ? '已复制！' : (message.role === 'user' ? '复制' : '复制全文') }}</span>
        </button>
        <button
          v-if="message.role === 'assistant'"
          type="button"
          class="pill-action-btn"
          title="重新生成此回答"
          @click="$emit('regenerate')"
        >
          <el-icon><RefreshRight /></el-icon>
          <span>重新生成</span>
        </button>
        <button
          type="button"
          class="pill-action-btn pill-action-btn--danger"
          :title="message.role === 'user' ? '删除本条提问及对应回答' : '删除本轮问答'"
          @click="$emit('delete')"
        >
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </button>
      </div>

      <!-- 推荐探索长条胶囊 (对标侧边栏快捷追问) -->
      <div
        v-if="message.role === 'assistant' && !message.isStreaming && isLast && activeFollowUps.length > 0"
        class="message-followup-tray"
      >
        <div class="followup-tray-title">
          <span class="followup-sparkle">✦</span>
          <span>推荐继续探索：</span>
        </div>
        <div class="followup-pills-list">
          <button
            v-for="(fp, fIdx) in activeFollowUps"
            :key="fIdx"
            type="button"
            class="followup-pill-btn"
            :title="fp"
            @click="$emit('send-prompt', fp)"
          >
            <span class="followup-pill-text">{{ fp }}</span>
            <span class="followup-pill-arrow">↗</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { DocumentCopy, RefreshRight, Delete, Cpu } from '@element-plus/icons-vue';
import type { ChatMessage } from '@/composables/ai/useAIStream';
import CitationList from '@/components/knowledge/CitationList.vue';
import AIThinking from '@/components/ai/AIChat/AIThinking.vue';
import { splitCopilotStream } from '@/utils/ai/copilot-stream-split';
import {
  isThinkingPanelHidden,
  resolveReasoningFolded
} from '@/utils/ai/thinking-display';
import { usePreferenceStore } from '@/stores/user/preference';
import { renderChatMarkdown, bindMarkdownCodeCopy, renderMermaidInElement } from '@/utils/ai/chat-markdown';
import { useAuthStore } from '@/stores/auth/auth';
import { DEFAULT_AVATAR } from '@/constants/auth';
import { normalizeAvatarUrl } from '@/utils/format/file';

const props = withDefaults(
  defineProps<{
    message: ChatMessage;
    isLast?: boolean;
    followUpPrompts?: string[];
  }>(),
  {
    isLast: false,
    followUpPrompts: () => []
  }
);

defineEmits<{
  (e: 'regenerate'): void;
  (e: 'delete'): void;
  (e: 'send-prompt', prompt: string): void;
  (e: 'reasoning-collapse'): void;
}>();

const preferenceStore = usePreferenceStore();
const authStore = useAuthStore();

const showThinkingPanel = computed(
  () => !isThinkingPanelHidden(preferenceStore.preferences.thinkingDisplayMode)
);

const resolvedReasoningFolded = computed(() =>
  resolveReasoningFolded(
    props.message.reasoningFolded,
    preferenceStore.preferences.thinkingDisplayMode
  )
);
const userAvatarBroken = ref(false);
const userAvatarSrc = computed(() => {
  const avatar = authStore.currentUser?.avatar;
  return normalizeAvatarUrl(avatar) || DEFAULT_AVATAR;
});

const bubbleRef = ref<HTMLDivElement | null>(null);

const answerContent = computed(() => splitCopilotStream(props.message.content || '').answer || props.message.content || '');

const thinkingDisplay = computed(() => {
  const native = props.message.reasoningContent?.trim();
  if (native) return native;
  return splitCopilotStream(props.message.content).thinking;
});

const renderedHtml = computed(() => renderChatMarkdown(answerContent.value));

const activeFollowUps = computed(() => {
  if (props.message.followUpPrompts && props.message.followUpPrompts.length > 0) {
    return props.message.followUpPrompts;
  }
  return props.followUpPrompts || [];
});

function refreshMarkdownUi() {
  nextTick(() => {
    nextTick(() => {
      if (!bubbleRef.value) return;
      bindMarkdownCodeCopy(bubbleRef.value);
      renderMermaidInElement(bubbleRef.value);
    });
  });
}

watch(renderedHtml, () => refreshMarkdownUi());
onMounted(() => refreshMarkdownUi());

const isCopied = ref(false);

function handleCopy() {
  const textToCopy = props.message.role === 'user'
    ? (props.message.content || '')
    : (answerContent.value || props.message.content || '');
  if (navigator.clipboard && textToCopy) {
    navigator.clipboard.writeText(textToCopy);
    isCopied.value = true;
    ElMessage.success(props.message.role === 'user' ? '已复制提问内容' : '已复制对话内容');
    setTimeout(() => {
      isCopied.value = false;
    }, 1500);
  }
}
</script>

<style scoped lang="scss">
.chat-message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;

  .msg-avatar-box {
    flex-shrink: 0;

    .assistant-avatar-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.18);

      .assistant-brand-logo {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }
    }

    .user-avatar-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #1E293B;
      color: #FFFFFF;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
      overflow: hidden;

      .user-avatar-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }

      .user-svg {
        width: 22px;
        height: 22px;
      }
    }
  }

  .msg-content-wrapper {
    max-width: 86%;
    min-width: 0;
    display: flex;
    flex-direction: column;

    .msg-meta-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 5px;

      .sender-name {
        font-size: 12.5px;
        font-weight: 600;
        color: #1E293B;
      }

      .msg-time {
        font-size: 11px;
        color: #94A3B8;
      }
    }

    .msg-bubble {
      position: relative;
      max-width: 100%;
      min-width: 0;
      box-sizing: border-box;
      padding: 16px 20px 24px;
      font-size: 13.5px;
      line-height: 1.7;
      word-break: break-word;

      .bubble-timestamp-corner {
        position: absolute;
        right: 14px;
        bottom: 6px;
        font-size: 11px;
        color: #94A3B8;
        user-select: none;
      }

      .stream-cursor {
        display: inline-block;
        margin-left: 2px;
        color: #1677FF;
        animation: blink 0.9s infinite;
      }
    }

    .msg-actions-bar {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 8px;

      &.is-user-actions {
        justify-content: flex-end;
      }

      .pill-action-btn {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 26px;
        padding: 0 10px;
        border-radius: 9999px;
        border: 1px solid #E2E8F0;
        background: #FFFFFF;
        font-size: 11.5px;
        color: #64748B;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          color: #1677FF;
          border-color: #CBD5E1;
        }

        &.active {
          background: #EFF6FF;
          border-color: #BFDBFE;
          color: #1677FF;
        }

        &--danger:hover {
          color: #EF4444;
          border-color: #FECACA;
          background: #FEF2F2;
        }
      }
    }
  }

  // 助手消息样式 (浅灰底白边，左上圆角)
  &--assistant {
    .msg-bubble {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 4px 18px 18px 18px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
      color: #1E293B;
      overflow: hidden;
    }
  }

  // 用户消息样式 (对齐原型：柔和天蓝背景底，蓝黑文字，右上圆角)
  &--user {
    flex-direction: row-reverse;

    .msg-content-wrapper {
      align-items: flex-end;

      .msg-meta-header {
        flex-direction: row-reverse;
      }

      .msg-bubble {
        background: #DCEBFE;
        border: 1px solid #BFDBFE;
        color: #1E3A8A;
        border-radius: 18px 4px 18px 18px;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);

        .bubble-timestamp-corner {
          color: #3B82F6;
        }
      }
    }
  }
}

// 格式化文本行
.msg-blocks-flow {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .formatted-text-block {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .bullet-line {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      padding-left: 4px;

      .bullet-dot {
        color: #1677FF;
        font-weight: bold;
      }
    }

    .numbered-line {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      margin-top: 2px;

      .number-badge {
        font-weight: 700;
        color: #1E293B;
      }
    }

    .empty-spacer-line {
      height: 6px;
    }

    :deep(strong) {
      color: #0F172A;
      font-weight: 700;
    }

    :deep(.inline-code) {
      background: rgba(15, 23, 42, 0.06);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Fira Code', Consolas, monospace;
      font-size: 12px;
      color: #0284C7;
    }
  }
}

/* 工业级 Markdown 渲染排版 */
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

  /* Code Compass Copilot 同款：表格横向滑动 + 公式不被 word-break 拆碎 */
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

  :deep(.ascii-tree-wrapper) {
    background: #f8fafc;
    border-color: #dbeafe;

    .code-header {
      background: #eff6ff;
    }

    .code-lang {
      color: #1d4ed8;
    }

    .code-copy-btn {
      background: #dbeafe;
      color: #1e40af;

      &:hover {
        background: #bfdbfe;
        color: #1e3a8a;
      }
    }

    .ascii-tree-pre {
      margin: 0;
      padding: 14px 16px;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 13px;
      line-height: 1.55;
      color: #0f172a;
      white-space: pre;
      word-break: normal;
      overflow-wrap: normal;
      overflow-x: auto;
    }
  }

  :deep(.code-block-wrapper:not(.ascii-tree-wrapper)) {
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

    pre.hljs,
    pre.hljs code {
      background: transparent;
      color: #abb2bf;
    }
  }

  /* Mermaid 图谱容器排版 */
  :deep(.mermaid-diagram-wrapper) {
    margin: 14px 0;
    border-radius: 10px;
    border: 1px solid rgba(22, 119, 255, 0.22);
    background: #fdfdfd;
    overflow: visible;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

    .mermaid-header {
      background: #f0f7ff;
      padding: 6px 12px;
      font-size: 11px;
      font-weight: 600;
      color: #1677ff;
      border-bottom: 1px solid rgba(22, 119, 255, 0.15);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }

    .mermaid-diagram {
      padding: 14px 10px;
      display: block;
      overflow-x: auto;
      overflow-y: visible;
      max-width: 100%;

      svg {
        display: block;
        max-width: none;
        width: auto;
        height: auto;
      }

      .mermaid-loading {
        color: #8c8c8c;
        font-size: 12px;
      }
    }
  }

  :deep(.math-fallback) {
    font-family: 'Fira Code', Consolas, monospace;
    font-size: 12px;
    color: #64748b;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 8px;
    margin: 10px 0;
    display: block;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.pill-action-btn--danger:hover {
  background: rgba(239, 68, 68, 0.08) !important;
  color: #ef4444 !important;
  border-color: rgba(239, 68, 68, 0.2) !important;
}

/* 推荐探索长条胶囊 */
.message-followup-tray {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(22, 119, 255, 0.04);
  border: 1px dashed rgba(22, 119, 255, 0.25);

  .followup-tray-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11.5px;
    font-weight: 600;
    color: #1677ff;
    margin-bottom: 8px;

    .followup-sparkle {
      font-size: 13px;
    }
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
    padding: 7px 12px;
    border-radius: 8px;
    background: #ffffff;
    border: 1px solid rgba(22, 119, 255, 0.16);
    color: #334155;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.18s ease;
    text-align: left;

    &:hover {
      background: #eff6ff;
      border-color: #1677ff;
      color: #1677ff;
      transform: translateX(2px);
    }

    .followup-pill-text {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .followup-pill-arrow {
      color: #94a3b8;
      font-size: 12px;
      margin-left: 8px;
      flex-shrink: 0;
    }
  }

  .recalled-memory-chip-row {
    margin-bottom: 8px;

    .mem-pill-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #F0FDF4;
      border: 1px solid #BBF7D0;
      border-radius: 9999px;
      padding: 4px 12px;
      font-size: 11px;
      max-width: 100%;
      flex-wrap: wrap;

      .mem-icon {
        color: #16A34A;
        font-size: 13px;
        flex-shrink: 0;
      }

      .mem-text {
        color: #15803D;
        font-weight: 600;
        white-space: nowrap;
      }

      .mem-tag {
        background: #DCFCE7;
        color: #166534;
        padding: 1px 8px;
        border-radius: 9999px;
        font-weight: 500;
      }
    }
  }
}
</style>

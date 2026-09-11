<template>
  <div class="chat-message-row" :class="`chat-message-row--${message.role}`">
    <!-- 头像 -->
    <div class="msg-avatar-box">
      <div v-if="message.role === 'assistant'" class="assistant-avatar-circle">
        <img class="assistant-brand-logo" src="@/assets/images/logo.png" alt="EduMind" />
      </div>
      <div v-else class="user-avatar-circle">
        <svg viewBox="0 0 24 24" class="user-svg" fill="currentColor">
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

      <div class="msg-bubble" :class="{ 'is-streaming': message.isStreaming }">
        <!-- 结构化分块渲染：文本块与高亮代码块 -->
        <div class="msg-blocks-flow">
          <template v-for="(block, idx) in parsedBlocks" :key="idx">
            <!-- 普通文本块 (支持格式化、加粗、无序列表) -->
            <div v-if="block.type === 'text'" class="formatted-text-block">
              <template v-for="(line, lineIdx) in block.lines" :key="lineIdx">
                <!-- 列表项 -->
                <div v-if="line.startsWith('• ') || line.startsWith('- ')" class="bullet-line">
                  <span class="bullet-dot">•</span>
                  <span class="bullet-content" v-html="formatInlineText(line.replace(/^[•\-]\s*/, ''))"></span>
                </div>
                <!-- 数字序号项 -->
                <div v-else-if="/^\d+\.\s/.test(line)" class="numbered-line">
                  <span class="number-badge">{{ line.match(/^\d+\./)?.[0] }}</span>
                  <span class="numbered-content" v-html="formatInlineText(line.replace(/^\d+\.\s*/, ''))"></span>
                </div>
                <!-- 普通段落行 -->
                <div v-else-if="line.trim()" class="paragraph-line" v-html="formatInlineText(line)"></div>
                <!-- 空行换行 -->
                <div v-else class="empty-spacer-line"></div>
              </template>
            </div>

            <!-- 专业代码块 (带语言标、复制代码、行号) -->
            <div v-else-if="block.type === 'code'" class="code-snippet-box">
              <div class="code-header-bar">
                <div class="code-lang-tag">
                  <span class="lang-pill">{{ block.lang || 'code' }}</span>
                </div>
                <button
                  type="button"
                  class="copy-code-action-btn"
                  @click="copySnippet(block.code)"
                >
                  <el-icon class="copy-icon"><DocumentCopy /></el-icon>
                  <span>{{ copiedSnippet === block.code ? '已复制！' : '复制代码' }}</span>
                </button>
              </div>

              <div class="code-body-container">
                <div class="code-gutter-col">
                  <span v-for="n in (block.codeLines || []).length" :key="n" class="line-num">{{ n }}</span>
                </div>
                <pre class="code-pre-content"><code><div v-for="(codeLine, cIdx) in (block.codeLines || [])" :key="cIdx" class="code-source-line" v-html="highlightSyntax(codeLine, block.lang)"></div></code></pre>
              </div>
            </div>
          </template>
        </div>

        <!-- 打字机闪烁光标 -->
        <span v-if="message.isStreaming" class="stream-cursor">▋</span>

        <!-- 气泡底端内联时间 (对齐原型设计) -->
        <div class="bubble-timestamp-corner">
          <span>{{ message.createdAt }}</span>
        </div>
      </div>

      <!-- 底部辅助长圆小工具条 (AI 回复特有) -->
      <div v-if="message.role === 'assistant' && !message.isStreaming" class="msg-actions-bar">
        <button type="button" class="pill-action-btn" title="复制回答" @click="handleCopy">
          <el-icon><DocumentCopy /></el-icon>
          <span>{{ copyText }}</span>
        </button>
        <button
          type="button"
          class="pill-action-btn"
          :class="{ active: liked }"
          title="回答有用"
          @click="liked = !liked"
        >
          <el-icon><Star /></el-icon>
          <span>{{ liked ? '已标记有启发' : '有启发' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { DocumentCopy, Star } from '@element-plus/icons-vue';
import type { ChatMessage } from '@/composables/ai/useAIStream';

const props = defineProps<{
  message: ChatMessage;
}>();

const liked = ref(false);
const copyText = ref('复制全文');
const copiedSnippet = ref('');

interface Block {
  type: 'text' | 'code';
  lines?: string[];
  lang?: string;
  code?: string;
  codeLines?: string[];
}

// 解析消息内容中的代码块和文字块
const parsedBlocks = computed<Block[]>(() => {
  const content = props.message.content || '';
  if (!content.includes('```')) {
    return [
      {
        type: 'text',
        lines: content.split('\n')
      }
    ];
  }

  const blocks: Block[] = [];
  const codeBlockRegex = /```([a-zA-Z0-9_\-\+\#]*)\n([\s\S]*?)(?:```|$)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = codeBlockRegex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      const textBefore = content.slice(lastIndex, match.index);
      if (textBefore.trim()) {
        blocks.push({
          type: 'text',
          lines: textBefore.replace(/^\n+|\n+$/g, '').split('\n')
        });
      }
    }

    const lang = match[1]?.trim().toLowerCase() || 'java';
    const code = match[2] || '';
    blocks.push({
      type: 'code',
      lang,
      code: code.trimEnd(),
      codeLines: code.trimEnd().split('\n')
    });

    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < content.length) {
    const remainingText = content.slice(lastIndex);
    if (remainingText.trim()) {
      blocks.push({
        type: 'text',
        lines: remainingText.replace(/^\n+|\n+$/g, '').split('\n')
      });
    }
  }

  return blocks;
});

// 行内文本格式化 (加粗、高亮关键词)
function formatInlineText(text: string): string {
  if (!text) return '';
  let sanitized = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  // **加粗**
  sanitized = sanitized.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  // `行内代码`
  sanitized = sanitized.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
  return sanitized;
}

// 简单轻量语法着色器 (支持 Java / Python / JS)
function highlightSyntax(line: string, _lang = 'java'): string {
  if (!line) return '&nbsp;';
  let s = line
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // 注释高亮
  if (s.trim().startsWith('//') || s.trim().startsWith('#')) {
    return `<span class="syntax-comment">${s}</span>`;
  }

  // 字符串高亮
  s = s.replace(/(".*?"|'.*?'|`.*?`)/g, '<span class="syntax-string">$1</span>');

  // Java/通用关键字高亮
  const keywords = [
    'public', 'private', 'protected', 'class', 'interface', 'extends', 'implements',
    'void', 'int', 'String', 'boolean', 'double', 'float', 'long', 'char',
    'return', 'new', 'this', 'super', 'if', 'else', 'for', 'while', 'switch', 'case',
    'try', 'catch', 'finally', 'throw', 'throws', 'import', 'package', 'static', 'final'
  ];
  const regex = new RegExp(`\\b(${keywords.join('|')})\\b`, 'g');
  s = s.replace(regex, '<span class="syntax-keyword">$1</span>');

  return s;
}

function handleCopy() {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(props.message.content);
    copyText.value = '已复制全文！';
    ElMessage.success('已复制对话内容');
    setTimeout(() => {
      copyText.value = '复制全文';
    }, 1500);
  }
}

function copySnippet(code?: string) {
  if (!code) return;
  if (navigator.clipboard) {
    navigator.clipboard.writeText(code);
    copiedSnippet.value = code;
    ElMessage.success('代码已复制到剪贴板');
    setTimeout(() => {
      copiedSnippet.value = '';
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

// 原型同款代码块容器
.code-snippet-box {
  margin: 8px 0;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  background: #FAFAFA;
  overflow: hidden;
  max-width: 100%;
  box-sizing: border-box;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);

  .code-header-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 14px;
    background: #F1F5F9;
    border-bottom: 1px solid #E2E8F0;

    .code-lang-tag {
      .lang-pill {
        font-size: 11px;
        font-weight: 700;
        color: #64748B;
        text-transform: lowercase;
      }
    }

    .copy-code-action-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 2px 8px;
      border-radius: 6px;
      border: 1px solid #CBD5E1;
      background: #FFFFFF;
      font-size: 11px;
      color: #475569;
      cursor: pointer;
      transition: all 0.2s ease;

      .copy-icon {
        font-size: 12px;
      }

      &:hover {
        color: #1677FF;
        border-color: #93C5FD;
        background: #EFF6FF;
      }
    }
  }

  .code-body-container {
    display: flex;
    font-family: 'Fira Code', Consolas, Monaco, monospace;
    font-size: 12.5px;
    line-height: 1.65;
    padding: 10px 0;
    overflow-x: auto;
    max-width: 100%;
    box-sizing: border-box;

    .code-gutter-col {
      display: flex;
      flex-direction: column;
      padding: 0 10px;
      border-right: 1px solid #E2E8F0;
      user-select: none;
      text-align: right;
      flex-shrink: 0;

      .line-num {
        color: #94A3B8;
        font-size: 11.5px;
      }
    }

    .code-pre-content {
      margin: 0;
      padding: 0 14px;
      flex: 1;
      min-width: 0;
      overflow-x: auto;

      code {
        font-family: inherit;
      }

      .code-source-line {
        white-space: pre-wrap;
        word-break: break-word;

        :deep(.syntax-keyword) {
          color: #7C3AED;
          font-weight: 600;
        }

        :deep(.syntax-string) {
          color: #059669;
        }

        :deep(.syntax-comment) {
          color: #94A3B8;
          font-style: italic;
        }
      }
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>

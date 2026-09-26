<template>
  <div class="summary-result">
    <div class="panel-head">
      <div class="head-left">
        <el-icon class="head-icon"><Reading /></el-icon>
        <h3>{{ title || '总结结果' }}</h3>
      </div>
      <div class="head-actions">
        <el-tag v-if="modeLabel" size="small" effect="light" class="mode-tag">{{ modeLabel }}</el-tag>
        <span v-if="wordCount > 0" class="meta-chip">{{ wordCount }} 字</span>
        <span v-if="courseName" class="meta-chip">{{ courseName }}</span>
      </div>
    </div>

    <div class="toolbar">
      <el-button size="small" :disabled="!hasContent" @click="emit('copy')">
        <el-icon><CopyDocument /></el-icon>
        <span>复制</span>
      </el-button>
      <el-button size="small" :disabled="!hasContent" @click="emit('export')">
        <el-icon><Download /></el-icon>
        <span>导出 Markdown</span>
      </el-button>
      <el-button size="small" :disabled="generating" @click="emit('regenerate')">
        <el-icon><Refresh /></el-icon>
        <span>重新生成</span>
      </el-button>
      <el-button v-if="generating" size="small" type="danger" plain @click="emit('stop')">
        <el-icon><VideoPause /></el-icon>
        <span>中止</span>
      </el-button>
    </div>

    <div class="result-body">
      <!-- 生成中且尚无正文：展示推演阶段 -->
      <div v-if="generating && !hasContent" class="thinking-box">
        <el-icon class="thinking-icon"><Loading /></el-icon>
        <p class="thinking-text">{{ phaseMessage || 'AI 正在研读资料并组织总结结构...' }}</p>
        <!-- 思考链默认折叠：既不刷屏，又能让用户确认「模型确实在思考」 -->
        <details v-if="reasoning" class="reasoning-details">
          <summary>查看思考过程（{{ reasoning.length }} 字）</summary>
          <pre class="reasoning-body">{{ reasoning }}</pre>
        </details>
      </div>

      <!-- 正文渲染：接入全局统一 chat-md-content 容器排版规范与代码块复制交互 -->
      <div v-else-if="hasContent" class="markdown-body chat-md-content" v-html="html"></div>

      <!-- 空状态 -->
      <div v-else class="empty-box">
        <el-icon class="empty-icon"><DocumentCopy /></el-icon>
        <p class="empty-title">还没有生成内容</p>
        <p class="empty-tip">在左侧选择知识库文档或粘贴文本，挑选一种总结模式后点击「生成智能总结」。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Reading,
  CopyDocument,
  Download,
  Refresh,
  VideoPause,
  Loading,
  DocumentCopy
} from '@element-plus/icons-vue';

defineProps<{
  html: string;
  title: string;
  modeLabel: string;
  wordCount: number;
  courseName: string;
  generating: boolean;
  hasContent: boolean;
  phaseMessage: string;
  reasoning: string;
}>();

const emit = defineEmits<{ copy: []; export: []; regenerate: []; stop: [] }>();
</script>

<style scoped lang="scss">
.summary-result {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  overflow: hidden;
  /* 视口高度铺满：header(64px) + 底部留白(40px)，正文区内部滚动（1200px 以下自动放开） */
  height: calc(100vh - 104px);
  min-height: 520px;

  .panel-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 22px 28px 14px;

    .head-left {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;

      .head-icon {
        font-size: 18px;
        color: #1677ff;
        flex-shrink: 0;
      }

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .head-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;

      .mode-tag {
        border-radius: 9999px;
      }

      .meta-chip {
        font-size: 11.5px;
        color: #64748b;
        background: #f1f5f9;
        border-radius: 9999px;
        padding: 3px 10px;
      }
    }
  }

  .toolbar {
    display: flex;
    gap: 8px;
    padding: 0 28px 16px;
    border-bottom: 1px solid #f1f5f9;
  }

  .result-body {
    flex: 1;
    /* 关键：flex 子项默认 min-height:auto 不会收缩，缺了这行 overflow:auto 永远不出现滚动条 */
    min-height: 0;
    padding: 22px 28px;
    overflow: auto;

    .thinking-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      min-height: 320px;
      text-align: center;

      .thinking-icon {
        font-size: 30px;
        color: #2563eb;
        animation: summary-spin 1.2s linear infinite;
      }

      .thinking-text {
        margin: 0;
        font-size: 13.5px;
        color: #475569;
      }

      .reasoning-details {
        width: 100%;
        max-width: 560px;
        margin-top: 6px;
        text-align: left;

        summary {
          cursor: pointer;
          font-size: 12px;
          color: #94a3b8;
          user-select: none;

          &:hover {
            color: #2563eb;
          }
        }

        .reasoning-body {
          margin: 8px 0 0;
          max-height: 180px;
          overflow: auto;
          padding: 10px 12px;
          border-radius: 10px;
          background: #f8fafc;
          border: 1px solid #eef2f7;
          font-size: 11.5px;
          line-height: 1.7;
          color: #64748b;
          white-space: pre-wrap;
          word-break: break-word;
        }
      }
    }

    .empty-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      min-height: 320px;
      text-align: center;

      .empty-icon {
        font-size: 38px;
        color: #cbd5e1;
      }

      .empty-title {
        margin: 0;
        font-size: 14px;
        font-weight: 600;
        color: #94a3b8;
      }

      .empty-tip {
        margin: 0;
        max-width: 420px;
        font-size: 12px;
        line-height: 1.7;
        color: #cbd5e1;
      }
    }

    .markdown-body {
      font-size: 13.5px;
      line-height: 1.85;
      color: #1e293b;
      word-break: break-word;

      :deep(h1),
      :deep(h2),
      :deep(h3) {
        color: #0f172a;
        font-weight: 700;
        line-height: 1.4;
        margin: 1.1em 0 0.5em;
      }

      :deep(h1) { font-size: 20px; }
      :deep(h2) {
        font-size: 17px;
        padding-left: 10px;
        border-left: 4px solid #2563eb;
      }
      :deep(h3) { font-size: 15px; }

      :deep(p) { margin: 0.5em 0; }

      :deep(ul),
      :deep(ol) {
        padding-left: 1.4em;
        margin: 0.5em 0;
      }

      :deep(li) { margin: 0.28em 0; }

      :deep(strong) {
        font-weight: 700;
        color: #0f172a;
      }

      :deep(strong.chat-md-label) {
        color: #1d4ed8;
      }

      :deep(table) {
        width: 100%;
        border-collapse: collapse;
        margin: 0.8em 0;
        font-size: 12.5px;
      }

      :deep(th),
      :deep(td) {
        border: 1px solid #e2e8f0;
        padding: 7px 10px;
        text-align: left;
      }

      :deep(th) {
        background: #f8fafc;
        color: #475569;
      }

      :deep(pre:not(.code-block-wrapper pre)) {
        background: #0f172a;
        color: #e2e8f0;
        padding: 14px 16px;
        border-radius: 12px;
        overflow: auto;
        font-size: 12.5px;
      }

      :deep(.code-block-wrapper) {
        border-radius: 12px;
        border: 1px solid #334155;
        overflow: hidden;
        margin: 14px 0;
        background: #0f172a;

        .code-header {
          background: #1e293b;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
          padding: 8px 14px;
          display: flex;
          align-items: center;
          justify-content: space-between;

          .code-lang {
            font-size: 11.5px;
            font-weight: 700;
            color: #38bdf8;
            letter-spacing: 0.5px;
            text-transform: uppercase;
          }

          .code-copy-btn {
            background: #334155;
            color: #e2e8f0;
            border: 1px solid rgba(255, 255, 255, 0.12);
            padding: 3px 10px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s ease;

            &:hover {
              background: #475569;
              color: #ffffff;
            }

            &.is-copied {
              background: #10b981;
              border-color: #10b981;
              color: #ffffff;
            }
          }
        }

        pre {
          background: #0f172a !important;
          padding: 14px 16px;
          margin: 0;
          overflow-x: auto;
          font-size: 12.5px;
          line-height: 1.6;
        }

        code {
          font-family: 'JetBrains Mono', Consolas, monospace;
        }
      }

      :deep(blockquote) {
        margin: 0.6em 0;
        padding: 6px 14px;
        border-left: 4px solid #bfdbfe;
        background: #f5f9ff;
        color: #475569;
      }
    }
  }
}

@keyframes summary-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 窄屏取消固定高度，交还给页面整体滚动 */
@media (max-width: 1200px) {
  .summary-result {
    height: auto;
  }
}
</style>

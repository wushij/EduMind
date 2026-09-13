<template>
  <div class="global-assistant-root">
    <div
      class="assistant-trigger-fab"
      :class="{ 'is-active': drawerVisible }"
      title="智教云 · 全局智能助手"
      @click="toggleDrawer"
    >
      <div class="fab-glow-ring" />
      <div class="fab-inner">
        <el-icon class="fab-icon"><Service /></el-icon>
        <span class="fab-text">AI 助手</span>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      title="智教云 · 全域智能助手"
      size="440px"
      direction="rtl"
      :with-header="false"
      class="global-assistant-drawer"
      destroy-on-close
    >
      <div class="drawer-layout">
        <div class="drawer-header">
          <div class="header-main">
            <div class="header-avatar">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="header-titles">
              <div class="title-row">
                <span class="main-title">智教云 · 智能助手</span>
                <span class="version-tag">V1.1 中枢</span>
              </div>
              <span class="sub-desc">跨业务统一意图分发 · 知识库检索 · 教学指导</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button link :icon="Delete" title="清空对话" @click="clearMessages" />
            <el-button link :icon="Close" title="关闭" @click="drawerVisible = false" />
          </div>
        </div>

        <div class="preset-prompts-bar">
          <span class="preset-label">快捷指令：</span>
          <div class="prompt-chips">
            <button
              v-for="chip in presetChips"
              :key="chip.label"
              type="button"
              class="prompt-chip"
              @click="handleSendPrompt(chip.prompt)"
            >
              {{ chip.label }}
            </button>
          </div>
        </div>

        <div ref="messagesScrollRef" class="messages-scroll-area">
          <div v-if="messages.length === 0" class="empty-welcome">
            <div class="robot-glow-box">
              <el-icon class="huge-robot-icon"><ChatDotRound /></el-icon>
            </div>
            <h4 class="welcome-title">您好！我是智教云智能教学副驾驶</h4>
            <p class="welcome-desc">
              您可以问我任何教学、试题组卷或学情分析相关的问题，或者点击上方快捷指令一键体验！
            </p>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-bubble-row"
            :class="[msg.role === 'user' ? 'is-user' : 'is-assistant']"
          >
            <div class="bubble-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="32" class="user-avatar">
                我
              </el-avatar>
              <div v-else class="assistant-avatar">
                <el-icon><Cpu /></el-icon>
              </div>
            </div>

            <div class="bubble-content-wrap">
              <div v-if="msg.intent" class="intent-recognition-pill">
                <el-tag size="small" :type="getIntentTagType(msg.intent)" effect="dark">
                  意图：{{ msg.intentDesc || msg.intent }}
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

              <div class="message-text">
                {{ msg.content }}
                <span v-if="msg.streaming" class="cursor-blink">|</span>
              </div>
            </div>
          </div>
        </div>

        <div class="drawer-footer-input">
          <div class="input-card">
            <el-input
              v-model="inputContent"
              type="textarea"
              :rows="2"
              placeholder="请输入您的问题或需求（Enter 发送，Shift+Enter 换行）..."
              resize="none"
              :disabled="isStreaming"
              @keydown.enter.exact.prevent="handleSubmit"
            />
            <div class="input-actions-bar">
              <span class="active-course-hint">
                课程空间: #{{ activeCourseId }}
              </span>
              <el-button
                type="primary"
                size="small"
                :loading="isStreaming"
                :disabled="!inputContent.trim()"
                @click="handleSubmit"
              >
                <span>发送</span>
                <el-icon class="el-icon--right"><Promotion /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  Service,
  Cpu,
  Close,
  Delete,
  ChatDotRound,
  Promotion
} from '@element-plus/icons-vue';
import { useGlobalAssistant } from '@/composables/ai/useGlobalAssistant';

const router = useRouter();

const {
  drawerVisible,
  inputContent,
  isStreaming,
  messages,
  messagesScrollRef,
  activeCourseId,
  presetChips,
  toggleDrawer,
  clearMessages,
  handleSubmit,
  handleSendPrompt,
  getIntentTagType
} = useGlobalAssistant();

function handleNavigate(path: string) {
  drawerVisible.value = false;
  router.push(path);
}
</script>

<style scoped lang="scss">
.global-assistant-root {
  .assistant-trigger-fab {
    position: fixed;
    right: 28px;
    bottom: 32px;
    z-index: 1000;
    width: 58px;
    height: 58px;
    border-radius: 50%;
    cursor: pointer;
    user-select: none;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
    box-shadow: 0 8px 24px rgba(22, 119, 255, 0.4);
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

    &:hover {
      transform: translateY(-3px) scale(1.05);
      box-shadow: 0 12px 30px rgba(114, 46, 209, 0.5);
    }

    &.is-active {
      transform: scale(0.92);
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
    }

    .fab-glow-ring {
      position: absolute;
      inset: -3px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba(22, 119, 255, 0.6), rgba(114, 46, 209, 0.6));
      filter: blur(6px);
      z-index: 1;
      animation: pulseGlow 2.5s infinite;
    }

    .fab-inner {
      position: relative;
      z-index: 2;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #FFFFFF;

      .fab-icon {
        font-size: 22px;
        margin-bottom: 1px;
      }
      .fab-text {
        font-size: 10px;
        font-weight: 600;
        line-height: 1;
        letter-spacing: 0.5px;
      }
    }
  }
}

.drawer-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;

  .drawer-header {
    background: #FFFFFF;
    padding: 16px 20px;
    border-bottom: 1px solid #E2E8F0;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-main {
      display: flex;
      align-items: center;
      gap: 12px;

      .header-avatar {
        width: 38px;
        height: 38px;
        border-radius: 10px;
        background: linear-gradient(135deg, #1677FF, #722ED1);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #FFFFFF;
        font-size: 20px;
      }

      .header-titles {
        display: flex;
        flex-direction: column;

        .title-row {
          display: flex;
          align-items: center;
          gap: 8px;

          .main-title {
            font-size: 15px;
            font-weight: 700;
            color: #1E293B;
          }

          .version-tag {
            font-size: 11px;
            background: #E6F4FF;
            color: #1677FF;
            padding: 1px 6px;
            border-radius: 4px;
            font-weight: 600;
          }
        }

        .sub-desc {
          font-size: 11px;
          color: #94A3B8;
          margin-top: 2px;
        }
      }
    }
  }

  .preset-prompts-bar {
    padding: 10px 18px;
    background: #FFFFFF;
    border-bottom: 1px solid #F1F5F9;
    display: flex;
    align-items: center;
    gap: 8px;

    .preset-label {
      font-size: 12px;
      color: #64748B;
      flex-shrink: 0;
    }

    .prompt-chips {
      display: flex;
      gap: 6px;
      overflow-x: auto;
      scrollbar-width: none;
      &::-webkit-scrollbar { display: none; }

      .prompt-chip {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 14px;
        background: #F1F5F9;
        border: 1px solid #E2E8F0;
        color: #475569;
        cursor: pointer;
        white-space: nowrap;
        transition: all 0.2s;

        &:hover {
          background: #E6F4FF;
          color: #1677FF;
          border-color: #91CAFF;
        }
      }
    }
  }

  .messages-scroll-area {
    flex: 1;
    overflow-y: auto;
    padding: 18px;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .empty-welcome {
      margin: auto 0;
      text-align: center;
      padding: 40px 20px;

      .robot-glow-box {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: linear-gradient(135deg, #E6F4FF, #F9F0FF);
        margin: 0 auto 16px;
        display: flex;
        align-items: center;
        justify-content: center;

        .huge-robot-icon {
          font-size: 32px;
          color: #1677FF;
        }
      }

      .welcome-title {
        font-size: 16px;
        font-weight: 600;
        color: #1E293B;
        margin: 0 0 8px 0;
      }

      .welcome-desc {
        font-size: 13px;
        color: #64748B;
        line-height: 1.6;
        margin: 0;
      }
    }

    .message-bubble-row {
      display: flex;
      gap: 10px;
      align-items: flex-start;

      &.is-user {
        flex-direction: row-reverse;

        .user-avatar {
          background: #1677FF;
          color: #FFFFFF;
          font-size: 12px;
          font-weight: 600;
        }

        .bubble-content-wrap {
          align-items: flex-end;

          .message-text {
            background: #1677FF;
            color: #FFFFFF;
            border-radius: 12px 2px 12px 12px;
          }
        }
      }

      &.is-assistant {
        .assistant-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: linear-gradient(135deg, #1677FF, #722ED1);
          color: #FFFFFF;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;
          flex-shrink: 0;
        }

        .bubble-content-wrap {
          align-items: flex-start;

          .message-text {
            background: #FFFFFF;
            color: #1E293B;
            border: 1px solid #E2E8F0;
            border-radius: 2px 12px 12px 12px;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
          }
        }
      }

      .bubble-content-wrap {
        display: flex;
        flex-direction: column;
        max-width: 82%;
        gap: 6px;

        .intent-recognition-pill {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .message-text {
          padding: 10px 14px;
          font-size: 13px;
          line-height: 1.6;
          word-break: break-word;
          white-space: pre-wrap;
        }

        .cursor-blink {
          animation: blink 1s infinite;
          font-weight: bold;
          color: #1677FF;
        }
      }
    }
  }

  .drawer-footer-input {
    background: #FFFFFF;
    padding: 14px 18px;
    border-top: 1px solid #E2E8F0;

    .input-card {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .input-actions-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .active-course-hint {
          font-size: 11px;
          color: #94A3B8;
        }
      }
    }
  }
}

@keyframes pulseGlow {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.12); opacity: 0.9; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>

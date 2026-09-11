<template>
  <div class="prototype-chat-dock">
    <!-- 附件预览标签条 -->
    <div v-if="attachedFiles.length > 0" class="attached-chips-row">
      <div v-for="(file, fIdx) in attachedFiles" :key="fIdx" class="file-chip">
        <el-icon class="file-icon"><Document /></el-icon>
        <span class="file-name">{{ file.name }}</span>
        <span class="file-size">({{ formatFileSize(file.size) }})</span>
        <button type="button" class="remove-chip-btn" @click="removeFile(fIdx)">×</button>
      </div>
    </div>

    <!-- 主体卡片容器 (模仿 AgentOne 自适应展开模式) -->
    <div class="chat-input-box-card" :class="{ 'is-expanded': isMultiline }">
      <!-- 隐藏的文件选择器 -->
      <input
        ref="fileInputRef"
        type="file"
        multiple
        style="display: none"
        @change="handleFilesSelected"
      />

      <!-- 上半区：文本输入与单行右侧操作按钮 -->
      <div class="input-upper-row" :class="{ 'is-multi': isMultiline }">
        <textarea
          ref="textareaRef"
          v-model="inputText"
          class="chat-textarea-native"
          :class="{ 'chat-textarea-native--multi': isMultiline, 'chat-textarea-native--single': !isMultiline }"
          placeholder="请输入你的问题，Enter 发送，Shift+Enter 换行..."
          :rows="isMultiline ? 2 : 1"
          @keydown="handleKeydown"
          @input="onInput"
        ></textarea>

        <!-- 单行模式下的右侧操作区 (单行时位于输入框右侧) -->
        <div v-if="!isMultiline" class="single-row-actions">
          <!-- 停止输出 / 暂停响应按钮 (AgentOne 胶囊同款风格) -->
          <button
            v-if="streaming"
            type="button"
            class="action-btn action-btn--stop"
            title="停止生成 / 暂停输出"
            @click="$emit('stop')"
          >
            <el-icon class="stop-icon"><VideoPause /></el-icon>
            <span>停止响应</span>
          </button>

          <!-- 发送按钮 -->
          <button
            v-else
            type="button"
            class="action-btn action-btn--send"
            :class="{ active: canSend }"
            :disabled="!canSend"
            title="发送 (Enter)"
            @click="handleSend"
          >
            <el-icon class="send-icon"><Promotion /></el-icon>
          </button>
        </div>
      </div>

      <!-- 下半区：原型工具栏与多行操作区 -->
      <div class="input-tools-bar">
        <div class="tools-left">
          <button
            type="button"
            class="tool-btn"
            title="上传课程资料或代码附件"
            @click="triggerUpload"
          >
            <el-icon class="tool-icon"><Paperclip /></el-icon>
            <span>上传文件</span>
          </button>

          <button
            type="button"
            class="tool-btn"
            :class="{ active: webSearchEnabled }"
            title="启用联网检索以获取最新技术动态与知识"
            @click="toggleWebSearch"
          >
            <el-icon class="tool-icon"><Compass /></el-icon>
            <span>联网搜索</span>
          </button>

          <button
            type="button"
            class="tool-btn"
            :class="{ active: codeModeActive }"
            title="切换/插入标准代码模板"
            @click="toggleCodeMode"
          >
            <el-icon class="tool-icon"><EditPen /></el-icon>
            <span>代码模式</span>
          </button>
        </div>

        <div class="tools-right">
          <span class="send-hint-text">按 Enter 发送，Shift+Enter 换行</span>

          <!-- 多行展开模式下：发送 / 停止按钮放置在工具栏右侧 (与 AgentOne 完全一致) -->
          <div v-if="isMultiline" class="multi-row-actions">
            <!-- 停止输出 / 暂停响应按钮 -->
            <button
              v-if="streaming"
              type="button"
              class="action-btn action-btn--stop"
              title="停止生成 / 暂停输出"
              @click="$emit('stop')"
            >
              <el-icon class="stop-icon"><VideoPause /></el-icon>
              <span>停止响应</span>
            </button>

            <!-- 发送按钮 -->
            <button
              v-else
              type="button"
              class="action-btn action-btn--send"
              :class="{ active: canSend }"
              :disabled="!canSend"
              title="发送 (Enter)"
              @click="handleSend"
            >
              <el-icon class="send-icon"><Promotion /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { Paperclip, Compass, EditPen, Document, VideoPause, Promotion } from '@element-plus/icons-vue';

const props = defineProps<{
  streaming?: boolean;
}>();

const emit = defineEmits<{
  (e: 'send', text: string): void;
  (e: 'stop'): void;
}>();

const inputText = ref('');
const textareaRef = ref<HTMLTextAreaElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const webSearchEnabled = ref(false);
const codeModeActive = ref(false);
const attachedFiles = ref<File[]>([]);
const isMultiline = ref(false);

const canSend = computed(() => {
  return (inputText.value.trim().length > 0 || attachedFiles.value.length > 0) && !props.streaming;
});

function checkMultiline() {
  const val = inputText.value || '';
  if (!val) {
    isMultiline.value = false;
    return;
  }
  if (val.includes('\n')) {
    isMultiline.value = true;
    return;
  }
  const el = textareaRef.value;
  if (el) {
    isMultiline.value = el.scrollHeight > 44;
  } else {
    isMultiline.value = val.length > 50;
  }
}

function adjustTextareaHeight() {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = 'auto';
  const minH = isMultiline.value ? 44 : 36;
  const nextH = Math.min(Math.max(el.scrollHeight, minH), 200);
  el.style.height = `${nextH}px`;
}

function onInput() {
  checkMultiline();
  adjustTextareaHeight();
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}

watch(
  inputText,
  () => {
    nextTick(() => {
      checkMultiline();
      adjustTextareaHeight();
    });
  },
  { immediate: true }
);

function handleSend() {
  if (!canSend.value) return;

  let textToSend = inputText.value.trim();
  if (attachedFiles.value.length > 0) {
    const fileNames = attachedFiles.value.map(f => f.name).join(', ');
    textToSend = `[已附加资料: ${fileNames}]\n${textToSend}`;
  }

  emit('send', textToSend);
  inputText.value = '';
  attachedFiles.value = [];
  isMultiline.value = false;
  nextTick(() => {
    adjustTextareaHeight();
  });
}

function triggerUpload() {
  fileInputRef.value?.click();
}

function handleFilesSelected(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.files) {
    const files = Array.from(target.files);
    attachedFiles.value.push(...files);
    ElMessage.success(`已添加 ${files.length} 个附件`);
  }
}

function removeFile(index: number) {
  attachedFiles.value.splice(index, 1);
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function toggleWebSearch() {
  webSearchEnabled.value = !webSearchEnabled.value;
  if (webSearchEnabled.value) {
    ElMessage.success('已开启联网搜索增强');
  } else {
    ElMessage.info('已关闭联网搜索');
  }
}

function toggleCodeMode() {
  codeModeActive.value = !codeModeActive.value;
  if (codeModeActive.value) {
    const codeSnippet = "\n```java\n// 请在此输入或粘贴代码\npublic class Demo {\n    public static void main(String[] args) {\n        \n    }\n}\n```\n";
    inputText.value += codeSnippet;
    ElMessage.success('已插入 Java 代码模板');
  }
}

defineExpose({
  setInputText: (text: string) => {
    inputText.value = text;
    nextTick(() => {
      textareaRef.value?.focus();
      checkMultiline();
      adjustTextareaHeight();
    });
  }
});
</script>

<style scoped lang="scss">
.prototype-chat-dock {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding: 10px 18px 16px;
  background: #FAFCFE;

  .attached-chips-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 8px;

    .file-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 6px;
      background: #EFF6FF;
      border: 1px solid #BFDBFE;
      font-size: 12px;
      color: #1E40AF;

      .file-icon {
        font-size: 13px;
      }

      .file-name {
        font-weight: 500;
        max-width: 140px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .file-size {
        color: #60A5FA;
        font-size: 11px;
      }

      .remove-chip-btn {
        background: transparent;
        border: none;
        color: #93C5FD;
        font-size: 14px;
        cursor: pointer;
        padding: 0;
        line-height: 1;

        &:hover {
          color: #1E40AF;
        }
      }
    }
  }

  // 主体卡片容器 (模仿 AgentOne 动态响应)
  .chat-input-box-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    padding: 10px 14px 8px;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: #CBD5E1;
    }

    &:focus-within {
      border-color: #2563EB;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12), 0 4px 16px rgba(0, 0, 0, 0.05);
    }

    &.is-expanded {
      padding: 12px 14px 10px;
      box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
    }

    .input-upper-row {
      display: flex;
      align-items: flex-end;
      gap: 12px;
      width: 100%;

      &.is-multi {
        flex-direction: column;
        align-items: stretch;
      }

      .chat-textarea-native {
        border: none;
        outline: none;
        background: transparent;
        font-size: 14px;
        color: #1E293B;
        line-height: 1.55;
        font-family: inherit;
        resize: none;
        box-sizing: border-box;
        transition: height 0.12s ease-out;

        &::placeholder {
          color: #94A3B8;
        }

        &--single {
          flex: 1;
          padding: 4px 0;
          min-height: 36px;
          max-height: 44px;
          overflow-y: hidden;
        }

        &--multi {
          width: 100%;
          padding: 4px 2px;
          min-height: 44px;
          max-height: 200px;
          overflow-y: auto;
        }
      }

      .single-row-actions {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0;
        margin-bottom: 2px;
      }
    }

    .input-tools-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-top: 1px solid #F1F5F9;
      padding-top: 8px;
      margin-top: 6px;

      .tools-left {
        display: flex;
        align-items: center;
        gap: 14px;

        .tool-btn {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background: transparent;
          border: none;
          padding: 0;
          font-size: 12px;
          color: #64748B;
          cursor: pointer;
          transition: color 0.18s;

          .tool-icon {
            font-size: 13.5px;
          }

          &:hover {
            color: #2563EB;
          }

          &.active {
            color: #2563EB;
            font-weight: 600;
          }
        }
      }

      .tools-right {
        display: flex;
        align-items: center;
        gap: 12px;

        .send-hint-text {
          font-size: 11px;
          color: #94A3B8;
          user-select: none;
        }

        .multi-row-actions {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }
  }

  // ================= 仿 AgentOne 发送与停止按钮样式 =================
  .action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 34px;
    border: none;
    border-radius: 999px;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

    // 经典圆角发送按钮 (Promotion 图标)
    &--send {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      padding: 0;
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      color: #FFFFFF;
      box-shadow: 0 3px 10px rgba(37, 99, 235, 0.32);

      .send-icon {
        font-size: 15px;
        transform: translate(-1px, 1px);
      }

      &:disabled {
        opacity: 0.45;
        background: #94A3B8;
        box-shadow: none;
        cursor: not-allowed;
      }

      &.active:hover {
        transform: scale(1.08);
        box-shadow: 0 5px 16px rgba(37, 99, 235, 0.45);
      }

      &.active:active {
        transform: scale(0.94);
      }
    }

    // 仿 AgentOne 停止输出 / 暂停响应胶囊按钮 (VideoPause 图标)
    &--stop {
      position: relative;
      height: 34px;
      padding: 0 14px;
      gap: 5px;
      background: rgba(239, 68, 68, 0.1) !important;
      color: #EF4444 !important;
      border: 1px solid rgba(239, 68, 68, 0.28) !important;
      overflow: hidden;
      font-size: 12.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) !important;

      &::before {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
        opacity: 0;
        transition: opacity 0.25s ease;
        z-index: 0;
      }

      > * {
        position: relative;
        z-index: 1;
        transition: all 0.25s ease;
      }

      .stop-icon {
        font-size: 14px;
        transition: transform 0.25s ease;
      }

      &:hover {
        color: #FFFFFF !important;
        border-color: #EF4444 !important;
        box-shadow: 0 4px 16px rgba(239, 68, 68, 0.4), 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
        transform: translateY(-1px) scale(1.03);

        &::before {
          opacity: 1;
        }

        .stop-icon {
          transform: scale(1.15) rotate(-90deg);
        }
      }

      &:active {
        transform: translateY(0) scale(0.96);
        box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3) !important;
      }
    }
  }
}
</style>

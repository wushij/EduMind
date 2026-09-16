<template>
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
                    <button
                      v-if="inputContent.trim() && !isStreaming"
                      type="button"
                      class="composer-btn--clear"
                      title="清空当前输入"
                      @click="inputContent = ''"
                    >
                      <el-icon><CircleClose /></el-icon>
                    </button>

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

              <p class="copilot-disclaimer">
                内容由智教云知识图谱与教学大模型实时检索生成 · 教学结论仅供备课与研读参考
              </p>
            </footer>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { CircleClose, VideoPause, Promotion } from '@element-plus/icons-vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const {
  inputContent,
  isStreaming,
  isMultiline,
  textareaRef,
  handleInput,
  handleTextareaKeydown,
  handleSubmit,
  stopStreaming
} = inject(globalAssistantUiKey)!;
</script>

<template>
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
                      @click.stop="confirmDeleteSession(String(item.id))"
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
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { Delete } from '@element-plus/icons-vue';
import { globalAssistantUiKey } from '@/components/ai/global-assistant/global-assistant-ui-key';

const {
  isHistoryPanelOpen,
  sessions,
  conversationId,
  confirmClearAllSessions,
  startNewSession,
  selectSession,
  formatSessionTime,
  confirmDeleteSession
} = inject(globalAssistantUiKey)!;
</script>

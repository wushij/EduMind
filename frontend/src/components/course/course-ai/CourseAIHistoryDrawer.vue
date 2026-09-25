<template>
    <!-- 历史记录侧边抽屉 -->
    <el-drawer
      v-model="historyDrawerVisible"
      title="问答历史会话"
      size="360px"
      direction="rtl"
      :append-to-body="true"
      @open="handleDrawerOpen"
    >
      <ChatSessionList
        :sessions="sessions"
        :current-id="currentSessionId"
        show-clear-all
        @select="handleSelectSession"
        @create="handleCreateNewSession"
        @delete="handleDeleteSession"
        @clear-all="handleClearAllSessions"
      />
    </el-drawer>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import ChatSessionList from '@/components/ai/ChatSessionList.vue';
import { courseAiUiKey } from '@/components/course/course-ai/course-ai-ui-key';

const {
  historyDrawerVisible,
  sessions,
  currentSessionId,
  currentCourseIdNum,
  refreshSessionsMeta,
  handleSelectSession,
  handleCreateNewSession,
  handleDeleteSession,
  handleClearAllSessions
} = inject(courseAiUiKey)!;

function handleDrawerOpen() {
  if (currentCourseIdNum?.value) {
    void refreshSessionsMeta(currentCourseIdNum.value);
  }
}
</script>

<style scoped lang="scss">
// 抽屉头部：收紧默认 32px 下边距，由内部列表自行控制间距
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 14px 12px;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
}

// 抽屉内容区：垂直方向去掉默认内边距，水平方向与内部 14px 网格对齐，让背景铺满
:deep(.el-drawer__body) {
  padding: 0 14px;
  overflow: hidden;
  background: #F8FAFC;
}
</style>

<template>
    <!-- 历史记录侧边抽屉 -->
    <el-drawer
      v-model="historyDrawerVisible"
      title="问答历史会话"
      size="320px"
      direction="rtl"
      :append-to-body="true"
    >
      <ChatSessionList
        :sessions="sessions"
        :current-id="currentSessionId"
        @select="handleSelectSession"
        @create="handleCreateNewSession"
        @delete="handleDeleteSession"
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
  handleSelectSession,
  handleCreateNewSession,
  handleDeleteSession
} = inject(courseAiUiKey)!;
</script>

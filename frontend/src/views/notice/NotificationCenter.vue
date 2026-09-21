<template>
  <div class="notification-center-page profile-page-shell">
    <ProfilePageHero
      title="消息通知"
      subtitle="查看系统、教学、知识库与 AI 相关的全部通知"
    >
      <!-- 页面级操作统一放在顶部标题行右侧（与错题本等模块的 hero 操作位一致） -->
      <template #actions>
        <div class="hero-action-row">
          <el-button
            type="primary"
            plain
            round
            :disabled="unreadCount === 0"
            @click="onReadAll"
          >
            全部已读
          </el-button>
          <el-button
            type="danger"
            plain
            round
            :disabled="!hasNotifications"
            @click="onClearAll"
          >
            清空通知
          </el-button>
        </div>
      </template>
    </ProfilePageHero>

    <div class="profile-surface-card notify-surface">
      <NotificationPanel
        ref="panelRef"
        variant="page"
        :show-header="false"
        :show-head-actions-only="false"
        :page-size="20"
        :enable-realtime="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import NotificationPanel from '@/components/notification/NotificationPanel.vue';

/** 顶部操作按钮复用面板内部的同一份列表状态，避免重复请求与未读数不一致 */
const panelRef = ref<InstanceType<typeof NotificationPanel> | null>(null);

const unreadCount = computed(() => panelRef.value?.unreadCount ?? 0);

const hasNotifications = computed(() => {
  const panel = panelRef.value;
  if (!panel) return false;
  return (panel.total ?? 0) > 0 || (panel.notifications?.length ?? 0) > 0;
});

function onReadAll() {
  void panelRef.value?.handleReadAll();
}

function onClearAll() {
  void panelRef.value?.handleClearAll();
}
</script>

<style scoped lang="scss">
@use '@/styles/profile-page-shell.scss';

.notification-center-page {
  min-height: calc(100vh - 120px);
}
</style>

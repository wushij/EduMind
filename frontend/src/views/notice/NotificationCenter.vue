<template>
  <div class="notification-center-page profile-page-shell">
    <ProfilePageHero
      title="消息通知"
      subtitle="查看系统、教学、知识库与 AI 相关的全部通知"
    />

    <div class="profile-surface-card notify-surface">
      <NotificationPanel
        variant="page"
        :show-header="false"
        :show-head-actions-only="true"
        :page-size="20"
        :enable-realtime="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import NotificationPanel from '@/components/notification/NotificationPanel.vue';
</script>

<style scoped lang="scss">
@use '@/styles/profile-page-shell.scss';

.notification-center-page {
  min-height: calc(100vh - 120px);
}

.notify-surface {
  :deep(.notify-panel-head) {
    margin-bottom: 4px;
    justify-content: flex-end;
  }
}
</style>

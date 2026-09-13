<template>
  <div class="notification-center-page">
    <el-card shadow="never" class="center-card">
      <NotificationPanel variant="page" :page-size="20" :enable-realtime="true" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import NotificationPanel from '@/components/notification/NotificationPanel.vue';
</script>

<style scoped lang="scss">
.notification-center-page {
  padding: 20px 24px;
  min-height: calc(100vh - 120px);
  background: #f8fafc;
}

.center-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);

  :deep(.el-card__body) {
    padding: 24px;
  }
}
</style>

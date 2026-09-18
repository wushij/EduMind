<template>
  <div class="notify-wrap" @click="openDrawer">
    <button class="notify-btn" type="button" title="消息通知">
      <el-icon :size="20"><Bell /></el-icon>
    </button>
    <span v-if="unreadCount > 0" class="notify-dot">
      {{ unreadCount > 99 ? '99+' : unreadCount }}
    </span>
  </div>

  <el-drawer
    v-model="drawerVisible"
    title="消息通知"
    size="440px"
    append-to-body
    class="notify-drawer"
    @opened="onDrawerOpened"
  >
    <NotificationPanel
      ref="panelRef"
      variant="drawer"
      :show-pagination="false"
      :page-size="50"
      :enable-realtime="true"
      :realtime-active="() => drawerVisible"
      :auto-fetch="false"
      @view-all="goCenter"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Bell } from '@element-plus/icons-vue';
import { useNotifyStore } from '@/stores/notification/notify';
import NotificationPanel from './NotificationPanel.vue';

const router = useRouter();
const notifyStore = useNotifyStore();

const drawerVisible = ref(false);
const panelRef = ref<InstanceType<typeof NotificationPanel> | null>(null);
const unreadCount = computed(() => notifyStore.unreadCount);

function openDrawer() {
  drawerVisible.value = true;
}

function goCenter() {
  drawerVisible.value = false;
  void router.push('/notice');
}

async function onDrawerOpened() {
  await nextTick();
  panelRef.value?.refresh();
}
</script>

<style scoped lang="scss">
.notify-wrap {
  position: relative;
  cursor: pointer;
}

.notify-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;

  &:hover {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #1677ff;
  }
}

.notify-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 999px;
  border: 2px solid #fff;
  line-height: 1.2;
}
</style>

<style lang="scss">
.notify-drawer {
  .el-drawer__header {
    margin-bottom: 8px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e2e8f0;
  }

  .el-drawer__body {
    padding-top: 8px;
  }
}
</style>

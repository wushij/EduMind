import { defineStore } from 'pinia';
import { ref } from 'vue';
import { ElNotification } from 'element-plus';
import { getUnreadCount } from '@/api/notification';
import { getNotifyTypeMeta } from '@/utils/notification/notify-category';
import { tokenUtil } from '@/core/auth/token';
import { connectNotifyWs, disconnectNotifyWs, onNotifyPush } from '@/core/websocket/notify-ws';

export const useNotifyStore = defineStore('notify', () => {
  const unreadCount = ref(0);
  let wsStarted = false;
  let wsUnsub: (() => void) | null = null;

  async function refreshUnread() {
    if (!tokenUtil.get()) {
      unreadCount.value = 0;
      return 0;
    }
    try {
      const res = await getUnreadCount();
      unreadCount.value = res.data?.unreadCount ?? 0;
      return unreadCount.value;
    } catch {
      unreadCount.value = 0;
      return 0;
    }
  }

  function startWs() {
    if (!tokenUtil.get() || wsStarted) return;
    wsStarted = true;
    wsUnsub = onNotifyPush((payload) => {
      unreadCount.value = payload.unreadCount;
      if ((payload.priority ?? 0) >= 1) {
        return;
      }
      const meta = getNotifyTypeMeta(payload.type);
      ElNotification({
        title: `[${meta.label}] ${payload.title}`,
        message: payload.content,
        type: 'info',
        duration: 5000,
        position: 'top-right',
        customClass: 'notify-toast'
      });
    });
    connectNotifyWs();
    void refreshUnread();
  }

  function stopWs() {
    wsStarted = false;
    wsUnsub?.();
    wsUnsub = null;
    disconnectNotifyWs(false);
    unreadCount.value = 0;
  }

  return {
    unreadCount,
    refreshUnread,
    startWs,
    stopWs
  };
});

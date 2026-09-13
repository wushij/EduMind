import { ref } from 'vue';
import { onNotifyPush, type NotificationPush } from '@/core/websocket/notify-ws';
import { listNotifications, markNotificationAsRead } from '@/api/notification';
import { tokenUtil } from '@/core/auth/token';
import type { NotificationVO } from '@/types/notification';

export type PriorityNotifyItem = NotificationPush | NotificationVO;

const alertItem = ref<PriorityNotifyItem | null>(null);
const marqueeItem = ref<PriorityNotifyItem | null>(null);
let isListening = false;

export function useBroadcastPush() {
  function setupListener() {
    if (isListening) return;
    isListening = true;

    onNotifyPush((push) => {
      if (push.priority === 1) {
        alertItem.value = push;
      } else if (push.priority === 2) {
        marqueeItem.value = push;
      }
    });
  }

  async function checkUnreadPriority() {
    if (!tokenUtil.get()) return;
    try {
      const res = await listNotifications({ page: 1, pageSize: 20, category: 'system' });
      const list = res.data?.list?.list || [];
      for (const item of list) {
        if (item.isRead !== 0) continue;
        const priority = item.priority ?? 0;
        if (priority === 1 && !alertItem.value) {
          alertItem.value = item;
        } else if (priority === 2 && !marqueeItem.value) {
          marqueeItem.value = item;
        }
      }
    } catch {
      /* ignore */
    }
  }

  async function dismissAlert() {
    const item = alertItem.value;
    alertItem.value = null;
    if (item?.id) {
      try {
        await markNotificationAsRead(item.id);
      } catch {
        /* ignore */
      }
    }
  }

  async function dismissMarquee() {
    const item = marqueeItem.value;
    marqueeItem.value = null;
    if (item?.id) {
      try {
        await markNotificationAsRead(item.id);
      } catch {
        /* ignore */
      }
    }
  }

  return {
    alertItem,
    marqueeItem,
    setupListener,
    checkUnreadPriority,
    dismissAlert,
    dismissMarquee
  };
}

import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  deleteNotification,
  clearAllNotifications
} from '@/api/notification';
import { useNotifyStore } from '@/stores/notification/notify';
import { onNotifyPush } from '@/core/websocket/notify-ws';
import { formatRelativeTime } from '@/utils/format/date';
import {
  getNotifyTypeClass,
  getNotifyTypeMeta,
  matchesNotifyCategory,
  getCategoryEmptyText
} from '@/utils/notification/notify-category';
import { getNotificationNavigatePath } from '@/utils/notification/notify-nav';
import type { NotificationCategory, NotificationListResult, NotificationType, NotificationVO } from '@/types/notification';
import type { PageResult } from '@/types/common/api';

export interface NotifyListItem {
  id: number;
  type: NotificationType;
  typeLabel: string;
  typeClass: string;
  metaTag: string;
  title: string;
  content: string;
  refId?: number;
  read: boolean;
  time: string;
  rawTime: string;
  navigatePath: string | null;
}

export interface UseNotificationListOptions {
  /** drawer 模式默认 50 条，页面模式分页 */
  pageSize?: number;
  enablePagination?: boolean;
  /** 是否订阅 WebSocket 实时追加 */
  enableRealtime?: boolean;
  /** 仅抽屉打开时追加（由外部传入） */
  realtimeActive?: () => boolean;
}

function normalizeListPage(data?: NotificationListResult): {
  items: NotificationVO[];
  total: number;
  unreadCount?: number;
} {
  if (!data) {
    return { items: [], total: 0 };
  }
  const page = data.list;
  let items: NotificationVO[] = [];
  if (Array.isArray(page)) {
    items = page;
  } else if (page && typeof page === 'object') {
    const pageObj = page as PageResult<NotificationVO> & { records?: NotificationVO[] };
    items = pageObj.list ?? pageObj.records ?? [];
  }
  const pageTotal =
    page && typeof page === 'object' && !Array.isArray(page)
      ? Number((page as PageResult<NotificationVO>).total)
      : NaN;
  const total = Number.isFinite(pageTotal) && pageTotal >= 0
    ? pageTotal
    : Number(data.totalCount ?? items.length);
  return {
    items,
    total,
    unreadCount: data.unreadCount
  };
}

function toListItem(raw: {
  id: number;
  type: NotificationType;
  title: string;
  content: string;
  refId?: number;
  isRead: number;
  createTime: string;
}): NotifyListItem {
  const meta = getNotifyTypeMeta(raw.type);
  return {
    id: raw.id,
    type: raw.type,
    typeLabel: meta.label,
    typeClass: getNotifyTypeClass(raw.type),
    metaTag: meta.metaTag,
    title: raw.title,
    content: raw.content,
    refId: raw.refId,
    read: raw.isRead === 1,
    time: formatRelativeTime(raw.createTime),
    rawTime: raw.createTime,
    navigatePath: getNotificationNavigatePath(raw.type, raw.refId)
  };
}

export function useNotificationList(options: UseNotificationListOptions = {}) {
  const router = useRouter();
  const notifyStore = useNotifyStore();

  const loading = ref(false);
  const activeCategory = ref<NotificationCategory>('all');
  const notifications = ref<NotifyListItem[]>([]);
  const page = ref(1);
  const pageSize = ref(options.pageSize ?? 20);
  const total = ref(0);
  const unreadCount = computed(() => notifyStore.unreadCount);
  const emptyText = computed(() => getCategoryEmptyText(activeCategory.value));

  const detailVisible = ref(false);
  const currentDetail = ref<NotifyListItem | null>(null);

  let wsUnsub: (() => void) | null = null;

  async function fetchList() {
    loading.value = true;
    try {
      const res = await listNotifications({
        page: page.value,
        pageSize: pageSize.value,
        category: activeCategory.value
      });
      const { items, total: listTotal, unreadCount: apiUnread } = normalizeListPage(res.data);
      notifications.value = items.map(toListItem);
      total.value = listTotal;
      if (apiUnread != null) {
        notifyStore.unreadCount = apiUnread;
      }
    } catch {
      ElMessage.error('消息列表加载失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  }

  function changeCategory(category: NotificationCategory) {
    if (activeCategory.value === category) return;
    activeCategory.value = category;
    page.value = 1;
    void fetchList();
  }

  function onPageChange() {
    void fetchList();
  }

  function onSizeChange(size: number) {
    pageSize.value = size;
    page.value = 1;
    void fetchList();
  }

  async function markItemRead(item: NotifyListItem) {
    if (item.read) return;
    await markNotificationAsRead(item.id);
    item.read = true;
    if (notifyStore.unreadCount > 0) {
      notifyStore.unreadCount -= 1;
    }
  }

  async function openDetail(item: NotifyListItem) {
    await markItemRead(item);
    currentDetail.value = item;
    detailVisible.value = true;
  }

  async function handleItemClick(item: NotifyListItem) {
    if (item.navigatePath) {
      await markItemRead(item);
      await router.push(item.navigatePath);
      return;
    }
    await openDetail(item);
  }

  function closeDetail() {
    detailVisible.value = false;
  }

  async function handleReadAll() {
    await markAllNotificationsAsRead();
    notifications.value.forEach((item) => {
      item.read = true;
    });
    notifyStore.unreadCount = 0;
    ElMessage.success('已全部标为已读');
  }

  async function handleDeleteItem(item: NotifyListItem) {
    try {
      await ElMessageBox.confirm('确定要删除这条通知吗？删除后无法恢复。', '删除提示', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        roundButton: true
      });
      await deleteNotification(item.id);
      notifications.value = notifications.value.filter((n) => n.id !== item.id);
      total.value = Math.max(0, total.value - 1);
      if (!item.read && notifyStore.unreadCount > 0) {
        notifyStore.unreadCount -= 1;
      }
      ElMessage.success('通知已删除');
    } catch {
      // cancelled
    }
  }

  async function handleClearAll() {
    try {
      await ElMessageBox.confirm('确定要清空所有消息通知吗？清空后无法恢复。', '清空提示', {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning',
        roundButton: true
      });
      await clearAllNotifications();
      notifications.value = [];
      total.value = 0;
      notifyStore.unreadCount = 0;
      ElMessage.success('所有通知已清空');
    } catch {
      // cancelled
    }
  }

  function setupRealtime() {
    if (!options.enableRealtime) return;
    wsUnsub = onNotifyPush((payload) => {
      notifyStore.unreadCount = payload.unreadCount;
      const active = options.realtimeActive?.() ?? true;
      if (!active) return;
      if (!matchesNotifyCategory(payload.type, activeCategory.value)) return;
      if (options.enablePagination && page.value !== 1) return;
      const exists = notifications.value.some((item) => item.id === payload.id);
      if (exists) return;
      notifications.value.unshift(
        toListItem({
          id: payload.id,
          type: payload.type,
          title: payload.title,
          content: payload.content,
          refId: payload.refId,
          isRead: payload.isRead,
          createTime: payload.createTime
        })
      );
      total.value += 1;
    });
  }

  onMounted(() => {
    setupRealtime();
  });

  onUnmounted(() => {
    wsUnsub?.();
  });

  return {
    loading,
    activeCategory,
    notifications,
    page,
    pageSize,
    total,
    unreadCount,
    emptyText,
    detailVisible,
    currentDetail,
    fetchList,
    changeCategory,
    onPageChange,
    onSizeChange,
    handleItemClick,
    openDetail,
    closeDetail,
    markItemRead,
    handleReadAll,
    handleDeleteItem,
    handleClearAll
  };
}

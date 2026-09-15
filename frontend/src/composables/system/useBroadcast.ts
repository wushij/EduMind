import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { User, School, Reading, Key } from '@element-plus/icons-vue';
import {
  listBroadcasts,
  getBroadcastStats,
  deleteBroadcast,
  clearAllBroadcasts,
  createBroadcast,
  estimateBroadcastAudience,
  getBroadcastRecipients
} from '@/api/notification/broadcast';
import type {
  BroadcastCreateRequest,
  BroadcastStatsVO,
  NotificationBroadcastVO
} from '@/types/notification/broadcast';
import { useAuthStore } from '@/stores/auth/auth';

export function priorityLabel(p: number) {
  if (p === 2) return '紧急公告';
  if (p === 1) return '重要弹窗';
  return '普通广播';
}

export function priorityTagType(p: number): 'danger' | 'warning' | 'info' {
  if (p === 2) return 'danger';
  if (p === 1) return 'warning';
  return 'info';
}

export function audienceLabel(type: string, payload?: string) {
  if (type === 'all') return '全体用户';
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  };
  return map[payload || ''] || '指定角色';
}

export function audienceTagType(
  type: string,
  payload?: string
): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'all') return 'primary';
  if (payload === 'TEACHER') return 'success';
  if (payload === 'STUDENT') return 'warning';
  return 'info';
}

export function audienceIcon(type: string, payload?: string) {
  if (type === 'all') return User;
  if (payload === 'TEACHER') return School;
  if (payload === 'STUDENT') return Reading;
  return Key;
}

export function calcPercent(read?: number, total?: number) {
  if (!total || total <= 0) return 0;
  return Math.min(Math.round(((read || 0) / total) * 100), 100);
}

export function getReachProgressColor(read?: number, total?: number) {
  const pct = calcPercent(read, total);
  if (pct >= 80) return '#10b981';
  if (pct >= 40) return '#3b82f6';
  return '#f59e0b';
}

export function filterBroadcasts(
  list: NotificationBroadcastVO[],
  searchKeyword: string,
  priorityFilter: number | ''
): NotificationBroadcastVO[] {
  let result = list;
  if (searchKeyword.trim()) {
    const kw = searchKeyword.trim().toLowerCase();
    result = result.filter(
      (item) =>
        (item.title && item.title.toLowerCase().includes(kw)) ||
        (item.content && item.content.toLowerCase().includes(kw)) ||
        (item.senderName && item.senderName.toLowerCase().includes(kw))
    );
  }
  if (priorityFilter !== '') {
    result = result.filter((item) => (item.priority ?? 0) === Number(priorityFilter));
  }
  return result;
}

export function resolveSenderAvatar(
  row: NotificationBroadcastVO,
  currentUser?: { id?: number; username?: string; avatar?: string } | null
): string {
  if (row.senderAvatar) {
    return row.senderAvatar;
  }
  if (
    currentUser &&
    (row.senderId === currentUser.id ||
      (row.senderName && row.senderName === currentUser.username))
  ) {
    return currentUser.avatar || '';
  }
  return '';
}

export async function submitBroadcast(data: BroadcastCreateRequest) {
  return createBroadcast(data);
}

export async function estimateAudience(params: {
  targetType: string;
  targetPayload?: string;
}) {
  return estimateBroadcastAudience(params);
}

export async function fetchBroadcastRecipients(
  broadcastId: number,
  params: {
    isRead?: number;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }
) {
  return getBroadcastRecipients(broadcastId, params);
}

export function useBroadcast() {
  const authStore = useAuthStore();

  const loading = ref(false);
  const rawTableData = ref<NotificationBroadcastVO[]>([]);
  const searchKeyword = ref('');
  const targetTypeFilter = ref('');
  const priorityFilter = ref<number | ''>('');
  const drawerVisible = ref(false);
  const detailVisible = ref(false);
  const recipientsVisible = ref(false);
  const currentBroadcast = ref<NotificationBroadcastVO | null>(null);

  const query = reactive({
    pageNum: 1,
    pageSize: 10
  });

  const stats = ref<BroadcastStatsVO>({
    totalBroadcasts: 0,
    totalReach: 0,
    totalRead: 0,
    avgReadRate: 0
  });

  const filteredTableData = computed(() =>
    filterBroadcasts(rawTableData.value, searchKeyword.value, priorityFilter.value)
  );

  const paginatedData = computed(() => {
    const start = (query.pageNum - 1) * query.pageSize;
    return filteredTableData.value.slice(start, start + query.pageSize);
  });

  async function loadStats() {
    try {
      const res = await getBroadcastStats();
      if (res.data) stats.value = res.data;
    } catch {
      // ignore
    }
  }

  async function fetchData() {
    loading.value = true;
    try {
      const [res] = await Promise.all([
        listBroadcasts({
          page: 1,
          pageSize: 100,
          targetType: targetTypeFilter.value || undefined
        }),
        new Promise((resolve) => setTimeout(resolve, 220))
      ]);
      rawTableData.value = res.data?.list || [];
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    query.pageNum = 1;
    fetchData();
  }

  function handleReset() {
    searchKeyword.value = '';
    targetTypeFilter.value = '';
    priorityFilter.value = '';
    query.pageNum = 1;
    query.pageSize = 10;
    fetchData();
  }

  function handleSizeChange(size: number) {
    query.pageSize = size;
    query.pageNum = 1;
  }

  function handlePageChange(page: number) {
    query.pageNum = page;
  }

  function onSent() {
    loadStats();
    handleSearch();
  }

  function openDetail(row: NotificationBroadcastVO) {
    currentBroadcast.value = row;
    detailVisible.value = true;
  }

  function openRecipients(row: NotificationBroadcastVO | null) {
    if (!row) return;
    currentBroadcast.value = row;
    recipientsVisible.value = true;
  }

  function resolveSenderAvatarForRow(row: NotificationBroadcastVO) {
    return resolveSenderAvatar(row, authStore.currentUser);
  }

  async function handleDelete(row: NotificationBroadcastVO) {
    try {
      await ElMessageBox.confirm(`确定删除广播「${row.title}」？关联用户通知将同步移除。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      });
      await deleteBroadcast(row.id);
      ElMessage.success('已成功删除广播');
      fetchData();
      loadStats();
    } catch {
      // cancel
    }
  }

  async function handleClear() {
    try {
      await ElMessageBox.confirm('确定清空全部历史广播记录？此操作不可恢复。', '高危操作确认', {
        type: 'warning',
        confirmButtonText: '确认清空',
        cancelButtonText: '取消'
      });
      await clearAllBroadcasts();
      ElMessage.success('已清空全部广播记录');
      fetchData();
      loadStats();
    } catch {
      // cancel
    }
  }

  onMounted(() => {
    loadStats();
    fetchData();
  });

  return {
    authStore,
    loading,
    rawTableData,
    searchKeyword,
    targetTypeFilter,
    priorityFilter,
    drawerVisible,
    detailVisible,
    recipientsVisible,
    currentBroadcast,
    query,
    stats,
    filteredTableData,
    paginatedData,
    loadStats,
    fetchData,
    handleSearch,
    handleReset,
    handleSizeChange,
    handlePageChange,
    onSent,
    openDetail,
    openRecipients,
    priorityLabel,
    priorityTagType,
    audienceLabel,
    audienceTagType,
    audienceIcon,
    calcPercent,
    getReachProgressColor,
    resolveSenderAvatar: resolveSenderAvatarForRow,
    handleDelete,
    handleClear
  };
}

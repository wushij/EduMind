<template>
  <div class="notification-panel" :class="[`is-${variant}`]">
    <div v-if="showHeader" class="notify-panel-head" :class="{ 'is-compact': variant === 'drawer' }">
      <div v-if="variant === 'page'">
        <h2 class="notify-panel-title">
          <el-icon><Bell /></el-icon>
          消息通知
          <span v-if="unreadCount > 0" class="notify-unread-pill">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </h2>
        <p class="notify-panel-subtitle">
          查看系统、教学、知识库与 AI 相关的全部通知
        </p>
      </div>
      <div v-else-if="unreadCount > 0" class="drawer-unread-tip">
        {{ unreadCount }} 条未读消息
      </div>
      <div class="notify-head-actions">
        <button
          type="button"
          class="notify-head-btn is-primary"
          :disabled="unreadCount === 0"
          @click="handleReadAll"
        >
          全部已读
        </button>
        <button
          type="button"
          class="notify-head-btn is-danger"
          :disabled="total === 0 && notifications.length === 0"
          @click="handleClearAll"
        >
          清空通知
        </button>
        <el-button v-if="variant === 'drawer'" link type="primary" @click="emit('view-all')">
          查看全部
        </el-button>
      </div>
    </div>

    <div class="notify-filter-bar">
      <button
        v-for="cat in NOTIFY_CATEGORIES"
        :key="cat.key"
        type="button"
        class="notify-filter-tab"
        :class="{ active: activeCategory === cat.key }"
        @click="changeCategory(cat.key)"
      >
        {{ cat.label }}
      </button>
    </div>

    <div v-loading="loading" class="notify-card-list">
      <el-empty
        v-if="!loading && notifications.length === 0"
        :description="emptyText"
        :image-size="88"
      />
      <article
        v-for="item in notifications"
        :key="item.id"
        class="notify-card-item"
        :class="{ unread: !item.read }"
        @click="handleItemClick(item)"
      >
        <div class="notify-card-top">
          <div class="notify-card-title-group">
            <span class="notify-type-badge" :class="item.typeClass">{{ item.typeLabel }}</span>
            <span class="notify-card-title">{{ item.title }}</span>
          </div>
          <div class="notify-card-right">
            <span class="notify-card-time">{{ item.time }}</span>
            <button type="button" class="notify-action-btn" @click.stop="handleDeleteItem(item)">
              删除
            </button>
          </div>
        </div>
        <p class="notify-card-content">{{ item.content }}</p>
        <div v-if="item.navigatePath" class="notify-card-footer">
          <span class="notify-link-hint">点击查看详情 →</span>
        </div>
      </article>
    </div>

    <div v-if="showPagination && total > 0" class="notify-pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[8, 10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <NotificationDetailDialog
      v-model="detailVisible"
      :item="currentDetail"
      @close="closeDetail"
      @navigate="onDetailNavigate"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Bell } from '@element-plus/icons-vue';
import { NOTIFY_CATEGORIES } from '@/utils/notification/notify-category';
import { useNotificationList } from '@/composables/notification/useNotificationList';
import NotificationDetailDialog from './NotificationDetailDialog.vue';
import type { NotifyListItem } from '@/composables/notification/useNotificationList';

const props = withDefaults(
  defineProps<{
    variant?: 'page' | 'drawer';
    showHeader?: boolean;
    showPagination?: boolean;
    pageSize?: number;
    enableRealtime?: boolean;
    realtimeActive?: () => boolean;
    autoFetch?: boolean;
  }>(),
  {
    variant: 'page',
    showHeader: true,
    showPagination: true,
    pageSize: 20,
    enableRealtime: true,
    autoFetch: true
  }
);

const emit = defineEmits<{
  'view-all': [];
}>();

const router = useRouter();

const {
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
  closeDetail,
  handleReadAll,
  handleDeleteItem,
  handleClearAll
} = useNotificationList({
  pageSize: props.pageSize,
  enablePagination: props.showPagination,
  enableRealtime: props.enableRealtime,
  realtimeActive: props.realtimeActive
});

async function onDetailNavigate(item: NotifyListItem) {
  closeDetail();
  if (item.navigatePath) {
    await router.push(item.navigatePath);
  }
}

onMounted(() => {
  if (props.autoFetch) {
    void fetchList();
  }
});

defineExpose({ refresh: fetchList });
</script>

<style scoped lang="scss">
.notification-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &.is-page {
    padding: 4px 0;
  }

  &.is-drawer {
    gap: 12px;
  }
}

.notify-card-footer {
  margin-top: 8px;
}

.notify-panel-head.is-compact {
  margin-bottom: 0;
}

.drawer-unread-tip {
  font-size: 13px;
  font-weight: 600;
  color: #ef4444;
}
</style>

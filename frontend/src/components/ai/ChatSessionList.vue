<template>
  <aside class="chat-session-list">
    <!-- 头部新建按钮区 -->
    <div class="session-list-header">
      <button type="button" class="capsule-new-chat-btn" @click="$emit('create')">
        <el-icon class="btn-icon"><Plus /></el-icon>
        <span>新建问答会话</span>
      </button>
      <button
        v-if="showClearAll && sessions.length > 0"
        type="button"
        class="capsule-clear-all-btn"
        @click="handleClearAll"
      >
        清空全部历史
      </button>
    </div>

    <!-- 搜索筛选 -->
    <div class="session-search-box">
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索历史对话..."
        class="capsule-search-input"
      />
    </div>

    <!-- 会话列表 -->
    <div class="session-items-scroll">
      <div
        v-for="item in filteredSessions"
        :key="item.id"
        class="session-item"
        :class="{ active: item.id === currentId }"
        @click="$emit('select', item.id)"
      >
        <div class="session-main">
          <div class="session-title-line">
            <el-icon class="session-icon"><ChatDotRound /></el-icon>
            <span class="session-title" :title="item.title">{{ item.title }}</span>
          </div>
          <div class="session-time">{{ item.updatedAt }}</div>
        </div>

        <button
          v-if="filteredSessions.length > 1"
          type="button"
          class="session-del-btn"
          title="删除会话"
          @click.stop="handleDeleteSession(item)"
        >
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredSessions.length === 0" class="empty-sessions">
        <el-icon class="empty-icon"><Search /></el-icon>
        <span class="empty-text">未找到匹配会话</span>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, ChatDotRound, Close, Search } from '@element-plus/icons-vue';
import type { ChatSession } from '@/composables/ai/useAIStream';

const props = withDefaults(
  defineProps<{
    sessions: ChatSession[];
    currentId: string;
    showClearAll?: boolean;
  }>(),
  {
    showClearAll: false
  }
);

const emit = defineEmits<{
  (e: 'select', id: string): void;
  (e: 'create'): void;
  (e: 'delete', id: string): void;
  (e: 'clear-all'): void;
}>();

function handleClearAll() {
  ElMessageBox.confirm(
    '确定要清空全部问答历史会话吗？清空后记录将无法恢复。',
    '清空全部历史记录',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      lockScroll: false
    }
  )
    .then(() => emit('clear-all'))
    .catch(() => {});
}

function handleDeleteSession(item: ChatSession) {
  ElMessageBox.confirm(
    `确定要删除会话「${item.title || '当前会话'}」吗？删除后该会话全部问答记录将不可恢复。`,
    '删除会话二次确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      lockScroll: false
    }
  )
    .then(() => {
      emit('delete', item.id);
      ElMessage.success('已删除会话');
    })
    .catch(() => {});
}

const keyword = ref('');

const filteredSessions = computed(() => {
  if (!keyword.value.trim()) return props.sessions;
  const kw = keyword.value.trim().toLowerCase();
  return props.sessions.filter((s) => s.title.toLowerCase().includes(kw));
});
</script>

<style scoped lang="scss">
.chat-session-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 260px;
  height: 100%;
  box-sizing: border-box;
  background: #F8FAFC;
  flex-shrink: 0;

  .session-list-header {
    padding: 16px 14px 10px;
    display: flex;
    flex-direction: row;
    align-items: stretch;
    gap: 8px;

    .capsule-new-chat-btn {
      flex: 1;
      min-width: 0;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      border-radius: 9999px; // 长圆药丸
      border: 1px dashed #93C5FD;
      background: #EFF6FF;
      color: #1677FF;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.22s ease;

      .btn-icon {
        font-size: 16px;
        line-height: 1;
      }

      &:hover {
        background: #1677FF;
        color: #FFFFFF;
        border-style: solid;
        border-color: #1677FF;
        box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);
      }
    }

    .capsule-clear-all-btn {
      flex-shrink: 0;
      height: 36px;
      padding: 0 12px;
      border-radius: 9999px;
      border: 1px solid #fecaca;
      background: #fff;
      color: #dc2626;
      font-size: 12px;
      font-weight: 600;
      white-space: nowrap;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #fef2f2;
        border-color: #f87171;
      }
    }
  }

  .session-search-box {
    padding: 0 14px 10px;

    .capsule-search-input {
      width: 100%;
      height: 32px;
      padding: 0 14px;
      border-radius: 9999px; // 长圆跑道
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      font-size: 12px;
      color: #1E293B;
      outline: none;
      transition: all 0.2s;

      &::placeholder {
        color: #94A3B8;
      }

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
      }
    }
  }

  .session-items-scroll {
    flex: 1;
    overflow-y: auto;
    padding: 4px 10px 14px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .session-item {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.18s ease;

      .session-main {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 4px;

        .session-title-line {
          display: flex;
          align-items: center;
          gap: 6px;

          .session-icon {
            font-size: 14px;
            color: #64748B;
            display: inline-flex;
            align-items: center;
          }

          .session-title {
            font-size: 12.5px;
            font-weight: 500;
            color: #334155;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }

        .session-time {
          font-size: 11px;
          color: #94A3B8;
          padding-left: 20px;
        }
      }

      .session-del-btn {
        display: none;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        border: none;
        background: #F1F5F9;
        color: #94A3B8;
        font-size: 11px;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.15s;

        &:hover {
          background: #FEE2E2;
          color: #EF4444;
        }
      }

      &:hover {
        background: #F1F5F9;

        .session-del-btn {
          display: flex;
        }
      }

      &.active {
        background: #FFFFFF;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

        .session-main .session-title-line {
          .session-icon {
            color: #1677FF;
          }

          .session-title {
            color: #1677FF;
            font-weight: 600;
          }
        }

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 10px;
          bottom: 10px;
          width: 3px;
          border-radius: 0 4px 4px 0;
          background: #1677FF;
        }
      }
    }

    .empty-sessions {
      padding: 30px 10px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;

      .empty-icon {
        font-size: 26px;
        color: #94A3B8;
      }

      .empty-text {
        font-size: 12px;
        color: #94A3B8;
      }
    }
  }
}
</style>

<template>
  <div class="members-toolbar-panel">
    <div class="pill-filter-tabs">
      <button
        v-for="tab in roleTabs"
        :key="tab.value"
        type="button"
        class="pill-filter-btn"
        :class="{ active: currentRoleTab === tab.value }"
        @click="emit('update:currentRoleTab', tab.value)"
      >
        <span>{{ tab.label }}</span>
        <span class="tab-count-bubble">{{ tab.count }}</span>
      </button>
    </div>

    <div class="toolbar-right-actions">
      <div class="capsule-search-container">
        <span class="search-icon">
          <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
        </span>
        <input
          :value="searchKeyword"
          type="text"
          class="capsule-input-native"
          placeholder="搜索姓名、学号或用户名..."
          @input="emit('update:searchKeyword', ($event.target as HTMLInputElement).value)"
        />
        <el-icon v-if="searchKeyword" class="clear-btn" @click="emit('update:searchKeyword', '')"><CircleClose /></el-icon>
      </div>

      <button
        type="button"
        class="capsule-primary-btn"
        @click="emit('add')"
      >
        <el-icon><Plus /></el-icon>
        <span>添加选课成员</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, CircleClose } from '@element-plus/icons-vue';

defineProps<{
  roleTabs: Array<{ label: string; value: string; count: number }>;
  currentRoleTab: string;
  searchKeyword: string;
}>();

const emit = defineEmits<{
  'update:currentRoleTab': [value: string];
  'update:searchKeyword': [value: string];
  add: [];
}>();
</script>

<style scoped lang="scss">
.members-toolbar-panel {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 12px 20px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 2px 12px rgba(30, 80, 150, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;

  .pill-filter-tabs {
    display: flex;
    align-items: center;
    gap: 6px;
    background: #F1F5F9;
    padding: 4px;
    border-radius: 9999px;

    .pill-filter-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 32px;
      padding: 0 14px;
      border-radius: 9999px;
      border: none;
      background: transparent;
      color: #64748B;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      .tab-count-bubble {
        font-size: 11px;
        padding: 1px 6px;
        border-radius: 9999px;
        background: rgba(148, 163, 184, 0.2);
        color: #475569;
      }

      &.active {
        background: #FFFFFF;
        color: #1677FF;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

        .tab-count-bubble {
          background: #EAF3FF;
          color: #1677FF;
        }
      }
    }
  }

  .toolbar-right-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .capsule-search-container {
      display: flex;
      align-items: center;
      width: 260px;
      height: 38px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 9999px;
      padding: 0 14px;
      box-sizing: border-box;
      transition: all 0.2s ease;

      &:focus-within {
        border-color: #1677FF;
        box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.15);
      }

      .search-icon {
        display: flex;
        align-items: center;
        color: #94A3B8;
        margin-right: 6px;

        .svg-icon {
          width: 15px;
          height: 15px;
        }
      }

      .capsule-input-native {
        flex: 1;
        height: 100%;
        border: none;
        outline: none;
        background: transparent;
        font-size: 13px;
        color: #1E293B;

        &::placeholder {
          color: #94A3B8;
          font-size: 12.5px;
        }
      }

      .clear-btn {
        cursor: pointer;
        color: #94A3B8;
        font-size: 12px;
        &:hover { color: #64748B; }
      }
    }

    .capsule-primary-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 18px;
      border-radius: 9999px;
      background: #1677FF;
      color: #FFFFFF;
      border: none;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
      transition: all 0.2s ease;

      &:hover {
        background: #4096FF;
        transform: translateY(-1px);
      }
    }
  }
}
</style>

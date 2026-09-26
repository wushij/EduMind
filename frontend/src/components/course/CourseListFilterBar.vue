<template>
  <!-- 长圆胶囊筛选与检索工具栏 -->
  <div class="filter-toolbar-panel">
    <!-- 左侧：药丸标签组 (Pill Tabs) -->
    <div class="pill-tabs-nav">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        type="button"
        class="pill-tab-item"
        :class="{ active: currentStatusTab === tab.value }"
        @click="emit('status-tab-change', tab.value)"
      >
        <span>{{ tab.label }}</span>
        <span class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 右侧：长圆跑道搜索框与学期筛选 -->
    <div class="search-filter-row">
      <!-- 学期筛选下拉 (长圆) -->
      <el-select
        :model-value="selectedSemester"
        placeholder="全部学期"
        class="semester-select"
        @update:model-value="handleSemesterChange"
      >
        <el-option label="全部学期" value="ALL" />
        <el-option
          v-for="option in semesterOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <!-- 纯正长圆搜索条 (1:1 继承登录页输入框设计基因) -->
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
          placeholder="搜索课程名称、代号或教师..."
          @input="handleSearchInput"
        />
        <el-icon v-if="searchKeyword" class="clear-btn" @click="emit('clear-keyword')"><CircleClose /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CircleClose } from '@element-plus/icons-vue';
import { buildSemesterOptions } from '@/constants/semester';

/** 学期筛选项：与课程创建 / 编辑同源，按当前时间动态推导 */
const semesterOptions = buildSemesterOptions();

defineProps<{
  statusTabs: Array<{ label: string; value: string; count: number }>;
  currentStatusTab: string;
  selectedSemester: string;
  searchKeyword: string;
}>();

const emit = defineEmits<{
  'status-tab-change': [tabVal: string];
  'filter-change': [];
  'clear-keyword': [];
  'update:selectedSemester': [value: string];
  'update:searchKeyword': [value: string];
}>();

function handleSemesterChange(value: string) {
  emit('update:selectedSemester', value);
  emit('filter-change');
}

function handleSearchInput(event: Event) {
  emit('update:searchKeyword', (event.target as HTMLInputElement).value);
  emit('filter-change');
}
</script>

<style scoped lang="scss">
// 2. 长圆筛选与检索面板
.filter-toolbar-panel {
  background: #FFFFFF;
  border-radius: 18px;
  padding: 10px 16px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;

  // 药丸标签组
  .pill-tabs-nav {
    display: flex;
    align-items: center;
    gap: 6px;
    background: #F1F5F9;
    padding: 4px;
    border-radius: 9999px; // 长圆外框

    .pill-tab-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 32px;
      padding: 0 14px;
      border-radius: 9999px; // 药丸内按钮
      border: none;
      background: transparent;
      color: #64748B;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      .tab-count {
        font-size: 11px;
        padding: 1px 6px;
        border-radius: 9999px;
        background: rgba(148, 163, 184, 0.2);
        color: #475569;
      }

      &:hover {
        color: #1E293B;
      }

      &.active {
        background: #FFFFFF;
        color: #1677FF;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

        .tab-count {
          background: #EAF3FF;
          color: #1677FF;
        }
      }
    }
  }

  .search-filter-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .semester-select {
      width: 150px;
    }

    // 纯正长圆跑道输入框 (对齐 Login.vue)
    .capsule-search-container {
      display: flex;
      align-items: center;
      width: 320px;
      height: 40px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 9999px; // 纯正长圆胶囊
      padding: 0 16px;
      box-sizing: border-box;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        border-color: #CBD5E1;
      }

      &:focus-within {
        border-color: #1677FF;
        box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.16);
      }

      .search-icon {
        display: flex;
        align-items: center;
        color: #94A3B8;
        margin-right: 8px;

        .svg-icon {
          width: 16px;
          height: 16px;
        }
      }

      .capsule-input-native {
        flex: 1;
        height: 100%;
        border: none;
        outline: none;
        background: transparent;
        font-size: 13.5px;
        color: #1E293B;

        &::placeholder {
          color: #94A3B8;
          font-size: 13px;
        }
      }

      .clear-btn {
        cursor: pointer;
        color: #94A3B8;
        font-size: 12px;
        padding: 2px 4px;
        &:hover { color: #64748B; }
      }
    }
  }
}

// 响应式
@media (max-width: 860px) {
  .filter-toolbar-panel {
    flex-direction: column;
    align-items: stretch;

    .search-filter-row {
      flex-direction: column;
      align-items: stretch;

      .semester-select,
      .capsule-search-container {
        width: 100%;
      }
    }
  }
}
</style>

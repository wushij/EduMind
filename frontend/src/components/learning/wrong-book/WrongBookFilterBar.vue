<template>
  <div class="filter-card">
    <div class="filter-left">
      <!-- 自定义胶囊筛选组：避免 Element Plus el-radio-button 分段控件的首尾连体圆角无法对齐 -->
      <div class="type-pill-group" role="tablist" aria-label="错因类型筛选">
        <button
          v-for="opt in WRONG_ERROR_TYPE_OPTIONS"
          :key="opt.code || 'all'"
          type="button"
          role="tab"
          class="type-pill"
          :class="{ 'type-pill--active': selectedErrorType === opt.code }"
          :aria-selected="selectedErrorType === opt.code"
          @click="emit('update:selectedErrorType', opt.code)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div class="filter-right">
      <!-- 与题库列表一致的批量展开/收起 -->
      <div v-if="showExpandActions" class="list-expand-actions">
        <button
          type="button"
          class="list-expand-toggle-btn"
          :title="bodyExpanded ? '收起选项与参考答案' : '展开选项与参考答案'"
          @click="emit('toggle-body')"
        >
          <el-icon><component :is="bodyExpanded ? Fold : Expand" /></el-icon>
          <span>{{ bodyExpanded ? '收起题目' : '展开题目' }}</span>
        </button>
        <button
          type="button"
          class="list-expand-toggle-btn list-expand-toggle-btn--full"
          title="同时展开或收起全部题目的选项与解析"
          @click="emit('toggle-details')"
        >
          <el-icon><component :is="fullExpanded ? Fold : Expand" /></el-icon>
          <span>{{ fullExpanded ? '收起题目和解析' : '展开题目和解析' }}</span>
        </button>
      </div>

      <el-button
        type="primary"
        plain
        round
        class="batch-practice-btn"
        title="按当前错因筛选结果生成一组变式攻坚练习"
        @click="emit('batch-practice')"
      >
        <el-icon><Lightning /></el-icon>
        <span>发起变式攻坚</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Expand, Fold, Lightning } from '@element-plus/icons-vue';
import { WRONG_ERROR_TYPE_OPTIONS } from '@/types/learning/wrong-question';

withDefaults(
  defineProps<{
    selectedErrorType: string;
    /** 是否展示批量展开按钮（列表有数据时才展示） */
    showExpandActions?: boolean;
    /** 当前列表是否已批量展开题目 */
    bodyExpanded?: boolean;
    /** 题目与解析是否均已展开 */
    fullExpanded?: boolean;
  }>(),
  {
    showExpandActions: false,
    bodyExpanded: false,
    fullExpanded: false
  }
);

const emit = defineEmits<{
  'update:selectedErrorType': [value: string];
  'toggle-body': [];
  'toggle-details': [];
  'batch-practice': [];
}>();
</script>

<style scoped lang="scss">
.filter-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 18px;
  background: #fff;
  border: 1px solid #e8eef7;
  border-radius: 22px;
  box-shadow: 0 6px 20px rgba(30, 80, 150, 0.04);

  .filter-left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    min-width: 0;
  }

  .filter-right {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-left: auto;
  }

  .batch-practice-btn {
    font-weight: 600;
    padding: 0 16px;
  }
}

/* 宽屏下强制同排：筛选胶囊 + 批量展开 + 批量练习一行放得下 */
@media (min-width: 1200px) {
  .filter-card {
    flex-wrap: nowrap;

    .filter-left,
    .filter-right {
      flex-wrap: nowrap;
    }
  }
}

.type-pill-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.type-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 15px;
  border-radius: 9999px;
  border: 1px solid #e8eef7;
  background: #fff;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: #bfdbfe;
    color: #1677ff;
    background: #f5faff;
  }

  &:focus-visible {
    box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.18);
  }

  &--active {
    border-color: transparent;
    color: #fff;
    background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
    box-shadow: 0 4px 14px rgba(22, 119, 255, 0.26);

    &:hover {
      color: #fff;
      background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
    }
  }
}

.list-expand-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.list-expand-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 15px;
  border-radius: 9999px;
  border: 1px solid #e8eef7;
  background: #fff;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;

  :deep(.el-icon) {
    font-size: 14px;
  }

  &:hover {
    border-color: #bfdbfe;
    color: #1677ff;
    background: #f5faff;
  }

  &:focus-visible {
    box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.18);
  }

  &--full {
    border-color: #bbf7d0;
    background: #f3fdf6;
    color: #15803d;

    &:hover {
      border-color: #86efac;
      background: #eafaf0;
      color: #15803d;
    }
  }
}
</style>

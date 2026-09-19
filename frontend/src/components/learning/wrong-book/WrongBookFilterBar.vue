<template>
  <div class="filter-card">
    <div class="filter-left">
      <el-radio-group
        :model-value="selectedErrorType"
        @update:model-value="emit('update:selectedErrorType', $event)"
      >
        <el-radio-button
          v-for="opt in WRONG_ERROR_TYPE_OPTIONS"
          :key="opt.code || 'all'"
          :label="opt.code"
        >
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
    </div>
    <div class="filter-right">
      <el-button type="primary" plain class="batch-practice-btn" @click="emit('batch-practice')">
        <el-icon><Lightning /></el-icon>
        <span>一键发起错题变式攻坚练习</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lightning } from '@element-plus/icons-vue';
import { WRONG_ERROR_TYPE_OPTIONS } from '@/types/learning/wrong-question';

defineProps<{
  selectedErrorType: string;
}>();

const emit = defineEmits<{
  'update:selectedErrorType': [value: string];
  'batch-practice': [];
}>();
</script>

<style scoped lang="scss">
.filter-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 10px rgba(30, 80, 150, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;

  .filter-left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }

  .batch-practice-btn {
    border-radius: 9999px;
    font-weight: 600;
  }
}
</style>

<template>
  <div v-if="total > 0" class="pagination-bar">
    <el-pagination
      v-model:current-page="innerPageNum"
      v-model:page-size="innerPageSize"
      :page-sizes="pageSizes"
      :layout="layout"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    total: number;
    pageNum: number;
    pageSize: number;
    pageSizes?: number[];
    layout?: string;
  }>(),
  {
    pageSizes: () => [10, 20, 50],
    layout: 'total, sizes, prev, pager, next'
  }
);

const emit = defineEmits<{
  (e: 'update:pageNum', value: number): void;
  (e: 'update:pageSize', value: number): void;
  (e: 'change'): void;
}>();

const innerPageNum = computed({
  get: () => props.pageNum,
  set: (value: number) => emit('update:pageNum', value)
});

const innerPageSize = computed({
  get: () => props.pageSize,
  set: (value: number) => emit('update:pageSize', value)
});

function handleSizeChange() {
  emit('update:pageNum', 1);
  emit('change');
}

function handlePageChange() {
  emit('change');
}
</script>

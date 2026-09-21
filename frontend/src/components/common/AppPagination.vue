<template>
  <div v-if="safeTotal > 0" class="pagination-bar">
    <el-pagination
      v-model:current-page="innerPageNum"
      v-model:page-size="innerPageSize"
      :page-sizes="pageSizes"
      :layout="layout"
      :total="safeTotal"
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

/**
 * 后端 Long 字段会以字符串形式返回（如 total: "2"）。
 * el-pagination 内部通过 typeof value !== 'number' 判定 total 是否缺失，
 * 一旦收到字符串会直接渲染 null，表现为「分页容器是空白卡片」。
 * 因此这里统一做强类型归一化，保证分页在各模块表现一致。
 */
const safeTotal = computed(() => {
  const value = Number(props.total);
  return Number.isFinite(value) && value > 0 ? value : 0;
});

const safePageNum = computed(() => {
  const value = Number(props.pageNum);
  return Number.isFinite(value) && value > 0 ? value : 1;
});

const safePageSize = computed(() => {
  const value = Number(props.pageSize);
  return Number.isFinite(value) && value > 0 ? value : 10;
});

const innerPageNum = computed({
  get: () => safePageNum.value,
  set: (value: number) => emit('update:pageNum', value)
});

const innerPageSize = computed({
  get: () => safePageSize.value,
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

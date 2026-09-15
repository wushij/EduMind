<template>
  <el-dialog
    :model-value="visible"
    width="480px"
    append-to-body
    class="gb-dialog-shell"
    :close-on-click-modal="false"
    @update:model-value="onVisibleChange"
  >
    <template #header>
      <span class="dimension-warn-title">向量维度变更警告</span>
    </template>
    <p class="dimension-warn-body">
      存量知识切片向量不可跨维度比较，切换默认 Embedding 模型维度后需执行「全量切片同步与重建」，否则检索召回可能异常。
    </p>
    <template #footer>
      <el-button @click="onVisibleChange(false)">取消</el-button>
      <el-button type="primary" @click="onConfirm">仍要切换</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
  onVisibleChange: (visible: boolean) => void;
  onConfirm: () => void;
}>();
</script>

<style scoped lang="scss">
.dimension-warn-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.dimension-warn-body {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
}
</style>

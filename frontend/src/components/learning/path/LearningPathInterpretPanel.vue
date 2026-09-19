<template>
  <el-collapse v-if="hint" class="path-interpret-collapse">
    <el-collapse-item title="路径解读与导学说明" name="interpret">
      <p class="interpret-text">{{ hint }}</p>
      <p v-if="generatedAt" class="interpret-meta">生成时间：{{ generatedAt }}</p>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup lang="ts">
defineProps<{
  hint?: string;
  generatedAt?: string;
}>();
</script>

<style scoped lang="scss">
.path-interpret-collapse {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 4px 16px;

  .interpret-text {
    margin: 0 0 8px;
    font-size: 13px;
    color: #334155;
    line-height: 1.6;
  }

  .interpret-meta {
    margin: 0;
    font-size: 12px;
    color: #94a3b8;
  }
}
</style>

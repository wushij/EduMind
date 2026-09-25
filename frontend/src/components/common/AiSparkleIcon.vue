<template>
  <svg
    class="ai-sparkle-icon"
    viewBox="0 0 24 24"
    fill="currentColor"
    aria-hidden="true"
    focusable="false"
  >
    <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
  </svg>
</template>

<script setup lang="ts">
/**
 * AI 星芒图标（四角星 / sparkle）
 * 全站 AI 能力标识统一使用该图标，替代语义不准的魔法棒 MagicStick。
 * 尺寸 1em、颜色继承 currentColor，可直接嵌入 el-icon 或按钮文本流中。
 */
defineOptions({ name: 'AiSparkleIcon' });
</script>

<style scoped>
.ai-sparkle-icon {
  display: inline-block;
  width: 1em;
  height: 1em;
  vertical-align: -0.125em;
  flex-shrink: 0;
}
</style>

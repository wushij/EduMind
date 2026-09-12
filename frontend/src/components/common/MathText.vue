<template>
  <component :is="tag" class="math-text" :class="customClass" v-html="html" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { renderMathText } from '@/utils/format/render-math';

const props = withDefaults(
  defineProps<{
    text?: string;
    tag?: string;
    customClass?: string;
  }>(),
  {
    text: '',
    tag: 'span',
    customClass: ''
  }
);

const html = computed(() => renderMathText(props.text || ''));
</script>

<style lang="scss">
.math-text {
  line-height: 1.6;
  word-break: break-word;

  .katex {
    font-size: 1.05em;
  }

  .math-block {
    display: block;
    margin: 0.45em 0;
    overflow-x: auto;
  }
}
</style>

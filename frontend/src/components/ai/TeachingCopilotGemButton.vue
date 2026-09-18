<template>
  <component
    :is="decorative ? 'span' : 'button'"
    :type="decorative ? undefined : 'button'"
    class="teaching-copilot-gem-btn"
    :class="{ 'teaching-copilot-gem-btn--sm': size === 'sm', 'teaching-copilot-gem-btn--decorative': decorative }"
    :title="decorative ? undefined : title"
    :aria-label="decorative ? undefined : title"
    :aria-hidden="decorative ? 'true' : undefined"
    @click="onClick"
  >
    <span class="teaching-copilot-gem-btn__sparkle" aria-hidden="true">✦</span>
  </component>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    title?: string;
    decorative?: boolean;
    size?: 'md' | 'sm';
  }>(),
  {
    title: '打开 AI 教学副驾驶',
    decorative: false,
    size: 'md'
  }
);

const emit = defineEmits<{
  click: [];
}>();

function onClick(event: MouseEvent) {
  if (props.decorative) return;
  event.stopPropagation();
  emit('click');
}
</script>

<style scoped lang="scss">
.teaching-copilot-gem-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 8px;
  border: 1.5px solid rgba(22, 119, 255, 0.35);
  background: linear-gradient(180deg, #f8fbff 0%, #eff6ff 100%);
  color: #1677ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;

  &:hover:not(.teaching-copilot-gem-btn--decorative) {
    border-color: rgba(22, 119, 255, 0.55);
    background: #dbeafe;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.18);
    transform: translateY(-1px);
  }

  &--sm {
    width: 22px;
    height: 22px;
    border-radius: 6px;
    border-width: 1px;

    .teaching-copilot-gem-btn__sparkle {
      font-size: 12px;
    }
  }

  &--decorative {
    cursor: inherit;
    pointer-events: none;
  }

  &__sparkle {
    font-size: 14px;
    line-height: 1;
    font-weight: 700;
  }
}
</style>

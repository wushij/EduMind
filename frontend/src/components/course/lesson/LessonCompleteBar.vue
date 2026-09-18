<template>
  <div class="lesson-complete-bar">
    <button type="button" class="capsule-btn capsule-btn--ai" @click="emit('ai')">
      <span>AI 辅导</span>
    </button>
    <button
      v-if="lessonType === 'QUIZ'"
      type="button"
      class="capsule-btn capsule-btn--quiz"
      @click="emit('quiz')"
    >
      <span>智能自测</span>
    </button>
    <button type="button" class="capsule-btn capsule-btn--primary" @click="emit('complete')">
      <span>{{ completed ? '已学完' : '标记学完' }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  lessonType?: string;
  completed?: boolean;
}>();

const emit = defineEmits<{
  (e: 'complete'): void;
  (e: 'ai'): void;
  (e: 'quiz'): void;
}>();
</script>

<style scoped lang="scss">
.lesson-complete-bar {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: stretch;
  gap: 8px;
  width: 100%;
}

.capsule-btn {
  flex: 1 1 0;
  min-width: 0;
  border: none;
  border-radius: 999px;
  padding: 10px 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease,
    border-color 0.2s ease;

  span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &--primary {
    background: #2563eb;
    color: #fff;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.22);

    &:hover {
      background: #1d4ed8;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.32);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
      box-shadow: 0 1px 4px rgba(37, 99, 235, 0.25);
    }
  }

  &--ai {
    background: #eef2ff;
    color: #4338ca;
    border: 1px solid #c7d2fe;

    &:hover {
      background: #e0e7ff;
      border-color: #a5b4fc;
      color: #3730a3;
      box-shadow: 0 2px 8px rgba(79, 70, 229, 0.12);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
      background: #c7d2fe;
    }
  }

  &--quiz {
    background: #fffbeb;
    color: #b45309;
    border: 1px solid #fde68a;

    &:hover {
      background: #fef3c7;
      border-color: #fcd34d;
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }
  }
}
</style>

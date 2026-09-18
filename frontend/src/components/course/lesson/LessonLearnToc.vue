<template>
  <nav v-if="items.length" class="lesson-learn-toc" aria-label="本课目录">
    <h3 class="toc-title">本课目录</h3>
    <ul class="toc-list">
      <li
        v-for="item in items"
        :key="item.id"
        class="toc-item"
        :class="[`toc-item--level-${item.level}`, { 'is-active': activeId === item.id }]"
      >
        <button type="button" class="toc-link" :title="item.title" @click="scrollTo(item.id)">
          {{ item.title }}
        </button>
      </li>
    </ul>
  </nav>
  <p v-else class="toc-empty">正文暂无小节标题，可在编辑中使用「## 标题」编写目录。</p>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import type { LessonTocItem } from '@/utils/course/lesson-toc';
import { getScrollParent, scrollElementIntoView } from '@/utils/dom/scroll-into-view';

const props = defineProps<{
  items: LessonTocItem[];
}>();

const SCROLL_OFFSET = 96;

const activeId = ref<string | null>(null);
let observer: IntersectionObserver | null = null;

function scrollTo(id: string) {
  const el = document.getElementById(id);
  if (!el) return;
  scrollElementIntoView(el, SCROLL_OFFSET);
  activeId.value = id;
}

function setupObserver() {
  observer?.disconnect();
  observer = null;
  if (!props.items.length) return;

  const ids = props.items.map(i => i.id);
  const first = document.getElementById(ids[0]);
  const scrollRoot = getScrollParent(first);

  observer = new IntersectionObserver(
    entries => {
      const visible = entries
        .filter(e => e.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
      if (visible.length) {
        activeId.value = visible[0].target.id;
      }
    },
    {
      root: scrollRoot === document.documentElement ? null : scrollRoot,
      rootMargin: `-${SCROLL_OFFSET}px 0px -55% 0px`,
      threshold: [0, 0.1, 1]
    }
  );

  for (const id of ids) {
    const node = document.getElementById(id);
    if (node) observer.observe(node);
  }
}

function scheduleObserver() {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => setupObserver());
  });
}

watch(
  () => props.items,
  () => {
    activeId.value = props.items[0]?.id ?? null;
    scheduleObserver();
  },
  { deep: true }
);

onMounted(() => {
  activeId.value = props.items[0]?.id ?? null;
  scheduleObserver();
});

onUnmounted(() => {
  observer?.disconnect();
});
</script>

<style scoped lang="scss">
.lesson-learn-toc {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.toc-title {
  margin: 0 0 10px;
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
}

.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: min(55vh, 400px);
  overflow-y: auto;
  scrollbar-width: thin;
}

.toc-item {
  margin: 0;

  &--level-3 {
    .toc-link {
      padding-left: 14px;
      font-size: 12px;
    }
  }

  &--level-4 {
    .toc-link {
      padding-left: 22px;
      font-size: 11.5px;
      color: #64748b;
    }
  }

  &.is-active .toc-link {
    color: #2563eb;
    background: #eff6ff;
    font-weight: 600;
  }
}

.toc-link {
  display: block;
  width: 100%;
  border: none;
  background: transparent;
  text-align: left;
  padding: 7px 10px;
  border-radius: 8px;
  font-size: 12.5px;
  line-height: 1.35;
  color: #334155;
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &:hover {
    background: #f8fafc;
    color: #1d4ed8;
  }
}

.toc-empty {
  margin: 0 0 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}
</style>

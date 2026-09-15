<template>
  <div class="doc-file-icon" :class="`doc-file-icon--${kind}`" aria-hidden="true">
    <svg v-if="kind === 'pdf'" viewBox="0 0 24 24" class="doc-file-svg" fill="none">
      <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" fill="currentColor" opacity="0.18" />
      <path d="M14 3v5h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M8.5 13h7M8.5 16h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>

    <svg v-else-if="kind === 'word'" viewBox="0 0 24 24" class="doc-file-svg" fill="none">
      <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" fill="currentColor" opacity="0.2" />
      <path d="M14 3v5h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M8 12.5h8M8 15h6.5M8 17.5h7.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>

    <svg v-else-if="kind === 'ppt'" viewBox="0 0 24 24" class="doc-file-svg" fill="none">
      <rect x="4" y="5" width="16" height="11" rx="2" fill="currentColor" opacity="0.18" />
      <path d="M8 16.5h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      <path d="M10 8.5h4v3h-4z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
    </svg>

    <svg v-else-if="kind === 'md'" viewBox="0 0 24 24" class="doc-file-svg" fill="none">
      <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" fill="currentColor" opacity="0.16" />
      <path d="M14 3v5h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M9 13.5 10.8 16 12.6 13.5 14.4 16 16.2 13.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg v-else viewBox="0 0 24 24" class="doc-file-svg" fill="none">
      <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" fill="currentColor" opacity="0.16" />
      <path d="M14 3v5h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M8.5 13h7M8.5 16h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>
  </div>
</template>

<script setup lang="ts">
import type { DocumentFileKind } from '@/utils/knowledge/document';

defineProps<{
  kind: DocumentFileKind;
}>();
</script>

<style scoped lang="scss">
.doc-file-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid transparent;

  .doc-file-svg {
    width: 24px;
    height: 24px;
  }

  &--pdf {
    background: linear-gradient(145deg, #fff1f2 0%, #ffe4e6 100%);
    border-color: #fecdd3;
    color: #e11d48;
  }

  &--word {
    background: linear-gradient(145deg, #eff6ff 0%, #dbeafe 100%);
    border-color: #bfdbfe;
    color: #2563eb;
  }

  &--ppt {
    background: linear-gradient(145deg, #fff7ed 0%, #ffedd5 100%);
    border-color: #fed7aa;
    color: #ea580c;
  }

  &--md {
    background: linear-gradient(145deg, #f8fafc 0%, #e2e8f0 100%);
    border-color: #cbd5e1;
    color: #475569;
  }

  &--default {
    background: linear-gradient(145deg, #f8fafc 0%, #f1f5f9 100%);
    border-color: #e2e8f0;
    color: #64748b;
  }
}
</style>

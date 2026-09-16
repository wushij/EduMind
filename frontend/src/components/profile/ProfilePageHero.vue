<template>
  <div class="profile-page-hero">
    <div class="hero-bg-glow hero-bg-glow--blue" />
    <div class="hero-bg-glow hero-bg-glow--purple" />
    <div class="hero-content">
      <div class="hero-left">
        <span v-if="eyebrow" class="hero-eyebrow">{{ eyebrow }}</span>
        <h2>{{ title }}</h2>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="hero-right">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.footer" class="hero-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title: string;
  subtitle?: string;
}>();
</script>

<style scoped lang="scss">
.profile-page-hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 55%, #f5f3ff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 28px 32px 24px;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);

  .hero-bg-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(60px);
    opacity: 0.35;

    &--blue {
      width: 220px;
      height: 220px;
      background: #bfdbfe;
      top: -80px;
      right: 120px;
    }

    &--purple {
      width: 160px;
      height: 160px;
      background: #ddd6fe;
      bottom: -60px;
      left: 40px;
    }
  }

  .hero-content {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;

    @media (max-width: 768px) {
      flex-direction: column;
    }
  }

  .hero-left {
    min-width: 0;

    .hero-eyebrow {
      display: inline-block;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.04em;
      color: #1677ff;
      background: rgba(22, 119, 255, 0.08);
      border: 1px solid rgba(22, 119, 255, 0.15);
      padding: 4px 14px;
      border-radius: 9999px;
      margin-bottom: 10px;
    }

    h2 {
      margin: 0;
      font-size: 22px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: -0.02em;
    }

    p {
      margin: 6px 0 0;
      font-size: 13.5px;
      color: #64748b;
      line-height: 1.5;
      max-width: 720px;
    }
  }

  .hero-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
    flex-shrink: 0;
    flex-wrap: wrap;
  }

  .hero-footer {
    position: relative;
    margin-top: 22px;
  }
}

:deep(.hero-action-row) {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

:deep(.hero-pill-btn) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 9999px;
  border: 1px solid #bfdbfe;
  background: #ffffff;
  color: #2563eb;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);

  &:hover:not(:disabled) {
    background: #eff6ff;
    border-color: #93c5fd;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  &.is-outline {
    border-color: #cbd5e1;
    color: #334155;
    box-shadow: none;

    &:hover:not(:disabled) {
      border-color: #2563eb;
      color: #2563eb;
      background: #f8fafc;
    }
  }

  &.is-primary {
    border: none;
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    color: #ffffff;
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
    }
  }
}
</style>

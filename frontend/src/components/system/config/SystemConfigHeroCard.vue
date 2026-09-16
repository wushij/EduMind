<template>
  <ProfilePageHero
    subtitle="平台运行参数集中维护与安全管控，修改后请点击底部「保存全部」生效"
  >
    <template #title>
      <div class="hero-title-container">
        <div class="hero-icon-box">
          <el-icon :size="22"><Setting /></el-icon>
        </div>
        <h2 class="hero-heading">系统配置</h2>
      </div>
    </template>

    <template #actions>
      <div class="hero-action-row">
        <el-button
          round
          class="btn-refresh"
          :icon="Refresh"
          :loading="loading"
          @click="emit('refresh')"
        >
          重新加载
        </el-button>
        <span v-if="lastUpdatedText" class="updated-at-pill">
          <el-icon class="pill-clock-icon"><Clock /></el-icon>
          {{ lastUpdatedText }}
        </span>
      </div>

      <div class="hero-quota-badge-row">
        <div class="quota-pill" :class="isDirty ? 'is-warning' : 'is-success'">
          <span class="quota-dot" />
          {{ isDirty ? '存在待保存修改' : '所有配置已同步' }}
        </div>
        <div class="quota-percent-pill">
          已纳管 <strong>12</strong> 个配置分组
        </div>
      </div>
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import { Setting, Refresh, Clock } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';

withDefaults(
  defineProps<{
    loading: boolean;
    isDirty: boolean;
    lastUpdatedText?: string;
  }>(),
  {
    lastUpdatedText: ''
  }
);

const emit = defineEmits<{
  refresh: [];
}>();
</script>

<style scoped lang="scss">
.hero-title-container {
  display: flex;
  align-items: center;
  gap: 12px;

  .hero-icon-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 12px;
    background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
    flex-shrink: 0;
  }

  .hero-heading {
    margin: 0;
    font-size: 22px;
    font-weight: 800;
    color: #0f172a;
    letter-spacing: -0.02em;
  }
}

.hero-action-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;

  .btn-refresh {
    border-radius: 9999px;
    padding: 7px 18px;
    font-weight: 600;
    font-size: 13px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    color: #334155;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    transition: all 0.2s ease;

    &:hover {
      color: #1677ff;
      border-color: #bfdbfe;
      background: #f8faff;
    }
  }

  .updated-at-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 6px 13px;
    border-radius: 9999px;
    background: rgba(255, 255, 255, 0.85);
    border: 1px solid #e2e8f0;
    font-size: 11.5px;
    color: #64748b;
    font-weight: 500;

    .pill-clock-icon {
      font-size: 12px;
      color: #94a3b8;
    }
  }
}

.hero-quota-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.quota-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;

  .quota-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  &.is-success {
    background: #ecfdf5;
    color: #059669;
    border-color: #a7f3d0;

    .quota-dot {
      background: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
    }
  }

  &.is-warning {
    background: #fffbeb;
    color: #d97706;
    border-color: #fde68a;

    .quota-dot {
      background: #f59e0b;
      box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.25);
      animation: pulse-dot 2s infinite ease-in-out;
    }
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.75;
  }
}

.quota-percent-pill {
  padding: 8px 16px;
  border-radius: 9999px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #64748b;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

  strong {
    color: #0f172a;
    font-weight: 700;
    margin: 0 2px;
  }
}
</style>

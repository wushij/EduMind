<template>
  <ProfilePageHero
    title="个人偏好与教学工作台设置"
    subtitle="深度自定义 AI 助教应答范式、LaTeX/代码智能渲染引擎、多模态视觉风格及全站消息通知通道。"
  >
    <template #actions>
      <div class="hero-action-row">
        <el-button
          size="default"
          class="pill-btn-action outline-btn"
          :icon="RefreshLeft"
          :disabled="saving"
          @click="handleReset"
        >
          恢复默认配置
        </el-button>
        <el-button
          type="primary"
          size="default"
          class="pill-btn-action primary-save-btn"
          :icon="Check"
          :loading="saving"
          @click="handleSave"
        >
          保存偏好配置
        </el-button>
      </div>
    </template>
  </ProfilePageHero>
</template>

<script setup lang="ts">
import { Check, RefreshLeft } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';

defineProps<{
  saving: boolean;
  handleReset: () => void;
  handleSave: () => void;
}>();
</script>

<style scoped lang="scss">
.pill-btn-action {
  border-radius: 9999px;
  height: 38px;
  padding: 0 22px;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.25s ease;

  &.primary-save-btn {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    border: none;
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
    }
  }

  &.outline-btn {
    border-color: var(--el-border-color, #cbd5e1);
    color: var(--el-text-color-regular, #475569);
    background: #ffffff;

    &:hover {
      border-color: #94a3b8;
      color: var(--el-text-color-primary, #0f172a);
      background: #f8fafc;
    }
  }
}
</style>

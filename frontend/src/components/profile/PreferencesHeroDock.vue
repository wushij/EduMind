<template>
  <div class="pref-hero-dock">
    <div class="header-left">
      <div class="title-with-icon">
        <div class="header-icon-circle">
          <el-icon class="header-icon"><Setting /></el-icon>
        </div>
        <div class="title-text-group">
          <div class="title-row">
            <h1 class="main-title">个人偏好与教学工作台设置</h1>
            <span class="capsule-tag cloud-tag">
              <el-icon><Check /></el-icon>
              <span>云端全量同步</span>
            </span>
            <span class="capsule-tag live-tag">
              <el-icon><Lightning /></el-icon>
              <span>即时响应生效</span>
            </span>
          </div>
          <p class="sub-desc">
            深度自定义 AI 助教应答范式、LaTeX/代码智能渲染引擎、多模态视觉风格及全站消息通知通道。
          </p>
        </div>
      </div>
    </div>

    <div class="header-right">
      <el-button
        size="default"
        class="pill-btn-action outline-btn"
        :icon="RefreshLeft"
        @click="handleReset"
        :disabled="saving"
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
  </div>
</template>

<script setup lang="ts">
import { Setting, Check, RefreshLeft, Lightning } from '@element-plus/icons-vue';

defineProps<{
  saving: boolean;
  handleReset: () => void;
  handleSave: () => void;
}>();
</script>

<style scoped lang="scss">
.pref-hero-dock {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
  background: var(--el-bg-color, #ffffff);
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 20px;
  padding: 22px 28px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  transition: all 0.3s ease;

  .header-left {
    .title-with-icon {
      display: flex;
      align-items: center;
      gap: 16px;

      .header-icon-circle {
        width: 48px;
        height: 48px;
        border-radius: 14px;
        background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
        flex-shrink: 0;

        .header-icon {
          font-size: 24px;
          color: #ffffff;
        }
      }

      .title-text-group {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .title-row {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;

          .main-title {
            font-size: 20px;
            font-weight: 800;
            color: var(--el-text-color-primary, #0f172a);
            margin: 0;
            line-height: 1.2;
          }

          .capsule-tag {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11.5px;
            border-radius: 9999px;
            padding: 3px 10px;
            font-weight: 600;

            &.cloud-tag {
              background: #eff6ff;
              color: #2563eb;
              border: 1px solid #bfdbfe;
            }

            &.live-tag {
              background: #ecfdf5;
              color: #059669;
              border: 1px solid #a7f3d0;
            }
          }
        }

        .sub-desc {
          margin: 0;
          font-size: 13px;
          color: var(--el-text-color-secondary, #64748b);
          line-height: 1.5;
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

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
        background: var(--el-bg-color, #ffffff);

        &:hover {
          border-color: #94a3b8;
          color: var(--el-text-color-primary, #0f172a);
          background: var(--el-fill-color-light, #f8fafc);
        }
      }
    }
  }
}
</style>

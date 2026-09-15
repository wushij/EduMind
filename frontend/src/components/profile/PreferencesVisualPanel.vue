<template>
  <div class="pref-card">
    <div class="card-header-bar">
      <div class="section-icon-badge visual-badge">
        <el-icon><Brush /></el-icon>
      </div>
      <div class="section-title-wrap">
        <h3 class="card-section-title">视觉主题与显示模式</h3>
        <span class="section-subtitle">个性化定制控制台视觉风格与视窗排版密度</span>
      </div>
      <el-tag size="small" effect="plain" type="primary" class="section-pill-tag">外观交互</el-tag>
    </div>

    <div class="options-grid">
      <!-- 界面外观主题 -->
      <div class="pref-row">
        <div class="row-info">
          <div class="row-title-line">
            <span class="row-title">界面视觉外观</span>
            <el-tag size="small" type="success" effect="light" class="live-status-tag">点击即时生效</el-tag>
          </div>
          <span class="row-desc">选择契合个人审美的明亮浅色、深邃极夜或跟随操作系统昼夜节律自动调度。</span>
        </div>
        <el-radio-group
          v-model="preferenceStore.preferences.theme"
          size="default"
          class="pill-radio-group"
          @change="handleThemeChange"
        >
          <el-radio-button label="LIGHT">
            <span class="opt-btn-inner">
              <el-icon><Sunny /></el-icon>
              <span>明亮浅色</span>
            </span>
          </el-radio-button>
          <el-radio-button label="DARK">
            <span class="opt-btn-inner">
              <el-icon><Moon /></el-icon>
              <span>深邃极夜</span>
            </span>
          </el-radio-button>
          <el-radio-button label="AUTO">
            <span class="opt-btn-inner">
              <el-icon><Monitor /></el-icon>
              <span>跟随系统</span>
            </span>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 紧凑教学模式 -->
      <div class="pref-row">
        <div class="row-info">
          <span class="row-title">紧凑教学工作台布局</span>
          <span class="row-desc">适度精简列表行距与数据卡片留白，在同一屏幕内呈现更多教学指标与批阅内容。</span>
        </div>
        <el-switch v-model="preferenceStore.preferences.compactMode" class="custom-switch" />
      </div>

      <!-- 系统界面语言 -->
      <div class="pref-row">
        <div class="row-info">
          <span class="row-title">系统操作界面语言</span>
          <span class="row-desc">选择平台全站各功能模块的指引与术语显示语言。</span>
        </div>
        <el-select
          v-model="preferenceStore.preferences.language"
          size="default"
          class="pill-select"
          style="width: 190px"
        >
          <el-option label="简体中文 (zh-CN)" value="zh-CN" />
          <el-option label="English (en-US)" value="en-US" />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Brush, Sunny, Moon, Monitor } from '@element-plus/icons-vue';
import type { usePreferenceStore } from '@/stores/user/preference';

defineProps<{
  preferenceStore: ReturnType<typeof usePreferenceStore>;
  handleThemeChange: (val: unknown) => void;
}>();
</script>

<style scoped lang="scss">
.pref-card {
  background: var(--el-bg-color, #ffffff);
  border-radius: 20px;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  padding: 22px 26px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(37, 99, 235, 0.25);
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  }

  .card-header-bar {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--el-border-color-extra-light, #f1f5f9);

    .section-icon-badge {
      width: 38px;
      height: 38px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;

      &.visual-badge {
        background: #ede9fe;
        color: #7c3aed;
      }
    }

    .section-title-wrap {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;

      .card-section-title {
        font-size: 16px;
        font-weight: 700;
        color: var(--el-text-color-primary, #0f172a);
        margin: 0;
      }

      .section-subtitle {
        font-size: 12px;
        color: var(--el-text-color-secondary, #64748b);
      }
    }

    .section-pill-tag {
      border-radius: 9999px;
      font-weight: 600;
      font-size: 11.5px;
      padding: 2px 10px;
    }
  }

  .options-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .pref-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      background: var(--el-fill-color-light, #f8fafc);
      border-radius: 14px;
      border: 1px solid var(--el-border-color-extra-light, #edf2f7);
      gap: 20px;
      flex-wrap: wrap;
      transition: all 0.2s ease;

      &:hover {
        background: var(--el-fill-color, #f1f5f9);
        border-color: var(--el-border-color-lighter, #e2e8f0);
      }

      .row-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;
        min-width: 0;

        .row-title {
          font-size: 14px;
          font-weight: 700;
          line-height: 1.45;
          color: var(--el-text-color-primary, #1e293b);
        }

        .row-title-line {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .live-status-tag {
            border-radius: 9999px;
            font-size: 10.5px;
            height: 18px;
            padding: 0 6px;
          }
        }

        .row-desc {
          font-size: 12px;
          color: var(--el-text-color-secondary, #64748b);
          line-height: 1.5;
        }
      }
    }
  }
}

/* 药丸单选：外层统一轨道，内层选项无独立边框，避免 Element Plus 连体按钮的方角描边 */
:deep(.pill-radio-group) {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 5px;
  background: var(--el-bg-color, #ffffff);
  border: 1px solid var(--el-border-color-lighter, #e8edf3);
  border-radius: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);

  .el-radio-button {
    margin: 0 !important;

    .el-radio-button__inner {
      border: none !important;
      border-radius: 9999px !important;
      margin: 0 !important;
      padding: 7px 14px;
      font-size: 12.5px;
      font-weight: 600;
      line-height: 1.2;
      background: transparent;
      color: var(--el-text-color-regular, #64748b);
      box-shadow: none !important;
      transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
    }

    &:first-child .el-radio-button__inner,
    &:last-child .el-radio-button__inner {
      border-radius: 9999px !important;
    }

    .el-radio-button__original-radio:checked + .el-radio-button__inner {
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
      color: #ffffff !important;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.28) !important;
    }

    &:not(.is-active) .el-radio-button__inner:hover {
      background: var(--el-fill-color-light, #f1f5f9);
      color: var(--el-text-color-primary, #334155);
    }
  }

  .opt-btn-inner {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
  }
}

:deep(.pill-select) {
  .el-input__wrapper {
    border-radius: 9999px !important;
    padding: 4px 14px;
    box-shadow: 0 0 0 1px var(--el-border-color-lighter, #e2e8f0) inset;
    background: var(--el-bg-color, #ffffff);
  }
}

:deep(.custom-switch) {
  --el-switch-on-color: #2563eb;
}
</style>

<template>
  <div class="memory-governance-card" :class="{ 'consent-active': consentGranted }">
    <div class="shield-halo-accent"></div>

    <div class="governance-main-col">
      <div class="shield-badge-box" :class="{ active: consentGranted }">
        <el-icon><Lock /></el-icon>
      </div>
      <div class="compliance-content">
        <div class="title-row">
          <h3 class="governance-title">Agent 记忆沉淀知情同意与用户隐私合规治理</h3>
          <span class="compliance-pill-tag" :class="consentGranted ? 'active' : 'inactive'">
            {{ consentGranted ? '已获知情授权 · 持续沉淀' : '未获授权 · 严格隔离保护' }}
          </span>
          <span class="kms-shield-pill">
            <el-icon><Key /></el-icon>
            国密 SM4-GCM 硬件信道加固
          </span>
        </div>
        <p class="governance-desc">
          智教云 · EduMind 严格遵循《中华人民共和国个人信息保护法》(PIPL) 与可解释 AI 标准。用户享有 100% 透明知情权、单条即时纠错权与随时无条件被遗忘权。沉淀记忆仅用于当前学习者专属 AI 助教因材施教辅导，绝不用于基础大模型二次预训练，亦绝不进行任何跨组织或第三方商业共享。
        </p>
      </div>
    </div>

    <div class="governance-actions-col">
      <div class="action-item retention-box">
        <span class="label">留存周期：</span>
        <el-select
          :model-value="retentionDays"
          size="default"
          class="pill-select"
          style="width: 120px;"
          @update:model-value="$emit('update:retentionDays', $event)"
          @change="$emit('change')"
        >
          <el-option label="30 天" :value="30" />
          <el-option label="90 天" :value="90" />
          <el-option label="180 天 (推荐)" :value="180" />
          <el-option label="365 天" :value="365" />
        </el-select>
      </div>

      <div class="action-item switch-box">
        <span class="switch-label">{{ consentGranted ? '授权已生效' : '已暂停授权' }}</span>
        <el-switch
          :model-value="consentGranted"
          size="default"
          style="--el-switch-on-color: #10B981;"
          @update:model-value="$emit('update:consentGranted', $event)"
          @change="$emit('change')"
        />
      </div>

      <el-button
        type="danger"
        plain
        class="forget-pill-btn"
        @click="$emit('forget-all')"
      >
        <el-icon><Delete /></el-icon>
        <span>行使被遗忘权 (清空)</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lock, Key, Delete } from '@element-plus/icons-vue';

defineProps<{
  consentGranted: boolean;
  retentionDays: number;
}>();

defineEmits<{
  (e: 'update:consentGranted', val: boolean): void;
  (e: 'update:retentionDays', val: number): void;
  (e: 'change'): void;
  (e: 'forget-all'): void;
}>();
</script>

<style scoped lang="scss">
.memory-governance-card {
  position: relative;
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid #E2E8F0;
  padding: 22px 26px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  transition: all 0.3s ease;

  &.consent-active {
    border-color: rgba(16, 185, 129, 0.3);
    box-shadow: 0 6px 24px rgba(16, 185, 129, 0.06);

    .shield-halo-accent {
      background: radial-gradient(circle at 10% 20%, rgba(16, 185, 129, 0.08) 0%, transparent 60%);
    }
  }

  .shield-halo-accent {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    background: radial-gradient(circle at 10% 20%, rgba(37, 99, 235, 0.04) 0%, transparent 60%);
    transition: background 0.3s ease;
  }

  .governance-main-col {
    display: flex;
    align-items: flex-start;
    gap: 18px;
    flex: 1;
    z-index: 1;

    .shield-badge-box {
      width: 52px;
      height: 52px;
      border-radius: 18px;
      background: #F1F5F9;
      color: #64748B;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      flex-shrink: 0;
      transition: all 0.3s ease;

      &.active {
        background: #ECFDF5;
        color: #10B981;
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
      }
    }

    .compliance-content {
      .title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-bottom: 8px;

        .governance-title {
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
          margin: 0;
          line-height: 1.3;
        }

        .compliance-pill-tag {
          font-size: 12px;
          padding: 3px 10px;
          border-radius: 9999px;
          font-weight: 600;

          &.active {
            background: #ECFDF5;
            color: #059669;
            border: 1px solid rgba(16, 185, 129, 0.25);
          }

          &.inactive {
            background: #FEF2F2;
            color: #DC2626;
            border: 1px solid rgba(220, 38, 38, 0.2);
          }
        }

        .kms-shield-pill {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          padding: 2px 9px;
          border-radius: 9999px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          color: #475569;
          font-weight: 500;
        }
      }

      .governance-desc {
        font-size: 13px;
        color: #64748B;
        line-height: 1.6;
        margin: 0;
        max-width: 820px;
      }
    }
  }

  .governance-actions-col {
    display: flex;
    align-items: center;
    gap: 16px;
    z-index: 1;
    flex-shrink: 0;

    .action-item {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 9999px;
      padding: 6px 14px;

      .label,
      .switch-label {
        font-size: 13px;
        color: #475569;
        font-weight: 500;
      }
    }

    .forget-pill-btn {
      border-radius: 9999px;
      padding: 8px 18px;
      font-size: 13px;
      font-weight: 500;
      transition: all 0.2s ease;

      &:hover {
        background: #FEF2F2;
        border-color: #EF4444;
        color: #DC2626;
      }
    }
  }

  @media (max-width: 1100px) {
    flex-direction: column;
    align-items: flex-start;

    .governance-actions-col {
      width: 100%;
      justify-content: flex-start;
      flex-wrap: wrap;
    }
  }
}
</style>

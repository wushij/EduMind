<template>
  <section class="user-stats-grid">
    <!-- 指标卡 1：操作审计活跃度 -->
    <div class="stat-card stat-card--blue">
      <div class="stat-card__icon-box">
        <el-icon><DataAnalysis /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">操作审计活跃度</span>
        <div class="stat-card__value-row">
          <strong class="stat-card__num">{{ auditCount > 0 ? auditCount : '12+' }}</strong>
          <span class="stat-card__unit">条记录</span>
        </div>
        <div class="stat-card__sub-hint">
          <span>近 30 天管理写操作与安全轨迹</span>
        </div>
      </div>
    </div>

    <!-- 指标卡 2：角色身份与授权规模 -->
    <div class="stat-card stat-card--emerald">
      <div class="stat-card__icon-box">
        <el-icon><Stamp /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">角色身份与授权规模</span>
        <div class="stat-card__value-row">
          <strong class="stat-card__num">{{ roleCount || 1 }}</strong>
          <span class="stat-card__unit">项核心角色</span>
        </div>
        <div class="stat-card__sub-hint">
          <span>{{ roleSummaryText }}</span>
        </div>
      </div>
    </div>

    <!-- 指标卡 3：凭据与密码安全评级 -->
    <div class="stat-card stat-card--amber">
      <div class="stat-card__icon-box">
        <el-icon><Key /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">凭据与安全评级</span>
        <div class="stat-card__value-row">
          <strong class="stat-card__num text-amber-600">{{ securityScore.title }}</strong>
        </div>
        <div class="stat-card__sub-hint">
          <span>{{ securityScore.desc }}</span>
        </div>
      </div>
    </div>

    <!-- 指标卡 4：账号交付与运行就绪 -->
    <div class="stat-card stat-card--indigo">
      <div class="stat-card__icon-box">
        <el-icon><CircleCheck /></el-icon>
      </div>
      <div class="stat-card__content">
        <span class="stat-card__label">账号交付与运行状态</span>
        <div class="stat-card__value-row">
          <span class="ready-badge" :class="{ 'is-disabled': !isEnabled }">
            {{ isEnabled ? '就绪可用' : '已冻结截断' }}
          </span>
        </div>
        <div class="stat-card__sub-hint">
          <span>支持会话下线、实时权限生效与审计追溯</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { DataAnalysis, Stamp, Key, CircleCheck } from '@element-plus/icons-vue';

const props = withDefaults(
  defineProps<{
    auditCount?: number;
    roleCount?: number;
    roles?: string[];
    isEnabled?: boolean;
    securityScore?: { title: string; desc: string };
  }>(),
  {
    auditCount: 0,
    roleCount: 1,
    roles: () => ['ROLE_ADMIN'],
    isEnabled: true,
    securityScore: () => ({
      title: '高安全凭据',
      desc: '密码复杂度达标 · 具备独立管理控制台'
    })
  }
);

const roleSummaryText = computed(() => {
  if (!props.roles || props.roles.length === 0) return '平台标准角色授权';
  if (props.roles.some((r) => r.includes('ADMIN'))) {
    return '包含系统全局最高管理权限';
  }
  if (props.roles.some((r) => r.includes('TEACHER'))) {
    return '覆盖课程建设与教学AI批改';
  }
  return '享有在读修读与学伴互动权限';
});
</script>

<style scoped lang="scss">
.user-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  width: 100%;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 16px;
    border-radius: 14px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
    transition: all 0.2s ease;

    &:hover {
      border-color: #cbd5e1;
      transform: translateY(-1px);
      box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
    }

    &__icon-box {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .el-icon {
        font-size: 20px;
      }
    }

    &__content {
      display: flex;
      flex-direction: column;
      min-width: 0;
      flex: 1;
    }

    &__label {
      font-size: 12px;
      color: #64748b;
      font-weight: 500;
      line-height: 1.3;
    }

    &__value-row {
      display: flex;
      align-items: baseline;
      gap: 6px;
      margin-top: 2px;
    }

    &__num {
      font-size: 19px;
      font-weight: 800;
      color: #0f172a;
      line-height: 1.2;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }

    &__unit {
      font-size: 12px;
      color: #64748b;
      font-weight: 600;
    }

    &__sub-hint {
      font-size: 11.5px;
      color: #94a3b8;
      margin-top: 2px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      strong {
        color: #475569;
      }
    }

    .ready-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 8px;
      border-radius: 9999px;
      background: #ecfdf5;
      color: #059669;
      font-size: 12px;
      font-weight: 700;
      border: 1px solid #a7f3d0;

      &.is-disabled {
        background: #fef2f2;
        color: #dc2626;
        border-color: #fecaca;
      }
    }

    /* 各卡片主题强调色 */
    &--blue {
      .stat-card__icon-box {
        background: #eff6ff;
        color: #2563eb;
        border: 1px solid #bfdbfe;
      }
    }

    &--emerald {
      .stat-card__icon-box {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
      }
    }

    &--amber {
      .stat-card__icon-box {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid #fde68a;
      }
    }

    &--indigo {
      .stat-card__icon-box {
        background: #f5f3ff;
        color: #7c3aed;
        border: 1px solid #ddd6fe;
      }
    }
  }
}
</style>

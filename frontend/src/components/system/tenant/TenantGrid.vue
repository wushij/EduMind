<template>
  <div class="tenant-grid" v-if="filteredTenants.length > 0">
    <div
      v-for="tenant in filteredTenants"
      :key="tenant.id"
      class="tenant-card"
      :class="{ 'is-active-tenant': currentTenantId === tenant.id }"
    >
      <div v-if="currentTenantId === tenant.id" class="current-tenant-badge">
        <el-icon><Check /></el-icon>
        <span>当前使用租户</span>
      </div>

      <div class="card-header">
        <div class="tenant-badge">
          <div class="tenant-icon-box" :class="tenant.planCode ? tenant.planCode.toLowerCase() : 'standard'">
            <el-icon><School /></el-icon>
          </div>
          <div class="tenant-meta">
            <div class="name-line">
              <h3 class="tenant-name" :title="tenant.name">{{ tenant.name }}</h3>
            </div>
            <div class="tag-line">
              <span class="pill-tag code-pill">{{ tenant.tenantCode || tenant.code }}</span>
              <span class="pill-tag plan-pill" :class="tenant.planCode ? tenant.planCode.toLowerCase() : 'standard'">
                {{ tenant.planName || resolvePlanName(tenant.planCode) }}
              </span>
            </div>
          </div>
        </div>
        <div class="status-wrap">
          <span
            class="pill-tag status-pill"
            :class="tenant.status === 1 ? 'active' : 'disabled'"
          >
            {{ tenant.status === 1 ? '正常服务' : '已冻结' }}
          </span>
        </div>
      </div>

      <div class="card-body">
        <div class="tenant-metric-panel">
          <div class="metric-cell">
            <div class="cell-icon cell-icon--blue">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="cell-text">
              <span class="cell-label">独立校区</span>
              <span class="cell-value cell-value--blue">{{ tenant.campusCount || 1 }}</span>
            </div>
          </div>
          <div class="metric-cell">
            <div class="cell-icon cell-icon--teal">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="cell-text">
              <span class="cell-label">师生总数</span>
              <span class="cell-value cell-value--teal">{{ (tenant.memberCount || 0).toLocaleString() }}</span>
            </div>
          </div>
          <div class="metric-cell metric-cell--quota">
            <div class="cell-icon cell-icon--purple">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="cell-text">
              <span class="cell-label">Token 配额</span>
              <span class="cell-value cell-value--purple">{{ tenant.tokenUsagePercent || 0 }}%</span>
              <div class="quota-progress-track">
                <div
                  class="quota-progress-fill"
                  :style="{ width: `${Math.min(100, tenant.tokenUsagePercent || 0)}%` }"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="tenant-info-panel">
          <div class="info-line">
            <el-icon class="icon"><Link /></el-icon>
            <span class="label">域名：</span>
            <span class="val domain-text">{{ tenant.domain || '未绑定二级域名' }}</span>
            <el-tooltip content="复制域名" placement="top">
              <el-button
                v-if="tenant.domain"
                link
                type="primary"
                class="copy-btn"
                @click="$emit('copy-domain', tenant.domain)"
              >
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <div class="info-line">
            <el-icon class="icon"><User /></el-icon>
            <span class="label">管理员：</span>
            <span class="val">{{ tenant.adminName || '校级管理员' }}</span>
            <span class="phone-tag">{{ tenant.adminPhone || '未登记手机' }}</span>
          </div>
          <div class="info-line">
            <el-icon class="icon"><Clock /></el-icon>
            <span class="label">有效期至：</span>
            <span class="val">{{ tenant.expireTime ? tenant.expireTime.substring(0, 10) : '长期有效' }}</span>
            <span v-if="isExpiringSoon(tenant.expireTime)" class="expiring-warning-pill">即将到期</span>
          </div>
        </div>
      </div>

      <div class="card-footer">
        <div class="footer-left-btns">
          <el-button
            size="small"
            class="pill-btn-sm switch-btn"
            :type="currentTenantId === tenant.id ? 'success' : 'primary'"
            :plain="currentTenantId !== tenant.id"
            @click="$emit('switch-tenant', tenant)"
          >
            <el-icon><Switch /></el-icon>
            <span>{{ currentTenantId === tenant.id ? '当前租户' : '一键切入' }}</span>
          </el-button>
          <el-button size="small" class="pill-btn-sm outline-btn" @click="$emit('open-campus', tenant)">
            <el-icon><OfficeBuilding /></el-icon>
            <span>校区 ({{ tenant.campusCount || 1 }})</span>
          </el-button>
          <el-button size="small" class="pill-btn-sm outline-btn" @click="$emit('open-quota', tenant)">
            <el-icon><Cpu /></el-icon>
            <span>配额</span>
          </el-button>
          <el-button size="small" class="pill-btn-sm org-link-btn" @click="$emit('jump-org-tree', tenant)">
            <el-icon><Share /></el-icon>
            <span>组织架构</span>
          </el-button>
        </div>

        <div class="footer-right-more">
          <el-dropdown trigger="click" @command="(cmd: string) => $emit('more-command', cmd, tenant)">
            <el-button size="small" class="pill-btn-sm icon-more-btn" circle>
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu class="pill-dropdown-menu">
                <el-dropdown-item command="edit">
                  <el-icon><Edit /></el-icon>
                  <span>编辑基本信息</span>
                </el-dropdown-item>
                <el-dropdown-item command="toggleStatus">
                  <el-icon><Lock /></el-icon>
                  <span>{{ tenant.status === 1 ? '冻结租户' : '启用租户' }}</span>
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided class="danger-item" :disabled="tenant.id === 1">
                  <el-icon><Delete /></el-icon>
                  <span>注销租户</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </div>

  <el-empty v-else description="暂无符合筛选条件的学校租户" />
</template>

<script setup lang="ts">
import {
  School,
  Link,
  User,
  Clock,
  Switch,
  CopyDocument,
  Cpu,
  Share,
  MoreFilled,
  Edit,
  Delete,
  Check,
  OfficeBuilding,
  Lock,
  UserFilled
} from '@element-plus/icons-vue';
import type { TenantListVO } from '@/types/system/tenant';

defineProps<{
  filteredTenants: TenantListVO[];
  currentTenantId?: number;
  resolvePlanName: (planCode?: string) => string;
  isExpiringSoon: (expireTime?: string) => boolean;
}>();

defineEmits<{
  'copy-domain': [domain: string];
  'switch-tenant': [tenant: TenantListVO];
  'open-campus': [tenant: TenantListVO];
  'open-quota': [tenant: TenantListVO];
  'jump-org-tree': [tenant: TenantListVO];
  'more-command': [command: string, tenant: TenantListVO];
}>();
</script>

<style scoped lang="scss">
.tenant-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  width: 100%;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .tenant-card {
    position: relative;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 24px;
    padding: 24px 28px;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
    transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: column;
    gap: 0;
    justify-content: flex-start;

    &:hover {
      border-color: #93C5FD;
      transform: translateY(-3px);
      box-shadow: 0 14px 30px rgba(22, 119, 255, 0.1);
    }

    &.is-active-tenant {
      border-color: rgba(16, 185, 129, 0.5);
      background: linear-gradient(180deg, #FAFCFA 0%, #FFFFFF 100%);
      box-shadow: 0 6px 20px rgba(16, 185, 129, 0.12);
    }

    .current-tenant-badge {
      position: absolute;
      top: -10px;
      right: 28px;
      background: linear-gradient(135deg, #10B981 0%, #059669 100%);
      color: #FFFFFF;
      font-size: 11px;
      font-weight: 700;
      padding: 3px 12px;
      border-radius: 9999px;
      display: flex;
      align-items: center;
      gap: 4px;
      box-shadow: 0 3px 10px rgba(16, 185, 129, 0.3);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;

      .tenant-badge {
        display: flex;
        align-items: center;
        gap: 14px;

        .tenant-icon-box {
          width: 48px;
          height: 48px;
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;
          flex-shrink: 0;

          &.flagship {
            background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
            color: #D97706;
          }

          &.pro {
            background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
            color: #2563EB;
          }

          &.standard {
            background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
            color: #059669;
          }
        }

        .tenant-meta {
          .name-line {
            .tenant-name {
              margin: 0;
              font-size: 17px;
              font-weight: 800;
              color: #0F172A;
              line-height: 1.3;
            }
          }

          .tag-line {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-top: 6px;

            .pill-tag {
              padding: 2px 10px;
              border-radius: 9999px;
              font-size: 11px;
              font-weight: 700;

              &.code-pill {
                background: #F1F5F9;
                color: #475569;
                font-family: monospace;
              }

              &.plan-pill {
                &.flagship {
                  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
                  color: #FFFFFF;
                }

                &.pro {
                  background: linear-gradient(135deg, #3B82F6 0%, #6366F1 100%);
                  color: #FFFFFF;
                }

                &.standard {
                  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
                  color: #FFFFFF;
                }
              }
            }
          }
        }
      }

      .status-wrap {
        .pill-tag.status-pill {
          padding: 4px 12px;
          border-radius: 9999px;
          font-size: 11px;
          font-weight: 700;

          &.active {
            background: #ECFDF5;
            color: #059669;
            border: 1px solid rgba(16, 185, 129, 0.3);
          }

          &.disabled {
            background: #F1F5F9;
            color: #64748B;
            border: 1px solid #CBD5E1;
          }
        }
      }
    }

    .card-body {
      margin-bottom: 20px;
      display: flex;
      flex-direction: column;
      gap: 16px;

      .tenant-metric-panel {
        box-sizing: border-box;
        border-radius: 24px;
        border: 1.5px solid #E2E8F0;
        background: linear-gradient(160deg, #FFFFFF 0%, #F8FAFC 100%);
        padding: 18px 22px;
        box-shadow: 0 4px 20px rgba(30, 80, 150, 0.06);
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        column-gap: 20px;
        row-gap: 0;

        .metric-cell {
          min-width: 0;
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 4px 2px;

          &--quota {
            grid-column: 1 / -1;
            padding-top: 12px;
            margin-top: 4px;
            border-top: 1px solid #E2E8F0;
          }
        }

        .cell-icon {
          width: 44px;
          height: 44px;
          border-radius: 14px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 22px;
          flex-shrink: 0;

          &--blue {
            background: #FFFFFF;
            color: #2563EB;
            border: 1.5px solid #BFDBFE;
            box-shadow: 0 4px 14px rgba(37, 99, 235, 0.14);
          }

          &--teal {
            background: #FFFFFF;
            color: #0D9488;
            border: 1.5px solid #99F6E4;
            box-shadow: 0 4px 14px rgba(13, 148, 136, 0.14);
          }

          &--purple {
            background: #FFFFFF;
            color: #9333EA;
            border: 1.5px solid #E9D5FF;
            box-shadow: 0 4px 14px rgba(147, 51, 234, 0.14);
          }
        }

        .cell-text {
          display: flex;
          flex-direction: column;
          gap: 6px;
          min-width: 0;

          .cell-label {
            font-size: 13px;
            font-weight: 600;
            color: #64748B;
            line-height: 1.2;
          }

          .cell-value {
            font-size: 22px;
            font-weight: 800;
            letter-spacing: -0.02em;
            font-variant-numeric: tabular-nums;
            line-height: 1.1;

            &--blue { color: #2563EB; }
            &--teal { color: #0D9488; }
            &--purple { color: #9333EA; }
          }

          .quota-progress-track {
            width: 100%;
            max-width: none;
            height: 8px;
            margin-top: 2px;
            background: #E2E8F0;
            border-radius: 9999px;
            overflow: hidden;
            border: 1px solid #CBD5E1;

            .quota-progress-fill {
              height: 100%;
              min-width: 4px;
              background: linear-gradient(90deg, #9333EA 0%, #6366F1 100%);
              border-radius: 9999px;
            }
          }
        }
      }

      .tenant-info-panel {
        border-radius: 24px;
        border: 1.5px solid #E2E8F0;
        background: #FFFFFF;
        padding: 18px 24px;
        box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
        display: flex;
        flex-direction: column;
        gap: 10px;

        .info-line {
          display: flex;
          align-items: center;
          font-size: 12px;
          color: #475569;

          .icon {
            color: #94A3B8;
            font-size: 14px;
            margin-right: 6px;
            flex-shrink: 0;
          }

          .label {
            color: #94A3B8;
            margin-right: 4px;
          }

          .val {
            font-weight: 500;
            color: #1E293B;

            &.domain-text {
              color: #2563EB;
              font-family: monospace;
            }
          }

          .copy-btn {
            padding: 0 4px;
            font-size: 12px;
            color: #2563EB;
          }

          .phone-tag {
            margin-left: 8px;
            background: #F1F5F9;
            padding: 1px 8px;
            border-radius: 9999px;
            font-size: 11px;
            color: #64748B;
          }

          .expiring-warning-pill {
            margin-left: 8px;
            background: #FEF2F2;
            color: #EF4444;
            border: 1px solid rgba(239, 68, 68, 0.25);
            padding: 1px 8px;
            border-radius: 9999px;
            font-size: 10px;
            font-weight: 700;
          }
        }
      }
    }

    .card-footer {
      border-top: 1px solid #F1F5F9;
      padding-top: 18px;
      margin-top: auto;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .footer-left-btns {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .pill-btn-sm {
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          padding: 5px 14px;

          &.switch-btn {
            background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
            border: none;
            color: #FFF;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

            &:hover {
              opacity: 0.92;
            }
          }

          &.outline-btn {
            background: #F8FAFC;
            border: 1px solid #CBD5E1;
            color: #334155;

            &:hover {
              background: #EFF6FF;
              border-color: #93C5FD;
              color: #2563EB;
            }
          }

          &.org-link-btn {
            background: #ECFDF5;
            border: 1px solid rgba(16, 185, 129, 0.3);
            color: #059669;

            &:hover {
              background: #D1FAE5;
              border-color: #10B981;
            }
          }
        }
      }

      .footer-right-more {
        .pill-btn-sm.icon-more-btn {
          border-radius: 9999px;
          background: #F1F5F9;
          border: none;
          color: #64748B;

          &:hover {
            background: #E2E8F0;
            color: #0F172A;
          }
        }
      }
    }
  }
}

:deep(.pill-dropdown-menu) {
  border-radius: 20px;
  padding: 8px;

  .el-dropdown-menu__item {
    border-radius: 12px;
    font-size: 12px;
    padding: 6px 14px;

    &.danger-item {
      color: #EF4444;

      &:hover {
        background: #FEF2F2;
        color: #DC2626;
      }
    }
  }
}
</style>

<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <div class="stat-icon-pill primary">
        <el-icon><School /></el-icon>
      </div>
      <div class="stat-meta">
        <div class="stat-num text-primary">
          {{ overviewStats.totalTenants }}
          <small class="sub-text">{{ overviewStats.activeTenants }} 运行中</small>
        </div>
        <span class="stat-label">入驻学校/租户</span>
      </div>
    </div>

    <div class="hero-stat-card">
      <div class="stat-icon-pill success">
        <el-icon><OfficeBuilding /></el-icon>
      </div>
      <div class="stat-meta">
        <div class="stat-num text-success">{{ overviewStats.totalCampuses }}</div>
        <span class="stat-label">覆盖校区总数</span>
      </div>
    </div>

    <div class="hero-stat-card">
      <div class="stat-icon-pill warning">
        <el-icon><UserFilled /></el-icon>
      </div>
      <div class="stat-meta">
        <div class="stat-num text-warning">
          {{ overviewStats.totalMembers.toLocaleString() }}
          <small class="sub-text">师生总量</small>
        </div>
        <span class="stat-label">服务人员大盘</span>
      </div>
    </div>

    <div class="hero-stat-card">
      <div class="stat-icon-pill info">
        <el-icon><Lock /></el-icon>
      </div>
      <div class="stat-meta">
        <div class="stat-num text-info">{{ overviewStats.complianceRate }}%</div>
        <span class="stat-label">数据隔离安全合规</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { School, OfficeBuilding, UserFilled, Lock } from '@element-plus/icons-vue';
import type { TenantOverviewStatsVO } from '@/types/system/tenant';

defineProps<{
  overviewStats: TenantOverviewStatsVO;
}>();
</script>

<style scoped lang="scss">
.hero-stats-row {
  display: flex;
  gap: 14px;
  margin-top: 4px;
  flex-wrap: wrap;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(12px);
    padding: 8px 22px;
    border-radius: 9999px;
    border: 1.5px solid rgba(22, 119, 255, 0.14);
    box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
    display: flex;
    align-items: center;
    gap: 12px;
    transition: all 0.25s ease;

    &:hover {
      background: #FFFFFF;
      border-color: #1677FF;
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(22, 119, 255, 0.12);
    }

    .stat-icon-pill {
      width: 38px;
      height: 38px;
      border-radius: 9999px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;

      &.primary {
        background: #EFF6FF;
        color: #1677FF;
      }

      &.success {
        background: #ECFDF5;
        color: #10B981;
      }

      &.warning {
        background: #FFFBEB;
        color: #F59E0B;
      }

      &.info {
        background: #F0FDF4;
        color: #059669;
      }
    }

    .stat-meta {
      display: flex;
      flex-direction: column;

      .stat-num {
        font-size: 18px;
        font-weight: 800;
        line-height: 1.2;

        .sub-text {
          font-size: 11px;
          font-weight: 500;
          color: #94A3B8;
          margin-left: 4px;
        }

        &.text-primary { color: #1677FF; }
        &.text-success { color: #10B981; }
        &.text-warning { color: #F59E0B; }
        &.text-info { color: #0EA5E9; }
      }

      .stat-label {
        font-size: 11px;
        color: #64748B;
        font-weight: 500;
      }
    }
  }
}
</style>

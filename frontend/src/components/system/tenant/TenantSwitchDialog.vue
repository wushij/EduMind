<template>
  <el-dialog
    v-model="dialogVisible"
    title="切入学校租户 · 二次确认"
    width="540px"
    class="tenant-switch-dialog"
    destroy-on-close
    align-center
    :close-on-click-modal="false"
  >
    <div class="switch-content" v-if="targetTenant">
      <!-- 租户切换对流卡片 -->
      <div class="switch-flow-card">
        <div class="tenant-node current-node">
          <div class="node-badge">当前租户</div>
          <div class="node-name" :title="currentTenantName">{{ currentTenantName }}</div>
          <div class="node-sub">{{ currentTenantCode }}</div>
        </div>

        <div class="flow-arrow">
          <div class="arrow-circle">
            <el-icon><Right /></el-icon>
          </div>
          <span class="flow-text">切入代管</span>
        </div>

        <div class="tenant-node target-node">
          <div class="node-badge target-badge">目标学校</div>
          <div class="node-name" :title="targetTenant.name">{{ targetTenant.name }}</div>
          <div class="node-sub">{{ targetTenant.tenantCode || targetTenant.code }}</div>
        </div>
      </div>

      <!-- 目标租户规格清单 -->
      <div class="target-meta-box">
        <div class="meta-item">
          <span class="label">服务方案：</span>
          <el-tag size="small" :type="getPlanTagType(targetTenant.planCode)" effect="light">
            {{ targetTenant.planName || resolvePlanName(targetTenant.planCode) }}
          </el-tag>
        </div>
        <div class="meta-item">
          <span class="label">独立校区：</span>
          <span class="value">{{ targetTenant.campusCount || 1 }} 个校区</span>
        </div>
        <div class="meta-item">
          <span class="label">平台管理员：</span>
          <span class="value">{{ targetTenant.adminName || '校级管理员' }}</span>
        </div>
        <div class="meta-item" v-if="targetTenant.domain">
          <span class="label">绑定域名：</span>
          <span class="value font-mono">{{ targetTenant.domain }}</span>
        </div>
      </div>

      <!-- 隔离与权限变更提示卡片 -->
      <div class="isolation-notice-card">
        <el-icon class="notice-icon"><WarningFilled /></el-icon>
        <div class="notice-text">
          <div class="notice-title">多租户数据隔离安全合规声明</div>
          <div class="notice-desc">
            切入后，系统将切换为目标学校独立环境（包括独立校区组织架构、在册师生数据、校本课程及专属 AI 配额空间）。角色与菜单权限将按目标租户重新计算，此代管行为将记入系统全局操作审计日志。
          </div>
        </div>
      </div>

      <!-- 目标租户不可用预警（停用/冻结时禁止切入，与后端校验保持一致） -->
      <div v-if="targetTenant && targetTenant.status !== undefined && targetTenant.status !== 1" class="blocked-notice-card">
        <el-icon class="notice-icon"><WarningFilled /></el-icon>
        <div class="notice-text">
          <div class="notice-title">目标租户当前不可切入</div>
          <div class="notice-desc">该学校租户已停用或冻结，请先在租户治理中心恢复服务状态后再执行切入。</div>
        </div>
      </div>

      <!-- 可选切换原因输入框 -->
      <div class="reason-section">
        <label class="reason-label">切入理由 / 代管说明（可选，记入审计日志）：</label>
        <el-input
          v-model="switchReason"
          placeholder="如：例行教学巡检 / 协助配置校区组织架构 / AI 算力扩容排查..."
          clearable
          maxlength="100"
          show-word-limit
          class="pill-input"
          :disabled="submitting"
        />
      </div>

      <!-- 切换过程反馈：避免用户重复点击或误以为无响应 -->
      <div v-if="submitting" class="switching-progress">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在切入目标租户，页面将自动重载并重新计算权限…</span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-actions">
        <el-button class="pill-btn-action outline" @click="handleCancel" :disabled="submitting">
          取消
        </el-button>
        <el-button
          type="primary"
          class="pill-btn-action confirm"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleConfirm"
        >
          <el-icon v-if="!submitting"><Switch /></el-icon>
          <span>{{ submitting ? '切入中…' : '确认一键切入' }}</span>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Right, Switch, WarningFilled, Loading } from '@element-plus/icons-vue';
import { useTenantStore } from '@/stores/system/tenant';
import type { TenantListVO } from '@/types/system/tenant';
import { ElMessage } from 'element-plus';

const props = defineProps<{
  modelValue: boolean;
  tenant: TenantListVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'switched', tenantId: number): void;
}>();

const tenantStore = useTenantStore();
const switchReason = ref('');
/** 提交中 = 本地提交 或 Store 正在切换；两者取并集，避免重复提交 */
const submitting = computed(() => tenantStore.switching);

const canSubmit = computed(() => {
  const target = targetTenant.value;
  if (!target) return false;
  // 已是当前租户、或目标租户已停用时不允许提交
  if (tenantStore.currentTenant?.id === target.id) return false;
  if (target.status !== undefined && target.status !== 1) return false;
  return true;
});

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const targetTenant = computed(() => props.tenant);

const currentTenantName = computed(() => {
  return tenantStore.currentTenant?.name || tenantStore.activeTenantName || '当前租户';
});

const currentTenantCode = computed(() => {
  return tenantStore.currentTenant?.code || tenantStore.currentTenant?.tenantCode || 'CURRENT';
});

const resolvePlanName = (code?: string) => {
  if (!code) return '敏捷标准版';
  switch (code.toUpperCase()) {
    case 'FLAGSHIP': return '尊享旗舰版';
    case 'PRO': return '高配专业版';
    default: return '敏捷标准版';
  }
};

const getPlanTagType = (code?: string) => {
  if (!code) return 'info';
  switch (code.toUpperCase()) {
    case 'FLAGSHIP': return 'warning';
    case 'PRO': return 'primary';
    default: return 'success';
  }
};

const handleCancel = () => {
  dialogVisible.value = false;
  switchReason.value = '';
};

const handleConfirm = async () => {
  if (!targetTenant.value || !canSubmit.value) return;
  try {
    await tenantStore.switchTenant(targetTenant.value.id, switchReason.value.trim() || undefined);
    emit('switched', targetTenant.value.id);
    dialogVisible.value = false;
    switchReason.value = '';
  } catch (err: any) {
    ElMessage.error(err?.message || '切入学校租户失败');
  }
};
</script>

<style scoped lang="scss">
:deep(.el-dialog.tenant-switch-dialog) {
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.18);
  border: 1px solid #E2E8F0;

  .el-dialog__header {
    margin-right: 0;
    padding: 20px 24px 14px;
    border-bottom: 1px solid #F1F5F9;
    .el-dialog__title {
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
    }
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 14px 24px 20px;
    border-top: 1px solid #F1F5F9;
  }
}

.switch-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.switch-flow-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #F8FAFC;
  border: 1.5px dashed #CBD5E1;
  border-radius: 16px;
  padding: 16px;
  gap: 12px;

  .tenant-node {
    flex: 1;
    min-width: 0;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 12px 14px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

    &.target-node {
      border-color: #93C5FD;
      background: #EFF6FF;
    }

    .node-badge {
      display: inline-block;
      font-size: 11px;
      font-weight: 700;
      color: #64748B;
      background: #F1F5F9;
      padding: 2px 8px;
      border-radius: 6px;
      margin-bottom: 6px;

      &.target-badge {
        color: #2563EB;
        background: #DBEAFE;
      }
    }

    .node-name {
      font-size: 14px;
      font-weight: 700;
      color: #1E293B;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .node-sub {
      font-size: 11px;
      color: #94A3B8;
      font-family: monospace;
      margin-top: 2px;
    }
  }

  .flow-arrow {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .arrow-circle {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #2563EB;
      color: #FFFFFF;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.35);
      animation: pulse-arrow 2s infinite ease-in-out;
    }

    .flow-text {
      font-size: 11px;
      font-weight: 600;
      color: #2563EB;
      white-space: nowrap;
    }
  }
}

@keyframes pulse-arrow {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.08);
  }
}

.target-meta-box {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 14px;
  background: #F8FAFC;
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid #F1F5F9;

  .meta-item {
    display: flex;
    align-items: center;
    font-size: 13px;

    .label {
      color: #64748B;
      margin-right: 4px;
      white-space: nowrap;
    }

    .value {
      font-weight: 600;
      color: #334155;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      &.font-mono {
        font-family: monospace;
        color: #2563EB;
      }
    }
  }
}

.isolation-notice-card {
  display: flex;
  gap: 12px;
  background: #FEF9C3;
  border: 1px solid #FDE047;
  border-radius: 14px;
  padding: 12px 14px;

  .notice-icon {
    font-size: 20px;
    color: #CA8A04;
    margin-top: 2px;
    flex-shrink: 0;
  }

  .notice-text {
    .notice-title {
      font-size: 13px;
      font-weight: 700;
      color: #854D0E;
      margin-bottom: 3px;
    }

    .notice-desc {
      font-size: 12px;
      line-height: 1.55;
      color: #713F12;
    }
  }
}

// 目标租户停用时的阻断提示（与后端 status 校验保持一致）
.blocked-notice-card {
  display: flex;
  gap: 12px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 14px;
  padding: 12px 14px;

  .notice-icon {
    font-size: 20px;
    color: #DC2626;
    margin-top: 2px;
    flex-shrink: 0;
  }

  .notice-text {
    .notice-title {
      font-size: 13px;
      font-weight: 700;
      color: #991B1B;
      margin-bottom: 3px;
    }

    .notice-desc {
      font-size: 12px;
      line-height: 1.55;
      color: #B91C1C;
    }
  }
}

.reason-section {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .reason-label {
    font-size: 12.5px;
    font-weight: 600;
    color: #475569;
  }

  :deep(.pill-input) {
    .el-input__wrapper {
      border-radius: 10px;
      padding: 6px 12px;
      background: #F8FAFC;
    }
  }
}

// 切换过程反馈条
.switching-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: #2563EB;
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 10px;
  padding: 9px 12px;

  .is-loading {
    animation: rotating 1.4s linear infinite;
    font-size: 15px;
  }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  .pill-btn-action {
    border-radius: 9999px;
    height: 38px;
    padding: 0 24px;
    font-weight: 600;
    font-size: 13px;
    transition: all 0.25s ease;

    &.confirm {
      background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
      border: none;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.28);

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.38);
      }
    }

    &.outline {
      border-color: #CBD5E1;
      color: #475569;
      background: #FFFFFF;

      &:hover {
        color: #0F172A;
        border-color: #94A3B8;
        background: #F8FAFC;
      }
    }
  }
}
</style>

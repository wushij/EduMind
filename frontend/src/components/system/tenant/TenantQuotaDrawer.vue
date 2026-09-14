<template>
  <el-drawer
    v-model="visible"
    :title="`资源配额治理中心 · ${tenant?.name || ''}`"
    size="560px"
    class="tenant-quota-drawer"
    destroy-on-close
    @closed="handleClose"
  >
    <div class="quota-drawer-container" v-loading="loading">
      <!-- 租户信息胶囊条 -->
      <div class="quota-banner-card">
        <div class="banner-left">
          <div class="tenant-plan-pill" :class="tenant?.planCode ? tenant.planCode.toLowerCase() : 'standard'">
            {{ tenant?.planName || '标准方案' }}
          </div>
          <div class="tenant-title-meta">
            <span class="tenant-code">{{ tenant?.tenantCode || tenant?.code }}</span>
            <span class="tenant-name">{{ tenant?.name }}</span>
          </div>
        </div>
        <div class="banner-right">
          <span class="expire-badge">服务期至 {{ tenant?.expireTime ? tenant.expireTime.substring(0, 10) : '长期有效' }}</span>
        </div>
      </div>

      <div class="quota-tip-box">
        <el-icon><Compass /></el-icon>
        <span>系统基于多租户物理隔离体系实施配额管控。当资源使用率超过预警阈值时，系统将通过站内信与运维中心自动告警。</span>
      </div>

      <!-- 配额卡片组 -->
      <div class="quota-cards-grid">
        <div
          v-for="quota in quotaList"
          :key="quota.quotaType"
          class="quota-card-item"
          :class="{ 'is-warning': quota.isWarning || (quota.usagePercent || 0) >= quota.warningThreshold }"
        >
          <div class="card-top-row">
            <div class="quota-title-box">
              <div class="type-icon-box" :class="quota.quotaType.toLowerCase()">
                <el-icon><component :is="getQuotaIcon(quota.quotaType)" /></el-icon>
              </div>
              <div class="type-meta">
                <span class="quota-name">{{ getQuotaTypeName(quota.quotaType) }}</span>
                <span class="quota-desc">{{ getQuotaDesc(quota.quotaType) }}</span>
              </div>
            </div>
            <div class="quota-badge">
              <span
                class="pill-tag status-pill"
                :class="getStatusClass(quota)"
              >
                {{ getStatusText(quota) }}
              </span>
            </div>
          </div>

          <!-- 数值与进度条 -->
          <div class="card-metric-block">
            <div class="metric-num-line">
              <div class="left-val">
                <span class="used-num">{{ formatQuotaValue(quota.quotaType, quota.usedValue) }}</span>
                <span class="split">/</span>
                <span class="total-num">{{ formatQuotaValue(quota.quotaType, quota.limitValue) }}</span>
              </div>
              <div class="percent-val" :class="getStatusClass(quota)">
                {{ quota.usagePercent || 0 }}%
              </div>
            </div>

            <!-- 长圆边框胶囊进度槽 -->
            <div class="pill-progress-track">
              <div
                class="pill-progress-fill"
                :class="getStatusClass(quota)"
                :style="{ width: `${Math.min(100, quota.usagePercent || 0)}%` }"
              ></div>
              <!-- 预警阈值标记线 -->
              <div
                class="threshold-marker"
                :style="{ left: `${quota.warningThreshold}%` }"
                :title="`预警线: ${quota.warningThreshold}%`"
              ></div>
            </div>

            <div class="threshold-tip-row">
              <span class="tip-left">已消耗 {{ quota.usagePercent || 0 }}%</span>
              <span class="tip-right">预警阈值线: {{ quota.warningThreshold }}%</span>
            </div>
          </div>

          <!-- 底部快捷调配 -->
          <div class="card-footer-row">
            <span class="rule-hint">{{ getQuotaRuleHint(quota.quotaType) }}</span>
            <el-button size="small" class="pill-btn-sm edit-btn" @click="openAdjustDialog(quota)">
              <el-icon><Setting /></el-icon>
              <span>调整配额</span>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 调整配额对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="调整租户资源配额"
      width="440px"
      append-to-body
      destroy-on-close
      class="quota-adjust-dialog"
    >
      <div v-if="currentEditingQuota" class="adjust-dialog-body">
        <div class="target-quota-banner">
          <span class="target-title">{{ getQuotaTypeName(currentEditingQuota.quotaType) }}</span>
          <span class="target-tag">{{ currentEditingQuota.quotaType }}</span>
        </div>

        <el-form label-position="top">
          <el-form-item :label="`配额上限 (${getQuotaUnit(currentEditingQuota.quotaType)})`">
            <el-input-number
              v-model="adjustForm.limitValue"
              :min="1"
              :max="1000000000"
              :step="getQuotaStep(currentEditingQuota.quotaType)"
              class="pill-number-input"
              style="width: 100%;"
            />
          </el-form-item>

          <el-form-item label="安全预警告警阈值 (百分比 %)">
            <div class="slider-box">
              <el-slider
                v-model="adjustForm.warningThreshold"
                :min="50"
                :max="99"
                :marks="{ 75: '75%', 85: '85%', 95: '95%' }"
              />
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="modal-footer">
          <el-button class="pill-btn" @click="adjustDialogVisible = false">取消</el-button>
          <el-button type="primary" class="pill-btn confirm-btn" :loading="saving" @click="submitAdjust">
            保存配额配置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Compass,
  Cpu,
  FolderOpened,
  Odometer,
  UserFilled,
  Setting
} from '@element-plus/icons-vue';
import { listTenantQuotas, updateTenantQuota } from '@/api/system/tenant';
import type { TenantListVO, TenantQuotaVO } from '@/types/system/tenant';

const props = defineProps<{
  modelValue: boolean;
  tenant: TenantListVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'changed'): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const saving = ref(false);
const quotaList = ref<TenantQuotaVO[]>([]);

const adjustDialogVisible = ref(false);
const currentEditingQuota = ref<TenantQuotaVO | null>(null);
const adjustForm = ref({
  limitValue: 0,
  warningThreshold: 85
});

const loadQuotas = async () => {
  if (!props.tenant?.id) return;
  try {
    loading.value = true;
    const res = await listTenantQuotas(props.tenant.id);
    if (res?.data && res.data.length > 0) {
      quotaList.value = res.data;
    } else {
      // 默认保障配额
      quotaList.value = [
        { tenantId: props.tenant.id, quotaType: 'TOKEN', limitValue: 30000000, usedValue: 5600000, usagePercent: 18, warningThreshold: 85 },
        { tenantId: props.tenant.id, quotaType: 'STORAGE', limitValue: 300, usedValue: 68, usagePercent: 22, warningThreshold: 80 },
        { tenantId: props.tenant.id, quotaType: 'QPS', limitValue: 150, usedValue: 32, usagePercent: 21, warningThreshold: 90 },
        { tenantId: props.tenant.id, quotaType: 'SEATS', limitValue: 1200, usedValue: 640, usagePercent: 53, warningThreshold: 85 }
      ];
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载租户配额信息失败');
  } finally {
    loading.value = false;
  }
};

const getQuotaIcon = (type: string) => {
  switch (type) {
    case 'TOKEN': return Cpu;
    case 'STORAGE': return FolderOpened;
    case 'QPS': return Odometer;
    case 'SEATS': return UserFilled;
    default: return Compass;
  }
};

const getQuotaTypeName = (type: string) => {
  switch (type) {
    case 'TOKEN': return 'AI 算力 Token 额度';
    case 'STORAGE': return '云端存储空间';
    case 'QPS': return 'API 最大并发 QPS';
    case 'SEATS': return '师生授权许可席位';
    default: return type;
  }
};

const getQuotaDesc = (type: string) => {
  switch (type) {
    case 'TOKEN': return '覆盖大模型助教、智能答疑、作业批改等智能计算消耗';
    case 'STORAGE': return '包含课件、题库图片、音视频等教研教学资产存储';
    case 'QPS': return '保障高并发上课、在线考试场景下的系统响应吞吐';
    case 'SEATS': return '允许加入该学校租户的教师与学生有效账号总量';
    default: return '';
  }
};

const getQuotaUnit = (type: string) => {
  switch (type) {
    case 'TOKEN': return 'Tokens (个)';
    case 'STORAGE': return 'GB';
    case 'QPS': return '次/秒';
    case 'SEATS': return '席位 (人)';
    default: return '';
  }
};

const getQuotaStep = (type: string) => {
  switch (type) {
    case 'TOKEN': return 1000000;
    case 'STORAGE': return 50;
    case 'QPS': return 20;
    case 'SEATS': return 100;
    default: return 10;
  }
};

const getQuotaRuleHint = (type: string) => {
  switch (type) {
    case 'TOKEN': return '月度滚动周期，支持在线弹性扩容';
    case 'STORAGE': return '多副本分布式冷热分级存储';
    case 'QPS': return '网关级平滑限流与突发削峰保护';
    case 'SEATS': return '支持按班级与院系动态分配席位';
    default: return '';
  }
};

const formatQuotaValue = (type: string, val: number) => {
  if (type === 'TOKEN') {
    if (val >= 1000000) return `${(val / 1000000).toFixed(1)}M`;
    if (val >= 1000) return `${(val / 1000).toFixed(0)}K`;
    return val.toLocaleString();
  }
  if (type === 'STORAGE') return `${val} GB`;
  if (type === 'QPS') return `${val} QPS`;
  if (type === 'SEATS') return `${val} 人`;
  return val.toLocaleString();
};

const getStatusClass = (quota: TenantQuotaVO) => {
  const percent = quota.usagePercent || 0;
  if (percent >= quota.warningThreshold) return 'is-danger';
  if (percent >= 70) return 'is-warn';
  return 'is-healthy';
};

const getStatusText = (quota: TenantQuotaVO) => {
  const percent = quota.usagePercent || 0;
  if (percent >= quota.warningThreshold) return '超额预警';
  if (percent >= 70) return '负荷较高';
  return '健康充裕';
};

const openAdjustDialog = (quota: TenantQuotaVO) => {
  currentEditingQuota.value = quota;
  adjustForm.value = {
    limitValue: quota.limitValue,
    warningThreshold: quota.warningThreshold || 85
  };
  adjustDialogVisible.value = true;
};

const submitAdjust = async () => {
  if (!currentEditingQuota.value || !props.tenant?.id) return;
  try {
    saving.value = true;
    await updateTenantQuota({
      tenantId: props.tenant.id,
      quotaType: currentEditingQuota.value.quotaType,
      limitValue: adjustForm.value.limitValue,
      warningThreshold: adjustForm.value.warningThreshold
    });
    ElMessage.success('配额与预警配置已成功调配');
    adjustDialogVisible.value = false;
    await loadQuotas();
    emit('changed');
  } catch (e: any) {
    ElMessage.error(e.message || '调配失败');
  } finally {
    saving.value = false;
  }
};

const handleClose = () => {
  quotaList.value = [];
};

defineExpose({
  loadQuotas
});
</script>

<style scoped lang="scss">
.tenant-quota-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 16px;
    font-weight: 700;
    font-size: 16px;
    color: #0F172A;
    border-bottom: 1px solid rgba(226, 232, 240, 0.8);
    padding-bottom: 14px;
  }
}

.quota-drawer-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 4px;

  .quota-banner-card {
    background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
    border-radius: 20px;
    padding: 16px 20px;
    color: #FFFFFF;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.15);

    .banner-left {
      display: flex;
      align-items: center;
      gap: 14px;

      .tenant-plan-pill {
        padding: 4px 12px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 700;
        letter-spacing: 0.5px;

        &.flagship {
          background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
          color: #FFF;
          box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35);
        }

        &.pro {
          background: linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%);
          color: #FFF;
        }

        &.standard {
          background: linear-gradient(135deg, #10B981 0%, #059669 100%);
          color: #FFF;
        }
      }

      .tenant-title-meta {
        display: flex;
        flex-direction: column;

        .tenant-code {
          font-size: 11px;
          color: #94A3B8;
          font-family: monospace;
        }

        .tenant-name {
          font-size: 16px;
          font-weight: 700;
          color: #F8FAFC;
        }
      }
    }

    .banner-right {
      .expire-badge {
        font-size: 11px;
        color: #CBD5E1;
        background: rgba(255, 255, 255, 0.12);
        padding: 4px 12px;
        border-radius: 9999px;
      }
    }
  }

  .quota-tip-box {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 9999px;
    padding: 8px 18px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #475569;

    .el-icon {
      color: #1677FF;
      font-size: 15px;
      flex-shrink: 0;
    }
  }

  .quota-cards-grid {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .quota-card-item {
      background: #FFFFFF;
      border: 1.5px solid #E2E8F0;
      border-radius: 20px;
      padding: 16px 20px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
      transition: all 0.25s ease;

      &:hover {
        border-color: #93C5FD;
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(22, 119, 255, 0.08);
      }

      &.is-warning {
        border-color: rgba(239, 68, 68, 0.35);
        background: linear-gradient(180deg, #FEF2F2 0%, #FFFFFF 100%);
      }

      .card-top-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;

        .quota-title-box {
          display: flex;
          align-items: center;
          gap: 12px;

          .type-icon-box {
            width: 42px;
            height: 42px;
            border-radius: 9999px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;

            &.token {
              background: #EFF6FF;
              color: #2563EB;
            }

            &.storage {
              background: #F0FDF4;
              color: #16A34A;
            }

            &.qps {
              background: #FAF5FF;
              color: #9333EA;
            }

            &.seats {
              background: #FFFBEB;
              color: #D97706;
            }
          }

          .type-meta {
            display: flex;
            flex-direction: column;

            .quota-name {
              font-size: 15px;
              font-weight: 700;
              color: #0F172A;
            }

            .quota-desc {
              font-size: 11px;
              color: #94A3B8;
              margin-top: 2px;
            }
          }
        }

        .pill-tag.status-pill {
          padding: 3px 12px;
          border-radius: 9999px;
          font-size: 11px;
          font-weight: 700;

          &.is-healthy {
            background: #ECFDF5;
            color: #059669;
            border: 1px solid rgba(16, 185, 129, 0.3);
          }

          &.is-warn {
            background: #FFFBEB;
            color: #D97706;
            border: 1px solid rgba(245, 158, 11, 0.3);
          }

          &.is-danger {
            background: #FEF2F2;
            color: #DC2626;
            border: 1px solid rgba(239, 68, 68, 0.3);
          }
        }
      }

      .card-metric-block {
        margin-bottom: 12px;

        .metric-num-line {
          display: flex;
          justify-content: space-between;
          align-items: baseline;
          margin-bottom: 8px;

          .left-val {
            display: flex;
            align-items: baseline;
            gap: 4px;

            .used-num {
              font-size: 20px;
              font-weight: 800;
              color: #0F172A;
            }

            .split {
              font-size: 14px;
              color: #94A3B8;
            }

            .total-num {
              font-size: 13px;
              color: #64748B;
              font-weight: 600;
            }
          }

          .percent-val {
            font-size: 16px;
            font-weight: 800;

            &.is-healthy { color: #10B981; }
            &.is-warn { color: #F59E0B; }
            &.is-danger { color: #EF4444; }
          }
        }

        .pill-progress-track {
          position: relative;
          height: 10px;
          background: #E2E8F0;
          border-radius: 9999px;
          overflow: hidden;

          .pill-progress-fill {
            height: 100%;
            border-radius: 9999px;
            transition: width 0.4s ease;

            &.is-healthy {
              background: linear-gradient(90deg, #10B981 0%, #34D399 100%);
            }

            &.is-warn {
              background: linear-gradient(90deg, #F59E0B 0%, #FBBF24 100%);
            }

            &.is-danger {
              background: linear-gradient(90deg, #EF4444 0%, #F87171 100%);
            }
          }

          .threshold-marker {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 2px;
            background: #EF4444;
            opacity: 0.85;
            z-index: 2;
          }
        }

        .threshold-tip-row {
          display: flex;
          justify-content: space-between;
          margin-top: 6px;
          font-size: 11px;
          color: #94A3B8;
        }
      }

      .card-footer-row {
        border-top: 1px dashed #E2E8F0;
        padding-top: 10px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .rule-hint {
          font-size: 11px;
          color: #64748B;
        }

        .pill-btn-sm {
          border-radius: 9999px;
          font-size: 12px;
          background: #EFF6FF;
          color: #2563EB;
          border: 1px solid rgba(37, 99, 235, 0.2);

          &:hover {
            background: #DBEAFE;
          }
        }
      }
    }
  }
}

.pill-btn {
  border-radius: 9999px;
  font-weight: 600;
  padding: 8px 18px;
}

.adjust-dialog-body {
  .target-quota-banner {
    background: #F8FAFC;
    border: 1.5px solid #E2E8F0;
    border-radius: 14px;
    padding: 12px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .target-title {
      font-size: 15px;
      font-weight: 700;
      color: #0F172A;
    }

    .target-tag {
      font-size: 11px;
      background: #EFF6FF;
      color: #2563EB;
      padding: 2px 10px;
      border-radius: 9999px;
      font-weight: 600;
    }
  }
}
</style>

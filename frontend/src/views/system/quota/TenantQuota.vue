<template>
  <div class="tenant-quota-page" v-loading="loading">
    <PageHeroBanner
      title="租户资源配额与用量大盘 · 智能云容量管控"
      subtitle="实时监控各校区与租户的大模型 Token 消耗、向量检索库存储、QPS 并发峰值与预算超额预警"
      background-variant="system"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">{{ tokenPercentage }}%</span>
            <span class="stat-label">本月 Token 消耗水位</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ (quotas.storageUsed / 1024).toFixed(2) }} GB</span>
            <span class="stat-label">向量检索库容量</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ quotas.qpsPeak }} QPS</span>
            <span class="stat-label">今日并发峰值</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num" :class="tokenPercentage >= quotas.tokenWarningThreshold ? (tokenPercentage >= 100 ? 'text-danger' : 'text-warning') : 'text-info'">
              {{ tokenPercentage >= 100 ? '超额阻断' : (tokenPercentage >= quotas.tokenWarningThreshold ? '水位预警' : '配额正常') }}
            </span>
            <span class="stat-label">配额健康状态</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 顶部控制条与租户上下文 -->
      <div class="quota-top-bar">
        <div class="context-info">
          <el-icon><School /></el-icon>
          <span class="tenant-title">当前租户大盘：{{ tenantStore.activeTenantName }} ({{ tenantStore.activeCampusName }})</span>
          <el-tag size="small" type="success" effect="plain">企业教育旗舰版</el-tag>
        </div>
        <div class="action-buttons">
          <el-button plain @click="refreshQuotas">
            <el-icon><Refresh /></el-icon>
            <span>刷新用量</span>
          </el-button>
          <el-button type="primary" class="gradient-btn" @click="openConfigModal">
            <el-icon><Setting /></el-icon>
            <span>配额阈值与预警配置</span>
          </el-button>
        </div>
      </div>

      <!-- 四大核心配额卡片矩阵 -->
      <div class="telemetry-grid">
        <!-- 1. Token 配额 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box token"><el-icon><Coin /></el-icon></div>
              <div class="title-meta">
                <h4>AI Token 算力额度</h4>
                <span class="sub">周期：按自然月度重置</span>
              </div>
            </div>
            <el-tag :type="tokenPercentage >= quotas.tokenWarningThreshold ? (tokenPercentage >= 100 ? 'danger' : 'warning') : 'primary'" size="small">
              {{ tokenPercentage }}% 已消耗
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ (quotas.tokenUsed / 10000).toFixed(1) }}万</span>
              <span class="total-val">/ {{ (quotas.tokenLimit / 10000).toFixed(0) }}万 Tokens</span>
            </div>
            <el-progress
              :percentage="Math.min(100, tokenPercentage)"
              :color="isTokenWarning ? (tokenPercentage >= 100 ? '#EF4444' : '#F59E0B') : '#2563EB'"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>预警水位线：{{ quotas.tokenWarningThreshold }}%</span>
              <span class="est-text" :class="{ 'text-danger': tokenPercentage >= 100 }">
                {{ tokenPercentage >= 100 ? '租户 Token 配额已耗尽，请联系管理员扩容' : (isTokenWarning ? '建议尽快扩容' : '余量充足') }}
              </span>
            </div>
          </div>
        </div>

        <!-- 2. 向量库与知识存储 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box storage"><el-icon><PieChart /></el-icon></div>
              <div class="title-meta">
                <h4>Milvus 向量知识库存储</h4>
                <span class="sub">包含切片索引与元数据</span>
              </div>
            </div>
            <el-tag type="success" size="small">
              {{ storagePercentage }}% 容量
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ quotas.storageUsed }} MB</span>
              <span class="total-val">/ {{ quotas.storageLimit }} MB</span>
            </div>
            <el-progress
              :percentage="storagePercentage"
              color="#10B981"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>已建立向量切片：42,500 条</span>
              <span class="est-text">存储充足</span>
            </div>
          </div>
        </div>

        <!-- 3. 并发 QPS 峰值 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box qps"><el-icon><TrendCharts /></el-icon></div>
              <div class="title-meta">
                <h4>接口吞吐与 QPS 峰值</h4>
                <span class="sub">智能速率限制与突发缓冲</span>
              </div>
            </div>
            <el-tag type="warning" size="small">
              最大限制 {{ quotas.qpsLimit }} QPS
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ quotas.qpsPeak }} QPS</span>
              <span class="total-val">/ 限流值 {{ quotas.qpsLimit }} QPS</span>
            </div>
            <el-progress
              :percentage="Math.round((quotas.qpsPeak / quotas.qpsLimit) * 100)"
              color="#F59E0B"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>今日拒绝拦截：0 次</span>
              <span class="est-text text-success">吞吐平稳</span>
            </div>
          </div>
        </div>

        <!-- 4. Agent 席位与并发 -->
        <div class="telemetry-card">
          <div class="card-header">
            <div class="header-left">
              <div class="icon-box concurrency"><el-icon><Cpu /></el-icon></div>
              <div class="title-meta">
                <h4>Agent 并发会话席位</h4>
                <span class="sub">支持多工作流同时执行</span>
              </div>
            </div>
            <el-tag type="info" size="small">
              {{ quotas.concurrencyUsed }} / {{ quotas.concurrencyLimit }} 席位
            </el-tag>
          </div>
          <div class="card-body">
            <div class="usage-stats">
              <span class="used-val">{{ quotas.concurrencyUsed }} 席</span>
              <span class="total-val">/ 额定 {{ quotas.concurrencyLimit }} 席位</span>
            </div>
            <el-progress
              :percentage="Math.round((quotas.concurrencyUsed / quotas.concurrencyLimit) * 100)"
              color="#8B5CF6"
              :stroke-width="10"
              style="margin: 14px 0;"
            />
            <div class="card-bottom-info">
              <span>空闲可用席位：{{ quotas.concurrencyLimit - quotas.concurrencyUsed }}</span>
              <span class="est-text">弹性扩容支持</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 租户消耗流水与明细审计 -->
      <div class="history-table-card">
        <div class="table-header">
          <div class="title-row">
            <h3>近期 AI 算力与模型调用明细</h3>
            <span class="subtitle">记录各学院、教师在智能组卷、AI出题、批改与答疑场景下的精确消耗</span>
          </div>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
            style="width: 240px;"
          />
        </div>

        <el-table :data="auditRecords" stripe style="width: 100%;">
          <el-table-column prop="time" label="调用时间" width="170" />
          <el-table-column prop="caller" label="调用师生/服务" width="160">
            <template #default="{ row }">
              <div class="caller-cell">
                <span class="name">{{ row.caller }}</span>
                <span class="dept">{{ row.dept }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="modelName" label="模型规格" width="160">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" type="info">{{ row.modelName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizScenario" label="业务场景" width="150" />
          <el-table-column prop="tokens" label="消耗 Tokens" width="140">
            <template #default="{ row }">
              <span class="token-val font-mono">{{ row.tokens.toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时 (ms)" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.status === 'SUCCESS' ? '成功' : '拦截' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 阈值与预警配置弹窗 -->
    <el-dialog v-model="configDialogVisible" title="配置租户资源预警阈值与额度" width="520px">
      <el-form label-position="top">
        <el-form-item label="Token 预警阈值水位线 (%)">
          <el-slider v-model="editForm.warningThreshold" :min="50" :max="95" show-input />
          <span class="form-tip">当租户总用量达到该百分比时，系统自动发送飞书/邮件预警通知</span>
        </el-form-item>
        <el-form-item label="月度 Token 额度上限 (Tokens)">
          <el-input-number v-model="editForm.tokenLimit" :step="5000000" :min="1000000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="单校区最高 QPS 限流速率">
          <el-input-number v-model="editForm.qpsLimit" :step="10" :min="10" :max="200" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存配额策略</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { School, Refresh, Setting, Coin, PieChart, TrendCharts, Cpu } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { listTenantQuotas, updateTenantQuota } from '@/api/system/tenant';
import { useTenantStore } from '@/stores/system/tenant';

const tenantStore = useTenantStore();
const loading = ref(false);
const dateRange = ref<[Date, Date] | []>([]);
const configDialogVisible = ref(false);

const quotas = ref({
  tokenUsed: 4820000,
  tokenLimit: 10000000,
  tokenUsagePercent: 48,
  tokenWarningThreshold: 85,
  tokenIsWarning: false,
  storageUsed: 1820,
  storageLimit: 5120,
  qpsPeak: 42,
  qpsLimit: 80,
  concurrencyUsed: 16,
  concurrencyLimit: 50
});

const editForm = ref({
  warningThreshold: 85,
  tokenLimit: 10000000,
  qpsLimit: 80
});

// 水位优先使用后端计算好的 usagePercent，避免前端重复计算
const tokenPercentage = computed(() => {
  if (typeof quotas.value.tokenUsagePercent === 'number') {
    return quotas.value.tokenUsagePercent;
  }
  return quotas.value.tokenLimit > 0 ? Math.round((quotas.value.tokenUsed / quotas.value.tokenLimit) * 100) : 0;
});

// 预警状态优先读取后端 VO 的 isWarning 标志
const isTokenWarning = computed(() => {
  if (typeof quotas.value.tokenIsWarning === 'boolean') {
    return quotas.value.tokenIsWarning;
  }
  return tokenPercentage.value >= quotas.value.tokenWarningThreshold;
});

const storagePercentage = computed(() => {
  return quotas.value.storageLimit > 0 ? Math.round((quotas.value.storageUsed / quotas.value.storageLimit) * 100) : 0;
});

const auditRecords = ref([
  { time: '2026-09-13 11:20:12', caller: '张教授', dept: '高中部 / 数学教研组', modelName: 'DeepSeek-V3-Chat', bizScenario: 'AI 智能试卷生成', tokens: 4280, duration: 860, status: 'SUCCESS' },
  { time: '2026-09-13 11:15:45', caller: '王小凡', dept: '高三(1)班', modelName: 'DeepSeek-R1-Reasoning', bizScenario: '变式题启发式攻坚', tokens: 2150, duration: 1240, status: 'SUCCESS' },
  { time: '2026-09-13 11:02:30', caller: '李青青', dept: '高二(1)班', modelName: 'DeepSeek-V3-Chat', bizScenario: '课程智能助教答疑', tokens: 940, duration: 420, status: 'SUCCESS' },
  { time: '2026-09-13 10:48:19', caller: '陈老师', dept: '国际部 / AP物理', modelName: 'DeepSeek-V3-Chat', bizScenario: '全班作业智能批改', tokens: 6890, duration: 2100, status: 'SUCCESS' },
  { time: '2026-09-13 10:30:04', caller: '赵子轩', dept: '高三(1)班', modelName: 'DeepSeek-R1-Reasoning', bizScenario: '错题深度归因', tokens: 1820, duration: 990, status: 'SUCCESS' }
]);

const refreshQuotas = async () => {
  try {
    loading.value = true;
    const res = await listTenantQuotas();
    if (res?.data && res.data.length > 0) {
      const tokenItem = res.data.find(q => q.quotaType === 'TOKEN');
      if (tokenItem) {
        quotas.value.tokenUsed = tokenItem.usedValue;
        quotas.value.tokenLimit = tokenItem.limitValue;
        quotas.value.tokenUsagePercent = tokenItem.usagePercent ?? (tokenItem.limitValue > 0 ? Math.round((tokenItem.usedValue / tokenItem.limitValue) * 100) : 0);
        quotas.value.tokenWarningThreshold = tokenItem.warningThreshold;
        quotas.value.tokenIsWarning = tokenItem.isWarning ?? (quotas.value.tokenUsagePercent >= tokenItem.warningThreshold);
      }
      const storageItem = res.data.find(q => q.quotaType === 'STORAGE');
      if (storageItem) {
        quotas.value.storageUsed = storageItem.usedValue;
        quotas.value.storageLimit = storageItem.limitValue;
      }
      const qpsItem = res.data.find(q => q.quotaType === 'QPS');
      if (qpsItem) {
        quotas.value.qpsPeak = qpsItem.usedValue;
        quotas.value.qpsLimit = qpsItem.limitValue;
      }
      const seatsItem = res.data.find(q => q.quotaType === 'SEATS');
      if (seatsItem) {
        quotas.value.concurrencyUsed = seatsItem.usedValue;
        quotas.value.concurrencyLimit = seatsItem.limitValue;
      }
    }
    ElMessage.success('用量监控数据已同步');
  } catch (e) {
    // Keep baseline default
  } finally {
    loading.value = false;
  }
};

const openConfigModal = () => {
  editForm.value = {
    warningThreshold: quotas.value.tokenWarningThreshold,
    tokenLimit: quotas.value.tokenLimit,
    qpsLimit: quotas.value.qpsLimit
  };
  configDialogVisible.value = true;
};

const saveConfig = async () => {
  try {
    await updateTenantQuota({
      quotaType: 'TOKEN',
      limitValue: editForm.value.tokenLimit,
      warningThreshold: editForm.value.warningThreshold
    });
    quotas.value.tokenLimit = editForm.value.tokenLimit;
    quotas.value.tokenWarningThreshold = editForm.value.warningThreshold;
    quotas.value.qpsLimit = editForm.value.qpsLimit;
    ElMessage.success('配额策略已成功应用');
    configDialogVisible.value = false;
  } catch (e: any) {
    ElMessage.error(e.message || '保存配额失败');
  }
};

onMounted(() => {
  refreshQuotas();
});
</script>

<style scoped lang="scss">
.tenant-quota-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 14px;
    margin-top: 4px;
    flex-wrap: wrap;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(8px);
      padding: 6px 20px;
      border-radius: 9999px;
      border: 1.5px solid rgba(22, 119, 255, 0.12);
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #1677FF;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
      }

      .stat-num {
        font-size: 18px;
        font-weight: 800;
        line-height: 1;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-info { color: #0284C7; }
      }

      .stat-label {
        font-size: 12.5px;
        font-weight: 500;
        color: #475569;
        margin-top: 0;
        white-space: nowrap;
      }
    }
  }

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .quota-top-bar {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    margin-bottom: 22px;

    .context-info {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 15px;
      font-weight: 600;
      color: #1E293B;

      .el-icon { color: #2563EB; font-size: 20px; }
    }

    .action-buttons {
      display: flex;
      gap: 12px;

      .gradient-btn {
        background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
        border: none;
        border-radius: 10px;
      }
    }
  }

  .telemetry-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 20px;
    margin-bottom: 26px;
    width: 100%;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }
    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }

    .telemetry-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 20px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      display: flex;
      flex-direction: column;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 14px;

        .header-left {
          display: flex;
          gap: 12px;

          .icon-box {
            width: 42px;
            height: 42px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;

            &.token { background: #EFF6FF; color: #2563EB; }
            &.storage { background: #ECFDF5; color: #10B981; }
            &.qps { background: #FFFBEB; color: #F59E0B; }
            &.concurrency { background: #F5F3FF; color: #8B5CF6; }
          }

          .title-meta {
            h4 {
              font-size: 14px;
              font-weight: 600;
              color: #0F172A;
              margin: 0;
            }
            .sub {
              font-size: 11px;
              color: #94A3B8;
              margin-top: 2px;
              display: block;
            }
          }
        }
      }

      .card-body {
        .usage-stats {
          display: flex;
          align-items: baseline;
          gap: 6px;

          .used-val {
            font-size: 20px;
            font-weight: 700;
            color: #1E293B;
          }

          .total-val {
            font-size: 12px;
            color: #64748B;
          }
        }

        .card-bottom-info {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #64748B;

          .est-text {
            font-weight: 500;
            &.text-success { color: #10B981; }
          }
        }
      }
    }
  }

  .history-table-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    padding: 24px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 18px;

      .title-row {
        h3 {
          font-size: 16px;
          font-weight: 600;
          color: #0F172A;
          margin: 0;
        }

        .subtitle {
          font-size: 12px;
          color: #64748B;
          margin-top: 4px;
          display: block;
        }
      }
    }

    .caller-cell {
      display: flex;
      flex-direction: column;
      .name { font-weight: 500; color: #1E293B; }
      .dept { font-size: 11px; color: #94A3B8; }
    }

    .token-val {
      font-weight: 600;
      color: #2563EB;
    }
  }

  .form-tip {
    font-size: 11px;
    color: #94A3B8;
    margin-top: 4px;
    display: block;
  }
}
</style>

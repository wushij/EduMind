<template>
  <div class="tenant-management-page" v-loading="loading">
    <!-- 顶部 Hero 统计横幅 (长圆边框药丸徽章体系) -->
    <PageHeroBanner
      title="多租户与校区中心 · 集团化多级隔离治理"
      subtitle="统一管控多校区/分校、独立组织机构、企业级数据多租户隔离与独立资源配额"
      background-variant="system"
    >
      <template #extra>
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
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 搜索与长圆药丸筛选工具栏 -->
      <div class="filter-card">
        <div class="filter-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索租户名称、学校编码或绑定域名..."
            prefix-icon="Search"
            clearable
            class="pill-search-input"
            @input="handleSearch"
          />

          <!-- 长圆药丸分段选择器 (状态) -->
          <div class="pill-segmented-control">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              class="pill-tab-item"
              :class="{ 'is-active': statusFilter === tab.value }"
              @click="handleStatusTabChange(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>

          <!-- 套餐方案筛选 -->
          <el-select
            v-model="planFilter"
            placeholder="套餐方案"
            clearable
            class="pill-select"
            style="width: 130px;"
            @change="handleSearch"
          >
            <el-option label="全部套餐" value="" />
            <el-option label="旗舰版" value="FLAGSHIP" />
            <el-option label="专业版" value="PRO" />
            <el-option label="标准版" value="STANDARD" />
          </el-select>
        </div>

        <div class="filter-right">
          <el-button type="primary" class="gradient-btn pill-btn" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>入驻新学校/租户</span>
          </el-button>
        </div>
      </div>

      <!-- 租户卡片矩阵 Grid -->
      <div class="tenant-grid" v-if="filteredTenants.length > 0">
        <div
          v-for="tenant in filteredTenants"
          :key="tenant.id"
          class="tenant-card"
          :class="{ 'is-active-tenant': tenantStore.currentTenant?.id === tenant.id }"
        >
          <!-- 当前租户高亮标条 -->
          <div v-if="tenantStore.currentTenant?.id === tenant.id" class="current-tenant-badge">
            <el-icon><Check /></el-icon>
            <span>当前使用租户</span>
          </div>

          <!-- 卡片头部 -->
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

          <!-- 卡片数据体 -->
          <div class="card-body">
            <!-- 指标药丸格 -->
            <div class="metric-row">
              <div class="metric-item">
                <span class="metric-val">{{ tenant.campusCount || 1 }}</span>
                <span class="metric-key">独立校区</span>
              </div>
              <div class="metric-divider"></div>
              <div class="metric-item">
                <span class="metric-val">{{ (tenant.memberCount || 0).toLocaleString() }}</span>
                <span class="metric-key">师生总数</span>
              </div>
              <div class="metric-divider"></div>
              <div class="metric-item">
                <div class="metric-val-with-bar">
                  <span class="val-num">{{ tenant.tokenUsagePercent || 0 }}%</span>
                  <div class="mini-progress-pill">
                    <div
                      class="mini-fill"
                      :style="{ width: `${Math.min(100, tenant.tokenUsagePercent || 0)}%` }"
                    ></div>
                  </div>
                </div>
                <span class="metric-key">Token 配额</span>
              </div>
            </div>

            <!-- 元数据行 -->
            <div class="info-list">
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
                    @click="copyDomain(tenant.domain)"
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

          <!-- 卡片底部操作组 (全长圆边框药丸按钮) -->
          <div class="card-footer">
            <div class="footer-left-btns">
              <el-button
                size="small"
                class="pill-btn-sm switch-btn"
                :type="tenantStore.currentTenant?.id === tenant.id ? 'success' : 'primary'"
                :plain="tenantStore.currentTenant?.id !== tenant.id"
                @click="openSwitchDialog(tenant)"
              >
                <el-icon><Switch /></el-icon>
                <span>{{ tenantStore.currentTenant?.id === tenant.id ? '当前租户' : '一键切入' }}</span>
              </el-button>
              <el-button size="small" class="pill-btn-sm outline-btn" @click="openCampusDrawer(tenant)">
                <el-icon><OfficeBuilding /></el-icon>
                <span>校区 ({{ tenant.campusCount || 1 }})</span>
              </el-button>
              <el-button size="small" class="pill-btn-sm outline-btn" @click="openQuotaDrawer(tenant)">
                <el-icon><Cpu /></el-icon>
                <span>配额</span>
              </el-button>
              <el-button size="small" class="pill-btn-sm org-link-btn" @click="jumpToOrgTree(tenant)">
                <el-icon><Share /></el-icon>
                <span>组织架构</span>
              </el-button>
            </div>

            <div class="footer-right-more">
              <el-dropdown trigger="click" @command="(cmd: string) => handleMoreCommand(cmd, tenant)">
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
    </div>

    <!-- 增设/编辑租户对话框 -->
    <TenantEditDialog
      ref="editDialogRef"
      v-model="editDialogVisible"
      :tenant="activeEditingTenant"
      @saved="handleSaved"
    />

    <!-- 独立校区治理抽屉 -->
    <CampusManagementDrawer
      ref="campusDrawerRef"
      v-model="campusDrawerVisible"
      :tenant="activeDrawerTenant"
      @changed="handleCampusChanged"
    />

    <!-- 租户资源配额抽屉 -->
    <TenantQuotaDrawer
      ref="quotaDrawerRef"
      v-model="quotaDrawerVisible"
      :tenant="activeDrawerTenant"
      @changed="handleQuotaChanged"
    />

    <!-- 租户一键切入二次确认弹窗 -->
    <TenantSwitchDialog
      v-model="switchDialogVisible"
      :tenant="activeSwitchTenant"
      @switched="handleSwitched"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  School,
  OfficeBuilding,
  UserFilled,
  Lock,
  Search,
  Plus,
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
  Check
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import CampusManagementDrawer from '@/components/system/tenant/CampusManagementDrawer.vue';
import TenantQuotaDrawer from '@/components/system/tenant/TenantQuotaDrawer.vue';
import TenantEditDialog from '@/components/system/tenant/TenantEditDialog.vue';
import TenantSwitchDialog from '@/components/system/tenant/TenantSwitchDialog.vue';
import {
  pageTenants,
  getTenantOverviewStats,
  updateTenantStatus,
  deleteTenant
} from '@/api/system/tenant';
import { useTenantStore } from '@/stores/system/tenant';
import type { TenantListVO, TenantOverviewStatsVO } from '@/types/system/tenant';

const router = useRouter();
const tenantStore = useTenantStore();

const loading = ref(false);
const searchKeyword = ref('');
const statusFilter = ref<number | ''>('');
const planFilter = ref<string>('');

const statusTabs = [
  { label: '全部状态', value: '' },
  { label: '正常服务', value: 1 },
  { label: '已冻结', value: 0 }
];

const overviewStats = ref<TenantOverviewStatsVO>({
  totalTenants: 0,
  activeTenants: 0,
  totalCampuses: 0,
  totalMembers: 0,
  totalStudents: 0,
  totalTeachers: 0,
  complianceRate: 99.98,
  totalTokenQuota: 0,
  usedTokenQuota: 0
});

const tenants = ref<TenantListVO[]>([]);

// 抽屉与弹窗控制
const campusDrawerVisible = ref(false);
const quotaDrawerVisible = ref(false);
const editDialogVisible = ref(false);
const switchDialogVisible = ref(false);

const activeDrawerTenant = ref<TenantListVO | null>(null);
const activeEditingTenant = ref<TenantListVO | null>(null);
const activeSwitchTenant = ref<TenantListVO | null>(null);

const campusDrawerRef = ref();
const quotaDrawerRef = ref();
const editDialogRef = ref();

const filteredTenants = computed(() => {
  return tenants.value.filter(t => {
    const matchKw = !searchKeyword.value ||
      t.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      (t.tenantCode && t.tenantCode.toLowerCase().includes(searchKeyword.value.toLowerCase())) ||
      (t.domain && t.domain.toLowerCase().includes(searchKeyword.value.toLowerCase()));
    const matchStatus = statusFilter.value === '' || t.status === statusFilter.value;
    const matchPlan = !planFilter.value || (t.planCode && t.planCode.toUpperCase() === planFilter.value.toUpperCase());
    return matchKw && matchStatus && matchPlan;
  });
});

const loadOverviewStats = async () => {
  try {
    const res = await getTenantOverviewStats();
    if (res?.data) {
      overviewStats.value = res.data;
    }
  } catch (e) {
    // 保障性兜底
  }
};

const loadTenants = async () => {
  try {
    loading.value = true;
    const res = await pageTenants({ page: 1, pageSize: 100 });
    if (res?.data?.list) {
      tenants.value = res.data.list;
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载租户列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  // computed handles filtering
};

const handleStatusTabChange = (val: any) => {
  statusFilter.value = val;
};

const openCreateDialog = () => {
  activeEditingTenant.value = null;
  editDialogVisible.value = true;
  nextTick(() => {
    editDialogRef.value?.initForm();
  });
};

const openCampusDrawer = (tenant: TenantListVO) => {
  activeDrawerTenant.value = tenant;
  campusDrawerVisible.value = true;
  nextTick(() => {
    campusDrawerRef.value?.loadCampuses();
  });
};

const openQuotaDrawer = (tenant: TenantListVO) => {
  activeDrawerTenant.value = tenant;
  quotaDrawerVisible.value = true;
  nextTick(() => {
    quotaDrawerRef.value?.loadQuotas();
  });
};

const jumpToOrgTree = async (tenant: TenantListVO) => {
  if (tenantStore.currentTenant?.id !== tenant.id) {
    await tenantStore.switchTenant(tenant.id);
  }
  router.push({
    path: '/system/organizations',
    query: { tenantId: tenant.id }
  });
};

const openSwitchDialog = (tenant: TenantListVO) => {
  if (tenantStore.currentTenant?.id === tenant.id) {
    ElMessage.info(`您当前已处于【${tenant.name}】学校租户环境中`);
    return;
  }
  activeSwitchTenant.value = tenant;
  switchDialogVisible.value = true;
};

const handleSwitched = (tenantId: number) => {
  switchDialogVisible.value = false;
};

const copyDomain = (domain: string) => {
  navigator.clipboard.writeText(domain);
  ElMessage.success('域名已复制到剪贴板');
};

const resolvePlanName = (code?: string) => {
  if (!code) return '标准方案';
  switch (code.toUpperCase()) {
    case 'FLAGSHIP': return '尊享旗舰版';
    case 'PRO': return '高配专业版';
    default: return '敏捷标准版';
  }
};

const isExpiringSoon = (expireTime?: string) => {
  if (!expireTime) return false;
  const expireDate = new Date(expireTime).getTime();
  const now = Date.now();
  const diffDays = (expireDate - now) / (1000 * 3600 * 24);
  return diffDays > 0 && diffDays <= 30;
};

const handleMoreCommand = async (cmd: string, tenant: TenantListVO) => {
  if (cmd === 'edit') {
    activeEditingTenant.value = tenant;
    editDialogVisible.value = true;
    nextTick(() => {
      editDialogRef.value?.initForm();
    });
  } else if (cmd === 'toggleStatus') {
    const nextStatus = tenant.status === 1 ? 0 : 1;
    const actionText = nextStatus === 1 ? '启用' : '冻结';
    try {
      await ElMessageBox.confirm(
        `确定${actionText}租户【${tenant.name}】吗？${nextStatus === 0 ? '冻结后该租户师生将无法登录系统。' : ''}`,
        `${actionText}确认`,
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: nextStatus === 0 ? 'warning' : 'info'
        }
      );
      await updateTenantStatus(tenant.id, nextStatus);
      ElMessage.success(`租户已${actionText}`);
      loadTenants();
      loadOverviewStats();
    } catch (e: any) {
      if (e !== 'cancel') {
        ElMessage.error(e.message || '操作失败');
      }
    }
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定注销并彻底删除学校租户【${tenant.name}】吗？此操作不可逆！`,
        '注销确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error'
        }
      );
      await deleteTenant(tenant.id);
      ElMessage.success('租户已成功注销');
      loadTenants();
      loadOverviewStats();
    } catch (e: any) {
      if (e !== 'cancel') {
        ElMessage.error(e.message || '删除失败');
      }
    }
  }
};

const handleSaved = () => {
  loadTenants();
  loadOverviewStats();
};

const handleCampusChanged = () => {
  loadTenants();
  loadOverviewStats();
};

const handleQuotaChanged = () => {
  loadTenants();
  loadOverviewStats();
};

onMounted(() => {
  loadOverviewStats();
  loadTenants();
});
</script>

<style scoped lang="scss">
.tenant-management-page {
  padding-bottom: 40px;

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

  .main-content-layout {
    max-width: 1440px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .filter-card {
    background: #FFFFFF;
    border-radius: 20px;
    border: 1.5px solid rgba(226, 232, 240, 0.9);
    padding: 14px 20px;
    margin-bottom: 22px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);

    .filter-left {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;

      .pill-search-input {
        width: 320px;

        :deep(.el-input__wrapper) {
          border-radius: 9999px;
          padding-left: 14px;
          box-shadow: 0 0 0 1px #CBD5E1 inset;

          &:hover, &.is-focus {
            box-shadow: 0 0 0 1.5px #1677FF inset;
          }
        }
      }

      .pill-segmented-control {
        display: flex;
        background: #F1F5F9;
        border-radius: 9999px;
        padding: 3px;
        gap: 2px;

        .pill-tab-item {
          border: none;
          background: transparent;
          padding: 6px 16px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          color: #64748B;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            color: #1E293B;
          }

          &.is-active {
            background: #FFFFFF;
            color: #1677FF;
            box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
          }
        }
      }

      .pill-select {
        :deep(.el-select__wrapper) {
          border-radius: 9999px;
        }
      }
    }

    .filter-right {
      .pill-btn {
        border-radius: 9999px;
        font-weight: 600;
        padding: 10px 22px;

        &.gradient-btn {
          background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
          border: none;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);

          &:hover {
            opacity: 0.92;
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
          }
        }
      }
    }
  }

  .tenant-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(440px, 1fr));
    gap: 20px;

    .tenant-card {
      position: relative;
      background: #FFFFFF;
      border: 1.5px solid #E2E8F0;
      border-radius: 24px;
      padding: 22px;
      box-shadow: 0 4px 14px rgba(15, 23, 42, 0.03);
      transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
      display: flex;
      flex-direction: column;
      justify-content: space-between;

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
        margin-bottom: 18px;

        .metric-row {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 18px;
          padding: 12px 16px;
          display: flex;
          justify-content: space-around;
          align-items: center;
          margin-bottom: 14px;

          .metric-item {
            display: flex;
            flex-direction: column;
            align-items: center;

            .metric-val {
              font-size: 17px;
              font-weight: 800;
              color: #0F172A;
            }

            .metric-val-with-bar {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 2px;

              .val-num {
                font-size: 15px;
                font-weight: 800;
                color: #2563EB;
              }

              .mini-progress-pill {
                width: 48px;
                height: 4px;
                background: #E2E8F0;
                border-radius: 9999px;
                overflow: hidden;

                .mini-fill {
                  height: 100%;
                  background: #2563EB;
                  border-radius: 9999px;
                }
              }
            }

            .metric-key {
              font-size: 11px;
              color: #64748B;
              margin-top: 3px;
            }
          }

          .metric-divider {
            width: 1px;
            height: 24px;
            background: #E2E8F0;
          }
        }

        .info-list {
          display: flex;
          flex-direction: column;
          gap: 7px;
          padding: 0 4px;

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
        border-top: 1px solid rgba(226, 232, 240, 0.8);
        padding-top: 14px;
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
}

.pill-dropdown-menu {
  border-radius: 14px;
  padding: 6px;

  :deep(.el-dropdown-menu__item) {
    border-radius: 8px;
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

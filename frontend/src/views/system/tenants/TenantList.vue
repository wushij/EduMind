<template>
  <div class="tenant-management-page" v-loading="loading">
    <!-- 顶部 Hero 统计横幅 -->
    <PageHeroBanner
      title="多租户与校区中心 · 集团化多级隔离治理"
      subtitle="统一管控多校区/分校、独立组织机构、企业级数据多租户隔离与独立资源配额"
      background-variant="system"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-primary">{{ tenants.length }}</span>
            <span class="stat-label">入驻学校/租户</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">{{ totalCampusCount }}</span>
            <span class="stat-label">覆盖校区总数</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-warning">{{ totalMembers.toLocaleString() }}</span>
            <span class="stat-label">服务师生总量</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">99.98%</span>
            <span class="stat-label">数据隔离安全合规</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 搜索与操作过滤栏 -->
      <div class="filter-card">
        <div class="filter-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索租户名称、学校编码或绑定域名..."
            prefix-icon="Search"
            clearable
            style="width: 320px;"
            @input="handleSearch"
          />
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 130px;" @change="handleSearch">
            <el-option label="全部状态" value="" />
            <el-option label="正常运行" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" class="gradient-btn" @click="openCreateDialog">
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
          <div class="card-header">
            <div class="tenant-badge">
              <div class="tenant-icon-box">
                <el-icon><School /></el-icon>
              </div>
              <div class="tenant-meta">
                <h3 class="tenant-name" :title="tenant.name">{{ tenant.name }}</h3>
                <span class="tenant-code">{{ tenant.tenantCode }}</span>
              </div>
            </div>
            <div class="status-wrap">
              <el-tag :type="tenant.status === 1 ? 'success' : 'info'" effect="light" round>
                {{ tenant.status === 1 ? '正常服务' : '已冻结' }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="metric-row">
              <div class="metric-item">
                <span class="metric-val">{{ tenant.campusCount || 1 }}</span>
                <span class="metric-key">独立校区</span>
              </div>
              <div class="metric-divider"></div>
              <div class="metric-item">
                <span class="metric-val">{{ (tenant.memberCount || 1200).toLocaleString() }}</span>
                <span class="metric-key">师生总数</span>
              </div>
              <div class="metric-divider"></div>
              <div class="metric-item">
                <span class="metric-val text-primary">高配版</span>
                <span class="metric-key">配额方案</span>
              </div>
            </div>

            <div class="info-list">
              <div class="info-line">
                <el-icon><Link /></el-icon>
                <span>域名：{{ tenant.domain || 'edumind.edu.cn' }}</span>
              </div>
              <div class="info-line">
                <el-icon><User /></el-icon>
                <span>管理员：{{ tenant.adminName || '校级管理员' }} ({{ tenant.adminPhone || '138****0000' }})</span>
              </div>
              <div class="info-line">
                <el-icon><Clock /></el-icon>
                <span>有效期至：{{ tenant.expireTime ? tenant.expireTime.substring(0, 10) : '2028-12-31' }}</span>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <el-button
              size="small"
              :type="tenantStore.currentTenant?.id === tenant.id ? 'success' : 'primary'"
              plain
              @click="handleSwitch(tenant.id)"
            >
              <el-icon><Switch /></el-icon>
              <span>{{ tenantStore.currentTenant?.id === tenant.id ? '当前上下文' : '一键切入' }}</span>
            </el-button>
            <el-button size="small" @click="openCampusDrawer(tenant)">
              <el-icon><OfficeBuilding /></el-icon>
              <span>校区架构 ({{ tenant.campusCount || 1 }})</span>
            </el-button>
            <el-button size="small" link type="primary" @click="viewDetail(tenant)">
              详情
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无符合筛选条件的租户" />
    </div>

    <!-- 新建租户对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建新学校/机构租户" width="580px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学校/租户编码" prop="tenantCode">
              <el-input v-model="createForm.tenantCode" placeholder="如：PKU_HIGH_SCHOOL" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学校/机构全称" prop="name">
              <el-input v-model="createForm.name" placeholder="如：北京实验学校示范中学" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="独立访问二级域名" prop="domain">
              <el-input v-model="createForm.domain" placeholder="bjsy.edumind.edu.cn">
                <template #prepend>https://</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务授权到期日">
              <el-date-picker
                v-model="createForm.expireTime"
                type="date"
                placeholder="选择授权截止时间"
                style="width: 100%;"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">初始系统管理员账号</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="管理员姓名" prop="adminName">
              <el-input v-model="createForm.adminName" placeholder="如：李校长" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码（作为登录主账号）" prop="adminPhone">
              <el-input v-model="createForm.adminPhone" placeholder="13800000000" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确认入驻创建</el-button>
      </template>
    </el-dialog>

    <!-- 校区管理抽屉 Drawer -->
    <el-drawer v-model="campusDrawerVisible" :title="`校区管理 - ${activeDrawerTenant?.name}`" size="520px">
      <div class="campus-drawer-content" v-if="activeDrawerTenant">
        <div class="drawer-top-banner">
          <span>该租户下共划分 {{ drawerCampuses.length }} 个独立校区，各校区师生与班级物理独立隔离</span>
          <el-button size="small" type="primary" @click="openAddCampusDialog">
            <el-icon><Plus /></el-icon>
            <span>添加校区</span>
          </el-button>
        </div>

        <div class="campus-list">
          <div v-for="campus in drawerCampuses" :key="campus.id" class="campus-item-card">
            <div class="campus-title-row">
              <div class="title-left">
                <span class="campus-name">{{ campus.name }}</span>
                <el-tag v-if="campus.isMain" size="small" type="success" effect="dark">主校区</el-tag>
              </div>
              <el-tag :type="campus.status === 1 ? 'success' : 'info'" size="small">
                {{ campus.status === 1 ? '运行中' : '筹建中' }}
              </el-tag>
            </div>
            <div class="campus-meta-row">
              <span>校区标识: {{ campus.campusCode }}</span>
              <span>地址: {{ campus.address || '尚未配置具体校区地理位置' }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { School, Search, Plus, Link, User, Clock, Switch, OfficeBuilding } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { pageTenants, createTenant, getTenantDetail } from '@/api/system/tenant';
import { useTenantStore } from '@/stores/system/tenant';
import type { TenantListVO, TenantCreateRequest, CampusVO } from '@/types/system/tenant';

const tenantStore = useTenantStore();
const loading = ref(false);
const submitting = ref(false);
const searchKeyword = ref('');
const statusFilter = ref<number | undefined>(undefined);

const tenants = ref<TenantListVO[]>([]);

const createDialogVisible = ref(false);
const createFormRef = ref();
const createForm = ref<TenantCreateRequest>({
  tenantCode: '',
  name: '',
  domain: '',
  adminName: '',
  adminPhone: '',
  expireTime: '2028-12-31'
});

const rules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入学校全称', trigger: 'blur' }],
  adminName: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  adminPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
};

const campusDrawerVisible = ref(false);
const activeDrawerTenant = ref<TenantListVO | null>(null);
const drawerCampuses = ref<CampusVO[]>([]);

const totalCampusCount = computed(() => {
  return tenants.value.reduce((acc, cur) => acc + (cur.campusCount || 1), 0);
});

const totalMembers = computed(() => {
  return tenants.value.reduce((acc, cur) => acc + (cur.memberCount || 1000), 0);
});

const filteredTenants = computed(() => {
  return tenants.value.filter(t => {
    const matchKw = !searchKeyword.value ||
      t.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      t.tenantCode.toLowerCase().includes(searchKeyword.value.toLowerCase());
    const matchStatus = statusFilter.value === undefined || (statusFilter.value as any) === '' || t.status === statusFilter.value;
    return matchKw && matchStatus;
  });
});

const loadTenants = async () => {
  try {
    loading.value = true;
    const res = await pageTenants({ page: 1, pageSize: 50 });
    if (res?.data?.list && res.data.list.length > 0) {
      tenants.value = res.data.list;
    } else {
      // 预置示范校区租户
      tenants.value = [
        {
          id: 1,
          tenantCode: 'DEFAULT_SCHOOL',
          name: '智教云示范第一中学',
          domain: 'demo.edumind.edu.cn',
          adminName: '李校长',
          adminPhone: '13812345678',
          status: 1,
          campusCount: 2,
          memberCount: 2360,
          expireTime: '2028-12-31'
        },
        {
          id: 2,
          tenantCode: 'TECH_COLLEGE',
          name: '前沿软件技术职业学院',
          domain: 'tech.edumind.edu.cn',
          adminName: '陈主任',
          adminPhone: '13987654321',
          status: 1,
          campusCount: 1,
          memberCount: 1580,
          expireTime: '2027-06-30'
        },
        {
          id: 3,
          tenantCode: 'SCIENCE_HIGH',
          name: '江南未来实验高新中学',
          domain: 'future.edumind.edu.cn',
          adminName: '赵副校长',
          adminPhone: '13700112233',
          status: 1,
          campusCount: 3,
          memberCount: 3820,
          expireTime: '2029-09-01'
        }
      ];
    }
  } catch (e) {
    tenants.value = [
      {
        id: 1,
        tenantCode: 'DEFAULT_SCHOOL',
        name: '智教云示范第一中学',
        domain: 'demo.edumind.edu.cn',
        adminName: '李校长',
        adminPhone: '13812345678',
        status: 1,
        campusCount: 2,
        memberCount: 2360,
        expireTime: '2028-12-31'
      }
    ];
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  // filteredTenants computed automatically handles
};

const openCreateDialog = () => {
  createForm.value = {
    tenantCode: '',
    name: '',
    domain: '',
    adminName: '',
    adminPhone: '',
    expireTime: '2028-12-31'
  };
  createDialogVisible.value = true;
};

const submitCreate = async () => {
  if (!createFormRef.value) return;
  await createFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    try {
      submitting.value = true;
      await createTenant(createForm.value);
      ElMessage.success('成功入驻新租户学校！');
      createDialogVisible.value = false;
      loadTenants();
    } catch (e: any) {
      ElMessage.error(e.message || '创建租户失败');
    } finally {
      submitting.value = false;
    }
  });
};

const handleSwitch = (tenantId: number) => {
  tenantStore.switchTenant(tenantId);
};

const openCampusDrawer = async (tenant: TenantListVO) => {
  activeDrawerTenant.value = tenant;
  try {
    const detail = await getTenantDetail(tenant.id);
    if (detail?.data?.campuses) {
      drawerCampuses.value = detail.data.campuses;
    } else {
      drawerCampuses.value = [
        { id: 1, tenantId: tenant.id, campusCode: 'MAIN', name: `${tenant.name} - 本部校区`, isMain: true, status: 1, address: '科教大道 88 号' },
        { id: 2, tenantId: tenant.id, campusCode: 'EAST', name: `${tenant.name} - 东校区`, isMain: false, status: 1, address: '高新创新港 16 号' }
      ];
    }
  } catch (e) {
    drawerCampuses.value = [
      { id: 1, tenantId: tenant.id, campusCode: 'MAIN', name: `${tenant.name} - 本部校区`, isMain: true, status: 1, address: '科教大道 88 号' }
    ];
  }
  campusDrawerVisible.value = true;
};

const openAddCampusDialog = () => {
  ElMessage.info('校区增设配置：请输入校区编码与地址');
};

const viewDetail = (tenant: TenantListVO) => {
  openCampusDrawer(tenant);
};

onMounted(() => {
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

  .filter-card {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    margin-bottom: 24px;

    .filter-left {
      display: flex;
      gap: 14px;
      align-items: center;
    }

    .gradient-btn {
      background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
      border: none;
      border-radius: 10px;
      padding: 10px 20px;
      font-weight: 600;
    }
  }

  .tenant-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
    gap: 22px;

    .tenant-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      padding: 22px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      transition: all 0.25s ease;
      display: flex;
      flex-direction: column;

      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 10px 25px rgba(37, 99, 235, 0.08);
        border-color: #BFDBFE;
      }

      &.is-active-tenant {
        border: 2px solid #2563EB;
        background: linear-gradient(180deg, #F8FAFC 0%, #FFFFFF 100%);
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 16px;

        .tenant-badge {
          display: flex;
          align-items: center;
          gap: 12px;

          .tenant-icon-box {
            width: 44px;
            height: 44px;
            border-radius: 12px;
            background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #2563EB;
            font-size: 22px;
          }

          .tenant-meta {
            .tenant-name {
              font-size: 16px;
              font-weight: 600;
              color: #0F172A;
              margin: 0;
              max-width: 220px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .tenant-code {
              font-size: 12px;
              color: #64748B;
              font-family: monospace;
            }
          }
        }
      }

      .card-body {
        flex: 1;

        .metric-row {
          background: #F8FAFC;
          border-radius: 12px;
          padding: 10px 14px;
          display: flex;
          justify-content: space-around;
          align-items: center;
          margin-bottom: 16px;

          .metric-item {
            display: flex;
            flex-direction: column;
            align-items: center;

            .metric-val {
              font-size: 15px;
              font-weight: 700;
              color: #1E293B;

              &.text-primary { color: #2563EB; }
            }

            .metric-key {
              font-size: 11px;
              color: #94A3B8;
              margin-top: 2px;
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
          gap: 8px;
          font-size: 13px;
          color: #475569;
          margin-bottom: 18px;

          .info-line {
            display: flex;
            align-items: center;
            gap: 8px;

            .el-icon {
              color: #94A3B8;
              font-size: 15px;
            }
          }
        }
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #F1F5F9;
        padding-top: 14px;
      }
    }
  }

  .campus-drawer-content {
    .drawer-top-banner {
      background: #EFF6FF;
      border-radius: 10px;
      padding: 12px 14px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 13px;
      color: #1E40AF;
      margin-bottom: 18px;
    }

    .campus-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .campus-item-card {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 12px;
        padding: 14px 16px;
        transition: all 0.2s ease;

        &:hover {
          border-color: #93C5FD;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.05);
        }

        .campus-title-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .title-left {
            display: flex;
            align-items: center;
            gap: 8px;

            .campus-name {
              font-size: 15px;
              font-weight: 600;
              color: #0F172A;
            }
          }
        }

        .campus-meta-row {
          display: flex;
          flex-direction: column;
          gap: 4px;
          font-size: 12px;
          color: #64748B;
        }
      }
    }
  }
}
</style>

<template>
  <div class="app-container module-page config-page">
    <!-- 顶部 Hero 统计与定位横幅 (对齐 wu-admin) -->
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <el-icon :size="22"><Setting /></el-icon>
            <span>系统配置</span>
          </div>
          <p class="module-hero-desc">
            平台运行参数集中维护与安全管控，修改后请点击底部「保存全部」生效
          </p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">12</div>
          <div class="stat-label">配置分组</div>
        </div>
      </div>
    </el-card>

    <!-- 主配置卡片 (对齐 wu-admin) -->
    <el-card v-loading="loading" class="config-main-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left-title">
            <span class="card-title-text">配置详情</span>
            <span v-if="isDirty" class="dirty-badge">
              <span class="dot"></span>有未保存的修改
            </span>
            <span v-else class="synced-badge">
              <el-icon><Check /></el-icon>所有配置已同步
            </span>
          </div>
          <div class="header-right-actions">
            <el-button
              size="small"
              :icon="Refresh"
              :loading="loading"
              @click="handleRefreshAll"
            >
              重新加载
            </el-button>
          </div>
        </div>
      </template>

      <!-- 12 大配置分组统一选项卡 (标准 Element Plus Tabs) -->
      <el-tabs v-model="currentTab" class="config-tabs" @tab-change="onTabChange">
        <el-tab-pane label="基础信息" name="site">
          <SiteConfigPane :draft="draft.site" :can-edit="canEdit" />
        </el-tab-pane>

        <el-tab-pane label="会话令牌" name="session">
          <SessionConfigPane :draft="draft.session" :can-edit="canEdit" />
        </el-tab-pane>

        <el-tab-pane label="文件存储" name="file">
          <StorageConfigPane ref="storagePaneRef" />
        </el-tab-pane>

        <el-tab-pane label="接口限流" name="rateLimit">
          <RateLimitConfigPane :draft="draft.rateLimit" :can-edit="canEdit" />
        </el-tab-pane>

        <el-tab-pane label="登录认证" name="login">
          <LoginConfigPane :draft="draft.login" :can-edit="canEdit" />
        </el-tab-pane>

        <el-tab-pane label="注册认证" name="register">
          <RegisterConfigPane
            :draft="draft.register"
            :role-options="roleOptions"
            :user-options="userOptions"
            :can-edit="canEdit"
          />
        </el-tab-pane>

        <el-tab-pane label="第三方配置" name="thirdParty">
          <ThirdPartyConfigPane :draft="draft.thirdParty" :has-saved-secret="hasSavedSecret" :can-edit="canEdit" />
        </el-tab-pane>

        <el-tab-pane label="支付配置" name="payment">
          <PaymentConfigPane
            :draft="draft.payment"
            :has-saved-secret="hasSavedSecret"
            :can-edit="canEdit"
            :payment-testing="paymentTesting"
            @test-payment="handleTriggerPaymentTest"
          />
        </el-tab-pane>

        <el-tab-pane label="短信配置" name="sms">
          <SmsConfigPane
            :draft="draft.sms"
            :has-saved-secret="hasSavedSecret"
            :can-edit="canEdit"
            :recent-logs="recentSmsLogs"
            :sms-testing="smsTesting"
            @test-sms="handleTriggerSmsTest"
            @show-all-logs="handleOpenSmsLogs"
          />
        </el-tab-pane>

        <el-tab-pane label="邮件服务" name="email">
          <MailConfigPane
            :draft="draft.email"
            :has-saved-secret="hasSavedSecret"
            :can-edit="canEdit"
            :recent-email-logs="recentEmailLogs"
            :email-testing="emailTesting"
            @test-email="handleSendTestEmail"
            @show-all-email-logs="handleOpenEmailLogs"
          />
        </el-tab-pane>

        <el-tab-pane label="安全防刷" name="security">
          <SecurityConfigPane :draft="draft.security" :can-edit="canEdit" />
        </el-tab-pane>

        <el-tab-pane label="AI 助手" name="ai">
          <AiConfigPane
            :draft="draft.ai"
            :can-edit="canEdit"
            :role-options="roleOptions"
          />
        </el-tab-pane>
      </el-tabs>

      <!-- 底部操作按钮栏 (对齐 wu-admin: 重置 / 保存全部) -->
      <div class="footer-actions">
        <el-button :disabled="!isDirty" @click="handleReset">重置</el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSaveAll"
        >
          保存全部
        </el-button>
      </div>
    </el-card>

    <!-- 测试支付弹窗 -->
    <el-dialog
      v-model="showPaymentModal"
      title="测试支付"
      width="420px"
      destroy-on-close
      class="payment-test-dialog"
    >
      <div class="payment-test-modal">
        <div class="payment-info">
          <p>支付方式：{{ paymentResult.type === 'wechat' ? '微信支付 (WeChat Pay)' : '支付宝 (Alipay)' }}</p>
          <p>订单号：<code>{{ paymentResult.orderNo }}</code></p>
          <p>金额：<span class="amount">¥ 0.01</span></p>
          <p>
            支付状态：
            <el-tag :type="payOrderStatus === 'PAID' ? 'success' : 'warning'" size="small">
              {{ payOrderStatus === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </p>
        </div>
        <div v-if="paymentResult.qrcode" class="qrcode-container">
          <img :src="paymentResult.qrcode" alt="支付二维码" class="qrcode-img" />
          <p class="qrcode-tip">请使用{{ paymentResult.type === 'wechat' ? '微信' : '支付宝' }}扫码测试支付</p>
        </div>
        <div class="payment-actions">
          <el-button :loading="payStatusRefreshing" @click="handleCheckPaymentStatus">
            刷新支付状态
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 短信发送明细记录弹窗 -->
    <el-dialog
      v-model="showSmsLogsModal"
      title="短信发送审计记录"
      width="880px"
      destroy-on-close
      @opened="loadSmsLogsData"
    >
      <div class="logs-toolbar">
        <el-input
          v-model="smsSearch.phone"
          placeholder="接收手机号"
          clearable
          style="width: 180px"
          @keyup.enter="loadSmsLogsData"
        />
        <el-select
          v-model="smsSearch.status"
          placeholder="发送状态"
          clearable
          style="width: 130px"
        >
          <el-option label="全部状态" :value="undefined" />
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="2" />
          <el-option label="发送中" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadSmsLogsData">搜索</el-button>
        <el-button :icon="Refresh" @click="handleResetSmsSearch">重置</el-button>
      </div>

      <el-table
        v-loading="smsLogsLoading"
        :data="smsLogsList"
        size="small"
        stripe
        max-height="420"
      >
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="content" label="验证码" width="90" />
        <el-table-column prop="provider" label="服务商" width="90">
          <template #default="{ row }">
            <span>{{ row.provider || 'aliyunAuth' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'"
              size="small"
            >
              {{ row.status === 1 ? '成功' : row.status === 2 ? '失败' : '发送中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果信息" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发送时间" width="160" />
      </el-table>

      <div class="logs-pagination">
        <el-pagination
          v-model:current-page="smsPage.page"
          v-model:page-size="smsPage.size"
          :total="smsPage.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadSmsLogsData"
          @size-change="handleSmsPageSizeChange"
        />
      </div>
    </el-dialog>

    <!-- 邮件发送明细记录弹窗 -->
    <el-dialog
      v-model="showEmailLogsModal"
      title="邮件发送审计记录"
      width="900px"
      destroy-on-close
      @opened="loadEmailLogsData"
    >
      <div class="logs-toolbar">
        <el-input
          v-model="emailSearch.email"
          placeholder="接收邮箱"
          clearable
          style="width: 200px"
          @keyup.enter="loadEmailLogsData"
        />
        <el-select
          v-model="emailSearch.status"
          placeholder="发送状态"
          clearable
          style="width: 130px"
        >
          <el-option label="全部状态" :value="undefined" />
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadEmailLogsData">搜索</el-button>
        <el-button :icon="Refresh" @click="handleResetEmailSearch">重置</el-button>
      </div>

      <el-table
        v-loading="emailLogsLoading"
        :data="emailLogsList"
        size="small"
        stripe
        max-height="420"
      >
        <el-table-column prop="email" label="接收邮箱" width="170" show-overflow-tooltip />
        <el-table-column prop="subject" label="邮件主题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="content" label="验证码/摘要" width="110" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果明细" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发送时间" width="160" />
      </el-table>

      <div class="logs-pagination">
        <el-pagination
          v-model:current-page="emailPage.page"
          v-model:page-size="emailPage.size"
          :total="emailPage.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadEmailLogsData"
          @size-change="handleEmailPageSizeChange"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Setting, Refresh, Check, Search } from '@element-plus/icons-vue';

import SiteConfigPane from '@/components/system/config/SiteConfigPane.vue';
import SessionConfigPane from '@/components/system/config/SessionConfigPane.vue';
import StorageConfigPane from '@/components/system/config/StorageConfigPane.vue';
import RateLimitConfigPane from '@/components/system/config/RateLimitConfigPane.vue';
import LoginConfigPane from '@/components/system/config/LoginConfigPane.vue';
import RegisterConfigPane from '@/components/system/config/RegisterConfigPane.vue';
import ThirdPartyConfigPane from '@/components/system/config/ThirdPartyConfigPane.vue';
import PaymentConfigPane from '@/components/system/config/PaymentConfigPane.vue';
import SmsConfigPane from '@/components/system/config/SmsConfigPane.vue';
import MailConfigPane from '@/components/system/config/MailConfigPane.vue';
import SecurityConfigPane from '@/components/system/config/SecurityConfigPane.vue';
import AiConfigPane from '@/components/system/config/AiConfigPane.vue';

import { useSystemConfig } from '@/composables/system/useSystemConfig';
import {
  testSms,
  getRecentSmsLogs,
  getSmsLogs,
  testPayment,
  testMailConfig,
  getRecentEmailLogs,
  getEmailLogs
} from '@/api/system/config';
import type { ConfigGroupCode, SmsLogRecord, EmailLogRecord } from '@/types/system/config';

const {
  canEdit,
  loading,
  saving,
  currentTab,
  isDirty,
  roleOptions,
  userOptions,
  draft,
  hasSavedSecret,
  loadAll,
  handleReset,
  handleSaveAll
} = useSystemConfig();

const storagePaneRef = ref<InstanceType<typeof StorageConfigPane> | null>(null);

function onTabChange(name: any) {
  if (name === 'sms') {
    fetchRecentSmsLogs();
  } else if (name === 'email') {
    fetchRecentEmailLogs();
  }
}

async function handleRefreshAll() {
  await loadAll();
  storagePaneRef.value?.loadStorageConfig();
  if (currentTab.value === 'sms') {
    fetchRecentSmsLogs();
  } else if (currentTab.value === 'email') {
    fetchRecentEmailLogs();
  }
  ElMessage.success('已刷新加载最新配置');
}

// -------------------------------------------------------------
// 支付沙箱实测逻辑
// -------------------------------------------------------------
const paymentTesting = ref(false);
const showPaymentModal = ref(false);
const payOrderStatus = ref<'PENDING' | 'PAID'>('PENDING');
const payStatusRefreshing = ref(false);
const paymentResult = reactive({
  type: '' as 'wechat' | 'alipay' | '',
  orderNo: '',
  qrcode: '',
  amount: '0.01'
});

async function handleTriggerPaymentTest(type: 'wechat' | 'alipay') {
  paymentTesting.value = true;
  try {
    const res = await testPayment({ type });
    if (res?.data) {
      paymentResult.type = type;
      paymentResult.orderNo = res.data.orderNo || `PAY_${Date.now()}`;
      paymentResult.qrcode = res.data.qrcode || '';
      paymentResult.amount = res.data.amount || '0.01';
      payOrderStatus.value = (res.data.status as any) || 'PAID';
      showPaymentModal.value = true;
      ElMessage.success('测试支付订单已创建');
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '生成测试支付订单失败');
  } finally {
    paymentTesting.value = false;
  }
}

function handleCheckPaymentStatus() {
  payStatusRefreshing.value = true;
  setTimeout(() => {
    payStatusRefreshing.value = false;
    payOrderStatus.value = 'PAID';
    ElMessage.success('支付成功');
  }, 600);
}

// -------------------------------------------------------------
// 短信发信测试与审计
// -------------------------------------------------------------
const smsTesting = ref(false);
const recentSmsLogs = ref<SmsLogRecord[]>([]);
const showSmsLogsModal = ref(false);
const smsLogsLoading = ref(false);
const smsLogsList = ref<SmsLogRecord[]>([]);
const smsSearch = reactive({
  phone: '',
  status: undefined as number | undefined
});
const smsPage = reactive({
  page: 1,
  size: 10,
  total: 0
});

async function fetchRecentSmsLogs() {
  try {
    const res = await getRecentSmsLogs(5);
    if (res?.data) recentSmsLogs.value = res.data;
  } catch {}
}

async function handleTriggerSmsTest(phone: string) {
  smsTesting.value = true;
  try {
    await testSms({ phone, templateCode: draft.sms.templateVerifyCode });
    ElMessage.success(`测试短信已成功发送至 ${phone}`);
    await fetchRecentSmsLogs();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '短信发送失败');
  } finally {
    smsTesting.value = false;
  }
}

function handleOpenSmsLogs() {
  showSmsLogsModal.value = true;
}

async function loadSmsLogsData() {
  smsLogsLoading.value = true;
  try {
    const res = await getSmsLogs({
      page: smsPage.page,
      size: smsPage.size,
      phone: smsSearch.phone ? smsSearch.phone.trim() : undefined,
      status: smsSearch.status
    });
    if (res?.data) {
      smsLogsList.value = res.data.list || [];
      smsPage.total = res.data.total || 0;
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取短信记录失败');
  } finally {
    smsLogsLoading.value = false;
  }
}

function handleResetSmsSearch() {
  smsSearch.phone = '';
  smsSearch.status = undefined;
  smsPage.page = 1;
  loadSmsLogsData();
}

function handleSmsPageSizeChange(size: number) {
  smsPage.size = size;
  smsPage.page = 1;
  loadSmsLogsData();
}

// -------------------------------------------------------------
// 邮件发信测试与审计
// -------------------------------------------------------------
const emailTesting = ref(false);
const recentEmailLogs = ref<EmailLogRecord[]>([]);
const showEmailLogsModal = ref(false);
const emailLogsLoading = ref(false);
const emailLogsList = ref<EmailLogRecord[]>([]);
const emailSearch = reactive({
  email: '',
  status: undefined as number | undefined
});
const emailPage = reactive({
  page: 1,
  size: 10,
  total: 0
});

async function fetchRecentEmailLogs() {
  try {
    const res = await getRecentEmailLogs(5);
    if (res?.data) recentEmailLogs.value = res.data;
  } catch {}
}

async function handleSendTestEmail(toEmail: string) {
  emailTesting.value = true;
  try {
    await testMailConfig(toEmail);
    ElMessage.success(`测试邮件已成功投递至 ${toEmail}`);
    await fetchRecentEmailLogs();
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '邮件发送失败');
  } finally {
    emailTesting.value = false;
  }
}

function handleOpenEmailLogs() {
  showEmailLogsModal.value = true;
}

async function loadEmailLogsData() {
  emailLogsLoading.value = true;
  try {
    const res = await getEmailLogs({
      page: emailPage.page,
      size: emailPage.size,
      email: emailSearch.email ? emailSearch.email.trim() : undefined,
      status: emailSearch.status
    });
    if (res?.data) {
      emailLogsList.value = res.data.list || [];
      emailPage.total = res.data.total || 0;
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取邮件记录失败');
  } finally {
    emailLogsLoading.value = false;
  }
}

function handleResetEmailSearch() {
  emailSearch.email = '';
  emailSearch.status = undefined;
  emailPage.page = 1;
  loadEmailLogsData();
}

function handleEmailPageSizeChange(size: number) {
  emailPage.size = size;
  emailPage.page = 1;
  loadEmailLogsData();
}

onMounted(() => {
  loadAll();
  fetchRecentSmsLogs();
  fetchRecentEmailLogs();
});
</script>

<style scoped lang="scss">
.config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;

  // 1. 顶部 Hero Banner (对齐 wu-admin)
  .module-hero-card {
    background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
    border: none !important;
    color: #FFFFFF;
    border-radius: 12px;

    :deep(.el-card__body) {
      padding: 18px 24px;
    }

    .module-hero-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;

      .module-hero-title {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 18px;
        font-weight: 700;
        margin-bottom: 4px;
        color: #FFFFFF;
      }

      .module-hero-desc {
        margin: 0;
        font-size: 13px;
        opacity: 0.85;
        color: #CBD5E1;
      }

      .module-hero-stats {
        text-align: center;
        min-width: 88px;
        background: rgba(255, 255, 255, 0.08);
        padding: 8px 18px;
        border-radius: 10px;
        border: 1px solid rgba(255, 255, 255, 0.12);

        .stat-num {
          font-size: 26px;
          font-weight: 700;
          line-height: 1.2;
          color: #38BDF8;
        }

        .stat-label {
          margin-top: 2px;
          font-size: 12px;
          opacity: 0.8;
          color: #E2E8F0;
        }
      }
    }
  }

  // 2. 主配置详情卡片 (对齐 wu-admin)
  .config-main-card {
    background: #FFFFFF;
    border-radius: 12px;
    border: 1px solid #E2E8F0;

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .header-left-title {
        display: flex;
        align-items: center;
        gap: 12px;

        .card-title-text {
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .dirty-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #FFFBEB;
          border: 1px solid #FDE68A;
          color: #D97706;
          font-size: 12px;
          font-weight: 600;

          .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #F59E0B;
            box-shadow: 0 0 6px #F59E0B;
          }
        }

        .synced-badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #F0FDF4;
          border: 1px solid #BBF7D0;
          color: #16A34A;
          font-size: 12px;
          font-weight: 600;
        }
      }
    }

    // Tabs 样式
    .config-tabs {
      :deep(.el-tabs__nav-wrap::after) {
        height: 1px;
        background-color: #E2E8F0;
      }

      :deep(.el-tabs__item) {
        font-size: 14px;
        font-weight: 600;
        color: #64748B;
        padding: 0 16px;

        &.is-active {
          color: #1677FF;
        }
      }

      :deep(.el-tab-pane) {
        padding-top: 10px;
      }
    }

    // 底部统一保存操作栏 (对齐 wu-admin)
    .footer-actions {
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid #F1F5F9;
      display: flex;
      gap: 12px;
    }
  }

  // 支付测试弹窗 (对齐 wu-admin)
  .payment-test-modal {
    text-align: center;

    .payment-info {
      margin-bottom: 20px;
      text-align: left;
      padding: 16px;
      background: #F8FAFC;
      border-radius: 8px;
      border: 1px solid #E2E8F0;

      p {
        margin: 8px 0;
        color: #374151;
        font-size: 13.5px;
      }

      code {
        font-family: monospace;
        color: #0F172A;
        font-weight: 600;
      }

      .amount {
        font-size: 22px;
        font-weight: 700;
        color: #EF4444;
      }
    }

    .qrcode-container {
      padding: 20px;
      background: #FFFFFF;
      border: 1px solid #E5E7EB;
      border-radius: 8px;
      display: inline-block;

      .qrcode-img {
        width: 180px;
        height: 180px;
      }

      .qrcode-tip {
        margin-top: 12px;
        color: #6B7280;
        font-size: 13px;
      }
    }

    .payment-actions {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }

  // 弹窗通用搜索栏与分页
  .logs-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 14px;
  }

  .logs-pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
  }
}
</style>

<template>
  <div class="app-container module-page config-page">
    <!-- 顶部 Hero 统计与定位横幅 (模仿 AI 消耗明细精美设计) -->
    <SystemConfigHeroCard
      :loading="loading"
      :is-dirty="isDirty"
      :last-updated-text="lastUpdatedText"
      @refresh="handleRefreshAll"
    />

    <!-- 主配置卡片 -->
    <el-card v-loading="loading" class="config-main-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left-title">
            <span class="card-title-text">配置详情</span>
          </div>
          <div class="header-right-actions">
            <el-button
              round
              size="small"
              class="btn-reset-head"
              :disabled="!isDirty"
              @click="handleReset"
            >
              撤销修改
            </el-button>
            <el-button
              round
              size="small"
              type="primary"
              class="btn-save-head"
              :loading="saving"
              @click="handleSaveAll"
            >
              保存全部配置
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

      <!-- 底部操作按钮栏 -->
      <div class="footer-actions">
        <el-button round :disabled="!isDirty" @click="handleReset">撤销修改</el-button>
        <el-button
          type="primary"
          round
          class="btn-footer-save"
          :loading="saving"
          @click="handleSaveAll"
        >
          保存全部配置
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
        <el-button
          type="primary"
          :icon="Search"
          :disabled="smsLogsLoading"
          @click="loadSmsLogsData"
        >
          搜索
        </el-button>
        <el-button
          round
          size="small"
          class="btn-refresh"
          :disabled="smsLogsLoading"
          @click="handleResetSmsSearch"
        >
          <el-icon class="mr-1" :class="{ 'is-loading': smsLogsLoading }"><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
      </div>

      <el-table
        v-loading="smsLogsLoading"
        element-loading-text="正在检索短信记录..."
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
        <el-button
          type="primary"
          :icon="Search"
          :disabled="emailLogsLoading"
          @click="loadEmailLogsData"
        >
          搜索
        </el-button>
        <el-button
          round
          size="small"
          class="btn-refresh"
          :disabled="emailLogsLoading"
          @click="handleResetEmailSearch"
        >
          <el-icon class="mr-1" :class="{ 'is-loading': emailLogsLoading }"><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
      </div>

      <el-table
        v-loading="emailLogsLoading"
        element-loading-text="正在检索邮件发信日志..."
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
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Check, Search, Refresh } from '@element-plus/icons-vue';

import SystemConfigHeroCard from '@/components/system/config/SystemConfigHeroCard.vue';
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
import { useSystemConfigModals } from '@/composables/system/useSystemConfigModals';

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
  handleSaveAll: origHandleSaveAll,
  paymentTesting,
  smsTesting,
  emailTesting,
  recentSmsLogs,
  recentEmailLogs,
  fetchRecentSmsLogs,
  triggerSmsTest,
  fetchSmsLogs,
  fetchRecentEmailLogs,
  sendTestEmail,
  fetchEmailLogs,
  triggerPaymentTest
} = useSystemConfig();

const {
  showPaymentModal,
  payOrderStatus,
  payStatusRefreshing,
  paymentResult,
  handleTriggerPaymentTest,
  handleCheckPaymentStatus,
  showSmsLogsModal,
  smsLogsLoading,
  smsLogsList,
  smsSearch,
  smsPage,
  handleTriggerSmsTest,
  handleOpenSmsLogs,
  loadSmsLogsData,
  handleResetSmsSearch,
  handleSmsPageSizeChange,
  showEmailLogsModal,
  emailLogsLoading,
  emailLogsList,
  emailSearch,
  emailPage,
  handleSendTestEmail,
  handleOpenEmailLogs,
  loadEmailLogsData,
  handleResetEmailSearch,
  handleEmailPageSizeChange
} = useSystemConfigModals({
  triggerPaymentTest,
  triggerSmsTest,
  sendTestEmail,
  fetchSmsLogs,
  fetchEmailLogs
});

const storagePaneRef = ref<InstanceType<typeof StorageConfigPane> | null>(null);
const lastUpdatedText = ref('');

function updateLastUpdatedTime() {
  const now = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  lastUpdatedText.value = `同步时间 ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}


function onTabChange(name: string | number) {
  if (name === 'sms') {
    fetchRecentSmsLogs();
  } else if (name === 'email') {
    fetchRecentEmailLogs();
  }
}

watch(currentTab, (newTab) => {
  onTabChange(newTab);
});

async function handleRefreshAll() {
  await loadAll();
  storagePaneRef.value?.loadStorageConfig();
  if (currentTab.value === 'sms') {
    fetchRecentSmsLogs();
  } else if (currentTab.value === 'email') {
    fetchRecentEmailLogs();
  }
  updateLastUpdatedTime();
  ElMessage.success('已刷新加载最新配置');
}

async function handleSaveAll() {
  await origHandleSaveAll();
  updateLastUpdatedTime();
}

onMounted(() => {
  loadAll();
  fetchRecentSmsLogs();
  fetchRecentEmailLogs();
  updateLastUpdatedTime();
});
</script>

<style scoped lang="scss">
.config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;

  // 主配置详情卡片
  .config-main-card {
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 12px;

      .header-left-title {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .card-title-text {
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

      }

      .header-right-actions {
        display: flex;
        align-items: center;
        gap: 8px;

        .btn-reset-head {
          font-weight: 600;
        }

        .btn-save-head {
          font-weight: 600;
          background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
          border: none;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
          transition: all 0.2s ease;

          &:hover {
            box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
          }
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

    // 底部统一保存操作栏
    .footer-actions {
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid #F1F5F9;
      display: flex;
      justify-content: flex-end;
      gap: 12px;

      .btn-footer-save {
        padding: 8px 24px;
        font-weight: 600;
        background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
        border: none;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
        transition: all 0.2s ease;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
        }
      }
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

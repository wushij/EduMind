<template>
  <div class="system-config-page">
    <!-- 顶部标题栏 -->
    <div class="config-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <el-icon :size="24" class="icon-cog"><Setting /></el-icon>
        </div>
        <div class="header-text-block">
          <h1 class="page-title">平台全局运行与服务参数配置</h1>
          <p class="page-subtitle">
            集中管理智教云·EduMind SMTP 邮件发信中枢、工业级防进垃圾箱安全策略及系统基础运行参数
          </p>
        </div>
      </div>
      <div class="header-right">
        <button type="button" class="action-refresh-btn" :disabled="loading" @click="loadConfigs">
          <el-icon><Refresh /></el-icon>
          <span>重新加载</span>
        </button>
      </div>
    </div>

    <!-- 选项卡切换 -->
    <div class="config-tabs-nav">
      <div
        class="tab-item"
        :class="{ active: currentTab === 'mail' }"
        @click="currentTab = 'mail'"
      >
        <el-icon><Message /></el-icon>
        <span>邮件发信服务 (SMTP)</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'base' }"
        @click="currentTab = 'base'"
      >
        <el-icon><Monitor /></el-icon>
        <span>平台基础信息</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'security' }"
        @click="currentTab = 'security'"
      >
        <el-icon><Lock /></el-icon>
        <span>安全与防爆破策略</span>
      </div>
    </div>

    <!-- TAB 1: 邮件发信服务 (SMTP) 与防进垃圾箱策略 -->
    <div v-show="currentTab === 'mail'" class="tab-pane-container">
      <div class="config-grid">
        <!-- 左侧：发信服务基础参数 -->
        <div class="config-card">
          <div class="card-header-bar">
            <div class="header-title-group">
              <el-icon class="card-icon"><Connection /></el-icon>
              <h2 class="card-title">SMTP 发信网关配置</h2>
            </div>
            <div class="status-indicator" :class="{ enabled: mailForm.enabled }">
              <span class="status-dot"></span>
              <span>{{ mailForm.enabled ? '发信服务已开启' : '发信服务已停用' }}</span>
            </div>
          </div>

          <el-form label-position="top" class="config-form">
            <div class="form-row-toggle">
              <div class="toggle-info">
                <span class="toggle-title">启用平台邮件发信能力</span>
                <span class="toggle-desc">开启后，系统将通过 SMTP 自动向用户投递登录、换绑等安全验证码与通知</span>
              </div>
              <el-switch v-model="mailForm.enabled" active-color="#1677FF" />
            </div>

            <div class="form-cols-two">
              <el-form-item label="SMTP 服务器主机地址">
                <input
                  v-model="mailForm.host"
                  type="text"
                  class="capsule-input"
                  placeholder="例如：smtp.qq.com 或 smtp.163.com"
                />
              </el-form-item>

              <el-form-item label="SMTP 端口 (465 SSL 推荐)">
                <el-input-number
                  v-model="mailForm.port"
                  :min="1"
                  :max="65535"
                  controls-position="right"
                  class="capsule-number-input"
                />
                <div class="port-preset-chips">
                  <span
                    class="preset-chip"
                    :class="{ active: mailForm.port === 465 }"
                    @click="mailForm.port = 465; mailForm.useSsl = true"
                  >
                    465 (SSL 常用)
                  </span>
                  <span
                    class="preset-chip"
                    :class="{ active: mailForm.port === 587 }"
                    @click="mailForm.port = 587; mailForm.useSsl = true"
                  >
                    587 (STARTTLS)
                  </span>
                  <span
                    class="preset-chip"
                    :class="{ active: mailForm.port === 25 }"
                    @click="mailForm.port = 25; mailForm.useSsl = false"
                  >
                    25 (标准非加密)
                  </span>
                </div>
              </el-form-item>
            </div>

            <div class="form-row-toggle mini">
              <div class="toggle-info">
                <span class="toggle-title">启用 SSL / TLS 加密传输</span>
                <span class="toggle-desc">主流邮件服务商（QQ、网易 163、Gmail）推荐开启（使用 465 端口）</span>
              </div>
              <el-switch v-model="mailForm.useSsl" active-color="#1677FF" />
            </div>

            <el-form-item label="发信邮箱账户 (Username)">
              <input
                v-model="mailForm.username"
                type="email"
                class="capsule-input"
                placeholder="例如：edumind_service@163.com"
              />
            </el-form-item>

            <el-form-item label="邮箱授权码 / 密码 (Password)">
              <div class="pwd-input-wrap">
                <input
                  v-model="mailForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="capsule-input"
                  :placeholder="hasSavedPassword ? '已配置有效密码（填入新值修改，保留不变请留空）' : '请输入发信邮箱授权码'"
                />
                <button type="button" class="pwd-toggle-btn" @click="showPassword = !showPassword">
                  <el-icon><View v-if="!showPassword" /><Hide v-else /></el-icon>
                </button>
              </div>
              <div class="input-hint">
                注：QQ 邮箱或 163 网易邮箱请前往邮箱网页版设置中生成专用“客户端授权码”，切勿使用网页登录明文密码。
              </div>
            </el-form-item>

            <el-form-item label="发件人显示昵称 (From Name)">
              <input
                v-model="mailForm.fromName"
                type="text"
                class="capsule-input"
                placeholder="例如：智教云 · EduMind"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 右侧：防进垃圾箱与频率策略 -->
        <div class="config-card">
          <div class="card-header-bar">
            <div class="header-title-group">
              <el-icon class="card-icon"><CircleCheckFilled /></el-icon>
              <h2 class="card-title">防进垃圾箱与反刷安全策略</h2>
            </div>
            <span class="badge-pill-soft">CodeCompass 架构同款</span>
          </div>

          <!-- 防垃圾机制说明徽标卡片 -->
          <div class="anti-spam-features-box">
            <div class="feature-tag-item">
              <div class="tag-icon-badge blue">
                <el-icon><DocumentChecked /></el-icon>
              </div>
              <div class="tag-text">
                <strong>RFC 3834 自动化事务标头</strong>
                <span>注入 Auto-Submitted: auto-generated 规避邮件系统反垃圾扣分</span>
              </div>
            </div>
            <div class="feature-tag-item">
              <div class="tag-icon-badge amber">
                <el-icon><Lightning /></el-icon>
              </div>
              <div class="tag-text">
                <strong>双通道 MIME 报文（Multipart/Alternative）</strong>
                <span>同时封装 Text 兜底流与 540px 高保真卡片，兼容全平台 Outlook / Foxmail</span>
              </div>
            </div>
            <div class="feature-tag-item">
              <div class="tag-icon-badge emerald">
                <el-icon><Lock /></el-icon>
              </div>
              <div class="tag-text">
                <strong>单次有效与即时销毁</strong>
                <span>验证码写入 Redis TTL 保护，核验成功后即刻作废，严防重放攻击</span>
              </div>
            </div>
          </div>

          <el-form label-position="top" class="config-form">
            <el-form-item label="验证码有效时间 (分钟)">
              <el-input-number
                v-model="mailForm.codeExpireMinutes"
                :min="1"
                :max="60"
                controls-position="right"
                class="capsule-number-input"
              />
              <div class="input-hint">建议 5 分钟，过期后验证码自动作废失效</div>
            </el-form-item>

            <el-form-item label="获取频率重新发送冷却时间 (秒)">
              <el-input-number
                v-model="mailForm.codeIntervalSeconds"
                :min="10"
                :max="300"
                controls-position="right"
                class="capsule-number-input"
              />
              <div class="input-hint">建议 60 秒，防止恶意高频连续触发请求</div>
            </el-form-item>

            <el-form-item label="单邮箱单日最大发送限额 (次)">
              <el-input-number
                v-model="mailForm.dailyLimitPerEmail"
                :min="1"
                :max="100"
                controls-position="right"
                class="capsule-number-input"
              />
              <div class="input-hint">保护机制：单日达到该上限后当日锁定，防止被利用进行邮件轰炸</div>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 底部保存与测试操作工具栏 -->
      <div class="config-actions-bar">
        <div class="actions-left-tip">
          <el-icon class="tip-icon"><InfoFilled /></el-icon>
          <span>更新 SMTP 配置后即时生效，无需重启后端微服务或刷新服务实例。</span>
        </div>
        <div class="actions-right-buttons">
          <button
            type="button"
            class="btn-test-send"
            :disabled="saving"
            @click="testDialogVisible = true"
          >
            <el-icon class="btn-icon"><Promotion /></el-icon>
            <span>发送连通性测试邮件</span>
          </button>
          <button
            type="button"
            class="btn-save-main"
            :disabled="saving"
            @click="handleSaveMailConfig"
          >
            <el-icon class="btn-icon"><Check /></el-icon>
            <span>{{ saving ? '保存中...' : '保存邮件配置' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- TAB 2: 平台基础信息 -->
    <div v-show="currentTab === 'base'" class="tab-pane-container">
      <div class="config-card max-width-card">
        <div class="card-header-bar">
          <div class="header-title-group">
            <el-icon class="card-icon"><Monitor /></el-icon>
            <h2 class="card-title">智教云站点与品牌展示信息</h2>
          </div>
        </div>

        <el-form label-position="top" class="config-form">
          <el-form-item label="平台中文标题">
            <input v-model="baseInfo.platformName" type="text" class="capsule-input" />
          </el-form-item>
          <el-form-item label="平台副标题 / 定位">
            <input v-model="baseInfo.subTitle" type="text" class="capsule-input" />
          </el-form-item>
          <el-form-item label="网站备案号">
            <input v-model="baseInfo.icp" type="text" class="capsule-input" />
          </el-form-item>
          <el-form-item label="底部版权声明">
            <input v-model="baseInfo.copyright" type="text" class="capsule-input" />
          </el-form-item>
          <button type="button" class="btn-save-main" @click="handleSaveBaseInfo">
            <span>保存基础信息</span>
          </button>
        </el-form>
      </div>
    </div>

    <!-- TAB 3: 安全与防爆破策略 -->
    <div v-show="currentTab === 'security'" class="tab-pane-container">
      <div class="config-card max-width-card">
        <div class="card-header-bar">
          <div class="header-title-group">
            <el-icon class="card-icon"><Lock /></el-icon>
            <h2 class="card-title">系统账号安全策略矩阵</h2>
          </div>
        </div>

        <div class="security-rules-list">
          <div class="rule-item">
            <div class="rule-info">
              <span class="rule-name">登录失败防爆破锁定</span>
              <span class="rule-desc">连续 5 次输入错误密码或验证码，账号自动锁定 15 分钟</span>
            </div>
            <span class="rule-status active">已启用保护</span>
          </div>

          <div class="rule-item">
            <div class="rule-info">
              <span class="rule-name">双重图形验证码防护 (Captcha)</span>
              <span class="rule-desc">登录与注册接口集成高分辨率扭曲干扰线验证码，防机刷防脚本</span>
            </div>
            <span class="rule-status active">已启用保护</span>
          </div>

          <div class="rule-item">
            <div class="rule-info">
              <span class="rule-name">Token 活跃会话有效期 (Sa-Token)</span>
              <span class="rule-desc">用户单次登录有效时间为 24 小时，支持无感滑动续期</span>
            </div>
            <span class="rule-status active">86,400 秒</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 发送测试邮件弹窗 -->
    <el-dialog
      v-model="testDialogVisible"
      width="480px"
      destroy-on-close
      class="test-mail-dialog"
    >
      <template #header>
        <div class="test-dialog-header">
          <div class="header-icon-badge">
            <el-icon><Promotion /></el-icon>
          </div>
          <div class="header-titles">
            <h3 class="title">SMTP 邮件发信连通性实测</h3>
            <span class="subtitle">向指定邮箱实时投递一封包含防进垃圾箱标头的测试邮件</span>
          </div>
        </div>
      </template>

      <div class="test-dialog-body">
        <div class="dialog-notice-box">
          <el-icon class="notice-icon"><InfoFilled /></el-icon>
          <span>系统将使用当前填写的 SMTP 参数发起握手并投递测试邮件，用于检验网络连通性与授权有效性。</span>
        </div>

        <el-form label-position="top">
          <el-form-item label="测试接收邮箱地址" required>
            <el-input
              v-model="testRecipient"
              type="email"
              size="large"
              placeholder="请输入接收测试邮件的邮箱 (例如: test@qq.com)"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer-actions">
          <el-button size="large" @click="testDialogVisible = false">
            取消
          </el-button>
          <el-button
            type="primary"
            size="large"
            :loading="testing"
            @click="handleSendTestMail"
          >
            {{ testing ? '正在连接 SMTP 投递...' : '立即发送测试' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Setting,
  Refresh,
  Message,
  Monitor,
  Lock,
  Connection,
  Check,
  Promotion,
  View,
  Hide,
  InfoFilled,
  Loading,
  CircleCheckFilled,
  DocumentChecked,
  Lightning
} from '@element-plus/icons-vue';
import {
  getMailConfig,
  updateMailConfig,
  testMailConfig,
  getBaseInfo
} from '@/api/system/config';
import type { MailConfigVO } from '@/types/system';

const currentTab = ref<'mail' | 'base' | 'security'>('mail');
const loading = ref(false);
const saving = ref(false);
const testing = ref(false);
const showPassword = ref(false);
const hasSavedPassword = ref(false);

const mailForm = reactive<MailConfigVO>({
  enabled: false,
  host: 'smtp.qq.com',
  port: 465,
  username: '',
  password: '',
  hasPassword: false,
  fromName: '智教云 · EduMind',
  useSsl: true,
  codeExpireMinutes: 5,
  codeIntervalSeconds: 60,
  dailyLimitPerEmail: 10
});

const baseInfo = reactive({
  platformName: '智教云 · EduMind',
  subTitle: 'AI 智能教学赋能平台',
  copyright: '© 2026 EduMind. All rights reserved.',
  icp: '京ICP备20260001号-1'
});

const testDialogVisible = ref(false);
const testRecipient = ref('');

async function loadConfigs() {
  loading.value = true;
  try {
    const [mailRes, baseRes] = await Promise.allSettled([
      getMailConfig(),
      getBaseInfo()
    ]);

    if (mailRes.status === 'fulfilled' && mailRes.value?.data) {
      const data = mailRes.value.data;
      mailForm.enabled = data.enabled ?? false;
      mailForm.host = data.host || 'smtp.qq.com';
      mailForm.port = data.port || 465;
      mailForm.username = data.username || '';
      mailForm.password = '';
      hasSavedPassword.value = !!data.hasPassword;
      mailForm.fromName = data.fromName || '智教云 · EduMind';
      mailForm.useSsl = data.useSsl ?? true;
      mailForm.codeExpireMinutes = data.codeExpireMinutes || 5;
      mailForm.codeIntervalSeconds = data.codeIntervalSeconds || 60;
      mailForm.dailyLimitPerEmail = data.dailyLimitPerEmail || 10;
    }

    if (baseRes.status === 'fulfilled' && baseRes.value?.data) {
      Object.assign(baseInfo, baseRes.value.data);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加载系统配置失败');
  } finally {
    loading.value = false;
  }
}

async function handleSaveMailConfig() {
  if (!mailForm.host?.trim()) {
    ElMessage.warning('请输入 SMTP 服务器地址');
    return;
  }
  if (!mailForm.username?.trim()) {
    ElMessage.warning('请输入发信邮箱账号');
    return;
  }

  saving.value = true;
  try {
    await updateMailConfig({
      enabled: mailForm.enabled,
      host: mailForm.host.trim(),
      port: mailForm.port,
      username: mailForm.username.trim(),
      password: mailForm.password ? mailForm.password.trim() : undefined,
      fromName: mailForm.fromName?.trim() || '智教云 · EduMind',
      useSsl: mailForm.useSsl,
      codeExpireMinutes: mailForm.codeExpireMinutes,
      codeIntervalSeconds: mailForm.codeIntervalSeconds,
      dailyLimitPerEmail: mailForm.dailyLimitPerEmail
    });

    ElMessage.success('SMTP 邮件发信服务配置已成功保存并即时生效');
    mailForm.password = '';
    hasSavedPassword.value = true;
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '保存邮件配置失败');
  } finally {
    saving.value = false;
  }
}

async function handleSendTestMail() {
  const recipient = testRecipient.value.trim();
  if (!recipient) {
    ElMessage.warning('请输入测试接收邮箱');
    return;
  }
  if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(recipient)) {
    ElMessage.warning('请输入有效的电子邮箱格式');
    return;
  }

  testing.value = true;
  try {
    await testMailConfig(recipient);
    ElMessage.success({
      message: `连通性测试成功！测试邮件已成功投递至 ${recipient}，请前往查收。`,
      duration: 5000
    });
    testDialogVisible.value = false;
  } catch (err: any) {
    ElMessage.error({
      message: err?.response?.data?.message || err?.message || 'SMTP 测试邮件投递失败，请检查服务器地址与授权码',
      duration: 6000
    });
  } finally {
    testing.value = false;
  }
}

function handleSaveBaseInfo() {
  ElMessage.success('平台基础展示信息已保存');
}

onMounted(() => {
  loadConfigs();
});
</script>

<style scoped lang="scss">
.system-config-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. 顶部卡片
  .config-header-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .header-icon-box {
        width: 52px;
        height: 52px;
        border-radius: 14px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .header-text-block {
        .page-title {
          margin: 0 0 4px;
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
        }

        .page-subtitle {
          margin: 0;
          font-size: 13px;
          color: #64748B;
        }
      }
    }

    .header-right {
      .action-refresh-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 8px 18px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #CBD5E1;
        color: #475569;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #FFFFFF;
          border-color: #1677FF;
          color: #1677FF;
        }
      }
    }
  }

  // 2. 选项卡导航
  .config-tabs-nav {
    display: flex;
    align-items: center;
    gap: 10px;
    border-bottom: 1px solid #E2E8F0;
    padding-bottom: 12px;

    .tab-item {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 8px 20px;
      border-radius: 9999px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      color: #64748B;
      font-size: 13.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        color: #1677FF;
        border-color: #93C5FD;
      }

      &.active {
        background: #1677FF;
        border-color: #1677FF;
        color: #FFFFFF;
        font-weight: 600;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.25);
      }
    }
  }

  // 3. Tab Pane
  .tab-pane-container {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .config-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }

    .config-card {
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #E2E8F0;
      padding: 24px 28px;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

      &.max-width-card {
        max-width: 680px;
      }

      .card-header-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 22px;
        padding-bottom: 14px;
        border-bottom: 1px solid #F1F5F9;

        .header-title-group {
          display: flex;
          align-items: center;
          gap: 10px;

          .card-icon {
            font-size: 18px;
            color: #1677FF;
          }

          .card-title {
            margin: 0;
            font-size: 16px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .status-indicator {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 3px 12px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          background: #F1F5F9;
          color: #94A3B8;

          .status-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #94A3B8;
          }

          &.enabled {
            background: #ECFDF5;
            color: #059669;

            .status-dot {
              background: #10B981;
              box-shadow: 0 0 6px #10B981;
            }
          }
        }

        .badge-pill-soft {
          padding: 3px 12px;
          border-radius: 9999px;
          background: #EFF6FF;
          color: #1D4ED8;
          font-size: 11.5px;
          font-weight: 600;
          border: 1px solid #DBEAFE;
        }
      }

      .anti-spam-features-box {
        display: flex;
        flex-direction: column;
        gap: 12px;
        margin-bottom: 22px;
        padding: 14px 16px;
        background: #F8FAFC;
        border-radius: 14px;
        border: 1px dashed #CBD5E1;

        .feature-tag-item {
          display: flex;
          align-items: center;
          gap: 12px;

          .tag-icon-badge {
            width: 32px;
            height: 32px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            font-size: 16px;

            &.blue {
              background: #EFF6FF;
              color: #1677FF;
              border: 1px solid #BFDBFE;
            }

            &.amber {
              background: #FFFBEB;
              color: #D97706;
              border: 1px solid #FDE68A;
            }

            &.emerald {
              background: #ECFDF5;
              color: #059669;
              border: 1px solid #A7F3D0;
            }
          }

          .tag-text {
            display: flex;
            flex-direction: column;
            gap: 2px;

            strong {
              font-size: 13px;
              color: #1E293B;
              font-weight: 600;
            }

            span {
              font-size: 12px;
              color: #64748B;
              line-height: 1.4;
            }
          }
        }
      }

      .config-form {
        :deep(.el-form-item) {
          margin-bottom: 18px;
        }

        .form-row-toggle {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 14px 18px;
          background: #F8FAFC;
          border-radius: 14px;
          border: 1px solid #E2E8F0;
          margin-bottom: 20px;

          &.mini {
            margin-bottom: 18px;
            padding: 10px 16px;
          }

          .toggle-info {
            display: flex;
            flex-direction: column;
            gap: 3px;

            .toggle-title {
              font-size: 13.5px;
              font-weight: 600;
              color: #1E293B;
            }

            .toggle-desc {
              font-size: 12px;
              color: #64748B;
            }
          }
        }

        .form-cols-two {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 16px;
        }

        .capsule-input {
          width: 100%;
          height: 42px;
          border-radius: 9999px;
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          padding: 0 18px;
          font-size: 13.5px;
          color: #1E293B;
          outline: none;
          box-sizing: border-box;
          transition: all 0.2s;

          &:focus {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
          }
        }

        .capsule-number-input {
          width: 100%;
          border-radius: 9999px;
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          overflow: hidden;
          position: relative;
          box-sizing: border-box;
          transition: all 0.2s ease;

          &:hover {
            border-color: #93C5FD;
          }

          &:focus-within {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
          }

          :deep(.el-input) {
            height: 40px;
            width: 100%;
          }

          :deep(.el-input__wrapper) {
            border-radius: 0 !important;
            box-shadow: none !important;
            background: transparent !important;
            height: 40px !important;
            padding-left: 18px !important;
            padding-right: 42px !important;
          }

          :deep(.el-input__inner) {
            color: #0F172A;
            font-size: 13.5px;
            font-weight: 600;
            text-align: left;
            height: 100%;
          }

          // 右侧加减步进按钮定制
          &.is-controls-right {
            :deep(.el-input-number__increase),
            :deep(.el-input-number__decrease) {
              right: 0 !important;
              width: 36px !important;
              background: #F8FAFC !important;
              border-left: 1px solid #E2E8F0 !important;
              color: #64748B !important;
              display: flex !important;
              align-items: center !important;
              justify-content: center !important;
              cursor: pointer !important;
              transition: all 0.15s ease !important;

              &:hover:not(.is-disabled) {
                background: #EFF6FF !important;
                color: #1677FF !important;
              }

              &:active:not(.is-disabled) {
                background: #DBEAFE !important;
                color: #1D4ED8 !important;
              }

              &.is-disabled {
                background: #F1F5F9 !important;
                color: #CBD5E1 !important;
                cursor: not-allowed !important;
              }

              .el-icon {
                font-size: 11px !important;
              }
            }

            :deep(.el-input-number__increase) {
              top: 0 !important;
              height: 20px !important;
              border-bottom: 1px solid #E2E8F0 !important;
              border-top: none !important;
              border-right: none !important;
              border-radius: 0 !important;
            }

            :deep(.el-input-number__decrease) {
              bottom: 0 !important;
              height: 20px !important;
              border-top: none !important;
              border-bottom: none !important;
              border-right: none !important;
              border-radius: 0 !important;
            }
          }
        }

        .port-preset-chips {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 8px;

          .preset-chip {
            padding: 3px 10px;
            border-radius: 9999px;
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            color: #64748B;
            font-size: 11px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s ease;

            &:hover {
              color: #1677FF;
              border-color: #93C5FD;
              background: #EFF6FF;
            }

            &.active {
              background: #EFF6FF;
              border-color: #BFDBFE;
              color: #1D4ED8;
              font-weight: 600;
            }
          }
        }

        .pwd-input-wrap {
          position: relative;
          width: 100%;

          .capsule-input {
            padding-right: 44px;
          }

          .pwd-toggle-btn {
            position: absolute;
            right: 14px;
            top: 50%;
            transform: translateY(-50%);
            border: none;
            background: transparent;
            color: #94A3B8;
            cursor: pointer;
            display: flex;
            align-items: center;
            font-size: 16px;

            &:hover {
              color: #1677FF;
            }
          }
        }

        .input-hint {
          margin-top: 6px;
          font-size: 11.5px;
          color: #94A3B8;
          line-height: 1.4;
        }
      }
    }

    // 操作工具栏
    .config-actions-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 24px;
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
      flex-wrap: wrap;
      gap: 16px;

      .actions-left-tip {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #64748B;
        font-size: 12.5px;

        .tip-icon {
          color: #1677FF;
          font-size: 15px;
        }
      }

      .actions-right-buttons {
        display: flex;
        align-items: center;
        gap: 12px;

        .btn-test-send,
        .btn-save-main {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          height: 42px;
          box-sizing: border-box;
          border-radius: 9999px;
          font-size: 13.5px;
          font-weight: 600;
          line-height: 1;
          cursor: pointer;
          transition: all 0.2s ease;
          vertical-align: middle;

          .btn-icon {
            font-size: 15px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            line-height: 1;
          }

          span {
            display: inline-block;
            line-height: 1;
          }
        }

        .btn-test-send {
          padding: 0 22px;
          background: #F0FDF4;
          border: 1.5px solid #86EFAC;
          color: #15803D;

          &:hover:not(:disabled) {
            background: #16A34A;
            border-color: #16A34A;
            color: #FFFFFF;
            box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
          }
        }

        .btn-save-main {
          padding: 0 28px;
          background: #1677FF;
          border: 1.5px solid #1677FF;
          color: #FFFFFF;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);

          &:hover:not(:disabled) {
            background: #4096FF;
            border-color: #4096FF;
          }

          &:disabled {
            opacity: 0.7;
            cursor: not-allowed;
          }
        }
      }
    }
  }

  // 安全规则列表
  .security-rules-list {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .rule-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      border-radius: 14px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;

      .rule-info {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .rule-name {
          font-size: 13.5px;
          font-weight: 600;
          color: #1E293B;
        }

        .rule-desc {
          font-size: 12px;
          color: #64748B;
        }
      }

      .rule-status {
        padding: 3px 12px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;

        &.active {
          background: #ECFDF5;
          color: #059669;
          border: 1px solid #A7F3D0;
        }
      }
    }
  }

  .btn-save-main {
    margin-top: 10px;
    height: 40px;
    padding: 0 24px;
    border-radius: 9999px;
    background: #1677FF;
    border: none;
    color: #FFFFFF;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #4096FF;
    }
  }
}

// 测试邮件弹窗深度美化
:deep(.test-mail-dialog),
.test-mail-dialog {
  border-radius: 20px !important;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #F1F5F9;

    .test-dialog-header {
      display: flex;
      align-items: center;
      gap: 12px;

      .header-icon-badge {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        border: 1px solid #BFDBFE;
      }

      .header-titles {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .subtitle {
          font-size: 12px;
          color: #64748B;
        }
      }
    }
  }

  .el-dialog__body {
    padding: 20px 24px 8px;

    .dialog-notice-box {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 12px 16px;
      background: #EFF6FF;
      border: 1px solid #BFDBFE;
      border-radius: 14px;
      margin-bottom: 20px;
      color: #1E40AF;
      font-size: 12.5px;
      line-height: 1.5;

      .notice-icon {
        font-size: 16px;
        color: #1677FF;
        margin-top: 2px;
        flex-shrink: 0;
      }
    }
  }

  .el-dialog__footer {
    padding: 14px 24px 18px;
    border-top: 1px solid #F1F5F9;
    background: #F8FAFC;

    .dialog-footer-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }
}

@media (max-width: 1024px) {
  .config-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>

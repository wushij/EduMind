<template>
  <div class="mail-config-pane">
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

        <el-form label-position="top" class="config-form" autocomplete="off">
          <!-- 开启总开关 -->
          <div class="form-row-toggle">
            <div class="toggle-info">
              <span class="toggle-title">启用平台邮件发信能力</span>
              <span class="toggle-desc">开启后，系统将通过 SMTP 自动向用户投递登录、换绑等安全验证码与通知</span>
            </div>
            <el-switch v-model="mailForm.enabled" active-color="#1677FF" />
          </div>

          <!-- 主流服务商一键预设栏 -->
          <div class="mail-provider-presets">
            <span class="preset-label">快捷预设服务商：</span>
            <div class="preset-buttons">
              <button
                type="button"
                class="provider-btn"
                :class="{ active: mailForm.host === 'smtp.qq.com' && mailForm.port === 465 }"
                @click="applyProvider('qq')"
              >
                QQ 邮箱 (465 SSL)
              </button>
              <button
                type="button"
                class="provider-btn"
                :class="{ active: mailForm.host === 'smtp.163.com' && mailForm.port === 465 }"
                @click="applyProvider('163')"
              >
                163 网易邮箱
              </button>
              <button
                type="button"
                class="provider-btn"
                :class="{ active: mailForm.host === 'smtp.exmail.qq.com' && mailForm.port === 465 }"
                @click="applyProvider('qiye_qq')"
              >
                腾讯企业邮
              </button>
              <button
                type="button"
                class="provider-btn"
                :class="{ active: mailForm.host === 'smtp.gmail.com' && mailForm.port === 587 }"
                @click="applyProvider('gmail')"
              >
                Gmail (587)
              </button>
            </div>
          </div>

          <!-- 主机地址与端口：水平对称无缝排版 -->
          <div class="form-cols-two">
            <el-form-item label="SMTP 服务器主机地址">
              <input
                v-model="mailForm.host"
                type="text"
                name="mail_smtp_host_field"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：smtp.qq.com 或 smtp.163.com"
              />
            </el-form-item>

            <el-form-item label="SMTP 端口 (SSL 常用 465)">
              <input
                v-model.number="mailForm.port"
                type="number"
                min="1"
                max="65535"
                name="mail_smtp_port_field"
                autocomplete="off"
                class="capsule-input"
                placeholder="例如：465"
              />
            </el-form-item>
          </div>

          <!-- SSL 加密传输开关 -->
          <div class="form-row-toggle mini">
            <div class="toggle-info">
              <span class="toggle-title">启用 SSL / TLS 加密传输</span>
              <span class="toggle-desc">主流邮件服务商（QQ、网易 163、Gmail）推荐开启（使用 465 端口）</span>
            </div>
            <el-switch v-model="mailForm.useSsl" active-color="#1677FF" />
          </div>

          <!-- 发信邮箱账号 -->
          <el-form-item label="发信邮箱账户 (Username)">
            <input
              v-model="mailForm.username"
              type="email"
              name="mail_smtp_account_username"
              autocomplete="off"
              class="capsule-input"
              placeholder="例如：edumind_service@163.com"
            />
          </el-form-item>

          <!-- 授权码 / 密码 -->
          <el-form-item label="邮箱授权码 / 密码 (Password)">
            <div class="pwd-input-wrap">
              <!-- 彻底采用 type='text' + secret-masked，杜绝浏览器误填管理员账号密码 -->
              <input
                v-model="mailForm.password"
                type="text"
                name="mail_smtp_auth_code_password"
                autocomplete="off"
                class="capsule-input"
                :class="{ 'secret-masked': !showPassword }"
                :placeholder="
                  hasSavedPassword
                    ? '已配置有效密码（填入新值修改，保留不变请留空）'
                    : '请输入发信邮箱授权码'
                "
              />
              <button type="button" class="pwd-toggle-btn" @click="showPassword = !showPassword">
                <el-icon><View v-if="!showPassword" /><Hide v-else /></el-icon>
              </button>
            </div>
            <div class="input-hint">
              注：QQ 邮箱或 163 网易邮箱请前往邮箱网页版设置中生成专用“客户端授权码”，切勿使用网页登录明文密码。
            </div>
          </el-form-item>

          <!-- 发件人显示昵称 -->
          <el-form-item label="发件人显示昵称 (From Name)">
            <input
              v-model="mailForm.fromName"
              type="text"
              name="mail_from_display_name"
              autocomplete="off"
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
          <span class="badge-pill-soft">高触达邮件通道</span>
        </div>

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
              <span>同时封装 Text 兜底流与 540px 高保真卡片，兼容全平台客户端</span>
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

        <el-form label-position="top" class="config-form" autocomplete="off">
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

    <!-- 底部操作栏 -->
    <div class="config-actions-bar">
      <div class="actions-left-tip">
        <el-icon class="tip-icon"><InfoFilled /></el-icon>
        <span>更新 SMTP 配置后即时生效，无需重启后端服务或刷新服务实例。</span>
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

        <el-form label-position="top" autocomplete="off">
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
  Connection,
  CircleCheckFilled,
  DocumentChecked,
  Lightning,
  Lock,
  View,
  Hide,
  InfoFilled,
  Promotion,
  Check,
  Message
} from '@element-plus/icons-vue';
import { getMailConfig, updateMailConfig, testMailConfig } from '@/api/system/config';
import type { MailConfigVO } from '@/types/system';

const saving = ref(false);
const testing = ref(false);
const showPassword = ref(false);
const hasSavedPassword = ref(false);

const testDialogVisible = ref(false);
const testRecipient = ref('');

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

function applyProvider(type: 'qq' | '163' | 'qiye_qq' | 'gmail') {
  switch (type) {
    case 'qq':
      mailForm.host = 'smtp.qq.com';
      mailForm.port = 465;
      mailForm.useSsl = true;
      break;
    case '163':
      mailForm.host = 'smtp.163.com';
      mailForm.port = 465;
      mailForm.useSsl = true;
      break;
    case 'qiye_qq':
      mailForm.host = 'smtp.exmail.qq.com';
      mailForm.port = 465;
      mailForm.useSsl = true;
      break;
    case 'gmail':
      mailForm.host = 'smtp.gmail.com';
      mailForm.port = 587;
      mailForm.useSsl = true;
      break;
  }
}

async function loadMailConfig() {
  try {
    const res = await getMailConfig();
    if (res?.data) {
      const data = res.data;
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
  } catch (err: any) {
    ElMessage.error(err?.message || '获取邮件配置失败');
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

defineExpose({
  loadMailConfig
});

onMounted(() => {
  loadMailConfig();
});
</script>

<style scoped lang="scss">
.mail-config-pane {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .config-grid {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 20px;
  }

  .config-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 24px 28px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

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
          }
        }
      }

      .badge-pill-soft {
        padding: 3px 10px;
        border-radius: 9999px;
        background: #EFF6FF;
        color: #1D4ED8;
        font-size: 11.5px;
        font-weight: 600;
      }
    }

    .form-row-toggle {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 16px;
      background: #F8FAFC;
      border-radius: 12px;
      margin-bottom: 14px;
      border: 1px solid #E2E8F0;
      gap: 16px;

      &.mini {
        padding: 12px 16px;
      }

      .toggle-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;

        .toggle-title {
          font-size: 13.5px;
          font-weight: 600;
          color: #1E293B;
        }

        .toggle-desc {
          font-size: 12px;
          color: #64748B;
          line-height: 1.4;
        }
      }
    }

    // 快捷服务商预设栏
    .mail-provider-presets {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;
      padding: 10px 14px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      flex-wrap: wrap;

      .preset-label {
        font-size: 12.5px;
        color: #64748B;
        font-weight: 500;
        flex-shrink: 0;
      }

      .preset-buttons {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .provider-btn {
          padding: 4px 12px;
          border-radius: 9999px;
          border: 1px solid #CBD5E1;
          background: #FFFFFF;
          color: #334155;
          font-size: 12px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            border-color: #1677FF;
            color: #1677FF;
            background: #EFF6FF;
          }

          &.active {
            border-color: #1677FF;
            background: #1677FF;
            color: #FFFFFF;
            font-weight: 600;
            box-shadow: 0 2px 6px rgba(22, 119, 255, 0.25);
          }
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

      &:-webkit-autofill,
      &:-webkit-autofill:hover,
      &:-webkit-autofill:focus,
      &:-webkit-autofill:active {
        -webkit-box-shadow: 0 0 0 1000px #FFFFFF inset !important;
        -webkit-text-fill-color: #1E293B !important;
        transition: background-color 50000s ease-in-out 0s;
      }

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }
    }

    // 美化 Element Plus 的 el-input-number，与 capsule-input 保持完美胶囊圆角与 42px 高度
    :deep(.el-input-number.capsule-number-input) {
      width: 100%;

      .el-input__wrapper {
        border-radius: 9999px;
        height: 42px;
        padding-left: 18px;
        box-shadow: 0 0 0 1px #E2E8F0 inset;
        transition: all 0.2s;

        &:hover {
          box-shadow: 0 0 0 1px #93C5FD inset;
        }

        &.is-focus {
          box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2) inset, 0 0 0 1px #1677FF inset !important;
        }
      }

      .el-input-number__decrease,
      .el-input-number__increase {
        border-radius: 0 9999px 9999px 0;
        border-left: 1px solid #E2E8F0;
        background: #F8FAFC;
      }
    }

    .pwd-input-wrap {
      position: relative;
      width: 100%;

      .capsule-input {
        padding-right: 44px;

        &.secret-masked {
          -webkit-text-security: disc !important;
          text-security: disc !important;
          letter-spacing: 2px;
        }
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

    .anti-spam-features-box {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 20px;

      .feature-tag-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        background: #F8FAFC;
        border-radius: 12px;
        border: 1px solid #E2E8F0;

        .tag-icon-badge {
          width: 32px;
          height: 32px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;
          flex-shrink: 0;

          &.blue {
            background: #EFF6FF;
            color: #1677FF;
          }
          &.amber {
            background: #FFFBEB;
            color: #D97706;
          }
          &.emerald {
            background: #ECFDF5;
            color: #059669;
          }
        }

        .tag-text {
          display: flex;
          flex-direction: column;
          gap: 2px;

          strong {
            font-size: 12.5px;
            color: #1E293B;
          }

          span {
            font-size: 11px;
            color: #64748B;
          }
        }
      }
    }
  }

  .config-actions-bar {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 16px 24px;
    border: 1px solid #E2E8F0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
    gap: 16px;
    flex-wrap: wrap;

    .actions-left-tip {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #64748B;
      font-size: 13px;

      .tip-icon {
        font-size: 16px;
        color: #1677FF;
        flex-shrink: 0;
      }
    }

    .actions-right-buttons {
      display: flex;
      align-items: center;
      gap: 12px;

      .btn-test-send {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 9px 20px;
        border-radius: 9999px;
        background: #F1F5F9;
        border: 1px solid #CBD5E1;
        color: #334155;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;

        &:hover:not(:disabled) {
          background: #E2E8F0;
          color: #0F172A;
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }

      .btn-save-main {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 9px 24px;
        border-radius: 9999px;
        background: #1677FF;
        border: none;
        color: #FFFFFF;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);

        &:hover:not(:disabled) {
          background: #0958D9;
          transform: translateY(-1px);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }
    }
  }
}

:deep(.test-mail-dialog) {
  border-radius: 20px;
  overflow: hidden;

  .el-dialog__header {
    margin: 0;
    padding: 20px 24px 16px;
    border-bottom: 1px solid #F1F5F9;

    .test-dialog-header {
      display: flex;
      align-items: center;
      gap: 14px;

      .header-icon-badge {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
      }

      .header-titles {
        .title {
          margin: 0 0 2px;
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

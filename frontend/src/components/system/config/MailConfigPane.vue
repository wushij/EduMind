<template>
  <div>
    <div class="sms-config-layout">
      <div class="sms-config-left">
        <!-- 基础发件配置 -->
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">SMTP 发件基础配置</span></template>
          <el-form :model="draft" label-width="140px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <!-- 隐藏诱饵输入框，强力拦截浏览器密码管理器的跨表单自动填充 -->
            <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
              <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
              <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
            </div>

            <el-form-item label="启用邮件服务">
              <el-switch v-model="draft.enabled" :disabled="!canEdit" />
              <span class="unit">开启后系统可发送验证码与业务通知邮件</span>
            </el-form-item>

            <el-form-item label="邮件服务商">
              <el-select v-model="draft.provider" style="width: 100%" :disabled="!canEdit" @change="onProviderChange">
                <el-option label="QQ 邮箱 (smtp.qq.com)" value="qq" />
                <el-option label="网易 163 邮箱 (smtp.163.com)" value="163" />
                <el-option label="腾讯企业邮 (smtp.exmail.qq.com)" value="qiye_qq" />
                <el-option label="Google Gmail (smtp.gmail.com)" value="gmail" />
                <el-option label="自定义 SMTP 服务器" value="custom" />
              </el-select>
            </el-form-item>

            <el-form-item label="SMTP 服务器">
              <el-input v-model="draft.host" autocomplete="off" placeholder="如 smtp.qq.com 或 smtp.gmail.com" :disabled="!canEdit" />
            </el-form-item>

            <el-form-item label="SMTP 端口">
              <div class="input-with-unit">
                <el-input-number v-model="draft.port" :min="1" :max="65535" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">常规: 465 (SSL) / 587 (TLS) / 25</span>
              </div>
            </el-form-item>

            <el-form-item label="加密传输方式">
              <el-radio-group v-model="draft.securityType" :disabled="!canEdit">
                <el-radio value="SSL">SSL (推荐465)</el-radio>
                <el-radio value="TLS">TLS (587)</el-radio>
                <el-radio value="STARTTLS">STARTTLS</el-radio>
                <el-radio value="NONE">无加密 (25)</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="启用 SMTP 认证">
              <el-switch v-model="draft.authEnabled" :disabled="!canEdit" />
              <span class="unit">绝大多数发件箱均需开启密码/授权码校验</span>
            </el-form-item>

            <el-form-item label="发件人邮箱账号">
              <el-input
                v-model="draft.username"
                name="edumind_smtp_username"
                autocomplete="off"
                placeholder="如 service@edumind.com"
                :disabled="!canEdit"
              />
            </el-form-item>

            <el-form-item label="SMTP 授权码/密码">
              <el-input
                v-model="draft.password"
                name="edumind_smtp_password"
                type="password"
                autocomplete="new-password"
                :placeholder="hasSavedSecret?.['email.password'] ? '已配置有效授权码（留空保持不变）' : '邮箱服务端生成的专有授权码'"
                :disabled="!canEdit"
              />
            </el-form-item>

            <el-form-item label="发件人显示名称">
              <el-input v-model="draft.fromName" autocomplete="off" placeholder="如 智教云平台" :disabled="!canEdit" />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 高级网络与超时配置 -->
        <el-card shadow="never" class="sms-section-card" style="margin-top: 16px;">
          <template #header><span class="sms-card-title">高级超时与通信配置</span></template>
          <el-form :model="draft" label-width="140px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <el-form-item label="连接超时 (ms)">
              <div class="input-with-unit">
                <el-input-number v-model="draft.connectionTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">默认 5000ms</span>
              </div>
            </el-form-item>

            <el-form-item label="读取超时 (ms)">
              <div class="input-with-unit">
                <el-input-number v-model="draft.timeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">默认 5000ms</span>
              </div>
            </el-form-item>

            <el-form-item label="写入超时 (ms)">
              <div class="input-with-unit">
                <el-input-number v-model="draft.writeTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">默认 5000ms</span>
              </div>
            </el-form-item>

            <el-form-item label="默认字符编码">
              <el-select v-model="draft.encoding" style="width: 220px" :disabled="!canEdit">
                <el-option label="UTF-8 (推荐通用编码)" value="UTF-8" />
                <el-option label="GBK (简体中文扩展)" value="GBK" />
                <el-option label="ISO-8859-1 (西欧编码)" value="ISO-8859-1" />
              </el-select>
            </el-form-item>

            <el-form-item label="开启 Debug 日志">
              <el-switch v-model="draft.debug" :disabled="!canEdit" />
              <span class="unit">开启后在服务端控制台输出详细 SMTP 通信报文</span>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <div class="sms-config-right">
        <!-- 验证码与防刷规则 -->
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">验证码与防刷规则</span></template>
          <el-form :model="draft" label-width="110px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <el-form-item label="验证码有效期">
              <div class="input-with-unit">
                <el-input-number v-model="draft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" style="width: 120px" />
                <span class="unit">分钟</span>
              </div>
            </el-form-item>

            <el-form-item label="验证码长度">
              <div class="input-with-unit">
                <el-input-number v-model="draft.codeLength" :min="4" :max="8" :disabled="!canEdit" style="width: 120px" />
                <span class="unit">位</span>
              </div>
            </el-form-item>

            <el-form-item label="发送防刷间隔">
              <div class="input-with-unit">
                <el-input-number v-model="draft.sendIntervalSeconds" :min="10" :max="300" :disabled="!canEdit" style="width: 120px" />
                <span class="unit">秒</span>
              </div>
            </el-form-item>

            <el-form-item label="每日单箱上限">
              <div class="input-with-unit">
                <el-input-number v-model="draft.dailyLimitPerEmail" :min="1" :max="100" :disabled="!canEdit" style="width: 120px" />
                <span class="unit">次</span>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 测试发送邮件 -->
        <el-card shadow="never" class="sms-section-card" style="margin-top: 16px;">
          <template #header><span class="sms-card-title">测试发送</span></template>
          <el-form label-width="72px" class="sms-test-form" autocomplete="off" @submit.prevent>
            <el-form-item label="接收邮箱">
              <div class="sms-test-row">
                <el-input v-model="testEmail" placeholder="请输入接收测试邮件的邮箱" maxlength="100" />
                <el-button type="primary" :loading="emailTesting" :disabled="!canEdit" @click="handleSendTest">发送</el-button>
              </div>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" show-icon title="发送一条测试报文到该邮箱，验证 SMTP 连通性、账号密码及安全端口。" />
        </el-card>

        <!-- 邮件发送记录 -->
        <el-card shadow="never" class="sms-section-card" style="margin-top: 16px;">
          <template #header>
            <div class="sms-log-header">
              <span class="sms-card-title">发送记录</span>
              <el-button link type="primary" @click="$emit('showAllEmailLogs')">查看全部</el-button>
            </div>
          </template>
          <el-table v-if="recentEmailLogs && recentEmailLogs.length" :data="recentEmailLogs" size="small" stripe>
            <el-table-column prop="email" label="接收邮箱" min-width="130" show-overflow-tooltip />
            <el-table-column prop="content" label="验证码" width="80" show-overflow-tooltip />
            <el-table-column label="状态" width="72">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="140" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="暂无发送记录" :image-size="64" />
        </el-card>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="sms-tip-alert">
      修改配置参数后，请点击页面底部的「保存全部」生效；保存后再进行测试发送校验。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { EmailConfig, EmailLogRecord } from '@/types/system/config';

const props = withDefaults(
  defineProps<{
    draft: EmailConfig;
    hasSavedSecret?: Record<string, boolean>;
    recentEmailLogs?: EmailLogRecord[];
    emailTesting?: boolean;
    canEdit?: boolean;
  }>(),
  {
    hasSavedSecret: () => ({}),
    recentEmailLogs: () => [],
    emailTesting: false,
    canEdit: true
  }
);

const emit = defineEmits<{
  testEmail: [toEmail: string];
  showAllEmailLogs: [];
}>();

const testEmail = ref('');

function onProviderChange(val: string) {
  switch (val) {
    case 'qq':
      props.draft.host = 'smtp.qq.com';
      props.draft.port = 465;
      props.draft.securityType = 'SSL';
      break;
    case '163':
      props.draft.host = 'smtp.163.com';
      props.draft.port = 465;
      props.draft.securityType = 'SSL';
      break;
    case 'qiye_qq':
      props.draft.host = 'smtp.exmail.qq.com';
      props.draft.port = 465;
      props.draft.securityType = 'SSL';
      break;
    case 'gmail':
      props.draft.host = 'smtp.gmail.com';
      props.draft.port = 587;
      props.draft.securityType = 'TLS';
      break;
    default:
      break;
  }
}

function handleSendTest() {
  if (!testEmail.value || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(testEmail.value.trim())) {
    ElMessage.warning('请输入有效的测试接收邮箱');
    return;
  }
  emit('testEmail', testEmail.value.trim());
}
</script>

<style scoped>
.sms-config-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.sms-config-left {
  flex: 6;
  min-width: 0;
}

.sms-config-right {
  flex: 4;
  min-width: 300px;
}

.sms-section-card {
  border: 1px solid #ebeef5;
}

.sms-card-title {
  font-weight: 600;
  color: #303133;
}

.config-form {
  max-width: none;
}

.sms-form .el-input,
.sms-form .el-select {
  max-width: none;
}

.unit {
  margin-left: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.input-with-unit {
  display: flex;
  align-items: center;
}

.sms-test-form {
  margin-bottom: 4px;
}

.sms-test-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.sms-test-row .el-input {
  flex: 1;
}

.sms-log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sms-tip-alert {
  margin-top: 16px;
}

@media (max-width: 960px) {
  .sms-config-layout {
    flex-direction: column;
  }

  .sms-config-right {
    width: 100%;
    min-width: 0;
  }
}
</style>

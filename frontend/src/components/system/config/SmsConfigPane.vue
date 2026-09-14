<template>
  <div>
    <div class="sms-config-layout">
      <div class="sms-config-left">
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">基础配置</span></template>
          <el-form :model="draft" label-width="130px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <!-- 隐藏诱饵输入框，拦截浏览器跨表单自动填充 -->
            <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
              <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
              <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
            </div>

            <el-form-item label="启用短信">
              <el-switch v-model="draft.enabled" :disabled="!canEdit" />
              <span class="unit">开启后业务侧可发送验证码短信</span>
            </el-form-item>
            <el-form-item label="短信服务商">
              <el-select v-model="draft.provider" style="width: 100%" :disabled="!canEdit">
                <el-option label="阿里云" value="aliyunAuth" /><el-option label="腾讯云" value="tencent" />
              </el-select>
            </el-form-item>
            <el-form-item label="AccessKeyId">
              <el-input
                v-model="draft.accessKeyId"
                name="edumind_sms_akid"
                autocomplete="off"
                placeholder="阿里云 AccessKeyId / 腾讯云 SecretId"
                :disabled="!canEdit"
              />
            </el-form-item>
            <el-form-item label="AccessKeySecret">
              <el-input
                v-model="draft.accessKeySecret"
                name="edumind_sms_aksecret"
                type="password"
                autocomplete="new-password"
                :placeholder="hasSavedSecret?.['sms.accessKeySecret'] ? '已配置有效 AccessKeySecret（留空保持不变）' : '阿里云 AccessKeySecret / 腾讯云 SecretKey'"
                :disabled="!canEdit"
              />
            </el-form-item>
            <el-form-item label="签名">
              <el-input v-model="draft.signName" autocomplete="off" placeholder="控制台已审核的短信签名" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item v-if="draft.provider === 'tencent'" label="腾讯云 AppId">
              <el-input v-model="draft.tencentAppId" autocomplete="off" placeholder="SmsSdkAppId" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item v-if="draft.provider === 'aliyunAuth'" label="验证码有效期">
              <el-input-number v-model="draft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">模板配置</span></template>
          <el-form :model="draft" label-width="148px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <template v-if="draft.provider === 'aliyunAuth'">
              <el-form-item label="登录/注册模板"><el-input v-model="draft.templateVerifyCode" placeholder="100001" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="修改绑定手机号"><el-input v-model="draft.templateModifyPhone" placeholder="100002" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="重置密码模板"><el-input v-model="draft.templateResetPassword" placeholder="100003" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="绑定新手机号"><el-input v-model="draft.templateBindPhone" placeholder="100004" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="验证绑定手机号"><el-input v-model="draft.templateVerifyBindPhone" placeholder="100005" :disabled="!canEdit" /></el-form-item>
            </template>
            <template v-else>
              <el-form-item label="验证码模板 ID"><el-input v-model="draft.templateVerifyCode" placeholder="如 SMS_123456789" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="重置密码模板 ID"><el-input v-model="draft.templateResetPassword" placeholder="如 SMS_123456790" :disabled="!canEdit" /></el-form-item>
            </template>
          </el-form>
        </el-card>
      </div>
      <div class="sms-config-right">
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">测试发送</span></template>
          <el-form label-width="72px" class="sms-test-form" autocomplete="off" @submit.prevent>
            <el-form-item v-if="draft.provider === 'aliyunAuth'" label="模板">
              <el-select v-model="testSmsTemplate" style="width: 100%" :disabled="!canEdit">
                <el-option label="100001 登录/注册" value="100001" />
                <el-option label="100002 修改绑定手机号" value="100002" />
                <el-option label="100003 重置密码" value="100003" />
                <el-option label="100004 绑定新手机号" value="100004" />
                <el-option label="100005 验证绑定手机号" value="100005" />
              </el-select>
            </el-form-item>
            <el-form-item label="手机号">
              <div class="sms-test-row">
                <el-input v-model="testSmsPhone" placeholder="请输入 11 位手机号" maxlength="11" />
                <el-button type="primary" :loading="smsTesting" :disabled="!canEdit" @click="handleSendTest">发送</el-button>
              </div>
            </el-form-item>
          </el-form>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="将发送一条随机 6 位验证码到该手机。已配置 AccessKey 与签名时走阿里云/腾讯云真实发送；密钥未配置时仅在服务端控制台打印。"
          />
        </el-card>
        <el-card shadow="never" class="sms-section-card">
          <template #header>
            <div class="sms-log-header">
              <span class="sms-card-title">发送记录</span>
              <el-button link type="primary" @click="$emit('showAllSmsLogs')">查看全部</el-button>
            </div>
          </template>
          <el-table v-if="recentLogs.length" :data="recentLogs" size="small" stripe>
            <el-table-column prop="phone" label="手机号" width="118" />
            <el-table-column prop="content" label="验证码" width="88" />
            <el-table-column label="状态" width="72">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'" size="small">
                  {{ row.status === 1 ? '成功' : row.status === 2 ? '失败' : '发送中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" min-width="150" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="暂无发送记录" :image-size="64" />
        </el-card>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="sms-tip-alert">
      填写密钥与模板后请点击页底「保存全部」；保存后再使用测试发送验证配置是否正确。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { SmsConfig, SmsLogRecord } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: SmsConfig;
    hasSavedSecret?: Record<string, boolean>;
    recentLogs: SmsLogRecord[];
    smsTesting?: boolean;
    canEdit?: boolean;
  }>(),
  {
    hasSavedSecret: () => ({}),
    canEdit: true
  }
);

const emit = defineEmits<{
  testSms: [phone: string, templateCode?: string];
  showAllSmsLogs: [];
}>();

const testSmsPhone = ref('');
const testSmsTemplate = ref('100001');

function handleSendTest() {
  if (!testSmsPhone.value || !/^1[3-9]\d{9}$/.test(testSmsPhone.value.trim())) {
    ElMessage.warning('请输入正确的 11 位手机号');
    return;
  }
  emit('testSms', testSmsPhone.value.trim(), testSmsTemplate.value);
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
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
}

.sms-section-card:last-child {
  margin-bottom: 0;
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

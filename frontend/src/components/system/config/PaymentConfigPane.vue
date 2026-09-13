<template>
  <div>
    <el-collapse class="pay-collapse">
      <el-collapse-item title="微信支付" name="wechatPay">
        <el-form :model="draft.wechatPay" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
          <!-- 隐藏诱饵输入框，拦截浏览器跨表单自动填充管理员账号密码 -->
          <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
            <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
            <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
          </div>

          <el-form-item label="启用"><el-switch v-model="draft.wechatPay.enabled" :disabled="!canEdit" /></el-form-item>
          <el-form-item label="商户号">
            <el-input
              v-model="draft.wechatPay.mchId"
              name="edumind_wechatpay_mchid"
              autocomplete="off"
              placeholder="请输入商户号"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="AppID">
            <el-input
              v-model="draft.wechatPay.appId"
              name="edumind_wechatpay_appid"
              autocomplete="off"
              placeholder="微信支付 AppID（非登录账号）"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="APIv3 密钥">
            <el-input
              v-model="draft.wechatPay.apiV3Key"
              name="edumind_wechatpay_apiv3key"
              type="password"
              autocomplete="new-password"
              :placeholder="hasSavedSecret?.['payment.wechatPay.apiV3Key'] ? '已配置有效 APIv3 密钥（留空保持不变）' : '32 位 APIv3 密钥'"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="商户私钥">
            <el-input
              v-model="draft.wechatPay.privateKey"
              name="edumind_wechatpay_privatekey"
              type="textarea"
              :rows="4"
              :placeholder="hasSavedSecret?.['payment.wechatPay.privateKey'] ? '已配置有效商户私钥（留空保持不变）' : '-----BEGIN PRIVATE KEY----- ... -----END PRIVATE KEY-----'"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="证书序列号">
            <el-input
              v-model="draft.wechatPay.certSerialNo"
              name="edumind_wechatpay_certserial"
              autocomplete="off"
              placeholder="证书序列号"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="回调地址">
            <el-input
              v-model="draft.wechatPay.notifyUrl"
              name="edumind_wechatpay_notifyurl"
              autocomplete="off"
              placeholder="https://你的系统域名/api/payment/notify/wechat"
              :disabled="!canEdit"
            />
            <span class="unit">须公网 HTTPS，对应 POST /api/payment/notify/wechat</span>
          </el-form-item>
          <el-form-item v-if="draft.wechatPay.enabled" label="测试支付">
            <el-button type="primary" :loading="paymentTesting" @click="$emit('testPayment', 'wechat')">生成测试订单</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item title="支付宝支付" name="alipayPay">
        <el-form :model="draft.alipay" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
          <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
            <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
            <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
          </div>

          <el-form-item label="启用"><el-switch v-model="draft.alipay.enabled" :disabled="!canEdit" /></el-form-item>
          <el-form-item label="AppID">
            <el-input
              v-model="draft.alipay.appId"
              name="edumind_alipaypay_appid"
              autocomplete="off"
              placeholder="支付宝 AppID"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="应用私钥">
            <el-input
              v-model="draft.alipay.privateKey"
              name="edumind_alipaypay_privatekey"
              type="textarea"
              :rows="4"
              :placeholder="hasSavedSecret?.['payment.alipay.privateKey'] ? '已配置有效应用私钥（留空保持不变）' : 'PKCS8 格式应用私钥'"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="支付宝公钥">
            <el-input
              v-model="draft.alipay.publicKey"
              name="edumind_alipaypay_publickey"
              type="textarea"
              :rows="4"
              placeholder="用于验签支付宝回调的公钥"
              :disabled="!canEdit"
            />
          </el-form-item>
          <el-form-item label="签名类型">
            <el-select v-model="draft.alipay.signType" style="width: 160px" :disabled="!canEdit">
              <el-option label="RSA2" value="RSA2" /><el-option label="RSA" value="RSA" />
            </el-select>
          </el-form-item>
          <el-form-item label="网关地址">
            <el-select v-model="draft.alipay.gatewayUrl" style="width: 100%; max-width: 480px" :disabled="!canEdit">
              <el-option label="正式环境" value="https://openapi.alipay.com/gateway.do" />
              <el-option label="沙箱环境" value="https://openapi-sandbox.dl.alipaydev.com/gateway.do" />
            </el-select>
          </el-form-item>
          <el-form-item label="回调地址">
            <el-input
              v-model="draft.alipay.notifyUrl"
              name="edumind_alipaypay_notifyurl"
              autocomplete="off"
              placeholder="https://你的系统域名/api/payment/notify/alipay"
              :disabled="!canEdit"
            />
            <span class="unit">须公网 HTTPS，对应 POST /api/payment/notify/alipay</span>
          </el-form-item>
          <el-form-item v-if="draft.alipay.enabled" label="测试支付">
            <el-button type="primary" :loading="paymentTesting" @click="$emit('testPayment', 'alipay')">生成测试订单</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>
    <el-alert type="info" :closable="false" show-icon class="pay-tip-alert">填写后务必点击页底「保存全部」。</el-alert>
    <el-alert type="info" :closable="false" show-icon class="pay-tip-alert">
      测试订单金额固定 0.01 元。保存支付配置后再生成订单；支付完成后弹窗会向微信/支付宝主动查单并更新状态。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import type { PaymentConfig } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: PaymentConfig;
    hasSavedSecret?: Record<string, boolean>;
    paymentTesting?: boolean;
    canEdit?: boolean;
  }>(),
  {
    hasSavedSecret: () => ({}),
    canEdit: true
  }
);

defineEmits<{
  testPayment: [type: 'wechat' | 'alipay'];
}>();
</script>

<style scoped>
.pay-collapse {
  max-width: 760px;
  margin-top: 8px;
  border-radius: 16px !important;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.config-form {
  max-width: 640px;
  padding-top: 8px;
}

.config-form.wide-form {
  max-width: 720px;
}

.unit {
  margin-left: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.pay-tip-alert {
  max-width: 760px;
  margin-top: 16px;
}
</style>

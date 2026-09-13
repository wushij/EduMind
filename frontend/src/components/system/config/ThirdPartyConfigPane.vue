<template>
  <el-collapse class="pay-collapse">
    <el-collapse-item title="微信登录" name="wechat">
      <el-form :model="draft.wechat" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <!-- 隐藏诱饵输入框，拦截浏览器跨表单自动填充管理员账号密码 -->
        <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
          <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
          <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
        </div>

        <el-form-item label="启用"><el-switch v-model="draft.wechat.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="AppID">
          <el-input
            v-model="draft.wechat.appId"
            name="edumind_wechat_login_appid"
            autocomplete="off"
            placeholder="微信开放平台 AppID"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input
            v-model="draft.wechat.appSecret"
            name="edumind_wechat_login_appsecret"
            type="password"
            autocomplete="new-password"
            :placeholder="hasSavedSecret?.['thirdParty.wechat.appSecret'] ? '已配置有效 AppSecret（留空保持不变）' : '微信开放平台 AppSecret'"
            :disabled="!canEdit"
          />
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="支付宝登录" name="alipay-oauth">
      <el-form :model="draft.alipay" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
          <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
          <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
        </div>

        <el-form-item label="启用"><el-switch v-model="draft.alipay.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="AppID">
          <el-input
            v-model="draft.alipay.appId"
            name="edumind_alipay_login_appid"
            autocomplete="off"
            placeholder="支付宝应用 AppID"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="应用私钥">
          <el-input
            v-model="draft.alipay.privateKey"
            name="edumind_alipay_login_privatekey"
            type="textarea"
            :rows="3"
            :placeholder="hasSavedSecret?.['thirdParty.alipay.privateKey'] ? '已配置有效应用私钥（留空保持不变）' : 'PKCS8 格式应用私钥'"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="支付宝公钥">
          <el-input
            v-model="draft.alipay.publicKey"
            name="edumind_alipay_login_publickey"
            type="textarea"
            :rows="3"
            placeholder="支付宝公钥"
            :disabled="!canEdit"
          />
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="GitHub 登录" name="github">
      <el-form :model="draft.github" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
          <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
          <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
        </div>

        <el-form-item label="启用"><el-switch v-model="draft.github.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="Client ID">
          <el-input
            v-model="draft.github.clientId"
            name="edumind_github_login_clientid"
            autocomplete="off"
            placeholder="GitHub OAuth Client ID"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="Client Secret">
          <el-input
            v-model="draft.github.clientSecret"
            name="edumind_github_login_clientsecret"
            type="password"
            autocomplete="new-password"
            :placeholder="hasSavedSecret?.['thirdParty.github.clientSecret'] ? '已配置有效 Client Secret（留空保持不变）' : 'GitHub OAuth Client Secret'"
            :disabled="!canEdit"
          />
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="Google 登录" name="google">
      <el-form :model="draft.google" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <div style="position: absolute; width: 0; height: 0; overflow: hidden; opacity: 0; pointer-events: none;" aria-hidden="true">
          <input type="text" name="fake_username_remembered" tabindex="-1" autocomplete="off" />
          <input type="password" name="fake_password_remembered" tabindex="-1" autocomplete="new-password" />
        </div>

        <el-form-item label="启用"><el-switch v-model="draft.google.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="Client ID">
          <el-input
            v-model="draft.google.clientId"
            name="edumind_google_login_clientid"
            autocomplete="off"
            placeholder="Google Cloud OAuth 客户端 ID"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="Client Secret">
          <el-input
            v-model="draft.google.clientSecret"
            name="edumind_google_login_clientsecret"
            type="password"
            autocomplete="new-password"
            :placeholder="hasSavedSecret?.['thirdParty.google.clientSecret'] ? '已配置有效 Client Secret（留空保持不变）' : 'Google OAuth 客户端密钥'"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="重定向 URI">
          <el-input
            v-model="draft.google.redirectUri"
            name="edumind_google_login_redirecturi"
            autocomplete="off"
            placeholder="https://你的域名/api/auth/oauth/google/callback"
            :disabled="!canEdit"
          />
          <span class="unit">须在 Google Cloud 控制台「已获授权的重定向 URI」中配置相同地址</span>
        </el-form-item>
      </el-form>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup lang="ts">
import type { ThirdPartyConfig } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: ThirdPartyConfig;
    hasSavedSecret?: Record<string, boolean>;
    canEdit?: boolean;
  }>(),
  {
    hasSavedSecret: () => ({}),
    canEdit: true
  }
);
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
</style>

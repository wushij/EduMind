<template>
  <div class="security-config-pane">
    <el-form :model="draft" label-width="140px" class="config-form" autocomplete="off" @submit.prevent>
      <el-divider content-position="left">前端安全</el-divider>
      <el-form-item label="禁止前端调试">
        <el-switch
          v-model="draft.disableDevtool"
          :disabled="!canEdit"
        />
        <span class="unit">开启后将限制打开开发者工具（F12），降低随意查看源码与调试的风险</span>
      </el-form-item>

      <el-divider content-position="left">会话安全</el-divider>
      <el-form-item label="禁止多端同时在线">
        <el-switch
          v-model="draft.isConcurrent"
          :disabled="!canEdit"
        />
        <span class="unit">开启后，同一账号再次登录会先踢掉之前的会话，只保留最新一次登录</span>
      </el-form-item>

      <el-divider content-position="left">API 安全防线与防重放</el-divider>
      <el-form-item label="SM4 数据加密">
        <el-switch
          v-model="draft.sm4EncryptEnabled"
          :disabled="!canEdit"
        />
        <span class="unit">是否启用接口请求/响应数据加密（国密 SM4-CBC 模式 + 16 字节随机 IV 向量）</span>
      </el-form-item>

      <el-form-item label="数字签名验签">
        <el-switch
          v-model="draft.sm3SignEnabled"
          :disabled="!canEdit"
        />
        <span class="unit">是否启用接口签名验签（国密 HMAC-SM3 高性能签名防篡改）</span>
      </el-form-item>

      <el-form-item label="时间戳校验">
        <el-switch
          v-model="draft.timestampEnabled"
          :disabled="!canEdit"
        />
        <span class="unit">校验请求时间，防止过期请求（5 分钟时间窗口）</span>
      </el-form-item>

      <el-form-item label="Nonce 校验">
        <el-switch
          v-model="draft.nonceEnabled"
          :disabled="!canEdit"
        />
        <span class="unit">校验随机数，防止高频重放攻击（Redis 单次随机数查重）</span>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="保存全部后立即生效：接口安全开关将即时作用于系统所有 REST 接口；时间戳与 Nonce 建议保持开启。"
      class="security-tip-alert"
    />
  </div>
</template>

<script setup lang="ts">
import type { SecurityPlatformConfig } from '@/types/system/config';

withDefaults(
  defineProps<{
    draft: SecurityPlatformConfig;
    canEdit?: boolean;
  }>(),
  {
    canEdit: true
  }
);
</script>

<style scoped>
.security-config-pane {
  max-width: 760px;
  padding-top: 8px;
}

.config-form {
  margin-bottom: 20px;
}

.unit {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

.security-tip-alert {
  margin-top: 16px;
}
</style>

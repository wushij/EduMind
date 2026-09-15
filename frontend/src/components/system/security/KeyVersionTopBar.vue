<template>
  <div class="kms-top-bar">
    <div class="context-info">
      <div class="shield-badge">
        <el-icon><Lock /></el-icon>
      </div>
      <div class="context-text">
        <span class="tenant-title">当前租户隔离空间：{{ activeTenantName }} ({{ activeCampusName }})</span>
        <span class="tenant-sub">密钥材料严控在内存安全派生，禁止落盘与日志明文输出 · 历史密文按版本无缝向下兼容</span>
      </div>
    </div>
    <div class="action-buttons">
      <el-button type="primary" class="gradient-btn" @click="openCryptoTestModal(activeDataKey || activeModelKey)">
        <el-icon><Connection /></el-icon>
        <span>实机加解密校验</span>
      </el-button>
      <el-button type="primary" plain class="action-pill-btn" @click="openRotateModal()">
        <el-icon><RefreshRight /></el-icon>
        <span>轮换密钥版本</span>
      </el-button>
      <el-button
        round
        :icon="Refresh"
        class="btn-refresh"
        :loading="refreshing"
        @click="handleRefreshAll"
      >
        刷新大盘
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lock, Refresh, RefreshRight, Connection } from '@element-plus/icons-vue';
import type { SecurityKeyVersionVO } from '@/types/system/security-key';

defineProps<{
  activeTenantName: string;
  activeCampusName: string;
  activeDataKey: SecurityKeyVersionVO | null;
  activeModelKey: SecurityKeyVersionVO | null;
  refreshing: boolean;
  openCryptoTestModal: (targetKey?: SecurityKeyVersionVO | null) => void;
  openRotateModal: (alias?: string) => void;
  handleRefreshAll: () => void | Promise<void>;
}>();
</script>

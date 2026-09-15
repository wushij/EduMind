<template>
  <div class="active-keys-grid">
    <!-- 1. 教学业务数据主密钥 (edumind-data-key) -->
    <div class="active-key-card" :class="{ 'is-active': activeDataKey }">
      <div class="card-glow-bg"></div>
      <div class="card-header">
        <div class="header-left">
          <div class="key-icon-box data-icon">
            <el-icon><Lock /></el-icon>
          </div>
          <div class="key-title-meta">
            <div class="title-line">
              <h4>edumind-data-key</h4>
              <el-tag size="small" type="primary" effect="plain">教学数据主密钥</el-tag>
              <span class="active-pulse-badge">
                <span class="pulse-dot"></span>
                <span>生效中 v{{ activeDataKey?.keyVersion || 1 }}</span>
              </span>
            </div>
            <span class="key-desc">保护 AI 长期记忆沙箱、学生错题本、学情认知画像与对话切片高敏数据</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button size="small" type="primary" link @click="openCryptoTestModal(activeDataKey)">
            <el-icon><Connection /></el-icon>
            <span>加解密自检</span>
          </el-button>
          <el-button size="small" type="primary" link @click="openRotateModal('edumind-data-key')">
            <el-icon><RefreshRight /></el-icon>
            <span>轮换版本</span>
          </el-button>
        </div>
      </div>

      <div class="card-body">
        <div class="spec-grid">
          <div class="spec-item">
            <span class="k">加密算法标准</span>
            <span class="v font-semibold text-amber">国密 SM4-GCM</span>
          </div>
          <div class="spec-item">
            <span class="k">当前生效版本</span>
            <span class="v font-mono text-primary font-bold">v{{ activeDataKey?.keyVersion || 1 }}</span>
          </div>
          <div class="spec-item">
            <span class="k">密钥安全指纹</span>
            <div class="v fingerprint-box" @click="copyText(activeDataKey?.keyFingerprint || 'SM4-GCM#7F8A-3C2B')">
              <span class="font-mono text-xs">{{ activeDataKey?.keyFingerprint || 'SM4-GCM#7F8A-3C2B' }}</span>
              <el-icon class="copy-icon"><CopyDocument /></el-icon>
            </div>
          </div>
          <div class="spec-item">
            <span class="k">激活生效时间</span>
            <span class="v font-mono text-secondary">{{ activeDataKey?.activatedTime || activeDataKey?.createTime || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. AI 模型凭证密钥 (edumind-model-key) -->
    <div class="active-key-card" :class="{ 'is-active': activeModelKey }">
      <div class="card-glow-bg purple"></div>
      <div class="card-header">
        <div class="header-left">
          <div class="key-icon-box model-icon">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="key-title-meta">
            <div class="title-line">
              <h4>edumind-model-key</h4>
              <el-tag size="small" type="purple" effect="plain">AI模型凭证密钥</el-tag>
              <span class="active-pulse-badge purple">
                <span class="pulse-dot purple"></span>
                <span>生效中 v{{ activeModelKey?.keyVersion || 1 }}</span>
              </span>
            </div>
            <span class="key-desc">保护 AI 模型中枢 DeepSeek / OpenAI / Qwen 等大模型 API-Key 根凭证密文存储</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button size="small" type="primary" link @click="openCryptoTestModal(activeModelKey)">
            <el-icon><Connection /></el-icon>
            <span>加解密自检</span>
          </el-button>
          <el-button size="small" type="primary" link @click="openRotateModal('edumind-model-key')">
            <el-icon><RefreshRight /></el-icon>
            <span>轮换版本</span>
          </el-button>
        </div>
      </div>

      <div class="card-body">
        <div class="spec-grid">
          <div class="spec-item">
            <span class="k">加密算法标准</span>
            <span class="v font-semibold text-amber">国密 SM4-GCM</span>
          </div>
          <div class="spec-item">
            <span class="k">当前生效版本</span>
            <span class="v font-mono text-purple font-bold">v{{ activeModelKey?.keyVersion || 1 }}</span>
          </div>
          <div class="spec-item">
            <span class="k">密钥安全指纹</span>
            <div class="v fingerprint-box" @click="copyText(activeModelKey?.keyFingerprint || 'SM4-GCM#8B12-4E90')">
              <span class="font-mono text-xs">{{ activeModelKey?.keyFingerprint || 'SM4-GCM#8B12-4E90' }}</span>
              <el-icon class="copy-icon"><CopyDocument /></el-icon>
            </div>
          </div>
          <div class="spec-item">
            <span class="k">激活生效时间</span>
            <span class="v font-mono text-secondary">{{ activeModelKey?.activatedTime || activeModelKey?.createTime || '-' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Lock, RefreshRight, Connection, Cpu, CopyDocument } from '@element-plus/icons-vue';
import type { SecurityKeyVersionVO } from '@/types/system/security-key';

defineProps<{
  activeDataKey: SecurityKeyVersionVO | null;
  activeModelKey: SecurityKeyVersionVO | null;
  openCryptoTestModal: (targetKey?: SecurityKeyVersionVO | null) => void;
  openRotateModal: (alias?: string) => void;
  copyText: (text?: string) => void | Promise<void>;
}>();
</script>

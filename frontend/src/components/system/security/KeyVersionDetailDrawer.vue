<template>
  <el-drawer
    v-model="visible"
    title="国密 KMS 密钥版本元数据与安全合规规范"
    size="560px"
    destroy-on-close
    class="kms-detail-drawer"
  >
    <div v-if="selectedKeyDetail" class="drawer-content">
      <!-- 1. 顶层 Hero 概览卡片 -->
      <div class="drawer-hero-card">
        <div class="hero-top">
          <div class="hero-title-group">
            <div class="key-avatar-box" :class="selectedKeyDetail.keyAlias?.includes('model') ? 'model' : 'data'">
              <el-icon v-if="selectedKeyDetail.keyAlias?.includes('model')"><Cpu /></el-icon>
              <el-icon v-else><Lock /></el-icon>
            </div>
            <div class="title-meta">
              <div class="alias-line">
                <span class="val font-mono font-bold">{{ selectedKeyDetail.keyAlias }}</span>
                <span class="alias-pill-badge" :class="selectedKeyDetail.keyAlias?.includes('model') ? 'purple' : 'blue'">
                  {{ selectedKeyDetail.keyAlias?.includes('model') ? 'AI模型中枢凭证' : '教学沙箱数据' }}
                </span>
              </div>
              <span class="tenant-tag">所属租户空间 · 华东师范大学附属实验学校 (#{{ selectedKeyDetail.tenantId }})</span>
            </div>
          </div>

          <!-- 生效状态呼吸指示灯徽标 -->
          <div class="pulse-status-badge" :class="selectedKeyDetail.status === 'ACTIVE' ? 'active' : 'deprecated'">
            <span class="dot"></span>
            <span>{{ selectedKeyDetail.status === 'ACTIVE' ? '生效中 ACTIVE' : '已归档 DEPRECATED' }}</span>
          </div>
        </div>

        <!-- 3 个关键指标概览卡片 -->
        <div class="hero-grid">
          <div class="hero-item">
            <span class="hi-label">当前版本号</span>
            <span class="hi-val text-primary font-mono font-bold">v{{ selectedKeyDetail.keyVersion }}</span>
          </div>
          <div class="hero-item">
            <span class="hi-label">国密加密算法</span>
            <span class="hi-val text-amber font-mono font-bold">{{ selectedKeyDetail.algorithm }}</span>
          </div>
          <div class="hero-item">
            <span class="hi-label">合规认证标准</span>
            <span class="hi-val text-success font-bold">商密三级 (GM/T)</span>
          </div>
        </div>
      </div>

      <!-- 2. 国密密码学规格参数结构化卡片 -->
      <div class="detail-card">
        <div class="card-header">
          <div class="header-left">
            <div class="icon-bubble blue"><el-icon><Lock /></el-icon></div>
            <div class="header-text">
              <span class="title">国密密码学规格参数</span>
              <span class="sub">GM/T 0002-2012 / GB/T 32907-2016 规范体系</span>
            </div>
          </div>
        </div>

        <div class="spec-table">
          <div class="spec-row">
            <div class="spec-label">国家商密规范</div>
            <div class="spec-value">
              <span class="spec-pill blue">GB/T 32907-2016</span>
              <span class="spec-note">SM4 分组密码算法行业标准</span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">工作运行模式</div>
            <div class="spec-value">
              <span class="spec-pill cyan">SM4-GCM</span>
              <span class="spec-note">Galois/Counter Mode · 具备认证加密与消息完整性校验</span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">工作密钥长度</div>
            <div class="spec-value">
              <span class="badge-mono font-bold font-mono">128 bits</span>
              <span class="spec-note">(16 bytes 动态安全派生，严禁明文落盘)</span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">初始向量 (IV)</div>
            <div class="spec-value">
              <span class="badge-mono font-bold font-mono">96 bits</span>
              <span class="spec-note">12 bytes 密码学伪随机向量 (与密文封装传输)</span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">认证标签 (Tag)</div>
            <div class="spec-value">
              <span class="badge-mono font-bold font-mono">128 bits</span>
              <span class="spec-note">16 bytes 消息认证码 (防篡改与重放攻击)</span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">安全派生指纹</div>
            <div class="spec-value">
              <div class="fingerprint-copy-box font-mono" @click="copyText(selectedKeyDetail.keyFingerprint)">
                <span>{{ selectedKeyDetail.keyFingerprint }}</span>
                <el-icon class="copy-icon"><CopyDocument /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. 全链路业务保护范围卡片 -->
      <div class="detail-card">
        <div class="card-header">
          <div class="header-left">
            <div class="icon-bubble purple"><el-icon><Connection /></el-icon></div>
            <div class="header-text">
              <span class="title">全链路业务保护范围</span>
              <span class="sub">多租户沙箱逻辑与硬件密钥隔离保护边界</span>
            </div>
          </div>
        </div>

        <div class="spec-table">
          <div class="spec-row">
            <div class="spec-label">业务关联保护对象</div>
            <div class="spec-value wrap-pills">
              <span
                v-for="item in (selectedKeyDetail.usageScope || '学生错题 / 认知画像 / 对话切片 / 敏感学情').split('/')"
                :key="item"
                class="scope-pill"
              >
                {{ item.trim() }}
              </span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">激活生效时间</div>
            <div class="spec-value">
              <span class="font-mono text-secondary">{{ selectedKeyDetail.activatedTime || selectedKeyDetail.createTime || '-' }}</span>
            </div>
          </div>
          <div class="spec-row">
            <div class="spec-label">隐私保护机制</div>
            <div class="spec-value">
              <span class="fail-closed-badge">
                <el-icon><Lock /></el-icon>
                <span>Fail-Closed 隐私保护 (认证失败坚决熔断阻断)</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. 原始元数据 JSON 代码卡片 -->
      <div class="detail-card code-card">
        <div class="code-terminal-header">
          <div class="terminal-left">
            <div class="terminal-dots">
              <span class="dot red"></span>
              <span class="dot yellow"></span>
              <span class="dot green"></span>
            </div>
            <span class="terminal-filename font-mono">key-metadata-v{{ selectedKeyDetail.keyVersion }}.json</span>
          </div>
          <button type="button" class="copy-code-btn" @click="copyText(JSON.stringify(selectedKeyDetail, null, 2))">
            <el-icon><CopyDocument /></el-icon>
            <span>复制 JSON</span>
          </button>
        </div>
        <pre class="terminal-code-block font-mono"><code>{{ JSON.stringify(selectedKeyDetail, null, 2) }}</code></pre>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { Lock, CopyDocument, Connection, Cpu } from '@element-plus/icons-vue';
import type { SecurityKeyVersionVO } from '@/types/system/security-key';

const visible = defineModel<boolean>('visible', { default: false });

defineProps<{
  selectedKeyDetail: SecurityKeyVersionVO | null;
  copyText: (text?: string) => void | Promise<void>;
}>();
</script>

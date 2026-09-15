<template>
  <el-drawer
    :model-value="visible"
    title="算力抵扣与配额消费凭单"
    size="540px"
    destroy-on-close
    class="audit-detail-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="selectedLog" class="drawer-content">
      <!-- 头部 Trace 概览卡片 -->
      <div class="drawer-hero-card">
        <div class="hero-top">
          <div class="trace-title">
            <span class="label">算力流水单号</span>
            <span class="val font-mono">{{ selectedLog.traceId }}</span>
          </div>
          <button class="copy-pill-btn" @click="emit('copy-text', selectedLog.traceId)">
            <el-icon><CopyDocument /></el-icon>
            <span>复制单号</span>
          </button>
        </div>
        <div class="hero-grid">
          <div class="hero-item">
            <span class="hi-label">本次扣除算力</span>
            <span class="hi-val text-primary font-mono">-{{ selectedLog.totalTokens.toLocaleString() }} toks</span>
          </div>
          <div class="hero-item">
            <span class="hi-label">算力折算成本</span>
            <span class="hi-val text-amber font-mono">¥{{ selectedLog.estimatedCost.toFixed(4) }}</span>
          </div>
          <div class="hero-item">
            <span class="hi-label">扣减执行响应</span>
            <span class="hi-val text-emerald font-mono">{{ selectedLog.durationMs }}ms</span>
          </div>
        </div>
      </div>

      <!-- 结构化项：消费主体 -->
      <div class="detail-section">
        <h4 class="sec-heading">
          <el-icon class="sec-icon icon-blue"><User /></el-icon>
          <span>算力消费主体与归属</span>
        </h4>
        <div class="info-list">
          <div class="info-row">
            <span class="k">消费师生</span>
            <div class="v user-flex-item">
              <el-avatar :size="22" :src="getUserAvatarUrl(selectedLog)" class="mr-1" />
              <span class="font-bold">{{ selectedLog.realName || selectedLog.username }} (@{{ selectedLog.username }})</span>
            </div>
          </div>
          <div class="info-row">
            <span class="k">用户 ID / 身份</span>
            <span class="v">ID: {{ selectedLog.userId }} · <span class="role-pill" :class="(selectedLog.userRole || 'USER').toLowerCase()">{{ getRoleLabel(selectedLog.userRole) }}</span></span>
          </div>
          <div v-if="selectedLog.courseId" class="info-row">
            <span class="k">关联课程 ID</span>
            <span class="v font-mono">#{{ selectedLog.courseId }}</span>
          </div>
          <div v-if="selectedLog.conversationId" class="info-row">
            <span class="k">会话 ID</span>
            <span class="v font-mono">{{ selectedLog.conversationId }}</span>
          </div>
        </div>
      </div>

      <!-- 结构化项：算力与模型规格 -->
      <div class="detail-section">
        <h4 class="sec-heading">
          <el-icon class="sec-icon icon-purple"><Cpu /></el-icon>
          <span>模型计费规格与 Token 明细</span>
        </h4>
        <div class="info-list">
          <div class="info-row">
            <span class="k">计费模型</span>
            <span class="v font-mono font-bold">{{ selectedLog.model }}</span>
          </div>
          <div class="info-row">
            <span class="k">消费业务场景</span>
            <span class="v">
              <span class="scene-pill" :class="getSceneStyleClass(selectedLog.scene || selectedLog.toolName)">
                {{ getSceneLabel(selectedLog.scene || selectedLog.toolName) }}
              </span>
            </span>
          </div>
          <div class="info-row">
            <span class="k">Prompt Token (输入额度)</span>
            <span class="v font-mono">{{ selectedLog.promptTokens }} toks</span>
          </div>
          <div class="info-row">
            <span class="k">Completion Token (输出额度)</span>
            <span class="v font-mono">{{ selectedLog.completionTokens }} toks</span>
          </div>
        </div>
      </div>

      <!-- 原始算力凭据 JSON -->
      <div class="detail-section">
        <div class="json-header">
          <h4 class="sec-heading mb-0">
            <el-icon class="sec-icon icon-amber"><Document /></el-icon>
            <span>算力凭单完整元数据 (JSON)</span>
          </h4>
          <button type="button" class="copy-json-btn" @click="emit('copy-text', JSON.stringify(selectedLog, null, 2))">
            <el-icon><CopyDocument /></el-icon>
            <span>复制凭据</span>
          </button>
        </div>
        <pre class="json-code-block"><code>{{ JSON.stringify(selectedLog, null, 2) }}</code></pre>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { User, CopyDocument, Document, Cpu } from '@element-plus/icons-vue';
import type { AIAuditLog } from '@/types/system/audit';
import { getUserAvatarUrl, getRoleLabel, getSceneLabel, getSceneStyleClass } from '@/composables/system/useTenantQuota';

defineProps<{
  visible: boolean;
  selectedLog: AIAuditLog | null;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'copy-text': [text: string];
}>();
</script>

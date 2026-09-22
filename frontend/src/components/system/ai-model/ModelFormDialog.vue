<template>
  <el-dialog
    :model-value="visible"
    :title="editing ? `编辑模型配置: ${editing.name}` : '接入新模型'"
    width="560px"
    append-to-body
    class="model-dialog"
    :close-on-click-modal="false"
    @update:model-value="onVisibleChange"
  >
    <el-form :model="form" label-width="110px" class="model-form">
      <el-form-item label="配置唯一标识" required>
        <el-input
          v-model="form.name"
          class="pill-input"
          :disabled="!!editing"
          placeholder="如 deepseek-chat-prod、qwen-plus"
        />
      </el-form-item>

      <el-form-item label="模型类型" required>
        <el-radio-group
          v-model="form.configType"
          class="pill-radio-group"
          :disabled="!!editing"
          @change="onConfigTypeChange"
        >
          <el-radio-button label="chat">对话推理 (Chat)</el-radio-button>
          <el-radio-button label="embedding">向量计算 (Embedding)</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="模型服务商" required>
        <el-select
          v-model="form.provider"
          class="pill-select"
          placeholder="选择服务商（自动填充端点与型号建议）"
          style="width: 100%"
          @change="onProviderChange"
        >
          <el-option
            v-for="(preset, pKey) in currentPresetsGroup"
            :key="pKey"
            :label="preset.label"
            :value="pKey"
          />
        </el-select>
        <span v-if="form.configType === 'embedding'" class="form-item-tip">
          说明：DeepSeek 官方不提供向量接口；向量计算请选用通义千问 (DashScope) 或 OpenAI
        </span>
      </el-form-item>

      <el-form-item label="模型名称型号" required>
        <el-select
          v-model="form.modelName"
          class="pill-select"
          filterable
          allow-create
          default-first-option
          placeholder="可选择最新推荐型号或直接键入输入"
          style="width: 100%"
        >
          <el-option v-for="opt in currentModelOptions" :key="opt" :label="opt" :value="opt" />
        </el-select>
      </el-form-item>

      <el-form-item label="接口 Base URL">
        <el-input
          v-model="form.baseUrl"
          class="pill-input"
          placeholder="https://api.deepseek.com/v1（只填到 /v1）"
        />
      </el-form-item>

      <el-form-item label="API Key 秘钥">
        <el-input
          v-model="form.apiKey"
          class="pill-input"
          type="password"
          show-password
          autocomplete="new-password"
          :placeholder="editing?.hasApiKey ? '留空不修改已配置秘钥' : '请输入 API Key'"
        />
        <div v-if="currentPreset?.portalUrl" class="portal-link-wrap">
          <el-link type="primary" :href="currentPreset.portalUrl" target="_blank" class="portal-link">
            前往获取 {{ currentPreset.label }} API Key ↗
          </el-link>
        </div>
        <div class="form-item-tip kms-security-tip">
          <el-icon class="kms-tip-icon"><Lock /></el-icon>
          <span>已使用国密 SM4-GCM 加密存储，受 KMS 版本控制</span>
          <span v-if="editing?.keyVersion" class="kms-version-tag">（当前版本: v{{ editing.keyVersion }}）</span>
        </div>
      </el-form-item>

      <el-form-item v-if="form.configType === 'chat'" label="采样温度">
        <div class="slider-row">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.05" style="flex: 1" />
          <span class="slider-val">{{ form.temperature.toFixed(2) }}</span>
        </div>
      </el-form-item>

      <el-form-item v-if="form.configType === 'chat'" label="思考强度">
        <el-select v-model="form.reasoningEffort" class="pill-select" style="width: 100%">
          <el-option label="Low · 快速响应，留足正文" value="low" />
          <el-option label="Medium · 中度推演，均衡思考" value="medium" />
          <el-option label="High · 深度推演，攻克复杂逻辑" value="high" />
          <el-option label="Max · 最强思考，全算力展开" value="max" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="form.configType === 'embedding'" label="向量维度" required>
        <el-input-number
          v-model="form.dimension"
          class="pill-input-number"
          :min="128"
          :max="4096"
          :step="128"
          controls-position="right"
          style="width: 100%"
        />
        <span class="form-item-tip">
          OpenAI text-embedding-3-small → 1536；BAAI/bge-large-zh-v1.5 → 1024。切换不同维度后需执行「全量切片同步与重建」
        </span>
      </el-form-item>

      <el-form-item label="设为默认模型">
        <el-switch v-model="form.isDefault" />
        <span class="switch-hint">同类型下仅允许存在一个默认生效模型</span>
      </el-form-item>

      <el-form-item label="启用状态">
        <el-switch v-model="form.status" active-value="enabled" inactive-value="disabled" />
      </el-form-item>

      <el-form-item v-if="dialogTesting || dialogTestResult" class="dialog-test-form-item" label=" ">
        <div
          class="dialog-test-pill"
          :class="{
            testing: dialogTesting,
            success: !dialogTesting && dialogTestResult?.success,
            fail: !dialogTesting && dialogTestResult && !dialogTestResult.success
          }"
        >
          <template v-if="dialogTesting">
            <el-icon class="is-loading"><RefreshRight /></el-icon>
            <span>正在探测端点连通性，请稍候...</span>
          </template>
          <template v-else-if="dialogTestResult?.success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>连接成功，延时 {{ dialogTestResult.latency }}ms</span>
          </template>
          <template v-else-if="dialogTestResult">
            <el-icon><CircleCloseFilled /></el-icon>
            <span>{{ dialogTestResult.error || '连接失败' }}</span>
          </template>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button :loading="dialogTesting" :disabled="saving" @click="onDialogTest">
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
        <el-button @click="onVisibleChange(false)">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">
          保存配置并立即生效
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  RefreshRight,
  Connection,
  CircleCheckFilled,
  CircleCloseFilled,
  Lock
} from '@element-plus/icons-vue';
import type { AIModelConfigItem } from '@/types/system/model';

defineProps<{
  visible: boolean;
  editing: AIModelConfigItem | null;
  saving: boolean;
  dialogTesting: boolean;
  dialogTestResult: { success: boolean; latency?: number; error?: string } | null;
  form: {
    name: string;
    provider: string;
    configType: 'chat' | 'embedding';
    modelName: string;
    baseUrl: string;
    apiKey: string;
    temperature: number;
    reasoningEffort: string;
    dimension: number;
    isDefault: boolean;
    status: string;
  };
  currentPresetsGroup: Record<string, { label: string; portalUrl?: string }>;
  currentPreset: { label: string; portalUrl?: string } | null;
  currentModelOptions: string[];
  onVisibleChange: (visible: boolean) => void;
  onConfigTypeChange: (val: 'chat' | 'embedding') => void;
  onProviderChange: (providerKey: string) => void;
  onDialogTest: () => void;
  onSave: () => void;
}>();
</script>

<style scoped lang="scss">
.form-item-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  display: block;
}

.portal-link-wrap {
  margin-top: 4px;
}

.portal-link {
  font-size: 12px;
}

.kms-security-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #10b981;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 999px;
  padding: 4px 14px;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
}

.kms-tip-icon {
  font-size: 13px;
  color: #059669;
}

.kms-version-tag {
  color: #047857;
  font-weight: 600;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.slider-val {
  font-size: 13px;
  font-weight: 700;
  color: #1677ff;
  width: 38px;
}

.switch-hint {
  font-size: 12px;
  color: #64748b;
  margin-left: 10px;
}

.dialog-test-form-item {
  margin-bottom: 4px;

  :deep(.el-form-item__label) {
    padding-right: 0;
  }

  :deep(.el-form-item__content) {
    width: 100%;
  }
}

.dialog-test-pill {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 18px;
  border-radius: 999px;
  font-size: 13px;
  line-height: 1.4;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;

  &.testing {
    background: #f8fafc;
    color: #64748b;
    border: 1px solid #e2e8f0;
  }

  &.success {
    background: #f6ffed;
    color: #389e0d;
    border: 1px solid #b7eb8f;
  }

  &.fail {
    background: #fff2f0;
    color: #cf1322;
    border: 1px solid #ffccc7;
  }

  .el-icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  span {
    flex: 1;
    min-width: 0;
    word-break: break-word;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

<style lang="scss">
.model-dialog {
  .model-form {
    .pill-input .el-input__wrapper,
    .pill-select .el-select__wrapper {
      border-radius: 999px !important;
      box-shadow: 0 0 0 1px #e2e8f0 inset !important;
      padding: 4px 14px !important;
      background-color: #ffffff !important;
      transition: all 0.2s ease !important;
    }

    .pill-input .el-input__wrapper:hover,
    .pill-select .el-select__wrapper:hover {
      box-shadow: 0 0 0 1px #93c5fd inset !important;
    }

    .pill-input .el-input__wrapper.is-focus,
    .pill-select .el-select__wrapper.is-focused {
      box-shadow: 0 0 0 1px #1677ff inset, 0 0 0 3px rgba(22, 119, 255, 0.12) !important;
    }

    .pill-radio-group .el-radio-button__inner {
      border-radius: 0 !important;
      font-weight: 600 !important;
      padding: 8px 16px !important;
    }

    .pill-radio-group .el-radio-button:first-child .el-radio-button__inner {
      border-radius: 999px 0 0 999px !important;
    }

    .pill-radio-group .el-radio-button:last-child .el-radio-button__inner {
      border-radius: 0 999px 999px 0 !important;
    }

    .pill-input-number {
      width: 100% !important;
      border-radius: 999px !important;
      overflow: hidden !important;
      border: 1px solid #e2e8f0 !important;
      background-color: #ffffff !important;
      transition: all 0.2s ease !important;
    }

    .pill-input-number:hover {
      border-color: #93c5fd !important;
    }

    .pill-input-number.is-focus,
    .pill-input-number:focus-within {
      border-color: #1677ff !important;
      box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12) !important;
    }

    .pill-input-number .el-input__wrapper {
      border-radius: 0 !important;
      box-shadow: none !important;
      background: transparent !important;
      padding: 0 8px !important;
    }

    .pill-input-number .el-input__wrapper:hover,
    .pill-input-number .el-input__wrapper.is-focus {
      box-shadow: none !important;
    }

    .pill-input-number .el-input-number__decrease,
    .pill-input-number .el-input-number__increase {
      width: 34px !important;
      border: none !important;
      background-color: #f8fafc !important;
      color: #64748b !important;
    }

    .pill-input-number.is-controls-right .el-input-number__decrease,
    .pill-input-number.is-controls-right .el-input-number__increase {
      border-left: 1px solid #e2e8f0 !important;
    }

    .pill-input-number.is-controls-right .el-input-number__increase {
      border-radius: 0 999px 0 0 !important;
      border-bottom: 1px solid #e2e8f0 !important;
    }

    .pill-input-number.is-controls-right .el-input-number__decrease {
      border-radius: 0 0 999px 0 !important;
    }
  }

  .dialog-footer .el-button:not(.is-text):not(.is-link) {
    border-radius: 999px !important;
    font-weight: 600 !important;
    padding: 8px 18px !important;
  }
}
</style>

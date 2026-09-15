<template>
  <el-dialog
    v-model="visible"
    title="国密 SM4-GCM 实机在线加解密实时自检验证"
    width="640px"
    destroy-on-close
    class="crypto-test-dialog"
  >
    <div class="crypto-test-form">
      <!-- 密钥环境概览 -->
      <div class="test-target-capsule">
        <div class="capsule-left">
          <span class="label">测试目标别名：</span>
          <span class="val font-mono font-bold">{{ testForm.keyAlias }}</span>
        </div>
        <div class="capsule-right">
          <span class="label">使用密钥版本：</span>
          <span class="val font-mono text-primary font-bold">v{{ testForm.keyVersion || '最新活跃' }}</span>
          <span class="label ml-2">算法：</span>
          <el-tag size="small" type="warning" effect="light">SM4-GCM</el-tag>
        </div>
      </div>

      <!-- 快速测试样本填充条 -->
      <div class="quick-samples-bar">
        <span class="qs-label">快速加载真实业务测试用例：</span>
        <el-button size="small" plain @click="loadSampleText('student_memory')">
          学生高敏学情记忆明文
        </el-button>
        <el-button size="small" plain @click="loadSampleText('model_key')">
          DeepSeek-V3 API-Key
        </el-button>
      </div>

      <el-form label-position="top">
        <el-form-item label="操作类型">
          <el-radio-group v-model="testForm.operation" size="small" class="operation-radio-group">
            <el-radio-button value="ENCRYPT">
              <el-icon class="mr-1"><Lock /></el-icon>
              <span>执行 SM4-GCM 认证加密</span>
            </el-radio-button>
            <el-radio-button value="DECRYPT">
              <el-icon class="mr-1"><Key /></el-icon>
              <span>执行 SM4-GCM 认证解密</span>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="testForm.operation === 'ENCRYPT' ? '待加密明文 (Plaintext)' : '待解密国密密文 (Base64 Ciphertext)'">
          <el-input
            v-model="testForm.text"
            type="textarea"
            :rows="4"
            :placeholder="testForm.operation === 'ENCRYPT' ? '输入任意待加密敏感明文字符串...' : '输入标准 SM4-GCM 认证加密产生的 Base64 密文...'"
            class="code-textarea font-mono"
          />
        </el-form-item>
      </el-form>

      <!-- 测试结果回显区 -->
      <div v-if="testResult" class="test-result-box" :class="testResult.success ? 'success' : 'failed'">
        <div class="result-header">
          <div class="result-title">
            <el-icon :class="testResult.success ? 'text-success' : 'text-danger'">
              <CircleCheckFilled v-if="testResult.success" />
              <WarningFilled v-else />
            </el-icon>
            <span class="font-bold">{{ testResult.success ? '国密算法执行成功' : '国密防护阻断 (Fail-Closed)' }}</span>
            <span class="duration-badge font-mono">{{ testResult.durationMs }} ms</span>
          </div>
          <el-button
            v-if="testResult.resultText"
            size="small"
            link
            type="primary"
            @click="copyText(testResult.resultText)"
          >
            <el-icon><CopyDocument /></el-icon>
            <span>复制结果</span>
          </el-button>
        </div>
        <p class="result-message">{{ testResult.message }}</p>
        <div v-if="testResult.resultText" class="result-text-preview">
          <pre class="font-mono"><code>{{ testResult.resultText }}</code></pre>
        </div>
        <!-- 若加密成功，提供一键反向填入解密验证快捷按钮 -->
        <div v-if="testResult.success && testForm.operation === 'ENCRYPT'" class="mt-2 text-right">
          <el-button size="small" type="success" plain @click="flipToDecrypt">
            <el-icon><Key /></el-icon>
            <span>一键填入该密文进行反向解密验证</span>
          </el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="testingCrypto" class="gradient-btn" @click="executeCryptoTest">
        <el-icon><Connection /></el-icon>
        <span>立即执行 {{ testForm.operation === 'ENCRYPT' ? '国密加密' : '国密解密' }}</span>
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  Lock,
  Key,
  WarningFilled,
  CopyDocument,
  CircleCheckFilled,
  Connection
} from '@element-plus/icons-vue';
import type { SecurityKeyCryptoTestResponse } from '@/types/system/security-key';

const visible = defineModel<boolean>('visible', { default: false });

defineProps<{
  testForm: {
    keyAlias: string;
    keyVersion?: number;
    operation: 'ENCRYPT' | 'DECRYPT';
    text: string;
  };
  testResult: SecurityKeyCryptoTestResponse | null;
  testingCrypto: boolean;
  loadSampleText: (type: 'student_memory' | 'model_key') => void;
  executeCryptoTest: () => void | Promise<void>;
  flipToDecrypt: () => void;
  copyText: (text?: string) => void | Promise<void>;
}>();
</script>

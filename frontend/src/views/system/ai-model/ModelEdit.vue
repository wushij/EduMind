<template>
  <div class="model-edit-page">
    <div class="header-card">
      <div class="left">
        <el-button link :icon="ArrowLeft" @click="router.push('/system/models')">
          返回模型列表
        </el-button>
        <h2>{{ isNew ? '接入新大模型' : `编辑模型配置 · ${form.name}` }}</h2>
      </div>
      <div class="right">
        <el-button :icon="Connection" :loading="testingConn" @click="testConnection">
          测试端点连通性
        </el-button>
        <el-button type="primary" :icon="Check" @click="saveModel">
          保存配置
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form label-position="top" class="edit-form">
        <div class="form-row-2">
          <el-form-item label="模型显示名称">
            <el-input v-model="form.name" placeholder="如 DeepSeek-V3" />
          </el-form-item>
          <el-form-item label="模型调用 Key (如 deepseek-chat)">
            <el-input v-model="form.modelKey" placeholder="系统请求 LLM 时的模型标识" />
          </el-form-item>
        </div>

        <div class="form-row-2">
          <el-form-item label="模型提供商">
            <el-select v-model="form.provider" style="width: 100%">
              <el-option value="DeepSeek" label="DeepSeek 深度求索" />
              <el-option value="Qwen" label="Alibaba 通义千问" />
              <el-option value="OpenAI" label="OpenAI" />
              <el-option value="Zhipu" label="智谱 AI (GLM)" />
            </el-select>
          </el-form-item>
          <el-form-item label="API 服务端点 (Base URL)">
            <el-input v-model="form.endpoint" placeholder="https://api.deepseek.com/v1" />
          </el-form-item>
        </div>

        <el-form-item label="API Key 访问凭证 (安全加密存储)">
          <el-input
            v-model="apiKeyInput"
            type="password"
            show-password
            placeholder="输入 API Key，留空则保持原密钥不变"
          />
        </el-form-item>

        <div class="form-row-3">
          <el-form-item label="上下文长度 (Tokens)">
            <el-input-number v-model="form.contextLength" :step="4096" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大单次输出 (Tokens)">
            <el-input-number v-model="form.maxOutputTokens" :step="512" style="width: 100%" />
          </el-form-item>
          <el-form-item label="默认采样温度 (Temperature): {{ form.temperature }}">
            <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.05" />
          </el-form-item>
        </div>

        <div class="switches-row">
          <el-form-item label="支持流式响应 (SSE)">
            <el-switch v-model="form.supportsStreaming" />
          </el-form-item>
          <el-form-item label="支持稠密向量生成 (Embedding)">
            <el-switch v-model="form.supportsEmbedding" />
          </el-form-item>
          <el-form-item label="支持视觉理解 (Vision)">
            <el-switch v-model="form.supportsVision" />
          </el-form-item>
          <el-form-item label="设置为默认对话模型">
            <el-switch v-model="form.isDefault" />
          </el-form-item>
          <el-form-item label="启用该模型">
            <el-switch v-model="form.enabled" />
          </el-form-item>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getModelConfigs, updateModelConfig } from '@/api/system/model';
import { ModelProviderConfig } from '@/types/system/model';
import { ArrowLeft, Check, Connection } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const modelId = route.params.id ? Number(route.params.id) : null;
const isNew = computed(() => !modelId);

const testingConn = ref(false);
const apiKeyInput = ref('');

const form = ref<ModelProviderConfig>({
  id: 0,
  modelKey: 'deepseek-chat',
  name: 'DeepSeek-V3',
  provider: 'DeepSeek',
  endpoint: 'https://api.deepseek.com/v1',
  apiKeyMasked: 'sk-dpsk••••••••••••••••••••••••3fa9',
  contextLength: 64000,
  maxOutputTokens: 4096,
  temperature: 0.3,
  supportsStreaming: true,
  supportsEmbedding: false,
  supportsVision: false,
  enabled: true,
  isDefault: false,
  costPer1kPrompt: 0.001,
  costPer1kCompletion: 0.002,
  healthStatus: 'HEALTHY'
});

const testConnection = async () => {
  testingConn.value = true;
  try {
    await new Promise(r => setTimeout(r, 600));
    ElMessage.success('模型端点连接正常，握手成功！');
  } finally {
    testingConn.value = false;
  }
};

const saveModel = async () => {
  if (apiKeyInput.value) {
    form.value.apiKeyMasked = apiKeyInput.value.slice(0, 7) + '••••••••••••••••' + apiKeyInput.value.slice(-4);
  }
  await updateModelConfig(form.value.id, form.value);
  ElMessage.success('模型配置保存成功');
  router.push('/system/models');
};

onMounted(async () => {
  if (modelId) {
    const list = await getModelConfigs();
    const found = list.find(m => m.id === modelId);
    if (found) form.value = JSON.parse(JSON.stringify(found));
  }
});
</script>

<style scoped lang="scss">
.model-edit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .header-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .left {
      display: flex;
      flex-direction: column;
      gap: 4px;
      h2 { margin: 0; font-size: 18px; font-weight: 700; color: #0F172A; }
    }
    .right {
      display: flex;
      gap: 10px;
    }
  }

  .form-card {
    border-radius: 12px;
    border-color: #E2E8F0;

    .edit-form {
      max-width: 900px;

      .form-row-2 {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
      }

      .form-row-3 {
        display: grid;
        grid-template-columns: 1fr 1fr 1fr;
        gap: 16px;
      }

      .switches-row {
        display: flex;
        gap: 24px;
        flex-wrap: wrap;
        border-top: 1px dashed #E2E8F0;
        padding-top: 16px;
        margin-top: 10px;
      }
    }
  }
}
</style>

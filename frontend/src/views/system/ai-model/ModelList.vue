<template>
  <div class="model-list-page">
    <div class="model-header-card">
      <div class="header-left">
        <h2>AI 模型调度与多厂商接入</h2>
        <p>统一管理 DeepSeek、通义千问、OpenAI 及 BGE 向量模型接入密钥、并发配额与默认调度策略</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="router.push('/system/models/edit')">
          接入新模型
        </el-button>
      </div>
    </div>

    <!-- 模型卡片网格 -->
    <div v-loading="loading" class="model-cards-grid">
      <div
        v-for="model in modelConfigs"
        :key="model.id"
        class="model-card"
      >
        <div class="card-header">
          <div class="header-left-title">
            <span class="provider-pill" :class="`provider-${model.provider.toLowerCase()}`">
              {{ model.provider }}
            </span>
            <span v-if="model.isDefault" class="default-badge">默认模型</span>
          </div>
          <el-switch
            v-model="model.enabled"
            active-text="启用"
            @change="toggleModelStatus(model)"
          />
        </div>

        <h3 class="model-name">{{ model.name }}</h3>
        <span class="model-key-code">{{ model.modelKey }}</span>

        <!-- 模型能力徽章 -->
        <div class="capability-badges">
          <span class="cap-tag" :class="{ active: model.supportsStreaming }">流式 (SSE)</span>
          <span class="cap-tag" :class="{ active: model.supportsEmbedding }">向量 (Embed)</span>
          <span class="cap-tag" :class="{ active: model.supportsVision }">多模态 (Vision)</span>
        </div>

        <!-- 技术参数列表 -->
        <div class="tech-params-box">
          <div class="param-row">
            <span class="k">上下文窗口</span>
            <span class="v mono">{{ (model.contextLength / 1024).toFixed(0) }}k Tokens</span>
          </div>
          <div class="param-row">
            <span class="k">接口端点</span>
            <span class="v truncate" :title="model.endpoint">{{ model.endpoint }}</span>
          </div>
          <div class="param-row">
            <span class="k">密钥凭据</span>
            <span class="v mono text-muted">{{ model.apiKeyMasked }}</span>
          </div>
        </div>

        <div class="card-footer">
          <span class="cost-hint">
            ¥{{ (model.costPer1kPrompt * 1000).toFixed(2) }}/M (输入)
          </span>
          <el-button link type="primary" size="small" @click="router.push(`/system/models/edit/${model.id}`)">
            参数配置 →
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getModelConfigs, updateModelConfig } from '@/api/system/model';
import { ModelProviderConfig } from '@/types/system/model';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const router = useRouter();
const loading = ref(false);
const modelConfigs = ref<ModelProviderConfig[]>([]);

const fetchModels = async () => {
  loading.value = true;
  try {
    modelConfigs.value = await getModelConfigs();
  } finally {
    loading.value = false;
  }
};

const toggleModelStatus = async (model: ModelProviderConfig) => {
  await updateModelConfig(model.id, { enabled: model.enabled });
  ElMessage.success(`模型 ${model.name} 状态已更新`);
};

onMounted(() => {
  fetchModels();
});
</script>

<style scoped lang="scss">
.model-list-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .model-header-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      h2 { margin: 0; font-size: 20px; font-weight: 700; color: #0F172A; }
      p { margin: 4px 0 0 0; font-size: 13px; color: #64748B; }
    }
  }

  .model-cards-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }

    .model-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);
      transition: all 0.2s;

      &:hover {
        border-color: #93C5FD;
        transform: translateY(-2px);
        box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .header-left-title {
          display: flex;
          align-items: center;
          gap: 8px;

          .provider-pill {
            font-size: 11px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 4px;

            &.provider-deepseek { background: #EFF6FF; color: #2563EB; }
            &.provider-qwen { background: #FFF7ED; color: #EA580C; }
            &.provider-openai { background: #ECFDF5; color: #059669; }
          }

          .default-badge {
            font-size: 10.5px;
            background: #FEF3C7;
            color: #B45309;
            padding: 1px 6px;
            border-radius: 4px;
          }
        }
      }

      .model-name {
        margin: 0;
        font-size: 17px;
        font-weight: 700;
        color: #1E293B;
      }

      .model-key-code {
        font-family: ui-monospace, monospace;
        font-size: 12px;
        color: #64748B;
        background: #F8FAFC;
        padding: 2px 6px;
        border-radius: 4px;
        width: fit-content;
      }

      .capability-badges {
        display: flex;
        gap: 6px;

        .cap-tag {
          font-size: 11px;
          padding: 2px 6px;
          border-radius: 4px;
          background: #F1F5F9;
          color: #94A3B8;

          &.active {
            background: #EFF6FF;
            color: #2563EB;
            font-weight: 500;
          }
        }
      }

      .tech-params-box {
        background: #F8FAFC;
        border: 1px solid #F1F5F9;
        border-radius: 8px;
        padding: 10px 12px;
        display: flex;
        flex-direction: column;
        gap: 6px;

        .param-row {
          display: flex;
          justify-content: space-between;
          font-size: 12px;

          .k { color: #64748B; }
          .v {
            color: #334155;
            font-weight: 500;

            &.mono { font-family: ui-monospace, monospace; }
            &.truncate { max-width: 240px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            &.text-muted { color: #94A3B8; }
          }
        }
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #F1F5F9;
        padding-top: 10px;

        .cost-hint {
          font-size: 11.5px;
          color: #94A3B8;
        }
      }
    }
  }
}
</style>

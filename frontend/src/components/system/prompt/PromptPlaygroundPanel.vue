<template>
  <div class="section-card playground-card">
    <div class="section-card-header">
      <div class="sch-left">
        <span class="sec-icon orange"><el-icon><VideoPlay /></el-icon></span>
        <div class="sec-title-wrap">
          <span class="sec-title">实时热测试沙箱</span>
          <span class="sec-sub">即时渲染插槽并在线调用模型</span>
        </div>
      </div>
      <el-button
        type="primary"
        size="small"
        class="run-test-action-btn"
        :loading="testing"
        :icon="VideoPlay"
        @click="onRunTest"
      >
        运行测试
      </el-button>
    </div>

    <div class="preset-helper-bar">
      <span>需快速填充？</span>
      <button class="helper-link" type="button" @click="onFillDemo">
        一键填入高校课程真实 RAG 示例
      </button>
    </div>

    <div class="playground-inputs-area">
      <div v-for="v in variables" :key="v.name" class="side-input-item">
        <div class="s-label-row">
          <span class="s-name">&#123;&#123;{{ v.name }}&#125;&#125;</span>
          <span class="s-desc">{{ v.label || v.name }}</span>
        </div>
        <el-input
          v-model="testVariables[v.name]"
          :type="v.name.includes('context') || v.name.includes('history') ? 'textarea' : 'text'"
          :rows="v.name.includes('context') ? 4 : 2"
          :placeholder="`输入 ${v.label || v.name}...`"
          size="small"
        />
      </div>
    </div>

    <div class="playground-output-area">
      <div class="output-header-bar">
        <span class="o-title">大模型推理生成结果 (LLM Output)</span>
        <div v-if="testResult" class="o-metrics">
          <span class="metric-pill">{{ testResult.durationMs }}ms</span>
          <span class="metric-pill">{{ testResult.totalTokens }} Tokens</span>
        </div>
      </div>

      <div v-loading="testing" class="output-box">
        <PromptLlmOutput v-if="testResultOutput" :content="testResultOutput" />
        <div v-else class="empty-hint">
          <el-icon :size="28" class="hint-ic"><Promotion /></el-icon>
          <p>点击上方【运行测试】，直接将右侧入参注入 Prompt 并请求当前绑定的大模型进行实时推理。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { VideoPlay, Promotion } from '@element-plus/icons-vue';
import PromptLlmOutput from '@/components/system/PromptLlmOutput.vue';
import type { PromptTemplate, PromptTestResponse } from '@/types/system/prompt';

defineProps<{
  variables: PromptTemplate['variables'];
  testVariables: Record<string, string>;
  testResultOutput: string;
  testResult: PromptTestResponse | null;
  testing: boolean;
  onRunTest: () => void;
  onFillDemo: () => void;
}>();
</script>

<style scoped lang="scss">
.section-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  padding: 18px 22px;
  margin-bottom: 18px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);

  .section-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    margin-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;

    .sch-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .sec-icon {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;

        &.orange { background: #FFF7ED; color: #EA580C; }
      }

      .sec-title-wrap {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .sec-title {
          font-size: 14.5px;
          font-weight: 700;
          color: #0F172A;
        }

        .sec-sub {
          font-size: 11.5px;
          color: #64748B;
        }
      }
    }
  }
}

.playground-card {
  .preset-helper-bar {
    background: #F0F9FF;
    border: 1px solid #BAE6FD;
    border-radius: 8px;
    padding: 8px 12px;
    margin-bottom: 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #0369A1;

    .helper-link {
      background: transparent;
      border: none;
      color: #0284C7;
      font-weight: 600;
      cursor: pointer;
      text-decoration: underline;

      &:hover {
        color: #0369A1;
      }
    }
  }

  .playground-inputs-area {
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-height: 380px;
    overflow-y: auto;
    padding-right: 4px;
    margin-bottom: 14px;

    .side-input-item {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 8px 10px;

      .s-label-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 4px;

        .s-name {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          font-weight: 700;
          color: #1D4ED8;
        }

        .s-desc {
          font-size: 11px;
          color: #64748B;
        }
      }
    }
  }

  .playground-output-area {
    border-top: 1px solid #F1F5F9;
    padding-top: 14px;

    .output-header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .o-title {
        font-size: 13px;
        font-weight: 700;
        color: #0F172A;
      }

      .o-metrics {
        display: flex;
        gap: 6px;

        .metric-pill {
          font-size: 11px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 8px;
          border-radius: 999px;
          font-family: ui-monospace, monospace;
        }
      }
    }

    .output-box {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 14px;
      min-height: 180px;

      .empty-hint {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #94A3B8;
        text-align: center;
        padding: 30px 10px;

        .hint-ic {
          color: #CBD5E1;
          margin-bottom: 8px;
        }

        p {
          margin: 0;
          font-size: 12.5px;
          max-width: 260px;
          line-height: 1.5;
        }
      }
    }
  }
}
</style>

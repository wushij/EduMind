<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 5 步：确认出题指令参数并启动生成</h3>
    <p class="pane-desc">请核对以下出题约束，AI 将调用 EduMind 命题大模型引擎进行题目编排：</p>

    <!-- 综合配置核对卡片 -->
    <div class="summary-check-panel">
      <div class="summary-grid">
        <div class="summary-item">
          <span class="s-label">课程空间：</span>
          <strong class="s-val text-blue-600">{{ selectedCourseName }}</strong>
        </div>

        <div class="summary-item">
          <span class="s-label">命题场景：</span>
          <span class="s-val font-semibold">{{ formState.questionScene || '随堂测验' }}</span>
        </div>

        <div class="summary-item">
          <span class="s-label">包含章节：</span>
          <span class="s-val">已选择 {{ formState.chapterIds.length }} 个章节</span>
        </div>

        <div class="summary-item">
          <span class="s-label">题型与规模：</span>
          <span class="s-val">
            共 <strong>{{ formState.count }}</strong> 道试题（单题基准 {{ formState.scorePerQuestion }} 分，卷面总分 <strong>{{ formState.count * formState.scorePerQuestion }}</strong> 分）
          </span>
        </div>

        <div class="summary-item col-span-2">
          <span class="s-label">核心考点：</span>
          <div class="s-kp-tags">
            <span
              v-for="kp in formState.knowledgePointNames"
              :key="kp"
              class="summary-kp-pill"
            >
              {{ kp }}
            </span>
            <span v-if="formState.knowledgePointNames.length === 0" class="text-slate-400">
              全章节核心考点综合覆盖
            </span>
          </div>
        </div>

        <div class="summary-item col-span-2">
          <span class="s-label">专属策略指令：</span>
          <div class="prompt-directive-preview">
            <span class="p-text">{{ formState.promptDirective || '标准教学命题策略' }}</span>
            <span v-if="formState.customInstruction" class="p-extra">；{{ formState.customInstruction }}</span>
          </div>
        </div>
      </div>

      <!-- 可展开查看 AI 专属 Prompt 预览 -->
      <div class="prompt-inspector-toggle">
        <button
          type="button"
          class="toggle-link-btn"
          @click="showPromptPreview = !showPromptPreview"
        >
          <el-icon class="mr-1"><View /></el-icon>
          <span>{{ showPromptPreview ? '收起 AI 专属命题 Prompt 透视' : '展开查看 AI 专属命题 Prompt 透视' }}</span>
        </button>

        <div v-if="showPromptPreview" class="prompt-code-view">
          <pre><code>{{ generatedPromptPreview }}</code></pre>
        </div>
      </div>
    </div>

    <!-- 启动出题大按钮 -->
    <div class="launch-generate-box">
      <button
        type="button"
        class="capsule-generate-btn"
        :disabled="generating"
        @click="$emit('generate')"
      >
        <span class="btn-inner-content">
          <el-icon class="lightning-ic"><Lightning /></el-icon>
          <span>{{ generating ? 'AI 命题推演引擎运行中...' : '启动 AI 智能命题（进入深度推演引擎）' }}</span>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Lightning, View } from '@element-plus/icons-vue';
import type { QuestionGenerateFormState } from './question-generate-types';

const props = defineProps<{
  formState: QuestionGenerateFormState;
  selectedCourseName: string;
  difficultyLabel: string;
  generating: boolean;
}>();

defineEmits<{
  generate: [];
}>();

const showPromptPreview = ref(false);

const generatedPromptPreview = computed(() => {
  return `【EduMind 智能命题专家指令链】
目标课程：《${props.selectedCourseName}》
考查范围：${props.formState.chapterIds.length} 个重点章节
考查知识点：${props.formState.knowledgePointNames.join('、')}
目标题型：${props.formState.questionTypes.join('、')}
难度要求：${props.difficultyLabel}
题量与分值：${props.formState.count} 题，单题 ${props.formState.scorePerQuestion} 分
教学场景：${props.formState.questionScene}
专属命题指令：${props.formState.promptDirective}${props.formState.customInstruction ? '；' + props.formState.customInstruction : ''}
科学排版规约：LaTeX 公式（$inline$ / $$block$$）、Markdown 代码块、强诱惑力干扰项诊断分析。`;
});
</script>

<style scoped lang="scss">
.step-content-pane {
  .pane-title {
    margin: 0 0 6px 0;
    font-size: 18px;
    font-weight: 800;
    color: #0F172A;
  }

  .pane-desc {
    margin: 0 0 24px 0;
    font-size: 13.5px;
    color: #64748B;
  }
}

.summary-check-panel {
  background: #FFFFFF;
  border-radius: 18px;
  padding: 22px 24px;
  border: 1.5px solid #E2E8F0;
  box-shadow: 0 4px 16px rgba(30, 80, 160, 0.04);
  margin-bottom: 28px;

  .summary-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px 24px;

    .col-span-2 {
      grid-column: span 2;
    }

    .summary-item {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      font-size: 13.5px;

      .s-label {
        color: #64748B;
        width: 100px;
        flex-shrink: 0;
        font-weight: 500;
      }

      .s-val {
        color: #0F172A;
        line-height: 1.5;
      }

      .s-kp-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .summary-kp-pill {
          padding: 2px 8px;
          border-radius: 9999px;
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          color: #1D4ED8;
          font-size: 12px;
          font-weight: 500;
        }
      }

      .prompt-directive-preview {
        flex: 1;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 8px;
        padding: 6px 12px;
        font-size: 12.5px;
        color: #334155;
        line-height: 1.5;

        .p-extra {
          color: #4F46E5;
          font-weight: 600;
        }
      }
    }
  }

  .prompt-inspector-toggle {
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px dashed #E2E8F0;

    .toggle-link-btn {
      display: inline-flex;
      align-items: center;
      background: none;
      border: none;
      color: #4F46E5;
      font-size: 12.5px;
      font-weight: 600;
      cursor: pointer;
      padding: 0;

      &:hover {
        color: #4338CA;
        text-decoration: underline;
      }
    }

    .prompt-code-view {
      margin-top: 10px;
      background: #0F172A;
      color: #38BDF8;
      border-radius: 10px;
      padding: 14px 16px;
      font-family: monospace;
      font-size: 12px;
      line-height: 1.6;
      max-height: 200px;
      overflow-y: auto;

      pre {
        margin: 0;
        white-space: pre-wrap;
      }
    }
  }
}

.launch-generate-box {
  display: flex;
  justify-content: center;

  .capsule-generate-btn {
    width: 100%;
    max-width: 720px;
    height: 52px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
    color: #FFFFFF;
    border: none;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: 0 6px 24px rgba(22, 119, 255, 0.35);
    transition: all 0.25s ease;

    .btn-inner-content {
      display: inline-flex;
      align-items: center;
      gap: 8px;

      .lightning-ic {
        font-size: 18px;
      }
    }

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 30px rgba(114, 46, 209, 0.45);
    }

    &:disabled {
      opacity: 0.75;
      cursor: wait;
    }

    .generating-text {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      font-size: 15px;

      .spinner {
        width: 20px;
        height: 20px;
        border: 2.5px solid rgba(255, 255, 255, 0.3);
        border-top-color: #FFFFFF;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

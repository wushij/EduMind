<template>
  <div class="step-content-pane">
    <h3 class="pane-title">第 3 步：设定目标题型、难度与 AI 专属命题策略</h3>
    <p class="pane-desc">配置题型与难度梯度，并注入专属大模型命题提示词指令链：</p>

    <!-- 目标题型选择 -->
    <div class="setting-group-box">
      <div class="setting-header">
        <label class="setting-label">目标生成题型（支持多选组合）</label>
        <span class="setting-tip">已选 {{ formState.questionTypes.length }} 种题型</span>
      </div>
      <div class="type-pills-selector">
        <div
          v-for="t in typeOptions"
          :key="t.type"
          class="type-pill-card"
          :class="{ active: formState.questionTypes.includes(t.type) }"
          @click="$emit('toggle-type', t.type)"
        >
          <el-icon class="type-icon" :style="{ color: t.color }">
            <component :is="t.icon" />
          </el-icon>
          <div class="type-info">
            <span class="type-label">{{ t.label }}</span>
            <span v-if="t.desc" class="type-desc">{{ t.desc }}</span>
          </div>
          <el-icon v-if="formState.questionTypes.includes(t.type)" class="type-check">
            <Check />
          </el-icon>
        </div>
      </div>
    </div>

    <!-- 预期难度与认知层级 -->
    <div class="setting-group-box">
      <div class="setting-header">
        <label class="setting-label">整体考卷预期难度与布鲁姆认知目标</label>
      </div>
      <div class="diff-radios-row">
        <div
          v-for="d in difficultyOptions"
          :key="d.val"
          class="diff-pill-radio"
          :class="[d.colorClass, { active: formState.difficulty === d.val }]"
          @click="formState.difficulty = d.val"
        >
          <span class="diff-dot"></span>
          <div class="diff-meta">
            <strong class="diff-title">{{ d.label }}</strong>
            <span class="diff-desc">{{ d.desc }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 专属 AI 命题策略提示词指令 -->
    <div class="setting-group-box ai-prompt-box">
      <div class="setting-header">
        <div class="title-with-pill">
          <label class="setting-label text-indigo-900">AI 专属命题策略与提示词指令（Prompt Directive）</label>
          <span class="ai-pill-label">大模型专属认知注入</span>
        </div>
        <span class="setting-tip">可点击快捷添加预设命题策略，大模型将严格按指令定向出题</span>
      </div>

      <!-- 命题应用场景切换 -->
      <div class="scene-selector-row">
        <span class="scene-label">教学场景：</span>
        <div class="scene-pills">
          <span
            v-for="s in sceneOptions"
            :key="s"
            class="scene-pill"
            :class="{ active: formState.questionScene === s }"
            @click="formState.questionScene = s"
          >
            {{ s }}
          </span>
        </div>
      </div>

      <!-- 预设教学策略 Prompt 标签 -->
      <div class="preset-directives-row">
        <span class="directive-tips">推荐命题策略：</span>
        <div class="directive-chips">
          <span
            v-for="preset in presetDirectives"
            :key="preset"
            class="directive-chip"
            :class="{ active: formState.promptDirective.includes(preset) }"
            @click="togglePresetDirective(preset)"
          >
            <el-icon class="mr-1"><Plus v-if="!formState.promptDirective.includes(preset)" /><Check v-else /></el-icon>
            {{ preset }}
          </span>
        </div>
      </div>

      <!-- 教师自定义命题要求 -->
      <div class="custom-prompt-input-area">
        <el-input
          v-model="formState.customInstruction"
          type="textarea"
          :rows="2"
          placeholder="教师补充专属出题指令（例如：重点考查指针偏移与越界防护；主观题需给出分步采分点）..."
          maxlength="200"
          show-word-limit
          class="prompt-textarea"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check, Plus } from '@element-plus/icons-vue';
import type { QuestionType } from '@/mock/questions';
import type { DifficultyOption, QuestionGenerateFormState, QuestionTypeOption } from './question-generate-types';

const props = defineProps<{
  formState: QuestionGenerateFormState;
  typeOptions: QuestionTypeOption[];
  difficultyOptions: DifficultyOption[];
}>();

defineEmits<{
  'toggle-type': [type: QuestionType];
}>();

const sceneOptions = ['随堂巩固测验', '章节阶段摸底', '期末综合大考', '难点错题攻坚'];

const presetDirectives = [
  '注重真实工程案例与应用实战',
  '针对高频易混淆点设计强诱惑干扰项',
  '包含代码阅读与时间/空间复杂度推导',
  '公式使用标准 LaTeX 规范排版',
  '主观题明确给出分步采分细则'
];

function togglePresetDirective(preset: string) {
  const current = props.formState.promptDirective || '';
  const parts = current.split('；').map(s => s.trim()).filter(Boolean);
  const idx = parts.indexOf(preset);
  if (idx > -1) {
    parts.splice(idx, 1);
  } else {
    parts.push(preset);
  }
  props.formState.promptDirective = parts.join('；');
}
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
    margin: 0 0 22px 0;
    font-size: 13.5px;
    color: #64748B;
  }
}

.setting-group-box {
  margin-bottom: 22px;

  .setting-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .setting-label {
      font-size: 14.5px;
      font-weight: 700;
      color: #1E293B;
    }

    .setting-tip {
      font-size: 12px;
      color: #94A3B8;
    }
  }

  .type-pills-selector {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
    gap: 12px;

    .type-pill-card {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 18px;
      border-radius: 9999px;
      border: 1.5px solid #E2E8F0;
      background: #FFFFFF;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .type-icon {
        font-size: 18px;
        flex-shrink: 0;
      }

      .type-info {
        flex: 1;
        .type-label {
          font-size: 13.5px;
          font-weight: 600;
          color: #334155;
          display: block;
        }
        .type-desc {
          font-size: 11px;
          color: #94A3B8;
          display: block;
        }
      }

      .type-check {
        color: #1677FF;
        font-size: 14px;
      }

      &:hover {
        border-color: #93C5FD;
        background: #F8FAFC;
        transform: translateY(-1px);
      }

      &.active {
        border-color: #1677FF;
        background: #EFF6FF;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.12);

        .type-label {
          color: #1677FF;
        }
      }
    }
  }

  .diff-radios-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 14px;

    .diff-pill-radio {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 18px;
      border-radius: 16px;
      border: 1.5px solid #E2E8F0;
      background: #FFFFFF;
      cursor: pointer;
      transition: all 0.2s;

      .diff-dot {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: #CBD5E1;
        flex-shrink: 0;
      }

      .diff-meta {
        .diff-title {
          font-size: 14px;
          color: #1E293B;
          display: block;
        }
        .diff-desc {
          font-size: 11.5px;
          color: #64748B;
          margin-top: 2px;
          display: block;
        }
      }

      &:hover {
        border-color: #93C5FD;
      }

      &.active {
        border-color: #1677FF;
        background: #EFF6FF;

        .diff-dot {
          background: #1677FF;
          box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.2);
        }

        .diff-title {
          color: #1677FF;
        }
      }
    }
  }

  // 专属 Prompt 容器
  &.ai-prompt-box {
    background: linear-gradient(135deg, #F8FAFC 0%, #EEF2FF 100%);
    border: 1px solid #E0E7FF;
    border-radius: 18px;
    padding: 20px 22px;

    .title-with-pill {
      display: flex;
      align-items: center;
      gap: 8px;

      .ai-pill-label {
        font-size: 11px;
        font-weight: 700;
        background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
        color: #FFFFFF;
        padding: 2px 8px;
        border-radius: 9999px;
      }
    }

    .scene-selector-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;

      .scene-label {
        font-size: 13px;
        font-weight: 600;
        color: #475569;
      }

      .scene-pills {
        display: flex;
        gap: 8px;

        .scene-pill {
          padding: 4px 12px;
          border-radius: 9999px;
          font-size: 12px;
          background: #FFFFFF;
          border: 1px solid #CBD5E1;
          color: #475569;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            border-color: #4F46E5;
            color: #4F46E5;
          }

          &.active {
            border-color: #4F46E5;
            background: #4F46E5;
            color: #FFFFFF;
            font-weight: 600;
          }
        }
      }
    }

    .preset-directives-row {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      margin-bottom: 14px;

      .directive-tips {
        font-size: 12.5px;
        font-weight: 600;
        color: #475569;
        margin-top: 4px;
        flex-shrink: 0;
      }

      .directive-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .directive-chip {
          display: inline-flex;
          align-items: center;
          padding: 4px 10px;
          border-radius: 9999px;
          background: #FFFFFF;
          border: 1px solid #CBD5E1;
          color: #475569;
          font-size: 12px;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            border-color: #4F46E5;
            color: #4F46E5;
          }

          &.active {
            border-color: #4F46E5;
            background: #EEF2FF;
            color: #4F46E5;
            font-weight: 600;
          }
        }
      }
    }

    .custom-prompt-input-area {
      .prompt-textarea {
        :deep(.el-textarea__inner) {
          border-radius: 12px;
          font-size: 13px;
        }
      }
    }
  }
}
</style>

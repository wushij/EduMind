<template>
  <div class="pref-card">
    <div class="card-header-bar">
      <div class="section-icon-badge ai-badge">
        <el-icon><Cpu /></el-icon>
      </div>
      <div class="section-title-wrap">
        <h3 class="card-section-title">AI 智能教学引擎与应答偏好</h3>
        <span class="section-subtitle">自定义基座大模型调度、思维链推理展现与智能问答教师语气</span>
      </div>
      <el-tag size="small" effect="plain" type="success" class="section-pill-tag">AI 教学大脑</el-tag>
    </div>

    <div class="options-grid">
      <!-- 默认优先推理模型 (动态后端网关互通) -->
      <div class="pref-row">
        <div class="row-info">
          <div class="row-title-line">
            <span class="row-title">默认优先推理基座模型</span>
            <el-button
              link
              type="primary"
              size="small"
              :icon="Refresh"
              :loading="modelsLoading"
              @click="fetchDynamicModels"
              class="refresh-models-btn"
            >
              刷新模型池
            </el-button>
          </div>
          <span class="row-desc">
            在发起教学备课答疑、智能出题组卷或学情分析时，系统优先调度唤醒的基座大语言模型。
          </span>
        </div>
        <el-select
          v-model="preferenceStore.preferences.defaultModel"
          size="default"
          class="pill-select model-select"
          placeholder="请选择基座大模型"
          style="width: 290px"
        >
          <el-option
            v-for="model in availableModels"
            :key="model.modelKey"
            :label="model.name"
            :value="model.modelKey"
          >
            <div class="model-option-item">
              <div class="model-meta-main">
                <span class="model-name">{{ model.name }}</span>
                <el-tag size="small" :type="resolveProviderTagType(model.provider)" effect="light" class="provider-tag">
                  {{ model.provider }}
                </el-tag>
              </div>
              <div class="model-meta-sub">
                <span class="context-tag" v-if="model.contextLength">
                  {{ Math.round(model.contextLength / 1024) }}K 上下文
                </span>
                <span class="recommend-badge" v-if="model.isDefault">官方推荐</span>
              </div>
            </div>
          </el-option>
        </el-select>
      </div>

      <!-- RAG 知识库检索增强 -->
      <div class="pref-row">
        <div class="row-info">
          <div class="row-title-line">
            <span class="row-title">默认启用 RAG 校本知识库精准检索增强</span>
            <el-tag size="small" type="info" effect="plain" class="feature-tag">严谨教学溯源</el-tag>
          </div>
          <span class="row-desc">
            发起教学问答时，自动对齐校本课程教材、知识图谱及题库知识切片，输出具备学术引用标识的高置信度回答。
          </span>
        </div>
        <el-switch v-model="preferenceStore.preferences.enableRag" class="custom-switch" />
      </div>

      <!-- 思维链 Deep Thinking 展现模式 -->
      <div class="pref-row">
        <div class="row-info">
          <span class="row-title">深度思考过程 (Deep Thinking) 默认呈现策略</span>
          <span class="row-desc">
            面对复杂理科解题推理、高难度公式推导或命题生成时，AI 推理思考链的默认折叠展现形式。
          </span>
        </div>
        <el-radio-group
          v-model="preferenceStore.preferences.thinkingDisplayMode"
          size="default"
          class="pill-radio-group"
        >
          <el-radio-button label="EXPANDED">
            <span class="opt-btn-inner">
              <el-icon><Expand /></el-icon>
              <span>默认展开</span>
            </span>
          </el-radio-button>
          <el-radio-button label="COLLAPSED">
            <span class="opt-btn-inner">
              <el-icon><Fold /></el-icon>
              <span>默认折叠</span>
            </span>
          </el-radio-button>
          <el-radio-button label="HIDDEN">
            <span class="opt-btn-inner">
              <el-icon><Hide /></el-icon>
              <span>仅看最终解答</span>
            </span>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- AI 教师角色语气风格 -->
      <div class="pref-row">
        <div class="row-info">
          <span class="row-title">AI 助教教学启发风格偏好</span>
          <span class="row-desc">
            设置助教解答学生疑难问题时的教学语气与引导深度，匹配不同阶段的教学辅导需求。
          </span>
        </div>
        <el-select
          v-model="preferenceStore.preferences.aiTone"
          size="default"
          class="pill-select"
          style="width: 290px"
        >
          <el-option label="温和启发型 (循循善诱，多抛出引导问题启发思考)" value="HEURISTIC" />
          <el-option label="严谨学术型 (推理严密，提供标准公理、定理与论证)" value="RIGOROUS" />
          <el-option label="备考提分型 (直击考点考法，归纳题型套路与易错点)" value="EXAM_ORIENTED" />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Cpu, Refresh, Expand, Fold, Hide } from '@element-plus/icons-vue';
import type { usePreferenceStore } from '@/stores/user/preference';
import type { ModelProviderConfig } from '@/types/system/model';

defineProps<{
  preferenceStore: ReturnType<typeof usePreferenceStore>;
  modelsLoading: boolean;
  availableModels: ModelProviderConfig[];
  resolveProviderTagType: (provider: string) => string;
  fetchDynamicModels: () => void;
}>();
</script>

<style scoped lang="scss">
.pref-card {
  background: var(--el-bg-color, #ffffff);
  border-radius: 20px;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  padding: 22px 26px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(37, 99, 235, 0.25);
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  }

  .card-header-bar {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--el-border-color-extra-light, #f1f5f9);

    .section-icon-badge {
      width: 38px;
      height: 38px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;

      &.ai-badge {
        background: #dbeafe;
        color: #2563eb;
      }
    }

    .section-title-wrap {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;

      .card-section-title {
        font-size: 16px;
        font-weight: 700;
        color: var(--el-text-color-primary, #0f172a);
        margin: 0;
      }

      .section-subtitle {
        font-size: 12px;
        color: var(--el-text-color-secondary, #64748b);
      }
    }

    .section-pill-tag {
      border-radius: 9999px;
      font-weight: 600;
      font-size: 11.5px;
      padding: 2px 10px;
    }
  }

  .options-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .pref-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      background: var(--el-fill-color-light, #f8fafc);
      border-radius: 14px;
      border: 1px solid var(--el-border-color-extra-light, #edf2f7);
      gap: 20px;
      flex-wrap: wrap;
      transition: all 0.2s ease;

      &:hover {
        background: var(--el-fill-color, #f1f5f9);
        border-color: var(--el-border-color-lighter, #e2e8f0);
      }

      .row-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;
        min-width: 0;

        .row-title {
          font-size: 14px;
          font-weight: 700;
          line-height: 1.45;
          color: var(--el-text-color-primary, #1e293b);
        }

        .row-title-line {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .feature-tag {
            border-radius: 9999px;
            font-size: 11px;
            height: 20px;
            padding: 0 8px;
          }

          .refresh-models-btn {
            font-size: 12px;
            padding: 0;
            margin-left: 4px;
          }
        }

        .row-desc {
          font-size: 12px;
          color: var(--el-text-color-secondary, #64748b);
          line-height: 1.5;
        }
      }
    }
  }
}

/* 药丸单选：外层统一轨道，内层选项无独立边框，避免 Element Plus 连体按钮的方角描边 */
:deep(.pill-radio-group) {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 5px;
  background: var(--el-bg-color, #ffffff);
  border: 1px solid var(--el-border-color-lighter, #e8edf3);
  border-radius: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);

  .el-radio-button {
    margin: 0 !important;

    .el-radio-button__inner {
      border: none !important;
      border-radius: 9999px !important;
      margin: 0 !important;
      padding: 7px 14px;
      font-size: 12.5px;
      font-weight: 600;
      line-height: 1.2;
      background: transparent;
      color: var(--el-text-color-regular, #64748b);
      box-shadow: none !important;
      transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
    }

    &:first-child .el-radio-button__inner,
    &:last-child .el-radio-button__inner {
      border-radius: 9999px !important;
    }

    .el-radio-button__original-radio:checked + .el-radio-button__inner {
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
      color: #ffffff !important;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.28) !important;
    }

    &:not(.is-active) .el-radio-button__inner:hover {
      background: var(--el-fill-color-light, #f1f5f9);
      color: var(--el-text-color-primary, #334155);
    }
  }

  .opt-btn-inner {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
  }
}

:deep(.pill-select) {
  .el-input__wrapper {
    border-radius: 9999px !important;
    padding: 4px 14px;
    box-shadow: 0 0 0 1px var(--el-border-color-lighter, #e2e8f0) inset;
    background: var(--el-bg-color, #ffffff);
  }
}

.model-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;

  .model-meta-main {
    display: flex;
    align-items: center;
    gap: 6px;

    .model-name {
      font-weight: 600;
      color: var(--el-text-color-primary, #1e293b);
    }

    .provider-tag {
      border-radius: 6px;
      font-size: 10.5px;
      padding: 0 5px;
      height: 18px;
    }
  }

  .model-meta-sub {
    display: flex;
    align-items: center;
    gap: 4px;

    .context-tag {
      font-size: 11px;
      color: #94a3b8;
      font-family: monospace;
    }

    .recommend-badge {
      font-size: 10.5px;
      background: #fef2f2;
      color: #ef4444;
      padding: 1px 5px;
      border-radius: 4px;
      font-weight: 600;
    }
  }
}

:deep(.custom-switch) {
  --el-switch-on-color: #2563eb;
}
</style>

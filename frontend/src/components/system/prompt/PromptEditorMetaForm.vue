<template>
  <div class="section-card meta-section">
    <div class="section-card-header">
      <div class="sch-left">
        <span class="sec-icon"><el-icon><Cpu /></el-icon></span>
        <span class="sec-title">基础工程参数与大模型绑定</span>
      </div>
      <span class="sec-hint">配置模板标识、模型路由与采样超参数</span>
    </div>

    <div class="meta-form-body">
      <div class="grid-row-3">
        <div class="form-item-group">
          <label class="item-label">模板标识编码 (Code) <span class="req">*</span></label>
          <el-input
            v-model="form.code"
            placeholder="如 COURSE_RAG_GENERAL"
            class="mono-input"
          />
        </div>

        <div class="form-item-group">
          <label class="item-label">模板中文名称 <span class="req">*</span></label>
          <el-input v-model="form.name" placeholder="如 通用课程问答 (RAG)" />
        </div>

        <div class="form-item-group">
          <label class="item-label">所属业务分类 <span class="req">*</span></label>
          <el-select v-model="form.category" style="width: 100%">
            <el-option value="rag" label="课程问答 (RAG)" />
            <el-option value="question" label="智能命题 (Exam)" />
            <el-option value="grading" label="智能批改 (Grading)" />
            <el-option value="teaching" label="教案备课" />
            <el-option value="agent" label="Agent 规划" />
          </el-select>
        </div>
      </div>

      <div class="grid-row-3">
        <div class="form-item-group">
          <div class="label-with-val">
            <label class="item-label">默认绑定模型</label>
            <span v-if="modelsLoading" class="loading-mini">加载模型中...</span>
          </div>
          <el-select
            v-model="form.boundModel"
            style="width: 100%"
            filterable
            clearable
            :loading="modelsLoading"
            placeholder="未指定模型（选填，运行时跟随系统网关）"
          >
            <el-option value="" :label="defaultModelLabel">
              <div class="model-option-item">
                <span class="m-name" style="color: #64748B">{{ defaultModelLabel }}</span>
                <el-tag size="small" type="info" effect="plain">自适应</el-tag>
              </div>
            </el-option>
            <el-option
              v-for="item in modelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <div class="model-option-item">
                <div class="m-main">
                  <span class="m-name">{{ item.label }}</span>
                  <span
                    v-if="item.modelName && item.label !== item.modelName"
                    class="m-code"
                  >({{ item.modelName }})</span>
                </div>
                <div class="m-tags">
                  <el-tag v-if="item.isDefault" size="small" type="success" effect="plain">默认</el-tag>
                  <el-tag size="small" type="info" effect="light">{{ item.provider }}</el-tag>
                </div>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="form-item-group">
          <div class="label-with-val">
            <label class="item-label">采样温度 (Temperature)</label>
            <span class="val-tag">{{ form.temperature }}</span>
          </div>
          <el-slider
            v-model="form.temperature"
            :min="0.0"
            :max="1.0"
            :step="0.05"
            class="custom-slider"
          />
        </div>

        <div class="form-item-group">
          <label class="item-label">最大输出 Token</label>
          <div class="token-input-wrap">
            <el-input-number
              v-model="form.maxTokens"
              :min="256"
              :max="16384"
              :step="500"
              style="width: 100%"
            />
          </div>
        </div>
      </div>

      <div class="form-item-group full-width">
        <label class="item-label">模板定位与适用范围描述</label>
        <el-input
          v-model="form.description"
          placeholder="清晰阐明该提示词工程资产的教学业务场景、知识库依赖范围与安全边界..."
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Cpu } from '@element-plus/icons-vue';
import type { PromptTemplate } from '@/types/system/prompt';
import type { ModelOption } from '@/composables/system/usePromptEditor';

defineProps<{
  form: PromptTemplate;
  modelOptions: ModelOption[];
  modelsLoading: boolean;
  defaultModelLabel: string;
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
        background: #EFF6FF;
        color: #2563EB;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
      }

      .sec-title {
        font-size: 14.5px;
        font-weight: 700;
        color: #0F172A;
      }
    }

    .sec-hint {
      font-size: 11.5px;
      color: #64748B;
    }
  }
}

.meta-section {
  .meta-form-body {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .grid-row-3 {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 14px;

      @media (max-width: 800px) {
        grid-template-columns: 1fr;
      }
    }

    .form-item-group {
      display: flex;
      flex-direction: column;
      gap: 6px;

      &.full-width {
        width: 100%;
      }

      .item-label {
        font-size: 12.5px;
        font-weight: 600;
        color: #334155;

        .req {
          color: #EF4444;
          margin-left: 2px;
        }
      }

      .label-with-val {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .loading-mini {
          font-size: 11px;
          color: #64748B;
        }

        .val-tag {
          font-family: ui-monospace, monospace;
          font-size: 11.5px;
          font-weight: 700;
          color: #2563EB;
          background: #EFF6FF;
          padding: 1px 8px;
          border-radius: 999px;
        }
      }

      .mono-input {
        :deep(input) {
          font-family: ui-monospace, monospace;
          font-size: 12.5px;
          font-weight: 600;
          color: #1E40AF;
        }
      }
    }
  }
}
</style>

<style lang="scss">
.model-option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;

  .m-main {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    .m-name {
      font-weight: 600;
      color: #0F172A;
    }

    .m-code {
      font-family: ui-monospace, monospace;
      font-size: 11px;
      color: #64748B;
    }
  }

  .m-tags {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}
</style>

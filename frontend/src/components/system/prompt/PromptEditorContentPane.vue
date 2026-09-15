<template>
  <div class="prompt-content-pane">
    <div class="section-card prompt-editor-box">
      <div class="section-card-header">
        <div class="sch-left">
          <span class="sec-icon navy"><el-icon><CollectionTag /></el-icon></span>
          <div class="sec-title-wrap">
            <span class="sec-title">System Prompt (系统人设与硬性约束规则)</span>
            <span class="sec-sub">界定 AI 角色人设、课程数据边界、检索引用规则与防注入屏障</span>
          </div>
        </div>
        <div class="sch-actions">
          <el-button
            size="small"
            link
            :icon="CopyDocument"
            @click="onCopy(form.systemPrompt, 'System Prompt 已复制')"
          >
            复制
          </el-button>
          <el-button size="small" link @click="systemExpanded = !systemExpanded">
            {{ systemExpanded ? '收起高度' : '展开高度' }}
          </el-button>
        </div>
      </div>

      <div class="editor-textarea-wrap dark-theme" :class="{ expanded: systemExpanded }">
        <el-input
          v-model="form.systemPrompt"
          type="textarea"
          :rows="systemExpanded ? 24 : 10"
          placeholder="在此编排大模型 System Prompt 核心指令..."
          class="code-textarea"
        />
      </div>
    </div>

    <div class="section-card prompt-editor-box">
      <div class="section-card-header">
        <div class="sch-left">
          <span class="sec-icon blue"><el-icon><ChatDotRound /></el-icon></span>
          <div class="sec-title-wrap">
            <span class="sec-title">User Prompt Template (用户指令插槽模板)</span>
            <span class="sec-sub">与用户输入及业务上下文结合的指令插槽</span>
          </div>
        </div>
        <div class="sch-actions">
          <el-button size="small" link :icon="Refresh" @click="onSyncVariables">
            自动同步变量
          </el-button>
          <el-button
            size="small"
            link
            :icon="CopyDocument"
            @click="onCopy(form.userPromptTemplate, 'User Prompt 模板已复制')"
          >
            复制
          </el-button>
        </div>
      </div>

      <div class="variable-quick-bar">
        <span class="bar-title">点击快速插入变量插槽：</span>
        <div class="chips-list">
          <el-tag
            v-for="v in form.variables"
            :key="v.name"
            size="small"
            class="var-clickable-chip"
            @click="onInsertVar(v.name)"
          >
            + &#123;&#123;{{ v.name }}&#125;&#125;
          </el-tag>
        </div>
      </div>

      <div class="editor-textarea-wrap slate-theme">
        <el-input
          v-model="form.userPromptTemplate"
          type="textarea"
          :rows="6"
          placeholder="如：请基于当前课程知识库回答下面的问题。\n当前课程：{{course_name}}\n用户问题：{{question}}"
          class="code-textarea user-textarea"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CollectionTag, ChatDotRound, CopyDocument, Refresh } from '@element-plus/icons-vue';
import type { PromptTemplate } from '@/types/system/prompt';

defineProps<{
  form: PromptTemplate;
  onCopy: (text: string, msg?: string) => void;
  onInsertVar: (name: string) => void;
  onSyncVariables: () => void;
}>();

const systemExpanded = defineModel<boolean>('systemExpanded', { required: true });
</script>

<style scoped lang="scss">
.prompt-content-pane {
  display: flex;
  flex-direction: column;
}

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

        &.navy { background: #0F172A; color: #60A5FA; }
        &.blue { background: #E0F2FE; color: #0284C7; }
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

    .sch-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

.prompt-editor-box {
  .variable-quick-bar {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    padding: 8px 12px;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .bar-title {
      font-size: 11.5px;
      color: #64748B;
      font-weight: 500;
    }

    .chips-list {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;

      .var-clickable-chip {
        cursor: pointer;
        font-family: ui-monospace, monospace;
        font-size: 11px;
        background: #FFFFFF;
        border-color: #CBD5E1;
        color: #1E293B;
        transition: all 0.15s;

        &:hover {
          background: #EFF6FF;
          border-color: #93C5FD;
          color: #2563EB;
        }
      }
    }
  }

  .editor-textarea-wrap {
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid #1E293B;
    background: #0F172A;
    transition: all 0.2s ease;
    box-sizing: border-box;

    &:focus-within {
      border-color: #3B82F6 !important;
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.25) !important;
    }

    :deep(.el-textarea) {
      display: block;
      width: 100%;
      margin: 0 !important;
      padding: 0 !important;
      border: none !important;
      box-shadow: none !important;
      background: transparent !important;
    }

    :deep(.el-textarea__inner) {
      display: block !important;
      width: 100% !important;
      margin: 0 !important;
      border: none !important;
      outline: none !important;
      box-shadow: none !important;
      border-radius: 0 !important;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace !important;
      font-size: 13px !important;
      line-height: 1.7 !important;
      padding: 14px 16px !important;
      resize: vertical;
      transition: none !important;

      &:hover,
      &:focus {
        border: none !important;
        outline: none !important;
        box-shadow: none !important;
      }
    }

    &.dark-theme {
      background: #0F172A;
      border-color: #1E293B;

      :deep(.el-textarea__inner) {
        background: #0F172A !important;
        color: #E2E8F0 !important;

        &::placeholder {
          color: #475569 !important;
        }
      }
    }

    &.slate-theme {
      background: #1E293B;
      border-color: #334155;

      :deep(.el-textarea__inner) {
        background: #1E293B !important;
        color: #93C5FD !important;

        &::placeholder {
          color: #64748B !important;
        }
      }
    }
  }
}
</style>

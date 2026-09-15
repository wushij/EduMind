<template>
  <div class="editor-header-card">
    <div class="header-left">
      <el-button
        link
        class="back-btn"
        :icon="ArrowLeft"
        @click="onBack"
      >
        返回模板中心
      </el-button>

      <div class="title-meta-row">
        <h2 class="template-title">{{ form.name || '新建 Prompt 模板' }}</h2>
        <span class="code-badge" @click="onCopyCode">
          {{ form.code }}
          <el-icon class="copy-ic"><CopyDocument /></el-icon>
        </span>
        <span class="category-pill" :class="form.category">
          {{ getCategoryLabel(form.category) }}
        </span>
        <span class="version-badge">{{ form.version }}</span>
        <span
          class="status-indicator"
          :class="form.status === 'PUBLISHED' ? 'online' : 'draft'"
        >
          <span class="pulse-dot"></span>
          {{ form.status === 'PUBLISHED' ? '已发布生产' : '草稿' }}
        </span>
      </div>
    </div>

    <div class="header-right">
      <el-button :icon="Check" class="save-draft-btn" @click="onSave">
        保存草稿
      </el-button>
      <el-button
        type="primary"
        class="publish-btn"
        :icon="Upload"
        :loading="publishing"
        @click="onPublish"
      >
        发布生效至网关
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Check, Upload, CopyDocument } from '@element-plus/icons-vue';
import type { PromptTemplate } from '@/types/system/prompt';

defineProps<{
  form: PromptTemplate;
  publishing: boolean;
  getCategoryLabel: (category: string) => string;
  onBack: () => void;
  onCopyCode: () => void;
  onSave: () => void;
  onPublish: () => void;
}>();
</script>

<style scoped lang="scss">
.editor-header-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);

  @media (max-width: 900px) {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .back-btn {
      padding: 0;
      font-size: 13px;
      color: #64748b;
      align-self: flex-start;

      &:hover {
        color: #2563eb;
      }
    }

    .title-meta-row {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      .template-title {
        margin: 0;
        font-size: 20px;
        font-weight: 800;
        color: #0f172a;
        letter-spacing: -0.01em;
      }

      .code-badge {
        font-family: ui-monospace, SFMono-Regular, monospace;
        font-size: 11.5px;
        font-weight: 700;
        color: #1d4ed8;
        background: #eff6ff;
        padding: 3px 8px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        gap: 4px;
        cursor: pointer;
        transition: all 0.15s;

        &:hover {
          background: #dbeafe;
        }

        .copy-ic {
          font-size: 11px;
        }
      }

      .category-pill {
        font-size: 11.5px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 6px;

        &.rag {
          background: #e0f2fe;
          color: #0369a1;
        }
        &.question {
          background: #fef3c7;
          color: #b45309;
        }
        &.grading {
          background: #f3e8ff;
          color: #7e22ce;
        }
        &.teaching {
          background: #dcfce7;
          color: #15803d;
        }
      }

      .version-badge {
        font-family: ui-monospace, monospace;
        font-size: 11px;
        color: #64748b;
        background: #f1f5f9;
        padding: 2px 8px;
        border-radius: 999px;
      }

      .status-indicator {
        font-size: 11.5px;
        font-weight: 600;
        padding: 2px 10px;
        border-radius: 999px;
        display: flex;
        align-items: center;
        gap: 6px;

        &.online {
          background: #ecfdf5;
          color: #059669;
          .pulse-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #10b981;
          }
        }
        &.draft {
          background: #fffbeb;
          color: #d97706;
          .pulse-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #f59e0b;
          }
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .save-draft-btn {
      background: #f8fafc;
      border: 1px solid #cbd5e1;
      color: #334155;
      font-weight: 500;

      &:hover {
        background: #f1f5f9;
        color: #0f172a;
      }
    }

    .publish-btn {
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      border: none;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
      font-weight: 600;
      padding: 9px 20px;
      transition: background-color 0.15s ease, box-shadow 0.15s ease;

      &:hover {
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.45);
      }

      &:active {
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
      }
    }
  }
}
</style>

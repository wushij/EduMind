<template>
  <header class="ocr-studio-toolbar">
    <!-- 左侧：文档与引擎配置区 -->
    <div class="toolbar-section toolbar-section--left">
      <!-- 试卷快速预设选择 -->
      <div class="doc-selector-wrapper">
        <el-tooltip :content="selectedPaperName" placement="bottom" :show-after="600">
          <el-select
            :model-value="selectedPresetId"
            size="default"
            class="preset-select"
            @change="$emit('update:selectedPresetId', $event)"
          >
            <template #prefix>
              <el-icon class="prefix-icon"><Document /></el-icon>
            </template>
            <el-option
              v-for="paper in presetPapers"
              :key="paper.id"
              :label="paper.name"
              :value="paper.id"
            />
          </el-select>
        </el-tooltip>
      </div>

      <!-- 上传本地试卷图片/PDF -->
      <el-upload
        :show-file-list="false"
        :auto-upload="false"
        accept="image/*,.pdf"
        :on-change="handleUploadChange"
        class="upload-trigger-uploader"
      >
        <el-button size="default" class="toolbar-btn toolbar-btn--outline">
          <el-icon><Upload /></el-icon>
          <span class="btn-text">上传试卷/图片</span>
        </el-button>
      </el-upload>

      <!-- 多模态 OCR 引擎选择 -->
      <div class="engine-select-box">
        <el-select
          :model-value="selectedEngine"
          size="default"
          class="engine-select"
          :disabled="isProcessing"
          @change="handleEngineChange"
        >
          <template #prefix>
            <span class="engine-dot" :class="engineDotClass" />
          </template>
          <el-option label="MinerU (高精公式)" value="MINERU" />
          <el-option label="PaddleOCR-v4" value="PADDLE_OCR" />
          <el-option label="GPT-4o Vision" value="GPT4O_VISION" />
        </el-select>
      </div>
    </div>

    <!-- 中间：翻页导航与视图视口控制 -->
    <div class="toolbar-section toolbar-section--center">
      <!-- 缩略图大纲切换开关 -->
      <el-tooltip :content="showThumbnails ? '收起试卷多页缩略图' : '展开试卷多页缩略图'" placement="bottom">
        <button
          type="button"
          class="thumbnail-toggle-btn"
          :class="{ active: showThumbnails }"
          @click="$emit('toggleThumbnails')"
        >
          <el-icon><Grid /></el-icon>
          <span class="btn-label">大纲</span>
        </button>
      </el-tooltip>

      <!-- 聚合翻页胶囊 -->
      <div class="page-switcher-capsule">
        <button
          type="button"
          class="page-nav-btn"
          :disabled="currentPageIdx <= 0 || isProcessing"
          title="上一页"
          @click="$emit('prevPage')"
        >
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <span class="page-nav-indicator">
          第 <strong>{{ totalPages > 0 ? currentPageIdx + 1 : 0 }}</strong> / {{ totalPages }} 页
        </span>
        <button
          type="button"
          class="page-nav-btn"
          :disabled="currentPageIdx >= totalPages - 1 || isProcessing"
          title="下一页"
          @click="$emit('nextPage')"
        >
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <!-- 缩放控制组 -->
      <div class="zoom-controls-capsule">
        <button
          type="button"
          class="zoom-btn"
          title="缩小"
          @click="$emit('updateZoom', -0.1)"
        >
          -
        </button>
        <span class="zoom-text">{{ Math.round(zoomScale * 100) }}%</span>
        <button
          type="button"
          class="zoom-btn"
          title="放大"
          @click="$emit('updateZoom', 0.1)"
        >
          +
        </button>
        <button
          type="button"
          class="zoom-reset-btn"
          title="重置缩放 100%"
          @click="$emit('resetZoom')"
        >
          100%
        </button>
      </div>
    </div>

    <!-- 右侧：核心动作操作组 -->
    <div class="toolbar-section toolbar-section--right">
      <!-- 重新识别当前页 -->
      <el-button
        size="default"
        class="toolbar-btn toolbar-btn--refresh"
        :disabled="isProcessing"
        @click="$emit('reRunOcr')"
      >
        <el-icon><Refresh /></el-icon>
        <span class="btn-text">重新识别</span>
      </el-button>

      <!-- AI 智能辅助校对按钮 (永不折行的专属微发光胶囊) -->
      <button
        type="button"
        class="ai-studio-action-btn"
        @click="$emit('openAiDrawer')"
      >
        <el-icon class="sparkle-icon"><MagicStick /></el-icon>
        <span class="btn-text">AI 智能辅助校对</span>
        <span class="badge-tag">大模型</span>
      </button>

      <!-- 一键批量入库至题库 (主操作) -->
      <el-button
        size="default"
        type="primary"
        class="toolbar-btn toolbar-btn--primary"
        @click="$emit('openIngestModal')"
      >
        <el-icon><Files /></el-icon>
        <span class="btn-text">一键批量入库至题库</span>
      </el-button>

      <!-- 确认入库至知识库 -->
      <el-button
        size="default"
        type="success"
        class="toolbar-btn toolbar-btn--success"
        :disabled="isProcessing"
        @click="$emit('confirmKnowledge')"
      >
        <el-icon><Check /></el-icon>
        <span class="btn-text">入库至知识库</span>
      </el-button>

      <!-- 窄屏更多操作折叠菜单 -->
      <div class="more-actions-dropdown">
        <el-dropdown trigger="click" placement="bottom-end">
          <button type="button" class="more-trigger-btn" title="更多操作">
            <el-icon><MoreFilled /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="studio-dropdown-menu">
              <el-dropdown-item @click="$emit('reRunOcr')">
                <el-icon><Refresh /></el-icon> 重新识别本页切片
              </el-dropdown-item>
              <el-dropdown-item @click="$emit('openAiDrawer')">
                <el-icon><MagicStick /></el-icon> AI 智能辅助校对
              </el-dropdown-item>
              <el-dropdown-item divided @click="$emit('openIngestModal')">
                <el-icon><Files /></el-icon> 一键入库至题库
              </el-dropdown-item>
              <el-dropdown-item @click="$emit('confirmKnowledge')">
                <el-icon><Check /></el-icon> 确认入库至知识库
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Document,
  Upload,
  ArrowLeft,
  ArrowRight,
  Refresh,
  MagicStick,
  Files,
  Check,
  Grid,
  MoreFilled
} from '@element-plus/icons-vue';
import type { PresetPaper } from '@/composables/knowledge/useOcrWorkspace';

const props = defineProps<{
  selectedPresetId: string;
  presetPapers: PresetPaper[];
  selectedEngine: 'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION';
  isProcessing: boolean;
  currentPageIdx: number;
  totalPages: number;
  showThumbnails: boolean;
  zoomScale: number;
}>();

const emit = defineEmits<{
  'update:selectedPresetId': [id: string];
  'update:selectedEngine': [engine: 'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION'];
  'upload': [file: File];
  'prevPage': [];
  'nextPage': [];
  'toggleThumbnails': [];
  'updateZoom': [delta: number];
  'resetZoom': [];
  'reRunOcr': [];
  'openAiDrawer': [];
  'openIngestModal': [];
  'confirmKnowledge': [];
}>();

const selectedPaperName = computed(() => {
  const paper = props.presetPapers.find((p) => p.id === props.selectedPresetId);
  return paper?.name || '未知试卷';
});

const engineDotClass = computed(() => {
  switch (props.selectedEngine) {
    case 'MINERU':
      return 'dot-purple';
    case 'GPT4O_VISION':
      return 'dot-green';
    default:
      return 'dot-blue';
  }
});

function handleEngineChange(val: any) {
  emit('update:selectedEngine', val as 'MINERU' | 'PADDLE_OCR' | 'GPT4O_VISION');
}

function handleUploadChange(uploadFile: { raw?: File }) {
  if (uploadFile.raw) {
    emit('upload', uploadFile.raw);
  }
}
</script>

<style scoped lang="scss">
.ocr-studio-toolbar {
  width: 100%;
  background: #FFFFFF;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.04);
  padding: 10px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  box-sizing: border-box;
  flex-shrink: 0;

  .toolbar-section {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    &--left {
      flex: 0 1 auto;
      min-width: 0;

      .doc-selector-wrapper {
        width: 250px;
        min-width: 140px;
        flex-shrink: 1;

        :deep(.el-input__wrapper) {
          border-radius: 999px;
          background: #F8FAFC;
          box-shadow: 0 0 0 1px #E2E8F0 inset;

          &:hover,
          &.is-focus {
            background: #FFFFFF;
            box-shadow: 0 0 0 1.5px #2563EB inset;
          }
        }

        .prefix-icon {
          color: #2563EB;
          font-size: 15px;
          margin-right: 4px;
        }

        :deep(.el-input__inner) {
          font-size: 13px;
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
        }
      }

      .upload-trigger-uploader {
        flex-shrink: 0;
      }

      .engine-select-box {
        width: 155px;
        flex-shrink: 0;

        :deep(.el-input__wrapper) {
          border-radius: 999px;
          background: #F8FAFC;
          box-shadow: 0 0 0 1px #E2E8F0 inset;

          &:hover,
          &.is-focus {
            background: #FFFFFF;
            box-shadow: 0 0 0 1.5px #2563EB inset;
          }
        }

        .engine-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          display: inline-block;
          margin-right: 6px;

          &.dot-purple {
            background: #7C3AED;
            box-shadow: 0 0 6px rgba(124, 58, 237, 0.5);
          }

          &.dot-green {
            background: #10B981;
            box-shadow: 0 0 6px rgba(16, 185, 129, 0.5);
          }

          &.dot-blue {
            background: #2563EB;
            box-shadow: 0 0 6px rgba(37, 99, 235, 0.5);
          }
        }
      }
    }

    &--center {
      justify-content: center;
      gap: 12px;

      .thumbnail-toggle-btn {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 32px;
        padding: 0 12px;
        border-radius: 999px;
        border: 1px solid #E2E8F0;
        background: #F8FAFC;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        white-space: nowrap;

        &:hover {
          background: #FFFFFF;
          color: #2563EB;
          border-color: #BFDBFE;
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);
        }

        &.active {
          background: #EFF6FF;
          color: #2563EB;
          border-color: #93C5FD;
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
        }
      }

      .page-switcher-capsule {
        display: inline-flex;
        align-items: center;
        background: #F1F5F9;
        border-radius: 999px;
        padding: 2px 4px;

        .page-nav-btn {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          border: none;
          background: transparent;
          color: #475569;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover:not(:disabled) {
            background: #FFFFFF;
            color: #2563EB;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
          }

          &:disabled {
            opacity: 0.35;
            cursor: not-allowed;
          }
        }

        .page-nav-indicator {
          font-size: 13px;
          color: #334155;
          padding: 0 10px;
          white-space: nowrap;

          strong {
            color: #0F172A;
            font-weight: 700;
          }
        }
      }

      .zoom-controls-capsule {
        display: inline-flex;
        align-items: center;
        background: #F8FAFC;
        border-radius: 999px;
        border: 1px solid #E2E8F0;
        padding: 2px 4px;

        .zoom-btn {
          width: 24px;
          height: 24px;
          border-radius: 50%;
          border: none;
          background: transparent;
          color: #64748B;
          font-size: 14px;
          font-weight: 700;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.15s ease;

          &:hover {
            background: #E2E8F0;
            color: #0F172A;
          }
        }

        .zoom-text {
          font-size: 12px;
          font-weight: 600;
          color: #334155;
          padding: 0 6px;
          min-width: 40px;
          text-align: center;
          font-variant-numeric: tabular-nums;
        }

        .zoom-reset-btn {
          font-size: 11px;
          color: #2563EB;
          background: #EFF6FF;
          border: none;
          padding: 2px 6px;
          border-radius: 999px;
          cursor: pointer;
          font-weight: 600;
          transition: all 0.15s ease;

          &:hover {
            background: #DBEAFE;
          }
        }
      }
    }

    &--right {
      justify-content: flex-end;
      gap: 10px;

      .toolbar-btn {
        border-radius: 999px;
        font-size: 13px;
        font-weight: 500;
        white-space: nowrap;
        flex-shrink: 0;

        &--outline {
          border-color: #CBD5E1;
          color: #334155;

          &:hover {
            border-color: #2563EB;
            color: #2563EB;
            background: #F0F7FF;
          }
        }

        &--refresh {
          border-color: #E2E8F0;
          color: #475569;
        }

        &--primary {
          background: #2563EB;
          border-color: #2563EB;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);

          &:hover {
            background: #1D4ED8;
            border-color: #1D4ED8;
          }
        }

        &--success {
          background: #10B981;
          border-color: #10B981;
          box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);

          &:hover {
            background: #059669;
            border-color: #059669;
          }
        }
      }

      .ai-studio-action-btn {
        background: linear-gradient(135deg, #2563EB 0%, #7C3AED 100%);
        border: none;
        color: #FFFFFF;
        padding: 0 16px;
        height: 32px;
        border-radius: 999px;
        font-weight: 600;
        font-size: 13px;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        cursor: pointer;
        white-space: nowrap;
        flex-shrink: 0;
        box-shadow: 0 4px 14px rgba(124, 58, 237, 0.25);
        transition: all 0.25s ease;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(124, 58, 237, 0.35);
        }

        .sparkle-icon {
          font-size: 14px;
        }

        .btn-text {
          white-space: nowrap;
        }

        .badge-tag {
          background: rgba(255, 255, 255, 0.22);
          font-size: 10px;
          padding: 1px 6px;
          border-radius: 999px;
          letter-spacing: 0.5px;
        }
      }

      .more-actions-dropdown {
        display: none;

        .more-trigger-btn {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          border: 1px solid #E2E8F0;
          background: #F8FAFC;
          color: #64748B;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;

          &:hover {
            color: #0F172A;
            background: #E2E8F0;
          }
        }
      }
    }
  }
}

/* 响应式断点适配：窄屏时防止任何挤出或换行 */
@media (max-width: 1400px) {
  .ocr-studio-toolbar {
    .toolbar-section--right {
      .toolbar-btn--refresh {
        display: none;
      }
    }
  }
}

@media (max-width: 1280px) {
  .ocr-studio-toolbar {
    .toolbar-section--left {
      .doc-selector-wrapper {
        width: 170px;
      }
      .engine-select-box {
        width: 135px;
      }
    }

    .toolbar-section--right {
      .toolbar-btn--success {
        display: none;
      }
      .more-actions-dropdown {
        display: block;
      }
    }
  }
}
</style>

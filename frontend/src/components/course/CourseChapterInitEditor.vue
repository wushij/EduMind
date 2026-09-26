<template>
  <div class="form-block-card course-chapter-init-editor">
    <div class="block-card-header">
      <div class="block-icon-circle purple-glow">
        <el-icon><List /></el-icon>
      </div>
      <div class="block-title-box">
        <h3 class="block-title">2. 教学大纲与章节初始化</h3>
        <p class="block-desc">全链路直接生成教学章节，杜绝新课程“0章节空壳”，可利用 AI 一键智能推导</p>
      </div>

      <div class="chapter-ai-action">
        <button
          v-if="isAiGeneratingOutline"
          type="button"
          class="ai-generate-capsule-btn is-loading is-cancellable"
          title="点击中止本次大纲推演"
          @click="$emit('cancel-ai-generate')"
        >
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>推演中 ({{ thinkingTimeText || '0.0s' }}) · 停止</span>
        </button>
        <button
          v-else
          type="button"
          class="ai-generate-capsule-btn"
          @click="$emit('ai-generate')"
        >
          <el-icon class="ai-btn-icon"><AiSparkleIcon /></el-icon>
          <span>AI <span class="ai-btn-text-optional">智能</span>推荐课程大纲</span>
        </button>
      </div>
    </div>

    <div class="block-card-body">
      <div class="chapters-dynamic-list">
        <!-- AI 生成中微动效卡片 (与学情分析推演保持一致的 AI 认知推导动画，含推演计时与终止) -->
        <div v-if="isAiGeneratingOutline" class="ai-generating-banner">
          <div class="bot-avatar is-breathing">
            <el-icon class="is-spin"><Cpu /></el-icon>
          </div>
          <div class="generating-info">
            <div class="generating-title-row">
              <span class="generating-badge">DeepSeek AI 认知推演中</span>
              <span class="generating-timer-badge">
                <el-icon><Timer /></el-icon>
                <span>已推演 {{ thinkingTimeText || '0.0s' }}</span>
              </span>
              <span class="generating-hint">正在基于课程学科体系，推导演进结构化章节大纲目录...</span>
            </div>
            <div class="generating-shimmer-bar">
              <div class="shimmer-progress"></div>
            </div>
          </div>
          <button
            type="button"
            class="cancel-thinking-btn"
            title="中止本次大纲推演"
            @click="$emit('cancel-ai-generate')"
          >
            <el-icon><VideoPause /></el-icon>
            <span>停止生成</span>
          </button>
        </div>

        <template v-else-if="initialChapters.length > 0">
          <div
            v-for="(chapter, idx) in initialChapters"
            :key="idx"
            class="chapter-pill-item-row"
          >
            <div class="chapter-seq-badge">第 {{ idx + 1 }} 章</div>
            <el-input
              :model-value="chapter"
              placeholder="输入章节主标题..."
              size="default"
              class="chapter-title-input"
              @update:model-value="(value: string) => $emit('update-chapter', idx, String(value ?? ''))"
            />
            <button
              type="button"
              class="chapter-del-btn"
              title="移除本章"
              @click="$emit('remove-chapter', idx)"
            >
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </template>

        <!-- 默认为空时的优雅提示 -->
        <div v-else class="chapters-empty-placeholder">
          <div class="empty-icon-circle">
            <el-icon><List /></el-icon>
          </div>
          <p class="empty-main-text">暂未添加教学大纲章节</p>
          <p class="empty-sub-text">可点击右上角「AI 智能推荐课程大纲」一键智能生成，或点击下方手动添加章节</p>
        </div>

        <button
          type="button"
          class="add-chapter-dashed-btn"
          :disabled="isAiGeneratingOutline"
          @click="$emit('add-chapter')"
        >
          <el-icon><Plus /></el-icon>
          <span>添加新大纲章节</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { List, Plus, Close, Loading, Cpu, Timer, VideoPause } from '@element-plus/icons-vue';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

defineProps<{
  initialChapters: string[];
  isAiGeneratingOutline: boolean;
  thinkingTimeText?: string;
}>();

defineEmits<{
  'ai-generate': [];
  'cancel-ai-generate': [];
  'add-chapter': [];
  'remove-chapter': [idx: number];
  'update-chapter': [idx: number, value: string];
}>();
</script>

<style scoped lang="scss">
.course-chapter-init-editor {
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid #EBF1F7;
  box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
  padding: 24px 28px;
  margin-bottom: 20px;
  transition: box-shadow 0.22s;

  &:hover {
    box-shadow: 0 6px 24px rgba(30, 80, 150, 0.07);
  }

  .block-card-header {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 14px;
    margin-bottom: 22px;
    padding-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;

    .block-icon-circle {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;

      &.purple-glow {
        background: #F5F3FF;
        color: #7C3AED;
      }
    }

    .block-title-box {
      flex: 1;
      min-width: 0;

      .block-title {
        margin: 0 0 3px 0;
        font-size: 16px;
        font-weight: 700;
        color: #0F172A;
      }

      .block-desc {
        margin: 0;
        font-size: 12.5px;
        color: #64748B;
      }
    }
  }

  .block-card-body {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  // 右上角 AI 一键生成入口：跟随标题行右对齐
  .chapter-ai-action {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-left: auto;

    // 窄屏空间不足时省略「智能」，退化为「AI 推荐课程大纲」
    @media (max-width: 640px) {
      .ai-btn-text-optional {
        display: none;
      }
    }

    .ai-generate-capsule-btn {
      display: inline-flex;
      flex-shrink: 0;
      align-items: center;
      white-space: nowrap;
      gap: 7px;
      height: 36px;
      padding: 0 18px;
      border-radius: 9999px;
      background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);
      border: none;
      color: #FFFFFF;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 3px 10px rgba(124, 58, 237, 0.25);
      transition: all 0.22s;

      .ai-btn-icon {
        font-size: 15px;
      }

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(124, 58, 237, 0.35);
      }

      &:disabled {
        opacity: 0.85;
        cursor: not-allowed;
      }

      &.is-loading {
        background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%);
        box-shadow: 0 3px 12px rgba(99, 102, 241, 0.35);

        &.is-cancellable {
          cursor: pointer;
          &:hover {
            background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
            box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
          }
        }
      }
    }

  }

  .chapters-dynamic-list {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .chapter-pill-item-row {
      display: flex;
      align-items: center;
      gap: 12px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      padding: 8px 10px 8px 14px;
      transition: all 0.2s;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.05);
      }

      .chapter-seq-badge {
        font-size: 12px;
        font-weight: 700;
        color: #1677FF;
        background: #EFF6FF;
        padding: 2px 10px;
        border-radius: 9999px;
        white-space: nowrap;
      }

      .chapter-title-input {
        flex: 1;

        :deep(.el-input__wrapper) {
          box-shadow: none !important;
          background: transparent !important;
          padding: 0;
          height: 32px;
        }

        :deep(.el-input__inner) {
          font-size: 13.5px;
          color: #1E293B;
          font-weight: 500;
        }
      }

      .chapter-del-btn {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: none;
        background: #F1F5F9;
        color: #94A3B8;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-size: 12px;
        transition: all 0.2s;

        &:hover {
          background: #FEE2E2;
          color: #EF4444;
        }
      }
    }

    .ai-generating-banner {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px 20px;
      border-radius: 16px;
      background: linear-gradient(135deg, #FAF5FF 0%, #F5F3FF 50%, #EFF6FF 100%);
      border: 1px solid #E9D5FF;
      box-shadow: 0 4px 14px rgba(124, 58, 237, 0.08);

      .bot-avatar {
        width: 42px;
        height: 42px;
        border-radius: 50%;
        background: linear-gradient(135deg, #722ED1 0%, #9333EA 100%);
        color: #FFFFFF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;
        box-shadow: 0 3px 10px rgba(114, 46, 209, 0.3);

        &.is-breathing {
          animation: botPulse 2s infinite ease-in-out;
        }

        .is-spin {
          animation: spin 3s linear infinite;
        }
      }

      .generating-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .generating-title-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .generating-badge {
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 9999px;
          background: #F3E8FF;
          color: #7E22CE;
          font-weight: 700;
        }

        .generating-timer-badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 11.5px;
          font-family: var(--font-mono, monospace);
          padding: 2px 9px;
          border-radius: 9999px;
          background: #EFF6FF;
          color: #2563EB;
          font-weight: 600;
          border: 1px solid #DBEAFE;
        }

        .generating-hint {
          font-size: 13px;
          color: #475569;
          font-weight: 500;
        }
      }

      .generating-shimmer-bar {
        width: 100%;
        height: 5px;
        background: #E2E8F0;
        border-radius: 9999px;
        overflow: hidden;
        position: relative;

        .shimmer-progress {
          width: 35%;
          height: 100%;
          background: linear-gradient(90deg, #7C3AED 0%, #2563EB 50%, #7C3AED 100%);
          border-radius: 9999px;
          animation: shimmerSlide 1.5s infinite ease-in-out;
        }
      }

      .cancel-thinking-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 14px;
        border-radius: 9999px;
        background: #FFFFFF;
        border: 1px solid #FCA5A5;
        color: #DC2626;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        white-space: nowrap;
        box-shadow: 0 2px 6px rgba(220, 38, 38, 0.08);
        transition: all 0.2s;
        flex-shrink: 0;

        &:hover {
          background: #FEF2F2;
          border-color: #EF4444;
          transform: translateY(-1px);
          box-shadow: 0 4px 10px rgba(220, 38, 38, 0.15);
        }
      }
    }

    .chapters-empty-placeholder {
      padding: 26px 20px;
      text-align: center;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;
      border-radius: 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 6px;

      .empty-icon-circle {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: #F1F5F9;
        color: #94A3B8;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 17px;
        margin-bottom: 2px;
      }

      .empty-main-text {
        margin: 0;
        font-size: 13.5px;
        font-weight: 600;
        color: #475569;
      }

      .empty-sub-text {
        margin: 0;
        font-size: 12px;
        color: #94A3B8;
        max-width: 460px;
        line-height: 1.5;
      }
    }

    .add-chapter-dashed-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      height: 38px;
      border-radius: 9999px;
      border: 1.5px dashed #CBD5E1;
      background: transparent;
      color: #64748B;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      &:hover:not(:disabled) {
        border-color: #1677FF;
        color: #1677FF;
        background: #F0F7FF;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}

@keyframes botPulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 3px 10px rgba(114, 46, 209, 0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 5px 16px rgba(114, 46, 209, 0.5);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes shimmerSlide {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(350%);
  }
}
</style>

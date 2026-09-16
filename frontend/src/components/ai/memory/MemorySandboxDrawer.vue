<template>
  <el-drawer
    :model-value="visible"
    title="Agent 长期记忆语义召回沙盒测试"
    size="560px"
    class="memory-sandbox-drawer"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="sandbox-container">
      <div class="sandbox-intro-box">
        <el-icon class="intro-icon"><MagicStick /></el-icon>
        <p class="intro-text">
          在提问或规划任务前，Agent 将自动调用本空间的向量语义管线召回 Top-K 相关上下文片段，并以非侵入方式拼接入 System Prompt。在此可对召回效果进行即时诊断调试：
        </p>
      </div>

      <div class="quick-prompts-row">
        <span class="label">常用场景快捷注入：</span>
        <button
          v-for="(p, i) in quickPrompts"
          :key="i"
          type="button"
          class="prompt-chip"
          @click="$emit('update:queryPrompt', p)"
        >
          {{ p }}
        </button>
      </div>

      <div class="prompt-input-box">
        <el-input
          :model-value="queryPrompt"
          type="textarea"
          :rows="3"
          placeholder="输入测试场景提问 Prompt（例如：请根据我平时的复习习惯，帮我安排 3 道导数练习大题）..."
          @update:model-value="$emit('update:queryPrompt', $event)"
        />
        <el-button
          type="primary"
          class="gradient-recall-btn"
          :loading="loading"
          @click="$emit('retrieve')"
        >
          <el-icon><Search /></el-icon>
          <span>执行向量语义召回匹配</span>
        </el-button>
      </div>

      <div class="recalled-results-section" v-if="recalledItems.length > 0">
        <div class="results-header">
          <span class="title">召回命中的记忆片段 (Top-{{ recalledItems.length }})</span>
          <span class="sub">加权匹配得分越接近 100% 越优先拼入提示词</span>
        </div>

        <div class="recalled-cards-list">
          <div
            v-for="(rec, idx) in recalledItems"
            :key="rec.id"
            class="recalled-capsule-card"
          >
            <div class="card-top">
              <span class="rank-badge">#{{ idx + 1 }}</span>
              <div class="score-pill">
                <span class="score-label">匹配度</span>
                <span class="score-val">{{ Math.round((rec.confidenceScore || 0.9) * 100) }}%</span>
              </div>
              <span class="type-badge">{{ getMemoryTypeLabel(rec.memoryType) }}</span>
            </div>
            <p class="recalled-summary">{{ rec.summary }}</p>
          </div>
        </div>

        <!-- 模拟系统 Prompt 注入效果 -->
        <div class="prompt-injection-preview">
          <div class="preview-title">
            <el-icon><Document /></el-icon>
            <span>助教 System Prompt 注入段实时模拟：</span>
          </div>
          <pre class="code-preview"><code>### 🧠 学员个性化认知与长效偏好（严谨受控）
{{ simulatedPromptBlock }}</code></pre>
        </div>
      </div>

      <el-empty
        v-else-if="hasSearched"
        description="本次检索未召回相关记忆片段 (可能因相关度低于阈值或当前空间暂无匹配记忆)"
      />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { MagicStick, Search, Document } from '@element-plus/icons-vue';
import type { MemoryItemVO } from '@/types/ai/memory';
import { getMemoryTypeLabel } from '@/composables/ai/useAgentMemory';

const props = defineProps<{
  visible: boolean;
  queryPrompt: string;
  recalledItems: MemoryItemVO[];
  loading?: boolean;
  hasSearched?: boolean;
}>();

defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'update:queryPrompt', val: string): void;
  (e: 'retrieve'): void;
}>();

const quickPrompts = [
  '请结合我平时的做题节奏与易错考点，帮我针对性讲解一道典型例题',
  '我对抽象公式推导理解慢，请用通俗生动的比喻讲解这个原理',
  '帮我检查这道题的解答过程，指出我容易忽略的边界约束'
];

const simulatedPromptBlock = computed(() => {
  if (props.recalledItems.length === 0) return '（暂无召回片段）';
  return props.recalledItems
    .map((r) => `- [${r.memoryType || 'PREFERENCE'}] ${r.summary}`)
    .join('\n');
});
</script>

<style scoped lang="scss">
.sandbox-container {
  display: flex;
  flex-direction: column;
  gap: 18px;

  .sandbox-intro-box {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 16px;
    padding: 14px 16px;

    .intro-icon {
      font-size: 20px;
      color: #2563EB;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .intro-text {
      font-size: 13px;
      color: #1E40AF;
      line-height: 1.6;
      margin: 0;
    }
  }

  .quick-prompts-row {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .label {
      font-size: 12px;
      font-weight: 600;
      color: #475569;
    }

    .prompt-chip {
      text-align: left;
      font-size: 12px;
      color: #334155;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 9999px;
      padding: 6px 14px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #EFF6FF;
        border-color: #93C5FD;
        color: #2563EB;
      }
    }
  }

  .prompt-input-box {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .gradient-recall-btn {
      border-radius: 9999px;
      padding: 10px;
      font-size: 14px;
      font-weight: 600;
      background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
      border: none;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
      }
    }
  }

  .recalled-results-section {
    display: flex;
    flex-direction: column;
    gap: 14px;
    margin-top: 6px;

    .results-header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;

      .title {
        font-size: 14px;
        font-weight: 700;
        color: #0F172A;
      }

      .sub {
        font-size: 11px;
        color: #94A3B8;
      }
    }

    .recalled-cards-list {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .recalled-capsule-card {
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 16px;
        padding: 12px 16px;
        transition: all 0.2s ease;

        &:hover {
          background: #FFFFFF;
          border-color: #93C5FD;
          box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
        }

        .card-top {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 6px;

          .rank-badge {
            font-size: 12px;
            font-weight: 700;
            color: #2563EB;
          }

          .score-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            padding: 2px 8px;
            border-radius: 9999px;
            background: #ECFDF5;
            color: #059669;
            font-weight: 600;
          }

          .type-badge {
            font-size: 11px;
            color: #64748B;
          }
        }

        .recalled-summary {
          font-size: 13px;
          color: #334155;
          line-height: 1.55;
          margin: 0;
        }
      }
    }

    .prompt-injection-preview {
      background: #0F172A;
      border-radius: 16px;
      padding: 14px 16px;
      color: #E2E8F0;

      .preview-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #94A3B8;
        margin-bottom: 8px;
      }

      .code-preview {
        margin: 0;
        font-family: var(--font-mono, monospace);
        font-size: 12px;
        line-height: 1.6;
        white-space: pre-wrap;
        color: #38BDF8;
      }
    }
  }
}
</style>

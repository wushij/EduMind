<template>
  <el-dialog
    :model-value="visible"
    title="AI 学情智能认知提炼引擎"
    width="640px"
    class="memory-extract-dialog"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <AiCognitiveThinkingPanel
      v-if="loading"
      :active="loading"
      v-bind="AI_COGNITIVE_THINKING_PRESETS.memoryExtract"
      show-footer-actions
      @abort="handleCancel"
    />

    <!-- 2. AI 萃取与思考完成状态 -->
    <div v-else class="extract-dialog-content">
      <div class="dialog-tip">
        <el-icon class="tip-icon"><CircleCheck /></el-icon>
        <div class="tip-text">
          <p class="tip-headline">AI 深度推演已就绪 · 待采纳特征候选清单</p>
          <span>已基于本空间交互完成深度分析，<strong>未确认前绝不写入列表</strong>，请审阅并勾选确认采纳：</span>
        </div>
      </div>

      <!-- 萃取结果候选列表 -->
      <div v-if="candidates.length > 0" class="candidate-list">
        <div
          v-for="(item, idx) in candidates"
          :key="item.id || idx"
          class="candidate-card clickable-card"
          :class="{
            'is-selected': selectedMap[idx],
            'is-unselected': !selectedMap[idx],
            'is-reinforced': item.isNewlyCreated === false
          }"
          @click="toggleSelect(idx)"
        >
          <div class="candidate-header">
            <div class="header-left">
              <div class="select-checkbox" :class="{ checked: selectedMap[idx] }">
                <el-icon v-if="selectedMap[idx]"><Check /></el-icon>
              </div>
              <el-tag size="small" :type="getTypeTag(item.memoryType)" effect="light" class="type-pill">
                {{ getTypeLabel(item.memoryType) }}
              </el-tag>
              <span v-if="item.isNewlyCreated !== false" class="status-badge new-badge">
                全新候选
              </span>
              <span v-else class="status-badge reinforced-badge">
                强化已有特征
              </span>
            </div>

            <div class="header-right">
              <span class="adopt-badge" :class="{ 'is-active': selectedMap[idx] }">
                {{ selectedMap[idx] ? '勾选采纳' : '跳过舍弃' }}
              </span>
              <span class="conf-pill">
                置信度 {{ Math.round((item.confidenceScore || 0.95) * 100) }}%
              </span>
            </div>
          </div>

          <p class="candidate-summary">{{ item.summary }}</p>

          <!-- AI 思考推导依据 (Reasoning Trace) -->
          <div v-if="item.reasoning" class="reasoning-box">
            <div class="reasoning-title">
              <el-icon class="reason-icon"><Opportunity /></el-icon>
              <span>AI 认知推导依据</span>
            </div>
            <p class="reasoning-text">{{ item.reasoning }}</p>
          </div>
        </div>
      </div>

      <!-- 无新增特征时的友好提示 -->
      <div v-else class="empty-candidate-box">
        <el-icon class="empty-icon"><CircleCheck /></el-icon>
        <p class="empty-title">当前问答与学情暂无新增认知特征</p>
        <p class="empty-sub">已有的学习习惯已在记忆库中完整覆盖，系统已自适应刷新历史条目的有效权重。</p>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <template v-if="!loading">
          <template v-if="candidates.length > 0">
            <div class="adopt-actions-group">
              <el-button
                class="pill-ctrl-btn secondary-btn"
                @click="handleDismiss"
              >
                暂不采纳全部 (放弃)
              </el-button>
              <el-button
                type="primary"
                class="confirm-btn gradient-btn"
                :loading="confirming"
                :disabled="selectedCount === 0"
                @click="handleConfirm"
              >
                <el-icon><Check /></el-icon>
                <span>确认采纳并沉淀入库 ({{ selectedCount }})</span>
              </el-button>
            </div>
          </template>
          <template v-else>
            <el-button
              type="primary"
              class="confirm-btn"
              @click="$emit('update:visible', false)"
            >
              已知晓并返回
            </el-button>
          </template>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { CircleCheck, Opportunity, Check } from '@element-plus/icons-vue';
import type { MemoryItemVO } from '@/types/ai/memory';
import { getMemoryTypeLabel } from '@/composables/ai/useAgentMemory';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';

const props = defineProps<{
  visible: boolean;
  candidates: MemoryItemVO[];
  loading?: boolean;
  confirming?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'cancel'): void;
  (e: 'confirm', selected: MemoryItemVO[]): void;
}>();

const selectedMap = ref<Record<number, boolean>>({});

watch(
  () => props.candidates,
  (list) => {
    const map: Record<number, boolean> = {};
    if (Array.isArray(list)) {
      list.forEach((_, i) => {
        map[i] = true;
      });
    }
    selectedMap.value = map;
  },
  { immediate: true }
);

function toggleSelect(idx: number) {
  selectedMap.value[idx] = !selectedMap.value[idx];
}

const selectedCount = computed(() => {
  return Object.values(selectedMap.value).filter(Boolean).length;
});

function handleConfirm() {
  const selected = props.candidates.filter((_, idx) => selectedMap.value[idx]);
  emit('confirm', selected);
}

function handleDismiss() {
  emit('update:visible', false);
}

function handleCancel() {
  emit('cancel');
  emit('update:visible', false);
}

function getTypeLabel(type?: string) {
  return getMemoryTypeLabel(type);
}

function getTypeTag(type?: string) {
  switch (type) {
    case 'PREFERENCE': return 'success';
    case 'PROFILE': return 'primary';
    case 'EPISODIC': return 'warning';
    default: return 'info';
  }
}
</script>

<style scoped lang="scss">
.memory-extract-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
  }
}

/* AI 萃取成果展示 */
.extract-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .dialog-tip {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    background: #F0FDF4;
    border: 1px solid #BBF7D0;
    border-radius: 18px;
    padding: 14px 16px;

    .tip-icon {
      font-size: 20px;
      color: #059669;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .tip-text {
      .tip-headline {
        font-size: 14px;
        font-weight: 600;
        color: #065F46;
        margin: 0 0 4px;
      }
      span {
        font-size: 12px;
        color: #047857;
        line-height: 1.5;
      }
    }
  }

  .candidate-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 380px;
    overflow-y: auto;
    padding-right: 4px;

    .candidate-card {
      background: #FFFFFF;
      border: 1.5px solid #E2E8F0;
      border-radius: 18px;
      padding: 14px 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
      transition: all 0.25s ease;

      &.clickable-card {
        cursor: pointer;
        user-select: none;
      }

      &.is-selected {
        border-color: #3B82F6;
        background: #F8FAFC;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);
      }

      &.is-unselected {
        opacity: 0.55;
        border-color: #E2E8F0;
        background: #FAFAFA;
      }

      &:hover {
        border-color: #93C5FD;
      }

      &.is-reinforced {
        border-left: 4px solid #3B82F6;
      }

      .candidate-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .header-left {
          display: flex;
          align-items: center;
          gap: 8px;

          .select-checkbox {
            width: 18px;
            height: 18px;
            border-radius: 5px;
            border: 1.5px solid #94A3B8;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            transition: all 0.2s ease;
            background: #FFFFFF;

            &.checked {
              background: #2563EB;
              border-color: #2563EB;
              color: #FFFFFF;
            }
          }

          .type-pill {
            border-radius: 9999px;
            font-weight: 500;
          }

          .status-badge {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 9999px;

            &.new-badge {
              background: #DCFCE7;
              color: #15803D;
              border: 1px solid #86EFAC;
            }

            &.reinforced-badge {
              background: #EFF6FF;
              color: #1D4ED8;
              border: 1px solid #BFDBFE;
            }
          }
        }

        .header-right {
          display: flex;
          align-items: center;
          gap: 8px;

          .adopt-badge {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 9999px;
            transition: all 0.2s ease;

            &.is-active {
              background: #EFF6FF;
              color: #1D4ED8;
              border: 1px solid #BFDBFE;
            }

            &:not(.is-active) {
              background: #F1F5F9;
              color: #94A3B8;
              border: 1px solid #E2E8F0;
            }
          }

          .conf-pill {
            font-size: 12px;
            font-weight: 700;
            color: #2563EB;
            background: #F1F5F9;
            padding: 3px 10px;
            border-radius: 9999px;
          }
        }
      }

      .candidate-summary {
        font-size: 13px;
        font-weight: 500;
        color: #1E293B;
        line-height: 1.6;
        margin: 0 0 10px;
      }

      .reasoning-box {
        background: #F8FAFC;
        border: 1px dashed #CBD5E1;
        border-radius: 12px;
        padding: 10px 12px;

        .reasoning-title {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 11px;
          font-weight: 600;
          color: #475569;
          margin-bottom: 4px;

          .reason-icon {
            font-size: 14px;
            color: #EAB308;
          }
        }

        .reasoning-text {
          font-size: 12px;
          color: #64748B;
          line-height: 1.5;
          margin: 0;
        }
      }
    }
  }

  .empty-candidate-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 30px 20px;
    background: #F8FAFC;
    border: 1px dashed #CBD5E1;
    border-radius: 18px;

    .empty-icon {
      font-size: 32px;
      color: #10B981;
      margin-bottom: 8px;
    }

    .empty-title {
      font-size: 14px;
      font-weight: 600;
      color: #1E293B;
      margin: 0 0 4px;
    }

    .empty-sub {
      font-size: 12px;
      color: #64748B;
      text-align: center;
      margin: 0;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  width: 100%;

  .adopt-actions-group {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    width: 100%;

    .secondary-btn {
      border-radius: 9999px;
      padding: 8px 18px;
      font-weight: 500;
      color: #64748B;
      border-color: #CBD5E1;

      &:hover {
        color: #1E293B;
        border-color: #94A3B8;
        background-color: #F8FAFC;
      }
    }
  }
}

.confirm-btn {
  border-radius: 9999px;
  padding: 8px 24px;
  font-weight: 500;
}

</style>

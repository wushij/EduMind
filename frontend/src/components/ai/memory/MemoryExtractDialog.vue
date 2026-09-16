<template>
  <el-dialog
    :model-value="visible"
    title="AI 学情智能认知提炼引擎"
    width="640px"
    class="memory-extract-dialog"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <!-- 1. AI 正在深度思考与研判状态 -->
    <div v-if="loading" class="thinking-wrapper">
      <div class="thinking-radar-box" :class="{ 'is-paused': isPaused }">
        <div class="pulse-ring ring-1"></div>
        <div class="pulse-ring ring-2"></div>
        <div class="pulse-ring ring-3"></div>

        <!-- 精巧浅色科技罗盘星芒主体 -->
        <div class="compass-pill-core">
          <svg viewBox="0 0 48 48" class="compass-svg" fill="none">
            <defs>
              <!-- 明亮科技蓝与紫罗兰渐变 -->
              <linearGradient id="extractCompassGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#2563EB" />
                <stop offset="50%" stop-color="#3B82F6" />
                <stop offset="100%" stop-color="#7C3AED" />
              </linearGradient>
              <linearGradient id="extractNorthStarGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#F59E0B" />
                <stop offset="100%" stop-color="#FBBF24" />
              </linearGradient>
              <!-- 清新浅白蓝内盘背景渐变（告别深色） -->
              <linearGradient id="extractInnerBgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF" />
                <stop offset="100%" stop-color="#F0F7FF" />
              </linearGradient>
              <filter id="extractStarGlow" x="-20%" y="-20%" width="140%" height="140%">
                <feGaussianBlur stdDeviation="1.2" result="blur" />
                <feComposite in="SourceGraphic" in2="blur" operator="over" />
              </filter>
            </defs>

            <!-- 外层刻度圈 (慢速旋转) -->
            <circle
              cx="24"
              cy="24"
              r="20"
              stroke="url(#extractCompassGrad)"
              stroke-width="1.8"
              stroke-dasharray="2 4"
              class="compass-dial"
            />

            <!-- 清爽浅色内盘底色 -->
            <circle
              cx="24"
              cy="24"
              r="15"
              fill="url(#extractInnerBgGrad)"
              stroke="rgba(37, 99, 235, 0.28)"
              stroke-width="1.2"
              class="compass-inner-core"
            />

            <!-- 罗盘四向极光星芒 -->
            <path
              d="M24 11 L26 22 L37 24 L26 26 L24 37 L22 26 L11 24 L22 22 Z"
              fill="url(#extractCompassGrad)"
              filter="url(#extractStarGlow)"
              class="compass-star"
            />

            <!-- 北针高亮极星 (晨金点缀) -->
            <path d="M24 11 L26 24 L24 22 Z" fill="#FFFFFF" opacity="0.95" />
            <path d="M24 11 L22 24 L24 22 Z" fill="url(#extractNorthStarGrad)" />

            <!-- 中心罗盘微宝石 -->
            <circle cx="24" cy="24" r="2.8" fill="#FFFFFF" stroke="rgba(37, 99, 235, 0.4)" stroke-width="0.8" />
            <circle cx="24" cy="24" r="1.4" fill="#2563EB" />
          </svg>
        </div>
      </div>

      <div class="thinking-header">
        <h4 class="thinking-title">
          {{ isPaused ? 'AI 认知引擎推演已暂停' : 'AI 认知引擎正在深度推演分析中...' }}
        </h4>
        <span class="thinking-timer" :class="{ 'timer-paused': isPaused }">
          {{ isPaused ? '已暂停 ' + elapsedTimeText : elapsedTimeText }}
        </span>
      </div>

      <!-- 流水线阶段步骤条 -->
      <div class="pipeline-steps">
        <div class="pipeline-step" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
          <span class="step-dot"></span>
          <span class="step-label">检索本空间师生问答交互与错题日志</span>
        </div>
        <div class="pipeline-step" :class="{ active: currentStep >= 2, done: currentStep > 2 }">
          <span class="step-dot"></span>
          <span class="step-label">调用认知心理学模型推导多维学习风格与薄弱点</span>
        </div>
        <div class="pipeline-step" :class="{ active: currentStep >= 3, done: currentStep > 3 }">
          <span class="step-dot"></span>
          <span class="step-label">执行语义去重对比与 PIPL 隐私合规安全评级</span>
        </div>
      </div>
    </div>

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
        <div v-else class="thinking-action-buttons">
          <el-button
            :type="isPaused ? 'primary' : 'warning'"
            plain
            class="pill-ctrl-btn pause-btn"
            @click="togglePause"
          >
            <el-icon><VideoPlay v-if="isPaused" /><VideoPause v-else /></el-icon>
            <span>{{ isPaused ? '继续推演' : '暂停推演' }}</span>
          </el-button>

          <el-button
            type="danger"
            plain
            class="pill-ctrl-btn abort-btn"
            @click="handleCancel"
          >
            <el-icon><Close /></el-icon>
            <span>中止并退出</span>
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue';
import { CircleCheck, Opportunity, VideoPause, VideoPlay, Close, Check } from '@element-plus/icons-vue';
import type { MemoryItemVO } from '@/types/ai/memory';
import { getMemoryTypeLabel } from '@/composables/ai/useAgentMemory';

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

const currentStep = ref(1);
const isPaused = ref(false);
const elapsedTimeText = ref('0.0s');
let accumulatedMs = 0;
let lastStartTime = 0;
let timerId: number | null = null;
let stepTimerId: number | null = null;

watch(
  () => props.loading,
  (isLoading) => {
    if (isLoading) {
      currentStep.value = 1;
      isPaused.value = false;
      accumulatedMs = 0;
      elapsedTimeText.value = '0.0s';
      startTimers();
    } else {
      stopTimers();
    }
  },
  { immediate: true }
);

function startTimers() {
  stopTimers();
  lastStartTime = Date.now();
  resumeTimers();
}

function resumeTimers() {
  if (timerId !== null) clearInterval(timerId);
  timerId = window.setInterval(() => {
    if (!isPaused.value) {
      const currentMs = accumulatedMs + (Date.now() - lastStartTime);
      elapsedTimeText.value = (currentMs / 1000).toFixed(1) + 's';
      if (currentMs < 900) {
        currentStep.value = 1;
      } else if (currentMs < 1900) {
        currentStep.value = 2;
      } else {
        currentStep.value = 3;
      }
    }
  }, 100);
}

function pauseTimers() {
  if (timerId !== null) {
    clearInterval(timerId);
    timerId = null;
  }
}

function togglePause() {
  if (isPaused.value) {
    isPaused.value = false;
    lastStartTime = Date.now();
    resumeTimers();
  } else {
    isPaused.value = true;
    accumulatedMs += Date.now() - lastStartTime;
    pauseTimers();
  }
}

function handleCancel() {
  stopTimers();
  emit('cancel');
  emit('update:visible', false);
}

function stopTimers() {
  if (timerId !== null) {
    clearInterval(timerId);
    timerId = null;
  }
  if (stepTimerId !== null) {
    clearTimeout(stepTimerId);
    stepTimerId = null;
  }
}

onUnmounted(() => {
  stopTimers();
});

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

/* 1. AI 思考中科技动效 */
.thinking-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px 20px;

  .thinking-radar-box {
    position: relative;
    width: 76px;
    height: 76px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 18px;

    .compass-pill-core {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, #FFFFFF 0%, #F0F7FF 100%);
      border: 1.4px solid rgba(59, 130, 246, 0.38);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 16px rgba(37, 99, 235, 0.16), 0 0 10px rgba(124, 58, 237, 0.08);
      z-index: 2;

      .compass-svg {
        width: 32px;
        height: 32px;
      }

      .compass-dial {
        transform-origin: center;
        animation: dialSlowSpin 20s linear infinite;
      }

      .compass-star {
        transform-origin: center;
        animation: starGlowBreath 3.5s ease-in-out infinite;
      }
    }

    &.is-paused {
      .compass-dial,
      .compass-star,
      .pulse-ring {
        animation-play-state: paused !important;
      }
    }

    .pulse-ring {
      position: absolute;
      border-radius: 50%;
      border: 1.2px solid rgba(59, 130, 246, 0.3);
      animation: pulse-out 2.4s ease-out infinite;

      &.ring-1 {
        width: 52px;
        height: 52px;
        animation-delay: 0s;
      }
      &.ring-2 {
        width: 64px;
        height: 64px;
        animation-delay: 0.8s;
      }
      &.ring-3 {
        width: 76px;
        height: 76px;
        animation-delay: 1.6s;
      }
    }
  }

  .thinking-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;

    .thinking-title {
      font-size: 16px;
      font-weight: 600;
      color: #1E293B;
      margin: 0;
    }

    .thinking-timer {
      font-size: 13px;
      font-weight: 700;
      color: #2563EB;
      background: #EFF6FF;
      border: 1px solid #BFDBFE;
      border-radius: 9999px;
      padding: 2px 10px;
      transition: all 0.3s ease;

      &.timer-paused {
        color: #D97706;
        background: #FFFBEB;
        border-color: #FDE68A;
      }
    }
  }

  .pipeline-steps {
    width: 100%;
    max-width: 480px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .pipeline-step {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 16px;
      border-radius: 9999px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      transition: all 0.3s ease;

      .step-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: #CBD5E1;
        transition: all 0.3s ease;
      }

      .step-label {
        font-size: 13px;
        color: #64748B;
        font-weight: 500;
      }

      &.active {
        background: #EFF6FF;
        border-color: #93C5FD;

        .step-dot {
          background: #3B82F6;
          box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
        }

        .step-label {
          color: #1D4ED8;
          font-weight: 600;
        }
      }

      &.done {
        background: #F0FDF4;
        border-color: #BBF7D0;

        .step-dot {
          background: #10B981;
        }

        .step-label {
          color: #15803D;
        }
      }
    }
  }
}

/* 2. AI 萃取成果展示 */
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

.thinking-action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;

  .pill-ctrl-btn {
    border-radius: 9999px;
    padding: 8px 18px;
    font-weight: 500;
    transition: all 0.25s ease;

    &.pause-btn {
      border-color: #FDE68A;
      &:hover {
        background-color: #FEF3C7;
      }
    }

    &.abort-btn {
      border-color: #FECACA;
      &:hover {
        background-color: #FEE2E2;
      }
    }
  }
}

.confirm-btn {
  border-radius: 9999px;
  padding: 8px 24px;
  font-weight: 500;
}

@keyframes pulse-out {
  0% {
    transform: scale(0.6);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

@keyframes dialSlowSpin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes starGlowBreath {
  0%, 100% {
    transform: scale(1);
    opacity: 0.95;
  }
  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}
</style>

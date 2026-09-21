<template>
  <teleport to="body">
    <transition name="ai-modal-fade">
      <div v-if="visible" class="ai-grading-modal-backdrop">
        <div class="ai-grading-card">
          <!-- 顶部发光微晕 -->
          <div class="glow-orb"></div>

          <!-- AI 核心脉冲罗盘环 -->
          <div class="compass-container">
            <div class="pulse-ring pulse-ring-1"></div>
            <div class="pulse-ring pulse-ring-2"></div>
            <div class="compass-core" :class="{ 'is-completed': isFinished }">
              <svg class="compass-svg" viewBox="0 0 48 48" fill="none">
                <defs>
                  <linearGradient id="aiCompassGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#1677FF" />
                    <stop offset="50%" stop-color="#3B82F6" />
                    <stop offset="100%" stop-color="#722ED1" />
                  </linearGradient>
                </defs>
                <circle cx="24" cy="24" r="21" stroke="url(#aiCompassGrad)" stroke-width="2" stroke-dasharray="4 3" opacity="0.5" />
                <circle cx="24" cy="24" r="16" stroke="currentColor" stroke-width="1.2" opacity="0.3" />
                <path
                  d="M24 8 L27.5 20.5 L40 24 L27.5 27.5 L24 40 L20.5 27.5 L8 24 L20.5 20.5 Z"
                  fill="url(#aiCompassGrad)"
                />
                <circle cx="24" cy="24" r="3" fill="#FFFFFF" />
              </svg>
            </div>
          </div>

          <!-- 核心标题与计时徽章 -->
          <div class="modal-header-meta">
            <h3 class="modal-title">
              {{ isFinished ? '智能评阅与学情分析完成' : 'AI 智能评阅与学情诊断中' }}
            </h3>
            <p class="modal-desc">
              {{ isFinished ? '所有试题与主观题解析已完成，正在生成诊断报告...' : '大模型正在对作答采分点进行深度语义解析并更新学情画像' }}
            </p>

            <!-- 秒级计时胶囊 -->
            <div class="timer-capsule" :class="{ 'is-finished': isFinished }">
              <span class="timer-dot" v-if="!isFinished"></span>
              <el-icon v-else class="timer-check-icon"><Check /></el-icon>
              <span class="timer-label">{{ isFinished ? '总评阅耗时：' : '评阅已耗时：' }}</span>
              <span class="timer-digits">{{ formattedTimer }}s</span>
            </div>
          </div>

          <!-- 智能流水线四阶段动效 -->
          <div class="pipeline-flow">
            <div
              v-for="(stage, idx) in stages"
              :key="stage.key"
              class="pipeline-item"
              :class="{
                'is-done': currentStageIndex > idx || isFinished,
                'is-running': currentStageIndex === idx && !isFinished,
                'is-pending': currentStageIndex < idx && !isFinished
              }"
            >
              <div class="stage-icon-wrap">
                <el-icon v-if="currentStageIndex > idx || isFinished" class="icon-done"><Check /></el-icon>
                <span v-else-if="currentStageIndex === idx && !isFinished" class="stage-spinner"></span>
                <span v-else class="stage-dot"></span>
              </div>
              <div class="stage-content">
                <span class="stage-title">{{ stage.title }}</span>
                <span class="stage-detail">{{ stage.detail }}</span>
              </div>
            </div>
          </div>

          <!-- 底部总进度长条 -->
          <div class="progress-section">
            <el-progress
              :percentage="progressPercentage"
              :stroke-width="6"
              :show-text="false"
              class="modal-progress-bar"
              :color="isFinished ? '#52C41A' : '#1677FF'"
            />
          </div>

          <!-- 中止响应与操作控制栏 -->
          <div class="modal-actions" v-if="!isFinished">
            <el-button
              class="abort-capsule-btn"
              plain
              size="default"
              :disabled="isAborting"
              :loading="isAborting"
              @click="handleAbort"
            >
              <el-icon class="mr-1"><VideoPause /></el-icon>
              中止评阅并返回答卷
            </el-button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue';
import { Check, VideoPause } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';

const props = defineProps<{
  visible: boolean;
  finished?: boolean;
}>();

const emit = defineEmits<{
  (e: 'abort'): void;
}>();

const isFinished = computed(() => !!props.finished);
const isAborting = ref(false);

// 计时系统 (精确到 0.1s)
const elapsedTenths = ref(0);
let timerHandle: ReturnType<typeof setInterval> | null = null;

const formattedTimer = computed(() => {
  const seconds = (elapsedTenths.value / 10).toFixed(1);
  return seconds;
});

// 四阶段评阅流水线
const stages = [
  { key: 'verify', title: '试卷有效性核验', detail: '作答数据完整性加密校验' },
  { key: 'objective', title: '客观试题即时判分', detail: '单选、多选、判断与填空题核对' },
  { key: 'subjective', title: '大模型主观深度评阅', detail: '基于采分点进行逻辑推理与智能分析' },
  { key: 'portrait', title: '学情画像与错题沉淀', detail: '更新知识点掌握图谱与强化推荐' }
];

const currentStageIndex = ref(0);
let stageHandle: ReturnType<typeof setTimeout> | null = null;

const progressPercentage = computed(() => {
  if (isFinished.value) return 100;
  return Math.min(95, Math.round(((currentStageIndex.value + 1) / (stages.length + 0.5)) * 100));
});

function startAnimation() {
  elapsedTenths.value = 0;
  currentStageIndex.value = 0;
  isAborting.value = false;

  if (timerHandle) clearInterval(timerHandle);
  timerHandle = setInterval(() => {
    elapsedTenths.value++;
  }, 100);

  simulateStages();
}

function simulateStages() {
  if (stageHandle) clearTimeout(stageHandle);
  // 阶段 0 -> 1: 0.6s
  stageHandle = setTimeout(() => {
    if (currentStageIndex.value < 1) currentStageIndex.value = 1;
    // 阶段 1 -> 2: 1.2s
    stageHandle = setTimeout(() => {
      if (currentStageIndex.value < 2) currentStageIndex.value = 2;
      // 阶段 2 -> 3: 2.2s (大模型推理阶段)
      stageHandle = setTimeout(() => {
        if (currentStageIndex.value < 3) currentStageIndex.value = 3;
      }, 2200);
    }, 1200);
  }, 600);
}

function stopAnimation() {
  if (timerHandle) {
    clearInterval(timerHandle);
    timerHandle = null;
  }
  if (stageHandle) {
    clearTimeout(stageHandle);
    stageHandle = null;
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      startAnimation();
    } else {
      stopAnimation();
    }
  },
  { immediate: true }
);

watch(
  () => props.finished,
  (val) => {
    if (val) {
      currentStageIndex.value = 3;
      stopAnimation();
    }
  }
);

onBeforeUnmount(() => {
  stopAnimation();
});

// 中止评阅
async function handleAbort() {
  try {
    await ElMessageBox.confirm('确定要中止当前智能评阅并返回答卷吗？', '中止评阅', {
      type: 'warning',
      confirmButtonText: '确定中止',
      cancelButtonText: '继续等待评阅'
    });
    isAborting.value = true;
    emit('abort');
  } catch {}
}
</script>

<style scoped lang="scss">
.ai-grading-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2999;
  background: rgba(10, 25, 50, 0.65);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.ai-grading-card {
  position: relative;
  width: 100%;
  max-width: 480px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  padding: 36px 32px 30px;
  box-shadow: 0 20px 50px rgba(10, 30, 70, 0.28), 0 0 0 1px rgba(255, 255, 255, 0.8) inset;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
  text-align: center;
}

// 顶部微光光晕
.glow-orb {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  width: 220px;
  height: 120px;
  background: radial-gradient(circle, rgba(22, 119, 255, 0.35) 0%, rgba(114, 46, 209, 0.15) 60%, transparent 80%);
  pointer-events: none;
  filter: blur(24px);
}

// 核心罗盘与光环
.compass-container {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;

  .pulse-ring {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: 1.5px solid rgba(22, 119, 255, 0.4);
    animation: radar-pulse 2.4s cubic-bezier(0.2, 0.8, 0.2, 1) infinite;

    &.pulse-ring-2 {
      animation-delay: 1.2s;
    }
  }

  .compass-core {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: #FFFFFF;
    box-shadow: 0 4px 16px rgba(22, 119, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1;

    .compass-svg {
      width: 36px;
      height: 36px;
      animation: spin-compass 4s linear infinite;
    }

    &.is-completed .compass-svg {
      animation: none;
    }
  }
}

.modal-header-meta {
  margin-bottom: 24px;

  .modal-title {
    margin: 0 0 6px;
    font-size: 19px;
    font-weight: 700;
    color: #0F172A;
  }

  .modal-desc {
    margin: 0 0 14px;
    font-size: 13px;
    color: #64748B;
    line-height: 1.5;
  }

  .timer-capsule {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 14px;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
    border-radius: 9999px;
    font-size: 12px;
    color: #334155;

    .timer-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #10B981;
      animation: pulse-dot 1s infinite alternate;
    }

    .timer-check-icon {
      color: #10B981;
      font-weight: 700;
    }

    .timer-digits {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-weight: 700;
      color: #1677FF;
      font-size: 13px;
    }

    &.is-finished {
      background: #F0FDF4;
      border-color: #BBF7D0;
      color: #15803D;

      .timer-digits {
        color: #15803D;
      }
    }
  }
}

// 四阶段流水线
.pipeline-flow {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #F8FAFC;
  border: 1px solid #EDF2F7;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 20px;
  text-align: left;

  .pipeline-item {
    display: flex;
    align-items: center;
    gap: 12px;
    transition: all 0.3s ease;

    .stage-icon-wrap {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      flex-shrink: 0;

      .icon-done {
        color: #10B981;
        font-weight: 700;
        font-size: 15px;
      }

      .stage-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #CBD5E1;
      }

      .stage-spinner {
        width: 14px;
        height: 14px;
        border: 2px solid rgba(22, 119, 255, 0.25);
        border-top-color: #1677FF;
        border-radius: 50%;
        animation: spin-compass 0.8s linear infinite;
      }
    }

    .stage-content {
      display: flex;
      flex-direction: column;
      gap: 1px;

      .stage-title {
        font-size: 13px;
        font-weight: 600;
        color: #64748B;
      }

      .stage-detail {
        font-size: 11px;
        color: #94A3B8;
      }
    }

    &.is-running {
      .stage-content {
        .stage-title {
          color: #1677FF;
        }
        .stage-detail {
          color: #3B82F6;
        }
      }
    }

    &.is-done {
      .stage-content {
        .stage-title {
          color: #0F172A;
        }
        .stage-detail {
          color: #64748B;
        }
      }
    }
  }
}

.progress-section {
  width: 100%;
  margin-bottom: 20px;

  .modal-progress-bar {
    margin: 0;
  }
}

.modal-actions {
  .abort-capsule-btn {
    border-radius: 9999px;
    font-weight: 500;
    color: #64748B;
    border-color: #CBD5E1;

    &:hover {
      color: #EF4444;
      border-color: #FCA5A5;
      background: #FEF2F2;
    }
  }
}

@keyframes spin-compass {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes radar-pulse {
  0% {
    transform: scale(0.7);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
  }
}

@keyframes pulse-dot {
  from { opacity: 0.4; }
  to { opacity: 1; }
}

// 弹窗淡入淡出动效
.ai-modal-fade-enter-active,
.ai-modal-fade-leave-active {
  transition: opacity 0.3s ease;
  .ai-grading-card {
    transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
}

.ai-modal-fade-enter-from,
.ai-modal-fade-leave-to {
  opacity: 0;
  .ai-grading-card {
    transform: scale(0.92);
  }
}
</style>

import { onUnmounted, ref, watch, type Ref } from 'vue';

export interface UseAiThinkingTimerOptions {
  /** 流水线阶段数，默认 3 */
  stepCount?: number;
  /** 阶段切换间隔（毫秒），与 MemoryExtract 原逻辑一致 */
  stepThresholdsMs?: [number, number];
}

/**
 * AI 认知推演计时与流水线阶段（与 MemoryExtractDialog 行为一致）
 */
export function useAiThinkingTimer(
  active: Ref<boolean>,
  options: UseAiThinkingTimerOptions = {}
) {
  const stepCount = options.stepCount ?? 3;
  const thresholds = options.stepThresholdsMs ?? [900, 1900];

  const currentStep = ref(1);
  const elapsedTimeText = ref('0.0s');

  let accumulatedMs = 0;
  let lastStartTime = 0;
  let timerId: number | null = null;

  function stopTimers() {
    if (timerId !== null) {
      clearInterval(timerId);
      timerId = null;
    }
  }

  function resumeTimers() {
    if (timerId !== null) clearInterval(timerId);
    timerId = window.setInterval(() => {
      const currentMs = accumulatedMs + (Date.now() - lastStartTime);
      elapsedTimeText.value = (currentMs / 1000).toFixed(1) + 's';
      if (stepCount <= 1) {
        currentStep.value = 1;
      } else if (stepCount === 2) {
        currentStep.value = currentMs < thresholds[0] ? 1 : 2;
      } else {
        if (currentMs < thresholds[0]) {
          currentStep.value = 1;
        } else if (currentMs < thresholds[1]) {
          currentStep.value = 2;
        } else {
          currentStep.value = Math.min(stepCount, 3);
        }
      }
    }, 100);
  }

  function resetTimers() {
    currentStep.value = 1;
    accumulatedMs = 0;
    elapsedTimeText.value = '0.0s';
    lastStartTime = Date.now();
  }

  function startTimers() {
    stopTimers();
    resetTimers();
    resumeTimers();
  }

  watch(
    active,
    (isActive) => {
      if (isActive) {
        startTimers();
      } else {
        stopTimers();
      }
    },
    { immediate: true }
  );

  onUnmounted(() => {
    stopTimers();
  });

  return {
    currentStep,
    elapsedTimeText,
    stopTimers
  };
}

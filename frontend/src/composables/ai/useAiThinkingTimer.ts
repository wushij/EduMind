import { onUnmounted, ref, watch, type Ref } from 'vue';

export interface UseAiThinkingTimerOptions {
  /** 流水线阶段数，默认 3 */
  stepCount?: number;
  /** 显式指定前两步的累积切换阈值（毫秒），不传则按步数自动展开 */
  stepThresholdsMs?: [number, number];
}

/**
 * 生成各阶段的累积切换阈值（毫秒）。
 *
 * 3 步以内保持既有节奏（0.9s / 1.9s），步数更多时按 1.8 倍递增展开：
 * 越靠后的阶段（大模型命题、效度校验）单项耗时越长，间隔理应更大。
 *
 * 旧实现把阶段直接钳制为 `Math.min(stepCount, 3)`，导致五步的组卷流程
 * 推进到第三步后永远不再变化，与真实进度严重脱节。
 */
function resolveStepThresholds(stepCount: number, explicit?: [number, number]): number[] {
  if (explicit && explicit.length) {
    return [...explicit];
  }
  if (stepCount <= 1) {
    return [];
  }

  const thresholds: number[] = [900];
  if (stepCount >= 3) {
    thresholds.push(1900);
  }

  let delta = 1000;
  while (thresholds.length < stepCount - 1) {
    delta = Math.round(delta * 1.8);
    thresholds.push(thresholds[thresholds.length - 1] + delta);
  }
  return thresholds;
}

/**
 * AI 认知推演计时与流水线阶段（与 MemoryExtractDialog 行为一致）
 */
export function useAiThinkingTimer(
  active: Ref<boolean>,
  options: UseAiThinkingTimerOptions = {}
) {
  const stepCount = Math.max(1, options.stepCount ?? 3);
  const thresholds = resolveStepThresholds(stepCount, options.stepThresholdsMs);

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

      let step = 1;
      for (const threshold of thresholds) {
        if (currentMs < threshold) {
          break;
        }
        step += 1;
      }
      currentStep.value = Math.min(stepCount, step);
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

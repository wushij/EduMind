import { usePreferenceStore } from '@/stores/user/preference';

export type ThinkingDisplayMode = 'EXPANDED' | 'COLLAPSED' | 'HIDDEN';

/** 读取个人偏好中的深度思考默认呈现策略 */
export function getThinkingDisplayMode(): ThinkingDisplayMode {
  try {
    return usePreferenceStore().preferences.thinkingDisplayMode;
  } catch {
    return 'COLLAPSED';
  }
}

/** EXPANDED → 展开；COLLAPSED / HIDDEN → 折叠（HIDDEN 时面板本身不渲染） */
export function getDefaultReasoningFolded(mode?: ThinkingDisplayMode): boolean {
  const resolved = mode ?? getThinkingDisplayMode();
  return resolved !== 'EXPANDED';
}

/** 「仅看最终解答」：不展示深度思考面板 */
export function isThinkingPanelHidden(mode?: ThinkingDisplayMode): boolean {
  const resolved = mode ?? getThinkingDisplayMode();
  return resolved === 'HIDDEN';
}

/** 消息未记录手动折叠状态时，回退到个人偏好默认 */
export function resolveReasoningFolded(stored?: boolean, mode?: ThinkingDisplayMode): boolean {
  if (stored !== undefined) return stored;
  return getDefaultReasoningFolded(mode);
}

export function shouldRenderThinkingPanel(
  hasReasoningContent: boolean,
  options?: { mode?: ThinkingDisplayMode; userRevealed?: boolean }
): boolean {
  if (!hasReasoningContent) return false;
  const mode = options?.mode ?? getThinkingDisplayMode();
  if (mode === 'HIDDEN' && !options?.userRevealed) return false;
  return true;
}

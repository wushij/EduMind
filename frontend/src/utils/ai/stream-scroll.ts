/** 聊天流式输出滚动贴底与视口状态（纯逻辑，无 Vue 依赖） */

export const SCROLL_PAUSE_DISTANCE = 160;
export const SCROLL_RESUME_DISTANCE = 30;

export function shouldPauseAutoFollow(distanceToBottom: number): boolean {
  return distanceToBottom > SCROLL_PAUSE_DISTANCE;
}

export function shouldResumeAutoFollow(distanceToBottom: number): boolean {
  return distanceToBottom < SCROLL_RESUME_DISTANCE;
}

export function getDistanceToBottom(el: HTMLElement): number {
  return el.scrollHeight - el.scrollTop - el.clientHeight;
}

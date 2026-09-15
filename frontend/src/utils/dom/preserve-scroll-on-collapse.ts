export interface ScrollCollapseSnapshot {
  scrollEl: HTMLElement;
  scrollTop: number;
  collapseTop: number;
  collapseHeight: number;
}

/** 优先命中聊天消息滚动容器，避免误选外层 app-content */
export function findChatScrollParent(el: HTMLElement | null): HTMLElement | null {
  let node = el?.parentElement ?? null;
  let fallback: HTMLElement | null = null;

  while (node) {
    const { overflowY } = window.getComputedStyle(node);
    const canScroll = overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay';
    if (canScroll) {
      if (
        node.dataset.chatScroll === 'true'
        || node.classList.contains('messages-flow-scroll')
        || node.classList.contains('ga-messages-scroll')
      ) {
        return node;
      }
      fallback = node;
    }
    node = node.parentElement;
  }

  return fallback;
}

export function captureScrollCollapseState(
  scrollEl: HTMLElement,
  collapsingEl: HTMLElement
): ScrollCollapseSnapshot {
  const scrollTop = scrollEl.scrollTop;
  const collapseTop = collapsingEl.getBoundingClientRect().top
    - scrollEl.getBoundingClientRect().top
    + scrollTop;

  return {
    scrollEl,
    scrollTop,
    collapseTop,
    collapseHeight: collapsingEl.offsetHeight
  };
}

/** 折叠上方内容后补偿 scrollTop，避免视口被“弹”到底部 */
export function applyScrollCollapseState(snapshot: ScrollCollapseSnapshot) {
  const { scrollEl, scrollTop, collapseTop, collapseHeight } = snapshot;
  if (collapseHeight <= 0) return;

  if (scrollTop > collapseTop) {
    scrollEl.scrollTop = Math.max(0, scrollTop - collapseHeight);
  }
}

export function preserveScrollOnCollapse(
  scrollEl: HTMLElement | null,
  collapsingEl: HTMLElement | null,
  afterLayout: () => void
) {
  if (!scrollEl || !collapsingEl) {
    afterLayout();
    return;
  }

  const snapshot = captureScrollCollapseState(scrollEl, collapsingEl);
  afterLayout();

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      applyScrollCollapseState(snapshot);
    });
  });
}

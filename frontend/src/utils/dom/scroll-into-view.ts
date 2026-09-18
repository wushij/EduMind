/** 查找实际产生纵向滚动的祖先（如布局里的 .app-content） */
export function getScrollParent(el: HTMLElement | null): HTMLElement {
  if (!el) return document.documentElement;
  let parent: HTMLElement | null = el.parentElement;
  while (parent) {
    const { overflowY } = getComputedStyle(parent);
    const scrollable =
      (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay') &&
      parent.scrollHeight > parent.clientHeight + 1;
    if (scrollable) return parent;
    parent = parent.parentElement;
  }
  return document.documentElement;
}

/** 在正确的滚动容器内定位元素，并留出顶栏偏移 */
export function scrollElementIntoView(el: HTMLElement, offsetTop = 96) {
  const scroller = getScrollParent(el);
  const elRect = el.getBoundingClientRect();
  const scrollerRect = scroller.getBoundingClientRect();
  const top = scroller.scrollTop + (elRect.top - scrollerRect.top) - offsetTop;
  scroller.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
}

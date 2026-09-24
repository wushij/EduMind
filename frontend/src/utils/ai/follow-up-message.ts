/**
 * 取「当轮」消息，供追问这类异步结果回写使用。
 *
 * <p>为什么必须有这个函数：消息数组是 Vue 的 `ref`，往里面 push 原始对象后再读出来的是
 * <b>响应式代理</b>，因此 `messages.value[length - 1] === 原始对象` 恒为 false。
 * 用引用比较当守卫，异步结果就永远写不回去，界面也不会刷新
 * ——线上表现就是「追问只有刷新页面 / 重进会话才出现」。</p>
 *
 * <p>这里统一按 id 判定当轮消息，并把数组里的<b>代理对象</b>返回给调用方回写；
 * 若直接改原始对象，Vue 收不到触发，视图同样不会更新。</p>
 */
export function pickTurnMessage<T extends { id?: string | number }>(
  messages: readonly T[],
  targetId: string | number | undefined
): T | undefined {
  if (targetId === undefined || targetId === null || messages.length === 0) {
    return undefined;
  }
  const last = messages[messages.length - 1];
  return last && last.id === targetId ? last : undefined;
}

import { computed, onMounted, onUnmounted, ref } from 'vue';
import { getConfigGroup } from '@/api/system/config';

export const ASSISTANT_ENABLED_STORAGE_KEY = 'edumind_sys_ai_assistant_enabled';
export const AI_CONFIG_CHANGED_EVENT = 'edumind:ai-config-changed';

/**
 * 「AI 助手」总开关（系统配置 → AI 配置 → 助手开关）。
 *
 * 背景：此前 assistantEnabled 只在配置页被读写，没有任何地方消费它，
 * 导致关掉开关后 PC 端与移动端的悬浮 AI 助手小窗依然显示，开关形同虚设。
 * 这里作为统一消费入口：悬浮入口组件据此决定是否渲染。
 *
 * 读取优先级：localStorage 缓存（保存后即时生效，无需刷新） → 后端配置。
 */
function readCachedEnabled(): boolean | null {
  if (typeof window === 'undefined' || !window.localStorage) return null;
  const raw = localStorage.getItem(ASSISTANT_ENABLED_STORAGE_KEY);
  if (raw === null) return null;
  return raw === 'true';
}

/** 全局单例状态：多个悬浮入口共享，避免各自重复拉取配置 */
const enabled = ref(readCachedEnabled() ?? true);
let listenersBound = false;

function syncFromEvent(event: Event) {
  const detail = (event as CustomEvent<{ assistantEnabled?: boolean }>).detail;
  if (typeof detail?.assistantEnabled === 'boolean') {
    enabled.value = detail.assistantEnabled;
    try {
      localStorage.setItem(ASSISTANT_ENABLED_STORAGE_KEY, String(detail.assistantEnabled));
    } catch {
      // ignore
    }
  }
}

function bindGlobalListeners() {
  if (listenersBound || typeof window === 'undefined') return;
  window.addEventListener(AI_CONFIG_CHANGED_EVENT, syncFromEvent);
  // 跨标签页同步：管理员在另一个标签页关闭助手，本页也应立即隐藏
  window.addEventListener('storage', (e) => {
    if (e.key === ASSISTANT_ENABLED_STORAGE_KEY && e.newValue !== null) {
      enabled.value = e.newValue === 'true';
    }
  });
  listenersBound = true;
}

/** 供配置页保存后直接调用，立即生效 */
export function applyAssistantEnabled(value: boolean) {
  enabled.value = value;
  try {
    localStorage.setItem(ASSISTANT_ENABLED_STORAGE_KEY, String(value));
  } catch {
    // ignore
  }
}

export function useAssistantVisibility() {
  bindGlobalListeners();

  async function loadFromServer() {
    try {
      const res = await getConfigGroup('ai');
      const raw = (res?.data as { configValue?: string } | undefined)?.configValue;
      if (!raw) return;
      const parsed = JSON.parse(raw) as { assistantEnabled?: boolean };
      if (typeof parsed.assistantEnabled === 'boolean') {
        applyAssistantEnabled(parsed.assistantEnabled);
      }
    } catch {
      // 拉取失败时保留上次已知状态，避免误隐藏
    }
  }

  onMounted(() => {
    // 已有缓存则以缓存为准（保存后即时生效），否则回源一次
    if (readCachedEnabled() === null) {
      void loadFromServer();
    }
  });

  onUnmounted(() => {
    // 单例状态与全局监听常驻，无需解绑
  });

  return {
    assistantEnabled: computed(() => enabled.value),
    showAssistant: computed(() => enabled.value),
    refresh: loadFromServer
  };
}

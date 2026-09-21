import { defineStore } from 'pinia';
import { ref, reactive, watch } from 'vue';
import { getUserPreferences, saveUserPreferences } from '@/api/profile/preferences';
import type { UserPreferenceVO } from '@/types/profile/preferences';
import { tokenUtil } from '@/core/auth/token';
import { ElMessage } from 'element-plus';

export interface UserPreferencesState {
  theme: 'LIGHT' | 'DARK' | 'AUTO';
  language: 'zh-CN' | 'en-US';
  defaultModel: string;
  enableRag: boolean;
  enableNotification: boolean;
  katexEnabled: boolean;
  codeHighlightEnabled: boolean;
  mermaidEnabled: boolean;
  thinkingDisplayMode: 'EXPANDED' | 'COLLAPSED' | 'HIDDEN';
  aiTone: 'HEURISTIC' | 'RIGOROUS' | 'EXAM_ORIENTED';
  notifySubmission: boolean;
  notifyGrading: boolean;
  notifyVector: boolean;
  compactMode: boolean;
  inferenceTemperature: number;
  ragTopK: number;
}

export const PREFERENCE_STORAGE_KEY = 'edumind_user_preferences';

const DEFAULT_PREFERENCES: UserPreferencesState = {
  theme: 'LIGHT',
  language: 'zh-CN',
  defaultModel: 'deepseek-v3',
  enableRag: true,
  enableNotification: true,
  katexEnabled: true,
  codeHighlightEnabled: true,
  mermaidEnabled: true,
  thinkingDisplayMode: 'COLLAPSED',
  aiTone: 'HEURISTIC',
  notifySubmission: true,
  notifyGrading: true,
  notifyVector: true,
  compactMode: false,
  inferenceTemperature: 0.5,
  ragTopK: 5
};

export const usePreferenceStore = defineStore('preference', () => {
  const preferences = reactive<UserPreferencesState>({ ...DEFAULT_PREFERENCES });
  const loading = ref(false);
  const saving = ref(false);
  let mqlListener: ((e: MediaQueryListEvent) => void) | null = null;

  // 1. 响应式应用暗黑/明亮/跟随系统主题
  const applyTheme = (theme: 'LIGHT' | 'DARK' | 'AUTO') => {
    if (typeof document === 'undefined') return;
    const root = document.documentElement;

    // 清理先前的系统主题媒体查询监听
    if (mqlListener) {
      const mql = window.matchMedia('(prefers-color-scheme: dark)');
      mql.removeEventListener('change', mqlListener);
      mqlListener = null;
    }

    if (theme === 'DARK') {
      root.classList.add('dark');
    } else if (theme === 'LIGHT') {
      root.classList.remove('dark');
    } else if (theme === 'AUTO') {
      const mql = window.matchMedia('(prefers-color-scheme: dark)');
      if (mql.matches) {
        root.classList.add('dark');
      } else {
        root.classList.remove('dark');
      }

      mqlListener = (e: MediaQueryListEvent) => {
        if (preferences.theme === 'AUTO') {
          if (e.matches) {
            root.classList.add('dark');
          } else {
            root.classList.remove('dark');
          }
        }
      };
      mql.addEventListener('change', mqlListener);
    }
  };

  // 2. 从本地缓存快速冷启动，保证零闪烁
  const loadFromStorage = () => {
    try {
      const raw = localStorage.getItem(PREFERENCE_STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        Object.assign(preferences, parsed);
      }
    } catch {
      // 容错兜底
    }
    applyTheme(preferences.theme);
  };

  // 3. 从后端接口拉取当前登录用户的云端偏好设置
  const loadPreferences = async () => {
    loadFromStorage();
    if (!tokenUtil.get()) {
      return;
    }
    try {
      loading.value = true;
      const res = await getUserPreferences({ silent: true });
      if (res?.data) {
        const data = res.data;
        if (data.theme) preferences.theme = data.theme as any;
        if (data.language) preferences.language = data.language as any;
        if (data.defaultModel) preferences.defaultModel = data.defaultModel;
        if (data.enableRag !== undefined) preferences.enableRag = data.enableRag;
        if (data.enableNotification !== undefined) preferences.enableNotification = data.enableNotification;

        // 解析扩展 JSON
        if (data.preferencesJson) {
          try {
            const ext = JSON.parse(data.preferencesJson);
            if (ext.katexEnabled !== undefined) preferences.katexEnabled = ext.katexEnabled;
            if (ext.codeHighlightEnabled !== undefined) preferences.codeHighlightEnabled = ext.codeHighlightEnabled;
            if (ext.mermaidEnabled !== undefined) preferences.mermaidEnabled = ext.mermaidEnabled;
            if (ext.thinkingDisplayMode) preferences.thinkingDisplayMode = ext.thinkingDisplayMode;
            if (ext.aiTone) preferences.aiTone = ext.aiTone;
            if (ext.notifySubmission !== undefined) preferences.notifySubmission = ext.notifySubmission;
            if (ext.notifyGrading !== undefined) preferences.notifyGrading = ext.notifyGrading;
            if (ext.notifyVector !== undefined) preferences.notifyVector = ext.notifyVector;
            if (ext.compactMode !== undefined) preferences.compactMode = ext.compactMode;
            if (typeof ext.inferenceTemperature === 'number') {
              preferences.inferenceTemperature = ext.inferenceTemperature;
            }
            if (typeof ext.ragTopK === 'number') {
              preferences.ragTopK = ext.ragTopK;
            }
          } catch {
            // ignore malformed json
          }
        }

        // 保存更新后的完整本地副本并应用
        localStorage.setItem(PREFERENCE_STORAGE_KEY, JSON.stringify(preferences));
        applyTheme(preferences.theme);
      }
    } catch (e) {
      console.warn('同步云端偏好设置失败，已使用本地配置缓存:', e);
    } finally {
      loading.value = false;
    }
  };

  // 4. 保存偏好设置至云端数据库与本地
  const savePreferences = async (custom?: Partial<UserPreferencesState>) => {
    if (custom) {
      Object.assign(preferences, custom);
    }
    try {
      saving.value = true;
      applyTheme(preferences.theme);

      // 提取扩展偏好
      const extJson = JSON.stringify({
        katexEnabled: preferences.katexEnabled,
        codeHighlightEnabled: preferences.codeHighlightEnabled,
        mermaidEnabled: preferences.mermaidEnabled,
        thinkingDisplayMode: preferences.thinkingDisplayMode,
        aiTone: preferences.aiTone,
        notifySubmission: preferences.notifySubmission,
        notifyGrading: preferences.notifyGrading,
        notifyVector: preferences.notifyVector,
        compactMode: preferences.compactMode,
        inferenceTemperature: preferences.inferenceTemperature,
        ragTopK: preferences.ragTopK
      });

      const payload: Partial<UserPreferenceVO> = {
        theme: preferences.theme,
        language: preferences.language,
        defaultModel: preferences.defaultModel,
        enableRag: preferences.enableRag,
        enableNotification: preferences.notifySubmission || preferences.notifyGrading || preferences.notifyVector,
        preferencesJson: extJson
      };

      await saveUserPreferences(payload);
      localStorage.setItem(PREFERENCE_STORAGE_KEY, JSON.stringify(preferences));
      ElMessage.success('个人偏好设置已成功保存并同步至云端');
      return true;
    } catch (err: any) {
      ElMessage.error(err?.message || '保存云端偏好设置失败，已临时保存在本地');
      localStorage.setItem(PREFERENCE_STORAGE_KEY, JSON.stringify(preferences));
      return false;
    } finally {
      saving.value = false;
    }
  };

  // 5. 恢复出厂默认值（支持优先恢复为系统/网关官方推荐模型）
  const resetToDefaults = async (overrideDefaultModel?: string) => {
    Object.assign(preferences, DEFAULT_PREFERENCES);
    if (overrideDefaultModel) {
      preferences.defaultModel = overrideDefaultModel;
    }
    await savePreferences();
  };

  // 监听 theme 变更即时响应
  watch(
    () => preferences.theme,
    (newTheme) => {
      applyTheme(newTheme);
    }
  );

  return {
    preferences,
    loading,
    saving,
    loadPreferences,
    savePreferences,
    applyTheme,
    resetToDefaults
  };
});

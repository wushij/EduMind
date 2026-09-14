import { get, put } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';

export interface UserPreferenceVO {
  theme: string;
  language: string;
  defaultModel: string;
  enableRag: boolean;
  enableNotification: boolean;
  preferencesJson?: string;
}

export const getUserPreferences = (config?: HttpRequestConfig) =>
  get<UserPreferenceVO>('/users/me/preferences', undefined, config);

export const saveUserPreferences = (data: Partial<UserPreferenceVO>) =>
  put<UserPreferenceVO>('/users/me/preferences', data);

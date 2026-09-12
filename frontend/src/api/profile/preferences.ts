import { get, put } from '@/core/http/request';

export interface UserPreferenceVO {
  theme: string;
  language: string;
  defaultModel: string;
  enableRag: boolean;
  enableNotification: boolean;
  preferencesJson?: string;
}

export const getUserPreferences = () => get<UserPreferenceVO>('/users/me/preferences');

export const saveUserPreferences = (data: Partial<UserPreferenceVO>) =>
  put<UserPreferenceVO>('/users/me/preferences', data);

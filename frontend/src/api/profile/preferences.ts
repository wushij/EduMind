import { get, put } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import type { UserPreferenceVO } from '@/types/profile/preferences';

export const getUserPreferences = (config?: HttpRequestConfig) =>
  get<UserPreferenceVO>('/users/me/preferences', undefined, config);

export const saveUserPreferences = (data: Partial<UserPreferenceVO>) =>
  put<UserPreferenceVO>('/users/me/preferences', data);

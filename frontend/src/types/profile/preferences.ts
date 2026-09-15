export interface UserPreferenceVO {
  theme: string;
  language: string;
  defaultModel: string;
  enableRag: boolean;
  enableNotification: boolean;
  preferencesJson?: string;
}

export type AIToolExecutionMode = 'ROUTE' | 'V05_NOTICE';

export interface AITool {
  id: string;
  name: string;
  category: 'TEACHER' | 'STUDENT' | 'GENERAL' | string;
  categoryLabel: string;
  description: string;
  detailedIntro: string;
  iconBg: string;
  iconName?: string;
  iconTheme?: string;
  iconEmoji?: string;
  modelId?: string;
  route: string;
  executionMode: AIToolExecutionMode;
  tags: string[];
  isRecommended: boolean;
  isHot: boolean;
  isFavorite: boolean;
  usageCount: number;
}

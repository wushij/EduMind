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
  route: string;
  tags: string[];
  isRecommended: boolean;
  isFavorite: boolean;
  usageCount: number;
  rating: number;
}

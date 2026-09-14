import { get } from '@/core/http/request';
import type { PersonalAiUsageQuery, PersonalAiUsageVO } from '@/types/profile/ai-usage';

export const getMyAiUsage = (params?: PersonalAiUsageQuery) =>
  get<PersonalAiUsageVO>('/users/me/ai-usage', params);

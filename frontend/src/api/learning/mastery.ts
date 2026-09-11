import { get } from '@/core/http/request';
import { KnowledgeMastery } from '@/types/learning/mastery';

export const getMasteryRates = () => get<KnowledgeMastery[]>('/learning/mastery');

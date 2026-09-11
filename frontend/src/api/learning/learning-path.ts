import { get } from '@/core/http/request';
import { LearningStep } from '@/types/learning/learning-path';

export const getLearningPath = () => get<LearningStep[]>('/learning/path');

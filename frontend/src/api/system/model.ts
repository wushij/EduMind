import { get } from '@/core/http/request';
import { AIModel } from '@/types/ai/model';

export const getSystemModels = () => get<AIModel[]>('/system/models');

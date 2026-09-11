import { get } from '@/core/http/request';

export const getTodayTasks = () => get<any[]>('/learning/tasks/today');

import { get } from '@/core/http/request';
import { WrongQuestion } from '@/types/learning/wrong-question';

export const getWrongQuestions = () => get<WrongQuestion[]>('/learning/wrong-questions');

import { post } from '@/core/http/request';
import type {
  SmartPaperComposeRequest,
  SmartPaperComposeVO,
  SmartPaperSwapRequest
} from '@/types/ai/paper-compose';
import type { QuestionVO } from '@/types/question/question';

/**
 * 发起智能组卷运算（支持题库优选与真实大模型原创命题补全）
 */
export const composeSmartPaper = (data: SmartPaperComposeRequest) =>
  post<SmartPaperComposeVO>('/ai/paper/compose', data, { timeout: 180000 });

/**
 * 兼容性保留 v2 别名，统一调用后端真实组卷接口
 */
export const composeSmartPaperV2 = (data: SmartPaperComposeRequest) =>
  post<SmartPaperComposeVO>('/ai/paper/compose', data, { timeout: 180000 });

/**
 * 中止当前正在运行的智能组卷计算
 */
export const cancelSmartPaperCompose = () =>
  post<void>('/ai/paper/compose/cancel');

/**
 * 局部换一题（优先同考点题库抽取，若无则大模型重新原创生成）
 */
export const swapPaperQuestion = (data: SmartPaperSwapRequest) =>
  post<QuestionVO>('/ai/paper/swap-question', data, { timeout: 60000 });

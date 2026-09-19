import { post } from '@/core/http/request';
import type { SmartPaperComposeRequest, SmartPaperComposeVO } from '@/types/ai/paper-compose';

export const composeSmartPaper = (data: SmartPaperComposeRequest) =>
  post<SmartPaperComposeVO>('/ai/paper/compose', data, { timeout: 180000 });

export const composeSmartPaperV2 = (data: SmartPaperComposeRequest) =>
  post<SmartPaperComposeVO>('/ai/paper/compose/v2', data, { timeout: 180000 });

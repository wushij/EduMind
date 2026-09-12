import { post } from '@/core/http/request';
import type { SmartPaperComposeRequest, SmartPaperComposeVO } from '@/types/ai/paper-compose';

export const composeSmartPaper = (data: SmartPaperComposeRequest) =>
  post<SmartPaperComposeVO>('/ai/paper/compose', data);

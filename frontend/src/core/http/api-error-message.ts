import { isAxiosError } from 'axios';

/** 优先展示后端 ApiResult.message，避免 axios 默认的 "Request failed with status code 500" */
export function resolveApiErrorMessage(err: unknown, fallback = '请求失败，请稍后重试'): string {
  if (isAxiosError(err)) {
    const data = err.response?.data;
    if (data && typeof data === 'object' && 'message' in data) {
      const msg = String((data as { message?: string }).message || '').trim();
      if (msg) return msg;
    }
  }
  if (err instanceof Error) {
    const msg = err.message?.trim() || '';
    if (msg && !/^Request failed with status code \d+$/i.test(msg)) {
      return msg;
    }
  }
  return fallback;
}

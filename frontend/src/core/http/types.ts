import type { AxiosRequestConfig } from 'axios';

export interface RequestOptions {
  /** 为 true 时不弹出全局错误提示（页面自行降级处理） */
  silent?: boolean;
}

export type HttpRequestConfig = AxiosRequestConfig & RequestOptions;

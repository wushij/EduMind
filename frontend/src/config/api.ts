/**
 * API 与网络通信相关配置
 * 优先读取 Vite 环境变量，提供本地开发 fallback 回退
 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '/api';
export const API_TIMEOUT: number = Number(import.meta.env.VITE_API_TIMEOUT) || 30000;

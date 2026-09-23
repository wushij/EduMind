/**
 * API 与网络通信相关配置
 * 优先读取 Vite 环境变量，提供本地开发 fallback 回退
 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '/api';
export const API_TIMEOUT: number = Number(import.meta.env.VITE_API_TIMEOUT) || 30000;
/**
 * AI / 长耗时请求超时（3 分钟，与后端 ai.llm.timeout-ms 对齐）。
 *
 * 交卷后的 AI 评阅、变式题生成等场景会在服务端逐题串行调用大模型，
 * 一份多主观题的卷子远超 30 秒，必须显式放宽，否则必然超时失败。
 */
export const AI_REQUEST_TIMEOUT = 180000;

/**
 * 文档解析 / 切片 / 向量索引等「任务型」接口超时（3 分钟）。
 *
 * 这些接口在服务端要拉对象存储、抽正文、写切片并触发向量化，
 * 默认 30 秒会被判超时（表现为"点了解析/切片但面板空白"）。
 */
export const LONG_TASK_TIMEOUT: number = Number(import.meta.env.VITE_LONG_TASK_TIMEOUT) || 180000;
/** @deprecated 敏感路径签名已默认开启，保留仅作兼容 */
export const SM_ENABLED: boolean = import.meta.env.VITE_SM_ENABLED === 'true';
export const SM_HMAC_SECRET: string = import.meta.env.VITE_SM_HMAC_SECRET || 'EduMind_Platform_SecretKey_2026';

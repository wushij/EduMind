/**
 * Mock 数据开关：仅开发环境且 VITE_USE_MOCK=true 时启用 Mock fallback
 */
export const USE_MOCK: boolean =
  import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true';

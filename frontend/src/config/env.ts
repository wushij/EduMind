/**
 * 运行环境判断与应用运行时环境配置
 */
export const ENV: string = import.meta.env.MODE || 'development';
export const IS_DEV: boolean = import.meta.env.DEV;
export const IS_PROD: boolean = import.meta.env.PROD;

// 保持向下兼容的小驼峰别名
export const isDev: boolean = IS_DEV;
export const isProd: boolean = IS_PROD;

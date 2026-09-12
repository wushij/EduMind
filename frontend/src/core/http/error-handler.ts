import type { App } from 'vue';
import { IS_DEV } from '@/config/env';

type ErrorExtra = Record<string, unknown>;

/**
 * 开发环境统一输出错误，便于 DevTools 排查。
 * 生产环境仍可通过 Vue / Promise 全局钩子记录致命错误。
 */
export function logAppError(scope: string, error: unknown, extra?: ErrorExtra) {
  if (!IS_DEV && scope !== 'Vue' && scope !== 'UnhandledPromise') {
    return;
  }

  const message = error instanceof Error ? error.message : String(error);
  const detail = {
    scope,
    message,
    ...(extra || {})
  };

  console.groupCollapsed(`[EduMind:${scope}] ${message}`);
  console.error('detail:', detail);
  if (error instanceof Error && error.stack) {
    console.error(error.stack);
  } else if (error) {
    console.error(error);
  }
  console.groupEnd();
}

export function setupAppErrorHandlers(app: App) {
  app.config.errorHandler = (err, instance, info) => {
    logAppError('Vue', err, {
      info,
      component: (instance as { $options?: { name?: string } } | null)?.$options?.name
    });
  };

  window.addEventListener('unhandledrejection', (event) => {
    logAppError('UnhandledPromise', event.reason);
  });

  window.addEventListener('error', (event) => {
    logAppError('WindowError', event.error || event.message, {
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno
    });
  });
}

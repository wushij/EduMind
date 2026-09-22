import axios, { AxiosInstance, AxiosResponse } from 'axios';
import type { HttpRequestConfig } from './types';
import { ElMessage } from 'element-plus';
import { TOKEN_KEY } from '@/constants/auth';
import { API_BASE_URL, API_TIMEOUT } from '@/config';
import { buildRequestSecurityHeaders } from './request-signature';
import { storage } from '../storage/local';
import { logAppError } from './error-handler';

/**
 * 注意：这里刻意不设置全局 Content-Type。
 *
 * 原因：axios 1.x 的 default transformRequest 中，如果「当前请求头已经是 JSON 类型」
 * （contentType 含 application/json）而 body 又是 FormData/Blob，
 * 它会静默把 FormData 序列化成 JSON 字符串并按 JSON 发出（axios 默认头 + 未覆写时命中）。
 * 结果就是 multipart 接口必然 415：
 * 「不支持的 Content-Type: application/json;charset=courses/{id}/resources/upload 等上传接口」。
 *
 * 交给 axios 按 body 自行推断才是正确的：
 * 普通对象 → application/json；FormData → multipart/form-data(含 boundary，由浏览器补全)。
 */
export const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT
});

// 请求拦截器
axiosInstance.interceptors.request.use(
  (config) => {
    // 注入 Sa-Token 鉴权令牌
    const token = storage.get(TOKEN_KEY);
    if (token) {
      config.headers['satoken'] = token;
    }

    // 注入多租户隔离上下文
    const tenantId = storage.get('edumind_tenant_id');
    if (tenantId) {
      config.headers['X-Tenant-Id'] = String(tenantId);
    }

    const urlPath = config.url || '/';
    const body = typeof config.data === 'string'
      ? config.data
      : config.data ? JSON.stringify(config.data) : '';
    const securityHeaders = buildRequestSecurityHeaders(
      (config.method || 'get').toUpperCase(),
      urlPath,
      body
    );
    config.headers['X-Timestamp'] = securityHeaders['X-Timestamp'];
    config.headers['X-Nonce'] = securityHeaders['X-Nonce'];
    if (securityHeaders['X-Signature']) {
      config.headers['X-Signature'] = securityHeaders['X-Signature'];
    }

    return config;
  },
  (error) => Promise.reject(error)
);

let lastErrorMsg = '';
let lastErrorTime = 0;
function showErrorMessage(msg: string) {
  const now = Date.now();
  if (msg === lastErrorMsg && now - lastErrorTime < 2500) {
    return;
  }
  lastErrorMsg = msg;
  lastErrorTime = now;
  ElMessage.error(msg);
}

function isAuthEntryPath() {
  const path = window.location.pathname;
  return (
    path.startsWith('/auth/login')
    || path.startsWith('/auth/register')
    || path.startsWith('/auth/forgot')
  );
}

function handleUnauthorized(message = '登录状态已失效，请重新登录') {
  storage.remove(TOKEN_KEY);
  storage.remove('edumind_user_info');
  // 已在登录/注册页时，通常是本地残留过期 Token 被 bootstrap 校验失败，静默清理即可
  if (isAuthEntryPath()) {
    return;
  }
  showErrorMessage(message);
  setTimeout(() => {
    window.location.href = '/auth/login';
  }, 500);
}

/**
 * AI / 长耗时路径判定：仅用于超时提示文案，
 * 避免把普通接口超时统一误报成「大模型推演耗时较长，请减少生成题量」。
 */
function isAiHeavyRequest(url?: string): boolean {
  const target = url || '';
  return ['/ai/', '/grade', '/diagnose', '/variants', '/submit', '/compose'].some((key) =>
    target.includes(key)
  );
}

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    const silent = (response.config as HttpRequestConfig).silent;
    if (res.code && res.code !== 200) {
      const msg = res.message || '请求处理失败';
      if (!silent) {
        logAppError('API', new Error(msg), {
          url: response.config?.url,
          method: response.config?.method,
          code: res.code,
          data: res
        });
      }

      if (res.code === 401 || res.code === 1001) {
        handleUnauthorized(msg);
        return Promise.reject(new Error(msg));
      }

      if (res.code === 429) {
        showErrorMessage('操作过于频繁，请稍后重试');
        return Promise.reject(new Error(msg));
      }

      if (!silent) {
        showErrorMessage(msg);
      }
      return Promise.reject(new Error(msg));
    }
    return res;
  },
  (error) => {
    if (axios.isCancel(error) || error?.name === 'CanceledError' || error?.code === 'ERR_CANCELED') {
      return Promise.reject(error);
    }
    const status = error.response?.status;
    const apiBody = error.response?.data;
    const silent = (error.config as HttpRequestConfig | undefined)?.silent;
    let apiMessage =
      (typeof apiBody === 'object' && apiBody !== null && 'message' in apiBody
        ? String((apiBody as { message?: string }).message || '')
        : '') || error.message || '网络通信异常';

    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      // 普通接口超时不要甩锅给大模型：只有 AI / 长耗时路径才用推演口径提示
      apiMessage = isAiHeavyRequest(error.config?.url)
        ? 'AI 深度推演耗时较长，本次请求已超时，请稍后重试'
        : '请求超时：服务端响应较慢，请稍后重试';
    }

    if (!silent) {
      logAppError('HTTP', error, {
        url: error.config?.url,
        method: error.config?.method,
        status,
        code: typeof apiBody === 'object' && apiBody !== null ? (apiBody as { code?: number }).code : undefined,
        data: apiBody
      });
    }

    if (status === 401) {
      handleUnauthorized(apiMessage);
      return Promise.reject(error);
    }

    if (status === 403) {
      if (!silent) {
        showErrorMessage(apiMessage || '没有相关权限');
      }
      return Promise.reject(error);
    }

    if (status === 404) {
      if (!silent) {
        showErrorMessage(apiMessage || '请求资源未找到');
      }
      return Promise.reject(error);
    }

    if (status === 429) {
      showErrorMessage('操作过于频繁，请稍后重试');
      return Promise.reject(error);
    }

    if (!silent) {
      showErrorMessage(apiMessage);
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;

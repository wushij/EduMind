import axios, { AxiosInstance, AxiosResponse } from 'axios';
import type { HttpRequestConfig } from './types';
import { ElMessage } from 'element-plus';
import { TOKEN_KEY } from '@/constants/auth';
import { API_BASE_URL, API_TIMEOUT } from '@/config';
import { buildRequestSecurityHeaders } from './request-signature';
import { storage } from '../storage/local';
import { logAppError } from './error-handler';

export const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
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

function handleUnauthorized(message = '登录状态已失效，请重新登录') {
  storage.remove(TOKEN_KEY);
  storage.remove('edumind_user_info');
  showErrorMessage(message);
  if (!window.location.pathname.startsWith('/auth/login')) {
    setTimeout(() => {
      window.location.href = '/auth/login';
    }, 500);
  }
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
    const status = error.response?.status;
    const apiBody = error.response?.data;
    const silent = (error.config as HttpRequestConfig | undefined)?.silent;
    const apiMessage =
      (typeof apiBody === 'object' && apiBody !== null && 'message' in apiBody
        ? String((apiBody as { message?: string }).message || '')
        : '') || error.message || '网络通信异常';

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

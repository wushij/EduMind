import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import { TOKEN_KEY } from '@/constants/auth';
import { API_BASE_URL, API_TIMEOUT } from '@/config';
import { getTimestamp, generateNonce } from '@/utils/crypto';
import { storage } from '../storage/local';

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

    // 自动注入防重放安全请求头：13 位毫秒时间戳 + 32 位唯一 Nonce
    config.headers['X-Timestamp'] = String(getTimestamp());
    config.headers['X-Nonce'] = generateNonce();

    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '请求处理失败');
      return Promise.reject(new Error(res.message || 'Error'));
    }
    return res;
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      storage.remove(TOKEN_KEY);
      storage.remove('edumind_user_info');
      if (!window.location.pathname.startsWith('/auth/login')) {
        window.location.href = '/auth/login';
      }
      return Promise.reject(error);
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络通信异常');
    return Promise.reject(error);
  }
);

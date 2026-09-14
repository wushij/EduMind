import { SM_HMAC_SECRET } from '@/config';
import { generateNonce, generateRequestSignature, getTimestamp } from '@/utils/crypto';

/** 与后端 SecurityProperties.sensitivePaths 保持一致 */
const SENSITIVE_PREFIXES = ['/api/analytics', '/api/ai/agent'];

function stripQuery(path: string): string {
  const idx = path.indexOf('?');
  return idx >= 0 ? path.slice(0, idx) : path;
}

export function normalizeApiPath(path: string): string {
  const rawPath = stripQuery(path);
  if (rawPath.startsWith('http')) {
    return new URL(rawPath).pathname;
  }
  if (rawPath.startsWith('/api/')) {
    return rawPath;
  }
  return `/api${rawPath.startsWith('/') ? rawPath : `/${rawPath}`}`;
}

export function isSensitiveApiPath(path: string): boolean {
  const normalized = normalizeApiPath(path);
  return SENSITIVE_PREFIXES.some((prefix) => normalized.startsWith(prefix));
}

export function buildRequestSignature(
  method: string,
  path: string,
  timestamp: number,
  nonce: string,
  body = ''
): string {
  return generateRequestSignature(
    method.toUpperCase(),
    normalizeApiPath(path),
    timestamp,
    nonce,
    body,
    SM_HMAC_SECRET
  );
}

export interface RequestSecurityHeaders {
  'X-Timestamp': string;
  'X-Nonce': string;
  'X-Signature'?: string;
}

/**
 * 为请求生成防重放与签名字段。
 * 敏感路径始终签名，避免后端动态开启国密验签后前端未同步导致 403。
 */
export function buildRequestSecurityHeaders(
  method: string,
  path: string,
  body = ''
): RequestSecurityHeaders {
  const timestamp = getTimestamp();
  const nonce = generateNonce();
  const apiPath = normalizeApiPath(path);
  const headers: RequestSecurityHeaders = {
    'X-Timestamp': String(timestamp),
    'X-Nonce': nonce
  };

  if (isSensitiveApiPath(apiPath)) {
    headers['X-Signature'] = buildRequestSignature(method, apiPath, timestamp, nonce, body);
  }

  return headers;
}

/** EventSource 等无法自定义 Header 的场景，将安全字段写入 Query */
export function appendSecurityQuery(url: string, method: string, body = ''): string {
  const headers = buildRequestSecurityHeaders(method, url, body);
  if (!headers['X-Signature']) {
    return url;
  }

  const resolved = url.startsWith('http') ? new URL(url) : new URL(url, window.location.origin);
  resolved.searchParams.set('X-Timestamp', headers['X-Timestamp']);
  resolved.searchParams.set('X-Nonce', headers['X-Nonce']);
  resolved.searchParams.set('X-Signature', headers['X-Signature']);
  return url.startsWith('http') ? resolved.toString() : `${resolved.pathname}${resolved.search}`;
}

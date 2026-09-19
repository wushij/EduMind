import axios from '@/core/http/axios';

/** 将后端返回的 /api/... 规范为 axios baseURL(/api) 可用的相对路径 */
export function normalizeDownloadApiPath(apiPath: string): string {
  if (!apiPath) return apiPath;
  // 去掉 origin
  const pathOnly = apiPath.replace(/^https?:\/\/[^/]+/i, '');
  // baseURL 已是 /api 时，去掉多余前缀
  if (pathOnly.startsWith('/api/')) {
    return pathOnly.slice(4); // => /question/exports/...
  }
  if (pathOnly.startsWith('/api')) {
    return pathOnly.slice(4) || '/';
  }
  return pathOnly.startsWith('/') ? pathOnly : `/${pathOnly}`;
}

export async function downloadByApiPath(apiPath: string, filename?: string) {
  const requestPath = normalizeDownloadApiPath(apiPath);
  const response: any = await axios.get(requestPath, { responseType: 'blob' });

  const rawData = response instanceof Blob
    ? response
    : (response?.data instanceof Blob ? response.data : response);

  // 若后端返回 JSON 错误但被包装成 blob，尝试解析
  if (rawData instanceof Blob && rawData.type?.includes('json')) {
    const text = await rawData.text();
    try {
      const err = JSON.parse(text);
      throw new Error(err.message || '文件下载失败');
    } catch (e: any) {
      if (e.message && e.message !== '文件下载失败') {
        throw e;
      }
      // 非 JSON，继续当文件处理
    }
  }

  const blob = rawData instanceof Blob ? rawData : new Blob([rawData]);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'export.pdf';
  a.click();
  window.URL.revokeObjectURL(url);
}

/** 读取存储文件文本（Markdown / TXT 预览） */
export async function fetchStorageText(apiPath: string): Promise<string> {
  const requestPath = normalizeDownloadApiPath(apiPath);
  const response: unknown = await axios.get(requestPath, { responseType: 'text' });
  if (typeof response === 'string') {
    return response;
  }
  if (response && typeof response === 'object' && 'data' in response) {
    const data = (response as { data?: unknown }).data;
    if (typeof data === 'string') {
      return data;
    }
  }
  return String(response ?? '');
}

/** 读取存储文件二进制（PDF 预览等） */
export async function fetchStorageBlob(apiPath: string): Promise<Blob> {
  const requestPath = normalizeDownloadApiPath(apiPath);
  const response: unknown = await axios.get(requestPath, { responseType: 'blob' });
  if (response instanceof Blob) {
    return response;
  }
  if (response && typeof response === 'object' && 'data' in response) {
    const data = (response as { data?: unknown }).data;
    if (data instanceof Blob) {
      return data;
    }
  }
  return new Blob([response as BlobPart]);
}

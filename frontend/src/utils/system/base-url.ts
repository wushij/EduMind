/**
 * OpenAI 兼容接口 Base URL 归一化（与后端 BaseUrlNormalizer 保持同一套规则）。
 *
 * 后台「模型配置」的 Base URL 只接受「基址」，系统调用时会自动追加
 * /chat/completions（对话）或 /embeddings（向量）。若把完整端点整段粘贴进来，
 * 会出现 /v1/chat/completions/v1/chat/completions 的重复拼接导致 404，
 * 因此在保存与测试连接之前统一归一化一次，并回写表单让用户看到修正结果。
 */

const ENDPOINT_SUFFIXES = ['/chat/completions', '/embeddings', '/completions'];

/** 基址已自带版本段（/v1、/v4 等）时不再追加。 */
const VERSION_SEGMENT = /\/v\d+$/i;

export function normalizeModelBaseUrl(raw: string | null | undefined): string {
  let url = (raw ?? '').trim();
  if (!url) return '';

  url = url.replace(/\/+$/, '');
  for (const suffix of ENDPOINT_SUFFIXES) {
    if (url.length > suffix.length && url.toLowerCase().endsWith(suffix)) {
      url = url.slice(0, url.length - suffix.length).replace(/\/+$/, '');
      break;
    }
  }
  if (!url) return '';
  return VERSION_SEGMENT.test(url) ? url : `${url}/v1`;
}

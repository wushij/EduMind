/** RRF 融合分通常远小于 1，不宜直接 ×100 当相似度 */
const RRF_SCORE_CEILING = 0.25;

export function collectCitationScores(
  citations?: Array<{ score?: number }> | null
): number[] {
  if (!citations?.length) return [];
  return citations
    .map((c) => c.score)
    .filter((s): s is number => s != null && !Number.isNaN(s));
}

function isRrfStyleScore(score: number, peerMax: number): boolean {
  if (score > 1) return false;
  return peerMax <= RRF_SCORE_CEILING;
}

/**
 * 将引用分数展示为 0–100%：
 * - 向量相似度（约 0.2–1）：score × 100
 * - RRF 等融合分（一批内都很小）：按本批最高分归一化，最高为 100%
 */
export function citationMatchPercent(score?: number, peerScores?: number[]): number | null {
  if (score == null || Number.isNaN(score)) return null;
  if (score > 1) {
    return Math.round(Math.min(100, Math.max(0, score)));
  }

  const peers = peerScores?.length ? peerScores : [score];
  const maxPeer = Math.max(...peers, score);
  if (maxPeer <= 0) return 0;

  if (isRrfStyleScore(score, maxPeer)) {
    return Math.round(Math.min(100, Math.max(0, (score / maxPeer) * 100)));
  }

  return Math.round(Math.min(100, Math.max(0, score * 100)));
}

export function formatCitationMatchLabel(score?: number, peerScores?: number[]): string {
  const pct = citationMatchPercent(score, peerScores);
  if (pct == null) return '—';
  return `${pct}%`;
}

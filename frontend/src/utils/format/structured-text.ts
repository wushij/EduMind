import { renderMathText } from './render-math';

/**
 * 将 AI 生成的方案或诊断文本（如 1. 症结研判... 2. 干预对策... 或 一、偏差本质... 二、教学诊断...）
 * 智能拆分为结构化分段卡片，并自动修复数学公式 (LaTeX $、$$、\(、\[) 与换行粘连
 */
export function formatStructuredProposal(text: string): string {
  if (!text) return '';

  // 1. 清理模型末尾残留的代码标记（如：主要失分诱因代码：[ ]）与换行转义
  let normalized = text
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/主要失分诱因代码\s*[：:]\s*(\[[^\]]*\])?/g, '')
    .replace(/(?<=[。；！？\n”」）\)]|\s)\s*([0-9]{1,2}[.、]|【[^】]+】|[一二三四五六七八九十][、.]|[(（][0-9]{1,2}[)）]|[①②③④⑤⑥⑦⑧⑨⑩])/g, '\n\n$1')
    .replace(/靶向微课点拨/g, '重点考点点拨')
    .trim();

  // 2. 按多换行切分段落
  const paragraphs = normalized.split(/\n{2,}/).map(p => p.trim()).filter(Boolean);

  if (paragraphs.length <= 1 && !/^[0-9]{1,2}[.、]|^【|^[一二三四五六七八九十][、.]|^[(（][0-9]{1,2}[)）]|^[①②③④⑤⑥⑦⑧⑨⑩]/.test(normalized)) {
    // 若仅是简单纯文本，且没有编号结构，直接安全渲染公式
    return `<div class="structured-intro-p">${renderMathText(normalized)}</div>`;
  }

  const resultHtml = paragraphs.map((p) => {
    // 优先匹配带显式冒号的标题：如「1. 失分归因与认知障碍诊断：内容」或「一、偏差本质：内容」
    const matchWithColon = p.match(/^([0-9]{1,2}[.、]|【[^】]+】|[一二三四五六七八九十][、.]|[(（][0-9]{1,2}[)）]|[①②③④⑤⑥⑦⑧⑨⑩])\s*([^：:\n]{2,30}[：:])\s*([\s\S]*)$/);
    // 匹配即使缺失冒号的高频教育研判固定短语：如「一、偏差本质标准解的关键是...」或「二、教学诊断与补救根本原因是...」
    const matchCommonTitle = p.match(/^([0-9]{1,2}[.、]|【[^】]+】|[一二三四五六七八九十][、.]|[(（][0-9]{1,2}[)）]|[①②③④⑤⑥⑦⑧⑨⑩])\s*(偏差本质|教学诊断与补救|失分归因与认知障碍诊断|考点点拨与认知重建|重点考点点拨|靶向微课点拨|梯度变式训练与提分预期|梯度变式训练|教学诊断|补救对策|失分归因|认知症结研判|教学干预对策)\s*[：:]?\s*([\s\S]*)$/);
    // 匹配常规无标题数字编号：如「1. 内容」或「一、内容」
    const matchSimplePrefix = p.match(/^([0-9]{1,2}[.、]|【[^】]+】|[一二三四五六七八九十][、.]|[(（][0-9]{1,2}[)）]|[①②③④⑤⑥⑦⑧⑨⑩])\s*([\s\S]*)$/);

    let badge = '';
    let rawTitle = '';
    let body = p;

    if (matchWithColon) {
      badge = matchWithColon[1] ? matchWithColon[1].replace(/[.、()（）]/g, '') : '';
      rawTitle = matchWithColon[2] ? matchWithColon[2].trim() : '';
      body = matchWithColon[3] ? matchWithColon[3].trim() : '';
    } else if (matchCommonTitle) {
      badge = matchCommonTitle[1] ? matchCommonTitle[1].replace(/[.、()（）]/g, '') : '';
      rawTitle = matchCommonTitle[2] ? matchCommonTitle[2].trim() + '：' : '';
      body = matchCommonTitle[3] ? matchCommonTitle[3].trim() : '';
    } else if (matchSimplePrefix) {
      badge = matchSimplePrefix[1] ? matchSimplePrefix[1].replace(/[.、()（）]/g, '') : '';
      body = matchSimplePrefix[2] ? matchSimplePrefix[2].trim() : '';
    }

    if (badge) {
      const renderedBody = renderMathText(body);

      return `
        <div class="structured-point-card">
          <div class="point-badge-header">
            <span class="point-num-pill">${badge}</span>
            ${rawTitle ? `<span class="point-title-text">${rawTitle}</span>` : ''}
          </div>
          <div class="point-body-text">${renderedBody}</div>
        </div>
      `;
    }

    // 导语或普通总结段落
    return `<div class="structured-intro-p">${renderMathText(p)}</div>`;
  });

  return resultHtml.join('');
}

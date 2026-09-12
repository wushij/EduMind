import katex from 'katex';

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderKatex(formula: string, displayMode: boolean): string {
  try {
    return katex.renderToString(formula.trim(), {
      throwOnError: false,
      displayMode,
      strict: 'ignore'
    });
  } catch {
    return displayMode ? `$$${formula}$$` : `$${formula}$`;
  }
}

export function renderMathText(text: string): string {
  if (!text) return '';

  let html = escapeHtml(text);

  html = html.replace(/\$\$([\s\S]+?)\$\$/g, (_, formula: string) => {
    return `<span class="math-block">${renderKatex(formula, true)}</span>`;
  });

  html = html.replace(/\\\[([\s\S]+?)\\\]/g, (_, formula: string) => {
    return `<span class="math-block">${renderKatex(formula, true)}</span>`;
  });

  html = html.replace(/\$([^$\n]+?)\$/g, (_, formula: string) => {
    return renderKatex(formula, false);
  });

  html = html.replace(/\\\((.+?)\\\)/g, (_, formula: string) => {
    return renderKatex(formula, false);
  });

  return html;
}

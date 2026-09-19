import { ElMessage } from 'element-plus';

export interface PrintExamPaperOptions {
  /** 仅打印当前预览页（非连续预览） */
  currentViewOnly?: boolean;
  /** 打印时隐藏水印层 */
  hideWatermark?: boolean;
}

function collectDocumentStyles(): string {
  const parts: string[] = [];
  document.querySelectorAll('link[rel="stylesheet"]').forEach((node) => {
    parts.push(node.outerHTML);
  });
  document.querySelectorAll('style').forEach((node) => {
    parts.push(node.outerHTML);
  });
  return parts.join('\n');
}

function preparePrintClone(source: HTMLElement, options: PrintExamPaperOptions): HTMLElement {
  const clone = source.cloneNode(true) as HTMLElement;
  clone.removeAttribute('id');
  clone.classList.add('export-paper-root');
  clone.style.transform = 'none';
  clone.style.margin = '0';
  clone.style.padding = '0';

  if (options.hideWatermark) {
    clone.querySelectorAll('.watermark-layer').forEach((el) => el.remove());
  }

  if (options.currentViewOnly) {
    clone.querySelectorAll('.simulated-sheet:not(.is-print-active)').forEach((el) => el.remove());
  }

  clone.querySelectorAll('.simulated-sheet').forEach((sheet) => {
    const el = sheet as HTMLElement;
    el.style.transform = 'none';
    el.style.boxShadow = 'none';
    el.style.margin = '0 auto';
    el.style.width = '100%';
    el.style.maxWidth = '100%';
    // 保留 SCSS 中的卷面 padding，勿 inline 置 0（否则打印仅余 @page 边距，视觉上顶格贴边）
  });

  return clone;
}

const INLINE_PRINT_CSS = `
  @page { size: A4 portrait; margin: 18mm 16mm 16mm 16mm; }
  html, body {
    margin: 0 !important;
    padding: 0 !important;
    background: #fff !important;
    width: 100% !important;
  }
  body {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .export-paper-root {
    display: block !important;
    width: 100% !important;
    max-width: 178mm !important;
    margin: 0 auto !important;
    padding: 0 !important;
    gap: 0 !important;
    box-sizing: border-box !important;
  }
  .export-paper-root .simulated-sheet {
    display: flex !important;
    flex-direction: row !important;
    align-items: flex-start !important;
    width: 100% !important;
    max-width: 100% !important;
    min-height: auto !important;
    height: auto !important;
    margin: 0 auto !important;
    padding: 36px 32px 36px 28px !important;
    box-sizing: border-box !important;
    page-break-after: always;
    break-after: page;
    box-shadow: none !important;
    border: none !important;
    border-bottom: none !important;
    color: #000 !important;
    box-sizing: border-box !important;
  }
  .export-paper-root .simulated-sheet:last-child {
    page-break-after: auto;
    break-after: auto;
  }
`;

/** 供弹窗内滚动预览（依赖全局 export-paper-surface.scss） */
export function cloneExamPaperElement(options: PrintExamPaperOptions = {}): HTMLElement | null {
  const source = document.getElementById('printable-exam-paper');
  if (!source) {
    return null;
  }
  return preparePrintClone(source, options);
}

function writeExamPaperToIframe(iframe: HTMLIFrameElement, options: PrintExamPaperOptions): boolean {
  const doc = iframe.contentDocument;
  if (!doc) {
    return false;
  }

  const clone = cloneExamPaperElement(options);
  if (!clone) {
    return false;
  }

  doc.open();
  doc.write(
    `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8" /><title>试卷打印</title>${collectDocumentStyles()}<style>${INLINE_PRINT_CSS}</style></head><body></body></html>`
  );
  doc.close();
  doc.body.appendChild(clone);

  const titleEl =
    clone.querySelector('.paper-main-title') ?? clone.querySelector('.sheet-title') ?? clone.querySelector('.analysis-title');
  const pageTitle = titleEl?.textContent?.trim();
  if (pageTitle) {
    doc.title = pageTitle.length > 80 ? `${pageTitle.slice(0, 80)}…` : pageTitle;
  }

  return true;
}

/**
 * 在隐藏 iframe 中仅渲染试卷 DOM，再唤起系统打印（不包含侧边栏与页面外壳）。
 */
export function printExamPaperInIframe(options: PrintExamPaperOptions = {}): boolean {
  const iframe = document.createElement('iframe');
  iframe.setAttribute('title', 'exam-paper-print');
  iframe.setAttribute('aria-hidden', 'true');
  iframe.src = 'about:blank';
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  iframe.style.visibility = 'hidden';
  document.body.appendChild(iframe);

  if (!writeExamPaperToIframe(iframe, options)) {
    iframe.remove();
    ElMessage.warning('未找到可打印的试卷，请先选择试卷并等待预览加载完成');
    return false;
  }

  const win = iframe.contentWindow;
  if (!win) {
    iframe.remove();
    ElMessage.error('无法创建打印上下文');
    return false;
  }

  const cleanup = () => {
    setTimeout(() => iframe.remove(), 500);
  };

  win.onafterprint = cleanup;

  const trigger = () => {
    try {
      win.focus();
      win.print();
    } catch {
      ElMessage.error('调用打印失败，请检查浏览器打印权限');
      cleanup();
    }
  };

  setTimeout(trigger, 200);
  return true;
}

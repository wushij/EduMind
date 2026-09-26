const FENCED_CODE_BLOCK_RE = /(```[\s\S]*?```)/g;

function protectCodeSegments(content: string): { text: string; segments: string[] } {
  const segments: string[] = [];
  // 1. 先保护所有代码块、行内代码、LaTeX 公式环境及美元符公式（$$...$$、$...$、\[...\]、\(...\)、\begin{...}...\end{...}）
  let text = content.replace(
    /(```[\s\S]*?```|`[^`\n]+`|\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\begin\{[a-zA-Z*]+\}[\s\S]*?\\end\{[a-zA-Z*]+\}|(?<!\$)\$(?!\$)[^$\n]+?(?<!\$)\$(?!\$)|\\\([\s\S]*?\\\))/g,
    (segment) => {
      segments.push(segment);
      return `\u0000CODE${segments.length - 1}\u0000`;
    }
  );

  return { text, segments };
}

function restoreCodeSegments(text: string, segments: string[]): string {
  let result = text;
  for (let pass = 0; pass < 24; pass++) {
    const next = result.replace(/\u0000CODE(\d+)\u0000/g, (_match, index) => segments[Number(index)] ?? '');
    if (next === result) break;
    result = next;
  }
  return result;
}

function countTableColumns(row: string): number {
  const parts = row.trim().replace(/^\|/, '').replace(/\|$/, '').split('|');
  return Math.max(parts.length, 1);
}

function isMarkdownTableSep(trimmed: string): boolean {
  if (!trimmed.includes('|')) return false;
  if (!trimmed.includes('-') && !trimmed.includes(':')) return false;
  const body = trimmed.replace(/^\|/, '').replace(/\|$/, '');
  const cells = body.split('|').map((c) => c.trim());
  if (!cells.length) return false;
  return cells.every((c) => /^:?-{1,}:?$/.test(c));
}

function isMarkdownTableRow(trimmed: string): boolean {
  if (!trimmed || trimmed.startsWith('```')) return false;
  if (/^#{1,6}\s/.test(trimmed)) return false;
  // 必须首尾带 | 或行内有至少 2 个 pipe 的标准结构
  if (trimmed.startsWith('|') && trimmed.endsWith('|') && trimmed.length > 2) return true;
  if (trimmed.startsWith('|') && trimmed.includes('|', 1)) return true;
  const pipeCount = (trimmed.match(/\|/g) || []).length;
  return (
    pipeCount >= 2 &&
    !trimmed.startsWith('//') &&
    !trimmed.startsWith('http://') &&
    !trimmed.startsWith('https://') &&
    !trimmed.startsWith('>')
  );
}

/**
 * 模型常把「## 标题 | 列1 | 列2 …」压成一行，需拆成标题 + 表格。
 * 必须确保后半段真正是表格表头（紧随分隔行，或以 | 开头结尾且拥有至少 2 个列单元格），绝不误拆数学公式（如绝对值）与普通副标题。
 */
function splitHeadingFromPipeLine(line: string, nextLine?: string): string {
  const trimmed = line.trim();
  if (!/^#{1,6}/.test(trimmed)) return line;

  const match = trimmed.match(/^([ \t]*#{1,6}[ \t]*[^\n|]+?)(\s*\|[^|\n]+\|.*)$/);
  if (!match) return line;

  const candidateTable = match[2].trim();
  const hasSepNext = nextLine ? isMarkdownTableSep(nextLine.trim()) : false;
  const pipeCount = (candidateTable.match(/\|/g) || []).length;
  // 仅当下行紧跟表格分隔行，或者该部分具有完整表头结构（以 | 开头且结尾，包含至少 2 个列）时才拆分
  const isLikelyTableHeader = candidateTable.startsWith('|') && candidateTable.endsWith('|') && pipeCount >= 3;

  if (hasSepNext || isLikelyTableHeader) {
    const heading = match[1].trim().replace(/^(#{1,6})\s*/, '$1 ');
    return `${heading}\n\n${candidateTable}`;
  }

  return line;
}

function extractLeadingHeading(cells: string[]): { heading?: string; cells: string[] } {
  if (!cells.length) return { cells };
  const first = cells[0]?.trim() ?? '';
  const headingMatch = first.match(/^(#{1,6}\s+.+)$/);
  if (headingMatch) {
    return { heading: headingMatch[1], cells: cells.slice(1) };
  }
  return { cells };
}

function buildTableSeparator(colCount: number): string {
  return `| ${Array.from({ length: colCount }, () => '---').join(' | ')} |`;
}

function splitTableRowCells(line: string): string[] {
  let trimmed = line.trim();
  if (trimmed.startsWith('|')) trimmed = trimmed.slice(1);
  if (trimmed.endsWith('|') && !trimmed.endsWith('\\|')) trimmed = trimmed.slice(0, -1);

  const cells: string[] = [];
  let current = '';
  let inBacktick = false;

  for (let i = 0; i < trimmed.length; i++) {
    const ch = trimmed[i];
    if (ch === '`') {
      inBacktick = !inBacktick;
      current += ch;
    } else if (ch === '\\' && i + 1 < trimmed.length && trimmed[i + 1] === '|') {
      current += '\\|';
      i++;
    } else if (ch === '|' && !inBacktick) {
      cells.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  cells.push(current);
  return cells;
}

function countOccurrences(str: string, substr: string): number {
  let count = 0;
  let pos = 0;
  while ((pos = str.indexOf(substr, pos)) !== -1) {
    count += 1;
    pos += substr.length;
  }
  return count;
}

function isDelimUnbalanced(str: string, delim: string): boolean {
  return countOccurrences(str, delim) % 2 !== 0;
}

function getMergeScore(left: string, right: string): number {
  let score = 500;
  if (isDelimUnbalanced(left, '**')) score -= 1500;
  if (isDelimUnbalanced(left, '~~') || isDelimUnbalanced(left, '*')) score -= 1300;
  if (countOccurrences(left, '(') - countOccurrences(left, ')') > 0) score -= 1100;
  if (countOccurrences(left, '[') - countOccurrences(left, ']') > 0) score -= 1100;
  if (left.trim() === '' || right.trim() === '') score -= 1000;
  return score;
}

function repairTableRow(line: string, expectedCols: number): string {
  if (expectedCols <= 0) return line;
  const trimmed = line.trim();
  if (isMarkdownTableSep(trimmed)) return buildTableSeparator(expectedCols);

  const cells = splitTableRowCells(trimmed);
  if (cells.length === expectedCols) {
    return `| ${cells.map((c) => c.trim()).join(' | ')} |`;
  }
  if (cells.length < expectedCols) {
    while (cells.length < expectedCols) cells.push('');
    return `| ${cells.map((c) => c.trim()).join(' | ')} |`;
  }

  while (cells.length > expectedCols) {
    let bestIdx = 0;
    let bestScore = Infinity;
    for (let i = 0; i < cells.length - 1; i++) {
      const score = getMergeScore(cells[i], cells[i + 1]);
      if (score < bestScore) {
        bestScore = score;
        bestIdx = i;
      }
    }
    cells.splice(bestIdx, 2, `${cells[bestIdx]}__EMD_PIPE_MERGE__${cells[bestIdx + 1]}`);
  }

  return `| ${cells.map((c) => c.replace(/__EMD_PIPE_MERGE__/g, '\\|').trim()).join(' | ')} |`;
}

function isPlainPipeTableSep(trimmed: string): boolean {
  if (!trimmed.includes('|')) return false;
  const cells = trimmed.split('|').map((c) => c.trim());
  return cells.length >= 2 && cells.every((c) => /^:?-{2,}:?$/.test(c));
}

function isPlainPipeTableRow(trimmed: string): boolean {
  if (/^#{1,6}\s/.test(trimmed)) return false;
  return trimmed.includes('|') && !trimmed.startsWith('|') && !isPlainPipeTableSep(trimmed);
}

function buildMarkdownTableRows(cells: string[], colCount: number, titlePrefix?: string): string {
  const rows: string[] = [];
  for (let r = 0; r < cells.length / colCount; r++) {
    const rowCells = cells.slice(r * colCount, (r + 1) * colCount);
    rows.push(`| ${rowCells.map((c) => c.trim()).join(' | ')} |`);
  }
  const table = [rows[0], buildTableSeparator(colCount), ...rows.slice(1)].join('\n');
  return titlePrefix ? `${titlePrefix.trim()}\n\n${table}` : table;
}

/** 模型常把整张表压成一行 pipe 文本，拆成标准 Markdown 表格 */
function expandCompressedPipeLine(trimmed: string): string | null {
  const pipeCount = (trimmed.match(/\|/g) || []).length;
  if (pipeCount < 3) return null;

  let cells = splitTableRowCells(trimmed);
  if (cells.length < 6) return null;

  const { heading, cells: bodyCells } = extractLeadingHeading(cells);
  cells = bodyCells;
  if (cells.length < 6) return heading ? `${heading}\n\n${trimmed}` : null;

  for (let colCount = 8; colCount >= 3; colCount--) {
    if (cells.length % colCount !== 0) continue;
    if (cells.length / colCount < 2) continue;
    return buildMarkdownTableRows(cells, colCount, heading);
  }

  const rest = cells.slice(1);
  for (let colCount = 8; colCount >= 3; colCount--) {
    if (rest.length < colCount * 2) continue;
    if (rest.length % colCount !== 0) continue;
    return buildMarkdownTableRows(rest, colCount, heading ?? cells[0]);
  }

  return null;
}

function normalizePlainPipeTables(content: string): string {
  const lines = content.split('\n');
  const out: string[] = [];
  let i = 0;

  while (i < lines.length) {
    const trimmed = lines[i].trim();
    const nextTrim = i + 1 < lines.length ? lines[i + 1].trim() : '';

    const expanded = expandCompressedPipeLine(trimmed);
    if (expanded) {
      out.push(expanded);
      i += 1;
      continue;
    }

    if (isPlainPipeTableRow(trimmed) && isPlainPipeTableSep(nextTrim)) {
      const colCount = Math.max(countTableColumns(nextTrim), splitTableRowCells(trimmed).length);
      out.push(repairTableRow(trimmed, colCount));
      out.push(buildTableSeparator(colCount));
      i += 2;
      while (i < lines.length && (isPlainPipeTableRow(lines[i].trim()) || isMarkdownTableRow(lines[i].trim()))) {
        const rowTrim = lines[i].trim();
        if (isMarkdownTableSep(rowTrim) || isPlainPipeTableSep(rowTrim)) {
          i += 1;
          continue;
        }
        out.push(repairTableRow(rowTrim, colCount));
        i += 1;
      }
      continue;
    }

    out.push(lines[i]);
    i += 1;
  }

  return out.join('\n');
}

function normalizeMarkdownTables(content: string): string {
  const lines = content.split('\n');
  const out: string[] = [];
  let i = 0;

  while (i < lines.length) {
    const trimmed = lines[i].trim();
    if (!isMarkdownTableRow(trimmed) && !isMarkdownTableSep(trimmed)) {
      out.push(lines[i]);
      i++;
      continue;
    }

    // 收集连续的候选表格行
    const tableLines: string[] = [];
    while (
      i < lines.length &&
      (isMarkdownTableRow(lines[i].trim()) || isMarkdownTableSep(lines[i].trim()))
    ) {
      tableLines.push(lines[i]);
      i++;
    }

    // 判断该连续块是否真正构成表格：
    // 1. 包含明确的分隔行（|---|---|）；或者
    // 2. 至少有 2 行数据行（大模型漏写分隔行）
    const hasSep = tableLines.some((l) => isMarkdownTableSep(l.trim()));
    const isRealTable = hasSep || tableLines.length >= 2;

    if (!isRealTable) {
      // 只有单行且无分隔行，绝非 Markdown 表格（如带有数学绝对值的独立行），按原样输出
      for (const line of tableLines) {
        out.push(line);
      }
      continue;
    }

    // 对真正的表格块进行规范化
    let sawSeparator = false;
    const firstSep = tableLines.find((l) => isMarkdownTableSep(l.trim()));
    let tableColCount = firstSep ? countTableColumns(firstSep.trim()) : countTableColumns(tableLines[0].trim());

    let processedRowIndex = 0;
    for (const rawLine of tableLines) {
      const lineTrim = rawLine.trim();
      const isSep = isMarkdownTableSep(lineTrim);

      if (isSep) {
        if (sawSeparator) continue;
        sawSeparator = true;
        tableColCount = Math.max(tableColCount, countTableColumns(lineTrim));
        out.push(buildTableSeparator(tableColCount));
        processedRowIndex++;
        continue;
      }

      // 如果到了第 2 行（索引 1）还没有分隔行，自动插入分隔行
      if (processedRowIndex === 1 && !sawSeparator) {
        out.push(buildTableSeparator(tableColCount));
        sawSeparator = true;
        processedRowIndex++;
      }

      out.push(repairTableRow(lineTrim, tableColCount));
      processedRowIndex++;
    }
  }

  return out.join('\n');
}

/** 聊天 Markdown 表格容错：pipe 单行表、缺分隔符表、标准 GFM 表 */
export function normalizeChatTables(content: string): string {
  if (!content?.trim()) return content;
  const withoutCodeBlocks = content.split(FENCED_CODE_BLOCK_RE).map((segment, index) => {
    if (index % 2 === 1) return segment;

    // 先保护代码块、数学公式（$$...$$、$...$、\[...\]、\(...\)）及绝对值
    const { text: protectedSegment, segments } = protectCodeSegments(segment);
    const lines = protectedSegment.split('\n');
    const withHeadingSplit = lines
      .map((line, idx) => splitHeadingFromPipeLine(line, lines[idx + 1]))
      .join('\n');

    let text = normalizePlainPipeTables(withHeadingSplit);
    text = normalizeMarkdownTables(text);
    let restored = restoreCodeSegments(text, segments);

    // 若非代码段末尾是以 | 结尾的表格行，确保其末尾保留空行，防止在还原代码块时与 ``` 紧贴
    if (restored.trimEnd().endsWith('|') && !restored.endsWith('\n\n')) {
      restored = restored.trimEnd() + '\n\n';
    }
    return restored;
  });
  return withoutCodeBlocks.join('');
}

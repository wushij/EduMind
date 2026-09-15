const FENCED_CODE_BLOCK_RE = /(```[\s\S]*?```)/g;

function protectCodeSegments(content: string): { text: string; segments: string[] } {
  const segments: string[] = [];
  // 保护代码块、数学公式（$$...$$、$...$、\[...\]、\(...\)）以及几何线段绝对值（如 |AF|、|BF|）
  const text = content.replace(
    /(```[\s\S]*?```|`[^`\n]+`|\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|(?<!\$)\$(?!\$)[^$\n]+?(?<!\$)\$(?!\$)|\\\([\s\S]*?\\\)|\b\|[A-Za-z0-9_+^.-]{1,10}\|\b|(?<!\S)\|[A-Za-z0-9_+^.-]{1,10}\|(?!\S))/g,
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

/** 模型常把「## 标题|列1|列2…」压成一行，需拆成标题 + 表格 */
function splitHeadingFromPipeLine(line: string): string {
  const trimmed = line.trim();
  const match = trimmed.match(/^(#{1,6}\s+[^\n|]+?)(\s*\|.+)$/);
  if (!match) return line;
  return `${match[1].trim()}\n\n${match[2].trim()}`;
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
  const { text, segments } = protectCodeSegments(content);
  const lines = text.split('\n');
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

  return restoreCodeSegments(out.join('\n'), segments);
}

function normalizeMarkdownTables(content: string): string {
  const { text, segments } = protectCodeSegments(content);
  const lines = text.split('\n');
  const out: string[] = [];
  let inTable = false;
  let tableRowIndex = 0;
  let sawSeparator = false;
  let tableColCount = 0;

  for (let idx = 0; idx < lines.length; idx++) {
    const line = lines[idx];
    const trimmed = line.trim();
    const isTableSep = isMarkdownTableSep(trimmed);
    const isTableRow = isMarkdownTableRow(trimmed);

    if (isTableRow || isTableSep) {
      if (!inTable) {
        inTable = true;
        tableRowIndex = 0;
        sawSeparator = false;
        const nextLine = idx + 1 < lines.length ? lines[idx + 1].trim() : '';
        tableColCount = isMarkdownTableSep(nextLine) ? countTableColumns(nextLine) : countTableColumns(trimmed);
      }

      if (tableRowIndex === 1 && !isTableSep) {
        out.push(buildTableSeparator(tableColCount));
        sawSeparator = true;
        tableRowIndex += 1;
      }

      if (isTableSep) {
        if (sawSeparator) continue;
        sawSeparator = true;
        tableColCount = countTableColumns(trimmed);
        out.push(buildTableSeparator(tableColCount));
        tableRowIndex += 1;
        continue;
      }

      out.push(repairTableRow(trimmed, tableColCount));
      tableRowIndex += 1;
      continue;
    }

    inTable = false;
    tableRowIndex = 0;
    sawSeparator = false;
    tableColCount = 0;
    out.push(line);
  }

  return restoreCodeSegments(out.join('\n'), segments);
}

/** 聊天 Markdown 表格容错：pipe 单行表、缺分隔符表、标准 GFM 表 */
export function normalizeChatTables(content: string): string {
  if (!content?.trim()) return content;
  const withoutCodeBlocks = content.split(FENCED_CODE_BLOCK_RE).map((segment, index) => {
    if (index % 2 === 1) return segment;
    const withHeadingSplit = segment
      .split('\n')
      .map((line) => splitHeadingFromPipeLine(line))
      .join('\n');
    let text = normalizePlainPipeTables(withHeadingSplit);
    text = normalizeMarkdownTables(text);
    // 若非代码段末尾是以 | 结尾的表格行，确保其末尾保留空行，防止在还原代码块时与 ``` 紧贴
    if (text.trimEnd().endsWith('|') && !text.endsWith('\n\n')) {
      text = text.trimEnd() + '\n\n';
    }
    return text;
  });
  return withoutCodeBlocks.join('');
}

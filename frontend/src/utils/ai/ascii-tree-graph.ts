/** 检测是否为「├─ / └─ / │」字符画知识树（非真实源码） */
export function looksLikeAsciiKnowledgeTree(code: string): boolean {
  const t = code.trim();
  if (!/[├└]─/.test(t) && !/[├└]/.test(t)) return false;
  const branchCount = (t.match(/[├└]/g) || []).length;
  if (branchCount < 2) return false;
  if (/^(?:public\s+class|import\s+|#include|package\s+|def\s+|function\s+|const\s+\w+\s*=)/m.test(t)) {
    return false;
  }
  if (/;\s*$|;\s*\n/m.test(t) && !/：|←/.test(t)) return false;
  return true;
}

/** 将粘连在一行的字符树拆成多行 */
export function expandAsciiTreeToMultiline(text: string): string {
  let s = text.replace(/\r\n/g, '\n').trim();
  if (!/[├└│]/.test(s)) return text;

  // 先拆「父节点末尾 │ ├─子节点」类嵌套，保留行首 │
  s = s.replace(/([^\n\r])(│[ \t]+[├└]─)/g, '$1\n$2');

  // 再拆顶层 ├─ / └─（保留「│ ├─」子节点行）
  s = s.replace(/([^\n\r])([├└]─)/g, (full, before: string, branch: string, offset: number, whole: string) => {
    const head = whole.slice(0, offset + before.length);
    if (/│[ \t]*$/.test(head)) return full;
    return `${before}\n${branch}`;
  });

  s = s.replace(/([^\n\r])([├└](?!─))/g, (full, before: string, branch: string, offset: number, whole: string) => {
    const head = whole.slice(0, offset + before.length);
    if (/│[ \t]*$/.test(head)) return full;
    return `${before}\n${branch}`;
  });

  return s;
}

function escapeMermaidLabel(label: string): string {
  return label
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\n/g, ' ')
    .trim()
    .slice(0, 44);
}

function branchDepth(line: string): number {
  const trimmed = line.trimStart();
  let pipes = 0;
  for (const ch of trimmed) {
    if (ch === '│') pipes += 1;
    else if (ch === '├' || ch === '└') break;
    else if (ch === ' ') continue;
    else break;
  }
  return pipes + 1;
}

function branchLabel(line: string): string | null {
  const m = line.trim().match(/^[│\s]*[├└]─?\s*(.*)$/);
  if (!m) return null;
  const label = m[1].replace(/\s*│\s*$/g, '').trim();
  return label || null;
}

/** 将字符树转为 Mermaid flowchart；失败时返回 null */
export function asciiTreeToMermaid(code: string): string | null {
  const expanded = expandAsciiTreeToMultiline(code);
  const rawLines = expanded.split('\n').map((l) => l.trimEnd()).filter((l) => l.trim());

  if (rawLines.length < 2) return null;

  let rootLabel = '';
  let bodyLines = rawLines;
  if (!/^[│\s]*[├└]/.test(rawLines[0])) {
    rootLabel = rawLines[0].trim();
    bodyLines = rawLines.slice(1);
  }

  const nodeDecls: string[] = [];
  const edgeDecls: string[] = [];
  const stack: { id: string; depth: number }[] = [];
  let counter = 0;

  let rootId = '';
  if (rootLabel) {
    rootId = `T${counter++}`;
    nodeDecls.push(`${rootId}["${escapeMermaidLabel(rootLabel)}"]`);
    stack.push({ id: rootId, depth: 0 });
  }

  for (const line of bodyLines) {
    const label = branchLabel(line);
    if (!label) continue;

    const depth = branchDepth(line);
    const id = `T${counter++}`;
    nodeDecls.push(`${id}["${escapeMermaidLabel(label)}"]`);

    while (stack.length > 0 && stack[stack.length - 1].depth >= depth) {
      stack.pop();
    }

    const parent = stack[stack.length - 1];
    if (parent) {
      edgeDecls.push(`${parent.id} --> ${id}`);
    }

    stack.push({ id, depth });
  }

  if (nodeDecls.length < 2 || edgeDecls.length < 1) return null;

  return ['flowchart TD', ...nodeDecls, ...edgeDecls].join('\n');
}

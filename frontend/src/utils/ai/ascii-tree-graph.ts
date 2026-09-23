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

interface ParsedBranch {
  label: string;
  indent: number;
}

function parseBranchLine(line: string): ParsedBranch | null {
  // 匹配前缀缩进（空格、制表符、竖线）以及分支标志（├、└、+、|）和横线
  // 如 "│   └── com/" 或 "    ├── src/" 或 "├─── EnvDiagnostics.java"
  const m = line.match(/^([\s│\|]*)([├└\+\|][─\-\s]*)(.*)$/);
  if (!m) return null;

  const prefix = m[1];
  let rawLabel = m[3] || '';

  // 彻底剔除标签开头的残留连接符（如 ─、-、—、-- 以及多余空格）
  rawLabel = rawLabel
    .replace(/^[─\-—\s]+/, '')
    .replace(/\s*│\s*$/g, '')
    .trim();
  if (!rawLabel) return null;

  // 计算视觉缩进：制表符转4空格，竖线与普通字符等宽
  const visualIndent = prefix.replace(/\t/g, '    ').length;

  return {
    label: rawLabel,
    indent: visualIndent
  };
}

/** 将字符树转为 Mermaid flowchart；失败时返回 null */
export function asciiTreeToMermaid(code: string): string | null {
  const expanded = expandAsciiTreeToMultiline(code);
  const rawLines = expanded.split('\n').map((l) => l.trimEnd()).filter((l) => l.trim());

  if (rawLines.length < 2) return null;

  let rootLabel = '';
  let bodyLines = rawLines;
  if (!/^[│\s]*[├└\+\|]/.test(rawLines[0])) {
    rootLabel = rawLines[0].trim();
    bodyLines = rawLines.slice(1);
  }

  const nodeDecls: string[] = [];
  const edgeDecls: string[] = [];
  const stack: { id: string; indent: number }[] = [];
  let counter = 0;

  if (rootLabel) {
    const rootId = `T${counter++}`;
    nodeDecls.push(`${rootId}["${escapeMermaidLabel(rootLabel)}"]`);
    stack.push({ id: rootId, indent: -1 });
  }

  for (const line of bodyLines) {
    const branch = parseBranchLine(line);
    if (!branch) continue;

    const id = `T${counter++}`;
    nodeDecls.push(`${id}["${escapeMermaidLabel(branch.label)}"]`);

    // 如果当前缩进小于等于栈顶节点的缩进，说明当前节点是栈顶节点的兄弟或更浅层节点，依次退栈
    while (stack.length > 1 && stack[stack.length - 1].indent >= branch.indent) {
      stack.pop();
    }

    const parent = stack[stack.length - 1];
    if (parent) {
      edgeDecls.push(`${parent.id} --> ${id}`);
    }

    stack.push({ id, indent: branch.indent });
  }

  if (nodeDecls.length < 2 || edgeDecls.length < 1) return null;

  return ['flowchart TD', ...nodeDecls, ...edgeDecls].join('\n');
}

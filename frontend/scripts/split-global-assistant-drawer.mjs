import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const srcPath = join(__dirname, '../src/components/ai/GlobalAssistantDrawer.vue');
const raw = readFileSync(srcPath, 'utf8');
const templateMatch = raw.match(/<template>([\s\S]*)<\/template>/);
if (!templateMatch) throw new Error('no template');
const template = templateMatch[1];

const outDir = join(__dirname, '../src/components/ai/global-assistant');
mkdirSync(outDir, { recursive: true });

const markers = [
  { name: 'GlobalAssistantHeader.vue', start: '<!-- 1. 顶部 Header -->', end: '<!-- 课程研读上下文横条' },
  { name: 'GlobalAssistantContextBanner.vue', start: '<!-- 课程研读上下文横条', end: '<!-- 历史会话下拉层' },
  { name: 'GlobalAssistantHistoryPanel.vue', start: '<!-- 历史会话下拉层', end: '<!-- 2. 消息流式主列表区域 -->' },
  { name: 'GlobalAssistantMessageList.vue', start: '<!-- 2. 消息流式主列表区域 -->', end: '<!-- 悬浮微圆钮' },
  { name: 'GlobalAssistantScrollFab.vue', start: '<!-- 悬浮微圆钮', end: '<!-- 4. 底部输入栏 -->' },
  { name: 'GlobalAssistantComposer.vue', start: '<!-- 4. 底部输入栏 -->', end: '</div>\n          </div>\n        </div>\n      </transition>' }
];

for (const m of markers) {
  const i = template.indexOf(m.start);
  const j = template.indexOf(m.end, i);
  if (i < 0 || j < 0) throw new Error(`marker failed ${m.name}`);
  const chunk = template.slice(i, j).trim();
  const vue = `<template>\n${chunk}\n</template>\n`;
  writeFileSync(join(outDir, m.name), vue, 'utf8');
  console.log('wrote', m.name, chunk.split('\n').length, 'lines');
}

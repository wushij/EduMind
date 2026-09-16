import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const srcPath = join(__dirname, '../src/components/course/CourseAIPanel.vue');
const raw = readFileSync(srcPath, 'utf8');
const templateMatch = raw.match(/<template>([\s\S]*)<\/template>/);
if (!templateMatch) throw new Error('no template');
const template = templateMatch[1];

const outDir = join(__dirname, '../src/components/course/course-ai');
mkdirSync(outDir, { recursive: true });

const markers = [
  { name: 'CourseAIHeroBanner.vue', start: '<!-- 1. 顶部专属视觉大 Banner -->', end: '<!-- 2. 原型同款三栏式教学工作台' },
  { name: 'CourseAIWorkbench.vue', start: '<!-- 2. 原型同款三栏式教学工作台', end: '<!-- 历史记录侧边抽屉 -->' },
  { name: 'CourseAIHistoryDrawer.vue', start: '<!-- 历史记录侧边抽屉 -->', end: '</div>\n</template>' }
];

for (const m of markers) {
  const i = template.indexOf(m.start);
  const j = template.indexOf(m.end, i);
  if (i < 0 || j < 0) throw new Error(`marker failed ${m.name} i=${i} j=${j}`);
  const chunk = template.slice(i, j).trim();
  writeFileSync(join(outDir, m.name), `<template>\n${chunk}\n</template>\n`, 'utf8');
  console.log('wrote', m.name);
}

const styleStart = raw.indexOf('<style scoped lang="scss">');
const styleEnd = raw.indexOf('</style>', styleStart);
const styleBody = raw
  .slice(styleStart + '<style scoped lang="scss">'.length, styleEnd)
  .trim();
writeFileSync(join(outDir, 'course-ai-panel-styles.scss'), styleBody, 'utf8');
console.log('wrote styles', styleBody.split('\n').length);

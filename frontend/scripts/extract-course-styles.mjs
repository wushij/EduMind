import { readFileSync, writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execSync } from 'node:child_process';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const raw = execSync('git show HEAD:frontend/src/components/course/CourseAIPanel.vue', {
  cwd: join(root, '..'),
  encoding: 'utf8'
});
const open = '<style scoped lang="scss">';
const a = raw.indexOf(open);
const b = raw.indexOf('</style>', a);
if (a < 0 || b < 0) throw new Error('style block not found');
const out = join(root, 'src/components/course/course-ai/course-ai-panel-styles.scss');
writeFileSync(out, raw.slice(a + open.length, b).trim(), 'utf8');
console.log('wrote', out, 'lines', raw.slice(a + open.length, b).trim().split('\n').length);

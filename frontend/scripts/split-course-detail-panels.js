const fs = require('fs');
const { execSync } = require('child_process');

const repoRoot = 'e:/EduMind';
const srcRoot = `${repoRoot}/frontend/src`;

execSync(
  'git checkout HEAD -- frontend/src/views/course/detail/KnowledgePoints.vue frontend/src/views/course/detail/Overview.vue',
  { cwd: repoRoot, stdio: 'inherit' }
);

function readGit(path) {
  return execSync(`git show HEAD:${path}`, {
    cwd: repoRoot,
    encoding: 'utf8',
    maxBuffer: 10 * 1024 * 1024,
  });
}

fs.writeFileSync(
  `${srcRoot}/components/course/KnowledgePointPanel.vue`,
  readGit('frontend/src/views/course/detail/KnowledgePoints.vue'),
  'utf8'
);
fs.writeFileSync(
  `${srcRoot}/components/course/CourseOverviewPanel.vue`,
  readGit('frontend/src/views/course/detail/Overview.vue'),
  'utf8'
);

const kpPath = `${srcRoot}/components/course/KnowledgePointPanel.vue`;
let kp = fs.readFileSync(kpPath, 'utf8');
const scriptRe = /<script setup lang="ts">[\s\S]*?<\/script>/;
const newScript = `<script setup lang="ts">
import { Search, Plus, StarFilled, Opportunity, Connection } from '@element-plus/icons-vue';
import { useKnowledgePoint } from '@/composables/course/useKnowledgePoint';

const {
  loading,
  creating,
  showCreateDrawer,
  showGraphDrawer,
  selectedGraphKp,
  searchKeyword,
  selectedChapterId,
  selectedLevel,
  chapters,
  knowledgePoints,
  newKp,
  filteredPoints,
  loadKnowledgePoints,
  getChapterTitle,
  getPointsForChapter,
  getLevelLabel,
  getLevelTagType,
  handleSaveNewKp,
  openGraphDrawer,
  handleAskAi,
  handleGenerateQuizForKp,
  handleDeleteKp,
} = useKnowledgePoint();
</script>`;

if (!scriptRe.test(kp)) {
  throw new Error('script block not found in KnowledgePointPanel.vue');
}
kp = kp.replace(scriptRe, newScript);
fs.writeFileSync(kpPath, kp, 'utf8');

const kpShell = `<template>
  <KnowledgePointPanel />
</template>

<script setup lang="ts">
import KnowledgePointPanel from '@/components/course/KnowledgePointPanel.vue';
</script>
`;

const ovShell = `<template>
  <CourseOverviewPanel :course="course" />
</template>

<script setup lang="ts">
import type { Course } from '@/types/course/course';
import CourseOverviewPanel from '@/components/course/CourseOverviewPanel.vue';

defineProps<{
  course?: Course | null;
}>();
</script>
`;

fs.writeFileSync(`${srcRoot}/views/course/detail/KnowledgePoints.vue`, kpShell, 'utf8');
fs.writeFileSync(`${srcRoot}/views/course/detail/Overview.vue`, ovShell, 'utf8');

console.log('KnowledgePointPanel lines:', fs.readFileSync(kpPath, 'utf8').split('\n').length);
console.log(
  'CourseOverviewPanel lines:',
  fs.readFileSync(`${srcRoot}/components/course/CourseOverviewPanel.vue`, 'utf8').split('\n').length
);

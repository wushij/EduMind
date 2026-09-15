import fs from 'fs';

const vue = fs.readFileSync('src/views/question/exams/ExamDetail.vue', 'utf8');
const tpl = vue.match(/<template>([\s\S]*)<\/template>/)[1];

let heroInner = tpl.split('\n').slice(3, 58).join('\n');
heroInner = heroInner
  .replace("@click=\"router.push('/question/exams')\"", '@click="router.push(\'/question/exams\')"')
  .replace('@click="handlePublishAsAssignment"', '@click="emit(\'publish\')"')
  .replace('@click="handleExportPaper"', '@click="emit(\'export\')"');
const heroTpl = `
<div class="exam-detail-hero-wrap">
${heroInner}
</div>
`.trim();

let paperTpl = tpl.split('\n').slice(62, 198).join('\n').trim();
paperTpl = paperTpl.replace('@click="printPaper"', '@click="emit(\'print\')"');
const sidebarTpl = tpl.split('\n').slice(200, 267).join('\n').trim();

function unindent(scss, spaces) {
  return scss
    .split('\n')
    .map((line) => (line.startsWith(' '.repeat(spaces)) ? line.slice(spaces) : line))
    .join('\n');
}

const heroScss = unindent(fs.readFileSync('scripts/_hero.scss', 'utf8'), 2);
const paperScss = unindent(fs.readFileSync('scripts/_paper.scss', 'utf8'), 4);
const sidebarScss = unindent(fs.readFileSync('scripts/_sidebar.scss', 'utf8'), 4);

const heroScript = `<script setup lang="ts">
import { ArrowLeft, Promotion, Download } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';
import type { Router } from 'vue-router';

defineProps<{
  router: Router;
  loading: boolean;
  examData: ExamPaper | null;
  getStatusLabel: (status?: string) => string;
  getStatusTagType: (status?: string) => string;
}>();

const emit = defineEmits<{
  publish: [];
  export: [];
}>();
</script>`;

const paperScript = `<script setup lang="ts">
import { Document, View, Printer, Check } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';
import type { GroupedSection } from '@/composables/question/useExam';

const viewMode = defineModel<'PAPER' | 'ANSWER_KEY'>('viewMode', { required: true });

defineProps<{
  examData: ExamPaper | null;
  groupedSections: GroupedSection[];
  totalQuestionsCount: number;
  getChineseNumber: (n: number) => string;
}>();

const emit = defineEmits<{
  print: [];
}>();
</script>`;

const sidebarScript = `<script setup lang="ts">
import { TrendCharts } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';
import type { GroupedSection } from '@/composables/question/useExam';

defineProps<{
  examData: ExamPaper | null;
  groupedSections: GroupedSection[];
  totalQuestionsCount: number;
  difficultyCounts: { EASY: number; MEDIUM: number; HARD: number };
  difficultyPercentages: { EASY: number; MEDIUM: number; HARD: number };
  coveredKnowledgePoints: string[];
}>();
</script>`;

function wrap(name, template, script, style) {
  return `<template>\n${template}\n</template>\n\n${script}\n\n<style scoped lang="scss">\n${style}\n</style>\n`;
}

fs.mkdirSync('src/components/question/exam', { recursive: true });
fs.writeFileSync(
  'src/components/question/exam/ExamDetailHeroSection.vue',
  wrap('hero', heroTpl, heroScript, `.exam-detail-hero-wrap {\n${heroScss}\n}`)
);
fs.writeFileSync(
  'src/components/question/exam/ExamDetailPaperDisplay.vue',
  wrap('paper', paperTpl, paperScript, paperScss)
);
fs.writeFileSync(
  'src/components/question/exam/ExamDetailStatsSidebar.vue',
  wrap('sidebar', sidebarTpl, sidebarScript, sidebarScss)
);
console.log('components written');

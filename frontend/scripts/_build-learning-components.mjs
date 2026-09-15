import fs from 'fs';

const vue = fs.readFileSync('src/views/learning/LearningHome.vue', 'utf8');
const tpl = vue.match(/<template>([\s\S]*)<\/template>/)[1];
const style = vue.match(/<style scoped lang="scss">([\s\S]*)<\/style>/)[1];

const leftTpl = tpl.split('\n').slice(35, 145).join('\n').trim();
const rightTpl = tpl.split('\n').slice(147, 175).join('\n').trim();

// Extract left column styles: weakness + tasks blocks inside .section-card
const leftStyleStart = style.indexOf('      // 薄弱考点列表');
const leftStyleEnd = style.indexOf('      // 推荐卡片堆叠');
const leftInner = style.slice(leftStyleStart, leftStyleEnd);

const headerStyleStart = style.indexOf('      .section-card-header');
const headerStyleEnd = style.indexOf('      // 薄弱考点列表');
const headerInner = style.slice(headerStyleStart, headerStyleEnd);

const rightStyleStart = style.indexOf('      // 推荐卡片堆叠');
const rightInner = style.slice(rightStyleStart);

const leftScss = `.learning-left-col {\n  .section-card {\n    background: #FFFFFF;\n    border-radius: 18px;\n    border: 1px solid #E2E8F0;\n    padding: 22px 24px;\n    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);\n    margin-bottom: 20px;\n${headerInner}\n${leftInner}\n  }\n}`;

const rightScss = `.learning-right-col {\n  .section-card {\n    background: #FFFFFF;\n    border-radius: 18px;\n    border: 1px solid #E2E8F0;\n    padding: 22px 24px;\n    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);\n    margin-bottom: 20px;\n${headerInner}\n${rightInner}\n  }\n}`;

const leftScript = `<script setup lang="ts">
import {
  WarningFilled,
  MagicStick,
  DocumentChecked,
  Timer,
  Check,
  Right
} from '@element-plus/icons-vue';

type WeakPoint = {
  id: string;
  name: string;
  course: string;
  mastery: number;
  level: string;
  reason: string;
};

type TodayTask = {
  id: string;
  title: string;
  course: string;
  type: string;
  estimatedMinutes: number;
  completed: boolean;
};

defineProps<{
  weakPoints: WeakPoint[];
  todayTasks: TodayTask[];
}>();

const emit = defineEmits<{
  'generate-weak': [];
  'study-point': [point: WeakPoint];
  'execute-task': [task: TodayTask];
}>();
</script>`;

const rightScript = `<script setup lang="ts">
import { Aim, Right } from '@element-plus/icons-vue';
import RecommendationCard from '@/components/learning/RecommendationCard.vue';
import type { RecommendationItem } from '@/types/learning/recommendation';
import { useRouter } from 'vue-router';

defineProps<{
  topRecommendations: RecommendationItem[];
}>();

const emit = defineEmits<{
  'start-recommendation': [item: RecommendationItem];
  'discuss-ai': [item: RecommendationItem];
}>();

const router = useRouter();
</script>`;

function wrap(template, script, scss) {
  return `<template>\n${template}\n</template>\n\n${script}\n\n<style scoped lang="scss">\n${scss}\n</style>\n`;
}

let left = leftTpl
  .replace('@click="handleGenerateWeakQuestions"', '@click="emit(\'generate-weak\')"')
  .replace('@click="handleStudyPoint(point)"', '@click="emit(\'study-point\', point)"')
  .replace('@click="handleExecuteTask(task)"', '@click="emit(\'execute-task\', task)"');

let right = rightTpl
  .replace('@start="handleStartRecommendation"', '@start="emit(\'start-recommendation\', $event)"')
  .replace('@discuss="handleDiscussAI"', '@discuss="emit(\'discuss-ai\', $event)"');

fs.writeFileSync('src/components/learning/LearningHomeLeftColumn.vue', wrap(left, leftScript, leftScss));
fs.writeFileSync('src/components/learning/LearningHomeRecommendationsPanel.vue', wrap(right, rightScript, rightScss));
console.log('learning ok');

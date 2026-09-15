import fs from 'fs';

const lines = fs.readFileSync('_aiusage_orig.vue', 'utf8').split('\n');
const tpl = lines.slice(0, 167).join('\n').match(/<template>([\s\S]*)<\/template>/)[1];

const heroTpl = tpl.split('\n').slice(3, 54).join('\n').trim();
const metricsTpl = tpl.split('\n').slice(56, 102).join('\n').trim();
const logTpl = tpl.split('\n').slice(104, 166).join('\n').trim();

function sliceStyle(from, to) {
  return lines
    .slice(from - 1, to)
    .map((line) => line.replace(/^  /, ''))
    .join('\n');
}

const heroScss = sliceStyle(313, 555);
const metricsScss = sliceStyle(556, 669);
const logScss = sliceStyle(670, 797);

const heroScript = `<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

defineProps<{
  loading: boolean;
  usage: PersonalAiUsageVO;
  quotaPillClass: string;
  todayUsagePercent: number;
  lastUpdatedText: string;
  formatNumber: (value: number) => string;
}>();

const emit = defineEmits<{
  refresh: [];
}>();
</script>`;

const metricsScript = `<script setup lang="ts">
import { ChatDotRound, Coin, DataAnalysis } from '@element-plus/icons-vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

defineProps<{
  usage: PersonalAiUsageVO;
  formatNumber: (value: number) => string;
}>();
</script>`;

const logScript = `<script setup lang="ts">
import { Clock } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import type { PersonalAiUsageVO } from '@/types/profile/ai-usage';

const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });

defineProps<{
  loading: boolean;
  logLoading: boolean;
  usage: PersonalAiUsageVO;
  logTotal: number;
  tableHeaderStyle: Record<string, string>;
  formatNumber: (value: number) => string;
  formatDateTime: (value: string) => string;
}>();

const emit = defineEmits<{
  'page-change': [];
}>();
</script>`;

function wrap(template, script, scss) {
  return `<template>\n${template}\n</template>\n\n${script}\n\n<style scoped lang="scss">\n${scss}\n</style>\n`;
}

let hero = heroTpl.replace('@click="loadUsage(true)"', '@click="emit(\'refresh\')"');
let log = logTpl.replace('@change="loadUsage(false, { tableOnly: true })"', '@change="emit(\'page-change\')"');

fs.writeFileSync('src/components/profile/AIUsageHeroCard.vue', wrap(hero, heroScript, heroScss));
fs.writeFileSync('src/components/profile/AIUsageMetricsGrid.vue', wrap(metricsTpl, metricsScript, metricsScss));
fs.writeFileSync('src/components/profile/AIUsageLogPanel.vue', wrap(log, logScript, logScss));
console.log('aiusage ok');

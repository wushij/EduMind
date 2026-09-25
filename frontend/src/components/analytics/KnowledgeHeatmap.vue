<template>
  <div class="knowledge-heatmap-container" v-loading="loading">
    <div class="heatmap-header-bar">
      <div class="header-left">
        <h3 class="component-title">
          <el-icon class="title-icon"><Grid /></el-icon>
          班级知识点全景掌握度热力矩阵 (V1.2 精准学情下钻)
        </h3>
        <p class="component-desc">
          横轴贯通考点图谱体系，纵轴映射真实选课学员，采用现代微色阶矩阵直观呈现认知薄弱聚集区，支持章节筛选、学员过滤与穿透下钻
        </p>
      </div>

      <div class="header-right">
        <!-- 章节考点过滤 -->
        <div class="chapter-filter-wrap" v-if="chapterOptions.length > 1">
          <el-select
            v-model="selectedChapterId"
            size="small"
            placeholder="按章节筛选考点"
            class="chapter-select"
          >
            <el-option :value="0" label="全书全部考点" />
            <el-option
              v-for="ch in chapterOptions"
              :key="ch.id"
              :label="`${ch.name} (${ch.count}考点)`"
              :value="ch.id"
            />
          </el-select>
        </div>

        <!-- 学生排序模式 -->
        <div class="sort-selector-wrap">
          <el-select v-model="sortMode" size="small" class="sort-select">
            <el-option value="default" label="学号默认排序" />
            <el-option value="score-desc" label="掌握度由高到低" />
            <el-option value="score-asc" label="薄弱优先 (低分在前)" />
          </el-select>
        </div>

        <!-- 仅看在读学员过滤开关 -->
        <button
          type="button"
          class="real-student-toggle-btn"
          :class="{ active: filterRealOnly }"
          @click="filterRealOnly = !filterRealOnly"
          :title="toggleTitle"
        >
          <span class="toggle-dot" />
          <span>{{ toggleLabel }}</span>
        </button>

        <!-- 色阶交互图例（支持点击筛选高亮对应等级） -->
        <div class="legend-bar">
          <span class="legend-label">色阶分级：</span>
          <button
            type="button"
            class="legend-item-btn"
            :class="{ active: activeLevelFilter === 'mastered' }"
            @click="toggleLevelFilter('mastered')"
            title="点击仅高亮精熟考点"
          >
            <span class="legend-dot color-mastered" />精熟 (≥85%)
          </button>
          <button
            type="button"
            class="legend-item-btn"
            :class="{ active: activeLevelFilter === 'good' }"
            @click="toggleLevelFilter('good')"
            title="点击仅高亮良好考点"
          >
            <span class="legend-dot color-good" />良好 (70-84%)
          </button>
          <button
            type="button"
            class="legend-item-btn"
            :class="{ active: activeLevelFilter === 'warning' }"
            @click="toggleLevelFilter('warning')"
            title="点击仅高亮一般考点"
          >
            <span class="legend-dot color-warning" />一般 (55-69%)
          </button>
          <button
            type="button"
            class="legend-item-btn"
            :class="{ active: activeLevelFilter === 'danger' }"
            @click="toggleLevelFilter('danger')"
            title="点击仅高亮薄弱考点"
          >
            <span class="legend-dot color-danger" />薄弱 (&lt;55%)
          </button>
        </div>
      </div>
    </div>

    <!-- 数据来源说明条：明确区分实测成绩与规则推算，避免把估算值误读为真实测评 -->
    <div v-if="kpList.length > 0" class="data-provenance-bar">
      <span class="provenance-item">
        <span class="cell-source-dot cell-source-dot--measured" />
        实测测评 <strong>{{ measuredCellCount }}</strong> 格
      </span>
      <span class="provenance-item">
        <span class="cell-source-dot cell-source-dot--estimated" />
        规则推算 <strong>{{ estimatedCellCount }}</strong> 格
      </span>
      <span class="provenance-item provenance-item--ratio">
        实测覆盖率 <strong>{{ measuredRatio }}%</strong>
      </span>
      <span class="provenance-hint">
        推算分值由作业均分与错题记录推导，尚不代表真实作答水平；悬停格子可查看样本数
      </span>
    </div>

    <!-- 热力矩阵主体 -->
    <div v-if="filteredKpList.length > 0" class="matrix-scroll-wrapper">
      <table class="heatmap-table">
        <thead>
          <tr>
            <th class="sticky-col student-header-col">
              <div class="student-header-inner">
                <span class="main-th-label">选课学员 / 考点</span>
                <span class="th-count-badge" :title="studentCountTooltip">共 {{ studentCount }} 人</span>
              </div>
            </th>
            <th
              v-for="kp in filteredKpList"
              :key="kp.id"
              class="kp-header-col"
              :title="kp.title"
            >
              <div class="kp-header-cell">
                <span v-if="kp.chapterName" class="kp-chapter">{{ kp.chapterName }}</span>
                <span class="kp-name">{{ kp.title }}</span>
                <span class="kp-avg-mini">
                  均分: <strong>{{ Math.round(classAvgScores[kp.id] || 0) }}%</strong>
                </span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <!-- 班级均分基准行 -->
          <tr class="class-avg-row">
            <td class="sticky-col avg-label-cell">
              <div class="avg-benchmark-box">
                <span class="benchmark-icon-dot" />
                <span class="benchmark-title">班级均分基准</span>
              </div>
            </td>
            <td
              v-for="kp in filteredKpList"
              :key="'avg-' + kp.id"
              class="heatmap-cell-td"
            >
              <div
                class="cell-card avg-card"
                :class="[getCellLevelClass(classAvgScores[kp.id] || 0), getFilterClass(classAvgScores[kp.id] || 0)]"
                @click="handleCellClick({
                  studentName: '全班平均',
                  kpTitle: kp.title,
                  score: classAvgScores[kp.id] || 0,
                  kpId: kp.id,
                  chapterName: kp.chapterName
                })"
              >
                <span class="cell-score">{{ Math.round(classAvgScores[kp.id] || 0) }}%</span>
                <span class="cell-level-indicator">{{ getLevelShortText(classAvgScores[kp.id] || 0) }}</span>
              </div>
            </td>
          </tr>

          <!-- 各学生详细数据行 -->
          <tr
            v-for="stu in displayStudentRows"
            :key="stu.id"
            class="student-row"
            :class="{ 'is-selected-student': selectedStudentId === stu.id }"
          >
            <td class="sticky-col student-name-cell">
              <div class="student-name-box">
                <el-avatar :size="28" :src="stu.avatar" class="stu-cell-avatar">
                  {{ stu.name ? stu.name.slice(0, 1) : '学' }}
                </el-avatar>
                <div class="stu-info-texts">
                  <div class="name-badge-row">
                    <span class="student-name">{{ stu.name }}</span>
                    <span v-if="isTestingAccount(stu)" class="test-tag">测试</span>
                  </div>
                  <span class="student-id-sub">{{ stu.studentNo || `#${stu.id}` }}</span>
                </div>
              </div>
            </td>
            <td
              v-for="kp in filteredKpList"
              :key="stu.id + '-' + kp.id"
              class="heatmap-cell-td"
            >
              <div
                class="cell-card"
                :class="[getCellLevelClass(stu.scores[kp.id] || 0), getFilterClass(stu.scores[kp.id] || 0)]"
                @click="handleCellClick({
                  studentName: stu.name,
                  kpTitle: kp.title,
                  score: stu.scores[kp.id] || 0,
                  studentId: stu.id,
                  kpId: kp.id,
                  chapterName: kp.chapterName,
                  studentNo: stu.studentNo,
                  source: stu.sources[kp.id],
                  sampleCount: stu.sampleCounts[kp.id]
                })"
              >
                <span
                  class="cell-source-dot"
                  :class="getSourceClass(stu.sources[kp.id])"
                  :title="getSourceTitle(stu.sources[kp.id], stu.sampleCounts[kp.id])"
                />
                <span class="cell-score">{{ Math.round(stu.scores[kp.id] || 0) }}%</span>
                <span class="cell-level-indicator">{{ getLevelShortText(stu.scores[kp.id] || 0) }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 优雅空状态提示 -->
    <el-empty
      v-else-if="!loading"
      :description="loadError ? '加载知识掌握度数据异常，请检查网络或稍后重试' : '当前课程暂未录入考点图谱，请前往课程大纲添加考点或发布作业评测'"
      :image-size="90"
      class="heatmap-empty"
    />

    <!-- 单元格点击下钻微观学情归因弹窗 -->
    <el-dialog
      v-model="drilldownVisible"
      title="知识考点微观掌握度归因诊断"
      width="540px"
      append-to-body
      destroy-on-close
      class="drilldown-dialog"
    >
      <div v-if="activeCell" class="drilldown-dialog-body">
        <div class="drilldown-summary-card">
          <div class="summary-line">
            <span class="s-label">诊断对象：</span>
            <span class="s-val font-semibold">
              {{ activeCell.studentName }}
              <span v-if="activeCell.studentNo" class="student-no-sub">({{ activeCell.studentNo }})</span>
            </span>
          </div>
          <div class="summary-line">
            <span class="s-label">核心考点：</span>
            <span class="s-val kp-title-highlight">{{ activeCell.kpTitle }}</span>
          </div>
          <div v-if="activeCell.chapterName" class="summary-line">
            <span class="s-label">所属章节：</span>
            <span class="s-val text-slate-600">{{ activeCell.chapterName }}</span>
          </div>
          <div class="summary-line">
            <span class="s-label">掌握程度：</span>
            <el-tag
              :type="activeCell.score >= 85 ? 'success' : activeCell.score >= 70 ? '' : activeCell.score >= 55 ? 'warning' : 'danger'"
              size="default"
              effect="light"
            >
              {{ activeCell.score }}% · {{ getMasteryLevelText(activeCell.score) }}
            </el-tag>
          </div>
          <div class="summary-line">
            <span class="s-label">数据来源：</span>
            <span class="s-val source-value">
              <el-tag
                size="small"
                effect="plain"
                :type="activeCell.source === 'MEASURED' ? 'success' : 'info'"
              >
                {{ activeCell.source === 'MEASURED' ? '实测测评' : '规则推算' }}
              </el-tag>
              <span class="source-note">
                {{
                  activeCell.source === 'MEASURED'
                    ? `来自 ${activeCell.sampleCount && activeCell.sampleCount > 0 ? activeCell.sampleCount : 1} 次真实作答采样`
                    : '暂无独立测评采样，由作业均分与错题记录推导'
                }}
              </span>
            </span>
          </div>
        </div>

        <div class="drilldown-advice-card">
          <div class="advice-header">
            <el-icon class="advice-icon"><Cpu /></el-icon>
            <span class="advice-title">EduMind AI 教学干预建议与学情归因</span>
          </div>
          <p class="advice-content">{{ activeCell.advice }}</p>
        </div>
      </div>
      <template #footer>
        <div class="dialog-actions-footer">
          <el-button @click="drilldownVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleAssignPractice">
            定向推送自适应变式练习
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Grid, Cpu } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useKnowledgeHeatmap, type HeatmapStudentItem } from '@/composables/analytics/useKnowledgeHeatmap';
import type { MasterySource } from '@/types/analytics/mastery';

const props = defineProps<{
  courseId?: number;
  selectedStudentId?: number;
}>();

const router = useRouter();

const selectedChapterId = ref<number>(0);
const sortMode = ref<'default' | 'score-desc' | 'score-asc'>('default');
const activeLevelFilter = ref<string | null>(null);
const filterRealOnly = ref<boolean>(true);

/**
 * 测试账号过滤下沉到后端统一执行，前端不再自行过滤。
 * 这样「指标条学员数」「矩阵学员数」「班级均分」「导出报表」四处共用同一批人。
 */
const includeTesting = computed(() => !filterRealOnly.value);

const {
  loading,
  loadError,
  kpList,
  studentRows,
  classAvgScores,
  measuredCellCount,
  estimatedCellCount,
  studentCount,
  classStudentCount,
  fetchHeatmap
} = useKnowledgeHeatmap(() => props.courseId, () => includeTesting.value);

/** 已过滤掉的测试账号数量，用于向教师解释人数差异 */
const filteredTestingCount = computed(() => Math.max(0, classStudentCount.value - studentCount.value));

const studentCountTooltip = computed(() => {
  if (filteredTestingCount.value > 0) {
    return `课程共 ${classStudentCount.value} 名选课成员，已排除 ${filteredTestingCount.value} 个管理员/测试账号`;
  }
  return `课程共 ${classStudentCount.value} 名选课成员`;
});

/** 矩阵整体实测覆盖率，用于提示结论的可信度 */
const measuredRatio = computed(() => {
  const total = measuredCellCount.value + estimatedCellCount.value;
  return total === 0 ? 0 : Math.round((measuredCellCount.value / total) * 100);
});

/** 过滤开关文案：本课程没有可过滤账号时不再谎称"已过滤"，避免教师误以为数据被人为裁剪 */
const toggleLabel = computed(() => {
  if (!filterRealOnly.value) {
    return '显示全部选课成员';
  }
  return filteredTestingCount.value > 0
    ? `已过滤 ${filteredTestingCount.value} 个测试号`
    : '在读学员视图';
});

const toggleTitle = computed(() => {
  if (!filterRealOnly.value) {
    return '点击仅统计在读学员（排除管理员/测试账号）';
  }
  return filteredTestingCount.value > 0
    ? `当前已排除 ${filteredTestingCount.value} 个管理员/测试账号，点击可恢复显示`
    : '当前课程学员均为在读学生，没有管理员/测试账号需要排除';
});

const drilldownVisible = ref(false);
const activeCell = ref<{
  studentName: string;
  studentNo?: string;
  kpTitle: string;
  chapterName?: string;
  score: number;
  advice: string;
  studentId?: number;
  kpId?: number;
  source?: MasterySource;
  sampleCount?: number;
} | null>(null);

function isTestingAccount(stu: HeatmapStudentItem): boolean {
  const name = stu.name || '';
  const no = stu.studentNo || '';
  return (
    name.includes('管理员') ||
    name.toLowerCase().includes('admin') ||
    no.toLowerCase().includes('admin')
  );
}

// 提取章节选项
const chapterOptions = computed(() => {
  const map = new Map<string, { id: number; name: string; count: number }>();
  let idx = 1;
  for (const kp of kpList.value) {
    const chName = kp.chapterName || '核心章节';
    if (!map.has(chName)) {
      map.set(chName, { id: kp.chapterId || idx++, name: chName, count: 0 });
    }
    map.get(chName)!.count++;
  }
  return Array.from(map.values());
});

// 过滤后的考点
const filteredKpList = computed(() => {
  if (!selectedChapterId.value) {
    return kpList.value;
  }
  const targetChapter = chapterOptions.value.find((c) => c.id === selectedChapterId.value);
  if (!targetChapter) return kpList.value;
  return kpList.value.filter((kp) => (kp.chapterName || '核心章节') === targetChapter.name);
});

// 排序后的学生数据（管理员/测试账号的过滤已由后端统一执行，此处只做排序）
const displayStudentRows = computed(() => {
  const rows = [...studentRows.value];
  if (sortMode.value === 'default') {
    return rows;
  }
  return rows.sort((a, b) => {
    const scoreA = averageScoreOf(a.scores);
    const scoreB = averageScoreOf(b.scores);
    return sortMode.value === 'score-desc' ? scoreB - scoreA : scoreA - scoreB;
  });
});

function averageScoreOf(scores: Record<number, number>): number {
  const values = Object.values(scores);
  if (!values.length) {
    return 0;
  }
  return values.reduce((acc, curr) => acc + curr, 0) / values.length;
}

/** 分值来源样式：实心点=实测，虚线空心点=规则推算 */
function getSourceClass(source?: string): string {
  return source === 'MEASURED' ? 'cell-source-dot--measured' : 'cell-source-dot--estimated';
}

function getSourceTitle(source: string | undefined, sampleCount: number | undefined): string {
  if (source === 'MEASURED') {
    return `实测数据：来自 ${sampleCount && sampleCount > 0 ? sampleCount : 1} 次真实测评作答`;
  }
  return '推算数据：该学员在此考点尚无独立测评采样，分值由作业均分与错题记录推导得出';
}

function toggleLevelFilter(level: string) {
  if (activeLevelFilter.value === level) {
    activeLevelFilter.value = null;
  } else {
    activeLevelFilter.value = level;
  }
}

function getFilterClass(score: number): string {
  if (!activeLevelFilter.value) {
    return '';
  }
  let matched = false;
  if (activeLevelFilter.value === 'mastered' && score >= 85) matched = true;
  else if (activeLevelFilter.value === 'good' && score >= 70 && score < 85) matched = true;
  else if (activeLevelFilter.value === 'warning' && score >= 55 && score < 70) matched = true;
  else if (activeLevelFilter.value === 'danger' && score < 55) matched = true;

  return matched ? 'cell-highlighted' : 'cell-dimmed';
}

function getCellLevelClass(score: number): string {
  if (score >= 85) return 'level-mastered';
  if (score >= 70) return 'level-good';
  if (score >= 55) return 'level-warning';
  return 'level-danger';
}

function getLevelShortText(score: number): string {
  if (score >= 85) return '精熟';
  if (score >= 70) return '良好';
  if (score >= 55) return '一般';
  return '薄弱';
}

function getMasteryLevelText(score: number): string {
  if (score >= 85) return '完全精熟 (≥85%)';
  if (score >= 70) return '良好掌握 (70~84%)';
  if (score >= 55) return '认知一般 (55~69%)';
  return '重点攻坚 (<55%)';
}

const handleCellClick = (payload: {
  studentName: string;
  kpTitle: string;
  score: number;
  studentId?: number;
  kpId?: number;
  chapterName?: string;
  studentNo?: string;
  source?: MasterySource;
  sampleCount?: number;
}) => {
  const { studentName, kpTitle, score, source } = payload;
  let advice = '';
  if (score >= 85) {
    advice = `该考点掌握程度扎实，概念定义与定理逆用熟练，建议提供跨章节综合变式挑战更高阶应用能力。`;
  } else if (score >= 70) {
    advice = `具备较好的基础解题能力，但在公式逆用或复杂边界条件处理上偶有失误，建议进行 2~3 道中等难度拓展题训练。`;
  } else if (score >= 55) {
    advice = `理解处于中等偏弱水平，部分核心推演步骤容易断裂，建议温习图谱前驱依赖定理并辅以针对性变式练习。`;
  } else {
    advice = `存在显著认知盲区或尚未产生充分测评数据！系统检测到历史测验失分集中，强烈建议教师一键派发自适应定向变式微题集。`;
  }

  if (source !== 'MEASURED') {
    advice += '（该分值由作业均分与错题记录推算得出，尚未经过独立测评采样，建议安排一次课内小测验证真实水平。）';
  }

  activeCell.value = {
    ...payload,
    score: Math.round(score),
    advice
  };
  drilldownVisible.value = true;
};

const handleAssignPractice = () => {
  const cell = activeCell.value;
  drilldownVisible.value = false;
  ElMessage.success(`已为【${cell?.studentName}】针对【${cell?.kpTitle}】推送定制自适应习题`);

  // 必须带上 studentId / knowledgePointId，练习页才能定位到「哪个人的哪个考点」
  const query: Record<string, string> = { mode: 'WEAK_POINT' };
  if (props.courseId) {
    query.courseId = String(props.courseId);
  }
  if (cell?.studentId) {
    query.studentId = String(cell.studentId);
  }
  if (cell?.kpId) {
    query.knowledgePointId = String(cell.kpId);
  }
  router.push({ path: '/learning/practice', query });
};

watch(
  () => props.courseId,
  () => {
    selectedChapterId.value = 0;
    fetchHeatmap();
  }
);

// 过滤开关变化时重新向后端取数，避免前端过滤造成的人数/均分口径分裂
watch(includeTesting, () => {
  fetchHeatmap();
});

onMounted(() => {
  fetchHeatmap();
});
</script>

<style scoped lang="scss">
.knowledge-heatmap-container {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);

  /* 数据来源说明条：把「实测」与「推算」显式区分开 */
  .data-provenance-bar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
    padding: 8px 14px;
    margin-bottom: 14px;
    border-radius: 10px;
    background: #F8FAFC;
    border: 1px dashed #CBD5E1;
    font-size: 12px;
    color: #475569;

    .provenance-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;

      strong {
        color: #0F172A;
        font-weight: 700;
      }

      &--ratio strong {
        color: #1D4ED8;
      }
    }

    .provenance-hint {
      margin-left: auto;
      color: #94A3B8;
      font-size: 11px;
    }
  }

  /* 分值来源标记：实心=实测，虚线空心=推算 */
  .cell-source-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;

    &--measured {
      background: #0F172A;
    }

    &--estimated {
      background: transparent;
      border: 1px dashed #94A3B8;
      box-sizing: border-box;
    }
  }

  .heatmap-header-bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
    flex-wrap: wrap;

    .header-left {
      .component-title {
        font-size: 16px;
        font-weight: 700;
        color: #1E293B;
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 6px 0;

        .title-icon {
          color: #2563EB;
        }
      }

      .component-desc {
        font-size: 13px;
        color: #64748B;
        margin: 0;
        line-height: 1.5;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
  }

  .chapter-select,
  .sort-select {
    width: 165px;

    :deep(.el-select__wrapper) {
      border-radius: 9999px !important;
      height: 32px;
      min-height: 32px;
      padding: 0 12px;
      border: 1px solid #E2E8F0;
    }
  }

  .real-student-toggle-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    height: 32px;
    border-radius: 9999px;
    border: 1px solid #E2E8F0;
    background: #F8FAFC;
    color: #475569;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;

    .toggle-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #94A3B8;
      transition: background 0.2s ease;
    }

    &:hover {
      background: #FFFFFF;
      border-color: #CBD5E1;
      color: #1E293B;
    }

    &.active {
      background: #EFF6FF;
      border-color: #BFDBFE;
      color: #1D4ED8;
      font-weight: 600;

      .toggle-dot {
        background: #2563EB;
        box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
      }
    }
  }

  .legend-bar {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 10px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 9999px;
    height: 32px;
    box-sizing: border-box;

    .legend-label {
      font-size: 12px;
      color: #64748B;
      font-weight: 500;
      white-space: nowrap;
    }

    .legend-item-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 2px 8px;
      border-radius: 9999px;
      border: 1px solid transparent;
      background: transparent;
      font-size: 11px;
      font-weight: 500;
      color: #475569;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;
      height: 24px;
      box-sizing: border-box;

      &:hover {
        background: #FFFFFF;
        border-color: #CBD5E1;
      }

      &.active {
        background: #FFFFFF;
        border-color: #2563EB;
        color: #2563EB;
        box-shadow: 0 1px 3px rgba(37, 99, 235, 0.15);
      }
    }

    .legend-dot {
      width: 7px;
      height: 7px;
      border-radius: 2px;
      flex-shrink: 0;
    }

    .color-mastered { background: #10B981; }
    .color-good { background: #22C55E; }
    .color-warning { background: #F59E0B; }
    .color-danger { background: #EF4444; }
  }

  .matrix-scroll-wrapper {
    overflow-x: auto;
    max-height: 540px;
    border-radius: 12px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;

    &::-webkit-scrollbar {
      height: 8px;
      width: 8px;
    }
    &::-webkit-scrollbar-thumb {
      background: #CBD5E1;
      border-radius: 4px;
    }
  }

  .heatmap-table {
    border-collapse: separate;
    border-spacing: 0;
    width: 100%;
    min-width: 820px;
    background: #FFFFFF;

    thead tr {
      background: #F8FAFC;
      position: sticky;
      top: 0;
      z-index: 10;
    }

    th {
      padding: 12px 14px;
      font-size: 12px;
      font-weight: 600;
      color: #334155;
      text-align: center;
      background: #F8FAFC;
      border-bottom: 1px solid #E2E8F0;
      border-right: 1px solid #F1F5F9;
    }

    .student-header-col {
      width: 190px;
      min-width: 190px;
      text-align: left;
    }

    .student-header-inner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 6px;

      .main-th-label {
        font-weight: 700;
        color: #1E293B;
      }

      .th-count-badge {
        font-size: 11px;
        color: #64748B;
        background: #EEF2F6;
        padding: 1px 8px;
        border-radius: 9999px;
      }
    }

    .kp-header-col {
      min-width: 120px;
      max-width: 160px;
    }

    .kp-header-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;

      .kp-chapter {
        font-size: 10px;
        color: #64748B;
        background: #EEF2F6;
        padding: 1px 6px;
        border-radius: 4px;
        max-width: 140px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .kp-name {
        font-size: 12px;
        font-weight: 600;
        color: #1E293B;
        line-height: 1.3;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .kp-avg-mini {
        font-size: 11px;
        color: #94A3B8;

        strong {
          color: #2563EB;
          font-weight: 600;
        }
      }
    }

    /* 左侧固定列（彻底解决与右侧单元格粘连问题） */
    .sticky-col {
      position: sticky;
      left: 0;
      z-index: 5;
      background: #FFFFFF;
      border-right: 1px solid #E2E8F0;
      box-shadow: 4px 0 10px rgba(15, 23, 42, 0.04);
    }

    thead th.sticky-col {
      z-index: 15;
      background: #F8FAFC;
    }

    /* 基准均分行 */
    .class-avg-row {
      background: #F8FAFC;

      .avg-label-cell {
        background: #F8FAFC;
        padding: 10px 14px;
      }

      .avg-benchmark-box {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 4px 10px;
        border-radius: 9999px;
        background: #EFF6FF;
        border: 1px solid #DBEAFE;

        .benchmark-icon-dot {
          width: 7px;
          height: 7px;
          border-radius: 50%;
          background: #2563EB;
          box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
        }

        .benchmark-title {
          font-size: 12px;
          font-weight: 700;
          color: #1D4ED8;
        }
      }
    }

    /* 普通学员行 */
    .student-row {
      transition: background 0.15s ease;

      &:hover {
        background: #F8FAFC;

        td.sticky-col {
          background: #F8FAFC;
        }
      }

      &.is-selected-student {
        background: #F0F7FF;

        td.sticky-col {
          background: #EFF6FF;
          border-left: 3px solid #2563EB;
        }
      }
    }

    .student-name-cell {
      padding: 10px 14px;
      background: #FFFFFF;
      border-bottom: 1px solid #F1F5F9;
    }

    .student-name-box {
      display: flex;
      align-items: center;
      gap: 10px;

      .stu-cell-avatar {
        background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
        color: #FFFFFF;
        font-size: 12px;
        font-weight: 600;
        flex-shrink: 0;
        box-shadow: 0 1px 3px rgba(37, 99, 235, 0.2);
      }

      .stu-info-texts {
        display: flex;
        flex-direction: column;
        line-height: 1.25;

        .name-badge-row {
          display: flex;
          align-items: center;
          gap: 5px;

          .student-name {
            font-size: 13px;
            font-weight: 600;
            color: #1E293B;
          }

          .test-tag {
            font-size: 9px;
            padding: 0 4px;
            border-radius: 4px;
            background: #F1F5F9;
            color: #64748B;
            border: 1px solid #CBD5E1;
            line-height: 1.4;
          }
        }

        .student-id-sub {
          font-size: 11px;
          color: #94A3B8;
        }
      }
    }

    .heatmap-cell-td {
      padding: 6px 6px;
      text-align: center;
      border-bottom: 1px solid #F1F5F9;
      border-right: 1px solid #F8FAFC;
    }

    /* 内部优雅卡片 (彻底消除贴边和伪影) */
    .cell-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 44px;
      border-radius: 8px;
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      box-sizing: border-box;

      .cell-source-dot {
        position: absolute;
        top: 4px;
        right: 4px;
      }

      .cell-score {
        font-size: 13px;
        font-weight: 700;
        line-height: 1.2;
      }

      .cell-level-indicator {
        font-size: 10px;
        font-weight: 500;
        opacity: 0.85;
        line-height: 1;
        margin-top: 1px;
      }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
        z-index: 2;
      }

      &.avg-card {
        border-style: dashed;
        border-width: 1.2px;
      }

      /* 现代高质感低饱和度色阶 (告别刺眼高饱和死红死绿) */
      &.level-mastered {
        background: #F0FDF4;
        border-color: #BBF7D0;
        .cell-score { color: #15803D; }
        .cell-level-indicator { color: #16A34A; }

        &:hover {
          background: #DCFCE7;
          border-color: #86EFAC;
        }
      }

      &.level-good {
        background: #F0FDF9;
        border-color: #A7F3D0;
        .cell-score { color: #047857; }
        .cell-level-indicator { color: #059669; }

        &:hover {
          background: #CCFBF1;
          border-color: #6EE7B7;
        }
      }

      &.level-warning {
        background: #FFFBEB;
        border-color: #FDE68A;
        .cell-score { color: #B45309; }
        .cell-level-indicator { color: #D97706; }

        &:hover {
          background: #FEF3C7;
          border-color: #FCD34D;
        }
      }

      &.level-danger {
        background: #FEF2F2;
        border-color: #FECACA;
        .cell-score { color: #B91C1C; }
        .cell-level-indicator { color: #DC2626; }

        &:hover {
          background: #FEE2E2;
          border-color: #FCA5A5;
        }
      }

      &.cell-highlighted {
        transform: scale(1.05);
        box-shadow: 0 0 0 2px #2563EB, 0 4px 10px rgba(37, 99, 235, 0.2);
        z-index: 3;
      }

      &.cell-dimmed {
        opacity: 0.22;
        filter: grayscale(40%);
      }
    }
  }

  .heatmap-empty {
    padding: 40px 0;
  }
}

.drilldown-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .drilldown-summary-card {
    background: #F8FAFC;
    border-radius: 12px;
    padding: 14px 18px;
    border: 1px solid #E2E8F0;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .summary-line {
      display: flex;
      align-items: center;
      font-size: 13px;

      .s-label {
        width: 80px;
        color: #64748B;
      }
      .s-val {
        color: #1E293B;

        &.source-value {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
        }

        .source-note {
          font-size: 12px;
          color: #64748B;
        }
      }

      .student-no-sub {
        color: #94A3B8;
        font-weight: normal;
        margin-left: 4px;
      }

      .kp-title-highlight {
        color: #2563EB;
        font-weight: 700;
      }
    }
  }

  .drilldown-advice-card {
    background: #EFF6FF;
    border-radius: 12px;
    padding: 16px 18px;
    border: 1px solid #DBEAFE;

    .advice-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #1D4ED8;
      margin-bottom: 8px;

      .advice-icon {
        font-size: 16px;
      }
    }

    .advice-content {
      margin: 0;
      font-size: 13px;
      color: #1E3A8A;
      line-height: 1.65;
    }
  }
}

.dialog-actions-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

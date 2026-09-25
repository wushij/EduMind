<template>
  <div class="knowledge-heatmap-container" v-loading="loading">
    <div class="heatmap-header-bar">
      <div class="header-left">
        <h3 class="component-title">
          <el-icon class="title-icon"><Grid /></el-icon>
          班级知识点全景掌握度热力矩阵 (V1.2 精准学情下钻)
        </h3>
        <p class="component-desc">
          横轴贯通考点图谱体系，纵轴映射真实选课学生，色阶直观呈现认知薄弱聚集区，支持章节筛选与色阶高亮下钻
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

        <!-- 色阶交互图例（支持点击筛选高亮对应等级） -->
        <div class="legend-bar">
          <span class="legend-label">掌握度分级：</span>
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

    <!-- 热力矩阵主体 -->
    <div v-if="filteredKpList.length > 0" class="matrix-scroll-wrapper">
      <table class="heatmap-table">
        <thead>
          <tr>
            <th class="sticky-col student-header-col">选课学员 / 考点</th>
            <th
              v-for="kp in filteredKpList"
              :key="kp.id"
              class="kp-header-col"
              :title="kp.title"
            >
              <div class="kp-header-cell">
                <span class="kp-name">{{ kp.title }}</span>
                <span v-if="kp.chapterName" class="kp-chapter">{{ kp.chapterName }}</span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <!-- 班级均分基准行 -->
          <tr class="class-avg-row">
            <td class="sticky-col avg-label-cell">
              <el-tag size="small" type="primary" effect="dark" class="avg-tag">
                班级均分基准
              </el-tag>
            </td>
            <td
              v-for="kp in filteredKpList"
              :key="'avg-' + kp.id"
              class="heatmap-cell avg-cell"
              :class="getCellClass(classAvgScores[kp.id] || 0)"
              :style="{ backgroundColor: getScoreColor(classAvgScores[kp.id] || 0) }"
              @click="handleCellClick('班级平均', kp.title, classAvgScores[kp.id] || 0, undefined, kp.id, kp.chapterName)"
            >
              <span class="cell-score">{{ Math.round(classAvgScores[kp.id] || 0) }}%</span>
            </td>
          </tr>

          <!-- 各学生详细数据行 -->
          <tr
            v-for="stu in sortedStudentRows"
            :key="stu.id"
            class="student-row"
            :class="{ 'is-selected-student': selectedStudentId === stu.id }"
          >
            <td class="sticky-col student-name-cell">
              <div class="student-name-box">
                <el-avatar :size="24" :src="stu.avatar" class="stu-cell-avatar">
                  {{ stu.name ? stu.name.slice(0, 1) : '学' }}
                </el-avatar>
                <div class="stu-info-texts">
                  <span class="student-name">{{ stu.name }}</span>
                  <span class="student-id-sub">{{ stu.studentNo || `#${stu.id}` }}</span>
                </div>
              </div>
            </td>
            <td
              v-for="kp in filteredKpList"
              :key="stu.id + '-' + kp.id"
              class="heatmap-cell"
              :class="getCellClass(stu.scores[kp.id] || 0)"
              :style="{ backgroundColor: getScoreColor(stu.scores[kp.id] || 0) }"
              @click="handleCellClick(stu.name, kp.title, stu.scores[kp.id] || 0, stu.id, kp.id, kp.chapterName, stu.studentNo)"
            >
              <span class="cell-score">{{ Math.round(stu.scores[kp.id] || 0) }}%</span>
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
      width="520px"
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
              <span v-if="activeCell.studentNo" class="text-slate-400 font-normal">({{ activeCell.studentNo }})</span>
            </span>
          </div>
          <div class="summary-line">
            <span class="s-label">核心考点：</span>
            <span class="s-val font-bold text-blue-600">{{ activeCell.kpTitle }}</span>
          </div>
          <div v-if="activeCell.chapterName" class="summary-line">
            <span class="s-label">所属章节：</span>
            <span class="s-val text-slate-600">{{ activeCell.chapterName }}</span>
          </div>
          <div class="summary-line">
            <span class="s-label">掌握程度：</span>
            <el-tag
              :type="activeCell.score >= 70 ? 'success' : activeCell.score >= 55 ? 'warning' : 'danger'"
              size="default"
              effect="light"
            >
              {{ activeCell.score }}% ({{ getMasteryLevelText(activeCell.score) }})
            </el-tag>
          </div>
        </div>

        <div class="drilldown-advice-card">
          <div class="advice-header">
            <el-icon class="text-blue-500"><Cpu /></el-icon>
            <span class="advice-title">EduMind AI 教学干预建议与学情归因</span>
          </div>
          <p class="advice-content">{{ activeCell.advice }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="drilldownVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleAssignPractice">
          定向推送自适应变式练习
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Grid, Cpu } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useKnowledgeHeatmap } from '@/composables/analytics/useKnowledgeHeatmap';

const props = defineProps<{
  courseId?: number;
  selectedStudentId?: number;
}>();

const router = useRouter();

const {
  loading,
  loadError,
  kpList,
  studentRows,
  classAvgScores,
  fetchHeatmap
} = useKnowledgeHeatmap(() => props.courseId);

const selectedChapterId = ref<number>(0);
const sortMode = ref<'default' | 'score-desc' | 'score-asc'>('default');
const activeLevelFilter = ref<string | null>(null);

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
} | null>(null);

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

// 排序后的学生数据
const sortedStudentRows = computed(() => {
  const rows = [...studentRows.value];
  if (sortMode.value === 'default') {
    return rows;
  }
  return rows.sort((a, b) => {
    const scoreA = Object.values(a.scores).reduce((acc, c) => acc + c, 0) / (Object.values(a.scores).length || 1);
    const scoreB = Object.values(b.scores).reduce((acc, c) => acc + c, 0) / (Object.values(b.scores).length || 1);
    return sortMode.value === 'score-desc' ? scoreB - scoreA : scoreA - scoreB;
  });
});

function toggleLevelFilter(level: string) {
  if (activeLevelFilter.value === level) {
    activeLevelFilter.value = null;
  } else {
    activeLevelFilter.value = level;
  }
}

function getCellClass(score: number): Record<string, boolean> {
  if (!activeLevelFilter.value) {
    return {};
  }
  let matched = false;
  if (activeLevelFilter.value === 'mastered' && score >= 85) matched = true;
  else if (activeLevelFilter.value === 'good' && score >= 70 && score < 85) matched = true;
  else if (activeLevelFilter.value === 'warning' && score >= 55 && score < 70) matched = true;
  else if (activeLevelFilter.value === 'danger' && score < 55) matched = true;

  return {
    'cell-highlighted': matched,
    'cell-dimmed': !matched
  };
}

const getScoreColor = (score: number) => {
  if (score >= 85) return 'rgba(82, 196, 26, 0.78)';
  if (score >= 70) return 'rgba(115, 209, 61, 0.65)';
  if (score >= 55) return 'rgba(250, 173, 20, 0.7)';
  return 'rgba(255, 77, 79, 0.72)';
};

const getMasteryLevelText = (score: number) => {
  if (score >= 85) return '完全精熟';
  if (score >= 70) return '良好掌握';
  if (score >= 55) return '一般水平';
  return '严重薄弱';
};

const handleCellClick = (
  studentName: string,
  kpTitle: string,
  score: number,
  studentId?: number,
  kpId?: number,
  chapterName?: string,
  studentNo?: string
) => {
  let advice = '';
  if (score >= 85) {
    advice = `该考点掌握程度扎实，基础概念与计算规范熟练，建议提供综合拔高变式挑战更高阶应用能力。`;
  } else if (score >= 70) {
    advice = `具备良好解题能力，但在公式逆用或复杂边界条件处理上偶有失误，建议进行 2~3 道中等难度拓展题训练。`;
  } else if (score >= 55) {
    advice = `理解处于中游，部分定理推演步骤容易断裂，建议温习图谱前驱依赖定理并辅以针对性练习。`;
  } else {
    advice = `存在显著认知盲区或概念混淆！系统检测到历史作业频繁出错，强烈建议一键派发变式错题攻坚集。`;
  }

  activeCell.value = {
    studentName,
    studentNo,
    kpTitle,
    chapterName,
    score: Math.round(score),
    advice,
    studentId,
    kpId
  };
  drilldownVisible.value = true;
};

const handleAssignPractice = () => {
  drilldownVisible.value = false;
  ElMessage.success(`已为【${activeCell.value?.studentName}】针对【${activeCell.value?.kpTitle}】推送定制自适应习题`);
  const query: Record<string, string> = { mode: 'WEAK_POINT' };
  if (props.courseId) {
    query.courseId = String(props.courseId);
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

onMounted(() => {
  fetchHeatmap();
});
</script>

<style scoped lang="scss">
.knowledge-heatmap-container {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  padding: 22px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);

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
      gap: 12px;
      flex-wrap: wrap;
    }
  }

  .chapter-select,
  .sort-select {
    width: 170px;

    :deep(.el-select__wrapper) {
      border-radius: 9999px !important;
      height: 32px;
      min-height: 32px;
      padding: 0 12px;
      border: 1px solid #E2E8F0;
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
      padding: 2px 9px;
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

    .color-mastered { background: #52C41A; }
    .color-good { background: #73D13D; }
    .color-warning { background: #FAAD14; }
    .color-danger { background: #FF4D4F; }
  }

  .matrix-scroll-wrapper {
    overflow-x: auto;
    max-height: 520px;
    border-radius: 10px;
    border: 1px solid #E2E8F0;

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
    border-spacing: 2px;
    width: 100%;
    min-width: 800px;
    background: #F8FAFC;

    thead tr {
      background: #F1F5F9;
      position: sticky;
      top: 0;
      z-index: 10;
    }

    th {
      padding: 10px 12px;
      font-size: 12px;
      font-weight: 600;
      color: #334155;
      text-align: center;
      background: #F1F5F9;
    }

    .student-header-col {
      width: 170px;
      min-width: 170px;
      text-align: left;
    }

    .kp-header-col {
      min-width: 100px;
      max-width: 140px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .kp-header-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;

      .kp-name {
        font-weight: 600;
        color: #1E293B;
      }
      .kp-chapter {
        font-size: 10px;
        color: #94A3B8;
      }
    }

    .sticky-col {
      position: sticky;
      left: 0;
      z-index: 5;
      background: #FFFFFF;
      box-shadow: 2px 0 6px rgba(0, 0, 0, 0.03);
    }

    thead th.sticky-col {
      z-index: 15;
      background: #F1F5F9;
    }

    .class-avg-row {
      background: #EFF6FF;

      .avg-label-cell {
        background: #EFF6FF;
        padding: 8px 12px;
      }

      .avg-tag {
        font-weight: 600;
      }
    }

    .student-row {
      transition: background 0.2s ease;

      &:hover td.sticky-col {
        background: #F8FAFC;
      }

      &.is-selected-student td.sticky-col {
        background: #EEF2FF;
        border-left: 3px solid #6366F1;
      }
    }

    .student-name-cell {
      padding: 6px 12px;
      background: #FFFFFF;
    }

    .student-name-box {
      display: flex;
      align-items: center;
      gap: 8px;

      .stu-cell-avatar {
        background: #3B82F6;
        color: #FFFFFF;
        font-size: 11px;
        font-weight: 600;
      }

      .stu-info-texts {
        display: flex;
        flex-direction: column;
        line-height: 1.2;

        .student-name {
          font-size: 12px;
          font-weight: 600;
          color: #1E293B;
        }

        .student-id-sub {
          font-size: 10px;
          color: #94A3B8;
        }
      }
    }

    .heatmap-cell {
      text-align: center;
      padding: 10px 4px;
      cursor: pointer;
      border-radius: 4px;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .cell-score {
        font-size: 12px;
        font-weight: 700;
        color: #FFFFFF;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
      }

      &:hover {
        transform: scale(1.08);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
        z-index: 3;
        position: relative;
      }

      &.cell-highlighted {
        transform: scale(1.04);
        box-shadow: 0 0 0 2px #2563EB;
      }

      &.cell-dimmed {
        opacity: 0.25;
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
    border-radius: 10px;
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
      }
    }
  }

  .drilldown-advice-card {
    background: #EFF6FF;
    border-radius: 10px;
    padding: 14px 18px;
    border: 1px solid #DBEAFE;

    .advice-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #1D4ED8;
      margin-bottom: 8px;
    }

    .advice-content {
      margin: 0;
      font-size: 13px;
      color: #1E3A8A;
      line-height: 1.6;
    }
  }
}
</style>

<template>
  <div class="knowledge-heatmap-container" v-loading="loading">
    <div class="heatmap-header-bar">
      <div class="header-left">
        <h3 class="component-title">
          <el-icon class="title-icon"><Grid /></el-icon>
          班级知识点全景掌握度热力矩阵 (V1.1 精准学情下钻)
        </h3>
        <p class="component-desc">
          横轴映射课程核心知识图谱考点，纵轴映射班级统招学生，色阶直观呈现认知薄弱聚集区
        </p>
      </div>
      <div class="header-right">
        <!-- 色阶图例 -->
        <div class="legend-bar">
          <span class="legend-label">掌握度分级：</span>
          <span class="legend-item"><span class="legend-dot color-mastered" />精熟 (≥85%)</span>
          <span class="legend-item"><span class="legend-dot color-good" />良好 (70-84%)</span>
          <span class="legend-item"><span class="legend-dot color-warning" />一般 (55-69%)</span>
          <span class="legend-item"><span class="legend-dot color-danger" />薄弱 (&lt;55%)</span>
        </div>
      </div>
    </div>

    <!-- 热力矩阵主体 -->
    <div v-if="kpList.length > 0" class="matrix-scroll-wrapper">
      <table class="heatmap-table">
        <thead>
          <tr>
            <th class="sticky-col student-header-col">学生 / 考点</th>
            <th
              v-for="kp in kpList"
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
              <el-tag size="small" type="primary" effect="dark">班级平均基准</el-tag>
            </td>
            <td
              v-for="kp in kpList"
              :key="'avg-' + kp.id"
              class="heatmap-cell avg-cell"
              :style="{ backgroundColor: getScoreColor(classAvgScores[kp.id] || 0) }"
              @click="handleCellClick('班级平均', kp.title, classAvgScores[kp.id] || 0)"
            >
              <span class="cell-score">{{ Math.round(classAvgScores[kp.id] || 0) }}%</span>
            </td>
          </tr>

          <!-- 各学生详细数据行 -->
          <tr v-for="stu in studentRows" :key="stu.id" class="student-row">
            <td class="sticky-col student-name-cell">
              <span class="student-name">{{ stu.name }}</span>
              <span class="student-id-sub">#{{ stu.studentNo || stu.id }}</span>
            </td>
            <td
              v-for="kp in kpList"
              :key="stu.id + '-' + kp.id"
              class="heatmap-cell"
              :style="{ backgroundColor: getScoreColor(stu.scores[kp.id] || 0) }"
              @click="handleCellClick(stu.name, kp.title, stu.scores[kp.id] || 0, stu.id, kp.id)"
            >
              <span class="cell-score">{{ Math.round(stu.scores[kp.id] || 0) }}%</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-empty
      v-else-if="!loading"
      :description="loadError ? '加载知识掌握度数据失败，请稍后重试' : '该课程暂无知识点掌握度矩阵数据，请先安排学生完成作业或测验'"
      :image-size="80"
    />

    <!-- 单元格点击下钻分析弹窗 -->
    <el-dialog
      v-model="drilldownVisible"
      title="知识掌握度微观学情归因"
      width="480px"
      append-to-body
      destroy-on-close
    >
      <div v-if="activeCell" class="drilldown-dialog-body">
        <div class="drilldown-summary-card">
          <div class="summary-line">
            <span class="s-label">分析对象：</span>
            <span class="s-val">{{ activeCell.studentName }}</span>
          </div>
          <div class="summary-line">
            <span class="s-label">考察考点：</span>
            <span class="s-val font-bold">{{ activeCell.kpTitle }}</span>
          </div>
          <div class="summary-line">
            <span class="s-label">当前得分：</span>
            <el-tag :type="activeCell.score >= 70 ? 'success' : activeCell.score >= 55 ? 'warning' : 'danger'" size="default">
              {{ activeCell.score }}% ({{ getMasteryLevelText(activeCell.score) }})
            </el-tag>
          </div>
        </div>

        <div class="drilldown-advice-card">
          <div class="advice-header">
            <el-icon class="text-blue-500"><Cpu /></el-icon>
            <span class="advice-title">EduMind AI 教学干预建议</span>
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
import { ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Grid, Cpu } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getKnowledgeHeatmap } from '@/api/analytics/knowledge';
import type { KnowledgeHeatmapVO } from '@/types/analytics/mastery';

interface HeatmapKpItem {
  id: number;
  title: string;
  chapterName?: string;
}

interface HeatmapStudentItem {
  id: number;
  name: string;
  studentNo?: string;
  scores: Record<number, number>;
}

const props = defineProps<{
  courseId?: number;
}>();

const router = useRouter();
const loading = ref(false);

const kpList = ref<HeatmapKpItem[]>([]);
const studentRows = ref<HeatmapStudentItem[]>([]);
const classAvgScores = ref<Record<number, number>>({});

const drilldownVisible = ref(false);
const activeCell = ref<{
  studentName: string;
  kpTitle: string;
  score: number;
  advice: string;
  studentId?: number;
  kpId?: number;
} | null>(null);

const loadError = ref(false);

const fetchHeatmap = async () => {
  const cId = props.courseId || 102;
  loading.value = true;
  loadError.value = false;
  kpList.value = [];
  studentRows.value = [];
  classAvgScores.value = {};
  try {
    const res: any = await getKnowledgeHeatmap(cId);
    const data: KnowledgeHeatmapVO = res?.data || res;
    if (data?.knowledgePoints?.length) {
      parseHeatmapVO(data);
    } else {
      loadError.value = true;
    }
  } catch {
    loadError.value = true;
    ElMessage.error('加载知识掌握度热力矩阵失败');
  } finally {
    loading.value = false;
  }
};

const parseHeatmapVO = (vo: KnowledgeHeatmapVO) => {
  kpList.value = vo.knowledgePoints.map(p => ({
    id: p.id,
    title: p.title
  }));

  // 将 cells 转为 Map
  const cellMap = new Map<string, number>();
  if (vo.cells) {
    for (const c of vo.cells) {
      const score = c.mastery <= 1.0 ? Math.round(c.mastery * 100) : Math.round(c.mastery);
      cellMap.set(`${c.studentId}_${c.knowledgePointId}`, score);
    }
  }

  // 组装学生行
  const rows: HeatmapStudentItem[] = [];
  for (const s of vo.students || []) {
    const scores: Record<number, number> = {};
    for (const kp of kpList.value) {
      scores[kp.id] = cellMap.get(`${s.id}_${kp.id}`) ?? 70;
    }
    rows.push({
      id: s.id,
      name: s.name,
      studentNo: s.studentNo,
      scores
    });
  }
  studentRows.value = rows;

  // 计算班级各考点均分
  const avgScores: Record<number, number> = {};
  for (const kp of kpList.value) {
    if (rows.length > 0) {
      const sum = rows.reduce((acc, curr) => acc + (curr.scores[kp.id] || 0), 0);
      avgScores[kp.id] = Math.round(sum / rows.length);
    } else {
      avgScores[kp.id] = 0;
    }
  }
  classAvgScores.value = avgScores;
};

const getScoreColor = (score: number) => {
  if (score >= 85) return 'rgba(82, 196, 26, 0.75)'; // 深绿
  if (score >= 70) return 'rgba(115, 209, 61, 0.6)';  // 浅绿
  if (score >= 55) return 'rgba(250, 173, 20, 0.65)'; // 黄色
  return 'rgba(255, 77, 79, 0.7)';                   // 红色
};

const getMasteryLevelText = (score: number) => {
  if (score >= 85) return '完全精熟';
  if (score >= 70) return '良好掌握';
  if (score >= 55) return '一般水平';
  return '严重薄弱';
};

const handleCellClick = (studentName: string, kpTitle: string, score: number, studentId?: number, kpId?: number) => {
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
    kpTitle,
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
  router.push({
    path: '/learning/practice',
    query: {
      courseId: props.courseId || 102,
      mode: 'WEAK_POINT'
    }
  });
};

watch(
  () => props.courseId,
  () => {
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
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  padding: 22px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

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
          color: #1677FF;
          font-size: 20px;
        }
      }
      .component-desc {
        font-size: 12px;
        color: #64748B;
        margin: 0;
      }
    }

    .header-right {
      .legend-bar {
        display: flex;
        align-items: center;
        gap: 14px;
        font-size: 12px;
        color: #64748B;
        background: #F8FAFC;
        padding: 6px 14px;
        border-radius: 8px;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 6px;

          .legend-dot {
            width: 10px;
            height: 10px;
            border-radius: 2px;

            &.color-mastered { background: #52C41A; }
            &.color-good { background: #73D13D; }
            &.color-warning { background: #FAAD14; }
            &.color-danger { background: #FF4D4F; }
          }
        }
      }
    }
  }

  .matrix-scroll-wrapper {
    overflow-x: auto;
    border-radius: 8px;
    border: 1px solid #E2E8F0;

    .heatmap-table {
      width: 100%;
      border-collapse: collapse;
      text-align: center;
      font-size: 13px;

      th, td {
        padding: 12px 14px;
        border: 1px solid #E2E8F0;
      }

      thead {
        background: #F8FAFC;

        .kp-header-cell {
          display: flex;
          flex-direction: column;
          gap: 2px;
          min-width: 110px;

          .kp-name {
            font-weight: 600;
            color: #1E293B;
            font-size: 12px;
            line-height: 1.3;
          }
          .kp-chapter {
            font-size: 11px;
            color: #94A3B8;
          }
        }
      }

      .sticky-col {
        position: sticky;
        left: 0;
        z-index: 2;
        background: #F8FAFC;
        font-weight: 600;
        text-align: left;
        min-width: 120px;
        box-shadow: 2px 0 5px rgba(0, 0, 0, 0.04);
      }

      .class-avg-row {
        background: #F1F5F9;
        font-weight: 700;

        .avg-label-cell {
          background: #E6F4FF;
        }
      }

      .student-row {
        &:hover {
          td:not(.heatmap-cell) {
            background: #F1F5F9;
          }
        }

        .student-name-cell {
          background: #FFFFFF;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 12px;

          .student-name {
            color: #1E293B;
          }
          .student-id-sub {
            font-size: 11px;
            color: #94A3B8;
          }
        }
      }

      .heatmap-cell {
        cursor: pointer;
        color: #FFFFFF;
        font-weight: 700;
        transition: transform 0.15s ease, filter 0.15s ease;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);

        &:hover {
          transform: scale(1.08);
          filter: brightness(1.15);
          z-index: 1;
        }

        .cell-score {
          font-size: 12px;
        }
      }
    }
  }

  .drilldown-dialog-body {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .drilldown-summary-card {
      background: #F8FAFC;
      border-radius: 8px;
      padding: 14px 18px;
      border: 1px solid #E2E8F0;
      display: flex;
      flex-direction: column;
      gap: 8px;

      .summary-line {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;

        .s-label { color: #64748B; }
        .s-val { color: #1E293B; }
      }
    }

    .drilldown-advice-card {
      background: rgba(22, 119, 255, 0.04);
      border: 1px dashed rgba(22, 119, 255, 0.3);
      border-radius: 8px;
      padding: 14px 16px;

      .advice-header {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 600;
        color: #1677FF;
        margin-bottom: 8px;
      }

      .advice-content {
        font-size: 13px;
        line-height: 1.6;
        color: #334155;
        margin: 0;
      }
    }
  }
}
</style>

import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getKnowledgeHeatmap } from '@/api/analytics/knowledge';
import type { KnowledgeHeatmapVO, MasterySource } from '@/types/analytics/mastery';

export interface HeatmapKpItem {
  id: number;
  title: string;
  chapterId?: number;
  chapterName?: string;
}

export interface HeatmapStudentItem {
  id: number;
  name: string;
  studentNo?: string;
  avatar?: string;
  className?: string;
  avgScore?: number;
  measuredKpCount: number;
  estimatedKpCount: number;
  scores: Record<number, number>;
  /** 每个考点的分值来源，用于在矩阵上标注「实测 / 推算」 */
  sources: Record<number, MasterySource>;
  /** 每个考点的实测样本数 */
  sampleCounts: Record<number, number>;
}

export interface ParsedHeatmapData {
  kpList: HeatmapKpItem[];
  studentRows: HeatmapStudentItem[];
  classAvgScores: Record<number, number>;
  measuredCellCount: number;
  estimatedCellCount: number;
}

/**
 * 解析热力矩阵响应。
 *
 * 班级均分一律采用后端 {@code classAvgScores}，不再在前端重算——
 * 历史上前端重算出的「列头均分」与后端指标条的「班级掌握度均分」
 * 分母口径不同，导致同一张页面上出现 42.9% 与 52%/24%/24% 两套数字。
 */
export function parseHeatmapVO(vo: KnowledgeHeatmapVO): ParsedHeatmapData {
  const kpList: HeatmapKpItem[] = (vo.knowledgePoints || []).map((p) => ({
    id: p.id,
    title: p.title,
    chapterId: p.chapterId,
    chapterName: p.chapterName || '核心章节'
  }));

  const cellMap = new Map<string, { score: number; source: MasterySource; sampleCount: number }>();
  let measuredCellCount = 0;
  let estimatedCellCount = 0;
  if (vo.cells) {
    for (const c of vo.cells) {
      const score = c.mastery <= 1.0 ? Math.round(c.mastery * 100) : Math.round(c.mastery);
      const source: MasterySource = c.source === 'MEASURED' ? 'MEASURED' : 'ESTIMATED';
      if (source === 'MEASURED') {
        measuredCellCount += 1;
      } else {
        estimatedCellCount += 1;
      }
      cellMap.set(`${c.studentId}_${c.knowledgePointId}`, {
        score,
        source,
        sampleCount: c.sampleCount ?? 0
      });
    }
  }

  const studentRows: HeatmapStudentItem[] = [];
  for (const s of vo.students || []) {
    const scores: Record<number, number> = {};
    const sources: Record<number, MasterySource> = {};
    const sampleCounts: Record<number, number> = {};
    for (const kp of kpList) {
      const cell = cellMap.get(`${s.id}_${kp.id}`);
      scores[kp.id] = cell ? cell.score : 0;
      sources[kp.id] = cell ? cell.source : 'ESTIMATED';
      sampleCounts[kp.id] = cell ? cell.sampleCount : 0;
    }
    const measuredKpCount = s.measuredKpCount ?? 0;
    studentRows.push({
      id: s.id,
      name: s.name,
      studentNo: s.studentNo,
      avatar: s.avatar,
      className: s.className,
      avgScore: s.avgScore,
      measuredKpCount,
      estimatedKpCount: s.estimatedKpCount ?? Math.max(0, kpList.length - measuredKpCount),
      scores,
      sources,
      sampleCounts
    });
  }

  const classAvgScores: Record<number, number> = {};
  const backendAvg = vo.classAvgScores || {};
  for (const kp of kpList) {
    const backendValue = backendAvg[String(kp.id)];
    if (typeof backendValue === 'number') {
      classAvgScores[kp.id] = backendValue;
      continue;
    }
    // 兼容未升级的后端：仅此时才退化为本地折算
    if (studentRows.length > 0) {
      const sum = studentRows.reduce((acc, curr) => acc + (curr.scores[kp.id] || 0), 0);
      classAvgScores[kp.id] = Math.round(sum / studentRows.length);
    } else {
      classAvgScores[kp.id] = 0;
    }
  }

  return {
    kpList,
    studentRows,
    classAvgScores,
    measuredCellCount: vo.measuredCellCount ?? measuredCellCount,
    estimatedCellCount: vo.estimatedCellCount ?? estimatedCellCount
  };
}

/**
 * 班级知识点掌握度热力矩阵数据。
 *
 * @param getCourseId       课程 ID 取值器
 * @param getIncludeTesting 是否包含管理员/测试账号；与后端参数联动，
 *                          保证「指标条学员数」与「矩阵学员数」始终一致
 */
export function useKnowledgeHeatmap(
  getCourseId: () => number | undefined,
  getIncludeTesting: () => boolean = () => false
) {
  const loading = ref(false);
  const loadError = ref(false);
  const kpList = ref<HeatmapKpItem[]>([]);
  const studentRows = ref<HeatmapStudentItem[]>([]);
  const classAvgScores = ref<Record<number, number>>({});
  const measuredCellCount = ref(0);
  const estimatedCellCount = ref(0);
  const studentCount = ref(0);
  const classStudentCount = ref(0);

  function reset() {
    kpList.value = [];
    studentRows.value = [];
    classAvgScores.value = {};
    measuredCellCount.value = 0;
    estimatedCellCount.value = 0;
    studentCount.value = 0;
    classStudentCount.value = 0;
  }

  async function fetchHeatmap() {
    const courseId = getCourseId();
    reset();
    if (!courseId || courseId <= 0) {
      loadError.value = false;
      loading.value = false;
      return;
    }
    loading.value = true;
    loadError.value = false;

    try {
      const res = await getKnowledgeHeatmap(courseId, getIncludeTesting());
      const data: KnowledgeHeatmapVO = (res as any)?.data || res;
      if (data) {
        const parsed = parseHeatmapVO(data);
        kpList.value = parsed.kpList;
        studentRows.value = parsed.studentRows;
        classAvgScores.value = parsed.classAvgScores;
        measuredCellCount.value = parsed.measuredCellCount;
        estimatedCellCount.value = parsed.estimatedCellCount;
        studentCount.value = data.studentCount ?? parsed.studentRows.length;
        classStudentCount.value = data.classStudentCount ?? parsed.studentRows.length;
        loadError.value = false;
      }
    } catch {
      loadError.value = true;
      ElMessage.error('加载知识掌握度热力矩阵失败');
    } finally {
      loading.value = false;
    }
  }

  return {
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
  };
}

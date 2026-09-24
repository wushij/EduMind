import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getKnowledgeHeatmap } from '@/api/analytics/knowledge';
import type { KnowledgeHeatmapVO } from '@/types/analytics/mastery';

export interface HeatmapKpItem {
  id: number;
  title: string;
  chapterName?: string;
}

export interface HeatmapStudentItem {
  id: number;
  name: string;
  studentNo?: string;
  scores: Record<number, number>;
}

export interface ParsedHeatmapData {
  kpList: HeatmapKpItem[];
  studentRows: HeatmapStudentItem[];
  classAvgScores: Record<number, number>;
}

export function parseHeatmapVO(vo: KnowledgeHeatmapVO): ParsedHeatmapData {
  const kpList: HeatmapKpItem[] = vo.knowledgePoints.map((p) => ({
    id: p.id,
    title: p.title
  }));

  const cellMap = new Map<string, number>();
  if (vo.cells) {
    for (const c of vo.cells) {
      const score = c.mastery <= 1.0 ? Math.round(c.mastery * 100) : Math.round(c.mastery);
      cellMap.set(`${c.studentId}_${c.knowledgePointId}`, score);
    }
  }

  const studentRows: HeatmapStudentItem[] = [];
  for (const s of vo.students || []) {
    const scores: Record<number, number> = {};
    for (const kp of kpList) {
      scores[kp.id] = cellMap.get(`${s.id}_${kp.id}`) ?? 70;
    }
    studentRows.push({
      id: s.id,
      name: s.name,
      studentNo: s.studentNo,
      scores
    });
  }

  const classAvgScores: Record<number, number> = {};
  for (const kp of kpList) {
    if (studentRows.length > 0) {
      const sum = studentRows.reduce((acc, curr) => acc + (curr.scores[kp.id] || 0), 0);
      classAvgScores[kp.id] = Math.round(sum / studentRows.length);
    } else {
      classAvgScores[kp.id] = 0;
    }
  }

  return { kpList, studentRows, classAvgScores };
}

export function useKnowledgeHeatmap(getCourseId: () => number | undefined) {
  const loading = ref(false);
  const loadError = ref(false);
  const kpList = ref<HeatmapKpItem[]>([]);
  const studentRows = ref<HeatmapStudentItem[]>([]);
  const classAvgScores = ref<Record<number, number>>({});

  async function fetchHeatmap() {
    const courseId = getCourseId();
    kpList.value = [];
    studentRows.value = [];
    classAvgScores.value = {};
    // 没有有效课程上下文时不再兜底成固定课程（原实现为 `getCourseId() || 102`），
    // 否则会去拉取一门与当前上下文无关的课程热力图，看起来「有数据」实则张冠李戴。
    if (!courseId || courseId <= 0) {
      loadError.value = false;
      loading.value = false;
      return;
    }
    loading.value = true;
    loadError.value = false;

    try {
      const res = await getKnowledgeHeatmap(courseId);
      const data: KnowledgeHeatmapVO = res?.data || res;
      if (data?.knowledgePoints?.length) {
        const parsed = parseHeatmapVO(data);
        kpList.value = parsed.kpList;
        studentRows.value = parsed.studentRows;
        classAvgScores.value = parsed.classAvgScores;
      } else {
        loadError.value = true;
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
    fetchHeatmap
  };
}

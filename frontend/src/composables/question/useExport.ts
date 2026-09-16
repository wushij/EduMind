import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Trophy,
  Lightning,
  Cpu,
  Reading
} from '@element-plus/icons-vue';
import { createPaperExportTask, getExportTaskStatus, listMyExportTasks, deleteExportTask } from '@/api/question/export';
import { downloadByApiPath } from '@/utils/download/blob-download';
import type { ExportTaskVO } from '@/types/question/export';

export interface ExportConfigForm {
  examId: number;
  paperTitle: string;
  paperSubtitle: string;
  paperSize: string;
  confidentialLevel: string;
  showSealingLine: boolean;
  showStudentInfo: boolean;
  showScoreGrid: boolean;
  showNoticeBar: boolean;
  showWatermark: boolean;
  watermarkText: string;
  showAnswerSheet: boolean;
  showAnalysis: boolean;
  fontFamily: string;
  lineSpacing: string;
  optionLayout: string;
  showPointBadge: boolean;
}

export interface ExportHistoryItem {
  taskId: string;
  title: string;
  size: string;
  type: string;
  status: string;
  progress?: number;
  errorMsg?: string;
  createTime: string;
  downloadUrl?: string;
}

export interface PresetTemplate {
  id: string;
  name: string;
  tag: string;
  tagType: string;
  desc: string;
  icon: unknown;
  bgColor: string;
  color: string;
  config: Partial<ExportConfigForm>;
}

export const PRESET_TEMPLATES: PresetTemplate[] = [
  {
    id: 'preset-1',
    name: '全真高考模拟大卷',
    tag: '国家考务规制',
    tagType: 'primary',
    desc: '含绝密标示、左侧密封装订线、准考证条形码与大题总得分网格表',
    icon: Trophy,
    bgColor: '#EFF6FF',
    color: '#2563EB',
    config: {
      paperTitle: '2026年普通高等学校招生全国统一考试冲刺预测卷',
      paperSubtitle: '理科数学 (全卷共4页 满分150分 考试时间120分钟)',
      paperSize: 'A4',
      confidentialLevel: '绝密 ★ 启用前',
      showSealingLine: true,
      showStudentInfo: true,
      showScoreGrid: true,
      showNoticeBar: true,
      showWatermark: true,
      watermarkText: '智教云示范第一中学 · 内部教学试卷 · 严禁翻印',
      showAnswerSheet: true,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'normal',
      optionLayout: 'horizontal',
      showPointBadge: true
    }
  },
  {
    id: 'preset-2',
    name: '单元随堂高效精练卷',
    tag: '轻量省纸速印',
    tagType: 'success',
    desc: '紧凑版面、去除密封线、极简考生信息栏，适合课后周测与大量快印',
    icon: Lightning,
    bgColor: '#ECFDF5',
    color: '#059669',
    config: {
      paperTitle: '高三数学第一轮复习 · 函数与导数专题课时测验',
      paperSubtitle: '单元过关诊断 (满分100分 建议用时60分钟)',
      paperSize: 'A4',
      confidentialLevel: '',
      showSealingLine: false,
      showStudentInfo: true,
      showScoreGrid: false,
      showNoticeBar: false,
      showWatermark: false,
      watermarkText: '',
      showAnswerSheet: false,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'compact',
      optionLayout: 'horizontal',
      showPointBadge: true
    }
  },
  {
    id: 'preset-3',
    name: 'AI 弱项攻坚变式卷',
    tag: '智适应靶向',
    tagType: 'warning',
    desc: '联动知识图谱学情画像，附带核心素养考点标记与分层变式题目',
    icon: Cpu,
    bgColor: '#FFFBEB',
    color: '#D97706',
    config: {
      paperTitle: 'EduMind AI 错题变式与核心素养攻坚测评卷',
      paperSubtitle: '基于空间几何与解析几何薄弱诊断智能生成',
      paperSize: 'A4',
      confidentialLevel: '内部教学诊断资料',
      showSealingLine: true,
      showStudentInfo: true,
      showScoreGrid: true,
      showNoticeBar: true,
      showWatermark: true,
      watermarkText: 'EduMind AI 智能教学系统 · 个性化测评',
      showAnswerSheet: true,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'normal',
      optionLayout: 'grid',
      showPointBadge: true
    }
  },
  {
    id: 'preset-4',
    name: '名师教研评讲卷',
    tag: '教师教研版',
    tagType: 'danger',
    desc: '题目下直接附带分步采分细则、考点突破口点拨与易错警示',
    icon: Reading,
    bgColor: '#FEF2F2',
    color: '#DC2626',
    config: {
      paperTitle: '2026届高三摸底统考数学试题 · 教师讲评与采分细则',
      paperSubtitle: '含考点分布、思路点拨与分步赋分标示 (教研专用)',
      paperSize: 'A4',
      confidentialLevel: '内部教学诊断资料',
      showSealingLine: false,
      showStudentInfo: false,
      showScoreGrid: false,
      showNoticeBar: false,
      showWatermark: true,
      watermarkText: '智教云名师教研室 · 备课讲评专用',
      showAnswerSheet: false,
      showAnalysis: true,
      fontFamily: 'SimSun',
      lineSpacing: 'relaxed',
      optionLayout: 'horizontal',
      showPointBadge: true
    }
  }
];

export function createDefaultConfigForm(): ExportConfigForm {
  return {
    examId: 101,
    paperTitle: '2026年普通高等学校招生全国统一考试冲刺预测卷',
    paperSubtitle: '理科数学 (全卷共4页 满分150分 考试时间120分钟)',
    paperSize: 'A4',
    confidentialLevel: '绝密 ★ 启用前',
    showSealingLine: true,
    showStudentInfo: true,
    showScoreGrid: true,
    showNoticeBar: true,
    showWatermark: true,
    watermarkText: '智教云示范第一中学 · 内部教学试卷 · 严禁翻印',
    showAnswerSheet: true,
    showAnalysis: true,
    fontFamily: 'SimSun',
    lineSpacing: 'normal',
    optionLayout: 'horizontal',
    showPointBadge: true
  };
}

export const DEFAULT_EXPORT_HISTORY: ExportHistoryItem[] = [
  {
    taskId: 'EXP-17262104001',
    title: '2026年普通高等学校招生全国统一考试冲刺预测卷',
    size: 'A4',
    type: 'PDF',
    status: 'SUCCESS',
    createTime: '2026-09-13 10:45:12',
    downloadUrl: '#'
  },
  {
    taskId: 'EXP-17262098002',
    title: '高三数学开学第一单元检测试题 (带答题卡)',
    size: 'B4',
    type: 'PDF',
    status: 'SUCCESS',
    createTime: '2026-09-12 16:30:00',
    downloadUrl: '#'
  },
  {
    taskId: 'EXP-17262095003',
    title: 'EduMind AI 错题变式与核心素养攻坚测评卷',
    size: 'A4',
    type: 'Word (.docx)',
    status: 'SUCCESS',
    createTime: '2026-09-12 14:10:25',
    downloadUrl: '#'
  }
];

export function parseExportTaskTitle(item: ExportTaskVO): string {
  let title = item.paperTitle;
  if (!title && item.exportParams) {
    try {
      const parsed = JSON.parse(item.exportParams);
      title = parsed.paperTitle;
    } catch {
      // ignore
    }
  }
  return title || `试卷 #${item.bizId} 考务排版`;
}

export function mapExportTaskToHistoryItem(
  item: ExportTaskVO,
  paperSize: string
): ExportHistoryItem {
  return {
    taskId: item.taskId,
    title: parseExportTaskTitle(item),
    size: paperSize || 'A4',
    type: 'PDF',
    status: item.status,
    progress: item.progress,
    errorMsg: item.errorMsg,
    createTime: item.createTime || '刚刚',
    downloadUrl: item.downloadUrl
  };
}

export function applyPresetConfig(
  configForm: ExportConfigForm,
  preset: PresetTemplate
): void {
  Object.assign(configForm, preset.config);
}

export function useExport() {
  const loading = ref(false);
  const exporting = ref(false);
  const zoomScale = ref(0.85);
  const examTimeScore = ref('150分 / 120分钟');
  const activeTab = ref('exam');
  const previewPageMode = ref('page1');
  const printScope = ref('all');
  const currentPresetId = ref('preset-1');

  const presetTemplates = PRESET_TEMPLATES;
  const configForm = ref<ExportConfigForm>(createDefaultConfigForm());
  const exportHistory = ref<ExportHistoryItem[]>([...DEFAULT_EXPORT_HISTORY]);

  const applyPreset = (preset: PresetTemplate) => {
    currentPresetId.value = preset.id;
    applyPresetConfig(configForm.value, preset);
    ElMessage.success(`已切换至「${preset.name}」排版预设方案`);
  };

  const pollExportTask = async (taskId: string) => {
    let attempts = 0;
    const maxAttempts = 30;
    while (attempts < maxAttempts) {
      await new Promise(resolve => setTimeout(resolve, 1000));
      attempts++;
      try {
        const statusRes = await getExportTaskStatus(taskId);
        const current = statusRes.data;
        if (current.status === 'SUCCESS') {
          ElMessage.success('试卷导出完成，已生成下载链接！');
          await refreshHistory();
          return;
        } else if (current.status === 'FAILED') {
          ElMessage.error(`试卷导出失败: ${current.errorMsg || '未知异常'}`);
          await refreshHistory();
          return;
        }
      } catch (err) {
        console.warn('轮询导出任务状态重试中...', err);
      }
    }
    ElMessage.warning('试卷导出排版耗时较长，请稍后刷新任务列表查看结果');
    await refreshHistory();
  };

  const handleCreateExportTask = async () => {
    try {
      exporting.value = true;
      const res = await createPaperExportTask({
        examId: configForm.value.examId,
        paperTitle: configForm.value.paperTitle,
        paperSubtitle: configForm.value.paperSubtitle,
        paperSize: configForm.value.paperSize as 'A4' | 'B4',
        showWatermark: configForm.value.showWatermark,
        watermarkText: configForm.value.watermarkText,
        showAnswerSheet: configForm.value.showAnswerSheet,
        showAnalysis: configForm.value.showAnalysis,
        showStudentInfo: configForm.value.showStudentInfo,
        showScoreGrid: configForm.value.showScoreGrid
      });

      const task = res.data;
      if (!task || !task.taskId) {
        throw new Error('创建导出任务返回异常');
      }

      ElMessage.info('试卷导出任务已提交排队，后台正在高保真排版中...');
      await refreshHistory();
      await pollExportTask(task.taskId);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '导出任务提交失败';
      ElMessage.error(message);
    } finally {
      exporting.value = false;
    }
  };

  const handleExportWord = () => {
    ElMessage.success('正在将试卷排版内容封装为 Office Open XML (.docx) 格式并开始下载...');
    exportHistory.value.unshift({
      taskId: 'EXP-' + Date.now(),
      title: configForm.value.paperTitle,
      size: configForm.value.paperSize,
      type: 'Word (.docx)',
      status: 'SUCCESS',
      createTime: '刚刚',
      downloadUrl: '#'
    });
  };

  const handlePrintDirect = () => {
    const prevMode = previewPageMode.value;
    if (printScope.value === 'all') {
      previewPageMode.value = 'continuous';
    }
    setTimeout(() => {
      window.print();
      if (printScope.value === 'all') {
        previewPageMode.value = prevMode;
      }
    }, 100);
  };

  const refreshHistory = async () => {
    try {
      const res = await listMyExportTasks();
      if (res?.data && Array.isArray(res.data)) {
        if (res.data.length === 0) {
          exportHistory.value = [];
          return;
        }
        exportHistory.value = res.data.map(item =>
          mapExportTaskToHistoryItem(item, configForm.value.paperSize)
        );
      }
    } catch (err) {
      console.warn('获取近期导出任务历史失败', err);
    }
  };

  const downloadFile = async (row: ExportHistoryItem) => {
    if (row.status === 'FAILED') {
      ElMessage.error(row.errorMsg || '该导出任务生成失败，无法下载');
      return;
    }
    if (row.status !== 'SUCCESS') {
      ElMessage.warning('导出任务正在后台排版中，请稍候...');
      return;
    }
    if (!row.downloadUrl || row.downloadUrl === '#') {
      ElMessage.warning('暂无可用下载地址');
      return;
    }

    ElMessage.success(`开始下载文件: ${row.title || '试卷排版'}`);
    try {
      await downloadByApiPath(row.downloadUrl, (row.title || 'export') + '.pdf');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '文件下载失败，请稍后重试';
      ElMessage.error(message);
    }
  };

  const handleDeleteExportTask = async (row: ExportHistoryItem) => {
    try {
      await ElMessageBox.confirm(
        `确定删除导出任务「${row.title || row.taskId}」吗？删除后云端归档文件将一并移除，且不可恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      await deleteExportTask(row.taskId);
      ElMessage.success('导出任务已删除');
      await refreshHistory();
    } catch (err: unknown) {
      if (err === 'cancel' || err === 'close') return;
      ElMessage.error(err instanceof Error ? err.message : '删除导出任务失败');
    }
  };

  onMounted(() => {
    refreshHistory();
  });

  return {
    loading,
    exporting,
    zoomScale,
    examTimeScore,
    activeTab,
    previewPageMode,
    printScope,
    currentPresetId,
    presetTemplates,
    configForm,
    exportHistory,
    applyPreset,
    handleCreateExportTask,
    handleExportWord,
    handlePrintDirect,
    refreshHistory,
    downloadFile,
    handleDeleteExportTask
  };
}

import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Trophy,
  Lightning,
  Cpu,
  Reading
} from '@element-plus/icons-vue';
import axios from 'axios';
import { createPaperExportTask, getExportTaskStatus, listMyExportTasks, deleteExportTask } from '@/api/question/export';
import { downloadByApiPath } from '@/utils/download/blob-download';
import { printExamPaperInIframe } from '@/utils/print/exam-paper-print';
import type { ExportTaskVO } from '@/types/question/export';
import type { ExamPaper } from '@/types/question/exam';
import type { ExamOptionItem } from '@/types/question/export';
import { getExamDetail } from '@/api/question/exam';
import { normalizeExamPaper } from '@/utils/question/normalize-exam';
import { groupQuestionsByType } from '@/composables/question/useExam';
import { useExam } from '@/composables/question/useExam';
import { usePagination } from '@/composables/common/usePagination';

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

export interface ActiveExportTask {
  taskId: string;
  title: string;
  status: string;
  progress?: number;
  errorMsg?: string;
  downloadUrl?: string;
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

function formatExportTaskError(raw?: string): string {
  if (!raw?.trim()) return 'PDF 生成失败，请稍后重试';
  let msg = raw.trim();
  msg = msg.replace(/^com\.edumind\.common\.exception\.BusinessException:\s*/i, '');
  if (/^PDF\s/.test(msg)) return msg;
  return `PDF 生成失败：${msg}`;
}

const LAYOUT_PRESET_KEYS: (keyof ExportConfigForm)[] = [
  'paperSize',
  'confidentialLevel',
  'showSealingLine',
  'showStudentInfo',
  'showScoreGrid',
  'showNoticeBar',
  'showWatermark',
  'watermarkText',
  'showAnswerSheet',
  'showAnalysis',
  'fontFamily',
  'lineSpacing',
  'optionLayout',
  'showPointBadge'
];

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
      paperSize: 'A4',
      confidentialLevel: '绝密 ★ 启用前',
      showSealingLine: true,
      showStudentInfo: true,
      showScoreGrid: true,
      showNoticeBar: true,
      showWatermark: true,
      watermarkText: '智教云 · 内部教学试卷 · 严禁翻印',
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
    desc: '联动学情画像，附带考点标记与分层变式题目',
    icon: Cpu,
    bgColor: '#FFFBEB',
    color: '#D97706',
    config: {
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
    desc: '题目下附带解析与采分细则（预览解析页）',
    icon: Reading,
    bgColor: '#FEF2F2',
    color: '#DC2626',
    config: {
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
    examId: 0,
    paperTitle: '',
    paperSubtitle: '',
    paperSize: 'A4',
    confidentialLevel: '绝密 ★ 启用前',
    showSealingLine: true,
    showStudentInfo: true,
    showScoreGrid: true,
    showNoticeBar: true,
    showWatermark: false,
    watermarkText: '智教云 · 内部教学试卷 · 严禁翻印',
    showAnswerSheet: true,
    showAnalysis: true,
    fontFamily: 'SimSun',
    lineSpacing: 'normal',
    optionLayout: 'horizontal',
    showPointBadge: true
  };
}

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
  preset: PresetTemplate,
  options?: { includeTitles?: boolean }
): void {
  const includeTitles = options?.includeTitles ?? false;
  const keys = includeTitles
    ? (Object.keys(preset.config) as (keyof ExportConfigForm)[])
    : LAYOUT_PRESET_KEYS;
  keys.forEach((key) => {
    const val = preset.config[key];
    if (val !== undefined) {
      (configForm as unknown as Record<string, unknown>)[key] = val;
    }
  });
}

function syncFormTitlesFromExam(configForm: ExportConfigForm, paper: ExamPaper, examTimeScore: { value: string }) {
  configForm.paperTitle = paper.title;
  configForm.paperSubtitle = `${paper.courseName} · 满分 ${paper.totalScore} 分 · ${paper.durationMinutes} 分钟`;
  examTimeScore.value = `${paper.totalScore}分 / ${paper.durationMinutes}分钟`;
}

export function useExport() {
  const route = useRoute();
  const { fetchExams, exams } = useExam();

  const loading = ref(false);
  const previewLoading = ref(false);
  const previewError = ref('');
  const exporting = ref(false);
  const zoomScale = ref(0.85);
  const examTimeScore = ref('');
  const activeTab = ref('exam');
  const previewPageMode = ref('body');
  const printScope = ref('all');
  const currentPresetId = ref('preset-1');

  const presetTemplates = PRESET_TEMPLATES;
  const configForm = ref<ExportConfigForm>(createDefaultConfigForm());
  const exportHistory = ref<ExportHistoryItem[]>([]);
  const { pageNum: exportHistoryPageNum, pageSize: exportHistoryPageSize, total: exportHistoryTotal } =
    usePagination(10);
  const activeExportTask = ref<ActiveExportTask | null>(null);
  const examPaper = ref<ExamPaper | null>(null);
  const examOptions = ref<ExamOptionItem[]>([]);
  const examsLoading = ref(false);

  const groupedSections = computed(() => {
    if (!examPaper.value?.questions?.length) return [];
    return groupQuestionsByType(examPaper.value.questions);
  });

  const totalQuestionsCount = computed(() => examPaper.value?.questions?.length ?? 0);

  const objectiveQuestionCount = computed(() => {
    const types = new Set(['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE']);
    return examPaper.value?.questions?.filter((q) => types.has(q.type)).length ?? 0;
  });

  const lastExportTask = computed(() => exportHistory.value[0] ?? null);

  async function loadExamOptions() {
    examsLoading.value = true;
    try {
      await fetchExams({ page: 1, pageSize: 100 });
      examOptions.value = exams.value.map((e) => ({
        id: e.id,
        title: e.title,
        courseName: e.courseName,
        totalScore: e.totalScore
      }));
    } catch {
      examOptions.value = [];
    } finally {
      examsLoading.value = false;
    }
  }

  async function loadExamPreview(examId: number) {
    if (!examId || examId <= 0) {
      examPaper.value = null;
      previewError.value = '';
      return;
    }
    previewLoading.value = true;
    previewError.value = '';
    try {
      const res = await getExamDetail(examId);
      examPaper.value = normalizeExamPaper((res.data || {}) as unknown as Record<string, unknown>);
      if (examPaper.value) {
        const fromList = examOptions.value.find((e) => e.id === examId);
        if (fromList?.courseName && examPaper.value.courseName?.startsWith('课程 #')) {
          examPaper.value.courseName = fromList.courseName;
        }
        syncFormTitlesFromExam(configForm.value, examPaper.value, examTimeScore);
      }
    } catch (err: unknown) {
      examPaper.value = null;
      previewError.value = err instanceof Error ? err.message : '加载试卷详情失败';
    } finally {
      previewLoading.value = false;
    }
  }

  function resolveInitialExamId(): number {
    const queryId = Number(route.query.examId);
    if (Number.isFinite(queryId) && queryId > 0) {
      return queryId;
    }
    return examOptions.value[0]?.id ?? 0;
  }

  const applyPreset = (preset: PresetTemplate) => {
    currentPresetId.value = preset.id;
    applyPresetConfig(configForm.value, preset);
    ElMessage.success(`已切换至「${preset.name}」排版预设方案`);
  };

  function scrollToExportJobs() {
    nextTick(() => {
      document.getElementById('export-job-table')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  function syncActiveExportTask(vo: ExportTaskVO, title: string) {
    activeExportTask.value = {
      taskId: vo.taskId,
      title,
      status: vo.status,
      progress: vo.progress,
      errorMsg: vo.errorMsg,
      downloadUrl: vo.downloadUrl
    };
  }

  const pollExportTask = async (taskId: string, title: string) => {
    let attempts = 0;
    const maxAttempts = 60;
    while (attempts < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      attempts++;
      try {
        const statusRes = await getExportTaskStatus(taskId);
        const current = statusRes.data;
        if (!current) continue;
        syncActiveExportTask(current, title);
        if (current.status === 'SUCCESS') {
          await refreshHistory();
          const row = exportHistory.value.find((r) => r.taskId === taskId);
          if (row?.downloadUrl) {
            await downloadFile(row);
          } else {
            ElMessage.success('PDF 已生成，请在下方任务列表点击「下载」');
          }
          if (activeExportTask.value) {
            activeExportTask.value.status = 'SUCCESS';
            activeExportTask.value.downloadUrl = row?.downloadUrl || current.downloadUrl;
          }
          return;
        }
        if (current.status === 'FAILED') {
          ElMessage.error(formatExportTaskError(current.errorMsg));
          await refreshHistory();
          return;
        }
        await refreshHistory();
      } catch (err) {
        console.warn('轮询导出任务状态重试中...', err);
      }
    }
    ElMessage.warning('排版仍在进行，请稍后在下方「导出任务」列表查看并下载');
    await refreshHistory();
  };

  const handleCreateExportTask = async () => {
    if (!configForm.value.examId || configForm.value.examId <= 0) {
      ElMessage.warning('请先选择要导出的试卷');
      return;
    }
    if (!examPaper.value?.questions?.length) {
      ElMessage.warning('当前试卷没有题目，无法导出');
      return;
    }
    try {
      exporting.value = true;
      const form = configForm.value;
      const res = await createPaperExportTask({
        examId: form.examId,
        paperTitle: form.paperTitle || examPaper.value.title,
        paperSubtitle: form.paperSubtitle,
        paperSize: form.paperSize as 'A4' | 'B4',
        confidentialLevel: form.confidentialLevel,
        showSealingLine: form.showSealingLine,
        showWatermark: form.showWatermark,
        watermarkText: form.watermarkText,
        showAnswerSheet: form.showAnswerSheet,
        showAnalysis: form.showAnalysis,
        showStudentInfo: form.showStudentInfo,
        showScoreGrid: form.showScoreGrid,
        showNoticeBar: form.showNoticeBar,
        showPointBadge: form.showPointBadge,
        fontFamily: form.fontFamily,
        lineSpacing: form.lineSpacing,
        optionLayout: form.optionLayout
      });

      const task = res.data;
      if (!task?.taskId) {
        throw new Error('创建导出任务返回异常');
      }

      const taskTitle = form.paperTitle || examPaper.value.title;
      syncActiveExportTask(task, taskTitle);
      scrollToExportJobs();
      exportHistoryPageNum.value = 1;
      await refreshHistory();
      await pollExportTask(task.taskId, taskTitle);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '导出任务提交失败';
      ElMessage.error(message);
    } finally {
      exporting.value = false;
    }
  };

  const handleExportWord = () => {
    ElMessage.info('Word (.docx) 导出即将支持，请暂时使用「生成高保真 PDF」或「纯净打印」');
  };

  const printDialogVisible = ref(false);
  const printPreviewRestore = { mode: 'body', zoom: 0.85 };

  function preparePrintPreviewState() {
    printPreviewRestore.mode = previewPageMode.value;
    printPreviewRestore.zoom = zoomScale.value;
    zoomScale.value = 1;
    if (printScope.value === 'all') {
      previewPageMode.value = 'continuous';
    }
  }

  function restorePrintPreviewState() {
    zoomScale.value = printPreviewRestore.zoom;
    if (printScope.value === 'all') {
      previewPageMode.value = printPreviewRestore.mode;
    }
  }

  const handlePrintDirect = () => {
    if (!examPaper.value?.questions?.length) {
      ElMessage.warning('请先选择含题目的试卷后再打印');
      return;
    }
    preparePrintPreviewState();
    nextTick(async () => {
      await nextTick();
      requestAnimationFrame(() => {
        printDialogVisible.value = true;
      });
    });
  };

  const confirmPrintFromDialog = () => {
    const ok = printExamPaperInIframe({
      currentViewOnly: printScope.value === 'current',
      hideWatermark: true
    });
    if (ok) {
      printDialogVisible.value = false;
    }
  };

  watch(printDialogVisible, (open) => {
    if (!open) {
      restorePrintPreviewState();
    }
  });

  const refreshHistory = async () => {
    try {
      const res = await listMyExportTasks({
        page: exportHistoryPageNum.value,
        pageSize: exportHistoryPageSize.value
      });
      const data = res?.data;
      // 新版：PageResult；旧版未重启后端时仍为数组，前端先客户端分页并显示条数
      if (Array.isArray(data)) {
        exportHistoryTotal.value = data.length;
        const start = (exportHistoryPageNum.value - 1) * exportHistoryPageSize.value;
        const slice = data.slice(start, start + exportHistoryPageSize.value);
        exportHistory.value = slice.map((item) =>
          mapExportTaskToHistoryItem(item, configForm.value.paperSize)
        );
        return;
      }
      if (data?.list) {
        exportHistoryTotal.value = Number(data.total ?? data.list.length ?? 0);
        exportHistory.value = data.list.map((item) =>
          mapExportTaskToHistoryItem(item, configForm.value.paperSize)
        );
        if (exportHistory.value.length === 0 && exportHistoryPageNum.value > 1 && exportHistoryTotal.value > 0) {
          exportHistoryPageNum.value = 1;
          await refreshHistory();
        }
        return;
      }
      exportHistory.value = [];
      exportHistoryTotal.value = 0;
    } catch (err) {
      console.warn('获取近期导出任务历史失败', err);
    }
  };

  const onExportHistoryPageChange = () => {
    refreshHistory();
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

    ElMessage.success(`开始下载: ${row.title || '试卷排版'}`);
    try {
      await downloadByApiPath(row.downloadUrl, `${row.title || 'export'}.pdf`);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '文件下载失败，请稍后重试';
      ElMessage.error(message);
    }
  };

  function resolveApiErrorMessage(err: unknown, fallback: string): string {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data;
      if (data && typeof data === 'object' && 'message' in data) {
        const msg = (data as { message?: string }).message;
        if (msg) return msg;
      }
    }
    if (err instanceof Error && err.message && !err.message.startsWith('Request failed with status code')) {
      return err.message;
    }
    return fallback;
  }

  const handleDeleteExportTask = async (row: ExportHistoryItem) => {
    try {
      const running = row.status === 'PENDING' || row.status === 'PROCESSING';
      await ElMessageBox.confirm(
        running
          ? `任务「${row.title || row.taskId}」仍在排队或生成中。删除后无法继续下载该次导出，确定要清理这条记录吗？`
          : `确定删除导出任务「${row.title || row.taskId}」吗？删除后云端归档文件将一并移除，且不可恢复。`,
        running ? '清理卡住的任务' : '删除确认',
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
      ElMessage.error(resolveApiErrorMessage(err, '删除导出任务失败'));
    }
  };

  watch(
    () => configForm.value.examId,
    (id) => {
      if (id > 0) {
        loadExamPreview(id);
      }
    }
  );

  let historyPollTimer: ReturnType<typeof setInterval> | null = null;

  function hasPendingExportJobs() {
    return exportHistory.value.some(
      (row) => row.status === 'PENDING' || row.status === 'PROCESSING'
    );
  }

  function syncHistoryPollTimer() {
    if (hasPendingExportJobs() && historyPollTimer == null) {
      historyPollTimer = setInterval(() => {
        void refreshHistory();
      }, 3000);
    } else if (!hasPendingExportJobs() && historyPollTimer != null) {
      clearInterval(historyPollTimer);
      historyPollTimer = null;
    }
  }

  watch(exportHistory, () => syncHistoryPollTimer(), { deep: true });

  onMounted(async () => {
    loading.value = true;
    try {
      await loadExamOptions();
      const initialId = resolveInitialExamId();
      if (initialId > 0) {
        configForm.value.examId = initialId;
      }
      applyPresetConfig(configForm.value, PRESET_TEMPLATES[0]);
      await refreshHistory();
      syncHistoryPollTimer();
    } finally {
      loading.value = false;
    }
  });

  onUnmounted(() => {
    if (historyPollTimer != null) {
      clearInterval(historyPollTimer);
      historyPollTimer = null;
    }
  });

  const dismissActiveExportTask = () => {
    activeExportTask.value = null;
  };

  const downloadActiveExportTask = async () => {
    const active = activeExportTask.value;
    if (!active) return;
    const row = exportHistory.value.find((r) => r.taskId === active.taskId);
    if (row) {
      await downloadFile(row);
      return;
    }
    if (active.downloadUrl) {
      await downloadFile({
        taskId: active.taskId,
        title: active.title,
        size: configForm.value.paperSize,
        type: 'PDF',
        status: 'SUCCESS',
        createTime: '刚刚',
        downloadUrl: active.downloadUrl
      });
    }
  };

  return {
    loading,
    previewLoading,
    previewError,
    exporting,
    activeExportTask,
    zoomScale,
    examTimeScore,
    activeTab,
    previewPageMode,
    printScope,
    currentPresetId,
    presetTemplates,
    configForm,
    exportHistory,
    exportHistoryPageNum,
    exportHistoryPageSize,
    exportHistoryTotal,
    onExportHistoryPageChange,
    examPaper,
    examOptions,
    examsLoading,
    groupedSections,
    totalQuestionsCount,
    objectiveQuestionCount,
    lastExportTask,
    applyPreset,
    handleCreateExportTask,
    handleExportWord,
    handlePrintDirect,
    printDialogVisible,
    confirmPrintFromDialog,
    refreshHistory,
    downloadFile,
    handleDeleteExportTask,
    loadExamPreview,
    scrollToExportJobs,
    dismissActiveExportTask,
    downloadActiveExportTask
  };
}

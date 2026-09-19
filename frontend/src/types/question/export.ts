export interface PaperExportRequest {
  examId: number;
  paperTitle: string;
  paperSubtitle?: string;
  paperSize: 'A4' | 'B4';
  confidentialLevel?: string;
  showSealingLine?: boolean;
  showWatermark: boolean;
  watermarkText?: string;
  showAnswerSheet: boolean;
  showAnalysis: boolean;
  showStudentInfo: boolean;
  showScoreGrid: boolean;
  showNoticeBar?: boolean;
  showPointBadge?: boolean;
  fontFamily?: string;
  lineSpacing?: string;
  optionLayout?: string;
}

export interface ExportTaskVO {
  taskId: string;
  tenantId?: number;
  userId?: number;
  bizType: string;
  bizId?: number;
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';
  progress: number;
  downloadUrl?: string;
  errorMsg?: string;
  exportParams?: string;
  paperTitle?: string;
  createTime?: string;
}

export interface ExamOptionItem {
  id: number;
  title: string;
  courseName?: string;
  totalScore?: number;
}

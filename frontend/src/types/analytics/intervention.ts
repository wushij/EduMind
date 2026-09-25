export interface TargetStudentVO {
  studentId: number;
  realName: string;
  username?: string;
  studentNo?: string;
  avatar?: string;
  score?: number;
  riskLevel?: 'RISK' | 'WARNING' | 'GOOD' | 'EXCELLENT';
}

export interface InterventionResourceVO {
  resourceId: number;
  title: string;
  type: string;
  duration?: string;
  url?: string;
  description?: string;
}

export interface InterventionQuestionVO {
  questionId: number;
  stem: string;
  type: string;
  difficulty?: string;
  options?: string[];
  answer?: string;
  analysis?: string;
}

export interface InterventionTrackingStatsVO {
  dispatchedCount: number;
  completedCount: number;
  avgImprovementScore: number;
}

export interface TeachingInterventionVO {
  id: number;
  tenantId: number;
  courseId: number;
  courseName: string;
  knowledgePointId?: number;
  knowledgePointTitle?: string;
  triggerType: 'EXAM_WEAK' | 'ACTIVITY_DROP' | 'HOMEWORK_DELAY' | 'ANOMALOUS_ATTENDANCE';
  title: string;
  proposalText: string;
  affectedStudentCount: number;
  status: 'PENDING' | 'APPROVED' | 'DISPATCHED' | 'REVOKED';
  approvedBy?: string;
  createTime: string;
  dispatchedTime?: string;
  expectedImprovement?: string;
  targetStudents?: TargetStudentVO[];
  resources?: InterventionResourceVO[];
  questions?: InterventionQuestionVO[];
  trackingStats?: InterventionTrackingStatsVO;
}

export interface InterventionOverviewStatsVO {
  pendingCount: number;
  totalAffectedStudents: number;
  /** 干预后掌握度平均提升点数；无真实闭环跟踪数据时为 null（页面渲染 '—'） */
  avgImprovementRate: number | null;
  /** 干预闭环完成率 (0~100)；无真实数据时为 null（页面渲染 '—'） */
  completionRate: number | null;
  totalInterventions: number;
  dispatchedCount: number;
}

export interface InterventionActionRequest {
  remark?: string;
  proposalText?: string;
  resourceIds?: number[];
  customQuestionIds?: number[];
  targetStudentIds?: number[];
}

export interface InterventionCreateRequest {
  courseId?: number;
  courseName?: string;
  knowledgePointId?: number;
  knowledgePointTitle?: string;
  triggerType?: string;
  title: string;
  proposalText: string;
  affectedStudentCount?: number;
  targetStudentIds?: number[];
  resourceIds?: number[];
  customQuestionIds?: number[];
  expectedImprovement?: string;
}

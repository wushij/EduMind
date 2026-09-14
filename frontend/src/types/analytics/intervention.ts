export interface TeachingInterventionVO {
  id: number;
  tenantId: number;
  courseId: number;
  courseName: string;
  triggerType: 'EXAM_WEAK' | 'ACTIVITY_DROP' | 'HOMEWORK_DELAY' | 'ANOMALOUS_ATTENDANCE';
  title: string;
  proposalText: string;
  affectedStudentCount: number;
  status: 'PENDING' | 'APPROVED' | 'DISPATCHED' | 'REVOKED';
  approvedBy?: string;
  createTime: string;
}

export interface InterventionActionRequest {
  remark?: string;
  customQuestionIds?: number[];
}

export interface InterventionCreateRequest {
  courseId?: number;
  courseName?: string;
  triggerType?: string;
  title: string;
  proposalText: string;
  affectedStudentCount?: number;
  customQuestionIds?: number[];
}


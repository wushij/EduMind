import { get, post, put } from '@/core/http/request';

export const createSubmission = (assignmentId: number, answers: Array<{ questionId: number; answer: string }>) =>
  post<number>(`/submissions/assignments/${assignmentId}`, { answers });

export const getSubmissionDetail = (id: number) => get<any>(`/submissions/${id}`);

export const getSubmissionGrading = (id: number) => get<any>(`/submissions/${id}/grading`);

export const gradeSubmission = (id: number) => post<any>(`/submissions/${id}/grade`);

export const reviewGrading = (id: number, items: Array<{ questionId: number; score: number; teacherComment?: string }>) =>
  put<void>(`/submissions/${id}/grading/review`, { items });

export const getSubmissionsByAssignment = (assignmentId: number) =>
  get<any[]>(`/submissions/assignments/${assignmentId}`);

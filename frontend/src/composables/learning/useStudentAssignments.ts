import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyAssignments, getAssignmentPaper, submitAssignment } from '@/api/question/assignment';
import { getSubmissionDetail } from '@/api/question/submission';
import type { AssignmentPaper, StudentAssignment } from '@/types/question/assignment';
import type { HttpRequestConfig } from '@/core/http/types';

export function useStudentAssignments() {
  const loading = ref(false);
  const assignments = ref<StudentAssignment[]>([]);
  const paper = ref<AssignmentPaper | null>(null);

  async function fetchMyAssignments(courseId?: number) {
    loading.value = true;
    try {
      const res = await getMyAssignments({ courseId });
      assignments.value = res.data || [];
      return assignments.value;
    } catch (err: unknown) {
      assignments.value = [];
      ElMessage.error(err instanceof Error ? err.message : '加载学习任务失败');
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function loadPaper(assignmentId: number) {
    loading.value = true;
    try {
      const res = await getAssignmentPaper(assignmentId);
      paper.value = res.data ?? null;
      return paper.value;
    } catch (err: unknown) {
      paper.value = null;
      ElMessage.error(err instanceof Error ? err.message : '加载作业卷面失败');
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function submitPaper(
    assignmentId: number,
    answers: Array<{ questionId: number; answer: string }>,
    config?: HttpRequestConfig
  ) {
    const res = await submitAssignment(assignmentId, answers, config);
    return res.data;
  }

  async function loadSubmissionResult(submissionId: number) {
    const res = await getSubmissionDetail(submissionId);
    return res.data;
  }

  return {
    loading,
    assignments,
    paper,
    fetchMyAssignments,
    loadPaper,
    submitPaper,
    loadSubmissionResult
  };
}

import { ref } from 'vue';
import {
  getAssignments,
  getAssignmentDetail,
  createAssignment,
  publishAssignment,
  submitAssignment,
  getAssignmentSubmissions
} from '@/api/question/assignment';
import { Assignment } from '@/types/question/assignment';
import { USE_MOCK } from '@/config/mock';

export function useAssignment() {
  const assignments = ref<Assignment[]>([]);
  const currentAssignment = ref<Assignment | null>(null);
  const submissions = ref<any[]>([]);
  const loading = ref(false);
  const total = ref(0);

  async function fetchAssignments(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getAssignments(params);
      assignments.value = (res.data?.list || []) as Assignment[];
      total.value = res.data?.total ?? assignments.value.length;
    } catch {
      assignments.value = USE_MOCK ? [] : [];
      total.value = 0;
    } finally {
      loading.value = false;
    }
  }

  async function fetchAssignmentDetail(id: number) {
    loading.value = true;
    try {
      const res = await getAssignmentDetail(id);
      currentAssignment.value = res.data;
    } catch {
      currentAssignment.value = null;
    } finally {
      loading.value = false;
    }
    return currentAssignment.value;
  }

  async function createAndPublish(data: Record<string, any>, publish = true) {
    const res = await createAssignment(data);
    const id = res.data;
    if (publish && id) await publishAssignment(id);
    return id;
  }

  async function submit(id: number, answers: Array<{ questionId: number; answer: string }>) {
    return submitAssignment(id, answers);
  }

  async function fetchSubmissions(assignmentId: number) {
    try {
      const res = await getAssignmentSubmissions(assignmentId);
      submissions.value = res.data || [];
    } catch {
      submissions.value = [];
    }
    return submissions.value;
  }

  return {
    assignments,
    currentAssignment,
    submissions,
    loading,
    total,
    fetchAssignments,
    fetchAssignmentDetail,
    createAndPublish,
    submit,
    fetchSubmissions
  };
}

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
    } catch (err) {
      assignments.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchAssignmentDetail(id: number) {
    loading.value = true;
    try {
      const res = await getAssignmentDetail(id);
      currentAssignment.value = res.data;
    } catch (err) {
      currentAssignment.value = null;
      throw err;
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
    } catch (err) {
      submissions.value = [];
      throw err;
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

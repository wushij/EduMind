import { ref, computed } from 'vue';
import { getMyAssignments } from '@/api/question/assignment';
import { getCourseList } from '@/api/course/course';
import { SUBMISSION_STATUS } from '@/constants/question/assignment';
import type { StudentAssignment } from '@/types/question/assignment';
import type { Course } from '@/types/course/course';
import { normalizeCourseListFromApi } from '@/utils/course/course-display';

export type LearningTaskType = 'ASSIGNMENT';

export interface LearningTaskItem {
  id: number;
  title: string;
  type: LearningTaskType;
  courseName: string;
  courseId: number;
  dueDate: string;
  estimatedMinutes: number;
  status: 'PENDING' | 'COMPLETED';
  assignmentId: number;
}

function mapAssignmentToTask(a: StudentAssignment): LearningTaskItem {
  const done =
    a.mySubmissionStatus === SUBMISSION_STATUS.GRADED ||
    a.mySubmissionStatus === SUBMISSION_STATUS.REVIEWED;
  return {
    id: a.id,
    assignmentId: a.id,
    title: a.title,
    type: 'ASSIGNMENT',
    courseName: a.courseName || '—',
    courseId: a.courseId,
    dueDate: a.deadline ? String(a.deadline).replace('T', ' ').slice(0, 16) : '—',
    estimatedMinutes: Math.max(15, Math.min(60, a.totalScore ?? 30)),
    status: done ? 'COMPLETED' : 'PENDING'
  };
}

export function useLearningTasks() {
  const loading = ref(false);
  const tasks = ref<LearningTaskItem[]>([]);
  const courseOptions = ref<Course[]>([]);

  const pendingCount = computed(() => tasks.value.filter((t) => t.status === 'PENDING').length);
  const completedCount = computed(() => tasks.value.filter((t) => t.status === 'COMPLETED').length);
  const completionPercentage = computed(() => {
    if (!tasks.value.length) return 0;
    return Math.round((completedCount.value / tasks.value.length) * 100);
  });

  async function loadCourseOptions() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      courseOptions.value = normalizeCourseListFromApi(
        (res.data?.list || []) as unknown as Record<string, unknown>[]
      ) as unknown as Course[];
    } catch {
      courseOptions.value = [];
    }
  }

  async function fetchTasks(courseId?: number) {
    loading.value = true;
    try {
      const res = await getMyAssignments(courseId ? { courseId } : undefined);
      const list = res.data || [];
      tasks.value = list
        .filter((a) => a.status === 'PUBLISHED' || a.status === 'CLOSED')
        .map(mapAssignmentToTask);
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    tasks,
    courseOptions,
    pendingCount,
    completedCount,
    completionPercentage,
    loadCourseOptions,
    fetchTasks
  };
}

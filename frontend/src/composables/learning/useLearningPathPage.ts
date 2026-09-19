import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getLearningHomeOverview } from '@/api/learning/home';
import { getLearningPathDetail, listLearningPathStudents } from '@/api/learning/learning-path';
import { useAuthStore } from '@/stores/auth/auth';
import { RoleEnum } from '@/constants/auth';
import type { LearningPathDetailVO, LearningPathStudentItem, LearningPathTask } from '@/types/learning/learning-path';

export function useLearningPathPage() {
  const route = useRoute();
  const router = useRouter();
  const authStore = useAuthStore();

  const loading = ref(false);
  const detail = ref<LearningPathDetailVO | null>(null);
  const courseOptions = ref<Array<{ id: number; name: string }>>([]);
  const courseId = ref<number>(0);
  const studentId = ref<number | null>(null);
  const studentOptions = ref<LearningPathStudentItem[]>([]);
  const hasEnrolledCourses = ref(true);

  const canPickStudent = computed(() =>
    authStore.hasAnyRole([RoleEnum.ADMIN, RoleEnum.TEACHER])
  );

  const path = computed(() => detail.value);

  async function loadCourses() {
    try {
      const res = await getLearningHomeOverview();
      const courses = res.data?.courses ?? [];
      courseOptions.value = courses.map((c) => ({
        id: c.courseId,
        name: c.courseName || `课程 #${c.courseId}`
      }));
      hasEnrolledCourses.value = courseOptions.value.length > 0;

      const queryCid = route.query.courseId ? Number(route.query.courseId) : null;
      if (queryCid && courseOptions.value.some((c) => c.id === queryCid)) {
        courseId.value = queryCid;
      } else if (res.data?.primaryCourseId) {
        courseId.value = res.data.primaryCourseId;
      } else if (courseOptions.value.length) {
        courseId.value = courseOptions.value[0].id;
      }
    } catch {
      courseOptions.value = [];
      hasEnrolledCourses.value = false;
    }
  }

  async function loadStudents() {
    if (!courseId.value || !canPickStudent.value) {
      studentOptions.value = [];
      return;
    }
    try {
      const res = await listLearningPathStudents(courseId.value);
      studentOptions.value = res.data ?? [];
      const querySid = route.query.studentId ? Number(route.query.studentId) : null;
      if (querySid && studentOptions.value.some((s) => s.studentId === querySid)) {
        studentId.value = querySid;
      } else if (studentOptions.value.length) {
        studentId.value = studentOptions.value[0].studentId;
      }
    } catch {
      studentOptions.value = [];
    }
  }

  function resolveStudentId(): number | undefined {
    if (canPickStudent.value && studentId.value != null) {
      return studentId.value;
    }
    return authStore.currentUser?.id;
  }

  async function loadPath() {
    if (!courseId.value) return;
    const sid = resolveStudentId();
    if (!sid) return;
    loading.value = true;
    try {
      const res = await getLearningPathDetail(courseId.value, sid);
      detail.value = res.data ?? null;
    } catch {
      detail.value = null;
      ElMessage.error('加载学习路径失败');
    } finally {
      loading.value = false;
    }
  }

  async function refreshAll() {
    await loadCourses();
    await loadStudents();
    if (!canPickStudent.value) {
      studentId.value = authStore.currentUser?.id ?? null;
    }
    await loadPath();
  }

  function onCourseChange(id: number) {
    courseId.value = id;
    void loadStudents().then(() => loadPath());
    router.replace({
      query: { ...route.query, courseId: String(id), studentId: studentId.value ? String(studentId.value) : undefined }
    });
  }

  function onStudentChange(id: number) {
    studentId.value = id;
    void loadPath();
    router.replace({
      query: { ...route.query, courseId: String(courseId.value), studentId: String(id) }
    });
  }

  function executeTask(task: LearningPathTask) {
    if (task.targetUrl) {
      router.push(task.targetUrl);
      return;
    }
    router.push({ path: '/learning/practice', query: { courseId: String(courseId.value) } });
  }

  onMounted(() => {
    void refreshAll();
  });

  watch(
    () => authStore.currentUser?.id,
    () => {
      if (!canPickStudent.value) {
        studentId.value = authStore.currentUser?.id ?? null;
      }
    }
  );

  return {
    loading,
    detail,
    path,
    courseOptions,
    courseId,
    studentId,
    studentOptions,
    canPickStudent,
    hasEnrolledCourses,
    loadPath,
    refreshAll,
    onCourseChange,
    onStudentChange,
    executeTask
  };
}

import { ref, watch, type MaybeRef, unref } from 'vue';
import {
  createCourseAnnouncement,
  getCourseOverview,
  saveCourseInstructors,
  saveCourseObjectives,
  syncCourseInstructors,
  updateCourseAnnouncement,
  withdrawCourseAnnouncement
} from '@/api/course/overview';
import type {
  CourseAnnouncementCreateRequest,
  CourseAnnouncementUpdateRequest,
  CourseInstructorsSaveRequest,
  CourseObjectivesSaveRequest,
  CourseOverviewVO
} from '@/types/course/overview';

export function useCourseOverview(courseIdInput: MaybeRef<number | undefined>) {
  const overview = ref<CourseOverviewVO | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchOverview() {
    const courseId = unref(courseIdInput);
    if (!courseId) {
      overview.value = null;
      return;
    }
    loading.value = true;
    error.value = null;
    try {
      const res = await getCourseOverview(courseId);
      overview.value = res.data;
    } catch (e: unknown) {
      overview.value = null;
      error.value = e instanceof Error ? e.message : '加载课程概览失败';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function saveObjectives(payload: CourseObjectivesSaveRequest) {
    const courseId = unref(courseIdInput);
    if (!courseId) return;
    await saveCourseObjectives(courseId, payload);
    await fetchOverview();
  }

  async function publishAnnouncement(payload: CourseAnnouncementCreateRequest) {
    const courseId = unref(courseIdInput);
    if (!courseId) return;
    await createCourseAnnouncement(courseId, payload);
    await fetchOverview();
  }

  async function patchAnnouncement(announcementId: number, payload: CourseAnnouncementUpdateRequest) {
    const courseId = unref(courseIdInput);
    if (!courseId) return;
    await updateCourseAnnouncement(courseId, announcementId, payload);
    await fetchOverview();
  }

  async function withdrawAnnouncement(announcementId: number) {
    const courseId = unref(courseIdInput);
    if (!courseId) return;
    await withdrawCourseAnnouncement(courseId, announcementId);
    await fetchOverview();
  }

  async function saveInstructors(payload: CourseInstructorsSaveRequest) {
    const courseId = unref(courseIdInput);
    if (!courseId) return;
    await saveCourseInstructors(courseId, payload);
    await fetchOverview();
  }

  async function syncInstructors() {
    const courseId = unref(courseIdInput);
    if (!courseId) return;
    await syncCourseInstructors(courseId);
    await fetchOverview();
  }

  watch(
    () => unref(courseIdInput),
    (id) => {
      if (id) {
        fetchOverview();
      }
    },
    { immediate: true }
  );

  return {
    overview,
    loading,
    error,
    fetchOverview,
    saveObjectives,
    publishAnnouncement,
    patchAnnouncement,
    withdrawAnnouncement,
    saveInstructors,
    syncInstructors
  };
}

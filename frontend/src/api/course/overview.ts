import { get, post, put } from '@/core/http/request';
import type { PageResult } from '@/types/common/api';
import type {
  CourseAnnouncementCreateRequest,
  CourseAnnouncementUpdateRequest,
  CourseAnnouncementVO,
  CourseInstructorCardVO,
  CourseInstructorsSaveRequest,
  CourseObjectiveVO,
  CourseObjectivesSaveRequest,
  CourseOverviewVO
} from '@/types/course/overview';
import { mapCourse } from '@/utils/course/map-course';

function mapOverview(raw: Record<string, unknown>): CourseOverviewVO {
  const courseRaw = (raw.course || {}) as Record<string, unknown>;
  return {
    course: mapCourse(courseRaw),
    objectives: Array.isArray(raw.objectives) ? raw.objectives as CourseObjectiveVO[] : [],
    announcementsPreview: Array.isArray(raw.announcementsPreview)
      ? raw.announcementsPreview as CourseAnnouncementVO[]
      : [],
    announcementTotal: Number(raw.announcementTotal ?? 0),
    instructors: Array.isArray(raw.instructors) ? raw.instructors as CourseInstructorCardVO[] : [],
    capabilityTags: Array.isArray(raw.capabilityTags)
      ? raw.capabilityTags as CourseOverviewVO['capabilityTags']
      : [],
    editable: Boolean(raw.editable)
  };
}

export const getCourseOverview = async (courseId: number) => {
  const res = await get<Record<string, unknown>>(`/courses/${courseId}/overview`);
  return { ...res, data: mapOverview((res.data || {}) as Record<string, unknown>) };
};

export const saveCourseObjectives = (courseId: number, data: CourseObjectivesSaveRequest) =>
  put<void>(`/courses/${courseId}/objectives`, data);

export const getCourseObjectives = (courseId: number) =>
  get<CourseObjectiveVO[]>(`/courses/${courseId}/objectives`);

export const pageCourseAnnouncements = (
  courseId: number,
  params: { page?: number; pageSize?: number; status?: string } = {}
) => get<PageResult<CourseAnnouncementVO>>(`/courses/${courseId}/announcements`, params);

export const createCourseAnnouncement = (courseId: number, data: CourseAnnouncementCreateRequest) =>
  post<CourseAnnouncementVO>(`/courses/${courseId}/announcements`, data);

export const updateCourseAnnouncement = (
  courseId: number,
  announcementId: number,
  data: CourseAnnouncementUpdateRequest
) => put<CourseAnnouncementVO>(`/courses/${courseId}/announcements/${announcementId}`, data);

export const withdrawCourseAnnouncement = (courseId: number, announcementId: number) =>
  post<void>(`/courses/${courseId}/announcements/${announcementId}/withdraw`);

export const getCourseInstructors = (courseId: number) =>
  get<CourseInstructorCardVO[]>(`/courses/${courseId}/instructors`);

export const saveCourseInstructors = (courseId: number, data: CourseInstructorsSaveRequest) =>
  put<void>(`/courses/${courseId}/instructors`, data);

export const syncCourseInstructors = (courseId: number) =>
  post<void>(`/courses/${courseId}/instructors/sync`);

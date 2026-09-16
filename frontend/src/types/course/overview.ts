import type { Course } from '@/types/course/course';

export interface CourseObjectiveVO {
  id?: number;
  sortOrder?: number;
  title: string;
  description?: string;
}

export interface CourseAnnouncementVO {
  id: number;
  courseId?: number;
  title: string;
  content: string;
  pinned?: boolean;
  status?: string;
  publishTime?: string;
  publisherId?: number;
  publisherName?: string;
}

export interface CourseInstructorCardVO {
  userId: number;
  username?: string;
  realName?: string;
  avatar?: string;
  memberRole?: string;
  roleLabel?: string;
  intro?: string;
  officeHours?: string;
  primary?: boolean;
  sortOrder?: number;
}

export interface CourseCapabilityTagVO {
  code: string;
  label: string;
  tone?: 'primary' | 'ai' | 'kb' | 'success' | 'default';
}

export interface CourseOverviewVO {
  course: Course;
  objectives: CourseObjectiveVO[];
  announcementsPreview: CourseAnnouncementVO[];
  announcementTotal: number;
  instructors: CourseInstructorCardVO[];
  capabilityTags: CourseCapabilityTagVO[];
  editable: boolean;
}

export interface CourseObjectivesSaveRequest {
  objectives: Array<{ title: string; description?: string }>;
}

export interface CourseAnnouncementCreateRequest {
  title: string;
  content: string;
  pinned?: boolean;
}

export interface CourseAnnouncementUpdateRequest {
  title?: string;
  content?: string;
  pinned?: boolean;
  status?: string;
}

export interface CourseInstructorProfileItem {
  userId: number;
  intro?: string;
  officeHours?: string;
  sortOrder?: number;
  primary?: boolean;
}

export interface CourseInstructorsSaveRequest {
  instructors: CourseInstructorProfileItem[];
}

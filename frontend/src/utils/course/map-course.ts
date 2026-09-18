import { Course, CourseStatus } from '@/types/course/course';

export function normalizeCourseStatus(status: unknown): CourseStatus {
  if (status === 1 || status === 'ACTIVE') {
    return 'ACTIVE';
  }
  if (
    status === 0
    || status === 2
    || status === 'ARCHIVED'
    || status === 'INACTIVE'
  ) {
    return 'ARCHIVED';
  }
  if (status === 'DRAFT') {
    return 'DRAFT';
  }
  return 'ACTIVE';
}

export function isActiveCourseStatus(status: CourseStatus | undefined): boolean {
  return status === 'ACTIVE' || status === 1;
}

export function isArchivedCourseStatus(status: CourseStatus | undefined): boolean {
  return (
    status === 'ARCHIVED'
    || status === 'INACTIVE'
    || status === 0
    || status === 2
  );
}

export function mapCourse(raw: Record<string, any>): Course {
  return {
    id: Number(raw.id),
    title: raw.name || raw.title || '',
    name: raw.name || raw.title,
    code: raw.code,
    coverUrl: raw.coverUrl || raw.cover || raw.coverImage,
    cover: raw.coverUrl || raw.cover || raw.coverImage,
    teacherId: raw.teacherId,
    teacherName: raw.teacherName || '',
    teacherAvatar: raw.teacherAvatar || raw.teacher_avatar,
    semester: raw.semester,
    studentCount: Number(raw.studentCount ?? 0),
    chapterCount: Number(raw.chapterCount ?? 0),
    knowledgePointCount: Number(raw.knowledgePointCount ?? 0),
    resourceCount: Number(raw.resourceCount ?? 0),
    progress: raw.progress,
    aiUsageCount: raw.aiUsageCount,
    status: normalizeCourseStatus(raw.status),
    category: raw.category || '计算机与软件',
    credits: raw.credits != null ? Number(raw.credits) : 3.0,
    plannedHours: raw.plannedHours != null ? Number(raw.plannedHours) : 48,
    aiPersona: raw.aiPersona || 'socrates',
    welcomeMessage: raw.welcomeMessage || '',
    description: raw.description,
    createdAt: raw.createdAt,
    knowledgeBaseId: raw.knowledgeBaseId != null ? Number(raw.knowledgeBaseId) : undefined,
    editable: raw.editable === true || raw.editable === 1 || raw.editable === 'true'
  };
}

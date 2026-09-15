import { Course } from '@/types/course/course';

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
    semester: raw.semester,
    studentCount: Number(raw.studentCount ?? 0),
    chapterCount: Number(raw.chapterCount ?? 0),
    knowledgePointCount: Number(raw.knowledgePointCount ?? 0),
    resourceCount: Number(raw.resourceCount ?? 0),
    progress: raw.progress,
    aiUsageCount: raw.aiUsageCount,
    status: raw.status ?? 'ACTIVE',
    description: raw.description,
    createdAt: raw.createdAt
  };
}

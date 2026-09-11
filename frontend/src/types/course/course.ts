export type CourseStatus = 'ACTIVE' | 'ARCHIVED' | 'DRAFT' | number;

export interface Course {
  id: number;
  title: string;
  name?: string;
  code?: string;
  cover?: string;
  coverUrl?: string;
  teacherId?: number;
  teacherName: string;
  semester?: string;
  studentCount: number;
  chapterCount: number;
  knowledgePointCount?: number;
  progress?: number;
  aiUsageCount?: number;
  status: CourseStatus;
  description?: string;
  createdAt?: string;
}

export type CourseVO = Course;

export interface CourseQuery {
  keyword?: string;
  status?: string;
  semester?: string;
  page?: number;
  pageSize?: number;
}

export interface CourseCreateRequest {
  name: string;
  code?: string;
  description?: string;
  coverUrl?: string;
  semester?: string;
}

export type CourseStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED' | 'DRAFT' | number;

export interface Course {
  id: number;
  title: string;
  name?: string;
  code?: string;
  cover?: string;
  coverUrl?: string;
  teacherId?: number;
  teacherName: string;
  teacherAvatar?: string;
  semester?: string;
  studentCount: number;
  chapterCount: number;
  knowledgePointCount?: number;
  resourceCount?: number;
  category?: string;
  credits?: number;
  plannedHours?: number;
  aiPersona?: string;
  welcomeMessage?: string;
  progress?: number;
  aiUsageCount?: number;
  status: CourseStatus;
  description?: string;
  createdAt?: string;
  knowledgeBaseId?: number;
  /** 当前用户是否可维护本课程（主讲或本课助教） */
  editable?: boolean;
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
  category?: string;
  credits?: number;
  plannedHours?: number;
  knowledgeBaseId?: number;
  aiPersona?: string;
  welcomeMessage?: string;
  initialChapters?: string[];
}

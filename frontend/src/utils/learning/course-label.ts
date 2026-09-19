import type { Course } from '@/types/course/course';

export function courseLabel(c: Pick<Course, 'id' | 'title' | 'name'>): string {
  const title = c.title || c.name;
  if (title && String(title).trim()) {
    return String(title).trim();
  }
  return `课程 #${c.id}`;
}

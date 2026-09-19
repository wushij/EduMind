export type CourseTab = 'overview' | 'resources' | 'ai' | 'chapters';

export function resolveCourseRoute(courseId: number, tab: CourseTab = 'overview'): string {
  return `/course/${courseId}/${tab}`;
}

/** 后端 CourseVO 使用 name，前端部分页面使用 title，统一展示名 */
export function getCourseDisplayName(course: {
  id?: number | string;
  title?: string;
  name?: string;
}): string {
  const label = (course.title || course.name || '').trim();
  if (label) return label;
  if (course.id !== undefined && course.id !== null && course.id !== '') {
    return `课程 #${course.id}`;
  }
  return '未命名课程';
}

export function normalizeCourseFromApi<T extends Record<string, unknown>>(course: T): T & { title: string } {
  const title = getCourseDisplayName(course as { id?: number | string; title?: string; name?: string });
  return { ...course, title };
}

export function normalizeCourseListFromApi(list: Record<string, unknown>[]) {
  return list.map((item) => normalizeCourseFromApi(item));
}

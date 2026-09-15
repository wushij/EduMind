export interface LessonPlanRequest {
  courseId: number;
  topic: string;
  hours?: number;
  objectives?: string;
}

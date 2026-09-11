export interface Chapter {
  id: number;
  courseId?: number;
  title: string;
  orderNum?: number;
  sort?: number;
  description?: string;
  sections?: Array<{
    id: number;
    title: string;
    description?: string;
    completed?: boolean;
    duration?: string;
    knowledgePointCount?: number;
    type?: string;
  }>;
  children?: Chapter[];
}

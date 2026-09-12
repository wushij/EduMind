export interface LearningStep {
  stage: string;
  title: string;
  status: 'DONE' | 'DOING' | 'TODO';
}

export interface LearningPathTask {
  title: string;
  type: string;
  refId?: number;
  status: string;
}

export interface LearningPathWeek {
  weekNo: number;
  theme: string;
  tasks: LearningPathTask[];
}

export interface LearningPathVO {
  courseId: number;
  title: string;
  weeks: LearningPathWeek[];
}

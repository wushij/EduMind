export interface LearningStep {
  stage: string;
  title: string;
  status: 'DONE' | 'DOING' | 'TODO';
}

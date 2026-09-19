import { describe, it, expect } from 'vitest';
import { SUBMISSION_STATUS } from '@/constants/question/assignment';

describe('useLearningTasks mapping', () => {
  it('treats GRADED and REVIEWED as completed', () => {
    const doneStatuses = [SUBMISSION_STATUS.GRADED, SUBMISSION_STATUS.REVIEWED];
    expect(doneStatuses).toContain('GRADED');
    expect(doneStatuses).toContain('REVIEWED');
  });
});

import { describe, it, expect } from 'vitest';
import {
  getDistanceToBottom,
  shouldPauseAutoFollow,
  shouldResumeAutoFollow,
  SCROLL_PAUSE_DISTANCE,
  SCROLL_RESUME_DISTANCE
} from './stream-scroll';

describe('stream-scroll', () => {
  it('computes distance to bottom from scroll metrics', () => {
    const el = {
      scrollHeight: 1000,
      scrollTop: 400,
      clientHeight: 500
    } as HTMLElement;
    expect(getDistanceToBottom(el)).toBe(100);
  });

  it('uses shared thresholds for pause and resume', () => {
    expect(shouldPauseAutoFollow(SCROLL_PAUSE_DISTANCE + 1)).toBe(true);
    expect(shouldPauseAutoFollow(SCROLL_PAUSE_DISTANCE)).toBe(false);
    expect(shouldResumeAutoFollow(SCROLL_RESUME_DISTANCE - 1)).toBe(true);
    expect(shouldResumeAutoFollow(SCROLL_RESUME_DISTANCE)).toBe(false);
  });
});

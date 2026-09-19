import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { defineComponent, ref } from 'vue';
import { mount } from '@vue/test-utils';
import { useAiThinkingTimer } from './useAiThinkingTimer';

describe('useAiThinkingTimer', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function mountTimer(initialActive = true) {
    const active = ref(initialActive);
    const wrapper = mount(
      defineComponent({
        setup() {
          const timer = useAiThinkingTimer(active);
          return { active, ...timer };
        },
        template: '<div />'
      })
    );
    return { wrapper, active };
  }

  it('starts at 0.0s and advances when active', async () => {
    const { wrapper } = mountTimer(true);
    expect(wrapper.vm.elapsedTimeText).toBe('0.0s');

    vi.advanceTimersByTime(1000);
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.elapsedTimeText).toBe('1.0s');
    expect(wrapper.vm.currentStep).toBe(2);
  });

  it('stops updating when active becomes false', async () => {
    const { wrapper, active } = mountTimer(true);
    vi.advanceTimersByTime(500);
    await wrapper.vm.$nextTick();
    active.value = false;
    await wrapper.vm.$nextTick();
    vi.advanceTimersByTime(2000);
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.elapsedTimeText).toBe('0.5s');
  });
});

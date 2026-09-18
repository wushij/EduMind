import { describe, expect, it, vi, beforeEach } from 'vitest';
import { defineComponent, ref, reactive } from 'vue';
import { mount } from '@vue/test-utils';
import { useLessonStudioCopilotBridge } from '@/composables/course/useLessonStudioCopilotBridge';

describe('useLessonStudioCopilotBridge', () => {
  const insertSnippetAtCursor = vi.fn();
  const rollbackLastSnippet = vi.fn();

  beforeEach(() => {
    insertSnippetAtCursor.mockClear();
    rollbackLastSnippet.mockClear();
  });

  function mountBridge(options: Parameters<typeof useLessonStudioCopilotBridge>[0]) {
    const Comp = defineComponent({
      setup() {
        useLessonStudioCopilotBridge(options);
        return () => null;
      }
    });
    return mount(Comp);
  }

  it('applies title and supports rollback via events', () => {
    const meta = reactive({ title: '原标题', description: '', durationMinutes: 30, lessonType: 'LECTURE' });
    const studio = reactive({
      mainMarkdown: '',
      objectiveCallout: { title: '目标', body: '' },
      knowledgePointIds: [] as number[]
    });
    const editorRef = ref({
      getSelectedText: () => '',
      focusEditor: () => {},
      insertSnippetAtCursor,
      rollbackLastSnippet
    });
    const onDirty = vi.fn();

    const wrapper = mountBridge({
      meta,
      studio,
      knowledgePoints: ref([]),
      editorRef,
      onDirty
    });

    window.dispatchEvent(
      new CustomEvent('apply-lesson-title', { detail: { title: '新标题' } })
    );
    expect(meta.title).toBe('新标题');
    expect(onDirty).toHaveBeenCalled();

    window.dispatchEvent(new CustomEvent('rollback-lesson-title'));
    expect(meta.title).toBe('原标题');
    wrapper.unmount();
  });

  it('inserts markdown snippet through editor expose', () => {
    const meta = reactive({ title: 't', description: '', durationMinutes: 30, lessonType: 'LECTURE' });
    const studio = reactive({
      mainMarkdown: 'hello',
      objectiveCallout: { title: '目标', body: '' },
      knowledgePointIds: [] as number[]
    });
    const editorRef = ref({
      getSelectedText: () => '',
      focusEditor: () => {},
      insertSnippetAtCursor,
      rollbackLastSnippet
    });

    const wrapper = mountBridge({
      meta,
      studio,
      knowledgePoints: ref([]),
      editorRef,
      onDirty: () => {}
    });

    window.dispatchEvent(
      new CustomEvent('insert-lesson-markdown-snippet', { detail: { text: '段落' } })
    );
    expect(insertSnippetAtCursor).toHaveBeenCalledWith('\n\n段落\n\n');
    wrapper.unmount();
  });
});

import { describe, expect, it, vi, beforeEach } from 'vitest';
import { defineComponent, ref, reactive } from 'vue';
import { mount } from '@vue/test-utils';
import { ElMessage } from 'element-plus';
import { useLessonStudioCopilotBridge } from '@/composables/course/useLessonStudioCopilotBridge';
import type { KnowledgePoint } from '@/types/course/knowledge-point';

vi.mock('element-plus', () => {
  const message = Object.assign(vi.fn(), {
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  });
  return { ElMessage: message };
});

const messageMock = ElMessage as unknown as ReturnType<typeof vi.fn>;

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

  it('重复应用同一批考点建议时只写入一次，并提示已在关联列表中', () => {
    messageMock.mockClear();
    const meta = reactive({ title: 't', description: '', durationMinutes: 30, lessonType: 'LECTURE' });
    const studio = reactive({
      mainMarkdown: '',
      objectiveCallout: { title: '目标', body: '' },
      knowledgePointIds: [] as number[]
    });
    const knowledgePoints = ref<KnowledgePoint[]>([
      { id: 1, title: '洛必达法则求未定式极限' },
      { id: 2, title: '两个重要极限及其应用' }
    ]);
    const onDirty = vi.fn();
    const editorRef = ref({
      getSelectedText: () => '',
      focusEditor: () => {},
      insertSnippetAtCursor,
      rollbackLastSnippet
    });

    const wrapper = mountBridge({ meta, studio, knowledgePoints, editorRef, onDirty });
    const detail = { tags: ['洛必达法则求未定式极限', '两个重要极限及其应用'] };

    window.dispatchEvent(new CustomEvent('apply-lesson-knowledge-suggestions', { detail }));
    expect(studio.knowledgePointIds).toEqual([1, 2]);
    expect(onDirty).toHaveBeenCalledTimes(1);

    // 备课对话里多条回答会推荐同一批考点：再次应用不应重复写入表单、重复标记脏数据
    window.dispatchEvent(new CustomEvent('apply-lesson-knowledge-suggestions', { detail }));
    expect(studio.knowledgePointIds).toEqual([1, 2]);
    expect(onDirty).toHaveBeenCalledTimes(1);
    expect(messageMock).toHaveBeenLastCalledWith(
      expect.objectContaining({ type: 'info', grouping: true })
    );

    wrapper.unmount();
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import { getCourseList } from '@/api/course/course';
import { getChapters } from '@/api/course/chapter';
import { getCourseKnowledgePoints } from '@/api/course/knowledge-point';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import { getDocuments } from '@/api/knowledge/document';
import { getChunks } from '@/api/knowledge/chunk';
import { getQuestions } from '@/api/question/question';
import { PROMPT_DEMO_SNAPSHOT } from '@/constants/system/prompt-demo';
import { applyPromptDemoVariables, resolvePromptDemoFill } from './usePromptDemoData';

vi.mock('@/api/course/course', () => ({ getCourseList: vi.fn() }));
vi.mock('@/api/course/chapter', () => ({ getChapters: vi.fn() }));
vi.mock('@/api/course/knowledge-point', () => ({ getCourseKnowledgePoints: vi.fn() }));
vi.mock('@/api/knowledge/knowledge-base', () => ({ getKnowledgeBases: vi.fn() }));
vi.mock('@/api/knowledge/document', () => ({ getDocuments: vi.fn() }));
vi.mock('@/api/knowledge/chunk', () => ({ getChunks: vi.fn() }));
vi.mock('@/api/question/question', () => ({ getQuestions: vi.fn() }));

const mockCourseList = getCourseList as unknown as Mock;
const mockChapters = getChapters as unknown as Mock;
const mockKnowledgePoints = getCourseKnowledgePoints as unknown as Mock;
const mockKnowledgeBases = getKnowledgeBases as unknown as Mock;
const mockDocuments = getDocuments as unknown as Mock;
const mockChunks = getChunks as unknown as Mock;
const mockQuestions = getQuestions as unknown as Mock;

describe('resolvePromptDemoFill', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('接口不可用时静默回落种子快照（课程 ID 必须是库里真实存在的 ID）', async () => {
    mockCourseList.mockRejectedValue(new Error('403 没有相关权限'));

    const fill = await resolvePromptDemoFill();

    expect(fill.source).toBe('snapshot');
    expect(fill.variables.course_id).toBe(String(PROMPT_DEMO_SNAPSHOT.courseId));
    expect(fill.variables.course_name).toBe('Java面向对象程序设计');
    expect(fill.variables.chapter_id).toBe('10');
    expect(fill.variables.knowledge_point_ids).toBe('15,16');
    expect(fill.variables.question_id).toBe('1007');
    expect(fill.variables.retrieved_context).toContain('Java面向对象编程实战教程.docx');
    expect(fill.label).toContain('种子快照');
  });

  it('课程列表为空时同样回落种子快照，避免填入空课程', async () => {
    mockCourseList.mockResolvedValue({ data: { list: [] } });

    const fill = await resolvePromptDemoFill();

    expect(fill.source).toBe('snapshot');
    expect(fill.variables.course_id).toBe(String(PROMPT_DEMO_SNAPSHOT.courseId));
  });

  it('按真实课程 / 章节 / 知识点 / 知识库 / 题库解析演示数据', async () => {
    mockCourseList.mockResolvedValue({
      data: { list: [{ id: 101, title: '数据结构与算法', description: '计算机核心专业课。' }] }
    });
    mockChapters.mockResolvedValue({
      data: [
        { id: 3, title: '第二章 线性表结构', children: [{ id: 5, title: '2.2 链式存储与单双向链表' }] }
      ]
    });
    mockKnowledgePoints.mockResolvedValue({
      data: [
        { id: 11, title: '顺序表插入与删除时间开销', chapterId: 4 },
        { id: 12, title: '单链表就地逆置算法', chapterId: 5 }
      ]
    });
    mockKnowledgeBases.mockResolvedValue({
      data: [{ id: 1, name: '数据结构与算法专业知识库' }]
    });
    mockDocuments.mockResolvedValue({
      data: [
        { id: 1, fileName: '数据结构第二章-线性表与链表深度解析.pdf', parseStatus: 'SUCCESS' }
      ]
    });
    mockChunks.mockResolvedValue([
      {
        id: 1,
        documentId: 1,
        content: '【线性表定义与特征】线性表是具有相同数据类型的 n 个数据元素的有限序列。',
        tokenCount: 32,
        chunkIndex: 0,
        status: 'INDEXED'
      }
    ]);
    mockQuestions.mockResolvedValue({
      data: {
        list: [
          {
            id: 1210,
            courseId: 101,
            knowledgePointId: 12,
            type: 'SHORT_ANSWER',
            stem: '简述单链表就地逆置算法的核心思想。',
            options: '[]',
            answer: '三指针迭代反转指针朝向，时间 O(n)，空间 O(1)。',
            analysis: '单次遍历链表并用常数个辅助指针完成逆置。',
            difficulty: 3,
            score: 15
          }
        ]
      }
    });

    const fill = await resolvePromptDemoFill();

    expect(fill.source).toBe('remote');
    expect(fill.variables.course_id).toBe('101');
    expect(fill.variables.course_name).toBe('数据结构与算法');
    expect(fill.variables.chapter_id).toBe('5');
    expect(fill.variables.chapter_name).toBe('2.2 链式存储与单双向链表');
    expect(fill.variables.knowledge_point_ids).toBe('12');
    expect(fill.variables.knowledge_point_names).toBe('单链表就地逆置算法');
    expect(fill.variables.knowledge_base_name).toBe('数据结构与算法专业知识库');
    expect(fill.variables.retrieved_context).toContain('数据结构第二章-线性表与链表深度解析.pdf');
    expect(fill.variables.question_id).toBe('1210');
    expect(fill.variables.max_score).toBe('15');
    expect(fill.variables.reference_answer).toContain('三指针');
    // 非种子课程时示例提问 / 学生作答必须跟随真实课程与题目改写
    expect(fill.variables.question).toContain('数据结构与算法');
    expect(fill.variables.question).toContain('单链表就地逆置算法');
    expect(fill.variables.lesson_title).toBe('单链表就地逆置算法');
    expect(fill.variables.student_answer).toContain('仅复述结论');
    expect(JSON.parse(fill.variables.existing_questions)).toEqual(['简述单链表就地逆置算法的核心思想。']);
  });

  it('选择题选项会被归一化后拼回题干（后端下发的是 JSON 字符串）', async () => {
    mockCourseList.mockResolvedValue({ data: { list: [{ id: 102, title: 'Java面向对象程序设计' }] } });
    mockChapters.mockResolvedValue({ data: [{ id: 10, title: '2.1 封装、继承与多态机制' }] });
    mockKnowledgePoints.mockResolvedValue({
      data: [{ id: 16, title: 'ArrayList 与 LinkedList 源码剖析', chapterId: 10 }]
    });
    mockKnowledgeBases.mockRejectedValue(new Error('500'));
    mockDocuments.mockRejectedValue(new Error('500'));
    mockChunks.mockRejectedValue(new Error('500'));
    mockQuestions.mockResolvedValue({
      data: {
        list: [
          {
            id: 1007,
            courseId: 102,
            knowledgePointId: 16,
            type: 'SINGLE_CHOICE',
            stem: '关于 ArrayList 与 LinkedList，正确的是（ ）。',
            options: JSON.stringify([{ key: 'A', content: '随机访问 O(1)' }]),
            answer: 'A',
            analysis: 'ArrayList 底层为 Object[] 数组。',
            difficulty: 2,
            score: 5
          }
        ]
      }
    });

    const fill = await resolvePromptDemoFill();

    expect(fill.variables.question_stem).toContain('A. 随机访问 O(1)');
    expect(fill.variables.question_type).toBe('SINGLE_CHOICE');
    expect(fill.variables.scoring_points).toBe(PROMPT_DEMO_SNAPSHOT.questionScoringPoints);
    // 知识库接口失败时 RAG 上下文回落快照，但其余字段仍来自真实课程
    expect(fill.variables.retrieved_context).toContain('<rag_context>');
  });
});

describe('applyPromptDemoVariables', () => {
  it('只写入模板真正声明的插槽，并返回命中数量', () => {
    const target: Record<string, string> = { question: '旧值' };
    const filled = applyPromptDemoVariables(
      target,
      { question: '新值', course_name: 'Java面向对象程序设计', unused: 'x' },
      ['question', 'course_name', 'missing']
    );

    expect(filled).toBe(2);
    expect(target.question).toBe('新值');
    expect(target.course_name).toBe('Java面向对象程序设计');
    expect(target.missing).toBeUndefined();
    expect(target.unused).toBeUndefined();
  });
});

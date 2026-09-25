import type { V05Alternative, V05ToolNoticeConfig } from '@/types/ai/v05-notice';

const DEFAULT_ALTERNATIVES: V05Alternative[] = [
  { label: '课程 AI 问答', route: '/course/101/ai' },
  { label: '学习推荐', route: '/learning/recommendations' }
];

export const V05_TOOL_NOTICES: Record<string, V05ToolNoticeConfig> = {
  tool_summary: {
    title: 'AI 课程总结',
    description:
      'V0.5 已上线：上传课件或长文档，自动提炼章节要点与复习精要。',
    alternatives: [
      { label: '打开 AI 总结', route: '/ai/summary' },
      { label: '知识库文档', route: '/knowledge' }
    ]
  },
  tool_wrong_analysis: {
    title: 'AI 错题分析',
    description:
      'V0.5 将结合学生作答记录自动归因错因，并推荐变式巩固练习。',
    alternatives: [
      { label: '学习推荐', route: '/learning/recommendations' },
      { label: 'AI 智能批改', route: '/ai/grading' }
    ]
  },
  tool_knowledge_explain: {
    title: 'AI 知识点讲解',
    description:
      'V0.5 将提供苏格拉底式追问与由浅入深的概念讲解，帮助学生建立直觉理解。',
    alternatives: [
      { label: '课程 AI 问答', route: '/course/101/ai' },
      { label: '学习推荐', route: '/learning/recommendations' }
    ]
  },
  tool_learning_plan: {
    title: 'AI 学习计划',
    description:
      'V0.5 将基于学情画像自动生成周计划与每日复习任务清单。',
    alternatives: [
      { label: '学习中心', route: '/learning' },
      { label: '学习推荐', route: '/learning/recommendations' }
    ]
  },
  tool_ppt: {
    title: 'AI PPT 生成',
    description:
      'V0.5 将支持根据教学大纲生成课件页结构与演讲备注。',
    alternatives: [
      { label: '课程 AI 助手', route: '/course/ai' },
      { label: '知识库', route: '/knowledge' }
    ]
  },
  tool_polish: {
    title: 'AI 教学文本润色',
    description:
      'V0.5 将支持在抽屉内输入题干或教案文本，流式返回润色建议与修订稿。',
    alternatives: [
      { label: 'AI 智能出题', route: '/ai/question/generate' },
      { label: '课程 AI 问答', route: '/course/101/ai' }
    ]
  },
  tool_translate: {
    title: 'AI 双语专业翻译',
    description:
      'V0.5 将支持中英术语对照翻译，保持计算机与专业课词汇一致性。',
    alternatives: DEFAULT_ALTERNATIVES
  }
};

export function getV05ToolNotice(toolId: string): V05ToolNoticeConfig {
  return (
    V05_TOOL_NOTICES[toolId] ?? {
      title: 'AI 工具',
      description: '该工具完整能力将于 V0.5 正式上线，敬请期待。',
      alternatives: DEFAULT_ALTERNATIVES
    }
  );
}

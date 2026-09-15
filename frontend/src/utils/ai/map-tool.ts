import type { AITool } from '@/types/ai/tool';
import type { IconTheme } from '@/types/common/icon';

interface ToolStyleConfig {
  iconName: string;
  theme: IconTheme;
  gradient: string;
}

const TOOL_STYLE_MAP: Record<string, ToolStyleConfig> = {
  tool_course_tutor: {
    iconName: 'Service',
    theme: 'blue',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)'
  },
  tool_question_gen: {
    iconName: 'EditPen',
    theme: 'cyan',
    gradient: 'linear-gradient(135deg, #06B6D4 0%, #0284C7 100%)'
  },
  tool_exam_gen: {
    iconName: 'Tickets',
    theme: 'purple',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%)'
  },
  tool_practice_gen: {
    iconName: 'Reading',
    theme: 'amber',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)'
  },
  tool_grading: {
    iconName: 'CircleCheck',
    theme: 'emerald',
    gradient: 'linear-gradient(135deg, #10B981 0%, #059669 100%)'
  },
  tool_lesson_plan: {
    iconName: 'Notebook',
    theme: 'rose',
    gradient: 'linear-gradient(135deg, #F43F5E 0%, #BE123C 100%)'
  },
  tool_doc_summary: {
    iconName: 'DocumentCopy',
    theme: 'teal',
    gradient: 'linear-gradient(135deg, #14B8A6 0%, #0D9488 100%)'
  },
  tool_knowledge_graph: {
    iconName: 'Connection',
    theme: 'indigo',
    gradient: 'linear-gradient(135deg, #6366F1 0%, #4338CA 100%)'
  },
  tool_code_analysis: {
    iconName: 'Cpu',
    theme: 'cyan',
    gradient: 'linear-gradient(135deg, #38BDF8 0%, #0284C7 100%)'
  }
};

const FALLBACK_PALETTES: ToolStyleConfig[] = [
  { iconName: 'MagicStick', theme: 'blue', gradient: 'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)' },
  { iconName: 'Tickets', theme: 'cyan', gradient: 'linear-gradient(135deg, #06B6D4 0%, #0284C7 100%)' },
  { iconName: 'CircleCheck', theme: 'emerald', gradient: 'linear-gradient(135deg, #10B981 0%, #059669 100%)' },
  { iconName: 'Reading', theme: 'amber', gradient: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)' },
  { iconName: 'Notebook', theme: 'purple', gradient: 'linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%)' },
  { iconName: 'DocumentCopy', theme: 'rose', gradient: 'linear-gradient(135deg, #F43F5E 0%, #BE123C 100%)' }
];

function resolveToolStyle(raw: Record<string, any>): ToolStyleConfig {
  const byId = TOOL_STYLE_MAP[raw.id];
  if (byId) return byId;

  const name = raw.name || '';
  if (name.includes('出题')) return TOOL_STYLE_MAP.tool_question_gen;
  if (name.includes('组卷')) return TOOL_STYLE_MAP.tool_exam_gen;
  if (name.includes('助教') || name.includes('问答')) return TOOL_STYLE_MAP.tool_course_tutor;
  if (name.includes('批改')) return TOOL_STYLE_MAP.tool_grading;
  if (name.includes('教案')) return TOOL_STYLE_MAP.tool_lesson_plan;
  if (name.includes('总结') || name.includes('速记')) return TOOL_STYLE_MAP.tool_doc_summary;
  if (name.includes('练习') || name.includes('刷题')) return TOOL_STYLE_MAP.tool_practice_gen;
  if (name.includes('图谱')) return TOOL_STYLE_MAP.tool_knowledge_graph;
  if (name.includes('代码') || name.includes('编程')) return TOOL_STYLE_MAP.tool_code_analysis;

  if (raw.icon) {
    let iconName = raw.icon;
    if (iconName === 'Checked') iconName = 'CircleCheck';
    return {
      iconName,
      theme: 'blue',
      gradient: 'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)'
    };
  }

  const hash = String(raw.id || raw.name || '')
    .split('')
    .reduce((acc, c) => acc + c.charCodeAt(0), 0);
  return FALLBACK_PALETTES[hash % FALLBACK_PALETTES.length];
}

function parseTags(rawTags: any): string[] {
  if (Array.isArray(rawTags)) {
    return rawTags.map(String).filter(Boolean);
  }
  if (typeof rawTags === 'string') {
    return rawTags
      .split(/[,，;；\s]+/)
      .map(t => t.trim())
      .filter(Boolean);
  }
  return [];
}

export function mapTool(raw: Record<string, any>): AITool {
  const category = raw.category || 'GENERAL';
  const categoryLabels: Record<string, string> = {
    TEACHER: '教师提效',
    STUDENT: '学生助学',
    GENERAL: '通用工具'
  };

  const style = resolveToolStyle(raw);
  const executionMode = raw.executionMode === 'V05_NOTICE' ? 'V05_NOTICE' : 'ROUTE';

  return {
    id: raw.id,
    name: raw.name,
    category,
    categoryLabel: categoryLabels[category] || category,
    description: raw.description || '',
    detailedIntro: raw.detailedIntro || raw.description || '',
    iconBg: style.gradient,
    iconName: style.iconName,
    iconTheme: style.theme,
    iconEmoji: '',
    modelId: raw.modelId || raw.model_id || '',
    route: raw.route || '/',
    executionMode,
    tags: parseTags(raw.tags),
    isRecommended: !!raw.isRecommended,
    isHot: !!raw.isHot,
    isFavorite: !!raw.isFavorite,
    usageCount: raw.useCount ?? raw.usageCount ?? 0
  };
}

/** 课程 AI 助教人设：全站统一使用小写 canonical id（与建课、数据库一致） */

export type CourseAiPersonaId = 'socrates' | 'academic' | 'engineer' | 'gentle';

export interface CourseAiPersonaOption {
  id: CourseAiPersonaId;
  name: string;
  desc: string;
}

/** 编辑抽屉 / 建课等人设卡片共用 */
export const COURSE_AI_PERSONA_OPTIONS: readonly CourseAiPersonaOption[] = [
  {
    id: 'socrates',
    name: '苏格拉底启发式助教',
    desc: '善于通过多轮递进发问引导学生自主推导答案，培养批判性思维'
  },
  {
    id: 'academic',
    name: '严谨治学学术导师',
    desc: '注重数学严密性、定理定义准确性与学术范式规范'
  },
  {
    id: 'engineer',
    name: '一线工程实战导师',
    desc: '从工业界工程踩坑、并发高可用、代码架构等视角深度剖析'
  },
  {
    id: 'gentle',
    name: '温和鼓励引路人',
    desc: '通俗易懂拆解晦涩概念，步步正向激励，适合初学者破冰'
  }
];

const LEGACY_PERSONA_MAP: Record<string, CourseAiPersonaId> = {
  SOCRATIC: 'socrates',
  STRICT: 'academic',
  PRACTICAL: 'engineer',
  GENTLE: 'gentle',
  socrates: 'socrates',
  academic: 'academic',
  engineer: 'engineer',
  gentle: 'gentle'
};

const VALID_IDS = new Set<CourseAiPersonaId>(COURSE_AI_PERSONA_OPTIONS.map(p => p.id));

export function normalizeCourseAiPersona(raw?: string | null): CourseAiPersonaId {
  if (raw == null || !String(raw).trim()) {
    return 'socrates';
  }
  const key = String(raw).trim();
  const mapped = LEGACY_PERSONA_MAP[key] ?? LEGACY_PERSONA_MAP[key.toUpperCase()];
  if (mapped) {
    return mapped;
  }
  const lower = key.toLowerCase() as CourseAiPersonaId;
  if (VALID_IDS.has(lower)) {
    return lower;
  }
  return 'socrates';
}

export function getCourseAiPersonaLabel(id?: string | null): string {
  const canonical = normalizeCourseAiPersona(id);
  const found = COURSE_AI_PERSONA_OPTIONS.find(p => p.id === canonical);
  return found?.name ?? '苏格拉底启发式助教';
}

export function getCourseAiPersonaWelcomeFallback(
  personaId: CourseAiPersonaId,
  courseName: string
): string {
  const name = courseName?.trim() || '本课程';
  switch (personaId) {
    case 'academic':
      return `同学你好！我是《${name}》学术助教。我将为你提供严谨的概念辨析、定理推导与学术论文溯源。`;
    case 'engineer':
      return `嗨！我是《${name}》实战导师。代码是运行出来的，遇到任何运行报错或架构疑惑，随时把代码发给我！`;
    case 'gentle':
      return `同学你好！我是《${name}》的 AI 助教。学习路上不必焦虑，我会用通俗的方式陪你一步步掌握每个知识点。`;
    default:
      return `同学你好！我是《${name}》苏格拉底启发式 AI 助教。学习不是被动接受，让我们通过层层深入的提问，共同探索知识的底层逻辑！`;
  }
}

export function getCourseAiPersonaPromptPrefix(persona?: string | null): string {
  const id = normalizeCourseAiPersona(persona);
  if (id === 'academic') {
    return '[助教风格: 严谨学术推导型，强调数理严密性与学术定理推导] ';
  }
  if (id === 'engineer') {
    return '[助教风格: 工程实战导师型，强调工业落地、代码排错与系统架构] ';
  }
  if (id === 'gentle') {
    return '[助教风格: 温和鼓励引路人，通俗拆解概念并正向激励] ';
  }
  return '[助教风格: 苏格拉底启发型，善用反问引导、步步启发自主思考] ';
}

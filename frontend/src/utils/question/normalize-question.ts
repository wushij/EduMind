import type { Difficulty, Question, QuestionOption, QuestionType } from '@/types/question/question';
import { normalizeMathTextNewlines } from '@/utils/format/render-math';

const TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
};

const DIFFICULTY_LABELS: Record<Difficulty, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
};

function mapDifficulty(raw: unknown): Difficulty {
  if (typeof raw === 'string') {
    const upper = raw.toUpperCase();
    if (upper === 'EASY' || upper === 'MEDIUM' || upper === 'HARD') {
      return upper as Difficulty;
    }
  }
  const level = Number(raw);
  if (!Number.isFinite(level)) return 'MEDIUM';
  if (level <= 2) return 'EASY';
  if (level <= 3) return 'MEDIUM';
  return 'HARD';
}

function splitAnswerKeys(correctAnswer?: string): Set<string> {
  return new Set(
    String(correctAnswer || '')
      .split(/[,，、|]/)
      .map((item) => item.trim())
      .filter(Boolean)
  );
}

function optionsFromKeyValueObject(obj: Record<string, unknown>, correctAnswer?: string): QuestionOption[] {
  const answerKeys = splitAnswerKeys(correctAnswer);
  const keys = Object.keys(obj)
    .filter((k) => /^[A-Ha-h]$/.test(k.trim()))
    .sort((a, b) => a.toUpperCase().localeCompare(b.toUpperCase()));
  if (keys.length < 2) {
    return [];
  }
  return keys.map((k) => {
    const key = k.toUpperCase();
    return {
      key,
      content: String(obj[k] ?? ''),
      isCorrect: answerKeys.has(key)
    };
  });
}

function normalizeOptionArray(options: unknown[], correctAnswer?: string): QuestionOption[] {
  const answerKeys = splitAnswerKeys(correctAnswer);

  return options.map((item, index) => {
    if (typeof item === 'string') {
      const key = String.fromCharCode(65 + index);
      return {
        key,
        content: item,
        isCorrect: answerKeys.has(key)
      };
    }

    if (item && typeof item === 'object') {
      const opt = item as Record<string, unknown>;
      const key = String(opt.key ?? opt.label ?? String.fromCharCode(65 + index));
      const content = String(
        opt.content ?? opt.text ?? opt.value ?? opt.option ?? opt.desc ?? opt.label ?? ''
      );
      return {
        key,
        content,
        isCorrect: Boolean(opt.isCorrect ?? answerKeys.has(key))
      };
    }

    const key = String.fromCharCode(65 + index);
    return { key, content: '', isCorrect: answerKeys.has(key) };
  });
}

export function parseQuestionOptions(raw: unknown, correctAnswer?: string): QuestionOption[] {
  if (Array.isArray(raw)) {
    return normalizeOptionArray(raw, correctAnswer);
  }

  if (raw && typeof raw === 'object') {
    const fromObj = optionsFromKeyValueObject(raw as Record<string, unknown>, correctAnswer);
    if (fromObj.length >= 2) {
      return fromObj;
    }
  }

  if (typeof raw === 'string' && raw.trim()) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return normalizeOptionArray(parsed, correctAnswer);
      }
      if (parsed && typeof parsed === 'object') {
        const fromObj = optionsFromKeyValueObject(parsed as Record<string, unknown>, correctAnswer);
        if (fromObj.length >= 2) {
          return fromObj;
        }
      }
    } catch {
      try {
        const sanitized = raw.replace(/\\(?!["\\/bfnrt]|u[0-9a-fA-F]{4})/g, '\\\\');
        const parsed = JSON.parse(sanitized);
        if (Array.isArray(parsed)) {
          return normalizeOptionArray(parsed, correctAnswer);
        }
        if (parsed && typeof parsed === 'object') {
          const fromObj = optionsFromKeyValueObject(parsed as Record<string, unknown>, correctAnswer);
          if (fromObj.length >= 2) {
            return fromObj;
          }
        }
      } catch {
        return [];
      }
      return [];
    }
  }

  return [];
}

/** 至少 2 个选项且 content 非空 */
export function hasFilledQuestionOptions(options?: QuestionOption[]): boolean {
  if (!options?.length) return false;
  return options.filter((o) => String(o.content || '').trim().length > 0).length >= 2;
}

export function mergeQuestionAnalysisText(analysis?: string, distractorAnalysis?: string): string {
  const a = String(analysis || '').trim();
  const d = String(distractorAnalysis || '').trim();
  if (!d) return a;
  if (!a) return d;
  if (a.includes(d)) return a;
  return `${a}\n\n${d}`;
}

/**
 * 归一化题目主键：edu_question.id 是 19 位雪花 ID，超出 JS 安全整数范围，
 * 直接 Number() 会静默丢精度，回传后端就匹配不到真实题目（试卷保存冲突、详情题干空白）。
 * 后端已将其序列化为字符串，这里只在安全范围内还原成数字，超出则原样保留字符串。
 */
function normalizeQuestionId(raw: unknown): number | string {
  if (raw === null || raw === undefined || raw === '') return 0;
  if (typeof raw === 'number') return raw;
  const text = String(raw).trim();
  if (!/^\d+$/.test(text)) return text;
  const asNumber = Number(text);
  return Number.isSafeInteger(asNumber) ? asNumber : text;
}

export function normalizeQuestion(raw: any): Question {
  if (!raw || typeof raw !== 'object') {
    return {
      id: 0,
      courseId: 0,
      type: 'SINGLE_CHOICE',
      difficulty: 'MEDIUM',
      score: 5,
      stem: '',
      correctAnswer: '',
      analysis: '',
      knowledgePointNames: []
    };
  }
  const correctAnswer = String(raw.correctAnswer ?? raw.answer ?? '');
  const difficulty = mapDifficulty(raw.difficulty);
  const options = parseQuestionOptions(
    raw.options ?? raw.choices ?? raw.optionList,
    correctAnswer
  );
  const type = String(raw.type || 'SINGLE_CHOICE') as QuestionType;

  return {
    id: normalizeQuestionId(raw.id),
    courseId: Number(raw.courseId || 0),
    courseName: raw.courseName ? String(raw.courseName) : undefined,
    chapterId: raw.chapterId != null ? Number(raw.chapterId) : undefined,
    chapterName: raw.chapterName ? String(raw.chapterName) : undefined,
    type,
    typeLabel: raw.typeLabel ? String(raw.typeLabel) : TYPE_LABELS[type],
    difficulty,
    difficultyLabel: raw.difficultyLabel ? String(raw.difficultyLabel) : DIFFICULTY_LABELS[difficulty],
    score: Number(raw.score ?? 5),
    stem: String(raw.stem || ''),
    options,
    correctAnswer,
    analysis: normalizeMathTextNewlines(
      mergeQuestionAnalysisText(String(raw.analysis || ''), String(raw.distractorAnalysis || ''))
    ),
    knowledgePointNames: Array.isArray(raw.knowledgePointNames)
      ? raw.knowledgePointNames.map(String)
      : raw.knowledgePointName
        ? [String(raw.knowledgePointName)]
        : [],
    knowledgePointId: raw.knowledgePointId != null ? Number(raw.knowledgePointId) : undefined,
    cognitiveLevel: raw.cognitiveLevel ? String(raw.cognitiveLevel) : undefined,
    distractorAnalysis: raw.distractorAnalysis ? String(raw.distractorAnalysis) : undefined,
    createdAt: String(raw.createdAt ?? raw.createTime ?? '')
  };
}

export function normalizeQuestionList(list: any[] = []): Question[] {
  if (!Array.isArray(list)) return [];
  return list.map(normalizeQuestion);
}

/** 前端 Difficulty / 数字 → 后端 1–5 难度等级 */
export function difficultyToApiLevel(difficulty?: Difficulty | string | number | null): number {
  if (typeof difficulty === 'number' && Number.isFinite(difficulty)) {
    const n = Math.round(difficulty);
    if (n >= 1 && n <= 5) return n;
  }
  if (typeof difficulty === 'string') {
    const upper = difficulty.toUpperCase();
    if (upper === 'EASY') return 2;
    if (upper === 'HARD') return 4;
    if (upper === 'MEDIUM') return 3;
    const parsed = Number(difficulty);
    if (Number.isFinite(parsed) && parsed >= 1 && parsed <= 5) return Math.round(parsed);
  }
  return 3;
}

export function serializeQuestionOptionsForApi(options?: QuestionOption[] | string | null): string {
  if (typeof options === 'string') return options;
  if (Array.isArray(options)) return JSON.stringify(options);
  return '[]';
}

/** 创建/更新试题时对齐 QuestionCreateDTO / QuestionUpdateDTO */
export function serializeQuestionForApi(data: Partial<Question>): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    stem: data.stem ?? '',
    type: data.type,
    options: serializeQuestionOptionsForApi(data.options),
    answer: String(data.correctAnswer ?? (data as { answer?: string }).answer ?? '').trim(),
    analysis: data.analysis ?? '',
    difficulty: difficultyToApiLevel(data.difficulty),
    score: data.score ?? 5
  };
  if (data.courseId != null && data.courseId !== '') {
    payload.courseId = Number(data.courseId);
  }
  if (data.knowledgePointId != null) {
    payload.knowledgePointId = Number(data.knowledgePointId);
  }
  if ((data as { bankId?: number }).bankId != null) {
    payload.bankId = Number((data as { bankId?: number }).bankId);
  }
  return payload;
}

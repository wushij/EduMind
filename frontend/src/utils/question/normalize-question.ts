import type { Difficulty, Question, QuestionOption, QuestionType } from '@/types/question/question';

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
      const content = String(opt.content ?? opt.text ?? opt.value ?? '');
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

  if (typeof raw === 'string' && raw.trim()) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return normalizeOptionArray(parsed, correctAnswer);
      }
    } catch {
      return [];
    }
  }

  return [];
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
  const options = parseQuestionOptions(raw.options, correctAnswer);
  const type = String(raw.type || 'SINGLE_CHOICE') as QuestionType;

  return {
    id: Number(raw.id || 0),
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
    analysis: String(raw.analysis || ''),
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

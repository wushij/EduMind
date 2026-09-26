/**
 * 学年 / 学期通用选项来源（课程开课学期、学期筛选、试卷考试学期共用）
 * ------------------------------------------------------------------
 * 这些学期列表此前被写死在 4 处（课程创建、课程编辑、课程列表筛选、试卷创建），
 * 跨年后就会出现「选不到新学期」「还停留在过期学期」的问题。
 * 这里按系统当前时间统一动态推导，页面只渲染选项，不再各自维护硬编码列表。
 */

export interface SemesterOption {
  label: string;
  value: string;
}

type SeasonKey = 'spring' | 'summer' | 'autumn';

/** 一个自然年内学期的先后顺序：春季 → 暑期实训专周 → 秋季 */
const SEASON_ORDER: readonly SeasonKey[] = ['spring', 'summer', 'autumn'];

const SEASON_SUFFIX: Record<SeasonKey, string> = {
  spring: '春季学期',
  summer: '暑期实训专周',
  autumn: '秋季学期'
};

/** 月份 → 学期季节：2~7 月为春季学期，8 月~次年 1 月为秋季学期 */
export function resolveSeasonByMonth(month: number): SeasonKey {
  return month >= 2 && month <= 7 ? 'spring' : 'autumn';
}

function formatSemesterLabel(year: number, season: SeasonKey): string {
  return `${year}年${SEASON_SUFFIX[season]}`;
}

/** 当前学期所属年份与季节（1 月归属上一年的秋季学期） */
function resolveCurrentSemester(date: Date = new Date()): { year: number; season: SeasonKey } {
  const month = date.getMonth() + 1;
  const season = resolveSeasonByMonth(month);
  const year = season === 'autumn' && month === 1 ? date.getFullYear() - 1 : date.getFullYear();
  return { year, season };
}

/** 当前开课学期（如 2026年秋季学期；1 月归属上一年的秋季学期） */
export function getCurrentSemester(date: Date = new Date()): string {
  const { year, season } = resolveCurrentSemester(date);
  return formatSemesterLabel(year, season);
}

/**
 * 课程开课学期下拉选项：以「当前学期」为首项，只向后覆盖未来学期，
 * 不再出现已经过期的历史学期（避免下拉框里冒出 2025 这类旧学年）。
 * 默认覆盖到下一学年结束，如当前为 2026年秋季学期 → 2026秋 / 2027春 / 2027暑 / 2027秋 共 4 项。
 */
export function buildSemesterOptions(date: Date = new Date(), futureYears = 1): SemesterOption[] {
  const { year: startYear, season: currentSeason } = resolveCurrentSemester(date);
  const currentIndex = SEASON_ORDER.indexOf(currentSeason);
  const labels: string[] = [];

  for (let y = startYear; y <= startYear + futureYears; y += 1) {
    SEASON_ORDER.forEach((season, index) => {
      // 起始年份里早于「当前学期」的季节（如秋季学期之前的春/暑期）直接跳过
      if (y === startYear && index < currentIndex) return;
      labels.push(formatSemesterLabel(y, season));
    });
  }

  return labels.map(label => ({ label, value: label }));
}

/** 学年起始年：8 月~次年 7 月记为一个完整学年 */
export function resolveAcademicStartYear(date: Date = new Date()): number {
  const month = date.getMonth() + 1;
  return month >= 8 ? date.getFullYear() : date.getFullYear() - 1;
}

/** 学年制学期文案：第一学期=秋季，第二学期=春季 */
function formatAcademicTerm(startYear: number, index: 1 | 2): SemesterOption {
  const orderText = index === 1 ? '一' : '二';
  return {
    label: `${startYear}-${startYear + 1} 第${orderText}学期`,
    value: `${startYear}-${startYear + 1}-${index}`
  };
}

/**
 * 试卷考试学期下拉选项（学年制，按时间倒序，最近的学期在前）：
 * 覆盖当前学年前后各 aroundYears 个学年。
 */
export function buildAcademicTermOptions(date: Date = new Date(), aroundYears = 1): SemesterOption[] {
  const startYear = resolveAcademicStartYear(date);
  const options: SemesterOption[] = [];
  for (let y = startYear + aroundYears; y >= startYear - aroundYears; y -= 1) {
    options.push(formatAcademicTerm(y, 2), formatAcademicTerm(y, 1));
  }
  return options;
}

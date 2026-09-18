export enum CourseStatus {
  DRAFT = 0,
  PUBLISHED = 1,
  ARCHIVED = 2
}

/** 新建 / 编辑课程时的学科专业快捷选项（不含自定义输入） */
export const COURSE_CATEGORY_PRESETS = [
  '计算机与软件',
  '人工智能与大模型',
  '数据科学与大数据',
  '电子与信息工程',
  '网络与信息安全',
  '机械与智能制造',
  '土木与建筑工程',
  '电气与自动化',
  '材料与化学工程',
  '能源与环境工程',
  '生物医药与生命科学',
  '数学与统计',
  '物理与地球科学',
  '经济金融与管理',
  '法学与公共管理',
  '教育学与心理学',
  '文学与新闻传播',
  '外国语',
  '艺术与设计',
  '体育与健康',
  '农林与食品科学',
  '交通运输与物流',
  '通识与素质教育'
] as const;

export type CourseCategoryPreset = (typeof COURSE_CATEGORY_PRESETS)[number];

export function isCourseCategoryPreset(value: string): value is CourseCategoryPreset {
  return (COURSE_CATEGORY_PRESETS as readonly string[]).includes(value);
}

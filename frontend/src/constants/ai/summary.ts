import type { Component } from 'vue';
import { Reading, List, Warning, Notebook } from '@element-plus/icons-vue';
import type { SummaryMode, SummarySourceType } from '@/types/ai/summary';

/** 总结模式选项（与后端 SummaryMode 枚举一一对应） */
export interface SummaryModeOption {
  value: SummaryMode;
  label: string;
  desc: string;
  icon: Component;
}

export const SUMMARY_MODE_OPTIONS: SummaryModeOption[] = [
  {
    value: 'OVERVIEW',
    label: '全文速览',
    desc: '一句话主旨 + 6~10 条核心要点，3 分钟建立整体认知',
    icon: Reading
  },
  {
    value: 'CHAPTER',
    label: '章节要点',
    desc: '沿用原章节层级，保留关键定义、公式与结论',
    icon: List
  },
  {
    value: 'MISTAKE',
    label: '易错清单',
    desc: '逐条梳理易错点：易错点 → 为什么错 → 正确做法',
    icon: Warning
  },
  {
    value: 'REVIEW',
    label: '复习精要',
    desc: '考前冲刺：必记结论、公式清单与记忆口诀',
    icon: Notebook
  }
];

/** 资料来源切换页签 */
export const SUMMARY_SOURCE_OPTIONS: Array<{ value: SummarySourceType; label: string }> = [
  { value: 'DOCUMENT', label: '知识库文档' },
  { value: 'TEXT', label: '粘贴文本' }
];

/** 文档解析就绪状态：只有解析完成才可以直接总结 */
export const SUMMARY_DOC_READY_STATUS = ['SUCCESS', 'PARSED', 'CHUNKED', 'INDEXED'];

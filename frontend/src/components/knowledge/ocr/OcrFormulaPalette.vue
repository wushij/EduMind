<template>
  <div class="ocr-formula-palette">
    <div class="palette-header">
      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat.key"
          type="button"
          class="cat-tab-btn"
          :class="{ active: activeCategory === cat.key }"
          @click="activeCategory = cat.key"
        >
          <el-icon class="cat-icon"><component :is="cat.icon" /></el-icon>
          <span>{{ cat.label }}</span>
        </button>
      </div>

      <!-- 智能排版一键工具下拉组 -->
      <div class="format-tools-group">
        <el-dropdown trigger="click" @command="handleFormatCommand">
          <button type="button" class="format-trigger-btn">
            <el-icon><AiSparkleIcon /></el-icon>
            <span>智能排版格式化</span>
            <el-icon class="arrow-down"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="format-dropdown-menu">
              <el-dropdown-item command="punctuation">
                <el-icon><EditPen /></el-icon> 规范化中文学术标点（，。？！：）
              </el-dropdown-item>
              <el-dropdown-item command="options">
                <el-icon><Tickets /></el-icon> 优化选择题选项换行排版
              </el-dropdown-item>
              <el-dropdown-item command="latex_spaces">
                <el-icon><Operation /></el-icon> 清理 LaTeX 公式两端多余空格
              </el-dropdown-item>
              <el-dropdown-item command="clear_empty_lines">
                <el-icon><Delete /></el-icon> 清除多余连续空行
              </el-dropdown-item>
              <el-dropdown-item divided command="reset">
                <el-icon><RefreshLeft /></el-icon> 重置为 OCR 原始识别文本
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 对应分类下的符号与模板列表 -->
    <div class="palette-symbols-scroll">
      <div class="symbols-chips-row">
        <button
          v-for="(item, idx) in currentItems"
          :key="idx"
          type="button"
          class="symbol-chip-btn"
          :title="item.code"
          @click="$emit('insert-formula', item.code)"
        >
          <span class="chip-label">{{ item.label }}</span>
          <span v-if="item.preview" class="chip-preview">{{ item.preview }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Operation,
  TrendCharts,
  Compass,
  Tickets,
  ArrowDown,
  EditPen,
  Delete,
  RefreshLeft
} from '@element-plus/icons-vue';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

const emit = defineEmits<{
  'insert-formula': [latex: string];
  'format': [action: 'punctuation' | 'options' | 'latex_spaces' | 'clear_empty_lines' | 'reset'];
}>();

const activeCategory = ref<'algebra' | 'calculus' | 'geometry' | 'template'>('algebra');

const categories = [
  { key: 'algebra', label: '常用代数', icon: Operation },
  { key: 'calculus', label: '微积分与求和', icon: TrendCharts },
  { key: 'geometry', label: '几何与方程', icon: Compass },
  { key: 'template', label: '试卷题型模板', icon: Tickets }
] as const;

const itemsMap = {
  algebra: [
    { label: '分式', preview: 'a/b', code: '$\\frac{a}{b}$' },
    { label: '根式', preview: '√x', code: '$\\sqrt{x}$' },
    { label: '二次根式', preview: '√(x²+y²)', code: '$\\sqrt{x^2 + y^2}$' },
    { label: '上标幂', preview: 'x²', code: '$x^{2}$' },
    { label: '数列项', preview: 'aₙ', code: '$a_{n}$' },
    { label: '对数', preview: 'ln x', code: '$\\ln x$' },
    { label: '对数以a为底', preview: 'log_a b', code: '$\\log_{a} b$' },
    { label: '绝对值', preview: '|x|', code: '$|x|$' },
    { label: '区间闭开', preview: '[a, b)', code: '$[a, b)$' },
    { label: '正无穷', preview: '+∞', code: '$+\\infty$' }
  ],
  calculus: [
    { label: '极限', preview: 'lim x→0', code: '$\\lim_{x \\to 0} f(x)$' },
    { label: '定积分', preview: '∫_a^b', code: '$\\int_{a}^{b} f(x)dx$' },
    { label: '不定积分', preview: '∫ f(x)dx', code: '$\\int f(x)dx$' },
    { label: '求和', preview: '∑ aᵢ', code: '$$\\sum_{i=1}^{n} a_i$$' },
    { label: '求积', preview: '∏ xᵢ', code: '$$\\prod_{i=1}^{n} x_i$$' },
    { label: '一阶导数', preview: "f'(x)", code: "$f'(x)$" },
    { label: '偏导数', preview: '∂y/∂x', code: '$\\frac{\\partial y}{\\partial x}$' }
  ],
  geometry: [
    { label: '三角形', preview: '△ABC', code: '$\\triangle ABC$' },
    { label: '角符号', preview: '∠A', code: '$\\angle A$' },
    { label: '向量', preview: 'a⃗', code: '$\\vec{a}$' },
    { label: '垂直', preview: '⊥', code: '$\\perp$' },
    { label: '平行', preview: '∥', code: '$\\parallel$' },
    { label: '圆周率', preview: 'π', code: '$\\pi$' },
    { label: '大括号方程组', preview: '{方程组}', code: '$$\\begin{cases} x + y = 1 \\\\ 2x - y = 0 \\end{cases}$$' },
    { label: '二阶行列式', preview: '|矩阵|', code: '$$\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}$$' }
  ],
  template: [
    { label: '选择题四选项', preview: 'A. B. C. D.', code: 'A. \nB. \nC. \nD. ' },
    { label: '填空题横线', preview: '________', code: '________' },
    { label: '解答题规范框架', preview: '【解析】解:...', code: '【解析】\n解：（1）由题意得：\n\n（2）若进一步设：\n故所求结论为：' },
    { label: '选择题题头', preview: 'Q. 题干', code: '**1. 【考点】**\n题目题干内容（   ）' },
    { label: '证明题结构', preview: '【证明】', code: '【证明】\n因为 $\\triangle ABC$ 中各边满足：\n所以有：\n综上所述，命题得证。' }
  ]
};

const currentItems = computed(() => {
  return itemsMap[activeCategory.value] || [];
});

function handleFormatCommand(command: 'punctuation' | 'options' | 'latex_spaces' | 'clear_empty_lines' | 'reset') {
  emit('format', command);
}
</script>

<style scoped lang="scss">
.ocr-formula-palette {
  background: #F8FAFC;
  border-bottom: 1px solid #E2E8F0;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .palette-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    .category-tabs {
      display: flex;
      align-items: center;
      gap: 6px;
      overflow-x: auto;
      scrollbar-width: none;
      &::-webkit-scrollbar { display: none; }

      .cat-tab-btn {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 28px;
        padding: 0 10px;
        border-radius: 6px;
        border: 1px solid transparent;
        background: transparent;
        color: #64748B;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.15s ease;
        white-space: nowrap;

        .cat-icon {
          font-size: 13px;
        }

        &:hover {
          color: #2563EB;
          background: #EEF2FF;
        }

        &.active {
          color: #2563EB;
          background: #FFFFFF;
          border-color: #BFDBFE;
          font-weight: 600;
          box-shadow: 0 1px 4px rgba(37, 99, 235, 0.08);
        }
      }
    }

    .format-tools-group {
      flex-shrink: 0;

      .format-trigger-btn {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        height: 28px;
        padding: 0 10px;
        border-radius: 6px;
        border: 1px solid #E2E8F0;
        background: #FFFFFF;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.15s ease;
        white-space: nowrap;

        &:hover {
          color: #7C3AED;
          border-color: #DDD6FE;
          background: #FAF5FF;
        }

        .arrow-down {
          font-size: 10px;
          margin-left: 2px;
        }
      }
    }
  }

  .palette-symbols-scroll {
    overflow-x: auto;
    scrollbar-width: thin;
    padding-bottom: 2px;

    .symbols-chips-row {
      display: flex;
      align-items: center;
      gap: 8px;
      white-space: nowrap;

      .symbol-chip-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 26px;
        padding: 0 10px;
        border-radius: 6px;
        border: 1px solid #E2E8F0;
        background: #FFFFFF;
        color: #334155;
        font-size: 11px;
        cursor: pointer;
        transition: all 0.15s ease;
        white-space: nowrap;
        flex-shrink: 0;

        .chip-label {
          font-weight: 500;
        }

        .chip-preview {
          color: #2563EB;
          font-family: 'Times New Roman', serif;
          font-style: italic;
          font-weight: 600;
          font-size: 12px;
          background: #F1F5F9;
          padding: 1px 4px;
          border-radius: 4px;
        }

        &:hover {
          border-color: #93C5FD;
          background: #EFF6FF;
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);

          .chip-preview {
            background: #DBEAFE;
          }
        }
      }
    }
  }
}

:deep(.format-dropdown-menu) {
  .el-dropdown-menu__item {
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>

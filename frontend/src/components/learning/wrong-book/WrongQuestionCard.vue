<template>
  <el-card class="question-item-card" shadow="hover">
    <div class="card-top-bar">
      <div class="badge-group">
        <el-tag size="small" type="danger" effect="dark" class="error-count-tag">
          累计错误 {{ item.wrongCount }} 次
        </el-tag>
        <el-tag v-if="item.knowledgePointName" size="small" type="info" effect="plain">
          {{ item.knowledgePointName }}
        </el-tag>
        <el-tag v-if="item.type" size="small" type="info" effect="plain">
          {{ formatQuestionType(item.type) }}
        </el-tag>
        <el-tag v-if="item.difficulty" size="small" :type="getDifficultyType(item.difficulty)" effect="plain">
          {{ item.difficulty }}
        </el-tag>
      </div>
      <span class="item-time">录入时间：{{ item.createTime || '近期学习' }}</span>
    </div>

    <div class="stem-container">
      <MathText :text="item.stem || '暂无题目内容'" />
    </div>

    <div v-if="parsedOptions(item.options).length > 0" class="options-container">
      <div
        v-for="opt in parsedOptions(item.options)"
        :key="opt.key"
        class="option-row"
        :class="{
          'is-answer': opt.key === item.answer,
          'is-wrong': opt.key === item.studentAnswer
        }"
      >
        <span class="option-key">{{ opt.key }}.</span>
        <MathText class="option-val" :text="opt.val" />
        <span v-if="opt.key === item.answer" class="option-label answer-label">正确答案</span>
        <span v-else-if="opt.key === item.studentAnswer" class="option-label wrong-label">历史作答</span>
      </div>
    </div>

    <div v-if="!parsedOptions(item.options).length" class="answers-compare-row">
      <div class="ans-box wrong-box">
        <span class="ans-title">历史错误提交：</span>
        <span class="ans-text">{{ item.studentAnswer || '作答不完整或步骤中断' }}</span>
      </div>
      <div class="ans-box right-box">
        <span class="ans-title">标准参考答案：</span>
        <span class="ans-text">{{ item.answer || '详见完整解析' }}</span>
      </div>
    </div>

    <div class="ai-diagnosis-banner">
      <div class="ai-header">
        <div class="ai-tag">
          <el-icon><Cpu /></el-icon>
          <span>AI 认知归因诊断</span>
        </div>
        <div v-if="errorTags.length" class="error-types-tags">
          <el-tag v-for="err in errorTags" :key="err" size="small" type="warning" effect="plain">
            {{ err }}
          </el-tag>
        </div>
      </div>
      <p class="diagnosis-text">
        {{ item.diagnosis || '系统已捕获该知识点失分模式，点击下方按钮获取完整诊断与前驱依赖解析。' }}
      </p>
    </div>

    <div class="card-footer-bar">
      <el-button size="small" type="primary" text @click="emit('open-diagnosis', item)">
        <el-icon><Document /></el-icon>
        <span>查看深度归因与前驱知识</span>
      </el-button>
      <div class="footer-right">
        <el-button size="small" type="success" plain @click="emit('mark-mastered', item)">
          <el-icon><Check /></el-icon>
          <span>标为已攻克</span>
        </el-button>
        <el-button size="small" type="primary" @click="emit('start-variant', item)">
          <el-icon><Aim /></el-icon>
          <span>练习同类变式题</span>
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Cpu, Document, Check, Aim } from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import type { WrongQuestionRecordItem } from '@/types/learning/wrong-question';

const props = defineProps<{
  item: WrongQuestionRecordItem;
  formatQuestionType: (type?: string) => string;
  getDifficultyType: (diff?: string) => 'success' | 'warning' | 'danger';
  parsedOptions: (optionsJson?: string) => Array<{ key: string; val: string }>;
  displayErrorTags: (item: WrongQuestionRecordItem) => string[];
}>();

const emit = defineEmits<{
  'open-diagnosis': [item: WrongQuestionRecordItem];
  'mark-mastered': [item: WrongQuestionRecordItem];
  'start-variant': [item: WrongQuestionRecordItem];
}>();

const errorTags = computed(() => props.displayErrorTags(props.item));
</script>

<style scoped lang="scss">
.question-item-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  transition: all 0.25s ease;

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 8px 24px rgba(22, 119, 255, 0.08);
    transform: translateY(-2px);
  }
}

.card-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 8px;

  .badge-group {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .error-count-tag {
      font-weight: 600;
      border-radius: 9999px;
    }
  }

  .item-time {
    font-size: 12px;
    color: #94a3b8;
  }
}

.stem-container {
  font-size: 15px;
  line-height: 1.7;
  color: #1e293b;
  font-weight: 500;
  margin-bottom: 14px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
  border-left: 4px solid #1677ff;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;

  .option-row {
    display: flex;
    align-items: center;
    padding: 8px 14px;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    font-size: 14px;
    color: #334155;

    .option-key {
      font-weight: 700;
      margin-right: 8px;
      color: #1677ff;
    }

    .option-val {
      flex: 1;
    }

    .option-label {
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 9999px;
      margin-left: 12px;
      font-weight: 600;

      &.answer-label {
        background: #eaf8ee;
        color: #52c41a;
        border: 1px solid #b7eb8f;
      }

      &.wrong-label {
        background: #fff1f0;
        color: #f5222d;
        border: 1px solid #ffa39e;
      }
    }

    &.is-answer {
      background: #f6ffed;
      border-color: #b7eb8f;
    }

    &.is-wrong {
      background: #fff1f0;
      border-color: #ffa39e;
    }
  }
}

.answers-compare-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  .ans-box {
    padding: 10px 14px;
    border-radius: 8px;
    font-size: 13px;

    .ans-title {
      font-weight: 600;
      display: block;
      margin-bottom: 4px;
    }

    &.wrong-box {
      background: #fff2f0;
      border: 1px solid #ffccc7;
      color: #cf1322;
    }

    &.right-box {
      background: #f6ffed;
      border: 1px solid #d9f7be;
      color: #389e0d;
    }
  }
}

.ai-diagnosis-banner {
  background: linear-gradient(135deg, #f0f5ff 0%, #f5f0ff 100%);
  border: 1px solid #d6e4ff;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 14px;

  .ai-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
    flex-wrap: wrap;
    gap: 8px;

    .ai-tag {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #1677ff;
      font-size: 13px;
      font-weight: 700;
    }

    .error-types-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }
  }

  .diagnosis-text {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: #475569;
  }
}

.card-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  flex-wrap: wrap;
  gap: 8px;

  .footer-right {
    display: flex;
    gap: 10px;
  }
}
</style>

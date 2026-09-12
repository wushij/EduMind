<template>
  <div class="prompt-editor-page">
    <!-- 顶部导航与保存操作栏 -->
    <div class="editor-header-card">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="router.push('/system/prompts')">
          返回模板列表
        </el-button>
        <div class="title-row">
          <h2>{{ form.name || '新建 Prompt 模板' }}</h2>
          <span class="version-tag">{{ form.version }}</span>
          <el-tag :type="form.status === 'PUBLISHED' ? 'success' : 'warning'" size="small">
            {{ form.status === 'PUBLISHED' ? '已发布生产' : '草稿' }}
          </el-tag>
        </div>
      </div>

      <div class="header-right">
        <el-button :icon="Check" @click="saveForm">保存草稿</el-button>
        <el-button
          type="primary"
          :icon="Upload"
          :loading="publishing"
          @click="publishForm"
        >
          发布生效至网关
        </el-button>
      </div>
    </div>

    <!-- 双栏编辑与实时测试工作台 -->
    <div class="editor-workbench-grid">
      <!-- 左栏：Prompt 模板工程配置与内容编写 -->
      <div class="col-card editor-form-col">
        <div class="col-header">
          <span class="sec-title">模板基础与提示词编写</span>
        </div>

        <el-form label-position="top" class="prompt-form">
          <div class="form-row-2">
            <el-form-item label="模板编码 (唯一标识)">
              <el-input v-model="form.code" placeholder="如 PROMPT_COURSE_QA" />
            </el-form-item>
            <el-form-item label="所属业务分类">
              <el-select v-model="form.category" style="width: 100%">
                <el-option value="rag" label="课程问答 (RAG)" />
                <el-option value="question" label="智能命题 (Exam)" />
                <el-option value="grading" label="智能批改 (Grading)" />
                <el-option value="teaching" label="教案备课" />
                <el-option value="agent" label="Agent 规划" />
              </el-select>
            </el-form-item>
          </div>

          <div class="form-row-2">
            <el-form-item label="默认绑定模型">
              <el-select v-model="form.boundModel" style="width: 100%">
                <el-option value="deepseek-chat" label="DeepSeek-V3" />
                <el-option value="qwen-plus" label="通义千问 Qwen-Plus" />
                <el-option value="gpt-4o-mini" label="GPT-4o Mini" />
              </el-select>
            </el-form-item>
            <el-form-item label="推荐生成温度 (Temperature): {{ form.temperature }}">
              <el-slider v-model="form.temperature" :min="0.0" :max="1.0" :step="0.05" />
            </el-form-item>
          </div>

          <el-form-item label="System Prompt (系统人设与硬性约束规则)">
            <el-input
              v-model="form.systemPrompt"
              type="textarea"
              :rows="5"
              placeholder="定义大模型人设、专业背景、思考过程以及输出格式约束..."
              class="code-textarea"
            />
          </el-form-item>

          <el-form-item>
            <template #label>
              <div class="prompt-label-with-vars">
                <span>User Prompt Template (用户指令模板)</span>
                <div class="vars-inline">
                  <span class="hint">点击快速插入变量：</span>
                  <el-tag
                    v-for="v in form.variables"
                    :key="v.name"
                    size="small"
                    class="insert-var-tag"
                    @click="insertVar(v.name)"
                  >
                    + &#123;&#123;{{ v.name }}&#125;&#125;
                  </el-tag>
                </div>
              </div>
            </template>
            <el-input
              ref="userPromptInputRef"
              v-model="form.userPromptTemplate"
              type="textarea"
              :rows="8"
              placeholder="在此编排变量插槽，如：【课程】：{{course_name}} \n【问题】：{{user_question}}..."
              class="code-textarea"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 右栏：变量在线填参与即时调试台 -->
      <div class="col-card test-runner-col">
        <div class="col-header">
          <span class="sec-title">在线热测试运行台 (Live Playground)</span>
          <el-button
            type="primary"
            size="small"
            :loading="testing"
            :icon="VideoPlay"
            @click="handleRunTest"
          >
            运行测试
          </el-button>
        </div>

        <div class="test-inputs-section">
          <div class="section-title">动态入参填充 (Dynamic Variables)</div>
          <div class="vars-input-list">
            <div
              v-for="v in form.variables"
              :key="v.name"
              class="var-input-row"
            >
              <label class="var-name">&#123;&#123;{{ v.name }}&#125;&#125;</label>
              <el-input
                v-model="testVariables[v.name]"
                size="small"
                :placeholder="`输入 ${v.label || v.name}...`"
              />
            </div>
          </div>
        </div>

        <!-- 测试运行结果展示 -->
        <div class="test-result-section">
          <div class="section-title">
            <span>大模型生成结果 (LLM Response)</span>
            <div v-if="testResult" class="meta-metrics">
              <span class="metric-tag">{{ testResult.durationMs }} ms</span>
              <span class="metric-tag">{{ testResult.totalTokens }} Tokens</span>
            </div>
          </div>

          <div v-if="testResult" class="result-display-box">
            <pre class="output-text">{{ testResult.output }}</pre>
          </div>

          <div v-else class="empty-test-hint">
            <el-icon class="hint-icon"><Promotion /></el-icon>
            <p>点击上方【运行测试】可直接渲染 Prompt 并请求大模型，快速验证提示词效果与格式稳定性</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { usePrompt } from '@/composables/system/usePrompt';
import { PromptTemplate } from '@/types/system/prompt';
import {
  ArrowLeft,
  Check,
  Upload,
  VideoPlay,
  Promotion
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const promptId = route.params.id ? Number(route.params.id) : null;

const {
  testing,
  publishing,
  currentPrompt,
  testResult,
  loadPrompt,
  handleSave,
  handlePublish,
  runTest
} = usePrompt();

const form = ref<PromptTemplate>({
  id: 0,
  code: 'PROMPT_NEW',
  name: '新建业务提示词模板',
  category: 'rag',
  description: '',
  systemPrompt: '你是一位高校优秀名师与 AI 助教。请准确专业地回答学生提出的学术问题。',
  userPromptTemplate: '=== 课程：{{course_name}} ===\n【学生疑问】：{{user_question}}',
  variables: [
    { name: 'course_name', label: '课程名称', defaultValue: 'Java程序设计' },
    { name: 'user_question', label: '学生问题', defaultValue: '什么是面向对象的多态性？' }
  ],
  version: 'v1.0.0',
  status: 'DRAFT',
  boundModel: 'deepseek-chat',
  temperature: 0.3,
  maxTokens: 2048,
  callCount: 0,
  createdAt: '',
  updatedAt: ''
});

const testVariables = ref<Record<string, string>>({
  course_name: 'Java程序设计',
  user_question: '什么是面向对象的多态性？'
});

const insertVar = (name: string) => {
  form.value.userPromptTemplate += ` {{${name}}}`;
};

const handleRunTest = async () => {
  const templateId = form.value.id || promptId;
  if (!templateId) {
    ElMessage.warning('请先保存模板后再测试');
    return;
  }
  await runTest(templateId, {
    systemPrompt: form.value.systemPrompt,
    userPromptTemplate: form.value.userPromptTemplate,
    variables: testVariables.value,
    model: form.value.boundModel,
    temperature: form.value.temperature
  });
};

const saveForm = async () => {
  const newId = await handleSave(form.value);
  if (newId && !form.value.id) {
    form.value.id = newId;
    router.replace(`/system/prompts/editor/${newId}`);
  }
};

const publishForm = async () => {
  await saveForm();
  if (form.value.id) {
    await handlePublish(form.value.id);
  }
};

onMounted(async () => {
  if (promptId) {
    await loadPrompt(promptId);
    if (currentPrompt.value) {
      form.value = JSON.parse(JSON.stringify(currentPrompt.value));
      form.value.variables.forEach(v => {
        testVariables.value[v.name] = v.defaultValue || '';
      });
    }
  }
});
</script>

<style scoped lang="scss">
.prompt-editor-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .editor-header-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .title-row {
        display: flex;
        align-items: center;
        gap: 10px;

        h2 { margin: 0; font-size: 18px; font-weight: 700; color: #0F172A; }
        .version-tag { font-family: ui-monospace, monospace; font-size: 11px; background: #F1F5F9; padding: 2px 6px; border-radius: 4px; }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }

  .editor-workbench-grid {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 16px;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    .col-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 18px 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

      .col-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #F1F5F9;
        padding-bottom: 12px;

        .sec-title {
          font-size: 14px;
          font-weight: 700;
          color: #1E293B;
        }
      }
    }

    .editor-form-col {
      .prompt-form {
        .form-row-2 {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 14px;
        }

        .prompt-label-with-vars {
          display: flex;
          justify-content: space-between;
          align-items: center;
          width: 100%;

          .vars-inline {
            display: flex;
            align-items: center;
            gap: 6px;

            .hint { font-size: 11px; color: #94A3B8; }
            .insert-var-tag {
              cursor: pointer;
              font-family: ui-monospace, monospace;
              font-size: 10.5px;
            }
          }
        }

        .code-textarea {
          :deep(textarea) {
            font-family: ui-monospace, monospace;
            font-size: 12.5px;
            line-height: 1.6;
            background: #FAFBFC;
          }
        }
      }
    }

    .test-runner-col {
      .test-inputs-section {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .section-title {
          font-size: 12px;
          font-weight: 700;
          color: #475569;
        }

        .vars-input-list {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .var-input-row {
            display: flex;
            align-items: center;
            gap: 10px;

            .var-name {
              font-family: ui-monospace, monospace;
              font-size: 11.5px;
              color: #2563EB;
              background: #EFF6FF;
              padding: 2px 6px;
              border-radius: 4px;
              width: 130px;
              flex-shrink: 0;
            }
          }
        }
      }

      .test-result-section {
        margin-top: 14px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        flex: 1;

        .section-title {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 12px;
          font-weight: 700;
          color: #475569;

          .meta-metrics {
            display: flex;
            gap: 6px;

            .metric-tag {
              font-family: ui-monospace, monospace;
              font-size: 11px;
              background: #ECFDF5;
              color: #059669;
              padding: 2px 6px;
              border-radius: 4px;
            }
          }
        }

        .result-display-box {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 8px;
          padding: 14px;
          min-height: 240px;

          .output-text {
            margin: 0;
            font-size: 13px;
            line-height: 1.65;
            color: #1E293B;
            white-space: pre-wrap;
            font-family: inherit;
          }
        }

        .empty-test-hint {
          background: #F8FAFC;
          border: 1px dashed #CBD5E1;
          border-radius: 8px;
          padding: 60px 20px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: #94A3B8;

          .hint-icon { font-size: 32px; margin-bottom: 10px; }
          p { margin: 0; font-size: 12.5px; text-align: center; }
        }
      }
    }
  }
}
</style>

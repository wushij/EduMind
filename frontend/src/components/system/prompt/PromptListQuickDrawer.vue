<template>
  <el-drawer
    :model-value="drawerVisible"
    :size="'58%'"
    destroy-on-close
    class="prompt-quick-drawer"
    @update:model-value="$emit('update:drawerVisible', $event)"
  >
    <template #header>
      <div v-if="activeItem" class="drawer-header-content">
        <div class="dh-top">
          <span class="dh-code">{{ activeItem.code }}</span>
          <span class="dh-version">{{ activeItem.version }}</span>
          <span class="dh-cat" :class="activeItem.category">
            {{ getCategoryLabel(activeItem.category) }}
          </span>
        </div>
        <h2 class="dh-title">{{ activeItem.name }}</h2>
        <p class="dh-desc">{{ activeItem.description }}</p>
      </div>
    </template>

    <div v-if="activeItem" class="drawer-body-wrap">
      <el-tabs :model-value="drawerActiveTab" class="drawer-tabs" @update:model-value="$emit('update:drawerActiveTab', $event)">
        <!-- 选项卡 1：提示词工程规范 -->
        <el-tab-pane label="提示词工程规范" name="preview">
          <div class="spec-tab-content">
            <!-- 模型绑定与调参参数卡片 -->
            <div class="param-summary-card">
              <div class="p-item">
                <span class="k">绑定模型</span>
                <span class="v model-val">{{ activeItem.boundModel || '未指定模型（跟随系统网关）' }}</span>
              </div>
              <div class="p-item">
                <span class="k">采样温度 (Temperature)</span>
                <span class="v">{{ activeItem.temperature ?? 0.3 }}</span>
              </div>
              <div class="p-item">
                <span class="k">最大 Token 限制</span>
                <span class="v">{{ activeItem.maxTokens ?? 8000 }} Tokens</span>
              </div>
              <div class="p-item">
                <span class="k">所属场景分类</span>
                <span class="v">{{ getCategoryLabel(activeItem.category) }}</span>
              </div>
            </div>

            <!-- System Prompt 区域 -->
            <div class="prompt-block">
              <div class="block-header">
                <span class="b-title">System Prompt (系统人设与硬性约束规则)</span>
                <el-button
                  size="small"
                  link
                  :icon="CopyDocument"
                  @click="$emit('copy-text', activeItem.systemPrompt, 'System Prompt 已复制')"
                >
                  复制内容
                </el-button>
              </div>
              <div class="code-view-box system-box">
                <pre>{{ activeItem.systemPrompt || '（未单独设置 System Prompt，使用系统默认人设）' }}</pre>
              </div>
            </div>

            <!-- User Prompt Template 区域 -->
            <div class="prompt-block">
              <div class="block-header">
                <span class="b-title">User Prompt Template (用户指令插槽模板)</span>
                <el-button
                  size="small"
                  link
                  :icon="CopyDocument"
                  @click="$emit('copy-text', activeItem.userPromptTemplate, 'User Prompt 模板已复制')"
                >
                  复制内容
                </el-button>
              </div>
              <div class="code-view-box user-box">
                <pre>{{ activeItem.userPromptTemplate }}</pre>
              </div>
            </div>

            <!-- 变量插槽明细清单 -->
            <div class="prompt-block">
              <div class="block-header">
                <span class="b-title">动态注入参数插槽清单 ({{ activeItem.variables.length }} 个)</span>
              </div>
              <div class="vars-table-card">
                <div class="var-table-header">
                  <span class="col-name">变量标识</span>
                  <span class="col-label">中文语义</span>
                  <span class="col-sample">默认示例</span>
                </div>
                <div
                  v-for="v in activeItem.variables"
                  :key="v.name"
                  class="var-table-row"
                >
                  <span class="col-name">&#123;&#123;{{ v.name }}&#125;&#125;</span>
                  <span class="col-label">{{ v.label || v.name }}</span>
                  <span class="col-sample">{{ v.defaultValue || '—' }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 选项卡 2：实时热测试演练台 -->
        <el-tab-pane label="在线热测试演练 (Live Playground)" name="test">
          <div class="playground-tab-content">
            <!-- 参数填报表单 -->
            <div class="test-inputs-panel">
              <div class="panel-head">
                <span class="panel-title">1. 动态入参模拟填充</span>
                <el-button
                  size="small"
                  link
                  :loading="demoFilling"
                  @click="$emit('populate-dummy')"
                >
                  {{ demoFilling ? '正在读取真实课程数据…' : '一键填入高校课程示例数据' }}
                </el-button>
              </div>

              <div class="vars-inputs-grid">
                <div
                  v-for="v in activeItem.variables"
                  :key="v.name"
                  class="input-group"
                >
                  <label class="group-label">
                    &#123;&#123;{{ v.name }}&#125;&#125;
                    <span class="sub-label">({{ v.label || v.name }})</span>
                  </label>
                  <el-input
                    :model-value="testVariables[v.name]"
                    :type="v.name.includes('context') || v.name.includes('history') ? 'textarea' : 'text'"
                    :rows="v.name.includes('context') ? 4 : 2"
                    :placeholder="`请输入 ${v.label || v.name}...`"
                    @update:model-value="$emit('update-test-variable', v.name, $event)"
                  />
                </div>
              </div>

              <div class="run-test-bar">
                <el-button
                  type="primary"
                  :icon="VideoPlay"
                  :loading="testing"
                  class="run-btn"
                  @click="$emit('execute-test')"
                >
                  {{ testing ? '模型思考推理中...' : '立即运行 Prompt 热测试' }}
                </el-button>
              </div>
            </div>

            <!-- 渲染后 Prompt 与推理结果 -->
            <div v-if="testResultOutput || testing" class="test-output-panel">
              <div class="panel-head">
                <span class="panel-title">2. 模型输出与溯源结果</span>
                <span v-if="testDuration > 0" class="meta-tag">
                  耗时: {{ testDuration }}ms
                </span>
              </div>

              <div v-loading="testing" class="output-content-card">
                <PromptLlmOutput v-if="testResultOutput" :content="testResultOutput" />
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <div class="drawer-footer-actions">
        <el-button @click="$emit('update:drawerVisible', false)">关闭</el-button>
        <el-button
          type="primary"
          @click="$emit('edit', activeItem?.id)"
        >
          前往完整编辑器配置 →
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { CopyDocument, VideoPlay } from '@element-plus/icons-vue';
import PromptLlmOutput from '@/components/system/PromptLlmOutput.vue';
import type { PromptTemplate } from '@/types/system/prompt';

defineProps<{
  drawerVisible: boolean;
  drawerActiveTab: 'preview' | 'test';
  activeItem: PromptTemplate | null;
  testVariables: Record<string, string>;
  testResultOutput: string;
  testDuration: number;
  testing: boolean;
  /** 演示数据实时读取中：课程 / 章节 / 知识点 / 知识库 / 题库 */
  demoFilling?: boolean;
  getCategoryLabel: (category: string) => string;
}>();

defineEmits<{
  'update:drawerVisible': [value: boolean];
  'update:drawerActiveTab': [value: 'preview' | 'test'];
  'update-test-variable': [name: string, value: string];
  'copy-text': [text: string, msg: string];
  'populate-dummy': [];
  'execute-test': [];
  edit: [id?: number];
}>();
</script>

<style scoped lang="scss">
.prompt-quick-drawer {
  .drawer-header-content {
    .dh-top {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 6px;

      .dh-code {
        font-family: ui-monospace, monospace;
        font-size: 12px;
        font-weight: 700;
        color: #2563EB;
        background: #EFF6FF;
        padding: 2px 10px;
        border-radius: 999px;
      }
      .dh-version {
        font-size: 12px;
        color: #64748B;
        background: #F1F5F9;
        padding: 2px 8px;
        border-radius: 999px;
      }
      .dh-cat {
        font-size: 11px;
        padding: 2px 8px;
        border-radius: 999px;
        font-weight: 600;

        &.rag {
          background: #E0F2FE;
          color: #0369A1;
        }
        &.question {
          background: #FEF3C7;
          color: #B45309;
        }
        &.grading {
          background: #F3E8FF;
          color: #7E22CE;
        }
        &.teaching {
          background: #DCFCE7;
          color: #15803D;
        }
      }
    }

    .dh-title {
      margin: 0 0 6px 0;
      font-size: 20px;
      font-weight: 800;
      color: #0F172A;
    }

    .dh-desc {
      margin: 0;
      font-size: 13px;
      color: #64748B;
      line-height: 1.5;
    }
  }

  .drawer-body-wrap {
    .drawer-tabs {
      :deep(.el-tabs__item) {
        font-size: 14px;
        font-weight: 600;
      }
    }

    .spec-tab-content {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .param-summary-card {
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 10px;
        padding: 12px 18px;
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 12px;

        .p-item {
          display: flex;
          flex-direction: column;
          gap: 3px;

          .k {
            font-size: 11.5px;
            color: #64748B;
          }
          .v {
            font-size: 13.5px;
            font-weight: 600;
            color: #0F172A;

            &.model-val {
              font-family: ui-monospace, monospace;
              color: #2563EB;
            }
          }
        }
      }

      .prompt-block {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 10px;
        padding: 14px 16px;

        .block-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          .b-title {
            font-size: 13.5px;
            font-weight: 700;
            color: #1E293B;
          }
        }

        .code-view-box {
          background: #0F172A;
          border-radius: 8px;
          padding: 12px 14px;
          max-height: 280px;
          overflow-y: auto;

          pre {
            margin: 0;
            font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
            font-size: 12px;
            line-height: 1.6;
            color: #E2E8F0;
            white-space: pre-wrap;
            word-break: break-word;
          }

          &.user-box {
            background: #1E293B;
            pre {
              color: #93C5FD;
            }
          }
        }

        .vars-table-card {
          border: 1px solid #F1F5F9;
          border-radius: 8px;
          overflow: hidden;

          .var-table-header {
            display: grid;
            grid-template-columns: 180px 140px 1fr;
            padding: 8px 12px;
            background: #F8FAFC;
            font-size: 12px;
            font-weight: 600;
            color: #64748B;
          }

          .var-table-row {
            display: grid;
            grid-template-columns: 180px 140px 1fr;
            padding: 8px 12px;
            border-top: 1px solid #F1F5F9;
            font-size: 12.5px;
            align-items: center;

            .col-name {
              font-family: ui-monospace, monospace;
              font-weight: 600;
              color: #2563EB;
            }
            .col-label {
              color: #475569;
            }
            .col-sample {
              color: #64748B;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }
        }
      }
    }

    .playground-tab-content {
      display: flex;
      flex-direction: column;
      gap: 18px;

      .test-inputs-panel {
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 12px;
        padding: 16px;

        .panel-head {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 14px;

          .panel-title {
            font-size: 14px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .vars-inputs-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px;

          @media (max-width: 800px) {
            grid-template-columns: 1fr;
          }

          .input-group {
            display: flex;
            flex-direction: column;
            gap: 4px;

            .group-label {
              font-family: ui-monospace, monospace;
              font-size: 12px;
              font-weight: 600;
              color: #1E293B;

              .sub-label {
                font-family: sans-serif;
                font-weight: normal;
                color: #64748B;
                margin-left: 4px;
              }
            }
          }
        }

        .run-test-bar {
          margin-top: 16px;
          display: flex;
          justify-content: flex-end;

          .run-btn {
            padding: 10px 22px;
            font-weight: 600;
          }
        }
      }

      .test-output-panel {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 12px;
        padding: 16px;

        .panel-head {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .panel-title {
            font-size: 14px;
            font-weight: 700;
            color: #0F172A;
          }

          .meta-tag {
            font-size: 11.5px;
            color: #64748B;
            background: #F1F5F9;
            padding: 2px 8px;
            border-radius: 4px;
          }
        }

        .output-content-card {
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 8px;
          padding: 16px;
          min-height: 180px;
        }
      }
    }
  }

  .drawer-footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}
</style>

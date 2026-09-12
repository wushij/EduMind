<template>
  <div class="rag-debug-workbench-page">
    <!-- 顶部工作台标题与状态提示 -->
    <div class="workbench-header-bar">
      <div class="left-info">
        <h3>RAG 全链路诊断工作台 (RAG Pipeline Debug Workbench)</h3>
        <p>支持对检索、重排、Prompt 上下文注入与 LLM 结构化生成进行全链路时序与质量诊断</p>
      </div>
      <div class="right-actions">
        <el-button
          type="primary"
          :icon="VideoPlay"
          :loading="debugLoading"
          class="run-pipeline-btn"
          @click="runDebugPipeline"
        >
          执行全链路诊断 (Run Pipeline)
        </el-button>
      </div>
    </div>

    <!-- 原型 §十四 同款三栏式工作台 -->
    <div class="three-column-grid">
      <!-- 1. 左栏：输入与调试超参 (Input & Hyperparameters) -->
      <div class="column-card col-left">
        <div class="col-title">
          <el-icon class="icon"><Setting /></el-icon>
          <span>诊断入参与超参</span>
        </div>

        <div class="param-form">
          <div class="form-group">
            <label>用户测试问题 (Query)</label>
            <el-input
              v-model="query"
              type="textarea"
              :rows="3"
              placeholder="输入待诊断的学生疑问..."
            />
          </div>

          <div class="form-group">
            <label>生成模型 (LLM Model)</label>
            <el-select v-model="selectedModel" style="width: 100%">
              <el-option value="deepseek-chat" label="DeepSeek-V3 (推荐)" />
              <el-option value="qwen-plus" label="通义千问 Qwen-Plus" />
              <el-option value="gpt-4o-mini" label="GPT-4o Mini" />
            </el-select>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>召回数量 Top-K: {{ topK }}</label>
              <el-slider v-model="topK" :min="1" :max="8" :step="1" />
            </div>
            <div class="form-group flex-1">
              <label>阈值: {{ scoreThreshold.toFixed(2) }}</label>
              <el-slider v-model="scoreThreshold" :min="0.4" :max="0.9" :step="0.05" />
            </div>
          </div>

          <div class="form-group">
            <label>生成温度 (Temperature): {{ temperature }}</label>
            <el-slider v-model="temperature" :min="0.0" :max="1.0" :step="0.1" />
          </div>

          <div class="form-group">
            <label>系统提示词模板 (System Prompt)</label>
            <el-input
              v-model="customSystemPrompt"
              type="textarea"
              :rows="4"
              class="prompt-textarea"
            />
          </div>
        </div>
      </div>

      <!-- 2. 中栏：召回上下文与切片打分 (Retrieved Contexts) -->
      <div class="column-card col-center">
        <div class="col-title">
          <el-icon class="icon"><DocumentCopy /></el-icon>
          <span>召回切片上下文 (Retrieved Context)</span>
          <span v-if="debugResponse" class="count-tag">
            {{ debugResponse.retrievedChunks.length }} 个切片注入
          </span>
        </div>

        <div v-if="debugResponse && debugResponse.retrievedChunks.length > 0" class="retrieved-scroll-list">
          <div
            v-for="(chunk, idx) in debugResponse.retrievedChunks"
            :key="chunk.id"
            class="chunk-debug-item"
          >
            <div class="item-header">
              <div class="header-left">
                <span class="idx-badge">#{{ idx + 1 }}</span>
                <span class="doc-badge" :title="chunk.documentName">{{ chunk.documentName }}</span>
                <span v-if="chunk.pageNo" class="page-badge">P.{{ chunk.pageNo }}</span>
              </div>
              <div class="score-badge">
                {{ (chunk.score * 100).toFixed(1) }}%
              </div>
            </div>
            <div v-if="chunk.heading" class="item-heading">
              {{ chunk.heading }}
            </div>
            <div class="item-snippet">
              {{ chunk.content }}
            </div>
          </div>
        </div>

        <div v-else class="center-empty">
          <el-icon class="empty-icon"><Files /></el-icon>
          <p>尚未运行诊断或未召回符合阈值的切片</p>
          <el-button type="primary" size="small" plain @click="runDebugPipeline">
            执行诊断召回
          </el-button>
        </div>
      </div>

      <!-- 3. 右栏：Prompt 组装、时序追踪与 LLM 输出 (Generation & Tracing) -->
      <div class="column-card col-right">
        <div class="col-title">
          <el-icon class="icon"><Cpu /></el-icon>
          <span>组装 Prompt 与模型输出 (Response)</span>
        </div>

        <div v-if="debugResponse" class="right-content-scroll">
          <!-- 阶段耗时时序面板 -->
          <RAGDebugPanel :debug-data="debugResponse" />

          <!-- 模型最终回复 -->
          <div class="response-section">
            <div class="sec-label">
              <span>模型生成回复内容 (LLM Output)</span>
              <el-tag size="small" type="success">生成完成</el-tag>
            </div>
            <div class="response-box">
              {{ debugResponse.llmResponse }}
            </div>
          </div>

          <!-- 组装后 System/User 完整 Prompt 审查折叠 -->
          <el-collapse class="prompt-collapse">
            <el-collapse-item title="查看发送给 LLM 的完整 Prompt 报文" name="1">
              <pre class="raw-prompt-preview">{{ debugResponse.assembledPrompt }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div v-else class="right-empty">
          <el-icon class="empty-icon"><ChatLineRound /></el-icon>
          <p>点击上方【执行全链路诊断】运行 Pipeline 并观测生成过程与时序耗时</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRAG } from '@/composables/knowledge/useRAG';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import RAGDebugPanel from '@/components/knowledge/RAGDebugPanel.vue';
import {
  VideoPlay,
  Setting,
  DocumentCopy,
  Cpu,
  Files,
  ChatLineRound
} from '@element-plus/icons-vue';

const { kbId } = useKnowledgeRoute();

const {
  query,
  topK,
  scoreThreshold,
  selectedModel,
  temperature,
  customSystemPrompt,
  debugLoading,
  debugResponse,
  runDebugPipeline
} = useRAG(kbId);
</script>

<style scoped lang="scss">
.rag-debug-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .workbench-header-bar {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 18px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

    .left-info {
      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 700;
        color: #0F172A;
      }
      p {
        margin: 4px 0 0 0;
        font-size: 13px;
        color: #64748B;
      }
    }

    .right-actions {
      .run-pipeline-btn {
        font-weight: 600;
        padding: 10px 20px;
        box-shadow: 0 2px 10px rgba(37, 99, 235, 0.2);
      }
    }
  }

  /* 原型三栏式布局 */
  .three-column-grid {
    display: grid;
    grid-template-columns: 320px 1fr 1.2fr;
    gap: 16px;
    min-height: 640px;

    @media (max-width: 1440px) {
      grid-template-columns: 300px 1fr 1fr;
    }

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }

    .column-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 18px;
      display: flex;
      flex-direction: column;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

      .col-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        font-weight: 700;
        color: #1E293B;
        border-bottom: 1px solid #F1F5F9;
        padding-bottom: 12px;
        margin-bottom: 14px;

        .icon {
          color: #2563EB;
          font-size: 16px;
        }

        .count-tag {
          margin-left: auto;
          font-size: 11px;
          background: #EFF6FF;
          color: #2563EB;
          padding: 2px 8px;
          border-radius: 4px;
          font-weight: 600;
        }
      }

      /* 左栏表单 */
      &.col-left {
        .param-form {
          display: flex;
          flex-direction: column;
          gap: 14px;

          .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;

            label {
              font-size: 12px;
              color: #475569;
              font-weight: 600;
            }

            .prompt-textarea {
              font-size: 12.5px;
            }
          }

          .form-row {
            display: flex;
            gap: 12px;
            .flex-1 { flex: 1; }
          }
        }
      }

      /* 中栏召回切片 */
      &.col-center {
        .retrieved-scroll-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
          overflow-y: auto;
          max-height: 600px;
          padding-right: 4px;

          .chunk-debug-item {
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            padding: 12px;
            transition: all 0.15s;

            &:hover {
              border-color: #BFDBFE;
              background: #FFFFFF;
            }

            .item-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 6px;

              .header-left {
                display: flex;
                align-items: center;
                gap: 6px;
                min-width: 0;

                .idx-badge {
                  font-size: 11px;
                  font-weight: 700;
                  color: #2563EB;
                  background: #EFF6FF;
                  padding: 1px 6px;
                  border-radius: 4px;
                }

                .doc-badge {
                  font-size: 12px;
                  font-weight: 600;
                  color: #334155;
                  white-space: nowrap;
                  overflow: hidden;
                  text-overflow: ellipsis;
                  max-width: 140px;
                }

                .page-badge {
                  font-family: ui-monospace, monospace;
                  font-size: 10.5px;
                  background: #FEF3C7;
                  color: #B45309;
                  padding: 1px 4px;
                  border-radius: 3px;
                }
              }

              .score-badge {
                font-family: ui-monospace, monospace;
                font-size: 12px;
                font-weight: 700;
                color: #059669;
                background: #ECFDF5;
                padding: 1px 6px;
                border-radius: 4px;
              }
            }

            .item-heading {
              font-size: 12.5px;
              font-weight: 600;
              color: #1E293B;
              margin-bottom: 6px;
            }

            .item-snippet {
              font-size: 12px;
              line-height: 1.6;
              color: #64748B;
            }
          }
        }

        .center-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          flex: 1;
          color: #94A3B8;
          padding: 40px 20px;

          .empty-icon { font-size: 40px; margin-bottom: 10px; }
          p { font-size: 13px; margin: 0 0 12px 0; }
        }
      }

      /* 右栏 LLM 回复与时序 */
      &.col-right {
        .right-content-scroll {
          display: flex;
          flex-direction: column;
          gap: 16px;
          overflow-y: auto;
          max-height: 600px;
          padding-right: 4px;

          .response-section {
            display: flex;
            flex-direction: column;
            gap: 8px;

            .sec-label {
              display: flex;
              justify-content: space-between;
              align-items: center;
              font-size: 12.5px;
              font-weight: 700;
              color: #334155;
            }

            .response-box {
              background: #F8FAFC;
              border: 1px solid #E2E8F0;
              border-radius: 8px;
              padding: 14px;
              font-size: 13px;
              line-height: 1.7;
              color: #1E293B;
              white-space: pre-wrap;
            }
          }

          .prompt-collapse {
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            overflow: hidden;

            .raw-prompt-preview {
              margin: 0;
              font-family: ui-monospace, monospace;
              font-size: 11px;
              background: #0F172A;
              color: #E2E8F0;
              padding: 12px;
              border-radius: 6px;
              overflow-x: auto;
              white-space: pre-wrap;
              word-break: break-word;
            }
          }
        }

        .right-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          flex: 1;
          color: #94A3B8;
          padding: 40px 20px;

          .empty-icon { font-size: 40px; margin-bottom: 10px; }
          p { font-size: 13px; margin: 0; text-align: center; line-height: 1.5; }
        }
      }
    }
  }
}
</style>

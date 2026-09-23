<template>
  <div class="document-parse-container">
    <!-- 顶部工作台双联动控制器 (知识库切换 + 文档选择 + 流水线触发) -->
    <div class="filter-panel-card mb-4">
      <div class="filter-controls">
        <div class="filter-item">
          <span class="filter-label">知识库：</span>
          <el-select
            :model-value="kbId"
            placeholder="切换知识库"
            style="width: 260px"
            :loading="kbLoading"
            popper-class="kb-select-popper"
            @change="handleKbChange"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="Number(kb.id)"
            >
              <div class="kb-option-item">
                <span class="kb-name-text">{{ kb.name }}</span>
                <span v-if="kb.courseName" class="kb-course-tag">{{ kb.courseName }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="filter-item">
          <span class="filter-label">目标文档：</span>
          <el-select
            v-model="selectedDocumentId"
            placeholder="选择要解析的文档"
            style="width: 340px"
            :no-data-text="documents.length === 0 ? '当前知识库暂无文档' : '无匹配文档'"
            popper-class="doc-select-popper"
            @change="handleDocumentChange"
          >
            <el-option
              v-for="doc in documents"
              :key="doc.id"
              :label="doc.fileName || doc.name"
              :value="Number(doc.id)"
            >
              <div class="doc-option-item">
                <span class="doc-name-text" :title="doc.fileName || doc.name">
                  {{ doc.fileName || doc.name }}
                </span>
                <el-tag size="small" :type="getParseStatusTagType(doc.parseStatus, doc)">
                  {{ formatParseStatusLabel(doc.parseStatus, doc) }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </div>
      </div>

      <div class="filter-actions">
        <el-button
          type="primary"
          :icon="VideoPlay"
          :loading="pipelineRunning"
          :disabled="!selectedDocumentId"
          class="run-pipeline-btn"
          @click="handleRunPipeline"
        >
          执行解析 → 切片 → 向量化
        </el-button>
      </div>
    </div>

    <!-- 文档未选择或空知识库提示 -->
    <el-alert
      v-if="documents.length === 0 && !loading"
      type="info"
      title="当前知识库暂无入库文档"
      description="请先在当前知识库的「文档管理」中上传 PDF、Word 或 Markdown 课件，或者切换至其他包含课件的知识库。"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <!--
      原「文档信息卡 + 重新执行流水线」已移除：
      文件名在筛选条的「目标文档」里、切片/向量数量在顶部统计卡里、
      「重新执行流水线」与筛选条上的「执行解析 → 切片 → 向量化」是同一个 handleRunPipeline，
      整块属于重复信息。
    -->

    <!--
      原「三阶流水线执行步进指示器」已移除：它是只读状态条（文本解析 / 语义分块 / 向量索引），
      其中「已完成」与切片数在切片面板「共 N 块切片」里已有、向量状态在知识库顶部统计卡里已有，
      且第三阶会直接把后端枚举 INDEXED 显示给用户。整块属于重复信息。
    -->

    <!-- 工作台：切片分块流（文档目录大纲已按需求移除） -->
    <div class="workspace-grid">
      <div class="chunks-col">
        <el-card shadow="never" class="panel-card">
          <div class="panel-header-row">
            <h3 class="panel-title">已生成的切片分块</h3>
            <span v-if="chunks.length > 0" class="chunks-count-hint">共 {{ chunks.length }} 块切片</span>
          </div>
          <el-empty v-if="chunks.length === 0" description="暂无切片数据，可点击上方「执行解析」按钮开始处理" />
          <div v-else class="chunks-flow">
            <div
              v-for="(chunk, cIdx) in chunks"
              :id="`chunk-card-${chunk.id}`"
              :key="chunk.id"
              class="chunk-card"
            >
              <div class="chunk-top">
                <div class="chunk-index-badge">
                  <span class="chunk-num">#Chunk {{ cIdx + 1 }}</span>
                  <span class="tokens-tag">{{ chunk.tokenCount }} Tokens</span>
                  <span v-if="chunk.pageNo" class="page-tag">P.{{ chunk.pageNo }}</span>
                </div>
                <el-tag size="small" :type="chunk.status === 'INDEXED' ? 'success' : 'info'">
                  {{ chunk.status === 'INDEXED' ? '已向量化' : (chunk.status || '待索引') }}
                </el-tag>
              </div>
              <div v-if="chunk.heading" class="chunk-heading-preview">
                <el-icon class="mr-1"><Collection /></el-icon>
                <span>{{ chunk.heading }}</span>
              </div>
              <div class="chunk-content">{{ chunk.content }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { VideoPlay, Collection } from '@element-plus/icons-vue';
import { useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';
import { useDocumentParse } from '@/composables/knowledge/useDocumentUpload';
// 文档下拉选项的状态标签仍在使用这两个工具函数
import { formatParseStatusLabel, getParseStatusTagType } from '@/utils/knowledge/document';

const { kbId } = useKnowledgeRoute();

const {
  knowledgeBases,
  kbLoading,
  documents,
  selectedDocumentId,
  chunks,
  loading,
  pipelineRunning,
  handleKbChange,
  handleDocumentChange,
  handleRunPipeline
} = useDocumentParse(kbId);

// 文档目录大纲、三阶流水线状态条均已按需求移除（模板、样式与本处逻辑一并清理）
</script>

<style scoped lang="scss">
.document-parse-container {
  // 本页作为「知识库详情」的子视图渲染，外层（.kb-detail-container）已有统一留白与页面底色。
  // 这里再套一层 padding + 灰底，会让下方所有卡片比顶部 hero 左右各缩进 24px，出现"顶部边框长、内容边框短"的错位。
  padding: 0;
  background: transparent;

  .filter-panel-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    background: #ffffff;
    padding: 16px 20px;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);

    .filter-controls {
      display: flex;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
    }

    .filter-item {
      display: flex;
      align-items: center;
      gap: 8px;

      .filter-label {
        font-size: 13.5px;
        font-weight: 600;
        color: #475569;
        white-space: nowrap;
      }
    }

    .run-pipeline-btn {
      font-weight: 600;
      padding: 0 20px;
      border-radius: 9999px;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
    }
  }

  .workspace-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;

    .panel-card {
      border-radius: 14px;
      border: 1px solid #e2e8f0;
    }

    .panel-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      .panel-title {
        font-size: 15px;
        font-weight: 700;
        margin: 0;
        color: #0f172a;
      }

      .outline-count-hint,
      .chunks-count-hint {
        font-size: 12px;
        color: #94a3b8;
      }
    }

    .outline-tree-wrapper {
      max-height: 620px;
      overflow-y: auto;

      .outline-tree {
        :deep(.el-tree-node__content) {
          height: 34px;
          border-radius: 6px;
          margin-bottom: 2px;
          color: #334155;
          font-size: 13px;

          &:hover {
            background: #eff6ff;
            color: #2563eb;
          }
        }
      }
    }

    .chunks-flow {
      display: flex;
      flex-direction: column;
      gap: 14px;
      max-height: 620px;
      overflow-y: auto;
      padding-right: 4px;
    }

    .chunk-card {
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px;
      background: #ffffff;
      transition: all 0.25s ease;

      &:hover {
        border-color: #bfdbfe;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.06);
      }

      &.is-highlighted {
        border-color: #3b82f6;
        background: #f0f7ff;
        box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.3);
      }

      .chunk-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
      }

      .chunk-index-badge {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        font-weight: 600;

        .chunk-num {
          color: #1e293b;
        }

        .tokens-tag {
          color: #64748b;
          background: #f1f5f9;
          padding: 1px 7px;
          border-radius: 4px;
        }

        .page-tag {
          color: #2563eb;
          background: #eff6ff;
          padding: 1px 6px;
          border-radius: 4px;
        }
      }

      .chunk-heading-preview {
        display: flex;
        align-items: center;
        font-size: 12.5px;
        font-weight: 600;
        color: #2563eb;
        margin-bottom: 8px;
      }

      .chunk-content {
        font-size: 13px;
        line-height: 1.65;
        color: #334155;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
  }
}

@media (max-width: 960px) {
  .document-parse-container {
    .workspace-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>

<style lang="scss">
/**
 * 下拉选项会被 teleport 到 body，组件里的 scoped 样式匹配不到（弹层下不存在带 scope-id 的祖先），
 * 结果「知识库名 + 课程标签」会挤成一整行纯文本，看起来像名称重复。
 * 这里改用 popper-class + 全局样式限定作用域。
 */
.kb-select-popper .kb-option-item,
.doc-select-popper .doc-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;

  .kb-name-text,
  .doc-name-text {
    font-size: 13.5px;
    color: #1e293b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .kb-course-tag {
    flex-shrink: 0;
    font-size: 11px;
    color: #64748b;
    background: #f1f5f9;
    padding: 1px 6px;
    border-radius: 4px;
  }
}
</style>

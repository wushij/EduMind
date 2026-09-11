<template>
  <div class="document-parse-container">
    <!-- 顶部导航 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.back()">
        返回知识库详情
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库中心</el-breadcrumb-item>
        <el-breadcrumb-item>文档解析与向量切片工作台</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- V0.5 规划能力交互原型提示 -->
    <el-alert
      title="【V0.5 规划能力交互原型】本页面为文档智能切片分块、大纲树提取与嵌入向量入库工作台展示。生产级 OCR 深度解析流水线与 Milvus 向量库联动将于 V0.5 正式上线。"
      type="warning"
      effect="light"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <!-- 文档状态大看板 -->
    <div class="doc-hero-card">
      <div class="doc-info-left">
        <div class="doc-icon">📄</div>
        <div>
          <div class="doc-title-row">
            <h2 class="doc-title">{{ docInfo.fileName }}</h2>
            <el-tag type="success" size="small">解析完成 · 向量就绪</el-tag>
          </div>
          <div class="doc-meta-row">
            <span>文件大小：{{ docInfo.fileSize }}</span>
            <span>文档格式：{{ docInfo.fileType }}</span>
            <span>切分块数：<strong class="text-blue-600">{{ chunks.length }} 块</strong></span>
            <span>向量维度：<strong>1024 维 (bge-large-zh)</strong></span>
          </div>
        </div>
      </div>

      <div class="doc-actions-right">
        <el-button type="primary" plain @click="handleReParse">
          🔄 重新执行切片与向量化
        </el-button>
      </div>
    </div>

    <!-- 解析流程流水线可视化步骤条 -->
    <div class="pipeline-card">
      <div class="pipeline-steps">
        <div class="step-node active">
          <div class="step-circle">1</div>
          <div class="step-text">
            <span class="step-name">格式清洗与 OCR 提取</span>
            <span class="step-status">100% 完成</span>
          </div>
        </div>
        <div class="step-arrow">➔</div>

        <div class="step-node active">
          <div class="step-circle">2</div>
          <div class="step-text">
            <span class="step-name">标题大纲结构化重构</span>
            <span class="step-status">识别 4 级目录</span>
          </div>
        </div>
        <div class="step-arrow">➔</div>

        <div class="step-node active">
          <div class="step-circle">3</div>
          <div class="step-text">
            <span class="step-name">语义段落分块 (Chunking)</span>
            <span class="step-status">生成 {{ chunks.length }} 个独立分块</span>
          </div>
        </div>
        <div class="step-arrow">➔</div>

        <div class="step-node active">
          <div class="step-circle">4</div>
          <div class="step-text">
            <span class="step-name">嵌入向量入库 (VectorStore)</span>
            <span class="step-status">Milvus 检索就绪</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 左右双栏：左侧文档目录大纲与原文摘要，右侧已切分 Chunks 细化审查 -->
    <div class="workspace-grid">
      <!-- 左栏：大纲索引 -->
      <div class="outline-col">
        <el-card shadow="never" class="panel-card">
          <h3 class="panel-title">📑 文档结构化目录大纲</h3>
          <div class="outline-tree-wrapper">
            <el-tree
              :data="outlineData"
              default-expand-all
              node-key="id"
              highlight-current
              @node-click="handleOutlineClick"
            />
          </div>
        </el-card>
      </div>

      <!-- 右栏：切片分块详情流 -->
      <div class="chunks-col">
        <el-card shadow="never" class="panel-card">
          <div class="chunks-header">
            <h3 class="panel-title">🧩 已生成的切片分块 (Chunks)</h3>
            <span class="text-xs text-slate-500">点击分块可高亮审查对应上下文或微调边界</span>
          </div>

          <div class="chunks-flow">
            <div
              v-for="(chunk, cIdx) in chunks"
              :key="chunk.id"
              class="chunk-card"
            >
              <div class="chunk-top">
                <div class="chunk-index-badge">
                  <span>#Chunk {{ cIdx + 1 }}</span>
                  <span class="tokens-tag">{{ chunk.tokenCount }} Tokens</span>
                </div>
                <div class="chunk-meta-tags">
                  <span class="score-tag">权重：1.0</span>
                  <el-button link type="primary" size="small" @click="handleEditChunk(chunk)">
                    编辑分块
                  </el-button>
                </div>
              </div>

              <div class="chunk-content">
                {{ chunk.content }}
              </div>

              <div class="chunk-footer">
                <span class="vector-hash">向量标识：{{ chunk.vectorId }}</span>
                <span class="time-tag">提取于 {{ chunk.createTime }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';

const router = useRouter();

const docInfo = ref({
  fileName: '数据结构与算法_第3章_树与二叉树遍历深度解析.pdf',
  fileSize: '4.2 MB',
  fileType: 'PDF',
  status: 'PARSED'
});

const outlineData = ref([
  {
    id: 1,
    label: '第3章 树与二叉树基本概念',
    children: [
      { id: 11, label: '3.1 树的定义与抽象数据类型' },
      { id: 12, label: '3.2 二叉树的性质与满二叉树' }
    ]
  },
  {
    id: 2,
    label: '第4章 二叉树的遍历与线索化',
    children: [
      { id: 21, label: '4.1 先序、中序与后序递归算法' },
      { id: 22, label: '4.2 基于辅助栈的非递归遍历实现' },
      { id: 23, label: '4.3 层次遍历与广度优先搜索队列' }
    ]
  }
]);

const chunks = ref([
  {
    id: 101,
    tokenCount: 428,
    vectorId: 'vec_bge_0x7fa89012',
    createTime: '2026-09-11 12:30:10',
    content: '二叉树（Binary Tree）是 n (n >= 0) 个结点的有限集合，该集合或者为空集（称为空二叉树），或者由一个根结点和两棵互不相交的、分别称为根结点的左子树和右子树的二叉树组成。二叉树的第 i 层上至多有 2^(i-1) 个结点（i >= 1）。深度为 k 的二叉树至多有 2^k - 1 个结点。'
  },
  {
    id: 102,
    tokenCount: 480,
    vectorId: 'vec_bge_0x7fa89013',
    createTime: '2026-09-11 12:30:12',
    content: '先序遍历（Preorder Traversal）的操作过程为：若二叉树为空，则空操作返回；否则：1. 访问根结点；2. 先序遍历左子树；3. 先序遍历右子树。代码核心在于递归基的判断 if (root == NULL) return; 随后执行 visit(root) 并继续向下探查。'
  },
  {
    id: 103,
    tokenCount: 512,
    vectorId: 'vec_bge_0x7fa89014',
    createTime: '2026-09-11 12:30:15',
    content: '非递归中序遍历算法核心思想：利用辅助栈显式模拟系统调用栈。沿着根结点的左子树一路将结点入栈，直到左子树为空；然后弹出栈顶结点访问之，再转向其右子树重复该流程。时间复杂度为 O(n)，空间复杂度取决于二叉树的高度 O(h)。'
  }
]);

function handleOutlineClick(data: any) {
  ElMessage.info(`已定位到目录：${data.label}`);
}

function handleEditChunk(chunk: any) {
  ElMessage.info(`正在准备编辑 Chunk #${chunk.id} 内容与边界`);
}

function handleReParse() {
  ElMessage.success('已重新下发大文档语义分块与高维向量重新计算任务！');
}
</script>

<style scoped lang="scss">
.document-parse-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);

  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .doc-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .doc-info-left {
      display: flex;
      align-items: center;
      gap: 18px;

      .doc-icon {
        width: 56px;
        height: 56px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
      }

      .doc-title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .doc-title {
          font-size: 20px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }
      }

      .doc-meta-row {
        display: flex;
        gap: 20px;
        font-size: 13px;
        color: #64748b;
      }
    }
  }

  .pipeline-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    padding: 16px 28px;
    margin-bottom: 24px;

    .pipeline-steps {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .step-node {
        display: flex;
        align-items: center;
        gap: 12px;

        .step-circle {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          background: #2563eb;
          color: #ffffff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          font-size: 14px;
        }

        .step-text {
          display: flex;
          flex-direction: column;

          .step-name {
            font-size: 13px;
            font-weight: 700;
            color: #1e293b;
          }

          .step-status {
            font-size: 12px;
            color: #10b981;
          }
        }
      }

      .step-arrow {
        color: #94a3b8;
        font-size: 16px;
      }
    }
  }

  .workspace-grid {
    display: grid;
    grid-template-columns: 320px 1fr;
    gap: 24px;

    .panel-card {
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      padding: 18px 20px;

      .panel-title {
        font-size: 15px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 16px;
      }

      .chunks-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;

        .panel-title {
          margin: 0;
        }
      }
    }

    .chunks-flow {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .chunk-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 16px 18px;
        transition: all 0.2s;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
        }

        .chunk-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 10px;

          .chunk-index-badge {
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: 700;
            color: #2563eb;
            font-size: 14px;

            .tokens-tag {
              font-size: 11px;
              background: #eff6ff;
              color: #2563eb;
              padding: 1px 6px;
              border-radius: 4px;
              font-weight: normal;
            }
          }

          .chunk-meta-tags {
            display: flex;
            align-items: center;
            gap: 8px;

            .score-tag {
              font-size: 11px;
              color: #64748b;
            }
          }
        }

        .chunk-content {
          font-size: 13px;
          line-height: 1.6;
          color: #334155;
          margin-bottom: 10px;
          background: #ffffff;
          padding: 10px 12px;
          border-radius: 6px;
          border: 1px solid #edf2f7;
        }

        .chunk-footer {
          display: flex;
          justify-content: space-between;
          font-size: 11px;
          color: #94a3b8;
          font-family: monospace;
        }
      }
    }
  }
}
</style>

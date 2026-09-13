import { RouteRecordRaw } from 'vue-router';

export const knowledgeRoutes: RouteRecordRaw[] = [
  {
    path: '/knowledge',
    name: 'KnowledgeBaseList',
    component: () => import('@/views/knowledge/KnowledgeBaseList.vue'),
    meta: { title: '知识库管理', requiresAuth: true }
  },
  {
    path: '/knowledge/create',
    name: 'KnowledgeBaseCreate',
    component: () => import('@/views/knowledge/KnowledgeBaseCreate.vue'),
    meta: { title: '创建知识库', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/knowledge/ocr',
    name: 'KnowledgeOcrWorkspace',
    component: () => import('@/views/knowledge/ocr/OcrWorkspace.vue'),
    meta: { title: '多模态 OCR 识别与校对', requiresAuth: true, roles: ['ADMIN', 'TEACHER'] }
  },
  {
    path: '/knowledge/:id',
    name: 'KnowledgeBaseDetail',
    component: () => import('@/views/knowledge/KnowledgeBaseDetail.vue'),
    redirect: to => `/knowledge/${to.params.id}/documents`,
    meta: { title: '知识库详情', requiresAuth: true },
    children: [
      {
        path: 'documents',
        name: 'KnowledgeDocuments',
        component: () => import('@/views/knowledge/detail/Documents.vue'),
        meta: { title: '文档管理', requiresAuth: true }
      },
      {
        path: 'parse',
        name: 'KnowledgeDocumentParse',
        component: () => import('@/views/knowledge/detail/DocumentParse.vue'),
        meta: { title: '文档解析', requiresAuth: true }
      },
      {
        path: 'chunks',
        name: 'KnowledgeChunks',
        component: () => import('@/views/knowledge/detail/Chunks.vue'),
        meta: { title: '切片管理', requiresAuth: true }
      },
      {
        path: 'embeddings',
        name: 'KnowledgeEmbeddings',
        component: () => import('@/views/knowledge/detail/Embeddings.vue'),
        meta: { title: '向量状态', requiresAuth: true }
      },
      {
        path: 'retrieval',
        name: 'KnowledgeRetrieval',
        component: () => import('@/views/knowledge/detail/Retrieval.vue'),
        meta: { title: '检索测试', requiresAuth: true }
      },
      {
        path: 'rag-debug',
        name: 'KnowledgeRAGDebug',
        component: () => import('@/views/knowledge/detail/RAGDebug.vue'),
        meta: { title: 'RAG 诊断', requiresAuth: true }
      },
      {
        path: 'graph',
        name: 'KnowledgeGraph',
        component: () => import('@/views/knowledge/detail/KnowledgeGraph.vue'),
        meta: { title: '知识图谱', requiresAuth: true }
      }
    ]
  }
];

export interface KBDocument {
  id: number;
  knowledgeBaseId?: number;
  baseId?: number;
  /** 后端 fileName，兼容旧代码中的 name */
  name?: string;
  fileName: string;
  fileType?: string;
  fileSize?: number;
  chunkCount?: number;
  parseStatus?: string;
  chunkStatus?: string;
  errorMessage?: string;
  createTime?: string;
  createdAt?: string;
}

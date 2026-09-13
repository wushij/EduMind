export interface OcrPageVO {
  id: number;
  taskId: number;
  pageNo?: number;
  pageNumber?: number;
  imageUrl?: string;
  rawText?: string;
  rawOcrText?: string;
  proofreadText?: string;
  formulaLatex?: string;
  blocksJson?: string;
  boundingBoxesJson?: string;
  confidenceScore?: number;
  proofreadStatus?: boolean;
  status?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
}

export interface OcrTaskVO {
  id: number;
  tenantId: number;
  documentId: number;
  engine: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  pageCount: number;
  completedPageCount: number;
  errorMessage?: string;
  pages?: OcrPageVO[];
  createTime?: string;
}

export interface OcrTaskCreateRequest {
  documentId: number;
  engine: 'PADDLE_OCR' | 'MINERU' | 'GPT4O_VISION';
}

export interface OcrPageUpdateRequest {
  proofreadText: string;
}

export interface KBDocument {
  id: number;
  baseId: number;
  name: string;
  type: string;
  size: number;
  chunkCount: number;
  status: 'PENDING' | 'PARSING' | 'COMPLETED' | 'FAILED';
  uploadTime: string;
}

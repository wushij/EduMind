export interface CourseResourceItem {
  id: number;
  courseId: number;
  resourceId?: number;
  documentId?: number;
  title: string;
  resourceType: string;
  createTime?: string;
  size?: string;
  downloadUrl?: string;
  knowledgeParseStatus?: string;
  knowledgeChunkCount?: number;
}

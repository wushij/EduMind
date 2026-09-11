import { get, post, del } from '@/core/http/request';
import { axiosInstance } from '@/core/http/axios';
import { KBDocument } from '@/types/knowledge/document';

export const getDocuments = (kbId: number) => get<KBDocument[]>(`/knowledge-bases/${kbId}/documents`);

export const getDocumentDetail = (id: number) => get<KBDocument>(`/documents/${id}`);

export const uploadDocument = async (kbId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await axiosInstance.post(`/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};

export const deleteDocument = (kbId: number, documentId: number) =>
  del<void>(`/knowledge-bases/${kbId}/documents/${documentId}`);

export const parseDocument = (kbId: number, documentId: number) =>
  post<void>(`/knowledge-bases/${kbId}/documents/${documentId}/parse`);

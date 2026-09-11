export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page?: number;
  pageNum?: number;
  pageSize?: number;
}

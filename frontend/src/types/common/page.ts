export interface PageQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  list: T[];
}

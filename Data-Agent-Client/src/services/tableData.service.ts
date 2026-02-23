import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

export interface TableDataResponse {
  headers: string[];
  rows: unknown[][];
  totalCount: number;
  currentPage: number;
  pageSize: number;
  totalPages: number;
}

export const tableDataService = {
  getTableData: async (
    connectionId: string,
    tableName: string,
    catalog?: string,
    schema?: string,
    currentPage: number = 1,
    pageSize: number = 100
  ): Promise<TableDataResponse> => {
    const params: Record<string, string | number> = {
      connectionId,
      tableName,
      currentPage,
      pageSize
    };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<TableDataResponse>(ApiPaths.TABLE_DATA, { params });
    return response.data;
  },

  getViewData: async (
    connectionId: string,
    viewName: string,
    catalog?: string,
    schema?: string,
    currentPage: number = 1,
    pageSize: number = 100
  ): Promise<TableDataResponse> => {
    const params: Record<string, string | number> = {
      connectionId,
      viewName,
      currentPage,
      pageSize
    };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<TableDataResponse>(ApiPaths.VIEW_DATA, { params });
    return response.data;
  },
};

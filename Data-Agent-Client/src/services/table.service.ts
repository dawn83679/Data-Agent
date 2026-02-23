import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

export const tableService = {
  listTables: async (connectionId: string, catalog?: string, schema?: string): Promise<string[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;
    
    const response = await http.get<string[]>(ApiPaths.TABLES, { params });
    return response.data;
  },

  getTableDdl: async (
    connectionId: string,
    tableName: string,
    catalog?: string,
    schema?: string
  ): Promise<string> => {
    const params: Record<string, string> = {
      connectionId,
      tableName
    };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<string>(ApiPaths.TABLES_DDL, { params });
    return response.data;
  },

  deleteTable: async (
    connectionId: string,
    tableName: string,
    catalog?: string,
    schema?: string
  ): Promise<void> => {
    await http.delete(ApiPaths.TABLES, {
      data: {
        connectionId,
        tableName,
        catalog,
        schema
      }
    });
  },
};

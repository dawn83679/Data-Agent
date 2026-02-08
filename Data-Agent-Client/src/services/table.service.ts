import http from '../lib/http';

export const tableService = {
  listTables: async (connectionId: string, catalog?: string, schema?: string): Promise<string[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;
    
    const response = await http.get<string[]>('/tables', { params });
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
    
    const response = await http.get<string>('/tables/ddl', { params });
    return response.data;
  },
};

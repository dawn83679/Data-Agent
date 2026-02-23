import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

export const viewService = {
  listViews: async (connectionId: string, catalog?: string, schema?: string): Promise<string[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<string[]>(ApiPaths.VIEWS, { params });
    return response.data;
  },

  getViewDdl: async (
    connectionId: string,
    viewName: string,
    catalog?: string,
    schema?: string
  ): Promise<string> => {
    const params: Record<string, string> = {
      connectionId,
      viewName,
    };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    if (schema != null && schema !== '') params.schema = schema;

    const response = await http.get<string>(ApiPaths.VIEWS_DDL, { params });
    return response.data;
  },

  deleteView: async (
    connectionId: string,
    viewName: string,
    catalog?: string,
    schema?: string
  ): Promise<void> => {
    await http.delete(ApiPaths.VIEWS, {
      data: {
        connectionId,
        viewName,
        catalog,
        schema
      }
    });
  },
};

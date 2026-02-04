import http from '../lib/http';

/**
 * List schema names for the given connection and optional catalog.
 * Requires the connection to be opened first via connectionService.openConnection.
 */
export const schemaService = {
  listSchemas: async (connectionId: string, catalog?: string): Promise<string[]> => {
    const params: Record<string, string> = { connectionId };
    if (catalog != null && catalog !== '') params.catalog = catalog;
    const response = await http.get<string[]>('/schemas', { params });
    return response.data;
  },
};

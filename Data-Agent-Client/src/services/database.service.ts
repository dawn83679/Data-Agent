import http from '../lib/http';
import { ApiPaths } from '../constants/apiPaths';

/**
 * List database names on an active connection.
 * Requires the connection to be opened first via connectionService.openConnection.
 */
export const databaseService = {
  listDatabases: async (connectionId: string): Promise<string[]> => {
    const response = await http.get<string[]>(ApiPaths.DATABASES, {
      params: { connectionId },
    });
    return response.data;
  },

  deleteDatabase: async (connectionId: string, databaseName: string): Promise<void> => {
    await http.delete(ApiPaths.DATABASES, {
      data: { connectionId, databaseName }
    });
  },
};

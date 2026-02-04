import http from '../lib/http';

/**
 * List database names on an active connection.
 * Requires the connection to be opened first via connectionService.openConnection.
 */
export const databaseService = {
  listDatabases: async (connectionId: string): Promise<string[]> => {
    const response = await http.get<string[]>('/databases', {
      params: { connectionId },
    });
    return response.data;
  },
};

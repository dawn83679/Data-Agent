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
    await http.post(`${ApiPaths.DATABASES}/delete`, null, {
      params: { connectionId, databaseName },
    });
  },

  /**
   * Get list of available character sets for a connection
   */
  getCharacterSets: async (connectionId: string): Promise<string[]> => {
    const response = await http.get<string[]>(`${ApiPaths.DATABASES}/charsets`, {
      params: { connectionId },
    });
    return response.data;
  },

  /**
   * Get list of available collations for a given character set
   */
  getCollations: async (connectionId: string, charset: string): Promise<string[]> => {
    const response = await http.get<string[]>(`${ApiPaths.DATABASES}/collations`, {
      params: { connectionId, charset },
    });
    return response.data;
  },

  /**
   * Create a new database
   */
  createDatabase: async (
    connectionId: string,
    databaseName: string,
    charset: string,
    collation?: string
  ): Promise<void> => {
    await http.post(`${ApiPaths.DATABASES}/create`, null, {
      params: { connectionId, databaseName, charset, collation },
    });
  },

  /**
   * Check if a database exists
   */
  databaseExists: async (connectionId: string, databaseName: string): Promise<boolean> => {
    const response = await http.get<boolean>(`${ApiPaths.DATABASES}/exists`, {
      params: { connectionId, databaseName },
    });
    return response.data;
  },
};

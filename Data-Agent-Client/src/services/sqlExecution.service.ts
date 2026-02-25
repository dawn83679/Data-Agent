import http from '../lib/http';
import type { ExecuteSqlResponse, ExecuteSqlParams } from '../types/sql';
import { SqlPaths } from '../constants/apiPaths';

export const sqlExecutionService = {
  /**
   * Execute SQL on a specific connection and database
   * @param params - ExecuteSqlParams containing connectionId, database, schema, and SQL
   * @returns ExecuteSqlResponse with results or error
   */
  executeSql: async (params: ExecuteSqlParams): Promise<ExecuteSqlResponse> => {
    const response = await http.post<ExecuteSqlResponse>(SqlPaths.EXECUTE, {
      connectionId: params.connectionId,
      databaseName: params.databaseName ?? undefined,
      schemaName: params.schemaName ?? undefined,
      sql: params.sql,
    });
    return response.data;
  },
};
